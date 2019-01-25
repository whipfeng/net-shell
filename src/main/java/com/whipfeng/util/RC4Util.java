package com.whipfeng.util;

import io.netty.buffer.ByteBuf;

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

    public static void transfer(byte[] key, ByteBuf buf) {
        transfer(key, buf, buf.readerIndex(), buf.writerIndex());
    }

    public static void transfer(byte[] key, ByteBuf buf, int beginIdx, int endIdx) {
        int x = 0;
        int y = 0;
        int xorIdx;
        byte[] k = initKey(key);
        for (int i = beginIdx; i < endIdx; i++) {
            x = (x + 1) & 0xff;
            y = ((k[x] & 0xff) + y) & 0xff;
            byte tmp = k[x];
            k[x] = k[y];
            k[y] = tmp;
            xorIdx = ((k[x] & 0xff) + (k[y] & 0xff)) & 0xff;
            buf.setByte(i, buf.getByte(i) ^ k[xorIdx]);
        }
    }

    public static void transfer(byte[] key, byte[] buf) {
        transfer(key, buf, 0, buf.length);
    }

    public static void transfer(byte[] key, byte[] buf, int beginIdx, int endIdx) {
        int x = 0;
        int y = 0;
        int xorIdx;
        byte[] k = initKey(key);
        for (int i = beginIdx; i < endIdx; i++) {
            x = (x + 1) & 0xff;
            y = ((k[x] & 0xff) + y) & 0xff;
            byte tmp = k[x];
            k[x] = k[y];
            k[y] = tmp;
            xorIdx = ((k[x] & 0xff) + (k[y] & 0xff)) & 0xff;
            buf[i] = (byte) (buf[i] ^ k[xorIdx]);
        }
    }

    private int x = 0;
    private int y = 0;
    private byte[] k;

    public RC4Util(byte[] key) {
        k = initKey(key);
    }

    public void transfer(ByteBuf buf) {
        int xorIdx;
        for (int i = buf.readerIndex(); i < buf.writerIndex(); i++) {
            x = (x + 1) & 0xff;
            y = ((k[x] & 0xff) + y) & 0xff;
            byte tmp = k[x];
            k[x] = k[y];
            k[y] = tmp;
            xorIdx = ((k[x] & 0xff) + (k[y] & 0xff)) & 0xff;
            buf.setByte(i, buf.getByte(i) ^ k[xorIdx]);
        }
    }
}
