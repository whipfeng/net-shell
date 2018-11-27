package com.whipfeng.net.shell;

import com.whipfeng.net.shell.client.NetShellClient;
import com.whipfeng.net.shell.client.proxy.NetShellProxyClient;
import com.whipfeng.net.shell.server.proxy.NetShellProxyServer;
import com.whipfeng.net.shell.server.proxy.PasswordAuth;
import com.whipfeng.net.shell.transfer.NetShellTransfer;
import com.whipfeng.net.shell.server.NetShellServer;
import com.whipfeng.util.ArgsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.net.InetAddress;
import java.util.HashSet;
import java.util.Set;

/**
 * 网络外壳服务端，监听外部网络和外壳网络
 * Created by fz on 2018/11/19.
 */
public class NetShellStarter {

    private static final Logger logger = LoggerFactory.getLogger(NetShellStarter.class);

    public static void main(String args[]) throws Exception {
        logger.info("");
        logger.info("------------------------我是分隔符------------------------");

        ArgsUtil argsUtil = new ArgsUtil(args);
        String mode = argsUtil.get("-m", "client");
        logger.info("m=" + mode);

        /**
         *
         * java -jar net-shell-1.0-SNAPSHOT.jar -m server -nsPort 8808 -outPort 9099
         * java -jar net-shell-1.0-SNAPSHOT.jar -m client -nsHost localhost -nsPort 8808 -inHost 10.21.20.229 -inPort 22
         * java -jar net-shell-1.0-SNAPSHOT.jar -m proxy.server -nsPort 8808 -outPort 9099 -needAuth true -authFilePath E:\workspace_myself\net-shell\target\AuthList.txt
         * java -jar net-shell-1.0-SNAPSHOT.jar -m proxy.client -nsHost localhost -nsPort 8808 -network.code 0.0.0.0 -sub.mask.code 0.0.0.0
         * java -jar net-shell-1.0-SNAPSHOT.jar -m transfer -tsfPort 9099 -outHost 10.19.18.50 -outPort 19666
         */
        if ("server".equals(mode)) {
            int nsPort = argsUtil.get("-nsPort", 8088);
            int outPort = argsUtil.get("-outPort", 9099);
            logger.info("nsPort=" + nsPort);
            logger.info("outPort=" + outPort);
            NetShellServer netShellServer = new NetShellServer(nsPort, outPort);
            netShellServer.run();
        } else if ("client".equals(mode)) {
            String nsHost = argsUtil.get("-nsHost", "localhost");
            int nsPort = argsUtil.get("-nsPort", 8088);

            String inHost = argsUtil.get("-inHost", "10.21.20.229");
            int inPort = argsUtil.get("-inPort", 22);

            logger.info("nsHost=" + nsHost);
            logger.info("nsPort=" + nsPort);
            logger.info("inHost=" + inHost);
            logger.info("inPort=" + inPort);

            NetShellClient netShellClient = new NetShellClient(nsHost, nsPort, inHost, inPort);
            netShellClient.run();
        } else if ("proxy.server".equals(mode)) {
            int nsPort = argsUtil.get("-nsPort", 8088);
            int outPort = argsUtil.get("-outPort", 9099);

            boolean isNeedAuth = argsUtil.get("-needAuth", false);
            final String authFilePath = argsUtil.get("-authFilePath", null);

            logger.info("nsPort=" + nsPort);
            logger.info("outPort=" + outPort);
            logger.info("isNeedAuth=" + isNeedAuth);
            logger.info("authFilePath=" + authFilePath);

            final Set<String> authSet = new HashSet<String>();
            BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(authFilePath)));
            try {
                String line;
                while (null != (line = br.readLine())) {
                    authSet.add(line);
                }
            } finally {
                br.close();
            }

            PasswordAuth passwordAuth = new PasswordAuth() {
                @Override
                public boolean auth(String user, String password) {
                    String up = user + "/" + password;
                    return authSet.contains(up);
                }
            };

            NetShellProxyServer netShellProxyServer = new NetShellProxyServer(nsPort, outPort, isNeedAuth, passwordAuth);
            netShellProxyServer.run();
        } else if ("proxy.client".equals(mode)) {
            String nsHost = argsUtil.get("-nsHost", "localhost");
            int nsPort = argsUtil.get("-nsPort", 8088);

            String networkCodeStr = argsUtil.get("-network.code", "0.0.0.0");
            String subMaskCodeStr = argsUtil.get("-sub.mask.code", "0.0.0.0");
            int networkCode = ContextRouter.transferAddress(networkCodeStr);
            int subMaskCode = ContextRouter.transferAddress(subMaskCodeStr);

            logger.info("nsHost=" + nsHost);
            logger.info("nsPort=" + nsPort);
            logger.info("networkCode=" + networkCodeStr + "," + networkCode);
            logger.info("subMaskCode=" + subMaskCodeStr + "," + subMaskCode);


            NetShellProxyClient netShellProxyClient = new NetShellProxyClient(nsHost, nsPort, networkCode, subMaskCode);
            netShellProxyClient.run();
        } else {
            int tsfPort = argsUtil.get("-tsfPort", 9099);
            String outHost = argsUtil.get("-outHost", "10.19.18.50");
            ;
            int outPort = argsUtil.get("-outPort", 19666);
            logger.info("tsfPort=" + tsfPort);
            logger.info("outHost=" + outHost);
            logger.info("outPort=" + outPort);
            NetShellTransfer netShellTransfer = new NetShellTransfer(tsfPort, outHost, outPort);
            netShellTransfer.run();
        }
    }
}
