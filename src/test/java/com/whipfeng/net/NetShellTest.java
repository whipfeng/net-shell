package com.whipfeng.net;

import com.whipfeng.net.shell.client.NetShellClient;
import com.whipfeng.net.shell.client.proxy.NetShellProxyClient;
import com.whipfeng.net.shell.server.proxy.NetShellProxyServer;
import com.whipfeng.net.shell.server.proxy.PasswordAuth;
import com.whipfeng.net.shell.transfer.NetShellTransfer;
import com.whipfeng.net.shell.server.NetShellServer;
import com.whipfeng.net.shell.transfer.proxy.NetShellProxyTransfer;
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
    public void testNetShellProxyServer() throws Exception {
        int nsPort = 8088;
        int outPort = 9099;

        NetShellProxyServer netShellProxyServer = new NetShellProxyServer(nsPort, outPort, false, null);
        netShellProxyServer.run();
    }

    @Test
    public void testNetShellProxyServerWithAuth() throws Exception {
        int nsPort = 8088;
        int outPort = 9099;

        PasswordAuth pa = new PasswordAuth() {
            @Override
            public boolean auth(String user, String password) {
                return "migu_log".equals(user) && "migu_log123!".equals(password);
            }
        };

        NetShellProxyServer netShellProxyServer = new NetShellProxyServer(nsPort, outPort, true, pa);
        netShellProxyServer.run();
    }

    @Test
    public void testNetShellProxyClient() throws Exception {
        String nsHost = "localhost";
        int nsPort = 8088;

        NetShellProxyClient netShellProxyClient = new NetShellProxyClient(nsHost, nsPort, 169153536, -256);
        netShellProxyClient.run();
    }

    @Test
    public void testNetShellProxyTransfer() throws Exception {
        int tsfPort = 9099;
        String proxyHost = "localhost";
        int proxyPort = 8088;
        String username = "xxx";
        String password = "xxx";
        String dstHost = "localhost";
        int dstPort = 3306;

        NetShellProxyTransfer netShellProxyTransfer = new NetShellProxyTransfer(tsfPort, proxyHost, proxyPort, username, password, dstHost, dstPort);
        netShellProxyTransfer.run();
    }

    @Test
    public void testNetShellTransfer() throws Exception {
        int proxyPort = 9099;
        String outHost = "localhost";
        int outPort = 8088;

        NetShellTransfer netShellTransfer = new NetShellTransfer(proxyPort, outHost, outPort);
        netShellTransfer.run();
    }
}
