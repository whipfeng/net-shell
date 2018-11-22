package com.whipfeng.net.heart;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by fz on 2018/11/20.
 */
public class CustomHeartbeatCodec extends ByteToMessageCodec<ByteBuf> {

    private static final Logger logger = LoggerFactory.getLogger(CustomHeartbeatCodec.class);

    private static final byte HEAD_LEN = 5;

    protected static final byte PING_MSG = 1;
    protected static final byte PONG_MSG = 2;
    protected static final byte CUSTOM_MSG = 3;

    protected String name;
    protected int heartbeatCount = 0;

    public CustomHeartbeatCodec(String name) {
        this.name = name;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf in, ByteBuf out) throws Exception {
        out.writeInt(in.readableBytes());
        out.writeByte(CUSTOM_MSG);
        out.writeBytes(in);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() < HEAD_LEN) {
            return;
        }
        in.markReaderIndex();
        int len = in.readInt();
        byte flag = in.readByte();

        //发送ping,返回pong
        if (PING_MSG == flag) {
            sendFlagMsg(ctx, PONG_MSG);
            return;
        }

        //收到pong，打印后丢弃
        if (PONG_MSG == flag) {
            logger.debug(name + " Received 'PONG' from: " + ctx.channel().remoteAddress());
            return;
        }

        //收到应用消息
        if (CUSTOM_MSG == flag) {
            if (in.readableBytes() < len) {
                in.resetReaderIndex();
                return;
            }
            ByteBuf frame = ctx.alloc().buffer(len);
            frame.writeBytes(in, len);
            out.add(frame);
            return;
        }

        decode(ctx, flag);
    }

    protected void decode(ChannelHandlerContext ctx, byte flag) throws Exception {
        throw new CorruptedFrameException("Unsupported flag: " + flag);
    }

    @Override
    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent e = (IdleStateEvent) evt;
            switch (e.state()) {
                case READER_IDLE:
                    handleReaderIdle(ctx);
                    break;
                case WRITER_IDLE:
                    handleWriterIdle(ctx);
                    break;
                case ALL_IDLE:
                    handleAllIdle(ctx);
                    break;
            }
        }
    }

    protected ChannelFuture sendFlagMsg(ChannelHandlerContext ctx, byte flag) {
        ByteBuf out = ctx.alloc().buffer(HEAD_LEN);
        out.writeInt(0);
        out.writeByte(flag);
        return ctx.writeAndFlush(out);
    }

    protected void handleAllIdle(ChannelHandlerContext ctx) {
        logger.debug("Wait timeout:" + ctx.channel().remoteAddress());
        sendFlagMsg(ctx, PING_MSG);
        heartbeatCount++;
        logger.debug(name + " Send 'PING' to: " + ctx.channel().remoteAddress()
                + ",HeartbeatCount:" + heartbeatCount);
    }

    protected void handleReaderIdle(ChannelHandlerContext ctx) {
        logger.debug("Read timeout:" + ctx.channel().remoteAddress());
        ctx.close();
        ctx.fireChannelInactive();
    }

    protected void handleWriterIdle(ChannelHandlerContext ctx) {
        logger.debug("Write timeout:" + ctx.channel().remoteAddress());
        ctx.close();
        ctx.fireChannelInactive();
    }
}