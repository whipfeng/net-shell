package com.whipfeng.net.http;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.ReplayingDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.URL;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by cmll on 2019/1/4.
 */
public class HttpProxyDecoder extends ReplayingDecoder<HttpProxyDecoder.State> {

    private static final Logger logger = LoggerFactory.getLogger(HttpProxyDecoder.class);

    enum State {
        METHOD,
        ADDRESS,
        NO_HTTP_OR_DATA
    }


    private List<byte[]> reqMethods = new LinkedList<byte[]>();

    {
        reqMethods.add(HttpProxyConst.METHOD_OPTIONS);
        reqMethods.add(HttpProxyConst.METHOD_GET);
        reqMethods.add(HttpProxyConst.METHOD_HEAD);
        reqMethods.add(HttpProxyConst.METHOD_POST);
        reqMethods.add(HttpProxyConst.METHOD_PUT);
        reqMethods.add(HttpProxyConst.METHOD_DELETE);
        reqMethods.add(HttpProxyConst.METHOD_TRACE);
        reqMethods.add(HttpProxyConst.METHOD_CONNECT);
    }

    public HttpProxyDecoder() {
        super(State.METHOD);
    }

    @Override
    protected void decode(ChannelHandlerContext ctx, ByteBuf in, List<Object> out) throws Exception {
        //如果已经解码过则直接向下传递
        switch (state()) {
            case NO_HTTP_OR_DATA:
                int actualReadableBytes = actualReadableBytes();
                if (actualReadableBytes > 0) {
                    out.add(in.readSlice(actualReadableBytes).retain());
                }
                break;
            case METHOD:
                for (int idx = 0, next; ; idx = next) {
                    next = idx + 1;
                    byte com = in.readByte();
                    //遍历剔除HTTP方法
                    Iterator<byte[]> itr = reqMethods.iterator();
                    while (itr.hasNext()) {
                        byte[] reqMethod = itr.next();
                        byte cur = reqMethod[idx];
                        if (com == cur) {
                            if (reqMethod.length == next) {
                                in.readerIndex(in.readerIndex() - next);
                                checkpoint(State.ADDRESS);
                                return;
                            }
                        } else {
                            itr.remove();
                        }
                    }
                    //没有任何匹配，说明不是HTTP协议
                    if (reqMethods.isEmpty()) {
                        in.readerIndex(in.readerIndex() - next);
                        checkpoint(State.NO_HTTP_OR_DATA);
                        return;
                    }
                }
            case ADDRESS:
                byte[] buf = new byte[actualReadableBytes()];
                int idx = 0;
                for (; ; idx++) {
                    buf[idx] = in.readByte();
                    if ('\n' == buf[idx] && '\r' == buf[idx - 1]) {
                        break;
                    }
                }
                idx++;
                String addressStr = new String(buf, 0, idx - 2);
                byte[] reqMethod = reqMethods.get(0);
                logger.debug("Request Info:" + addressStr);
                HttpProxyRequest request = new HttpProxyRequest();
                request.setMethod(reqMethod);
                addressStr = addressStr.substring(reqMethod.length + 1, addressStr.length() - 9);
                if (HttpProxyConst.METHOD_CONNECT == reqMethod) {
                    String[] hostAndPort = addressStr.split(":");
                    if (hostAndPort.length != 2) {
                        throw new DecoderException("addressStr error: " + addressStr);
                    }
                    request.setHost(hostAndPort[0]);
                    request.setPort(Integer.parseInt(hostAndPort[1]));
                    for (; ; idx++) {
                        buf[idx] = in.readByte();
                        if ('\n' == buf[idx] && '\r' == buf[idx - 1] && '\n' == buf[idx - 2] && '\r' == buf[idx - 3]) {
                            break;
                        }
                    }
                    if (logger.isDebugEnabled()) {
                        logger.debug(new String(buf, 0, idx + 1));
                    }
                } else {
                    URL url = new URL(addressStr);
                    request.setHost(url.getHost());
                    request.setPort(url.getPort() == -1 ? url.getDefaultPort() : url.getPort());
                    in.readBytes(buf, idx, buf.length - idx);
                    ctx.channel().config().setAutoRead(false);
                    request.setCache(buf);
                }
                out.add(request);
                checkpoint(State.NO_HTTP_OR_DATA);
                break;
        }
    }
}
