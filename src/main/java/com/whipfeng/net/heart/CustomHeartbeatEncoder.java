package com.whipfeng.net.heart;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import io.netty.handler.timeout.IdleStateEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by fz on 2018/11/20.
 */
public class CustomHeartbeatEncoder extends MessageToByteEncoder<ByteBuf> {

    private static final Logger logger = LoggerFactory.getLogger(CustomHeartbeatEncoder.class);

    private static final byte PING_MSG = 1;
    private static final byte PONG_MSG = 2;
    private static final byte CUSTOM_MSG = 3;
    protected String name;
    private int heartbeatCount = 0;

    public CustomHeartbeatEncoder(String name) {
        this.name = name;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            ByteBuf in = (ByteBuf) msg;
            byte b = in.getByte(4);
            if (b == PING_MSG) {
                sendPongMsg(ctx);
            } else if (b == PONG_MSG) {
                logger.debug(name + "<-获取PONG从：" + ctx.channel().remoteAddress());
            } else {
                in.skipBytes(5);
                ctx.fireChannelRead(in);
            }
        } else {
            ctx.fireChannelRead(msg);
        }
    }

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        logger.debug("已连接：" + ctx.channel().remoteAddress());
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        logger.debug("已断开：" + ctx.channel().remoteAddress());
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        logger.error("异常断开：" + ctx.channel().remoteAddress(), cause);
        ctx.close();
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf in, ByteBuf out) throws Exception {
        out.writeInt(5 + in.readableBytes());
        out.writeByte(CUSTOM_MSG);
        out.writeBytes(in);
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
        ByteBuf out = ctx.alloc().buffer(5);
        out.writeInt(5);
        out.writeByte(PING_MSG);
        ctx.writeAndFlush(out);
        heartbeatCount++;
        logger.debug(name + "->发送PING到：" + ctx.channel().remoteAddress() + "，总数：" + heartbeatCount);
    }

    private void sendPongMsg(ChannelHandlerContext ctx) {
        ByteBuf out = ctx.alloc().buffer(5);
        out.writeInt(5);
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