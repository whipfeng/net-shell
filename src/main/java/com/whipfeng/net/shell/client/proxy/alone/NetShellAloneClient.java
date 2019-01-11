package com.whipfeng.net.shell.client.proxy.alone;

import com.whipfeng.net.heart.CustomHeartbeatEncoder;
import com.whipfeng.net.shell.client.proxy.NetShellProxyClientDecoder;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.v5.Socks5ClientEncoder;
import io.netty.handler.codec.socksx.v5.Socks5CommandResponseDecoder;
import io.netty.handler.codec.socksx.v5.Socks5InitialResponseDecoder;
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthResponseDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.BlockingQueue;

/**
 * 网络外壳客户端，访问内部网络和外壳网络
 * Created by fz on 2018/11/20.
 */
public class NetShellAloneClient {

    private static final Logger logger = LoggerFactory.getLogger(NetShellAloneClient.class);

    private String alHost;
    private int alPort;
    private volatile boolean isNeedAuth;
    private String username;
    private String password;

    private int networkCode;
    private int subMaskCode;

    private volatile long stopTime = 0;

    private boolean running = true;

    private BlockingQueue<NetShellProxyClientDecoder> blockingQueue = new ArrayBlockingQueue(1);

    public NetShellAloneClient(String alHost, int alPort, boolean isNeedAuth, String username, String password, int networkCode, int subMaskCode) {
        this.alHost = alHost;
        this.alPort = alPort;
        this.isNeedAuth = isNeedAuth;
        this.username = username;
        this.password = password;
        this.networkCode = networkCode;
        this.subMaskCode = subMaskCode;
    }

    public void run() throws Exception {
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            while (running) {
                final NetShellProxyClientDecoder netShellClientDecoder = new NetShellProxyClientDecoder(blockingQueue, networkCode, subMaskCode);
                blockingQueue.put(netShellClientDecoder);
                Bootstrap alBootstrap = new Bootstrap();
                alBootstrap.group(workerGroup)
                        .channel(NioSocketChannel.class)
                        .handler(new ChannelInitializer<SocketChannel>() {
                            public void initChannel(SocketChannel ch) throws Exception {
                                ch.pipeline()
                                        .addLast(Socks5ClientEncoder.DEFAULT)
                                        .addLast(new Socks5InitialResponseDecoder())
                                        .addLast(new Socks5InitialResponseHandler(username, password));

                                if (isNeedAuth) {
                                    ch.pipeline().addLast(new Socks5PasswordAuthResponseDecoder())
                                            .addLast(new Socks5PasswordAuthResponseHandler());
                                }

                                ch.pipeline().addLast(new Socks5CommandResponseDecoder())
                                        .addLast(new NetShellAloneHandler(isNeedAuth))
                                        .addLast("N-S-C-C", netShellClientDecoder)
                                        .addLast(new CustomHeartbeatEncoder());
                            }
                        });

                ChannelFuture future = alBootstrap.remoteAddress(alHost, alPort).connect();
                future.addListener(new ChannelFutureListener() {
                    public void operationComplete(ChannelFuture future) {
                        if (future.isSuccess()) {
                            stopTime = 0;
                            logger.info("Connect OK(A):" + future);
                        } else {
                            stopTime = 30000;//睡30秒再来
                            boolean result = blockingQueue.remove(netShellClientDecoder);
                            logger.info(result + " Lost Connect:" + future);
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
