package com.whipfeng.net.shell;

import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 交换
 * Created by fz on 2018/11/22.
 */
public class MsgExchangeHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(MsgExchangeHandler.class);

    private Channel channel;

    private Object attach;

    public MsgExchangeHandler(Channel channel) {
        this.channel = channel;
    }

    public MsgExchangeHandler(Channel channel, Object attach) {
        this.channel = channel;
        this.attach = attach;
    }

    public Channel getChannel() {
        return this.channel;
    }

    public Object takeAttach() {
        Object attach = this.attach;
        this.attach = null;
        return attach;
    }


    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        channel.writeAndFlush(msg);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        logger.info("Disconnect OK:" + ctx);
        channel.close();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) {
        logger.info("Disconnect forced:" + ctx, cause);
        channel.close();
        ctx.close();
    }
}
