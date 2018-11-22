package com.whipfeng.net.echo;

import io.netty.channel.ChannelHandlerAdapter;
import io.netty.channel.ChannelHandlerContext;

/**
 * @作者:CJY
 * @说明:这个是用来实现ECHO协议，这个协议的作用就是将客户端输入的信息全部返回
 * @时间:2017-4-8下午12:07:50
 */
public class EchoServerHandler extends ChannelHandlerAdapter {
    int count = 0;
    /*
     * @作者:CJY
     * @说明:该方法用于接收从客户端接收的信息
     * @时间:2017-4-8下午12:08:51
     * @see io.netty.channel.ChannelInboundHandlerAdapter#channelRead(io.netty.channel.ChannelHandlerContext, java.lang.Object)
     * @param ctx
     * @param msg
     * @throws Exception
     */
    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg)
            throws Exception {
        //ChannelHandlerContext提供各种不同的操作用于触发不同的I/O时间和操作
        //调用write方法来逐字返回接收到的信息
        //这里我们不需要在DISCARD例子当中那样调用释放，因为Netty会在写的时候自动释放
        //只调用write是不会释放的，它会缓存，直到调用flush
        ctx.write(msg);
        ctx.flush();
        System.out.println(Thread.currentThread().getName());
        count++;
        if (count > 5) {
            ctx.close();
        }
        //你可以直接使用writeAndFlush(msg)
        //ctx.writeAndFlush(msg);
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause)
            throws Exception {
        System.out.println(cause.getMessage());
        ////ctx.close();
    }

    @Override
    public void channelInactive(ChannelHandlerContext ctx) {
        System.out.println("I am lose!!!" + ctx.channel().remoteAddress());
        ctx.fireChannelInactive();
    }
}
