package com.sheridan.gcr.modularSys;

import it.unimi.dsi.fastutil.chars.Char2IntArrayMap;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.UUID;

public class IDGenerator {
    private static final char[] BASE62 = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz".toCharArray();
    private static final Char2IntArrayMap BASE62_INDEX_MAP = new Char2IntArrayMap();

    static {
        for (int i = 0; i < BASE62.length; i++) {
            BASE62_INDEX_MAP.put(BASE62[i], i);
        }
    }

    public static String genID(String input, int length) {
        byte[] hash = sha256(input);
        return base62Encode(hash, length);
    }

    private static byte[] sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return digest.digest(input.getBytes(StandardCharsets.UTF_8));
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private static String base62Encode(byte[] bytes, int length) {
        long value = 0;
        for (int i = 0; i < Math.min(8, bytes.length); i++) {
            value = (value << 8) | (bytes[i] & 0xFF);
        }

        value = value & 0x7FFFFFFFFFFFFFFFL;

        char[] result = new char[length];
        for (int i = length - 1; i >= 0; i--) {
            result[i] = BASE62[(int)(value % 62)];
            value /= 62;
        }
        return new String(result);
    }

    /**
     * String长度 <= 9，该方法获取不碰撞的唯一id值
     * */
    public static long base62ToLong(String s) {
        long value = 0;
        for (int i = 0; i < s.length(); i++) {
            value = value * 62 + BASE62_INDEX_MAP.get(s.charAt(i));
        }
        return value;
    }

    public static String randomId() {
        return UUID.randomUUID().toString().replace("-", "");
    }
}
