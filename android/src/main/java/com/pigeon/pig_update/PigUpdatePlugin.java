package com.pigeon.pig_update;

import android.app.Activity;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.TextUtils;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.azhon.appupdate.listener.OnDownloadListener;
import com.azhon.appupdate.manager.DownloadManager;
import com.azhon.appupdate.util.ApkUtil;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.lang.ref.WeakReference;
import java.util.HashMap;

import io.flutter.embedding.engine.plugins.FlutterPlugin;
import io.flutter.embedding.engine.plugins.activity.ActivityAware;
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding;
import io.flutter.plugin.common.EventChannel;
import io.flutter.plugin.common.MethodCall;
import io.flutter.plugin.common.MethodChannel;
import io.flutter.plugin.common.MethodChannel.MethodCallHandler;
import io.flutter.plugin.common.MethodChannel.Result;
import io.flutter.plugin.common.PluginRegistry;

/**
 * PigUpdatePlugin
 */
public class PigUpdatePlugin implements FlutterPlugin, MethodCallHandler, ActivityAware {
    /// The MethodChannel that will the communication between Flutter and native Android
    ///
    /// This local reference serves to register the plugin with the Flutter Engine and unregister it
    /// when the Flutter Engine is detached from the Activity
    private MethodChannel channel;
    private EventChannel eventChannel;
    private EventChannel.EventSink mSink;
    private DownloadManager manager = null;
    //事件派发流
    private EventChannel.StreamHandler streamHandler = new EventChannel.StreamHandler() {
        @Override
        public void onListen(Object o, EventChannel.EventSink sink) {
            mSink = sink;
        }

        @Override
        public void onCancel(Object o) {
            eventChannel = null;
            mSink = null;
        }
    };

    @Override
    public void onAttachedToEngine(@NonNull FlutterPluginBinding flutterPluginBinding) {
        channel = new MethodChannel(flutterPluginBinding.getBinaryMessenger(), "pig_update");
        channel.setMethodCallHandler(this);
    eventChannel = new EventChannel(flutterPluginBinding.getBinaryMessenger(), "plugins/update_listener");
        eventChannel.setStreamHandler(streamHandler);
    }

    @Override
    public void onMethodCall(@NonNull MethodCall call, @NonNull Result result) {

        switch (call.method) {
            case "getPlatformVersion":
                result.success("Android " + android.os.Build.VERSION.RELEASE);
                break;
            case Constants.GET_VERSION_CODE_METHOD:
                getVersionCode(result);
                break;
            case Constants.GET_VERSION_NAME_METHOD:
                getVersionName(result);
                break;
            case Constants.UPDATE_METHOD:
                update(call, result);
                break;
            case Constants.CANCEL_METHOD:
                cancel(result);
                break;
            default:
                result.notImplemented();
                break;
        }
    }

    /**
     * @Method getVersionCode
     * @Author: zhouliqinag
     * @Description:获取版本号
     * @Date: 19:36 2023/9/1
     */
    private void getVersionCode(@NonNull Result result) {
        long versionCode = ApkUtil.Companion.getVersionCode(PigUpdateApplication.getActivity());
        result.success(versionCode);
    }

