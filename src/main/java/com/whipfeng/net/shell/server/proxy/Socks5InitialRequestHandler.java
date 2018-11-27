package com.whipfeng.net.shell.server.proxy;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.SocksVersion;
import io.netty.handler.codec.socksx.v5.DefaultSocks5InitialRequest;
import io.netty.handler.codec.socksx.v5.DefaultSocks5InitialResponse;
import io.netty.handler.codec.socksx.v5.Socks5AuthMethod;
import io.netty.handler.codec.socksx.v5.Socks5InitialResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by fz on 2018/11/26.
 */
public class Socks5InitialRequestHandler extends SimpleChannelInboundHandler<DefaultSocks5InitialRequest> {

    private static final Logger logger = LoggerFactory.getLogger(Socks5InitialRequestHandler.class);

    private boolean isNeedAuth;

    public Socks5InitialRequestHandler(boolean isNeedAuth) {
        this.isNeedAuth = isNeedAuth;
    }

    @Override
    protected void messageReceived(ChannelHandlerContext ctx, DefaultSocks5InitialRequest msg) throws Exception {
        logger.debug("Init SOCKS5:" + msg);
        if (msg.decoderResult().isSuccess() && SocksVersion.SOCKS5.equals(msg.version())) {
            if (isNeedAuth) {
                Socks5InitialResponse initialResponse = new DefaultSocks5InitialResponse(Socks5AuthMethod.PASSWORD);
                ctx.writeAndFlush(initialResponse);
            } else {
                Socks5InitialResponse initialResponse = new DefaultSocks5InitialResponse(Socks5AuthMethod.NO_AUTH);
                ctx.writeAndFlush(initialResponse);
            }
            return;
        }
        logger.warn("Not SOCKS5 protocol:" + msg);
        ctx.close();
    }
}
