package com.eaglesakura.android.glkit.egl;

import com.eaglesakura.android.glkit.EGLUtil;

/**
 * プロセス単位でEGLの制御を行う
 */
public class EGLProcessState {
    /**
     * 管理しているデバイス数
     * <br>
     * 0になったら自動的に廃棄させる
     */
    private static int deviceNum = 0;

    private static final Object lock = new Object();

    /**
     * 有効なEGLデバイス数を増加させる
     */
    public static boolean incrementDevice() {
        synchronized (lock) {
            ++deviceNum;

            if (deviceNum == 1) {
                EGLUtil.log("EGLDevice == 1, request eglInitialize");
                return true;
            } else {
                return false;
            }
        }
    }

    /**
     * 有効なEGLデバイス数を減らす
     * <br>
     * デバイスが0になったらeglTerminateを行うべき
     */
    public static boolean decrementDevice() {
        synchronized (lock) {
            --deviceNum;
            if (deviceNum < 0) {
                throw new IllegalStateException("DeviceNum");
            }

            if (deviceNum == 0) {
                EGLUtil.log("EGLDevice == 0, request eglTerminate");
                return true;
            } else {
                return false;
            }
        }
    }
}
