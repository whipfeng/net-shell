package com.whipfeng.net.shell;

import com.whipfeng.util.RC4Util;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by cmll on 2019/1/2.
 */
public class RC4Transfer {

    private static final Logger logger = LoggerFactory.getLogger(RC4Transfer.class);

    private byte[] secretKey;

    public RC4Transfer(byte[] secretKey) {
        logger.info("Consultative results =" + Base64.encodeBase64URLSafeString(secretKey));
        this.secretKey = secretKey;
    }

    public ChannelHandler[] getIOHandlers() {
        ChannelHandler[] ioHandlers = new ChannelHandler[2];
        ioHandlers[0] = new ChannelInboundHandlerAdapter() {
            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
                if (msg instanceof ByteBuf) {
                    RC4Util.transfer(RC4Transfer.this.secretKey, (ByteBuf) msg);
                }
                ctx.fireChannelRead(msg);
            }
        };
        ioHandlers[1] = new ChannelOutboundHandlerAdapter() {
            @Override
            public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) throws Exception {
                if (msg instanceof ByteBuf) {
                    RC4Util.transfer(RC4Transfer.this.secretKey, (ByteBuf) msg);
                }
                ctx.write(msg, promise);
            }
        };
        return ioHandlers;
    }
}
