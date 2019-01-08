package com.whipfeng.net.shell.transfer;

import com.whipfeng.net.shell.MsgExchangeHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 代理转发器组装
 * Created by fz on 2018/11/22.
 */
public class NetShellTransferHandler extends ChannelHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(NetShellTransferHandler.class);

    private String dstHost;
    private int dstPort;

    public NetShellTransferHandler(String dstHost, int dstPort) {
        this.dstHost = dstHost;
        this.dstPort = dstPort;
    }

    @Override
    public void channelActive(final ChannelHandlerContext tsfCtx) {
        logger.info("Connect OK:" + tsfCtx);
        Bootstrap dstBootstrap = new Bootstrap();
        dstBootstrap.group(tsfCtx.channel().eventLoop().parent())
                .channel(NioSocketChannel.class)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(new ChannelInitializer<SocketChannel>() {
                    public void initChannel(SocketChannel ch) throws Exception {
                        ch.pipeline()
                                .addLast(new MsgExchangeHandler(tsfCtx.channel()));
                    }
                });
        ChannelFuture dstFuture = dstBootstrap.remoteAddress(dstHost, dstPort).connect();

        //异步等待连接结果
        dstFuture.addListener(new ChannelFutureListener() {
            public void operationComplete(ChannelFuture dstFuture) {
                if (dstFuture.isSuccess()) {
                    Channel dstChannel = dstFuture.channel();
                    Channel tsfChannel = tsfCtx.channel();

                    logger.info("Finish Connect:" + dstChannel.localAddress());

                    tsfCtx.pipeline().addLast(new MsgExchangeHandler(dstChannel));
                    //响应连接
                    tsfChannel.config().setAutoRead(true);
                    tsfChannel.read();

                    //如果代理网络已经挂了，则直接关闭外部网络
                    if (!tsfChannel.isActive()) {
                        dstChannel.close();
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
