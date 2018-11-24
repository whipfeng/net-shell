package com.whipfeng.net.shell.proxy;

import com.whipfeng.net.shell.MsgExchangeHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 代理转发器组装
 * Created by fz on 2018/11/22.
 */
public class NetShellProxyHandler extends ChannelHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(NetShellProxyHandler.class);

    private String outHost;
    private int outPort;

    public NetShellProxyHandler(String outHost, int outPort) {
        this.outHost = outHost;
        this.outPort = outPort;
    }

    @Override
    public void channelActive(final ChannelHandlerContext proxyCtx) {
        logger.info("Connect OK:" + proxyCtx.channel().remoteAddress());
        Bootstrap outBootstrap = new Bootstrap();
        outBootstrap.group(proxyCtx.channel().eventLoop().parent())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    public void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline()
                                .addLast(new MsgExchangeHandler(proxyCtx.channel()));
                    }
                });
        ChannelFuture outFuture = outBootstrap.remoteAddress(outHost, outPort).connect();

        //异步等待连接结果
        outFuture.addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture outFuture) {
                if (outFuture.isSuccess()) {
                    Channel outChannel = outFuture.channel();
                    Channel proxyChannel = proxyCtx.channel();

                    logger.info("Finish Connect:" + outChannel.localAddress());

                    proxyCtx.pipeline().addLast(new MsgExchangeHandler(outChannel));
                    //响应连接
                    proxyChannel.config().setAutoRead(true);
                    proxyChannel.read();

                    //如果代理网络已经挂了，则直接关闭外部网络
                    if (!proxyChannel.isActive()) {
                        outChannel.close();
                    }
                } else {
                    //如果内部网络没成功，则直接关闭代理网络
                    proxyCtx.close();
                }
            }
        });
        proxyCtx.fireChannelActive();
    }
}
