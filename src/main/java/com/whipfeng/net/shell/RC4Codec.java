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
        byte[] output = transferBytes(in);
        out.writeBytes(output);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        byte[] output = transferBytes(in);
        ByteBuf frame = ctx.alloc().buffer(output.length);
        frame.writeBytes(output);
        out.add(frame);
    }

    private byte[] transferBytes(ByteBuf in) {
        byte[] input = new byte[in.readableBytes()];
        in.readBytes(input);
        return RC4Util.transfer(input, this.secretKey);
    }
}
