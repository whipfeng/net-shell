package com.whipfeng.net.shell.server;

import com.whipfeng.net.shell.MsgExchangeHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 通道纽带
 * Created by fz on 2018/11/22.
 */
public class NetShellServerHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(NetShellServerHandler.class);

    private NetShellServerQueue bondQueue;

    public NetShellServerHandler(NetShellServerQueue bondQueue) {
        this.bondQueue = bondQueue;
    }

    @Override
    public void channelActive(ChannelHandlerContext outCtx) {
        logger.info("Connect OK:" + outCtx);
        ChannelHandlerContext nsCtx = bondQueue.matchNetShell(outCtx);
        if (null != nsCtx) {
            logger.info("Match Net:" + nsCtx);
            Channel nsChannel = nsCtx.channel();
            outCtx.pipeline().addLast(new MsgExchangeHandler(nsCtx.channel()));
            nsCtx.pipeline().addLast(new MsgExchangeHandler(outCtx.channel()));
            nsCtx.pipeline().get(NetShellServerDecoder.class).sendFlagMsg(nsCtx, NetShellServerDecoder.CONN_REQ_MSG);
            if (!nsChannel.isActive()) {
                outCtx.close();
            }
        }
        outCtx.fireChannelActive();
    }
}
