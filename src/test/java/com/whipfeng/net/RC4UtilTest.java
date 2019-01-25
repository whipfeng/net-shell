package com.whipfeng.net;

import com.whipfeng.util.RC4Util;
import org.junit.Test;

import java.security.SecureRandom;

/**
 * Created by cmll on 2019/1/2.
 */
public class RC4UtilTest {
    @Test
    public void testRC4Util() throws Exception {
        SecureRandom random = new SecureRandom();
        int numBytes = 128 + random.nextInt(128);
        byte[] key = random.generateSeed(numBytes);
        byte[] data;
       /* data = RC4Util.transfer("哈哈哈，你好啊".getBytes(CharsetUtil.UTF_8), key);
        System.out.println("加密后：" + new String(data, CharsetUtil.UTF_8));
        data = RC4Util.transfer(data, key);
        System.out.println("解密后：" +  new String(data, CharsetUtil.UTF_8));*/
    }
}
