package com.whipfeng.net.shell.transfer.proxy;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 网络外壳代理器，用于转发
 * Created by fz on 2018/11/19.
 */
public class NetShellProxyTransfer {

    private static final Logger logger = LoggerFactory.getLogger(NetShellProxyTransfer.class);

    private int tsfPort;
    private String proxyHost;
    private int proxyPort;
    private String username;
    private String password;
    private String dstHost;
    private int dstPort;

    private EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    private EventLoopGroup workerGroup = new NioEventLoopGroup();

    public NetShellProxyTransfer(int tsfPort, String proxyHost, int proxyPort, String username, String password, String dstHost, int dstPort) {
        this.tsfPort = tsfPort;
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.username = username;
        this.password = password;
        this.dstHost = dstHost;
        this.dstPort = dstPort;
    }

    public void run() throws Exception {
        try {
            ServerBootstrap tsfBootstrap = new ServerBootstrap();
            tsfBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.AUTO_READ, false)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    .addLast(new NetShellProxyTransferHandler(proxyHost, proxyPort, username, password, dstHost, dstPort));
                        }
                    });

            ChannelFuture tsfFuture = tsfBootstrap.bind(tsfPort).sync();
            tsfFuture.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
