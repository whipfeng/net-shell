package com.whipfeng.net;

import com.whipfeng.net.shell.client.NetShellClient;
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
}
