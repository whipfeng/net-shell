package com.whipfeng.net.shell;

import com.whipfeng.util.RC4Util;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelPromise;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by cmll on 2019/1/2.
 */
public class RC4TransferHandler extends ChannelHandlerAdapter {

    private static final Logger logger = LoggerFactory.getLogger(RC4TransferHandler.class);

    private byte[] secretKey;

    public RC4TransferHandler(byte[] secretKey) {
        logger.info("Consultative results =" + Base64.encodeBase64URLSafeString(secretKey));
        this.secretKey = secretKey;
    }

    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        if (msg instanceof ByteBuf) {
            RC4Util.transfer(this.secretKey, (ByteBuf) msg);
        }
        ctx.fireChannelRead(msg);
    }

    public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
        if (msg instanceof ByteBuf) {
            RC4Util.transfer(this.secretKey, (ByteBuf) msg);
        }
        ctx.write(msg, promise);
    }
}
