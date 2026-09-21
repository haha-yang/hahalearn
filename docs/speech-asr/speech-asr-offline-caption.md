# 离线中文识别与实时字幕

记录时间：2026-09-19  
模块：独立 Android Library `:speech-asr`（包名 `com.haha.speech`）  
Demo 页：`com.haha.speech.ui.SpeechCaptionActivity`（首页按钮「离线语音识别 / 实时字幕」）  
路由：`RoutePath.SPEECH_CAPTION` → `/speech/caption`

配套流程图：

| 图       | PNG                                       | 源文件                                       |
|---------|-------------------------------------------|-------------------------------------------|
| 模块分层    | [png](assets/speech-asr-architecture.png) | [mmd](assets/speech-asr-architecture.mmd) |
| 实时字幕时序  | [png](assets/speech-asr-stream-flow.png)  | [mmd](assets/speech-asr-stream-flow.mmd)  |
| 文件解码到识别 | [png](assets/speech-asr-file-decode.png)  | [mmd](assets/speech-asr-file-decode.mmd)  |

本文说明：音频如何变成 PCM、PCM 如何变成字、字幕如何区分 Partial / Final。引擎用 sherpa-onnx 流式
Zipformer；模型不进 APK，第一次点「准备中文模型」再下载。

---

## 1. 为什么拆成独立模块

和 `:BluetoothSdk` 一样：可复用的能力放 Library，页面放 `app`。

| 层   | 位置                           | 职责                             |
|-----|------------------------------|--------------------------------|
| UI  | `app/.../speech/ui`          | MVI 页：权限、按钮、字幕展示               |
| 会话  | `CaptionSession`             | 麦克风 / 文件 → 引擎 → 字幕聚合           |
| 引擎  | `StreamingAsrEngine`         | Partial / Final；Sherpa 或 Probe |
| PCM | `pcm` / `decode` / `feature` | 读字节、解压缩、RMS / VAD              |
| 模型  | `ModelStore`                 | filesDir / assets / 下载         |

![模块分层](assets/speech-asr-architecture.png)

学习时建议按目录往下看，不要先陷进 ONNX。

---

## 2. PCM 三要素

识别模型要的不是 MP3，而是波形采样：

| 项   | 本模块默认    | 含义                             |
|-----|----------|--------------------------------|
| 采样率 | 16000    | 每秒 16000 个点                    |
| 声道  | 1        | 单声道，双声道会先平均                    |
| 位深  | 16bit 小端 | 一个点 2 字节，再除以 32768 变成 `[-1,1]` |

类：`PcmFormat.ASR_DEFAULT`、`PcmUtils.bytesToFloatMono()`。

`PlayThread` 里的 `AudioTrack` 也是同一套约定，只是方向相反：这里读麦克风，那边写扬声器。

---

## 3. 三条输入路径

1. **麦克风**：`MicPcmSource` → `AudioRecord`，100ms 一块 PCM。  
   音源用 `VOICE_RECOGNITION`，比裸 `MIC` 更适合说话。
2. **PCM WAV**：`WavPcmReader` 自己解 `RIFF` / `fmt ` / `data`。`fmt ≠ 1` 直接拒绝。
3. **MP3/AAC**：`MediaExtractor` 选音轨 + `MediaCodec` 解码，再线性重采样到 16kHz。

![文件解码到识别](assets/speech-asr-file-decode.png)

`MediaPlayer` 只能播，拿不到采样点，所以 Demo 不走它。

---

## 4. 实时字幕时序

流式识别的关键是 **边说边出字**：

- `Partial`：当前这句话的草稿，下一块 PCM 会覆盖。
- `Final`：endpoint（静音 / 句尾）后定稿，追加到历史。
- Sherpa 的 `reset(stream)` 开启下一句，不能把两句粘在同一个 stream 状态里。

![实时字幕时序](assets/speech-asr-stream-flow.png)

`CaptionAggregator` 只做这两件事：覆盖当前行、追加历史行。UI 上「已确认句子」是 Final，「当前一句」是
Partial。

能量 VAD（`EnergyVad`）只给电平条和 Probe 引擎用。真正切句靠 Sherpa 的 endpoint 规则（尾静音 1.2s /
2.4s）。

---

## 5. 两个引擎

`AsrEngineFactory`：模型齐了用 Sherpa，否则用 Probe。

| 引擎                         | 何时                     | 输出                                  |
|----------------------------|------------------------|-------------------------------------|
| `ProbeAsrEngine`           | 还没下载模型                 | 用 RMS 演示 Partial / Final，字幕是「检测到语音」 |
| `SherpaStreamingAsrEngine` | `ModelStore.isReady()` | 离线中文 Zipformer，真正出字                 |

Sherpa 调用顺序（见 `SherpaStreamingAsrEngine`）：

```text
acceptWaveform(float[], 16000)
while (isReady(stream)) decode(stream)
text = getResult(stream).text
if (isEndpoint) { emit Final; reset(stream) }
```

模型：`sherpa-onnx-streaming-zipformer-zh-14M-2023-02-23`（int8，约十几 MB 级参数）。  
需要四个文件：`encoder*.int8.onnx`、`decoder*.onnx`、`joiner*.int8.onnx`、`tokens.txt`。  
`modelType = zipformer`（2023 的 14M 是 zipformer，不是 zipformer2）。

---

## 6. 模型怎么来

不进 git。`ModelStore` 顺序：

1. `filesDir/speech-asr/models/<模型目录>/` 四个文件已齐 → 直接用
2. 拷贝 `assets/speech-asr/zh-14m/` 里已解压的四个文件（可选）
3. **安装包** `filesDir/speech-asr/models/<模型目录>.tar.bz2`（或 assets 同名包）已存在 → **只解压，不下载
   **
4. 安装包不存在 → 下载后再解压；包会留下，下次跳过下载

GitHub 下不下来时：在电脑解开 tar.bz2，把四个文件 `adb push` 到应用 filesDir，或放到模块
`src/main/assets/speech-asr/zh-14m/`。

依赖：`com.github.k2-fsa.sherpa-onnx:sherpa-onnx:v1.13.5`（JitPack，根 `build.gradle` 已有仓库）。只打
`arm64-v8a`。

---

## 7. 怎么跑

1. 首页 → 「离线语音识别 / 实时字幕」
2. 允许麦克风。此时即可「开始实时字幕」，走 Probe，看 RMS / 分句。
3. 点「准备中文模型」：有安装包则解压，没有才下载。完成后引擎名变成 `Sherpa Zipformer 中文 14M`。
4. 再说中文，看 Partial 跳动、静音后进「已确认句子」。
5. 「识别音频文件」走解码路径，不录音。

Logcat 过滤：`CaptionSession` / `MicPcmSource` / `ModelStore`。

权限：`RECORD_AUDIO`（录音）、`INTERNET`（仅首次下模型）。之后可完全离线。

---

## 8. 还可以接着改的点

- 换成更大的 `sherpa-onnx-streaming-zipformer-zh-int8-2025-06-30`（zipformer2，更准、更重）
- 热词 / ITN（数字反归一化 `itn_zh_number.fst`）
- 把 `LinearResampler` 换成高质量重采样
- 给 `AudioTrack` 回放解码后的 PCM，确认解码没破音
