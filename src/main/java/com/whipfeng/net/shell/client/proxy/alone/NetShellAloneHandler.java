package com.whipfeng.net.shell.client.proxy.alone;

import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.v5.*;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by fz on 2018/11/26.
 */
public class NetShellAloneHandler extends SimpleChannelInboundHandler<DefaultSocks5CommandResponse> {

    private static final Logger logger = LoggerFactory.getLogger(NetShellAloneHandler.class);

    @Override
    protected void messageReceived(final ChannelHandlerContext alCtx, final DefaultSocks5CommandResponse cmdMsg) throws Exception {
        logger.info("Dest Server:" + cmdMsg);
        if (cmdMsg.decoderResult().isSuccess() && Socks5CommandStatus.SUCCESS.equals(cmdMsg.status())) {
            alCtx.pipeline()
                    .addBefore("N-S-C-C", null, new IdleStateHandler(0, 0, 5));
            alCtx.fireChannelActive();
            return;
        }
        alCtx.close();
    }

    @Override
    public void channelActive(ChannelHandlerContext alCtx) {
        Socks5InitialRequest initMsg = new DefaultSocks5InitialRequest(Socks5AuthMethod.PASSWORD);
        alCtx.writeAndFlush(initMsg);
    }
}
