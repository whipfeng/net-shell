package com.whipfeng.net.shell.server;

import com.whipfeng.net.heart.CustomHeartbeatEncoder;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 网络外壳服务端，监听外部网络和外壳网络
 * Created by fz on 2018/11/19.
 */
public class NetShellServer {

    private static final Logger logger = LoggerFactory.getLogger(NetShellServer.class);

    private int nsPort;
    private int outPort;
    private NetShellServerQueue bondQueue = new NetShellServerQueue();
    private EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    private EventLoopGroup workerGroup = new NioEventLoopGroup();

    public NetShellServer(int nsPort, int outPort) {
        this.nsPort = nsPort;
        this.outPort = outPort;
    }

    public void run() throws Exception {
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
                                    .addLast(new NetShellServerDecoder(bondQueue))
                                    .addLast(new CustomHeartbeatEncoder());
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
                                    .addLast(new NetShellServerHandler(bondQueue));
                        }
                    });

            ChannelFuture outFuture = outBootstrap.bind(outPort).sync();

            ChannelFuture nsClose = nsFuture.channel().closeFuture();
            ChannelFuture outClose = outFuture.channel().closeFuture();
            ChannelFutureListener closeListener = new ChannelFutureListener() {
                public void operationComplete(ChannelFuture future) {
                    if (!future.isSuccess()) {
                        logger.error("Listener fail, will stop.", future.cause());
                    }
                    shutdown();
                }
            };
            nsClose.addListener(closeListener);
            outClose.addListener(closeListener);
            nsClose.sync();
            outClose.sync();
        } finally {
            shutdown();
        }
    }

    public void shutdown() {
        bossGroup.shutdownGracefully();
        workerGroup.shutdownGracefully();
    }
}
