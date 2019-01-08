package com.whipfeng.net.shell.client;

import com.whipfeng.net.heart.CustomHeartbeatDecoder;
import com.whipfeng.net.shell.MsgExchangeHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;

/**
 * 网络外壳客户端编解码器
 * Created by user on 2018/11/22.
 */
public class NetShellClientDecoder extends CustomHeartbeatDecoder {

    private static final Logger logger = LoggerFactory.getLogger(NetShellClientDecoder.class);

    private static final byte CONN_REQ_MSG = 4;
    private static final byte CONN_ACK_MSG = 5;

    private BlockingQueue<NetShellClientDecoder> blockingQueue;
    private String inHost;
    private int inPort;

    public NetShellClientDecoder(BlockingQueue<NetShellClientDecoder> blockingQueue, String inHost, int inPort) {
        this.blockingQueue = blockingQueue;
        this.inHost = inHost;
        this.inPort = inPort;
    }

    @Override
    protected void decode(final ChannelHandlerContext nsCtx, byte flag) throws Exception {
        //请求连接
        if (CONN_REQ_MSG == flag) {
            logger.debug("Received 'CONN_REQ' from: " + nsCtx);

            boolean result = blockingQueue.remove(this);
            logger.info(result + " Finish Connect:" + nsCtx);

            Bootstrap inBootstrap = new Bootstrap();
            inBootstrap.group(nsCtx.channel().eventLoop().parent())
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .option(ChannelOption.AUTO_READ, false)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline()
                                    .addLast(new MsgExchangeHandler(nsCtx.channel()));
                        }
                    });
            ChannelFuture inFuture = inBootstrap.remoteAddress(inHost, inPort).connect();

            //异步等待连接结果
            inFuture.addListener(new ChannelFutureListener() {
                public void operationComplete(ChannelFuture inFuture) {
                    if (inFuture.isSuccess()) {
                        Channel inChannel = inFuture.channel();
                        Channel nsChannel = nsCtx.channel();
                        nsCtx.pipeline().addLast(new MsgExchangeHandler(inChannel));
                        //响应连接
                        sendFlagMsg(nsCtx, CONN_ACK_MSG);
                        inChannel.config().setAutoRead(true);
                        inChannel.read();

                        //如果外壳网络已经挂了，则直接关闭内部网络
                        if (!nsChannel.isActive()) {
                            inChannel.close();
                        }
                    } else {
                        //如果内部网络没成功，则直接关闭外壳网络
                        nsCtx.close();
                    }
                }
            });
            return;
        }
        super.decode(nsCtx, flag);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        boolean result = blockingQueue.remove(this);
        logger.info(result + " Lost Connect:" + ctx);
        ctx.fireChannelInactive();
    }
}
