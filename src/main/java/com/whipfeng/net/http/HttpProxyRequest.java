package com.whipfeng.net.http;

import java.util.Arrays;

/**
 * Created by cmll on 2019/1/4.
 */
public class HttpProxyRequest {

    private byte[] method;

    private String host;

    private int port;

    private byte[] cache;

    public byte[] getMethod() {
        return method;
    }

    public void setMethod(byte[] method) {
        this.method = method;
    }

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public byte[] getCache() {
        return cache;
    }

    public void setCache(byte[] cache) {
        this.cache = cache;
    }

    @Override
    public String toString() {
        return "HttpProxyRequest{" +
                "method=" + new String(method) +
                ", host='" + host + '\'' +
                ", port=" + port +
                '}';
    }
}
