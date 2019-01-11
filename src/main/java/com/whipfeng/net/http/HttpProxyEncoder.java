package com.whipfeng.net.http;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.MessageToByteEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;


/**
 * Created by cmll on 2019/1/4.
 */
public class HttpProxyEncoder extends MessageToByteEncoder<HttpProxyResponse> {

    private static final Logger logger = LoggerFactory.getLogger(HttpProxyEncoder.class);


    @Override
    protected void encode(ChannelHandlerContext ctx, HttpProxyResponse response, ByteBuf out) throws Exception {
        logger.info("Http response." + ctx);
        out.writeBytes(response.getAck());
    }
}
