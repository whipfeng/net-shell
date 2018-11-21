package com.whipfeng.net.heart;

import io.netty.buffer.ByteBuf;
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

    private static final byte PING_MSG = 1;
    private static final byte PONG_MSG = 2;
    private static final byte CUSTOM_MSG = 3;

    protected String name;
    private int heartbeatCount = 0;

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
            sendPongMsg(ctx);
            return;
        }

        //收到pong，打印后丢弃
        if (PONG_MSG == flag) {
            logger.debug(name + "Response 'PONG' from: " + ctx.channel().remoteAddress());
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

    private void sendPingMsg(ChannelHandlerContext ctx) {
        ByteBuf out = ctx.alloc().buffer(HEAD_LEN);
        out.writeInt(0);
        out.writeByte(PING_MSG);
        ctx.writeAndFlush(out);
        heartbeatCount++;
        logger.debug(name + "->发送PING到：" + ctx.channel().remoteAddress() + "，总数：" + heartbeatCount);
    }

    private void sendPongMsg(ChannelHandlerContext ctx) {
        ByteBuf out = ctx.alloc().buffer(HEAD_LEN);
        out.writeInt(0);
        out.writeByte(PONG_MSG);
        ctx.writeAndFlush(out);
        heartbeatCount++;
        logger.debug(name + "->发送PONG到：" + ctx.channel().remoteAddress() + "，总数：" + heartbeatCount);
    }

    private void handleAllIdle(ChannelHandlerContext ctx) {
        logger.debug("等超时：" + ctx.channel().remoteAddress());
        sendPingMsg(ctx);
    }

    private void handleReaderIdle(ChannelHandlerContext ctx) {
        logger.debug("读超时：" + ctx.channel().remoteAddress());
        ctx.close();
        ctx.fireChannelInactive();
    }

    private void handleWriterIdle(ChannelHandlerContext ctx) {
        logger.debug("写超时：" + ctx.channel().remoteAddress());
        ctx.close();
        ctx.fireChannelInactive();
    }
}