    /**
     * @Method getVersionName
     * @Author: zhouliqinag
     * @Description:获取版本名称
     * @Date: 19:36 2023/9/1
     */
    private void getVersionName(@NonNull Result result) {
        try {
            PackageInfo packageInfo =
                    PigUpdateApplication.getActivity().getApplicationContext().getPackageManager().getPackageInfo(PigUpdateApplication.getActivity().getPackageName(), 0);
            result.success(packageInfo.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            throw new RuntimeException(e);
        }
    }

    /**
     * @Method getVersionCode
     * @Author: zhouliqinag
     * @Description:下载 判断参数是否为空来设置
     * @Date: 19:36 2023/9/1
     */
    private void update(MethodCall call, Result result) {

        HashMap<String, Object> model = call.argument("model");
        //获取图标
        int smallIcon = PigUpdateApplication.getActivity().getResources().getIdentifier(
                String.valueOf(model.get("smallIcon")), "mipmap", PigUpdateApplication.getActivity().getPackageName()
        );
        DownloadManager.Builder downloadListener = new DownloadManager.Builder(PigUpdateApplication.getActivity());
        downloadListener.apkName(String.valueOf(model.get("apkName")));
        downloadListener.apkUrl(String.valueOf(model.get("apkUrl")));
        downloadListener.smallIcon(smallIcon);
        downloadListener.showNotification((Boolean) model.get("showNotification"));
        downloadListener.jumpInstallPage((Boolean) model.get("jumpInstallPage"));
        downloadListener.showBgdToast((Boolean) model.get("showBgdToast"));
        downloadListener.onDownloadListener(onDownloadListener);
        if (notNull(model,"showBgdToast")) {
            downloadListener.apkMD5(String.valueOf(model.get("apkMD5")));
        }
        manager = downloadListener.build();
        manager.download();
        result.success(true);
    }
    /**
     * @Method cancel
     * @Author: zhouliqinag
     * @Description:完成
     */
    private void cancel(Result result) {
        manager.cancel();
        result.success(true);
    }

    @Override
    public void onDetachedFromEngine(@NonNull FlutterPluginBinding binding) {
        channel.setMethodCallHandler(null);
    }

    @Override
    public void onAttachedToActivity(@NonNull ActivityPluginBinding binding) {
        WeakReference<Activity> activity = new WeakReference<>(binding.getActivity());
        PigUpdateApplication.setActivity(activity.get());
        PigUpdateApplication.setBinding(binding);
        binding.addActivityResultListener(onActivityResultListener);
        binding.addOnSaveStateListener(onSaveInstanceStateListener);
    }

    @Override
    public void onDetachedFromActivityForConfigChanges() {

    }

    @Override
    public void onReattachedToActivityForConfigChanges(@NonNull ActivityPluginBinding binding) {
        PigUpdateApplication.setBinding(binding);
    }

    @Override
    public void onDetachedFromActivity() {
        PigUpdateApplication.setActivity(null);
    }

    PluginRegistry.ActivityResultListener onActivityResultListener = new PluginRegistry.ActivityResultListener() {
        @Override
        public boolean onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
            return false;
        }
    };
    ///////////////////////////////////////////////////////////////////////////
    // 状态保存
    ///////////////////////////////////////////////////////////////////////////
    ActivityPluginBinding.OnSaveInstanceStateListener onSaveInstanceStateListener = new ActivityPluginBinding.OnSaveInstanceStateListener() {
        @Override
        public void onSaveInstanceState(@NonNull Bundle outState) {
        }

        @Override
        public void onRestoreInstanceState(@Nullable Bundle bundle) {

        }
    };


    OnDownloadListener onDownloadListener = new OnDownloadListener() {
        @Override
        public void start() {
            mSink.success(json("start").toString());
        }

        @Override
        public void downloading(int max, int progress) {
            try {
                JSONObject json = json("downloading");
                json.put("max", max);
                json.put("progress", progress);
                mSink.success(json.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void done(@NonNull File apk) {
            manager = null;
            try {
                JSONObject json = json("done");
                json.put("apk", apk.getPath());
                mSink.success(json.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        @Override
        public void cancel() {
            mSink.success(json("cancel").toString());
        }

        @Override
        public void error(@NonNull Throwable err) {
            try {
                JSONObject json = json("error");
                json.put("exception", err.getMessage());
                mSink.success(json.toString());
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
    };

    private JSONObject json(String type) {
        JSONObject json = new JSONObject();
        try {
            json.put("type", type);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return json;
    }

  //判断是否为空
  private Boolean notNull(HashMap<String, Object> model,String key){
    if (model.get(key) instanceof String) {
      return !TextUtils.isEmpty(""+model.get(key));
    }
    return model.get(key) != null;
  }
}
