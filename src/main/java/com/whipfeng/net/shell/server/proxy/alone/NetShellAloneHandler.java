package com.whipfeng.net.shell.server.proxy.alone;

import com.whipfeng.net.shell.ContextRouter;
import com.whipfeng.net.shell.MsgExchangeHandler;
import com.whipfeng.net.shell.server.proxy.NetShellProxyServerCodec;
import com.whipfeng.net.shell.server.proxy.NetShellProxyServerQueue;
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
    protected void messageReceived(final ChannelHandlerContext alCtx, final DefaultSocks5CommandRequest commandRequest) throws Exception {
        logger.info("Dest Server:" + commandRequest);
        if (commandRequest.decoderResult().isSuccess()) {
            Socks5CommandType cmdType = commandRequest.type();
            if (Socks5CommandType.BIND.equals(cmdType)) {
                alCtx.pipeline().addLast(new IdleStateHandler(10, 0, 0))
                        .addLast(new NetShellProxyServerCodec(bondQueue));
                Socks5CommandResponse commandResponse = new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, Socks5AddressType.IPv4);
                alCtx.writeAndFlush(commandResponse);
                return;
            }

            if (Socks5CommandType.CONNECT.equals(cmdType)) {
                ContextRouter outRouter = new ContextRouter(alCtx, commandRequest);
                ContextRouter nsRouter = bondQueue.matchNetShell(outRouter);
                if (null == nsRouter) {
                    return;
                }
                ChannelHandlerContext nsCtx = nsRouter.getCtx();
                logger.info("Match Net(A):" + nsCtx.channel().remoteAddress());
                Channel nsChannel = nsCtx.channel();
                alCtx.pipeline().addLast(new MsgExchangeHandler(nsCtx.channel()));
                nsCtx.pipeline().addLast(new MsgExchangeHandler(alCtx.channel()));
                nsCtx.pipeline().get(NetShellProxyServerCodec.class).sendReqMsg(nsCtx, commandRequest);
                if (!nsChannel.isActive()) {
                    alCtx.close();
                }
                return;
            }

            logger.warn("Wrong command type:" + commandRequest);
            alCtx.close();
        }
    }
}