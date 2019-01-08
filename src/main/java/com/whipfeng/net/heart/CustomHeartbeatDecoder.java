package com.whipfeng.net.heart;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.CorruptedFrameException;
import io.netty.handler.codec.ReplayingDecoder;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by fz on 2018/11/20.
 */
public class CustomHeartbeatDecoder extends ReplayingDecoder {

    private static final Logger logger = LoggerFactory.getLogger(CustomHeartbeatDecoder.class);

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        if (in.readableBytes() < CustomHeartbeatConst.HEAD_LEN) {
            return;
        }
        int readerIdx = in.readerIndex();
        int len = in.getInt(readerIdx);
        byte flag = in.getByte(readerIdx + 4);

        //发送ping,返回pong
        if (CustomHeartbeatConst.PING_MSG == flag) {
            in.skipBytes(CustomHeartbeatConst.HEAD_LEN);
            logger.debug("Received 'PING' from: " + ctx);
            sendFlagMsg(ctx, CustomHeartbeatConst.PONG_MSG);
            return;
        }

        //收到pong，打印后丢弃
        if (CustomHeartbeatConst.PONG_MSG == flag) {
            in.skipBytes(CustomHeartbeatConst.HEAD_LEN);
            logger.debug("Received 'PONG' from: " + ctx);
            return;
        }

        //收到应用消息
        if (CustomHeartbeatConst.CUSTOM_MSG == flag) {
            if (logger.isDebugEnabled()) {
                logger.debug("Received 'CUSTOM' from: " + ctx + ",len:" + in.readableBytes());
            }
            if (in.readableBytes() - CustomHeartbeatConst.HEAD_LEN < len) {
                return;
            }
            in.skipBytes(CustomHeartbeatConst.HEAD_LEN);
            ByteBuf frame = ctx.alloc().buffer(len);
            frame.writeBytes(in, len);
            out.add(frame);
            return;
        }

        if (in.readableBytes() - CustomHeartbeatConst.HEAD_LEN < len) {
            return;
        }
        in.skipBytes(CustomHeartbeatConst.HEAD_LEN);

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
        ByteBuf out = ctx.alloc().buffer(CustomHeartbeatConst.HEAD_LEN);
        out.writeInt(0);
        out.writeByte(flag);
        return ctx.writeAndFlush(out);
    }

    protected void handleAllIdle(ChannelHandlerContext ctx) {
        logger.debug("Wait timeout:" + ctx);
        sendFlagMsg(ctx, CustomHeartbeatConst.PING_MSG);
        logger.debug(" Send 'PING' to: " + ctx);
    }

    protected void handleReaderIdle(ChannelHandlerContext ctx) {
        logger.debug("Read timeout:" + ctx);
        ctx.close();
    }

    protected void handleWriterIdle(ChannelHandlerContext ctx) {
        logger.debug("Write timeout:" + ctx);
        ctx.close();
    }
}