package com.sang.musicnpc.util;

import net.minecraftforge.api.distmarker.Dist;
import net.minecraftforge.fml.loading.FMLEnvironment;

import java.lang.reflect.Method;

public final class DistSafeClient {
    private DistSafeClient() {}

    /** 서버에서는 아무것도 안 함. 클라일 때만 실행 */
    public static void invoke(String className, String methodName, Class<?>[] paramTypes, Object... args) {
        if (FMLEnvironment.dist != Dist.CLIENT) return;

        try {
            Class<?> clazz = Class.forName(className);
            Method method = clazz.getDeclaredMethod(methodName, paramTypes);
            method.setAccessible(true);
            method.invoke(null, args);
        } catch (Throwable t) {
            // 여기서 로그 찍고 싶으면 MusicNpc.LOGGER 써도 되는데,
            // 공용 유틸이라 의존 줄이려고 println 처리해둠
            System.err.println("[musicnpc] Client invoke failed: " + className + "#" + methodName);
            t.printStackTrace();
        }
    }
}
