package com.whipfeng.net.shell.server;

import com.whipfeng.net.heart.CustomHeartbeatCodec;
import com.whipfeng.net.shell.MsgExchangeHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.CorruptedFrameException;

/**
 * 网络外壳服务端编解码器
 * Created by user on 2018/11/22.
 */
public class NetShellServerCodec extends CustomHeartbeatCodec {

    private static final byte CONN_REQ_MSG = 4;
    private static final byte CONN_ACK_MSG = 5;

    public NetShellServerCodec() {
        super("NS-Server");
    }

    @Override
    protected void decode(final ChannelHandlerContext ctx, byte flag) throws Exception {
        //响应连接
        if (CONN_ACK_MSG == flag) {

        }
        throw new CorruptedFrameException("Unsupported flag: " + flag);
    }
}
