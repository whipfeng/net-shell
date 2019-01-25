package com.whipfeng.net.http;

import com.whipfeng.net.shell.server.proxy.PasswordAuth;
import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.ReplayingDecoder;
import org.apache.commons.codec.digest.DigestUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

/**
 * Created by cmll on 2019/1/4.
 */
public class HttpProxyDecoder extends ReplayingDecoder<HttpProxyDecoder.State> {

    private static byte[] METHOD_OPTIONS = new byte[]{'O', 'P', 'T', 'I', 'O', 'N', 'S'};
    private static byte[] METHOD_GET = new byte[]{'G', 'E', 'T'};
    private static byte[] METHOD_HEAD = new byte[]{'H', 'E', 'A', 'D'};
    private static byte[] METHOD_POST = new byte[]{'P', 'O', 'S', 'T'};
    private static byte[] METHOD_PUT = new byte[]{'P', 'U', 'T'};
    private static byte[] METHOD_DELETE = new byte[]{'D', 'E', 'L', 'E', 'T', 'E'};
    private static byte[] METHOD_TRACE = new byte[]{'T', 'R', 'A', 'C', 'E'};
    private static byte[] METHOD_CONNECT = new byte[]{'C', 'O', 'N', 'N', 'E', 'C', 'T'};

    private static final Logger logger = LoggerFactory.getLogger(HttpProxyDecoder.class);

    enum State {
        METHOD,
        ADDRESS,
        NO_HTTP_OR_DATA
    }


    private List<byte[]> reqMethods = new LinkedList<byte[]>();

    {
        reqMethods.add(METHOD_OPTIONS);
        reqMethods.add(METHOD_GET);
        reqMethods.add(METHOD_HEAD);
        reqMethods.add(METHOD_POST);
        reqMethods.add(METHOD_PUT);
        reqMethods.add(METHOD_DELETE);
        reqMethods.add(METHOD_TRACE);
        reqMethods.add(METHOD_CONNECT);
    }

    private PasswordAuth passwordAuth;

