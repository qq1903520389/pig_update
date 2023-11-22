package com.pigeon.pig_update;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.res.AssetManager;
import android.content.res.Resources;
import android.os.Handler;
import android.os.Looper;
import android.widget.Toast;

import androidx.annotation.NonNull;

import com.pigeon.utils.app.AppUtils;

import io.flutter.Log;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;

/**
 * @author: 周立强
 * @create: 2023-08-28 14:40
 */
public class PigUpdateApplication implements Thread.UncaughtExceptionHandler {
    private static Thread mUiThread;
    private static ActivityPluginBinding binding;
    private static Activity mActivity;


    private static Handler sHandler = new Handler(Looper.getMainLooper());

    public static Application sApplication;

    public static final String TAG = PigUpdateApplication.class.getSimpleName();

    Thread.UncaughtExceptionHandler mDefaultHandler;

    public static void init(Application application) { //在Application中初始化
        sApplication = application;
        mUiThread = Thread.currentThread();
    }

    public static ActivityPluginBinding getBinding() {
        return binding;
    }

    public static void setBinding(ActivityPluginBinding binding) {
        PigUpdateApplication.binding = binding;
    }

    public static Activity getActivity() {
        return PigUpdateApplication.mActivity;
    }

    public static void setActivity(Activity mActivity) {
        AppUtils.init(mActivity);
        PigUpdateApplication.mActivity = mActivity;
    }

    public static Context getContext() {
        return sApplication.getApplicationContext();
    }

    public static AssetManager getAssets() {
        return sApplication.getAssets();
    }

    public static Resources getResource() {
        return sApplication.getResources();
    }

    public static boolean isUIThread() {
        return Thread.currentThread() == mUiThread;
    }

    public static void runOnUI(Runnable r) {
        sHandler.post(r);
    }

    public static void runOnUIDelayed(Runnable r, long delayMills) {
        sHandler.postDelayed(r, delayMills);
    }

    public static void removeRunnable(Runnable r) {
        if (r == null) {
            sHandler.removeCallbacksAndMessages(null);
        } else {
            sHandler.removeCallbacks(r);
        }
    }

    public Context getApplicationContext() {
        return sApplication;
    }


    @Override
    public void uncaughtException(@NonNull Thread thread, @NonNull Throwable ex) {
        if (!handleException(ex) && mDefaultHandler != null) {
            //如果用户没有处理则让系统默认的异常处理器来处理
            mDefaultHandler.uncaughtException(thread, ex);
        } else {
            try {
                // 暂停3秒
                Thread.sleep(3000);
            } catch (InterruptedException e) {
                Log.e(TAG, "error : ", e);
            }
            // 退出程序
            android.os.Process.killProcess(android.os.Process.myPid());
            System.exit(1);
        }
    }

    /**
     * 自定义错误处理,收集错误信息 发送错误报告等操作均在此完成.
     *
     * @param ex
     * @return true:如果处理了该异常信息;否则返回false.
     */
    private boolean handleException(Throwable ex) {
        if (ex == null) {
            return false;
        }
        // 使用Toast来显示异常信息
        new Thread() {
            @Override
            public void run() {
                Looper.prepare();
                Toast.makeText(sApplication, "程序出现异常，即将退出～", Toast.LENGTH_SHORT).show();
                Looper.loop();
            }
        }.start();

        return true;
    }

}
