package com.whipfeng.net.shell.server.proxy.alone;

import com.whipfeng.net.heart.CustomHeartbeatEncoder;
import com.whipfeng.net.shell.ContextRouter;
import com.whipfeng.net.shell.RC4Transfer;
import com.whipfeng.net.shell.server.proxy.NetShellProxyServerDecoder;
import com.whipfeng.net.shell.server.proxy.NetShellProxyServerQueue;
import com.whipfeng.net.shell.server.proxy.Socks5InitialRequestHandler;
import com.whipfeng.net.shell.server.proxy.Socks5PasswordAuthRequestHandler;
import com.whipfeng.util.RSAUtil;
import com.whipfeng.util.Socks5AddressUtil;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.v5.*;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.crypto.BadPaddingException;
import java.io.ByteArrayInputStream;
import java.io.DataInputStream;

/**
 * Created by fz on 2018/11/26.
 */
public class NetShellAloneHandler extends SimpleChannelInboundHandler<DefaultSocks5CommandRequest> {

    private static final Logger logger = LoggerFactory.getLogger(NetShellAloneHandler.class);

    private NetShellProxyServerQueue bondQueue;

    public NetShellAloneHandler(NetShellProxyServerQueue bondQueue) {
        this.bondQueue = bondQueue;
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext alCtx, DefaultSocks5CommandRequest commandRequest) throws Exception {
        if (commandRequest.decoderResult().isSuccess()) {
            Socks5CommandType cmdType = commandRequest.type();
            if (Socks5CommandType.BIND.equals(cmdType)) {
                logger.info("Bind Server:" + commandRequest);
                alCtx.pipeline().addLast(new IdleStateHandler(10, 0, 0))
                        .addLast(new NetShellProxyServerDecoder(bondQueue))
                        .addLast(new CustomHeartbeatEncoder());
                Socks5CommandResponse commandResponse = new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, commandRequest.dstAddrType());
                alCtx.writeAndFlush(commandResponse);
                return;
            }

            Socks5InitialRequestHandler initialRequest = alCtx.pipeline().get(Socks5InitialRequestHandler.class);
            if (!initialRequest.hasPassAuth()) {
                logger.warn("Un have pass auth:" + commandRequest);
                alCtx.close();
                return;
            }

            if (Socks5CommandType.CONNECT.equals(cmdType)) {
                if (Socks5AddressUtil.FOCUS.equals(commandRequest.dstAddrType())) {
                    byte[] secretBuf;
                    try {
                        secretBuf = RSAUtil.privateDecrypt(commandRequest.dstAddr().getBytes(CharsetUtil.ISO_8859_1));
                    } catch (BadPaddingException bpe) {
                        Socks5CommandResponse commandResponse = new DefaultSocks5CommandResponse(Socks5CommandStatus.COMMAND_UNSUPPORTED, Socks5AddressType.DOMAIN);
                        alCtx.writeAndFlush(commandResponse).addListener(ChannelFutureListener.CLOSE);
                        return;
                    }

                    //緩衝讀入
                    DataInputStream dIs = new DataInputStream(new ByteArrayInputStream(secretBuf));
                    byte[] secretKey = new byte[dIs.readInt()];//加密密钥
                    dIs.read(secretKey);
                    byte[] unBuf = new byte[dIs.readInt()];//用户名
                    dIs.read(unBuf);
                    byte[] pwBuf = new byte[dIs.readInt()];//授权密钥
                    dIs.read(pwBuf);
                    byte[] dhBuf = new byte[dIs.readInt()];//请求地址
                    dIs.read(dhBuf);
                    Socks5PasswordAuthRequestHandler authRequest = alCtx.pipeline().get(Socks5PasswordAuthRequestHandler.class);
                    if (!new String(pwBuf, CharsetUtil.UTF_8).equals(authRequest.getPasswordAuth().findPassword(new String(unBuf, CharsetUtil.UTF_8)))) {
                        logger.warn("Un have pass auth:" + commandRequest);
                        alCtx.close();
                        return;
                    }
                    RC4Transfer rc4Codec = new RC4Transfer(secretKey);
                    alCtx.pipeline().addLast(rc4Codec.getIOHandlers());
                    commandRequest = new DefaultSocks5CommandRequest(cmdType, Socks5AddressType.DOMAIN, new String(dhBuf, CharsetUtil.UTF_8), commandRequest.dstPort());
                }
                logger.info("Dest Server:" + commandRequest);
                ContextRouter outRouter = new ContextRouter(alCtx, commandRequest);
                ContextRouter nsRouter = bondQueue.matchNetShell(outRouter);
                if (null == nsRouter) {
                    return;
                }
                ChannelHandlerContext nsCtx = nsRouter.getCtx();
                logger.info("Match Net(A):" + nsCtx);
                nsCtx.pipeline().get(NetShellProxyServerDecoder.class).sendReqMsg(nsRouter, outRouter);
                if (!nsCtx.channel().isActive()) {
                    alCtx.close();
                }
                return;
            }
        }
        logger.warn("Wrong command type:" + commandRequest);
        alCtx.close();
    }
}
