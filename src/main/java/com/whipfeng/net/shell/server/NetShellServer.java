package com.whipfeng.net.shell.server;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * 网络外壳服务端，监听外部网络和外壳网络
 * Created by fz on 2018/11/19.
 */
public class NetShellServer {
    private int nsPort;
    private int outPort;
    private ChannelBondQueue bondQueue = new ChannelBondQueue();

    public NetShellServer(int nsPort, int outPort) {
        this.nsPort = nsPort;
        this.outPort = outPort;
    }

    public void run() throws Exception {
        EventLoopGroup bossGroup = new NioEventLoopGroup(1);
        EventLoopGroup workerGroup = new NioEventLoopGroup();

        try {
            ServerBootstrap nsBootstrap = new ServerBootstrap();
            nsBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    .addLast(new IdleStateHandler(10, 0, 0))
                                    .addLast(new NetShellServerCodec(bondQueue));
                        }
                    });

            ChannelFuture nsFuture = nsBootstrap.bind(nsPort).sync();

            ServerBootstrap outBootstrap = new ServerBootstrap();
            outBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childOption(ChannelOption.AUTO_READ, false)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    .addLast(new ChannelBondHandler(bondQueue));
                        }
                    });

            ChannelFuture outFuture = outBootstrap.bind(outPort).sync();

            nsFuture.channel().closeFuture().sync();
            outFuture.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(String args[]) throws Exception {
        int nsPort = 8088;
        int outPort = 9099;
        new NetShellServer(nsPort, outPort).run();
    }
}
