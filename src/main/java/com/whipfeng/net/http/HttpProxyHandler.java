package com.whipfeng.net.http;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by cmll on 2019/1/4.
 */
public class HttpProxyHandler extends ChannelHandlerAdapter {
    private static final Logger logger = LoggerFactory.getLogger(HttpProxyHandler.class);
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        logger.debug(msg.toString());
    }
}
