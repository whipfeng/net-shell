package com.whipfeng.net.shell.proxy;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5InitialRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5ServerEncoder;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * 网络外壳代理端，代理访问内部网络和外壳网络
 * Created by fz on 2018/11/20.
 */
public class NetShellProxy {

    private static final Logger logger = LoggerFactory.getLogger(NetShellProxy.class);

    private String nsHost;
    private int nsPort;

    private volatile boolean isNeedAuth;
    private PasswordAuth passwordAuth;

    private boolean running = true;

    private BlockingQueue<NetShellProxyCodec> blockingQueue = new ArrayBlockingQueue(1);

    public NetShellProxy(String nsHost, int nsPort, boolean isNeedAuth, PasswordAuth passwordAuth) {
        this.nsHost = nsHost;
        this.nsPort = nsPort;
        this.isNeedAuth = isNeedAuth;
        this.passwordAuth = passwordAuth;
    }

    public void run() throws Exception {
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            while (running) {
                final NetShellProxyCodec netShellProxyCodec = new NetShellProxyCodec(blockingQueue);
                blockingQueue.put(netShellProxyCodec);
                Bootstrap nsBootstrap = new Bootstrap();
                nsBootstrap.group(workerGroup)
                        .channel(NioSocketChannel.class)
                        .handler(new ChannelInitializer<SocketChannel>() {
                            public void initChannel(SocketChannel ch) throws Exception {
                                ch.pipeline()
                                        .addLast(new IdleStateHandler(0, 0, 5))
                                        .addLast(netShellProxyCodec)
                                        .addLast(Socks5ServerEncoder.DEFAULT)
                                        .addLast(new Socks5InitialRequestDecoder())
                                        .addLast(new Socks5InitialRequestHandler(isNeedAuth));
                                if (isNeedAuth) {
                                    ch.pipeline().addLast(new Socks5PasswordAuthRequestDecoder())
                                            .addLast(new Socks5PasswordAuthRequestHandler(passwordAuth));
                                }
                                ch.pipeline().addLast(new Socks5CommandRequestDecoder())
                                        .addLast(new Socks5CommandRequestHandler());
                            }
                        });

                ChannelFuture future = nsBootstrap.remoteAddress(nsHost, nsPort).connect();
                future.addListener(new ChannelFutureListener() {
                    public void operationComplete(ChannelFuture future) {
                        if (future.isSuccess()) {
                            logger.info("Connect OK(P):" + future.channel().localAddress());
                        } else {
                            boolean result = blockingQueue.remove(netShellProxyCodec);
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
}
