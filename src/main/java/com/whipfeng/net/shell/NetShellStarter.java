package com.whipfeng.net.shell;

import com.whipfeng.net.shell.client.NetShellClient;
import com.whipfeng.net.shell.proxy.NetShellProxy;
import com.whipfeng.net.shell.server.NetShellServer;
import com.whipfeng.util.ArgsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
         * java -jar net-shell-1.0-SNAPSHOT.jar -m proxy -proxyPort 9099 -outHost 10.19.18.50 -outPort 19666
         * java -jar net-shell-1.0-SNAPSHOT.jar -m server -nsPort 8808 -outPort 9099
         * java -jar net-shell-1.0-SNAPSHOT.jar -m client -nsHost localhost -nsPort 8808 -inHost 10.21.20.229 -inPort 22
         */
        if ("proxy".equals(mode)) {
            int proxyPort = argsUtil.get("-proxyPort", 9099);
            String outHost = argsUtil.get("-outHost", "10.19.18.50");;
            int outPort = argsUtil.get("-outPort", 19666);
            logger.info("proxyPort=" + proxyPort);
            logger.info("outHost=" + outHost);
            logger.info("outPort=" + outPort);
            NetShellProxy netShellProxy = new NetShellProxy(proxyPort, outHost, outPort);
            netShellProxy.run();
        } else if ("server".equals(mode)) {
            int nsPort = argsUtil.get("-nsPort", 8088);
            int outPort = argsUtil.get("-outPort", 9099);
            logger.info("nsPort=" + nsPort);
            logger.info("outPort=" + outPort);
            NetShellServer netShellServer = new NetShellServer(nsPort, outPort);
            netShellServer.run();
        } else {
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
        }
    }
}
