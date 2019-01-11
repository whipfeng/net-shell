package com.whipfeng.net.shell.server.proxy.alone;

import com.whipfeng.net.http.HttpProxyRequest;
import com.whipfeng.net.shell.ContextRouter;
import com.whipfeng.net.shell.MsgExchangeHandler;
import com.whipfeng.net.shell.server.proxy.NetShellProxyServerDecoder;
import com.whipfeng.net.shell.server.proxy.NetShellProxyServerQueue;
import io.netty.channel.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by cmll on 2019/1/4.
 */
public class NetShellHttpHandler extends SimpleChannelInboundHandler<HttpProxyRequest> {
    private static final Logger logger = LoggerFactory.getLogger(NetShellHttpHandler.class);

    private NetShellProxyServerQueue bondQueue;

    public NetShellHttpHandler(NetShellProxyServerQueue bondQueue) {
        this.bondQueue = bondQueue;
    }

    @Override
    public void messageReceived(final ChannelHandlerContext alCtx, final HttpProxyRequest request) throws Exception {
        logger.info("Connect OK:" + request + alCtx);
        ChannelPipeline pipeline = alCtx.pipeline();
        while (this != pipeline.last()) {
            pipeline.removeLast();
        }

        ContextRouter outRouter = new ContextRouter(alCtx, request);
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
    }
}
