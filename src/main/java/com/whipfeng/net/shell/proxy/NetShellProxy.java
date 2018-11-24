package com.whipfeng.net.shell.proxy;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 网络外壳代理器，用于转发
 * Created by fz on 2018/11/19.
 */
public class NetShellProxy {

    private static final Logger logger = LoggerFactory.getLogger(NetShellProxy.class);

    private int proxyPort;
    private String outHost;
    private int outPort;
    private EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    private EventLoopGroup workerGroup = new NioEventLoopGroup();

    public NetShellProxy(int proxyPort, String outHost, int outPort) {
        this.proxyPort = proxyPort;
        this.outHost = outHost;
        this.outPort = outPort;
    }

    public void run() throws Exception {
        try {
            ServerBootstrap proxyBootstrap = new ServerBootstrap();
            proxyBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.AUTO_READ, false)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    .addLast(new NetShellProxyHandler(outHost, outPort));
                        }
                    });

            ChannelFuture proxyFuture = proxyBootstrap.bind(proxyPort).sync();
            proxyFuture.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
