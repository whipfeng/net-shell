package com.whipfeng.net.shell.proxy;

import com.whipfeng.net.shell.MsgExchangeHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.v5.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by fz on 2018/11/26.
 */
public class Socks5CommandRequestHandler extends SimpleChannelInboundHandler<DefaultSocks5CommandRequest> {

    private static final Logger logger = LoggerFactory.getLogger(Socks5CommandRequestHandler.class);

    @Override
    protected void messageReceived(final ChannelHandlerContext nsCtx, final DefaultSocks5CommandRequest msg) throws Exception {
        logger.info("Dest Server:" + msg.type() + "," + msg.dstAddr() + "," + msg.dstPort());
        if (msg.type().equals(Socks5CommandType.CONNECT)) {
            logger.trace("Connect prepare:" + msg);

            Bootstrap bootstrap = new Bootstrap();
            bootstrap.group(nsCtx.channel().eventLoop().parent())
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.AUTO_READ, false)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            //将目标服务器信息转发给客户端
                            ch.pipeline()
                                    .addLast(new MsgExchangeHandler(nsCtx.channel()));
                        }
                    });
            logger.trace("Connect start:" + msg);
            ChannelFuture inFuture = bootstrap.connect(msg.dstAddr(), msg.dstPort());
            inFuture.addListener(new ChannelFutureListener() {
                public void operationComplete(final ChannelFuture inFuture) throws Exception {
                    if (inFuture.isSuccess()) {
                        logger.trace("Connect OK:" + msg);
                        Channel inChannel = inFuture.channel();
                        nsCtx.pipeline().addLast(new MsgExchangeHandler(inFuture.channel()));
                        Socks5CommandResponse commandResponse = new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, Socks5AddressType.IPv4);
                        nsCtx.writeAndFlush(commandResponse);
                        inChannel.config().setAutoRead(true);
                        inChannel.read();

                        //如果外壳网络已经挂了，则直接关闭内部网络
                        if (!nsCtx.channel().isActive()) {
                            inChannel.close();
                        }
                    } else {
                        Socks5CommandResponse commandResponse = new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, Socks5AddressType.IPv4);
                        nsCtx.writeAndFlush(commandResponse).addListener(ChannelFutureListener.CLOSE);
                    }
                }
            });
        } else {
            nsCtx.fireChannelRead(msg);
        }
    }
}
