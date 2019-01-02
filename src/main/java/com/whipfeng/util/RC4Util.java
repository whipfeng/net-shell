package com.whipfeng.util;

/**
 * Created by cmll on 2019/1/2.
 */
public class RC4Util {

    private static byte[] initKey(byte[] key) {
        byte state[] = new byte[256];
        for (int i = 0; i < 256; i++) {
            state[i] = (byte) i;
        }
        int idx1 = 0;
        int idx2 = 0;
        for (int i = 0; i < 256; i++) {
            idx2 = ((key[idx1] & 0xff) + (state[i] & 0xff) + idx2) & 0xff;
            byte tmp = state[i];
            state[i] = state[idx2];
            state[idx2] = tmp;
            idx1 = (idx1 + 1) % key.length;
        }
        return state;
    }

    public static byte[] transfer(byte[] input, byte[] key) {
        int x = 0;
        int y = 0;
        int xorIdx;
        byte[] k = initKey(key);
        byte[] output = new byte[input.length];
        for (int i = 0; i < input.length; i++) {
            x = (x + 1) & 0xff;
            y = ((k[x] & 0xff) + y) & 0xff;
            byte tmp = k[x];
            k[x] = k[y];
            k[y] = tmp;
            xorIdx = ((k[x] & 0xff) + (k[y] & 0xff)) & 0xff;
            output[i] = (byte) (input[i] ^ k[xorIdx]);
        }
        return output;
    }
}
