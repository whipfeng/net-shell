package com.whipfeng.net.shell.transfer.proxy;

import com.whipfeng.net.shell.MsgExchangeHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by fz on 2018/11/26.
 */
public class Socks5CommandResponseHandler extends SimpleChannelInboundHandler<DefaultSocks5CommandResponse> {

    private static final Logger logger = LoggerFactory.getLogger(Socks5CommandResponseHandler.class);

    @Override
    protected void channelRead0(final ChannelHandlerContext proxyCtx, final DefaultSocks5CommandResponse cmdMsg) throws Exception {
        logger.info("Dest Server:" + cmdMsg);
        if (cmdMsg.decoderResult().isSuccess() && Socks5CommandStatus.SUCCESS.equals(cmdMsg.status())) {

            MsgExchangeHandler msgExchangeHandler = proxyCtx.pipeline().get(MsgExchangeHandler.class);
            Channel tsfChannel = msgExchangeHandler.getChannel();

            //响应连接
            tsfChannel.config().setAutoRead(true);
            tsfChannel.read();

            //如果代理网络已经挂了，则直接关闭外部网络
            if (!tsfChannel.isActive()) {
                proxyCtx.close();
            }
            return;
        }
        proxyCtx.close();
    }
}
