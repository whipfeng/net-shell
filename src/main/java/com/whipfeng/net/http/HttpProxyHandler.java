package com.whipfeng.net.http;

import com.whipfeng.net.shell.MsgExchangeHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by cmll on 2019/1/4.
 */
public class HttpProxyHandler extends SimpleChannelInboundHandler<HttpProxyRequest> {
    private static final Logger logger = LoggerFactory.getLogger(HttpProxyHandler.class);

    @Override
    public void channelActive(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelActive();
        logger.debug("Connect OK(H):" + ctx);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) throws Exception {
        ctx.fireChannelInactive();
        logger.debug("Disconnect OK(H):" + ctx);
    }

    @Override
    public void channelRead0(final ChannelHandlerContext hpCtx, final HttpProxyRequest request) throws Exception {
        logger.info("Connect OK:" + request + hpCtx);
        Bootstrap dstBootstrap = new Bootstrap();
        dstBootstrap.group(hpCtx.channel().eventLoop().parent())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    public void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline()
                                .addLast(new MsgExchangeHandler(hpCtx.channel()));
                    }
                });
        ChannelFuture dstFuture = dstBootstrap.remoteAddress(request.getHost(), request.getPort()).connect();

        //异步等待连接结果
        dstFuture.addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture dstFuture) {
                if (dstFuture.isSuccess()) {
                    Channel dstChannel = dstFuture.channel();
                    Channel hpChannel = hpCtx.channel();
                    logger.info("Finish Connect:" + dstChannel);

                    if (request.getCache().length > 0) {
                        ByteBuf buf = dstChannel.alloc().buffer(request.getCache().length);
                        buf.writeBytes(request.getCache());
                        dstChannel.writeAndFlush(buf);
                    }
                    hpCtx.pipeline().addLast(new MsgExchangeHandler(dstChannel));
                    //响应连接
                    if ("CONNECT".equals(request.getMethod())) {
                        hpChannel.writeAndFlush(HttpProxyResponse.buildConnectEstablished(request.getVersion()));
                    }

                    hpChannel.config().setAutoRead(true);
                    hpChannel.read();

                    //如果代理网络已经挂了，则直接关闭外部网络
                    if (!hpChannel.isActive()) {
                        dstChannel.close();
                    }
                } else {
                    //如果内部网络没成功，则直接关闭代理网络
                    hpCtx.close();
                }
            }
        });
    }
}
