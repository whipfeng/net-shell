package com.whipfeng.net.shell.server.proxy.alone;

import com.whipfeng.net.http.HttpProxyDecoder;
import com.whipfeng.net.http.HttpProxyEncoder;
import com.whipfeng.net.shell.server.proxy.*;
import com.whipfeng.util.Socks5AddressUtil;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5InitialRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthRequestDecoder;
import io.netty.handler.codec.socksx.v5.Socks5ServerEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 网络外壳服务端，监听外部网络和外壳网络
 * Created by fz on 2018/11/19.
 */
public class NetShellAloneServer {

    private static final Logger logger = LoggerFactory.getLogger(NetShellAloneServer.class);

    private int alPort;
    private volatile boolean isNeedAuth;
    private PasswordAuth passwordAuth;

    private NetShellProxyServerQueue bondQueue = new NetShellProxyServerQueue();
    private EventLoopGroup bossGroup = new NioEventLoopGroup(1);
    private EventLoopGroup workerGroup = new NioEventLoopGroup();

    public NetShellAloneServer(int alPort, boolean isNeedAuth, PasswordAuth passwordAuth) {
        this.alPort = alPort;
        this.isNeedAuth = isNeedAuth;
        this.passwordAuth = passwordAuth;
    }

    public void run() throws Exception {
        try {
            ServerBootstrap alBootstrap = new ServerBootstrap();
            alBootstrap.group(bossGroup, workerGroup)
                    .channel(NioServerSocketChannel.class)
                    .option(ChannelOption.SO_BACKLOG, 128)
                    .childOption(ChannelOption.SO_KEEPALIVE, true)
                    .childHandler(new ChannelInitializer<SocketChannel>() {
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    .addLast(new HttpProxyEncoder())
                                    .addLast(new HttpProxyDecoder(passwordAuth))
                                    .addLast(new NetShellHttpHandler(bondQueue))
                                    .addLast(new Socks5ServerEncoder(Socks5AddressUtil.DEFAULT_ENCODER))
                                    .addLast(new Socks5InitialRequestDecoder())
                                    .addLast(new Socks5InitialRequestHandler(isNeedAuth));
                            if (isNeedAuth) {
                                ch.pipeline().addLast(new Socks5PasswordAuthRequestDecoder())
                                        .addLast(new Socks5PasswordAuthRequestHandler(passwordAuth));
                            }
                            ch.pipeline().addLast(new Socks5CommandRequestDecoder(Socks5AddressUtil.DEFAULT_DECODER))
                                    .addLast(new NetShellAloneHandler(bondQueue));
                        }
                    });

            ChannelFuture alFuture = alBootstrap.bind(alPort).sync();
            alFuture.channel().closeFuture().sync();
        } finally {
            bossGroup.shutdownGracefully();
            workerGroup.shutdownGracefully();
        }
    }
}
