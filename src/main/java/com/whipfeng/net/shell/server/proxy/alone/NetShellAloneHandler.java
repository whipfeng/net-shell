package com.whipfeng.net.shell.server.proxy.alone;

import com.whipfeng.net.heart.CustomHeartbeatEncoder;
import com.whipfeng.net.shell.ContextRouter;
import com.whipfeng.net.shell.MsgExchangeHandler;
import com.whipfeng.net.shell.server.proxy.NetShellProxyServerDecoder;
import com.whipfeng.net.shell.server.proxy.NetShellProxyServerQueue;
import com.whipfeng.net.shell.server.proxy.Socks5InitialRequestHandler;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.v5.*;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by fz on 2018/11/26.
 */
public class NetShellAloneHandler extends SimpleChannelInboundHandler<DefaultSocks5CommandRequest> {

    private static final Logger logger = LoggerFactory.getLogger(NetShellAloneHandler.class);

    private NetShellProxyServerQueue bondQueue;

    public NetShellAloneHandler(NetShellProxyServerQueue bondQueue) {
        this.bondQueue = bondQueue;
    }

    @Override
    protected void channelRead0(final ChannelHandlerContext alCtx, final DefaultSocks5CommandRequest commandRequest) throws Exception {
        logger.info("Dest Server:" + commandRequest);

        if (commandRequest.decoderResult().isSuccess()) {
            Socks5CommandType cmdType = commandRequest.type();
            if (Socks5CommandType.BIND.equals(cmdType)) {
                alCtx.pipeline().addLast(new IdleStateHandler(10, 0, 0))
                        .addLast(new NetShellProxyServerDecoder(bondQueue))
                        .addLast(new CustomHeartbeatEncoder());
                Socks5CommandResponse commandResponse = new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, Socks5AddressType.IPv4);
                alCtx.writeAndFlush(commandResponse);
                return;
            }

            Socks5InitialRequestHandler initialRequest = alCtx.pipeline().get(Socks5InitialRequestHandler.class);
            if (!initialRequest.hasPassAuth()) {
                logger.warn("Un have pass auth:" + commandRequest);
                alCtx.close();
                return;
            }

            if (Socks5CommandType.CONNECT.equals(cmdType)) {
                ContextRouter outRouter = new ContextRouter(alCtx, commandRequest);
                ContextRouter nsRouter = bondQueue.matchNetShell(outRouter);
                if (null == nsRouter) {
                    return;
                }
                ChannelHandlerContext nsCtx = nsRouter.getCtx();
                logger.info("Match Net(A):" + nsCtx);
                nsCtx.pipeline().get(NetShellProxyServerDecoder.class).sendReqMsg(nsRouter, outRouter);
                if (!nsCtx.channel().isActive()) {
                    alCtx.close();
                }
                return;
            }
        }
        logger.warn("Wrong command type:" + commandRequest);
        alCtx.close();
    }
}
