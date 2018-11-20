package com.whipfeng.net.heart.client;

import com.whipfeng.net.heart.CustomHeartbeatHandler;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by fz on 2018/11/20.
 */
public class HeartClientHandler extends CustomHeartbeatHandler {

    private static final Logger logger = LoggerFactory.getLogger(HeartClientHandler.class);

    public HeartClientHandler() {
        super("客户端");
    }

    @Override
    protected void handleData(ChannelHandlerContext context, ByteBuf byteBuf) {
        byte[] data = new byte[byteBuf.readableBytes() - 5];
        byteBuf.skipBytes(5);
        byteBuf.readBytes(data);
        String content = new String(data);
        logger.debug(name + "<-获取内容：" + content+"，从：" + context.channel().remoteAddress());
    }

    @Override
    protected void handleAllIdle(ChannelHandlerContext ctx) {
        super.handleAllIdle(ctx);
        sendPingMsg(ctx);
    }
}
