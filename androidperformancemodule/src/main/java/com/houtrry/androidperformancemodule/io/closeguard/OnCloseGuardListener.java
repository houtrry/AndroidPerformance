package com.houtrry.androidperformancemodule.io.closeguard;

/**
 * @author: houtrry
 * @time: 2022/9/3
 * @desc:
 */

interface OnCloseGuardListener {
    /**
     *
     * @param message
     * @param throwable
     */
    void onReport(String message, Throwable throwable);
}
