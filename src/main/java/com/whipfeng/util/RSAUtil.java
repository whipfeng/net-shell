package com.whipfeng.util;

import org.apache.commons.codec.binary.Base64;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import java.io.ByteArrayOutputStream;
import java.security.*;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;

/**
 * Created by cmll on 2018/12/30.
 */
public class RSAUtil {

    private static RSAPublicKey PUBLIC_KEY;

    private static RSAPrivateKey PRIVATE_KEY;

    public static void initPublicKey(String publicKeyStr) throws InvalidKeySpecException, NoSuchAlgorithmException {
        PUBLIC_KEY = RSAUtil.getPublicKey(publicKeyStr);
    }

    public static void initPrivateKey(String privateKeyStr) throws InvalidKeySpecException, NoSuchAlgorithmException {
        PRIVATE_KEY = RSAUtil.getPrivateKey(privateKeyStr);
    }

    public static boolean noPublicKey() {
        return null == PUBLIC_KEY;
    }

    public static boolean noPrivateKey() {
        return null == PRIVATE_KEY;
    }


    private static final String RSA_ALGORITHM = "RSA";

    public static Map<String, String> createKeys(int keySize) throws NoSuchAlgorithmException {
        //为RSA算法创建一个KeyPairGenerator对象
        KeyPairGenerator kpg = KeyPairGenerator.getInstance(RSA_ALGORITHM);
        //初始化KeyPairGenerator对象,密钥长度
        kpg.initialize(keySize);
        //生成密匙对
        KeyPair keyPair = kpg.generateKeyPair();
        //得到公钥
        Key publicKey = keyPair.getPublic();
        String publicKeyStr = Base64.encodeBase64URLSafeString(publicKey.getEncoded());
        //得到私钥
        Key privateKey = keyPair.getPrivate();
        String privateKeyStr = Base64.encodeBase64URLSafeString(privateKey.getEncoded());
        Map<String, String> keyPairMap = new HashMap<String, String>();
        keyPairMap.put("publicKey", publicKeyStr);
        keyPairMap.put("privateKey", privateKeyStr);
        return keyPairMap;
    }


    private static RSAPublicKey getPublicKey(String publicKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        //通过X509编码的Key指令获得公钥对象
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
        X509EncodedKeySpec x509KeySpec = new X509EncodedKeySpec(Base64.decodeBase64(publicKey));
        RSAPublicKey key = (RSAPublicKey) keyFactory.generatePublic(x509KeySpec);
        return key;
    }


    private static RSAPrivateKey getPrivateKey(String privateKey) throws NoSuchAlgorithmException, InvalidKeySpecException {
        //通过PKCS#8编码的Key指令获得私钥对象
        KeyFactory keyFactory = KeyFactory.getInstance(RSA_ALGORITHM);
        PKCS8EncodedKeySpec pkcs8KeySpec = new PKCS8EncodedKeySpec(Base64.decodeBase64(privateKey));
        RSAPrivateKey key = (RSAPrivateKey) keyFactory.generatePrivate(pkcs8KeySpec);
        return key;
    }

    public static byte[] publicEncrypt(byte[] data) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, PUBLIC_KEY);
        return rsaSplitCodec(cipher, Cipher.ENCRYPT_MODE, data, PUBLIC_KEY.getModulus().bitLength());
    }

    public static byte[] privateDecrypt(byte[] data) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, PRIVATE_KEY);
        return rsaSplitCodec(cipher, Cipher.DECRYPT_MODE, data, PRIVATE_KEY.getModulus().bitLength());

    }

    public static byte[] privateEncrypt(byte[] data) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
        cipher.init(Cipher.ENCRYPT_MODE, PRIVATE_KEY);
        return rsaSplitCodec(cipher, Cipher.ENCRYPT_MODE, data, PRIVATE_KEY.getModulus().bitLength());
    }

    public static byte[] publicDecrypt(byte[] data) throws NoSuchPaddingException, NoSuchAlgorithmException, InvalidKeyException, BadPaddingException, IllegalBlockSizeException {
        Cipher cipher = Cipher.getInstance(RSA_ALGORITHM);
        cipher.init(Cipher.DECRYPT_MODE, PUBLIC_KEY);
        return rsaSplitCodec(cipher, Cipher.DECRYPT_MODE, data, PUBLIC_KEY.getModulus().bitLength());

    }

    private static byte[] rsaSplitCodec(Cipher cipher, int opMode, byte[] data, int keySize) throws BadPaddingException, IllegalBlockSizeException {
        int maxBlock;
        if (opMode == Cipher.DECRYPT_MODE) {
            maxBlock = keySize / 8;
        } else {
            maxBlock = keySize / 8 - 11;
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        int offSet = 0;
        byte[] buf;
        int i = 0;
        while (data.length > offSet) {
            if (data.length - offSet > maxBlock) {
                buf = cipher.doFinal(data, offSet, maxBlock);
            } else {
                buf = cipher.doFinal(data, offSet, data.length - offSet);
            }
            out.write(buf, 0, buf.length);
            i++;
            offSet = i * maxBlock;
        }
        return out.toByteArray();
    }
}
