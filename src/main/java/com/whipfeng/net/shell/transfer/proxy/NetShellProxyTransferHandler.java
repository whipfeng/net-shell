package com.whipfeng.net.shell.transfer.proxy;

import com.whipfeng.net.shell.MsgExchangeHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.v5.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 代理转发器组装
 * Created by fz on 2018/11/22.
 */
public class NetShellProxyTransferHandler extends ChannelInboundHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(NetShellProxyTransferHandler.class);

    private String proxyHost;
    private int proxyPort;

    private String username;
    private String password;
    private String dstHost;
    private int dstPort;

    public NetShellProxyTransferHandler(String proxyHost, int proxyPort, String username, String password, String dstHost, int dstPort) {
        this.proxyHost = proxyHost;
        this.proxyPort = proxyPort;
        this.username = username;
        this.password = password;
        this.dstHost = dstHost;
        this.dstPort = dstPort;
    }

    @Override
    public void channelActive(final ChannelHandlerContext tsfCtx) {
        logger.info("Connect OK:" + tsfCtx);
        Bootstrap proxyBootstrap = new Bootstrap();
        proxyBootstrap.group(tsfCtx.channel().eventLoop().parent())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    public void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline()
                                .addLast(Socks5ClientEncoder.DEFAULT)
                                .addLast(new Socks5InitialResponseDecoder())
                                .addLast(new Socks5InitialResponseHandler(username, password, dstHost, dstPort))
                                .addLast(new Socks5PasswordAuthResponseDecoder())
                                .addLast(new Socks5PasswordAuthResponseHandler(dstHost, dstPort))
                                .addLast(new Socks5CommandResponseDecoder())
                                .addLast(new Socks5CommandResponseHandler())
                                .addLast(new MsgExchangeHandler(tsfCtx.channel()));
                    }
                });
        ChannelFuture proxyFuture = proxyBootstrap.remoteAddress(proxyHost, proxyPort).connect();

        //异步等待连接结果
        proxyFuture.addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture proxyFuture) {
                if (proxyFuture.isSuccess()) {
                    Channel proxyChannel = proxyFuture.channel();
                    Channel tsfChannel = tsfCtx.channel();

                    logger.info("Finish Connect:" + proxyChannel.localAddress());

                    tsfCtx.pipeline().addLast(new MsgExchangeHandler(proxyChannel));

                    Socks5InitialRequest initMsg = new DefaultSocks5InitialRequest(Socks5AuthMethod.PASSWORD);
                    proxyChannel.writeAndFlush(initMsg);
                    //如果代理网络已经挂了，则直接关闭外部网络
                    if (!tsfChannel.isActive()) {
                        proxyChannel.close();
                    }
                } else {
                    //如果内部网络没成功，则直接关闭代理网络
                    tsfCtx.close();
                }
            }
        });
        tsfCtx.fireChannelActive();
    }
}
