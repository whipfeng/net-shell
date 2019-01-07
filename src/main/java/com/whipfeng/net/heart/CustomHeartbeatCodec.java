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

    protected static final byte HEAD_LEN = 5;

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
        int readerIdx = in.readerIndex();
        int len = in.getInt(readerIdx);
        byte flag = in.getByte(readerIdx + 4);

        //发送ping,返回pong
        if (PING_MSG == flag) {
            in.skipBytes(HEAD_LEN);
            logger.debug(name + " Received 'PING' from: " + ctx.channel().remoteAddress());
            sendFlagMsg(ctx, PONG_MSG);
            heartbeatCount++;
            logger.debug(name + " Send 'PONG' to: " + ctx.channel().remoteAddress()
                    + ",HeartbeatCount:" + heartbeatCount);
            return;
        }

        //收到pong，打印后丢弃
        if (PONG_MSG == flag) {
            in.skipBytes(HEAD_LEN);
            logger.debug(name + " Received 'PONG' from: " + ctx.channel().remoteAddress());
            return;
        }

        //收到应用消息
        if (CUSTOM_MSG == flag) {
            if (logger.isDebugEnabled()) {
                logger.debug(name + " Received 'CUSTOM' from: " + ctx.channel().remoteAddress()
                        + ",len:" + in.readableBytes());
            }
            if (in.readableBytes() - HEAD_LEN < len) {
                return;
            }
            in.skipBytes(HEAD_LEN);
            ByteBuf frame = ctx.alloc().buffer(len);
            frame.writeBytes(in, len);
            out.add(frame);
            return;
        }

        if (in.readableBytes() - HEAD_LEN < len) {
            return;
        }
        in.skipBytes(HEAD_LEN);

        if (len > 0) {
            int oldLen = in.readableBytes();
            decode(ctx, flag, in, len);
            if (oldLen - in.readableBytes() != len) {
                throw new CorruptedFrameException("Consume mismatch,flag=" + flag + ",len=" + len + ",but=" + (oldLen - in.readableBytes()));
            }
            return;
        }
        decode(ctx, flag);

    }

    protected void decode(ChannelHandlerContext ctx, byte flag, ByteBuf in, int len) throws Exception {
        throw new CorruptedFrameException("Unsupported flag: " + flag);
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
        ctx.fireUserEventTriggered(evt);
    }

    public ChannelFuture sendFlagMsg(ChannelHandlerContext ctx, byte flag) {
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
    }

    protected void handleWriterIdle(ChannelHandlerContext ctx) {
        logger.debug("Write timeout:" + ctx.channel().remoteAddress());
        ctx.close();
    }
}