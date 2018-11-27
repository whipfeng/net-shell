package com.whipfeng.net.shell.client.proxy;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * 网络外壳客户端，访问内部网络和外壳网络
 * Created by fz on 2018/11/20.
 */
public class NetShellProxyClient {

    private static final Logger logger = LoggerFactory.getLogger(NetShellProxyClient.class);

    private String nsHost;
    private int nsPort;

    private int networkCode;
    private int subMaskCode;

    private volatile long stopTime = 0;

    private boolean running = true;

    private BlockingQueue<NetShellProxyClientCodec> blockingQueue = new ArrayBlockingQueue(1);

    public NetShellProxyClient(String nsHost, int nsPort, int networkCode, int subMaskCode) {
        this.nsHost = nsHost;
        this.nsPort = nsPort;
        this.networkCode = networkCode;
        this.subMaskCode = subMaskCode;
    }

    public void run() throws Exception {
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            while (running) {
                final NetShellProxyClientCodec netShellClientCodec = new NetShellProxyClientCodec(blockingQueue, networkCode, subMaskCode);
                blockingQueue.put(netShellClientCodec);
                Bootstrap nsBootstrap = new Bootstrap();
                nsBootstrap.group(workerGroup)
                        .channel(NioSocketChannel.class)
                        .handler(new ChannelInitializer<SocketChannel>() {
                            public void initChannel(SocketChannel ch) throws Exception {
                                ch.pipeline()
                                        .addLast(new IdleStateHandler(0, 0, 5))
                                        .addLast(netShellClientCodec);
                            }
                        });

                ChannelFuture future = nsBootstrap.remoteAddress(nsHost, nsPort).connect();
                future.addListener(new ChannelFutureListener() {
                    public void operationComplete(ChannelFuture future) {
                        if (future.isSuccess()) {
                            stopTime = 0;
                            logger.info("Connect OK(P):" + future.channel().localAddress());
                        } else {
                            stopTime = 30000;//睡30秒再来
                            boolean result = blockingQueue.remove(netShellClientCodec);
                            logger.info(result + " Lost Connect:" + future.channel().localAddress());
                            logger.error("Connect fail, will try again.", future.cause());
                        }
                    }
                });
                if (stopTime > 0) {
                    Thread.sleep(stopTime);
                }
            }
        } finally {
            workerGroup.shutdownGracefully();
        }
    }
}
