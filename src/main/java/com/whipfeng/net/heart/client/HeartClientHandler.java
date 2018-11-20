package com.whipfeng.net.heart.client;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by fz on 2018/11/20.
 */
public class HeartClientHandler extends ChannelHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(HeartClientHandler.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            ByteBuf in = (ByteBuf) msg;
            byte[] data = new byte[in.readableBytes()];
            in.readBytes(data);
            String content = new String(data);
            logger.debug("<-获取内容：" + content + "，从" + ctx.channel().remoteAddress());
        }
    }
}
