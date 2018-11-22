package com.whipfeng.net.shell.server;

import io.netty.channel.ChannelHandlerContext;

import java.util.ArrayDeque;
import java.util.Queue;

/**
 * Created by fz on 2018/11/22.
 */
public class ChannelBondQueue {

    private Queue<ChannelHandlerContext> nsQueue = new ArrayDeque<ChannelHandlerContext>();
    private Queue<ChannelHandlerContext> outQueue = new ArrayDeque<ChannelHandlerContext>();

    ChannelHandlerContext matchNetShell(ChannelHandlerContext nsCtx) {
        return matchNet(nsCtx, nsQueue, outQueue);
    }

    ChannelHandlerContext matchNetOut(ChannelHandlerContext outCtx) {
        return matchNet(outCtx, outQueue, nsQueue);
    }

    private ChannelHandlerContext matchNet(ChannelHandlerContext setCtx, Queue<ChannelHandlerContext> setQueue, Queue<ChannelHandlerContext> getQueue) {
        ChannelHandlerContext getCtx = getQueue.poll();
        if (null == getCtx) {
            synchronized (this) {
                getCtx = getQueue.poll();
                if (null == getCtx) {
                    setQueue.offer(setCtx);
                }
            }
        }
        return getCtx;
    }
}
