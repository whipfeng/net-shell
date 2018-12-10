package com.whipfeng.net.shell;

import io.netty.channel.ChannelHandlerContext;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;

import java.util.StringTokenizer;

/**
 * 路由信息，储存网络号和子网掩码
 * Created by fz on 2018/11/27.
 */
public class ContextRouter {

    private int networkCode;
    private int subMaskCode;

    private Socks5CommandRequest commandRequest;
    private int ipv4;

    ChannelHandlerContext ctx;

    public ContextRouter(ChannelHandlerContext ctx, int networkCode, int subMaskCode) {
        this.networkCode = networkCode;
        this.subMaskCode = subMaskCode;
        this.ctx = ctx;
    }

    public ContextRouter(ChannelHandlerContext ctx, Socks5CommandRequest commandRequest) {
        this.commandRequest = commandRequest;
        this.ctx = ctx;
        try {
            ipv4 = transferAddress(commandRequest.dstAddr());
        } catch (NumberFormatException e) {
        }
    }

    public static int transferAddress(String address) {
        StringTokenizer tokenizer = new StringTokenizer(address, ".");
        int intAddress = 0;
        while (tokenizer.hasMoreTokens()) {
            intAddress = (intAddress << 8) | Integer.parseInt(tokenizer.nextToken());
        }
        return intAddress;
    }

    public static boolean isMatchFrom(ContextRouter cur, ContextRouter comp) {
        return (comp.ipv4 & cur.subMaskCode) == cur.networkCode;
    }

    public static boolean isMatchTo(ContextRouter cur, ContextRouter comp) {
        return isMatchFrom(comp, cur);
    }

    public int getNetworkCode() {
        return networkCode;
    }

    public int getSubMaskCode() {
        return subMaskCode;
    }

    public ChannelHandlerContext getCtx() {
        return ctx;
    }

    public Socks5CommandRequest getCommandRequest() {
        return commandRequest;
    }

    public int getIpv4() {
        return ipv4;
    }
}
