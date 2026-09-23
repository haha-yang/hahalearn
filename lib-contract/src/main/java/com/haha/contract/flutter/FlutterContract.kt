package com.haha.contract.flutter

/**
 * Flutter 与原生共用的通道约定。Engine 创建与 MethodChannel 调用留在 app。
 */
object FlutterContract {
    const val FLUTTER_ENGINE_ID = "haha_flutter_engine"
    const val CHANNEL_NAME = "com.haha.flutter_module/channel"
}
