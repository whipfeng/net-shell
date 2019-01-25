package com.whipfeng.net;

import com.whipfeng.net.shell.ContextRouter;
import com.whipfeng.net.shell.client.NetShellClient;
import com.whipfeng.net.shell.client.proxy.NetShellProxyClient;
import com.whipfeng.net.shell.client.proxy.alone.NetShellAloneClient;
import com.whipfeng.net.shell.server.proxy.NetShellProxyServer;
import com.whipfeng.net.shell.server.proxy.PasswordAuth;
import com.whipfeng.net.shell.server.proxy.alone.NetShellAloneServer;
import com.whipfeng.net.shell.transfer.NetShellTransfer;
import com.whipfeng.net.shell.server.NetShellServer;
import com.whipfeng.net.shell.transfer.proxy.NetShellProxyTransfer;
import com.whipfeng.util.RSAUtil;
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
            public String findPassword(String user) {
                return "migu_log".equals(user) ? "migu_log123!" : null;
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
    public void testNetShellAloneServerWithAuth() throws Exception {


        // 拿到私钥的字节流明文
        RSAUtil.initPrivateKey("MIICdwIBADANBgkqhkiG9w0BAQEFAASCAmEwggJdAgEAAoGBAI13J_ABFKJo2hslLyELqrTSRFxYKHNsHcdau3fjSH44cH2gUa0yD0iHR3zWA8C1nfAHy3VzMYCmkD3RoF6WYZOacAQLY1fmYvR0JpeCWb3OO_AJG1dwcuWmKmXDnp6spJ421gqmhS6SQmLvrElcuo2e3LBZqG73enss5E9REs6bAgMBAAECgYBNXNx6ZBzktHKSZcNgTwEL4fGAjrWQaY_fdb8P0TvTywm5qfMAxF-XSmPN4TwsJyY4HgsmL9jigjRfmkQZdiPlxFgdDF-rQXbftfMkzeRILRQD_xP5Ow1OetuYegDwVNtM9IkryIkj_gOc2nKFhDZT39ix_6wy3nZNIL30xDSgAQJBAMNlpbVjRqUMlm7wBPRxr_mE-UEOjZ6QjUT6Lq5Q5bThx4EYdOZvijJywL2rBBaBllX7l1Z7WyEwjgQ3PueAF4UCQQC5V2MamFsTYRoLBXeTU8g4oKuscz5IsdHr2_Z4x3ou_gaYyFNWkEujL3EQ-xl0bHN4GpH7MF-liZ1zkXteTlefAkEAuEm_X3ORpotXuqwP0EkFcu3SdGR4d2vYgY0AyBGuhMpJct2RK-FZUzldxcVs5pk0JEZVNxDDI2t7FkjQwGDUTQJBAIvN4iCeihytIzLrFc6Y5S_p6nUznOjC0UEjc-CZP44Q6bL3cP2b7KIUWCph7kqGv_b5u-IJFCzPCGizdaamW6sCQBYu3aeNAVNogh6vD3meknP1osNlgPK9L5NTHuh4Lm-JJmm-N-ni_e47k_UaJztYaAppN5HjuuGA9EtMnICvKcI");
        int alPort = 8088;

        PasswordAuth pa = new PasswordAuth() {
            @Override
            public String findPassword(String user) {
                return "migu_log".equals(user) ? "migu_log123!" : null;
            }
        };

        NetShellAloneServer netShellAloneServer = new NetShellAloneServer(alPort, true, pa);
        netShellAloneServer.run();
    }

    @Test
    public void testNetShellAloneClient() throws Exception {
        // 拿到公钥的字节流明文
        RSAUtil.initPublicKey("MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCNdyfwARSiaNobJS8hC6q00kRcWChzbB3HWrt340h-OHB9oFGtMg9Ih0d81gPAtZ3wB8t1czGAppA90aBelmGTmnAEC2NX5mL0dCaXglm9zjvwCRtXcHLlpiplw56erKSeNtYKpoUukkJi76xJXLqNntywWahu93p7LORPURLOmwIDAQAB");
        String alHost = "localhost";
        int alPort = 8088;

        NetShellAloneClient netShellAloneClient = new NetShellAloneClient(alHost, alPort, true, "migu_log", "migu_log123!", 0, 0);
        netShellAloneClient.run();
    }

    @Test
    public void testNetShellProxyTransfer() throws Exception {
        RSAUtil.initPublicKey("MIGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCNdyfwARSiaNobJS8hC6q00kRcWChzbB3HWrt340h-OHB9oFGtMg9Ih0d81gPAtZ3wB8t1czGAppA90aBelmGTmnAEC2NX5mL0dCaXglm9zjvwCRtXcHLlpiplw56erKSeNtYKpoUukkJi76xJXLqNntywWahu93p7LORPURLOmwIDAQAB");
        int tsfPort = 33388;
        String proxyHost = "localhost";
        int proxyPort = 8088;
        String username = "migu_log";
        String password = "migu_log123!";
        String dstHost = "localhost.111.pc";
        int dstPort = 8580;

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
