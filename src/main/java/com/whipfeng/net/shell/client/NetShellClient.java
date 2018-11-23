package com.whipfeng.net.shell.client;

import com.whipfeng.util.ArgsUtil;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
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
public class NetShellClient {

    private static final Logger logger = LoggerFactory.getLogger(NetShellClient.class);

    private String nsHost;
    private int nsPort;

    private String inHost;
    private int inPort;

    private boolean running = true;

    private BlockingQueue<NetShellClientCodec> blockingQueue = new ArrayBlockingQueue(1);

    public NetShellClient(String nsHost, int nsPort, String inHost, int inPort) {
        this.nsHost = nsHost;
        this.nsPort = nsPort;
        this.inHost = inHost;
        this.inPort = inPort;
    }

    public void run() throws Exception {
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            while (running) {
                final NetShellClientCodec netShellClientCodec = new NetShellClientCodec(blockingQueue, inHost, inPort);
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
                            logger.info("Connect OK:" + future.channel().localAddress());
                        } else {
                            boolean result = blockingQueue.remove(netShellClientCodec);
                            logger.info(result + " Lost Connect:" + future.channel().localAddress());
                            logger.error("Connect fail, will try again.", future.cause());
                        }
                    }
                });
            }
        } finally {
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        logger.info("");
        logger.info("------------------------我是分隔符------------------------");

        ArgsUtil argsUtil = new ArgsUtil(args);
        String nsHost = argsUtil.get("-nsHost", "localhost");
        int nsPort = argsUtil.get("-nsPort", 8088);

        String inHost = argsUtil.get("-inHost", "10.21.20.229");
        int inPort = argsUtil.get("-inPort", 22);

        logger.info("nsHost=" + nsHost);
        logger.info("nsPort=" + nsPort);
        logger.info("inHost=" + inHost);
        logger.info("inPort=" + inPort);

        NetShellClient netShellClient = new NetShellClient(nsHost, nsPort, inHost, inPort);
        netShellClient.run();
    }
}
