package com.whipfeng.net.shell.server.proxy;

import com.whipfeng.net.heart.CustomHeartbeatConst;
import com.whipfeng.net.heart.CustomHeartbeatDecoder;
import com.whipfeng.net.http.HttpProxyRequest;
import com.whipfeng.net.http.HttpProxyResponse;
import com.whipfeng.net.shell.MsgExchangeHandler;
import com.whipfeng.net.shell.ContextRouter;
import com.whipfeng.net.shell.RC4Transfer;
import com.whipfeng.util.RC4Util;
import com.whipfeng.util.RSAUtil;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.handler.codec.socksx.v5.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.nio.charset.Charset;

/**
 * 网络外壳代理端编解码器
 * Created by fz on 2018/11/22.
 */
public class NetShellProxyServerDecoder extends CustomHeartbeatDecoder {

    private static final Logger logger = LoggerFactory.getLogger(NetShellProxyServerDecoder.class);

    private static final byte CONN_PRE_MSG = 4;
    private static final byte CONN_REQ_MSG = 5;
    private static final byte CONN_ACK_MSG = 6;
    private static final byte PW_EX_REQ_MSG = 7;
    private static final byte PW_EX_ACK_MSG = 8;
    private static final byte PW_EX_REQ_MSG_V2 = 9;

    private NetShellProxyServerQueue bondQueue;

    public NetShellProxyServerDecoder(NetShellProxyServerQueue bondQueue) {
        this.bondQueue = bondQueue;
    }

    @Override
    protected void decode(final ChannelHandlerContext nsCtx, byte flag) throws Exception {
        //响应连接
        if (CONN_ACK_MSG == flag) {
            logger.debug("Received(P) 'CONN_ACK' from: " + nsCtx);
            MsgExchangeHandler msgExchangeHandler = nsCtx.pipeline().get(MsgExchangeHandler.class);
            Channel outChannel = msgExchangeHandler.getChannel();
            Channel nsChannel = nsCtx.channel();
            Object attach = msgExchangeHandler.takeAttach();
            if (attach instanceof HttpProxyRequest) {
                HttpProxyRequest request = (HttpProxyRequest) attach;
                //响应连接
                if ("CONNECT".equals(request.getMethod())) {
                    outChannel.writeAndFlush(HttpProxyResponse.buildConnectEstablished(request.getVersion()));
                }
                if (request.getCache().length > 0) {
                    ByteBuf buf = nsChannel.alloc().buffer(request.getCache().length);
                    buf.writeBytes(request.getCache());
                    nsChannel.writeAndFlush(buf);
                }
                outChannel.config().setAutoRead(true);
                outChannel.read();
            } else {
                Socks5CommandResponse commandResponse = new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, Socks5AddressType.IPv4);
                outChannel.writeAndFlush(commandResponse);
            }
            return;
        }
        super.decode(nsCtx, flag);
    }

    private byte[] secretKey;
    private boolean isV2 = false;

    @Override
    protected void decode(final ChannelHandlerContext nsCtx, byte flag, ByteBuf in, int len) throws Exception {
        if (PW_EX_REQ_MSG_V2 == flag) {
            isV2 = true;
            flag = PW_EX_REQ_MSG;
        }
        //交換密碼請求
        if (PW_EX_REQ_MSG == flag) {
            if (RSAUtil.noPrivateKey()) {
                return;
            }
            byte[] key = new byte[len];
            in.readBytes(key);
            secretKey = RSAUtil.privateDecrypt(key);
            RC4Transfer rc4Codec = new RC4Transfer(secretKey);
            nsCtx.pipeline().addLast(rc4Codec.getIOHandlers());
            sendFlagMsg(nsCtx, PW_EX_ACK_MSG);
            return;
        }
        //連接前置任務
        if (CONN_PRE_MSG == flag && len == 8) {
            logger.debug("Received(P) 'CONN_PRE' from: " + nsCtx);
            int networkCode = in.readInt();
            int subMaskCode = in.readInt();
            logger.info("Received(P) 'CONN_PRE':" + networkCode + "," + subMaskCode);
            ContextRouter nsRouter = new ContextRouter(nsCtx, networkCode, subMaskCode);
            ContextRouter outRouter = bondQueue.matchNetOut(nsRouter);
            if (null != outRouter) {
                logger.info("Match Net(P):" + outRouter.getCtx());
                sendReqMsg(nsRouter, outRouter);
                if (!outRouter.getCtx().channel().isActive()) {
                    nsCtx.close();
                }
            }
            return;
        }
        super.decode(nsCtx, flag);
    }

    public ChannelFuture sendReqMsg(ContextRouter nsRouter, ContextRouter outRouter) {
        ChannelHandlerContext outCtx = outRouter.getCtx();
        ChannelHandlerContext nsCtx = nsRouter.getCtx();
        Socks5CommandRequest commandRequest = outRouter.getCommandRequest();
        outCtx.pipeline().addLast(new MsgExchangeHandler(nsCtx.channel()));
        nsCtx.pipeline().addLast(new MsgExchangeHandler(outCtx.channel(), commandRequest));

        String inHost = commandRequest.dstAddr();
        int inPort = commandRequest.dstPort();
        byte[] buf = inHost.getBytes(Charset.forName("UTF-8"));
        ByteBuf out = nsCtx.alloc().buffer(CustomHeartbeatConst.HEAD_LEN + 2 + buf.length);
        out.writeInt(2 + buf.length);
        out.writeByte(CONN_REQ_MSG);
        out.writeByte((inPort >>> 8) & 255);
        out.writeByte(inPort & 255);
        out.writeBytes(buf);
        if (isV2) {
            int startIdx = out.readerIndex() + CustomHeartbeatConst.HEAD_LEN;
            RC4Util.transfer(this.secretKey, out, startIdx, startIdx + 2 + buf.length);
        }
        return nsRouter.getCtx().writeAndFlush(out);
    }
}
