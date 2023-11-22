import 'dart:async';
import 'dart:convert';
import 'dart:io';

import 'package:flutter/services.dart';
import 'package:pig_update/update_model.dart';

class PigUpdatePlatform {
  static const MethodChannel _channel = MethodChannel('pig_update');
  static const EventChannel _listenerChannel =
      EventChannel('plugins/update_listener');
  static StreamSubscription? _listenerStream;

  ///获取应用的versionCode
  static Future<int> get getVersionCode async {
    return await _channel.invokeMethod('getVersionCode');
  }

  ///获取应用的versionName
  static Future<String> get getVersionName async {
    return await _channel.invokeMethod('getVersionName');
  }

  ///更新
  static Future<bool> update(UpdateModel model) async {
    return await _channel.invokeMethod('update', {
      'model': model.toJson(),
    });
  }

  ///监听
  static listener(Function callback) {
    if (!Platform.isAndroid) return;
    _listenerStream = _listenerChannel.receiveBroadcastStream().listen((data) {
      Map<String, dynamic> map = jsonDecode(data);
      callback(map);
    });
  }

  ///取消
  static Future<bool> get cancel async {
    return await _channel.invokeMethod('cancel');
  }

  static dispose() {
    _listenerStream?.cancel();
  }
}
