package com.whipfeng.util;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.DecoderException;
import io.netty.handler.codec.EncoderException;
import io.netty.handler.codec.socksx.v5.Socks5AddressDecoder;
import io.netty.handler.codec.socksx.v5.Socks5AddressEncoder;
import io.netty.handler.codec.socksx.v5.Socks5AddressType;
import io.netty.util.CharsetUtil;
import io.netty.util.NetUtil;

/**
 * Created by cmll on 2019/1/24.
 */
public class Socks5AddressUtil {

    public static final Socks5AddressType FOCUS = new Socks5AddressType(0x05, "FOCUS");

    public static Socks5AddressEncoder DEFAULT_ENCODER = new Socks5AddressEncoder() {
        @Override
        public void encodeAddress(Socks5AddressType addrType, String addrValue, ByteBuf out) throws Exception {
            final byte typeVal = addrType.byteValue();
            if (typeVal == Socks5AddressType.IPv4.byteValue()) {
                if (addrValue != null) {
                    out.writeBytes(NetUtil.createByteArrayFromIpAddressString(addrValue));
                } else {
                    out.writeInt(0);
                }
            } else if (typeVal == Socks5AddressType.DOMAIN.byteValue()) {
                if (addrValue != null) {
                    out.writeByte(addrValue.length());
                    out.writeCharSequence(addrValue, CharsetUtil.US_ASCII);
                } else {
                    out.writeByte(0);
                }
            } else if (typeVal == FOCUS.byteValue()) {
                byte[] buf = addrValue.getBytes(CharsetUtil.ISO_8859_1);
                out.writeInt(buf.length);
                out.writeBytes(buf);
            } else if (typeVal == Socks5AddressType.IPv6.byteValue()) {
                if (addrValue != null) {
                    out.writeBytes(NetUtil.createByteArrayFromIpAddressString(addrValue));
                } else {
                    out.writeLong(0);
                    out.writeLong(0);
                }
            } else {
                throw new EncoderException("unsupported addrType: " + (addrType.byteValue() & 0xFF));
            }
        }
    };

    public static Socks5AddressDecoder DEFAULT_DECODER = new Socks5AddressDecoder() {

        private static final int IPv6_LEN = 16;

        @Override
        public String decodeAddress(Socks5AddressType addrType, ByteBuf in) throws Exception {
            if (addrType == Socks5AddressType.IPv4) {
                return NetUtil.intToIpAddress(in.readInt());
            }
            if (addrType == Socks5AddressType.DOMAIN) {
                final int length = in.readUnsignedByte();
                final String domain = in.toString(in.readerIndex(), length, CharsetUtil.US_ASCII);
                in.skipBytes(length);
                return domain;
            }
            if (FOCUS.equals(addrType)) {
                byte[] buf = new byte[in.readInt()];
                in.readBytes(buf);
                return new String(buf, CharsetUtil.ISO_8859_1);
            }
            if (addrType == Socks5AddressType.IPv6) {
                if (in.hasArray()) {
                    final int readerIdx = in.readerIndex();
                    in.readerIndex(readerIdx + IPv6_LEN);
                    return NetUtil.bytesToIpAddress(in.array(), in.arrayOffset() + readerIdx, IPv6_LEN);
                } else {
                    byte[] tmp = new byte[IPv6_LEN];
                    in.readBytes(tmp);
                    return NetUtil.bytesToIpAddress(tmp);
                }
            } else {
                throw new DecoderException("unsupported address type: " + (addrType.byteValue() & 0xFF));
            }
        }
    };
}
