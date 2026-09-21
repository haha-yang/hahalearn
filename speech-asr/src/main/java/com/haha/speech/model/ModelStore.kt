package com.haha.speech.model

import android.content.Context
import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream
import org.apache.commons.compress.compressors.bzip2.BZip2CompressorInputStream
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

/**
 * 模型落在 filesDir/speech-asr/models/，不进 APK。
 * 查找顺序：已下载目录 → assets/speech-asr/zh-14m/ → GitHub 发行包。
 */
class ModelStore(context: Context) {

    private val appContext = context.applicationContext
    private val modelsRoot = File(appContext.filesDir, "speech-asr/models")
    val modelDir: File = File(modelsRoot, SherpaZh14mModel.DIR_NAME)

    private val http = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(5, TimeUnit.MINUTES)
        .build()

    val archiveFile: File = File(modelsRoot, SherpaZh14mModel.ARCHIVE_NAME)

    fun isReady(): Boolean = SherpaZh14mModel.isComplete(modelDir)

    fun hasLocalArchive(): Boolean = isUsableArchive(archiveFile)

    fun copyFromAssetsIfPresent(): Boolean {
        if (isReady()) return true
        val assetNames = listOf(
            SherpaZh14mModel.ENCODER,
            SherpaZh14mModel.DECODER,
            SherpaZh14mModel.JOINER,
            SherpaZh14mModel.TOKENS
        )
        val assetDir = "speech-asr/zh-14m"
        val available = runCatching { appContext.assets.list(assetDir)?.toSet() }.getOrNull()
            ?: return false
        if (!assetNames.all { it in available }) return false
        modelDir.mkdirs()
        assetNames.forEach { name ->
            appContext.assets.open("$assetDir/$name").use { input ->
                File(modelDir, name).outputStream().use { output ->
                    input.copyTo(output)
                }
            }
        }
        return isReady()
    }

    /**
     * @return true 表示这次走了网络下载；false 表示本地已有安装包，只解压。
     */
    fun prepare(onProgress: (downloaded: Long, total: Long) -> Unit = { _, _ -> }): Boolean {
        if (isReady()) return false
        if (copyFromAssetsIfPresent()) return false
        modelsRoot.mkdirs()
        if (!hasLocalArchive()) {
            copyArchiveFromAssetsIfPresent()
        }
        val archiveReady = hasLocalArchive()
        if (!archiveReady) {
            val part = File(modelsRoot, "${SherpaZh14mModel.ARCHIVE_NAME}.part")
            download(SherpaZh14mModel.ARCHIVE_URL, part, onProgress)
            if (archiveFile.exists()) archiveFile.delete()
            if (!part.renameTo(archiveFile)) {
                part.copyTo(archiveFile, overwrite = true)
                part.delete()
            }
        } else {
            Log.i(TAG, "archive exists, skip download: ${archiveFile.absolutePath}")
            onProgress(archiveFile.length(), archiveFile.length())
        }
        extractNeededFiles(archiveFile, modelDir)
        if (!isReady()) {
            error("模型解压后仍缺文件，目录=${modelDir.absolutePath}")
        }
        return !archiveReady
    }

    private fun isUsableArchive(file: File): Boolean = file.isFile && file.length() > 1024L

    private fun copyArchiveFromAssetsIfPresent() {
        val assetPath = listOf(
            "speech-asr/${SherpaZh14mModel.ARCHIVE_NAME}",
            "speech-asr/zh-14m/${SherpaZh14mModel.ARCHIVE_NAME}",
            SherpaZh14mModel.ARCHIVE_NAME
        ).firstOrNull { path ->
            runCatching {
                appContext.assets.open(path).close()
                true
            }.getOrDefault(false)
        } ?: return
        appContext.assets.open(assetPath).use { input ->
            FileOutputStream(archiveFile).use { output -> input.copyTo(output) }
        }
        Log.i(TAG, "copied archive from assets/$assetPath")
    }

    private fun download(
        url: String,
        dest: File,
        onProgress: (downloaded: Long, total: Long) -> Unit
    ) {
        val request = Request.Builder().url(url).get().build()
        http.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                error("下载模型失败 HTTP ${response.code} $url")
            }
            val body = response.body ?: error("下载响应无 body")
            val total = body.contentLength()
            dest.parentFile?.mkdirs()
            body.byteStream().use { input ->
                FileOutputStream(dest).use { output ->
                    val buf = ByteArray(DEFAULT_BUFFER_SIZE)
                    var read = 0L
                    while (true) {
                        val n = input.read(buf)
                        if (n < 0) break
                        output.write(buf, 0, n)
                        read += n
                        onProgress(read, total)
                    }
                }
            }
        }
        Log.i(TAG, "downloaded ${dest.name} ${dest.length()} bytes")
    }

    private fun extractNeededFiles(archive: File, destDir: File) {
        destDir.mkdirs()
        val needed = setOf(
            SherpaZh14mModel.ENCODER,
            SherpaZh14mModel.DECODER,
            SherpaZh14mModel.JOINER,
            SherpaZh14mModel.TOKENS
        )
        archive.inputStream().use { fileIn ->
            BZip2CompressorInputStream(fileIn).use { bzip ->
                TarArchiveInputStream(bzip).use { tar ->
                    var entry = tar.nextEntry
                    while (entry != null) {
                        val name = File(entry.name).name
                        if (!entry.isDirectory && name in needed) {
                            File(destDir, name).outputStream().use { output ->
                                tar.copyTo(output)
                            }
                        }
                        entry = tar.nextEntry
                    }
                }
            }
        }
    }

    companion object {
        private const val TAG = "ModelStore"
    }
}
