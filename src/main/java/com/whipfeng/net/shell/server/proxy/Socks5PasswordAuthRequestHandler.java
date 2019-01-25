package com.whipfeng.net.shell.server.proxy;

import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.v5.DefaultSocks5PasswordAuthRequest;
import io.netty.handler.codec.socksx.v5.DefaultSocks5PasswordAuthResponse;
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthResponse;
import io.netty.handler.codec.socksx.v5.Socks5PasswordAuthStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by fz on 2018/11/26.
 */
public class Socks5PasswordAuthRequestHandler extends SimpleChannelInboundHandler<DefaultSocks5PasswordAuthRequest> {

    private static final Logger logger = LoggerFactory.getLogger(Socks5PasswordAuthRequestHandler.class);

    private PasswordAuth passwordAuth;

    public Socks5PasswordAuthRequestHandler(PasswordAuth passwordAuth) {
        this.passwordAuth = passwordAuth;
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext ctx, final DefaultSocks5PasswordAuthRequest msg) throws Exception {
        logger.info("Login user info: " + msg);

        if (msg.decoderResult().isSuccess()) {
            if (msg.password().equals(passwordAuth.findPassword(msg.username()))) {
                Socks5PasswordAuthResponse passwordAuthResponse = new DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.SUCCESS);
                ctx.writeAndFlush(passwordAuthResponse);
            } else {
                logger.warn("Auth fail:" + msg.username() + ", " + msg.password());
                Socks5PasswordAuthResponse passwordAuthResponse = new DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.FAILURE);
                //发送鉴权失败消息，完成后关闭channel
                ctx.writeAndFlush(passwordAuthResponse).addListener(ChannelFutureListener.CLOSE);
            }
            return;
        }
        ctx.close();
    }

    public PasswordAuth getPasswordAuth() {
        return passwordAuth;
    }
}
