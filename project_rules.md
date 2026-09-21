# HahaLearn 项目说明与开发规范

个人 Android 实验工程：按功能分包演示系统能力（定位、蓝牙、网络、启动优化等），并沉淀 Activity 基建、MVI
框架、Flutter 混编与自定义 SPI。  
包名：`com.haha.hahalearn`（代码根包 `com.haha`）。Application：`com.haha.HahaApplication`。首页：
`com.haha.main.MainActivity`（`singleInstance`）。

详细框架文档：

- [`docs/base-mvvm-activity.md`](docs/base-mvvm-activity.md) — MVVM Activity 基建
- [`docs/mvi-framework.md`](docs/mvi-framework.md) — MVI 单向数据流
- [`docs/startup-optimization.md`](docs/startup-optimization.md) — 首页冷启动优化
- [`docs/retrofit/retrofit-usage-guide.md`](docs/retrofit/retrofit-usage-guide.md) — Retrofit
  使用与源码流程（纯 Call）
- [`docs/glide/glide-cache-engine-notes.md`](docs/glide/glide-cache-engine-notes.md) — Glide 缓存与
  Engine 链路
- [
  `docs/rxjava/rxjava-create-subscribe-operators.md`](docs/rxjava/rxjava-create-subscribe-operators.md) —
  RxJava
  create / subscribe / subscribeOn / observeOn / map 源码
- [`docs/okhttp/okhttp-call-to-response.md`](docs/okhttp/okhttp-call-to-response.md) — OkHttp 从
  `newCall` 到读 body（主线 `OkHttpTest` enqueue GET/POST，对照 `CacheFile` / `RetrofitTest`）
- [`docs/doflog/doflog-design-and-flow.md`](docs/doflog/doflog-design-and-flow.md) — DOFLog
  设计模式与打印流程
- [`docs/https-http-versions.md`](docs/https-http-versions.md) — HTTPS 上 HTTP/1.1、HTTP/2、HTTP/3
  协议对比与 Demo（`SocketTest` / `HttpsVersionTest`）
- [`docs/speech-asr/speech-asr-offline-caption.md`](docs/speech-asr/speech-asr-offline-caption.md) —
  离线中文识别与实时字幕（`:speech-asr`）
- [`docs/mqtt/mqtt-sender-receiver.md`](docs/mqtt/mqtt-sender-receiver.md) — MQTT Fixed Header
  与发送方 /
  接收方 Demo（入口 `MqttTest`，实现在 `retrofit.mqtt` 包，附图
  [`docs/mqtt/assets/mqtt-fixed-header.jpg`](docs/mqtt/assets/mqtt-fixed-header.jpg)）

---

## 1. 工程与版本

| 项                      | 当前值                                                             |
|------------------------|-----------------------------------------------------------------|
| 工程名                    | HahaLearn                                                         |
| Gradle Wrapper         | 8.13-all                                                        |
| Android Gradle Plugin  | 8.13.1                                                          |
| Kotlin                 | 2.1.0（插件 classpath 2.2.0）                                       |
| JDK / JVM target       | 17                                                              |
| compileSdk / targetSdk | 36                                                              |
| minSdk                 | 24                                                              |
| NDK                    | 28.2.13676358（16 KB page 对齐）                                    |
| CMake                  | 3.22.1，native so：`HahaLearn`                                      |
| ABI                    | `arm64-v8a`                                                     |
| Flutter 模块             | `flutterStudy/`（Dart 包名 `flutter_study`，SDK `^3.10.4`）        |
| 历史 Flutter 对齐          | Flutter SDK 3.38.5 / Dart 3.10.4 / Flutter Gradle Plugin 88.2.0 |

`buildFeatures`：`viewBinding`、`dataBinding`、`buildConfig` 已开；**Compose 未启用**（相关依赖与
`composeOptions` 已注释）。

---

## 2. 模块划分

`settings.gradle` 当前 include：

| 模块                            | 职责                                                 |
|-------------------------------|----------------------------------------------------|
| `:app`                        | 宿主壳：业务 Demo，只编译依赖 api / runtime，不依赖 ServiceImpl 源码 |
| `:lib-common`                 | BaseActivity / BaseMvvm* / 主题 / TimeMonitor        |
| `:BluetoothSdk`               | BLE 扫描 / 连接 / GATT / RFCOMM / A2DP                 |
| `:speech-asr`                 | 离线中文 ASR 流水线：PCM / WAV / MediaCodec / VAD / 实时字幕   |
| `:ServiceApi`                 | SPI 接口（如 `IUserService`）                           |
| `:ServiceRuntime`             | ServiceLoader / ServiceLoaderHelper 运行时            |
| `:ServiceImpl`                | SPI 实现，宿主 `runtimeOnly` 仅打包                        |
| `:ServiceAnnotation`          | `@BindView` / `@OnClick` / `@ServiceImpl` 等注解      |
| `:ServiceAnnotationProcessor` | 注解处理器                                              |
| `:ServiceAnnotationRuntime`   | 运行时绑定辅助                                            |
| `:pluginapp`                  | 插件化 Demo                                           |
| `:flutter` / `:flutterStudy`  | Flutter add-to-app（`include_flutter.groovy`）       |

