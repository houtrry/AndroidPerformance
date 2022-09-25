package com.houtrry.androidperformancemodule.io.closeguard;

import android.util.Log;

import java.lang.reflect.Field;
import java.lang.reflect.Proxy;

/**
 * @author: houtrry
 * @time: 2022/9/3
 * @desc:
 */

class CloseGuardHelper {

    private static final String TAG = "CloseGuardHelper";

    private static final String PACKAGE_NAME_CLOSE_GUARD = "dalvik.system.CloseGuard";
    private static final String PACKAGE_NAME_CLOSE_GUARD_REPOTER = "dalvik.system.CloseGuard$Reporter";
    private static final String FIELD_CLOSE_GUARD_ENABLE = "ENABLE";
    private static final String FIELD_CLOSE_GUARD_REPORTER = "REPORTER";
    private static Object sOriginRepoter = null;

    private static OnCloseGuardListener sDefaultCloseGuardListener = new OnCloseGuardListener() {
        @Override
        public void onReport(String message, Throwable throwable) {
            Log.d(TAG, "close repoter: " + message + ", " + throwable.getMessage());
            for (StackTraceElement stackTraceElement : throwable.getStackTrace()) {
                Log.d(TAG, " " + stackTraceElement);
            }
        }
    };

    public static void setCloseGuardListener(OnCloseGuardListener closeGuardListener) {
        sDefaultCloseGuardListener = closeGuardListener;
    }

    public static boolean tryHook() {
        try {
            Class clsCloseGuard = Class.forName(PACKAGE_NAME_CLOSE_GUARD);
            Field fieldEnable = clsCloseGuard.getDeclaredField(FIELD_CLOSE_GUARD_ENABLE);
            fieldEnable.setAccessible(true);
            fieldEnable.set(null, true);
            Field fieldReporter = clsCloseGuard.getDeclaredField(FIELD_CLOSE_GUARD_REPORTER);
            fieldReporter.setAccessible(true);
            sOriginRepoter = fieldReporter.get(null);
            Class clsCloseGuardRepoter = Class.forName(PACKAGE_NAME_CLOSE_GUARD_REPOTER);
            Object newRepoter = Proxy.newProxyInstance(clsCloseGuard.getClassLoader(), new Class[]{clsCloseGuardRepoter}, new CloseGuardInvocationHandler(sOriginRepoter, sDefaultCloseGuardListener));
            fieldReporter.set(null, newRepoter);
            return true;
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static boolean tryUnHook() {
        try {
            Class clsCloseGuard = Class.forName(PACKAGE_NAME_CLOSE_GUARD);
            Field fieldEnable = clsCloseGuard.getDeclaredField(FIELD_CLOSE_GUARD_ENABLE);
            fieldEnable.setAccessible(true);
            fieldEnable.set(null, false);
            Field fieldReporter = clsCloseGuard.getDeclaredField(FIELD_CLOSE_GUARD_REPORTER);
            fieldReporter.setAccessible(true);
            fieldReporter.set(null, sOriginRepoter);
            return true;
        } catch (ClassNotFoundException | NoSuchFieldException | IllegalAccessException e) {
            e.printStackTrace();
        }
        return false;
    }
}
