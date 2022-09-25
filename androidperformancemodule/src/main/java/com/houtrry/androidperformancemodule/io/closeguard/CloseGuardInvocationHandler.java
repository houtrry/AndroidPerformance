package com.houtrry.androidperformancemodule.io.closeguard;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * @author: houtrry
 * @time: 2022/9/3
 * @desc:
 */

class CloseGuardInvocationHandler implements InvocationHandler {

    private Object mTargetCloseGuard = null;
    private OnCloseGuardListener mOnCloseGuardListener;

    public CloseGuardInvocationHandler(Object targetCloseGuard, OnCloseGuardListener onCloseGuardListener) {
        mTargetCloseGuard = targetCloseGuard;
        mOnCloseGuardListener = onCloseGuardListener;
    }


    @Override
    public Object invoke(Object proxy, Method method, Object[] args) {
        Object result = null;
        handleArgs(args);
        try {
            result = method.invoke(mTargetCloseGuard, args);
        } catch (IllegalAccessException | InvocationTargetException e) {
            e.printStackTrace();
        }
        return result;
    }

    private void handleArgs(Object[] args) {
        if (mOnCloseGuardListener == null) {
            return;
        }
        if (args == null || args.length < 2) {
            return;
        }
        Object arg0 = args[0];
        Object arg1 = args[1];
        if (arg0 instanceof String && arg1 instanceof Throwable) {
            mOnCloseGuardListener.onReport((String) arg0, (Throwable) arg1);
        }
    }
}
