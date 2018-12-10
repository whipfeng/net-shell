package com.whipfeng.net.shell.client.proxy.alone;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.v5.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by fz on 2018/11/26.
 */
public class Socks5PasswordAuthResponseHandler extends SimpleChannelInboundHandler<DefaultSocks5PasswordAuthResponse> {

    private static final Logger logger = LoggerFactory.getLogger(Socks5PasswordAuthResponseHandler.class);

    @Override
    protected void messageReceived(final ChannelHandlerContext ctx, final DefaultSocks5PasswordAuthResponse authMsg) throws Exception {
        logger.info("Login user info: " + authMsg);

        if (authMsg.decoderResult().isSuccess() && Socks5PasswordAuthStatus.SUCCESS.equals(authMsg.status())) {
            Socks5CommandRequest cmdMsg = new DefaultSocks5CommandRequest(Socks5CommandType.BIND, Socks5AddressType.IPv4, "127.0.0.1", 1080);
            ctx.writeAndFlush(cmdMsg);
            return;
        }
        ctx.close();
    }
}
