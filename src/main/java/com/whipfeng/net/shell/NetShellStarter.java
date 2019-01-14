package com.whipfeng.net.shell;

import com.whipfeng.net.shell.client.NetShellClient;
import com.whipfeng.net.shell.client.proxy.NetShellProxyClient;
import com.whipfeng.net.shell.client.proxy.alone.NetShellAloneClient;
import com.whipfeng.net.shell.server.proxy.NetShellProxyServer;
import com.whipfeng.net.shell.server.proxy.PasswordAuth;
import com.whipfeng.net.shell.server.proxy.alone.NetShellAloneServer;
import com.whipfeng.net.shell.transfer.NetShellTransfer;
import com.whipfeng.net.shell.server.NetShellServer;
import com.whipfeng.net.shell.transfer.proxy.NetShellProxyTransfer;
import com.whipfeng.util.ArgsUtil;
import com.whipfeng.util.RSAUtil;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.monitor.FileAlterationListenerAdaptor;
import org.apache.commons.io.monitor.FileAlterationMonitor;
import org.apache.commons.io.monitor.FileAlterationObserver;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

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

        String publicKey = argsUtil.get("-public.key", null);
        if (null != publicKey) {
            logger.info("public.key=" + publicKey);
            RSAUtil.initPublicKey(publicKey);
        }
        String privateKey = argsUtil.get("-private.key", null);
        if (null != privateKey) {
            logger.info("private.key=" + privateKey);
            RSAUtil.initPrivateKey(privateKey);
        }

        /**
         *
         * java -jar net-shell-1.0-SNAPSHOT.jar -m server -nsPort 8808 -outPort 9099
         * java -jar net-shell-1.0-SNAPSHOT.jar -m client -nsHost localhost -nsPort 8808 -inHost 10.21.20.229 -inPort 22
         * java -jar net-shell-1.0-SNAPSHOT.jar -m proxy.server -nsPort 8808 -outPort 9099 -needAuth true -authFilePath E:\workspace_myself\net-shell\target\AuthList.txt
         * java -jar net-shell-1.0-SNAPSHOT.jar -m proxy.client -nsHost localhost -nsPort 8808 -network.code 0.0.0.0 -sub.mask.code 0.0.0.0
         * java -jar net-shell-1.0-SNAPSHOT.jar -m alone.server -alPort 8808 -needAuth true -authFilePath E:\workspace_myself\net-shell\target\AuthList.txt -matchFilePath E:\xxx.txt
         * java -jar net-shell-1.0-SNAPSHOT.jar -m alone.client -alHost localhost -alPort 8808 -username xxx -password xxx -network.code 0.0.0.0 -sub.mask.code 0.0.0.0
         * java -jar net-shell-1.0-SNAPSHOT.jar -m transfer -tsfPort 9099 -dstHost 10.21.20.229 -dstPort 22
         * java -jar net-shell-1.0-SNAPSHOT.jar -m proxy.transfer -tsfPort 9099 -proxyHost localhost -proxyPort 8000 -username xxx -password xxx -dstHost 10.21.20.229 -dstPort 9666
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

            String inHost = argsUtil.get("-inHost", "xx.xx.xx.xx");
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
            String authFilePath = argsUtil.get("-authFilePath", null);
            String matchFilePath = argsUtil.get("-matchFilePath", null);

            logger.info("nsPort=" + nsPort);
            logger.info("outPort=" + outPort);
            logger.info("isNeedAuth=" + isNeedAuth);
            logger.info("authFilePath=" + authFilePath);
            logger.info("matchFilePath=" + matchFilePath);

            AnyTimerTask anyTimerTask = new AnyTimerTask(authFilePath, matchFilePath);
            ContextRouter.setMatcher(anyTimerTask);
            NetShellProxyServer netShellProxyServer = new NetShellProxyServer(nsPort, outPort, isNeedAuth, anyTimerTask);
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
        } else if ("alone.server".equals(mode)) {
            int alPort = argsUtil.get("-alPort", 8088);

            boolean isNeedAuth = argsUtil.get("-needAuth", false);
            String authFilePath = argsUtil.get("-authFilePath", null);
            String matchFilePath = argsUtil.get("-matchFilePath", null);

            logger.info("alPort=" + alPort);
            logger.info("isNeedAuth=" + isNeedAuth);
            logger.info("authFilePath=" + authFilePath);
            logger.info("matchFilePath=" + matchFilePath);

            AnyTimerTask anyTimerTask = new AnyTimerTask(authFilePath, matchFilePath);
            ContextRouter.setMatcher(anyTimerTask);
            NetShellAloneServer netShellAloneServer = new NetShellAloneServer(alPort, isNeedAuth, anyTimerTask);
            netShellAloneServer.run();
        } else if ("alone.client".equals(mode)) {
            String alHost = argsUtil.get("-alHost", "localhost");
            int alPort = argsUtil.get("-alPort", 8088);

            boolean isNeedAuth = argsUtil.get("-needAuth", false);
            String username = argsUtil.get("-username", "xxx");
            String password = argsUtil.get("-password", "xxx");
            String networkCodeStr = argsUtil.get("-network.code", "0.0.0.0");
            String subMaskCodeStr = argsUtil.get("-sub.mask.code", "0.0.0.0");
            int networkCode = ContextRouter.transferAddress(networkCodeStr);
            int subMaskCode = ContextRouter.transferAddress(subMaskCodeStr);

            logger.info("alHost=" + alHost);
            logger.info("alPort=" + alPort);
            logger.info("networkCode=" + networkCodeStr + "," + networkCode);
            logger.info("subMaskCode=" + subMaskCodeStr + "," + subMaskCode);

            NetShellAloneClient netShellAloneClient = new NetShellAloneClient(alHost, alPort, isNeedAuth, username, password, networkCode, subMaskCode);
            netShellAloneClient.run();
        } else if ("proxy.transfer".equals(mode)) {
            int tsfPort = argsUtil.get("-tsfPort", 9099);
            String proxyHost = argsUtil.get("-proxyHost", "xx.xx.xx.xx");
            int proxyPort = argsUtil.get("-proxyPort", 9666);
            String username = argsUtil.get("-username", "xxx");
            String password = argsUtil.get("-password", "xxx");
            String dstHost = argsUtil.get("-dstHost", "xx.xx.xx.xx");
            int dstPort = argsUtil.get("-dstPort", 9666);
            logger.info("tsfPort=" + tsfPort);
            logger.info("proxyHost=" + proxyHost);
            logger.info("proxyPort=" + proxyPort);
            logger.info("username=" + username);
            logger.info("password=" + password);
            logger.info("dstHost=" + dstHost);
            logger.info("dstPort=" + dstPort);
            NetShellProxyTransfer netShellProxyTransfer = new NetShellProxyTransfer(tsfPort, proxyHost, proxyPort, username, password, dstHost, dstPort);
            netShellProxyTransfer.run();
        } else {
            int tsfPort = argsUtil.get("-tsfPort", 9099);
            String dstHost = argsUtil.get("-dstHost", "xx.xx.xx.xx");
            int dstPort = argsUtil.get("-dstPort", 9666);
            logger.info("tsfPort=" + tsfPort);
            logger.info("dstHost=" + dstHost);
            logger.info("dstPort=" + dstPort);
            NetShellTransfer netShellTransfer = new NetShellTransfer(tsfPort, dstHost, dstPort);
            netShellTransfer.run();
        }
    }

    private static class AnyTimerTask extends FileAlterationListenerAdaptor implements PasswordAuth, ContextRouter.Matcher {

        private File authFile;
        private File matchFile;

        private AnyTimerTask(final String authFilePath, String matchFilePath) throws Exception {
            this.authFile = new File(authFilePath);
            this.matchFile = new File(matchFilePath);
            refresh();
            // 每隔1000毫秒扫描一次
            FileAlterationMonitor monitor = new FileAlterationMonitor(1000L);
            FileAlterationObserver authObserver = new FileAlterationObserver(this.authFile.getParentFile(), new FileFileFilter() {
                public boolean accept(File file) {
                    return file.isFile() && file.equals(authFile);
                }
            });
            authObserver.addListener(this);
            monitor.addObserver(authObserver);
            FileAlterationObserver matchObserver = new FileAlterationObserver(this.matchFile.getParentFile(), new FileFileFilter() {
                public boolean accept(File file) {
                    return file.isFile() && file.equals(matchFile);
                }
            });
            matchObserver.addListener(this);
            monitor.addObserver(matchObserver);
            monitor.start();
        }

        @Override
        public void onFileCreate(File file) {
            this.onFileChange(file);
            super.onFileCreate(file);
        }

        @Override
        public void onFileChange(File file) {
            if (file.equals(this.authFile)) {
                refreshAuth();
            }
            if (file.equals(this.matchFile)) {
                refreshMatch();
            }
            super.onFileChange(file);
        }

        @Override
        public void onFileDelete(File file) {
            this.onFileChange(file);
            super.onFileDelete(file);
        }


        private void refresh() {
            refreshAuth();
            refreshMatch();
        }

        private Map<String, String> passwordMap = new HashMap<String, String>();

        @Override
        public String findPassword(String user) throws Exception {
            return passwordMap.get(user);
        }

        private void refreshAuth() {
            logger.info("----------------Begin refresh auth----------------");
            Map<String, String> pwMap = new HashMap<String, String>();
            BufferedReader br = null;
            try {
                br = new BufferedReader(new InputStreamReader(new FileInputStream(authFile)));
                String line;
                while (null != (line = br.readLine())) {
                    String[] lineSplit = line.split("/", -1);
                    pwMap.put(lineSplit[0], lineSplit[1]);
                }
                passwordMap = pwMap;
            } catch (Exception e) {
                logger.error("Unread any user info." + authFile, e);
            } finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e) {
                    }
                }
            }
            logger.info("-----------------End refresh auth-----------------");
        }

        @Override
        public String match(String address) {
            List<MatchEntity> meList = matchEntityList;
            for (MatchEntity me : meList) {
                if (me.pattern.matcher(address).matches()) {
                    return me.address;
                }
            }
            return null;
        }

        List<MatchEntity> matchEntityList = new ArrayList<MatchEntity>();

        private class MatchEntity {
            private Pattern pattern;
            private String address;

            public MatchEntity(String address, Pattern pattern) {
                this.pattern = pattern;
                this.address = address;
            }
        }

        private void refreshMatch() {
            logger.info("----------------Begin refresh match----------------");
            List<MatchEntity> meList = new ArrayList<MatchEntity>();
            BufferedReader br = null;
            try {
                br = new BufferedReader(new InputStreamReader(new FileInputStream(matchFile)));
                String line;
                while (null != (line = br.readLine())) {
                    String[] lineSplit = line.split(" ", -1);
                    meList.add(new MatchEntity(lineSplit[0], Pattern.compile(lineSplit[1])));
                }
                matchEntityList = meList;
            } catch (Exception e) {
                logger.error("Unread any match info." + matchFile, e);
            } finally {
                if (br != null) {
                    try {
                        br.close();
                    } catch (IOException e) {
                    }
                }
            }
            logger.info("----------------End refresh match----------------");
        }
    }
}
