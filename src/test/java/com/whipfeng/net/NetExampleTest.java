package com.whipfeng.net;

import com.whipfeng.net.discard.DiscardServer;
import com.whipfeng.net.echo.EchoServer;
import com.whipfeng.net.heart.client.HeartClient;
import com.whipfeng.net.heart.server.HeartServer;
import org.junit.Test;

/**
 * Created by user on 2018/11/23.
 */
public class NetExampleTest {
    @Test
    public void testDiscardServer() throws Exception {
        int port = 8088;
        DiscardServer discardServer = new DiscardServer(port);
        discardServer.run();
    }

    @Test
    public void testEchoServer() throws Exception {
        int port = 8088;
        EchoServer echoServer = new EchoServer(port);
        echoServer.run();
    }

    @Test
    public void testHeartServer() throws Exception {
        int port = 8088;
        HeartServer heartServer = new HeartServer(port);
        heartServer.run();
    }

    @Test
    public void testHeartClient() throws Exception {
        String hostName = "localhost";
        int port = 8088;
        HeartClient heartClient = new HeartClient(hostName, port);
        heartClient.run();
    }
}
