package com.whipfeng.net.shell.server.proxy;

import com.whipfeng.net.heart.CustomHeartbeatCodec;
import com.whipfeng.net.shell.MsgExchangeHandler;
import com.whipfeng.net.shell.ContextRouter;
import com.whipfeng.net.shell.RC4Codec;
import com.whipfeng.util.RSAUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.socksx.v5.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.UnsupportedEncodingException;

/**
 * 网络外壳代理端编解码器
 * Created by fz on 2018/11/22.
 */
public class NetShellProxyServerCodec extends CustomHeartbeatCodec {

    private static final Logger logger = LoggerFactory.getLogger(NetShellProxyServerCodec.class);

    private static final byte CONN_PRE_MSG = 4;
    private static final byte CONN_REQ_MSG = 5;
    private static final byte CONN_ACK_MSG = 6;
    private static final byte PW_EX_REQ_MSG = 7;
    private static final byte PW_EX_ACK_MSG = 8;

    private NetShellProxyServerQueue bondQueue;

    public NetShellProxyServerCodec(NetShellProxyServerQueue bondQueue) {
        super("NS-Proxy-Server");
        this.bondQueue = bondQueue;
    }

    @Override
    protected void decode(final ChannelHandlerContext ctx, byte flag) throws Exception {
        //响应连接
        if (CONN_ACK_MSG == flag) {
            logger.debug(name + " Received(P) 'CONN_ACK' from: " + ctx.channel().remoteAddress());
            MsgExchangeHandler msgExchangeHandler = ctx.pipeline().get(MsgExchangeHandler.class);
            Channel outChannel = msgExchangeHandler.getChannel();
            Socks5CommandResponse commandResponse = new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, Socks5AddressType.IPv4);
            outChannel.writeAndFlush(commandResponse);
            return;
        }
        super.decode(ctx, flag);
    }

    @Override
    protected void decode(final ChannelHandlerContext nsCtx, byte flag, ByteBuf in, int len) throws Exception {
        //交換密碼請求
        if (PW_EX_REQ_MSG == flag) {
            if (RSAUtil.noPrivateKey()) {
                return;
            }
            byte[] key = new byte[len];
            in.readBytes(key);
            key = RSAUtil.privateDecrypt(key);
            RC4Codec rc4Codec = new RC4Codec(key);
            nsCtx.pipeline().addLast(rc4Codec);
            sendFlagMsg(nsCtx, PW_EX_ACK_MSG);
            return;
        }
        //連接前置任務
        if (CONN_PRE_MSG == flag && len == 8) {
            logger.debug(name + " Received(P) 'CONN_PRE' from: " + nsCtx.channel().remoteAddress());
            int networkCode = in.readInt();
            int subMaskCode = in.readInt();
            logger.info(name + " Received(P) 'CONN_PRE':" + networkCode + "," + subMaskCode);
            ContextRouter nsRouter = new ContextRouter(nsCtx, networkCode, subMaskCode);
            ContextRouter outRouter = bondQueue.matchNetOut(nsRouter);
            if (null != outRouter) {
                ChannelHandlerContext outCtx = outRouter.getCtx();
                logger.info("Match Net(P):" + outCtx.channel().remoteAddress());
                Channel outChannel = outCtx.channel();
                outCtx.pipeline().addLast(new MsgExchangeHandler(nsCtx.channel()));
                nsCtx.pipeline().addLast(new MsgExchangeHandler(outCtx.channel()));
                sendReqMsg(nsRouter, outRouter);
                if (!outChannel.isActive()) {
                    nsCtx.close();
                }
            }
            return;
        }
        super.decode(nsCtx, flag);
    }

    public ChannelFuture sendReqMsg(ContextRouter nsRouter, ContextRouter outRouter) throws UnsupportedEncodingException {
        String inHost = outRouter.getCommandRequest().dstAddr();
        int inPort = outRouter.getCommandRequest().dstPort();
        byte[] buf = inHost.getBytes("UTF-8");
        ByteBuf out = nsRouter.getCtx().alloc().buffer(HEAD_LEN + 2 + buf.length);
        out.writeInt(2 + buf.length);
        out.writeByte(CONN_REQ_MSG);
        out.writeByte((inPort >>> 8) & 255);
        out.writeByte(inPort & 255);
        out.writeBytes(buf);
        return nsRouter.getCtx().writeAndFlush(out);
    }
}
