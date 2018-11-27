package com.whipfeng.net.shell.server.proxy;

/**
 * 密码认证方式
 * Created by fz on 2018/11/26.
 */
public interface PasswordAuth {
    boolean auth(String user, String password);
}
