package com.whipfeng.net.http;

import java.nio.charset.Charset;

/**
 * Created by cmll on 2019/1/4.
 */
public class HttpProxyResponse {

    public static HttpProxyResponse buildConnectEstablished(String version) {
        String connectEstablished = version + " 200 Connection Established\r\n" +
                "\r\n";
        return buildResponse(connectEstablished);
    }

    public static HttpProxyResponse buildBasicAuthResponse(String version, String html, boolean isKeepAlive) {
        String basicAuth = version + " 407 Unauthorized\r\n" +
                "Proxy-Authenticate: Basic realm=\"Net-Shell-Auth\"\r\n" +
                "Content-Type: text/html;charset=utf-8\r\n" +
                "Content-Length: " + html.getBytes(Charset.forName("UTF-8")).length + "\r\n" +
                "Proxy-Connection: " + (isKeepAlive ? "keep-alive" : "close") + "\r\n" +
                "\r\n" + html;
        return buildResponse(basicAuth);
    }

    public static HttpProxyResponse buildDigestAuthResponse(String version, String html, boolean isKeepAlive, long nonce, boolean stale) {
        String digestAuth = version + " 407 Unauthorized\r\n" +
                "Proxy-Authenticate: Digest realm=\"Net-Shell-Auth\", nonce=\"" + nonce + "\", stale=\"" + stale + "\"\r\n" +
                "Content-Type: text/html;charset=utf-8\r\n" +
                "Content-Length: " + html.getBytes(Charset.forName("UTF-8")).length + "\r\n" +
                "Proxy-Connection: " + (isKeepAlive ? "keep-alive" : "close") + "\r\n" +
                "\r\n" + html;
        return buildResponse(digestAuth);
    }

    public static HttpProxyResponse buildResponse(String respStr) {
        return new HttpProxyResponse(respStr.getBytes(Charset.forName("UTF-8")));
    }

    private byte[] ack;

    public HttpProxyResponse(byte[] ack) {
        this.ack = ack;
    }

    public byte[] getAck() {
        return ack;
    }

    public void setAck(byte[] ack) {
        this.ack = ack;
    }

    @Override
    public String toString() {
        return "HttpProxyResponse{" +
                "ack=" + new String(ack) +
                '}';
    }
}
