package com.whipfeng.net.shell.client.proxy.alone;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.v5.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by fz on 2018/11/26.
 */
public class Socks5InitialResponseHandler extends SimpleChannelInboundHandler<DefaultSocks5InitialResponse> {

    private static final Logger logger = LoggerFactory.getLogger(Socks5InitialResponseHandler.class);

    private String username;
    private String password;

    public Socks5InitialResponseHandler(String username, String password) {
        this.username = username;
        this.password = password;
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, DefaultSocks5InitialResponse initMsg) throws Exception {
        logger.info("Init SOCKS5:" + initMsg);
        if (initMsg.decoderResult().isSuccess()) {
            if (Socks5AuthMethod.PASSWORD.equals(initMsg.authMethod())) {
                Socks5PasswordAuthRequest authMsg = new DefaultSocks5PasswordAuthRequest(username, password);
                ctx.writeAndFlush(authMsg);
            } else {
                Socks5CommandRequest cmdMsg = new DefaultSocks5CommandRequest(Socks5CommandType.BIND, Socks5AddressType.IPv4, "127.0.0.1", 1080);
                ctx.writeAndFlush(cmdMsg);
            }
            return;
        }
        ctx.close();
    }
}
