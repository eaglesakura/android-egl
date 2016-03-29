package com.eaglesakura.android.glkit.egl11;

import com.eaglesakura.android.glkit.EGLUtil;
import com.eaglesakura.android.glkit.egl.GLESVersion;
import com.eaglesakura.android.glkit.egl.IEGLContextGroup;

import java.util.Stack;

import javax.microedition.khronos.egl.EGL10;
import javax.microedition.khronos.egl.EGLConfig;
import javax.microedition.khronos.egl.EGLContext;
import javax.microedition.khronos.egl.EGLDisplay;
import javax.microedition.khronos.egl.EGLSurface;

import static javax.microedition.khronos.egl.EGL10.EGL_NO_CONTEXT;

/**
 * ContextGroupを管理する
 */
public class EGL11ContextGroup implements IEGLContextGroup {
    /**
     * Sharedの中心となるEGLContext
     * <br>
     * 最初に生成されたContextをマスターコンテキストとして認識する。
     */
    EGLContext masterContext;


    /**
     * Sharedで利用したOffscreenSurfaceのキャッシュを残す
     * <p/>
     * Adreno 430ではSurfaceを残しておかないとShared Contextで生成したデータが壊れる問題が発生している。
     * そのため、masterContextが廃棄されるまで、使用済みSurfaceはキャッシュとして残しておく
     */
    Stack<EGLSurface> cacheSurfaces = new Stack<>();

    /**
     * 生成したデバイス数
     * <br>
     * 破棄を続けて0になったら開放処理を行う
     */
    int deviceNum;

    final EGL11Manager controller;


    EGL11ContextGroup(EGL11Manager controller) {
        this.controller = controller;
    }

    /**
     * コンテキスト生成処理を行う
     */
    EGLContext createContext() {
        synchronized (this) {
            EGL10 egl = controller.egl;
            EGLDisplay display = controller.display;
            EGLConfig config = controller.config;
            EGLContext result;
            GLESVersion version = controller.getGLESVersion();

            if (masterContext == null) {
                result = egl.eglCreateContext(display, config, EGL_NO_CONTEXT, version.getContextAttribute());
                masterContext = result;
                EGLUtil.log("create master context");
            } else {
                result = egl.eglCreateContext(display, config, masterContext, version.getContextAttribute());
                EGLUtil.log("create shared context");
            }

            if (result == EGL_NO_CONTEXT) {
                EGLUtil.printEglError(egl.eglGetError());
                EGLUtil.log("create error shared context devices(%d)", deviceNum);
                throw new IllegalStateException("eglCreateContext");
            }

            // 共有数がひとつ増えた
            ++deviceNum;
            return result;
        }
    }

    /**
     * Context廃棄処理を行う
     */
    void destroyContext(EGLContext context) {
        synchronized (this) {
            EGL10 egl = controller.egl;
            EGLDisplay display = controller.display;

            if (context != masterContext) {
                // masterでない場合、Contextを廃棄する
                egl.eglDestroyContext(display, context);
                EGLUtil.printEglError(egl.eglGetError());
            }

            --deviceNum;

            // デバイス数が0になったなら、マスターも不要となる
            if (deviceNum == 0) {
                for (EGLSurface surface : cacheSurfaces) {
                    egl.eglDestroySurface(display, surface);
                    EGLUtil.log("destroy cache surface(%s)", surface.toString());
                }
                cacheSurfaces.clear();

                EGLUtil.log("desroy masterContext(%s)", masterContext.toString());
                egl.eglDestroyContext(display, masterContext);
                masterContext = null;
            }
        }
    }

    @Override
    public int getDeviceNum() {
        return deviceNum;
    }

    EGLSurface popCacheSurface() {
        synchronized (this) {
            if (cacheSurfaces.isEmpty()) {
                return null;
            } else {
                return cacheSurfaces.pop();
            }
        }
    }

    void pushCacheSurface(EGLSurface surface) {
        synchronized (this) {
            cacheSurfaces.push(surface);
        }
    }
}
