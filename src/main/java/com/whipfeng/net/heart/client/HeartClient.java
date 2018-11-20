package com.whipfeng.net.heart.client;

import com.whipfeng.net.heart.CustomHeartbeatHandler;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.LengthFieldBasedFrameDecoder;
import io.netty.handler.timeout.IdleStateHandler;

import java.util.Random;

/**
 * Created by fz on 2018/11/20.
 */
public class HeartClient {
    private String hostName;
    private int port;

    public HeartClient(String hostName, int port) {
        this.hostName = hostName;
        this.port = port;
    }

    public void run() throws Exception {
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap();
            b.group(workerGroup)
                    .channel(NioSocketChannel.class)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        public void initChannel(SocketChannel ch) throws Exception {
                            ChannelPipeline p = ch.pipeline();
                            p.addLast(new IdleStateHandler(0, 0, 5));
                            p.addLast(new LengthFieldBasedFrameDecoder(1024, 0, 4, -4, 0));
                            p.addLast(new HeartClientHandler());
                        }
                    });
            Channel ch = b.remoteAddress(hostName, port).connect().sync().channel();
            Random random = new Random(System.currentTimeMillis());
            for (int i = 0; i < 10; i++) {
                String content = "client msg " + i;
                ByteBuf buf = ch.alloc().buffer();
                buf.writeInt(5 + content.getBytes().length);
                buf.writeByte(CustomHeartbeatHandler.CUSTOM_MSG);
                buf.writeBytes(content.getBytes());
                ch.writeAndFlush(buf);

                Thread.sleep(random.nextInt(20000));
            }
        } finally {
            workerGroup.shutdownGracefully();
        }
    }

    public static void main(String[] args) throws Exception {
        String hostName = "localhost";
        int port = 8088;
        new HeartClient(hostName, port).run();
    }
}
