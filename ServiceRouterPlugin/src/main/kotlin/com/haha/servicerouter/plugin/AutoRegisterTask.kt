package com.haha.servicerouter.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.file.Directory
import org.gradle.api.file.RegularFile
import org.gradle.api.file.RegularFileProperty
import org.gradle.api.provider.ListProperty
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.PathSensitive
import org.gradle.api.tasks.PathSensitivity
import org.gradle.api.tasks.TaskAction
import java.io.BufferedOutputStream
import java.io.FileOutputStream
import java.util.jar.JarEntry
import java.util.jar.JarFile
import java.util.jar.JarOutputStream

/**
 * 单次遍历全部 CLASSES，按通道处理 Router 与 ServiceLoader：
 * 1. [RouterAutoRegister]：收集 Route / Interceptor Loader，改写 Router.loadRouterMap
 * 2. [ServiceAutoRegister]：收集 IServiceInit，改写 ServiceLoaderInit.loadServiceMap
 */
abstract class AutoRegisterTask : DefaultTask() {

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val allJars: ListProperty<RegularFile>

    @get:InputFiles
    @get:PathSensitive(PathSensitivity.RELATIVE)
    abstract val allDirectories: ListProperty<Directory>

    @get:OutputFile
    abstract val output: RegularFileProperty

    @TaskAction
    fun taskAction() {
        val channels: List<AutoRegisterChannel> = listOf(
            RouterAutoRegister(logger),
            ServiceAutoRegister(logger)
        )
        val written = hashSetOf<String>()

        JarOutputStream(BufferedOutputStream(FileOutputStream(output.get().asFile))).use { jos ->
            allJars.get().forEach { file ->
                JarFile(file.asFile).use { jar ->
                    jar.entries().asSequence().forEach { entry ->
                        if (entry.isDirectory || !written.add(entry.name)) {
                            return@forEach
                        }
                        val bytes = jar.getInputStream(entry).use { it.readBytes() }
                        handleClass(entry.name, bytes, channels, jos)
                    }
                }
            }
            allDirectories.get().forEach { dir ->
                dir.asFile.walkTopDown().filter { it.isFile }.forEach { file ->
                    val relative = file.relativeTo(dir.asFile).invariantSeparatorsPath
                    if (!written.add(relative)) {
                        return@forEach
                    }
                    handleClass(relative, file.readBytes(), channels, jos)
                }
            }
            channels.forEach { it.finish(jos) }
        }
    }

    private fun handleClass(
        entryName: String,
        bytes: ByteArray,
        channels: List<AutoRegisterChannel>,
        jos: JarOutputStream
    ) {
        if (channels.any { it.claimHost(entryName, bytes) }) {
            return
        }
        channels.forEach { it.scan(entryName, bytes) }
        jos.putClass(entryName, bytes)
    }
}

internal fun JarOutputStream.putClass(name: String, bytes: ByteArray) {
    putNextEntry(JarEntry(name))
    write(bytes)
    closeEntry()
}
