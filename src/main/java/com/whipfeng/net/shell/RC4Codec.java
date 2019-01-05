package com.whipfeng.net.shell;

import com.whipfeng.util.RC4Util;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by cmll on 2019/1/2.
 */
public class RC4Codec extends ByteToMessageCodec<ByteBuf> {

    private static final Logger logger = LoggerFactory.getLogger(RC4Codec.class);

    private byte[] secretKey;

    public RC4Codec(byte[] secretKey) {
        logger.info("Consultative results =" + Base64.encodeBase64URLSafeString(secretKey));
        this.secretKey = secretKey;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, ByteBuf in, ByteBuf out) throws Exception {
        RC4Util.transfer(in, this.secretKey);
        out.writeBytes(in);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        RC4Util.transfer(in, this.secretKey);
        out.add(in);
    }
}
