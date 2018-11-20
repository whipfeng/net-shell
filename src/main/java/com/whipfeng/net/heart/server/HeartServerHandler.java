package com.whipfeng.net.heart.server;

import com.whipfeng.net.heart.CustomHeartbeatHandler;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HeartServerHandler extends CustomHeartbeatHandler {
    private static final Logger logger = LoggerFactory.getLogger(HeartServerHandler.class);

    public HeartServerHandler() {
        super("服务端");
    }

    @Override
    protected void handleData(ChannelHandlerContext context, ByteBuf buf) {
        byte[] data = new byte[buf.readableBytes() - 5];
        ByteBuf responseBuf = Unpooled.copiedBuffer(buf);
        buf.skipBytes(5);
        buf.readBytes(data);
        String content = new String(data);
        logger.debug(name + "<-获取内容：" + content + "，从" + context.channel().remoteAddress());
        context.write(responseBuf);
    }

    @Override
    protected void handleReaderIdle(ChannelHandlerContext ctx) {
        super.handleReaderIdle(ctx);
        logger.info("主动关闭：" + ctx.channel().remoteAddress());
        ctx.close();
    }
}
