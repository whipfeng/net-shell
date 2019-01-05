package com.whipfeng.net.http;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ByteToMessageCodec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by cmll on 2019/1/4.
 */
public class HttpProxyCodec extends ByteToMessageCodec<HttpProxyResponse> {

    private static final Logger logger = LoggerFactory.getLogger(HttpProxyCodec.class);

    enum State {
        METHOD,
        ADDRESS,
        NO_HTTP
    }

    private State state = State.METHOD;

    private int index = 0;

    private List<byte[]> reqMethods = new LinkedList<byte[]>();

    {
        reqMethods.add(new byte[]{'O', 'P', 'T', 'I', 'O', 'N', 'S'});
        reqMethods.add(new byte[]{'G', 'E', 'T'});
        reqMethods.add(new byte[]{'H', 'E', 'A', 'D'});
        reqMethods.add(new byte[]{'P', 'O', 'S', 'T'});
        reqMethods.add(new byte[]{'P', 'U', 'T'});
        reqMethods.add(new byte[]{'D', 'E', 'L', 'E', 'T', 'E'});
        reqMethods.add(new byte[]{'T', 'R', 'A', 'C', 'E'});
        reqMethods.add(new byte[]{'C', 'O', 'N', 'N', 'E', 'C', 'T'});
    }


    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        //如果已经解码过则直接向下传递
        switch (state) {
            case NO_HTTP:
                out.add(in);
                break;
            case METHOD:
                //暫未匹配到方法
                if (notMatchMethod(in, out)) {
                    break;
                }
            case ADDRESS:
                break;
        }
    }

    private boolean notMatchMethod(ByteBuf in, List<Object> out) {
        in.markReaderIndex();
        in.skipBytes(index);
        int len = in.readableBytes();
        for (; index < len; index++) {
            byte com = in.readByte();
            //遍历剔除HTTP方法
            Iterator<byte[]> itr = reqMethods.iterator();
            while (itr.hasNext()) {
                byte[] reqMethod = itr.next();
                byte cur = reqMethod[index];
                if (com == cur && reqMethod.length == index + 1) {
                    state = State.ADDRESS;
                    in.resetReaderIndex();
                    return true;
                } else {
                    itr.remove();
                }
            }
            //没有任何匹配，说明不是HTTP协议
            if (reqMethods.isEmpty()) {
                state = State.NO_HTTP;
                in.resetReaderIndex();
                out.add(in);
                return false;
            }
        }
        in.resetReaderIndex();
        return false;
    }

    @Override
    protected void encode(ChannelHandlerContext ctx, HttpProxyResponse hpr, ByteBuf out) throws Exception {

    }
}
