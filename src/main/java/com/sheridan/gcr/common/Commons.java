package com.sheridan.gcr.common;

import net.neoforged.neoforge.event.server.ServerStartedEvent;

public class Commons {

    /**
     * 只在 Server Thread 可见
     */
    private static final ThreadLocal<Long> SERVER_START_TIME = ThreadLocal.withInitial(() -> 0L);

    /**
     * 服务器启动时调用（保证在 server thread）
     */
    public static void onServerStarted(ServerStartedEvent event) {
        SERVER_START_TIME.set(System.currentTimeMillis());
    }

    /**
     * 获取服务器启动时间
     * - Server Thread：返回真实值
     * - Client / 其他线程：返回 0
     */
    public static long getServerStartTime() {
        return SERVER_START_TIME.get();
    }

    /**
     * 是否在 Server Thread（有值说明是）
     */
    public static boolean isServerThread() {
        return SERVER_START_TIME.get() != 0L;
    }

    /**
     * 获取运行时长（ms）
     */
    public static long getServerUptime() {
        long start = SERVER_START_TIME.get();
        return start == 0L ? 0L : (System.currentTimeMillis() - start);
    }
}