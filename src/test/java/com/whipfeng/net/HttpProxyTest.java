package com.whipfeng.net;

import com.whipfeng.net.http.HttpProxyServer;
import com.whipfeng.net.shell.server.proxy.PasswordAuth;
import org.junit.Test;

/**
 * Created by cmll on 2019/1/4.
 */
public class HttpProxyTest {

    @Test
    public void testHttpProxyServer() throws Exception {
        int port = 8088;
        HttpProxyServer httpProxyServer = new HttpProxyServer(port, new PasswordAuth() {
            @Override
            public String findPassword(String user) throws Exception {
                return "123";
            }
        });
        httpProxyServer.run();
    }
}
