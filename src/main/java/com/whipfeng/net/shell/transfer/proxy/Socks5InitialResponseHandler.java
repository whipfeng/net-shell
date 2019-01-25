package com.whipfeng.net.shell.transfer.proxy;

import com.whipfeng.net.shell.RC4Transfer;
import com.whipfeng.util.RSAUtil;
import com.whipfeng.util.Socks5AddressUtil;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.v5.*;
import io.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.security.SecureRandom;

/**
 * Created by fz on 2018/11/26.
 */
public class Socks5InitialResponseHandler extends SimpleChannelInboundHandler<DefaultSocks5InitialResponse> {

    private static final Logger logger = LoggerFactory.getLogger(Socks5InitialResponseHandler.class);

    private String username;
    private byte[] unBuf;
    private String password;
    private byte[] pwBuf;
    private String dstHost;
    private byte[] dhBuf;
    private int dstPort;

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DefaultSocks5InitialResponse initMsg) throws Exception {
        logger.info("Init SOCKS5:" + initMsg);
        if (initMsg.decoderResult().isSuccess()) {
            if (Socks5AuthMethod.PASSWORD.equals(initMsg.authMethod())) {
                Socks5PasswordAuthRequest authMsg = new DefaultSocks5PasswordAuthRequest(username, password);
                ctx.writeAndFlush(authMsg);
            } else if (Socks5AuthMethod.GSSAPI.equals(initMsg.authMethod())) {
                //缓冲写出
                ByteArrayOutputStream bufOs = new ByteArrayOutputStream();
                DataOutputStream dOs = new DataOutputStream(bufOs);
                SecureRandom random = new SecureRandom();
                int numBytes = 128 + random.nextInt(128);
                byte[] secretKey = random.generateSeed(numBytes);
                dOs.writeInt(secretKey.length);//加密密码
                dOs.write(secretKey);
                dOs.writeInt(unBuf.length);//用户名
                dOs.write(unBuf);
                dOs.writeInt(pwBuf.length);//授权密码
                dOs.write(pwBuf);
                dOs.writeInt(dhBuf.length);//目标主机
                dOs.write(dhBuf);
                dOs.flush();
                //加密寫出
                String secretInfo = new String(RSAUtil.publicEncrypt(bufOs.toByteArray()), CharsetUtil.ISO_8859_1);
                Socks5CommandRequest cmdMsg = new DefaultSocks5CommandRequest(Socks5CommandType.CONNECT, Socks5AddressUtil.FOCUS, secretInfo, dstPort);
                ctx.writeAndFlush(cmdMsg);
                ctx.pipeline().remove(Socks5PasswordAuthResponseDecoder.class);
                RC4Transfer rc4Codec = new RC4Transfer(secretKey);
                for (ChannelHandler ioHandler : rc4Codec.getIOHandlers()) {
                    ctx.pipeline().addBefore("M-E-H", null, ioHandler);
                }
            } else {
                Socks5CommandRequest cmdMsg = new DefaultSocks5CommandRequest(Socks5CommandType.CONNECT, Socks5AddressType.DOMAIN, dstHost, dstPort);
                ctx.writeAndFlush(cmdMsg);
                ctx.pipeline().remove(Socks5PasswordAuthResponseDecoder.class);
            }
            return;
        }
        ctx.close();
    }

    public Socks5InitialResponseHandler(String username, String password, String dstHost, int dstPort) {
        this.username = username;
        this.unBuf = username.getBytes(CharsetUtil.UTF_8);
        this.password = password;
        this.pwBuf = password.getBytes(CharsetUtil.UTF_8);
        this.dstHost = dstHost;
        this.dhBuf = dstHost.getBytes(CharsetUtil.UTF_8);
        this.dstPort = dstPort;
    }
}
