package com.whipfeng.net.heart.server;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HeartServerHandler extends ChannelHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(HeartServerHandler.class);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            ByteBuf in = (ByteBuf) msg;
            ByteBuf out = Unpooled.copiedBuffer(in);
            byte[] data = new byte[in.readableBytes()];
            in.readBytes(data);
            String content = new String(data);
            logger.debug("<-获取内容：" + content + "，从" + ctx.channel().remoteAddress());
            ctx.write(out);
        }
    }
}