未启用（目录仍在，settings 已注释）：`:starrysky`。

`:ServiceRouter` / `:ServiceRouterProcessor` / `:ServiceRouterAnnotation` / `:ServiceRouterUtils`
已接入：页面路由 `DOFRouter`。`ServiceRouterPlugin`（includeBuild）在打包期 ASM 注入
`Router.loadRouterMap`，启动不再扫 dex；未注入时回退扫 dex。

---

## 3. 技术栈（以仓库现状为准）

- **语言**：Kotlin 为主，历史 Java 可保留；**新代码优先 Kotlin**。
- **UI**：XML + DataBinding（根节点必须 `<layout>`）+ ViewBinding；**不要引入 Compose 作为默认 UI**。
- **架构**：
    - 普通页：`BaseMvvmActivity` + `BaseViewModel`（`AndroidViewModel`）
    - 单向数据流页：`BaseMVIActivity` + `BaseMVIViewModel`
- **异步**：Kotlin Coroutines / Flow 为新代码首选；RxJava 1/2/3 为历史代码，新逻辑不要再加 Rx。
- **网络**：Retrofit + OkHttp + Gson/Moshi；MVI 示例走 `mviFrame` 内 Repository。
- **本地存储**：Room 2.5.0（`room-runtime` + `room-ktx`），不要直接操作 SQLite。
- **图片**：Glide 4.15.1。
- **DI**：无 Hilt/Koin；带参 ViewModel 用 `BaseViewModelFactory`。
- **其它**：AndroidX（`android.useAndroidX=true`）、LeakCanary（仅 debug）、ARouter
  API、AutoService、Lottie、Banner、SplashScreen（首页主题暂关）。

禁止：support 库新依赖、为 Demo 页引入 Hilt、默认用 Compose 重写现有 XML 页。

---

## 4. Activity 继承与页面接入

```text
AppCompatActivity
 └── BaseActivity                    // 系统栏、主题、Activity 栈、TimeMonitor
      ├── MainActivity               // 首页：开屏 + 异步预热，不走 MVVM 基类
      └── BaseMvvmActivity<VB, VM>   // 普通页 DataBinding、ViewModel、权限、onContentReady
           └── BaseMVIActivity<VB, VM, I, S, E>
```

| 场景     | 继承                                           | 必写                                                            |
|--------|----------------------------------------------|---------------------------------------------------------------|
| 首页     | `BaseActivity`（`MainActivity`）               | 开屏 / 异步 inflate / 权限都写在本页，不要改 `BaseMvvmActivity`              |
| 普通业务页  | `BaseMvvmActivity<XxxBinding, XxxViewModel>` | `getLayoutId` / `getViewModelClass` / `initView` / `initData` |
| 无独立 VM | 同上，VM 用 `BaseViewModel`                      | 同上                                                            |
| MVI 页  | `BaseMVIActivity`                            | 另加 `render`；点击只 `sendIntent`；副作用放 `handleEffect`              |

约定：

1. 普通页布局根节点必须是 `<layout>`，否则 DataBinding bind 失败。
2. `onContentReady()` 只服务 MVVM / MVI 页；MVI 必须先 `observeMvi` 再 `super`。
3. 权限 `registerForActivityResult` 固定在 `onCreate`，不能等到异步 inflate 回调。
4. 默认 `ViewModelProvider(this)[clazz]`，仅无参 / `Application` 单参；带 Repository 用
   `BaseViewModelFactory`。
5. 不要把首页开屏 / Async inflate 钩子加回 `BaseMvvmActivity`。

---

## 5. MVI 约定（新单向流页面必须遵守）

数据流：`View sendIntent(I)` → `ViewModel handleIntent` → `setState` / `sendEffect` → `render` /
`handleEffect`。

| 类型             | 用途                                                 |
|----------------|----------------------------------------------------|
| `IMviIntent`   | 用户意图，View 唯一入口                                     |
| `IMviUiState`  | 可重放状态，**单一 data class**（`isLoading`、列表、error 正交并存） |
| `IMviUiEffect` | Toast / 导航等一次性副作用；无副作用用 `NoUiEffect`               |

硬性规则：

- 不要在 View 直接调业务方法；不要在 `render` 里做只该发生一次的事（`render` 须幂等）。
- Toast / 跳转不要写入 State；Intent 由 Channel 串行消费。
- 收集必须 `repeatOnLifecycle(STARTED)`（基类已做）。
- IO 在 `handleIntent` 链里 `withContext(Dispatchers.IO)`。

示例：`com.haha.mviFrame.main`（用户列表）、`com.haha.mviFrame.data`（数据页）。完整步骤见
`docs/mvi-framework.md`。

---

## 6. `app` 功能包一览

按 `com.haha.*` 分包，一个包一类 Demo，新增能力优先新建包而不是塞进 `main`。

