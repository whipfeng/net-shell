package com.whipfeng.net.shell.client;

import com.whipfeng.net.heart.CustomHeartbeatCodec;
import com.whipfeng.net.shell.MsgExchangeHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.CorruptedFrameException;

/**
 * 网络外壳客户端编解码器
 * Created by user on 2018/11/22.
 */
public class NetShellClientCodec extends CustomHeartbeatCodec {

    private static final byte CONN_REQ_MSG = 4;
    private static final byte CONN_ACK_MSG = 5;

    private String inHost;
    private int inPort;

    public NetShellClientCodec(String inHost, int inPort) {
        super("NS-Client");
        this.inHost = inHost;
        this.inPort = inPort;
    }

    @Override
    protected void decode(final ChannelHandlerContext ctx, byte flag) throws Exception {
        //请求连接
        if (CONN_REQ_MSG == flag) {
            Bootstrap inBootstrap = new Bootstrap();
            inBootstrap.group(ctx.channel().eventLoop().parent())
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.AUTO_READ, false);
            ChannelFuture inFuture = inBootstrap.remoteAddress(inHost, inPort).connect();

            //异步等待连接结果
            inFuture.addListener(new ChannelFutureListener() {
                public void operationComplete(ChannelFuture future) {
                    if (future.isSuccess()) {
                        Channel channel = future.channel();
                        ctx.pipeline().addLast(new MsgExchangeHandler(channel));
                        channel.pipeline().addLast(new MsgExchangeHandler(ctx.channel()));
                        //响应连接
                        sendFlagMsg(ctx, CONN_ACK_MSG);
                        channel.config().setAutoRead(true);
                        channel.read();

                        //如果外壳网络已经挂了，则直接关闭内部网络
                        if (!ctx.channel().isActive()) {
                            channel.close();
                        }
                    } else {
                        //如果内部网络没成功，则直接关闭外壳网络
                        ctx.close();
                    }
                }
            });
        }
        throw new CorruptedFrameException("Unsupported flag: " + flag);
    }
}
