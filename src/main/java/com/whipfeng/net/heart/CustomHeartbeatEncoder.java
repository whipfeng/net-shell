package com.whipfeng.net.heart;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by fz on 2018/11/20.
 */
public class CustomHeartbeatEncoder extends MessageToByteEncoder<ByteBuf> {

    private static final Logger logger = LoggerFactory.getLogger(CustomHeartbeatEncoder.class);

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf in, ByteBuf out) throws Exception {
        out.writeInt(in.readableBytes());
        out.writeByte(CustomHeartbeatConst.CUSTOM_MSG);
        out.writeBytes(in);
    }
}