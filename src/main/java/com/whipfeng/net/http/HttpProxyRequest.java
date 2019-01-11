package com.whipfeng.net.http;

import io.netty.handler.codec.DecoderResult;
import io.netty.handler.codec.socksx.SocksVersion;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandType;

import java.util.HashMap;
import java.util.Map;

/**
 * Created by cmll on 2019/1/4.
 */
public class HttpProxyRequest implements Socks5CommandRequest {

    @Override
    public Socks5CommandType type() {
        return Socks5CommandType.CONNECT;
    }

    @Override
    public Socks5AddressType dstAddrType() {
        return Socks5AddressType.DOMAIN;
    }

    @Override
    public String dstAddr() {
        return this.host;
    }

    @Override
    public int dstPort() {
        return this.port;
    }

    @Override
    public SocksVersion version() {
        return SocksVersion.SOCKS5;
    }

    @Override
    public DecoderResult decoderResult() {
        return DecoderResult.SUCCESS;
    }

    @Override
    public void setDecoderResult(DecoderResult result) {
    }

    public interface Auth {

    }

    public class BasicAuth implements Auth {

        private String upBase64;

        public BasicAuth(String upBase64) {
            this.upBase64 = upBase64;
        }

        public String getUpBase64() {
            return upBase64;
        }

        public void setUpBase64(String upBase64) {
            this.upBase64 = upBase64;
        }

        @Override
        public String toString() {
            return "BasicAuth{" +
                    "upBase64='" + upBase64 + '\'' +
                    '}';
        }
    }

    public class DigestAuth implements Auth {

        private String method;
        private String userName;
        private String realm;
        private String opaque;
        private String uri;
        private String response;
        private String qop;
        private String nc;
        private String nonce;

        public String getMethod() {
            return method;
        }

        public void setMethod(String method) {
            this.method = method;
        }

        private Map<String, String> paramMap = new HashMap<String, String>();

        public String getUserName() {
            return userName;
        }

        public void setUserName(String userName) {
            this.userName = userName;
        }

        public String getRealm() {
            return realm;
        }

        public void setRealm(String realm) {
            this.realm = realm;
        }

        public String getOpaque() {
            return opaque;
        }

        public void setOpaque(String opaque) {
            this.opaque = opaque;
        }

        public String getUri() {
            return uri;
        }

        public void setUri(String uri) {
            this.uri = uri;
        }

        public String getResponse() {
            return response;
        }

        public void setResponse(String response) {
            this.response = response;
        }

        public String getQop() {
            return qop;
        }

        public void setQop(String qop) {
            this.qop = qop;
        }

        public String getNc() {
            return nc;
        }

        public void setNc(String nc) {
            this.nc = nc;
        }

        public String getNonce() {
            return nonce;
        }

        public void setNonce(String nonce) {
            this.nonce = nonce;
        }

        public void putParam(String name, String value) {
            paramMap.put(name, value);
        }

        public String getParam(String name) {
            return paramMap.get(name);
        }

        @Override
        public String toString() {
            return "DigestAuth{" +
                    "userName='" + userName + '\'' +
                    ", realm='" + realm + '\'' +
                    ", opaque='" + opaque + '\'' +
                    ", uri='" + uri + '\'' +
                    ", response='" + response + '\'' +
                    ", qop='" + qop + '\'' +
                    ", nc='" + nc + '\'' +
                    ", nonce='" + nonce + '\'' +
                    '}';
        }
    }

    private String method;
    private String uri;
    private String version;
    private String host;
    private int port;
    private Auth auth;

    private Map<String, String> headerMap = new HashMap<String, String>();

    private byte[] cache;

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
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

    public void putHeader(String name, String value) {
        headerMap.put(name, value);
    }

    public String getHeader(String name) {
        return headerMap.get(name);
    }

    public String getUri() {
        return uri;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public Auth getAuth() {
        return auth;
    }

    public void setAuth(Auth auth) {
        this.auth = auth;
    }

    @Override
    public String toString() {
        return "HttpProxyRequest{" +
                "method='" + method + '\'' +
                ", uri='" + uri + '\'' +
                ", version='" + version + '\'' +
                ", host='" + host + '\'' +
                ", port=" + port +
                ", auth='" + auth + '\'' +
                '}';
    }
}
