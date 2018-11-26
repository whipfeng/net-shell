package com.whipfeng.net.shell.proxy;

import com.whipfeng.net.heart.CustomHeartbeatCodec;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.BlockingQueue;

/**
 * 网络外壳代理端编解码器
 * Created by user on 2018/11/22.
 */
public class NetShellProxyCodec extends CustomHeartbeatCodec {

    private static final Logger logger = LoggerFactory.getLogger(NetShellProxyCodec.class);

    private static final byte CONN_REQ_MSG = 4;
    private static final byte CONN_ACK_MSG = 5;

    private BlockingQueue<NetShellProxyCodec> blockingQueue;

    public NetShellProxyCodec(BlockingQueue<NetShellProxyCodec> blockingQueue) {
        super("NS-Proxy");
        this.blockingQueue = blockingQueue;
    }

    @Override
    protected void decode(final ChannelHandlerContext nsCtx, byte flag) throws Exception {
        //请求连接
        if (CONN_REQ_MSG == flag) {
            logger.debug(name + " Received 'CONN_REQ' from: " + nsCtx.channel().remoteAddress());

            boolean result = blockingQueue.remove(this);
            logger.info(result + " Finish Connect:" + nsCtx.channel().localAddress());

            //响应连接
            sendFlagMsg(nsCtx, CONN_ACK_MSG);
            return;
        }
        super.decode(nsCtx, flag);
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        boolean result = blockingQueue.remove(this);
        logger.info(result + " Lost Connect:" + ctx.channel().localAddress());
        ctx.fireChannelInactive();
    }
}
