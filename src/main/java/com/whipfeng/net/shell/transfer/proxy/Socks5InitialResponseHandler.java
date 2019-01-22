package com.whipfeng.net.shell.transfer.proxy;

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
    private String dstHost;
    private int dstPort;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DefaultSocks5InitialResponse initMsg) throws Exception {
        logger.info("Init SOCKS5:" + initMsg);
        if (initMsg.decoderResult().isSuccess()) {
            if (Socks5AuthMethod.PASSWORD.equals(initMsg.authMethod())) {
                Socks5PasswordAuthRequest authMsg = new DefaultSocks5PasswordAuthRequest(username, password);
                ctx.writeAndFlush(authMsg);
            } else {
                Socks5CommandRequest cmdMsg = new DefaultSocks5CommandRequest(Socks5CommandType.CONNECT, Socks5AddressType.DOMAIN, dstHost, dstPort);
                ctx.writeAndFlush(cmdMsg);
            }
            return;
        }
        ctx.close();
    }

    public Socks5InitialResponseHandler(String username, String password, String dstHost, int dstPort) {
        this.username = username;
        this.password = password;
        this.dstHost = dstHost;
        this.dstPort = dstPort;
    }
}