    public HttpProxyDecoder(PasswordAuth passwordAuth) {
        super(State.METHOD);
        this.passwordAuth = passwordAuth;
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
                HttpProxyRequest request = new HttpProxyRequest();
                byte[] buf = new byte[actualReadableBytes()];
                int idx = 0;
                for (; ; idx++) {
                    buf[idx] = in.readByte();
                    if ('\n' == buf[idx] && '\r' == buf[idx - 1]) {
                        break;
                    }
                }
                idx++;
                String firstLine = new String(buf, 0, idx - 2);
                String[] firstSplit = firstLine.split(" ");
                if (firstSplit.length != 3) {
                    in.readerIndex(in.readerIndex() - idx);
                    checkpoint(State.NO_HTTP_OR_DATA);
                    return;
                }
                request.setMethod(firstSplit[0]);
                request.setUri(firstSplit[1]);
                request.setVersion(firstSplit[2]);

                int nextIdx = idx;
                for (; ; idx++) {
                    buf[idx] = in.readByte();
                    if ('\n' == buf[idx] && '\r' == buf[idx - 1]) {
                        int len = idx - nextIdx - 1;
                        if (len == 0) {
                            break;
                        }
                        String nextLine = new String(buf, nextIdx, len);
                        nextIdx = idx + 1;
                        String[] nextSplit = nextLine.split(": ");
                        if (nextSplit.length != 2) {
                            in.readerIndex(in.readerIndex() - nextIdx);
                            checkpoint(State.NO_HTTP_OR_DATA);
                            return;
                        }
                        request.putHeader(nextSplit[0], nextSplit[1]);
                        continue;
                    }
                }
                idx++;

                String hostName = request.getHeader("Host");
                if (null == hostName) {
                    in.readerIndex(in.readerIndex() - idx);
                    checkpoint(State.NO_HTTP_OR_DATA);
                    return;
                }
                String[] hostAndPort = hostName.split(":");
                if (hostAndPort.length == 1) {
                    request.setHost(hostAndPort[0]);
                    request.setPort(80);
                } else {
                    request.setHost(hostAndPort[0]);
                    request.setPort(Integer.parseInt(hostAndPort[1]));
                }

                byte[] reqMethod = reqMethods.get(0);

                //如果需要校验授权
                if (null != this.passwordAuth) {
                    String proxyAuth = request.getHeader("Proxy-Authorization");
                    if (null != proxyAuth) {
                        if (proxyAuth.startsWith("Digest ")) {
                            HttpProxyRequest.DigestAuth auth = request.new DigestAuth();
                            request.setAuth(auth);
                            String[] proxyAuthSplit = proxyAuth.substring(7, proxyAuth.length() - 1).split("\", ");
                            for (String authParam : proxyAuthSplit) {
                                String[] authParamSplit = authParam.split("=\"", -1);
                                auth.putParam(authParamSplit[0], authParamSplit[1]);
                            }
                            auth.setNonce(auth.getParam("nonce"));
                            auth.setNc(auth.getParam("nc"));
                            auth.setOpaque(auth.getParam("opaque"));
                            auth.setQop(auth.getParam("qop"));
                            auth.setRealm(auth.getParam("realm"));
                            auth.setResponse(auth.getParam("response"));
                            auth.setUri(auth.getParam("uri"));
                            auth.setUserName(auth.getParam("username"));
                            auth.setMethod(request.getMethod());
                        } else {
                            HttpProxyRequest.BasicAuth auth = request.new BasicAuth(proxyAuth.substring(6));
                            request.setAuth(auth);
                        }
                    }

                    boolean passAuth = true;
                    boolean stale = true;
                    HttpProxyRequest.Auth auth = request.getAuth();
                    if (null == auth || !(auth instanceof HttpProxyRequest.DigestAuth)) {
                        passAuth = false;
                        stale = false;
                    }
                    if (passAuth) {
                        HttpProxyRequest.DigestAuth digestAuth = (HttpProxyRequest.DigestAuth) auth;
                        try {
                            long oldNonce = Long.parseLong(digestAuth.getNonce());
                            long nonce = System.currentTimeMillis();
                            //一秒换一次
                            if (nonce - oldNonce > 1000) {
                                passAuth = false;
                                stale = true;
                            }
                        } catch (NumberFormatException nfe) {
                            passAuth = false;
                            stale = false;
                        }
                    }
                    if (passAuth) {
                        HttpProxyRequest.DigestAuth digestAuth = (HttpProxyRequest.DigestAuth) auth;
                        String password = passwordAuth.findPassword(digestAuth.getUserName());
                        if (password == null) {
                            passAuth = false;
                            stale = false;
                        } else {
                            String A1 = DigestUtils.md5Hex(digestAuth.getUserName() + ':' + digestAuth.getRealm() + ':' + password);
                            String A2 = DigestUtils.md5Hex(request.getMethod() + ':' + digestAuth.getUri());
                            String response = DigestUtils.md5Hex(A1 + ':' + digestAuth.getNonce() + ':' + A2);
                            if (!response.equals(digestAuth.getResponse())) {
                                passAuth = false;
                                stale = false;
                            }
                        }
                    }

                    if (!passAuth) {
                        String version = request.getVersion();
                        String pc = request.getHeader("Proxy-Connection");
                        String te = request.getHeader("Transfer-Encoding");
                        String cl = request.getHeader("Content-Length");
                        boolean isKeepAlive = (("HTTP/1.1".equals(version) && !"close".equals(pc)) || ("HTTP/1.0".equals(version) && "keep-alive".equals(pc)))
                                && ((METHOD_POST != reqMethod && METHOD_PUT != reqMethod) || "chunked".equals(te) || null != cl);
                        ChannelFuture respFuture = ctx.writeAndFlush(HttpProxyResponse.buildDigestAuthResponse(version, "Net-Shell need auth!", isKeepAlive, System.currentTimeMillis(), stale));
                        if (!isKeepAlive) {
                            respFuture.addListener(ChannelFutureListener.CLOSE);
                            return;
                        }
                        if (METHOD_POST == reqMethod || METHOD_PUT == reqMethod) {
                            if ("chunked".equals(te)) {
                                logger.warn("Have chunked entry." + request + ctx);
                                idx = 0;
                                for (; ; ) {
                                    buf[idx] = in.readByte();
                                    if ('\n' == buf[idx] && '\r' == buf[idx - 1]) {
                                        int chunkedLen = Integer.parseInt(new String(buf, 0, idx - 1), 16);
                                        in.skipBytes(chunkedLen + 2);
                                        if (chunkedLen == 0) {
                                            break;
                                        }
                                        idx = 0;
                                    } else {
                                        idx++;
                                    }
                                }
                            } else {
                                int contentLen = Integer.parseInt(cl);
                                in.skipBytes(contentLen);
                            }
                        }
                        checkpoint(State.METHOD);
                        return;
                    }
                }

                ctx.channel().config().setAutoRead(false);

                if (idx < buf.length) {
                    in.readBytes(buf, idx, buf.length - idx);
                }

                request.setCache(METHOD_CONNECT == reqMethod ? Arrays.copyOfRange(buf, idx, buf.length) : buf);

                if (logger.isDebugEnabled()) {
                    logger.debug(actualReadableBytes() + "\r\n" + new String(buf));
                }
                out.add(request);
                checkpoint(State.NO_HTTP_OR_DATA);
                break;
        }
    }
}
