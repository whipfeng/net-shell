package com.whipfeng.net.shell.client;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

/**
 * 网络外壳客户端，访问内部网络和外壳网络
 * Created by fz on 2018/11/20.
 */
public class NetShellClient {

    private String nsHost;
    private int nsPort;

    private String inHost;
    private int inPort;

    public NetShellClient(String nsHost, int nsPort, String inHost, int inPort) {
        this.nsHost = nsHost;
        this.nsPort = nsPort;
        this.inHost = inHost;
        this.inPort = inPort;
    }

    public void run() throws Exception {
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            Bootstrap nsBootstrap = new Bootstrap();
            nsBootstrap.group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    .addLast(new IdleStateHandler(0, 0, 5))
                                    .addLast(new NetShellClientCodec(inHost, inPort));
                        }
                    });
            nsBootstrap.remoteAddress(nsHost, nsPort).connect();
        } finally {
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        String nsHost = "localhost";
        int nsPort = 8088;

        String inHost = "10.21.20.229";
        int inPort = 22;

        new NetShellClient(nsHost, nsPort, inHost, inPort).run();
    }
}
