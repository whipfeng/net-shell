package com.whipfeng.net;

import com.whipfeng.util.RSAUtil;
import org.junit.Test;

import java.util.Map;

/**
 * Created by cmll on 2019/1/2.
 */
public class RSAUtilTest {
    @Test
    public void testRSAUtil() throws Exception {
        //设置密钥长度
        Map<String, String> keys = RSAUtil.createKeys(1024);

        // 拿到公钥的字符串明文
        String publicKey = keys.get("publicKey");

        // 拿到私钥的字符串明文
        String privateKey = keys.get("privateKey");

        System.out.println("公钥：" + publicKey);
        System.out.println("私钥：" + privateKey);

        // 拿到公钥的字节流明文
        RSAUtil.initPublicKey(publicKey);

        // 拿到私钥的字节流明文
        RSAUtil.initPrivateKey(privateKey);

        // 公钥加密：第一个值是要传进去的值，第二个值传公钥的字节流明文
        byte[] data;
        data = RSAUtil.publicEncrypt("123 ".getBytes("UTF-8"));
        System.out.println("公钥加密的字符串：" + new String(data, "UTF-8"));

        // 私钥解密：第一个值传公钥加密后的对象，第二个值传私钥的字节流明文
        data = RSAUtil.privateDecrypt(data);
        System.out.println("私钥解密的字符：" + new String(data, "UTF-8"));

        // 私钥加密：第一个值是要传进去的值，第二个值传私钥的字节流明文
        data = RSAUtil.privateEncrypt("456".getBytes("UTF-8"));
        System.out.println("私钥加密的字符串：" + new String(data, "UTF-8"));

        // 公钥解密：第一个值传私钥加密后的对象，第二个值传公钥的字节流明文
        data = RSAUtil.publicDecrypt(data);
        System.out.println("公钥解密的字符：" + new String(data, "UTF-8"));
    }
}
