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
        int port = 8580;
        HttpProxyServer httpProxyServer = new HttpProxyServer(port, null);
        httpProxyServer.run();
    }
}
