package com.whipfeng.net.shell.server;

import com.whipfeng.net.heart.CustomHeartbeatDecoder;
import com.whipfeng.net.shell.MsgExchangeHandler;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 网络外壳服务端编解码器
 * Created by user on 2018/11/22.
 */
public class NetShellServerDecoder extends CustomHeartbeatDecoder {

    private static final Logger logger = LoggerFactory.getLogger(NetShellServerDecoder.class);

    public static final byte CONN_REQ_MSG = 4;
    public static final byte CONN_ACK_MSG = 5;
    private NetShellServerQueue bondQueue;

    public NetShellServerDecoder(NetShellServerQueue bondQueue) {
        this.bondQueue = bondQueue;
    }

    @Override
    protected void decode(final ChannelHandlerContext ctx, byte flag) throws Exception {
        //响应连接
        if (CONN_ACK_MSG == flag) {
            logger.debug("Received 'CONN_ACK' from: " + ctx);
            MsgExchangeHandler msgExchangeHandler = ctx.pipeline().get(MsgExchangeHandler.class);
            Channel outChannel = msgExchangeHandler.getChannel();
            outChannel.config().setAutoRead(true);
            outChannel.read();
            return;
        }
        super.decode(ctx, flag);
    }

    @Override
    public void channelActive(ChannelHandlerContext nsCtx) throws Exception {
        logger.info("Connect OK:" + nsCtx);
        ChannelHandlerContext outCtx = bondQueue.matchNetOut(nsCtx);
        if (null != outCtx) {
            logger.info("Match Net:" + outCtx);
            Channel outChannel = outCtx.channel();
            outCtx.pipeline().addLast(new MsgExchangeHandler(nsCtx.channel()));
            nsCtx.pipeline().addLast(new MsgExchangeHandler(outCtx.channel()));
            sendFlagMsg(nsCtx, CONN_REQ_MSG);
            if (!outChannel.isActive()) {
                nsCtx.close();
            }
        }
        super.channelActive(nsCtx);
    }
}
