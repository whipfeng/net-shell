package com.whipfeng.net;

import com.whipfeng.net.shell.client.NetShellClient;
import com.whipfeng.net.shell.proxy.NetShellProxy;
import com.whipfeng.net.shell.proxy.PasswordAuth;
import com.whipfeng.net.shell.transfer.NetShellTransfer;
import com.whipfeng.net.shell.server.NetShellServer;
import org.junit.Test;

/**
 * Created by user on 2018/11/23.
 */
public class NetShellTest {

    @Test
    public void testNetShellServer() throws Exception {
        int nsPort = 8088;
        int outPort = 9099;

        NetShellServer netShellServer = new NetShellServer(nsPort, outPort);
        netShellServer.run();
    }

    @Test
    public void testNetShellClient() throws Exception {
        String nsHost = "localhost";
        int nsPort = 8808;

        String inHost = "10.21.20.229";
        int inPort = 22;

        NetShellClient netShellClient = new NetShellClient(nsHost, nsPort, inHost, inPort);
        netShellClient.run();
    }

    @Test
    public void testNetShellProxy() throws Exception {
        String nsHost = "localhost";
        int nsPort = 8088;

        NetShellProxy netShellProxy = new NetShellProxy(nsHost, nsPort, false, null);
        netShellProxy.run();
    }

    @Test
    public void testNetShellProxyWithAuth() throws Exception {
        String nsHost = "localhost";
        int nsPort = 8088;

        PasswordAuth pa = new PasswordAuth() {
            @Override
            public boolean auth(String user, String password) {
                return "test123".equals(user) && "test123".equals(password);
            }
        };

        NetShellProxy netShellProxy = new NetShellProxy(nsHost, nsPort, true, pa);
        netShellProxy.run();
    }

    @Test
    public void testNetShellTransfer() throws Exception {
        int proxyPort = 9099;
        String outHost = "10.19.18.50";
        int outPort = 19666;

        NetShellTransfer netShellProxy = new NetShellTransfer(proxyPort, outHost, outPort);
        netShellProxy.run();
    }
}