| 包                                                              | 内容                                                   |
|----------------------------------------------------------------|------------------------------------------------------|
| `base`                                                         | Activity / Fragment / ViewModel 基建、`ActivityManager` |
| `main`                                                         | 首页入口、TimeMonitor、Glide 预览、广播、插件 Demo                 |
| `mviFrame`                                                     | MVI 基类与示例                                            |
| `gps`                                                          | 定位                                                   |
| `bluetooth`                                                    | BLE UI，依赖 `:BluetoothSdk`                            |
| `wifi` / `network`                                             | Wi-Fi、网络监听与加密                                        |
| `flutter`                                                      | `FlutterIntegrationActivity` + MethodChannel         |
| `volume`                                                       | 音量 / 歌词 / 媒体                                         |
| `speech.ui`                                                    | 离线中文识别 / 实时字幕 Demo（核心在 `:speech-asr`）                |
| `waterfall`                                                    | 瀑布流自定义布局                                             |
| `animation` / `scene` / `transparency`                         | 动画、Scene、透明 Activity                                 |
| `liveData` / `coroutineScope` / `room`                         | 组件与协程、Room 示例                                        |
| `recyclerview` / `easyswipemenulayout`                         | 列表与侧滑菜单                                              |
| `remoteviews` / `dynamicTextView` / `imageViewer` / `selector` | 通知栏、动态文字、大图、Banner 选择                                |
| `log` / `permission` / `pluincore` / `binder`                  | 日志、权限、插件 Hook、Binder                                 |
| `service.accessibility`                                        | 无障碍 Service                                          |
| `ui`                                                           | Dialog、可折叠 TextView 等通用控件                            |

首页按钮跳转到上述 Activity；新增 Demo 需：包 + 布局 + Manifest 注册 + 首页入口。

---

## 7. 启动与性能

- 计时：`HahaApplication.attachBaseContext` 里 `TimeMonitor.startMonitor()`（**不含**
  `System.loadLibrary("HahaLearn")`）。Logcat tag：`TimeMonitor`。
- 首页：`MainActivity` 继承 `BaseActivity`，在 `onWindowReady()` 里挂容器 + 开屏 overlay，
  `AsyncLayoutInflater` 把首页挂到底下预热。不要改 `BaseMvvmActivity`。
- 开屏：本地缓存（`SplashAdCache`）有物料才展示；跳过立刻揭开；点素材进
  `SplashAdLandingActivity`（站内 WebView）。品牌 `Theme.App.Starting` 只盖进程创建。
- 重自定义 View（`CircleProgressView` / `PieChartView`）用 ViewStub，首帧后再 inflate。
- **不要**在 `BaseActivity` + `BaseMvvmActivity` 各 `setContentView` 一次。
- 其它页默认同步 DataBinding inflate。优化细节与数据见 `docs/startup-optimization.md`。

---

## 8. Flutter 混编

- 模块目录：`flutterStudy/`，由 `settings.gradle` 的 `include_flutter.groovy` 引入为 `:flutter`。
- Native 入口：`com.haha.flutter.FlutterIntegrationActivity`（`FlutterFragment` + `FlutterChannel` /
  `MethodChannel`）。
- 改 Flutter 侧逻辑放 `flutterStudy/lib/`；改宿主嵌入、通道、状态栏适配放 `app/.../flutter/`。
- 不要把 Flutter UI 逻辑写进 Kotlin Activity，通道协议变更需双侧同步。

---

## 9. 代码风格

- **命名**：类 `UpperCamelCase`；函数/变量 `lowerCamelCase`；常量 `UPPER_SNAKE_CASE`；资源按功能前缀（如
  `activity_main.xml`、`string` 按模块拆文件）。
- **文件头**：现有文件保留 `author / time / desc / version` 风格；新公共 API 补 KDoc。
- **格式**：缩进 4 空格；Kotlin official style（`kotlin.code.style=official`）。
- **优先 Kotlin 特性**：空安全、密封类 / `sealed interface`（Intent、Effect）、data class `copy`、扩展函数。
- **包结构**：功能包内再按 `ui` / `adapter` / `model` 拆；MVI 页同包放 `XxxIntent` / `XxxUiState` /
  `XxxUiEffect` / `XxxViewModel` / `XxxActivity`。
- **不要**：为风格统一而把存量 Java 一次性改写成 Kotlin；不要无关重构、不要扩写未要求的 Markdown。

---

## 10. 改代码时的约束（给协作者 / Agent）

1. 只改任务相关文件；不顺手格式化、不删注释掉的历史代码（除非任务要求）。
2. 新页面走现有基类，不要直接继承 `AppCompatActivity`。
3. 需要单向流时用 MVI 基类，不要再手写一套 Channel + 裸 `lifecycleScope.collect`。
4. 依赖版本以根 `build.gradle` 的 `ext` 与模块 `build.gradle` 为准，不要在规范里写与仓库不一致的「理想版本」。
5. Native / NDK / 16 KB 对齐相关改动需同时考虑 `ndkVersion` 与 Flutter/Rive so。
6. Debug 泄漏检测走 `src/debug` 的 `LeakCanaryInstaller`，不要在 `main` 里直接依赖 LeakCanary。
