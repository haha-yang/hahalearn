package com.haha.servicerouter.plugin

import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import java.util.jar.JarOutputStream

internal class RouterAutoRegister(
    private val logger: Logger
) : AutoRegisterChannel {
    private val routeLoaders = linkedSetOf<String>()
    private val interceptorLoaders = linkedSetOf<String>()
    private val routePuts = mutableListOf<RoutePut>()
    private var hostBytes: ByteArray? = null

    override fun claimHost(entryName: String, bytes: ByteArray): Boolean {
        if (!ScanSetting.Router.isHost(entryName)) {
            return false
        }
        hostBytes = bytes
        return true
    }

    override fun scan(entryName: String, bytes: ByteArray) {
        if (!ScanSetting.Router.matchesScan(entryName)) {
            return
        }
        val (className, interfaces) = inspectClass(bytes) { access, name, descriptor ->
            if (ScanSetting.Router.isLoadInto(name, descriptor, access)) {
                RoutePutCollector(routePuts)
            } else {
                null
            }
        } ?: return
        val dotted = ScanSetting.toDotted(className)
        when {
            ScanSetting.Router.IROUTE_LOADER in interfaces -> routeLoaders.add(dotted)
            ScanSetting.Router.IINTERCEPTOR_LOADER in interfaces -> interceptorLoaders.add(dotted)
        }
    }

    override fun finish(jos: JarOutputStream) {
        checkConflicts()
        logger.lifecycle("[${ScanSetting.Router.TAG}] Auto-register routes: $routeLoaders")
        logger.lifecycle(
            "[${ScanSetting.Router.TAG}] Auto-register interceptors: $interceptorLoaders"
        )
        val origin = hostBytes ?: return
        val injectNames = ArrayList<String>(routeLoaders.size + interceptorLoaders.size).apply {
            addAll(routeLoaders)
            addAll(interceptorLoaders)
        }
        if (injectNames.isEmpty()) {
            logger.lifecycle(
                "[${ScanSetting.Router.TAG}] no loader found, keep original ${ScanSetting.Router.LOAD_METHOD}"
            )
            jos.putClass(ScanSetting.Router.CLASS_FILE, origin)
            return
        }
        val rewritten = RegisterInjector.inject(
            origin,
            ScanSetting.Router.LOAD_METHOD,
            ScanSetting.VOID_DESC,
            ScanSetting.Router.registerCommand,
            injectNames
        )
        logger.lifecycle(
            "[${ScanSetting.Router.TAG}] injected ${injectNames.size} ${ScanSetting.REGISTER_METHOD}() " +
                    "into Router.${ScanSetting.Router.LOAD_METHOD}"
        )
        jos.putClass(ScanSetting.Router.CLASS_FILE, rewritten)
    }

    private fun checkConflicts() {
        val byPath = linkedMapOf<String, RoutePut>()
        routePuts.forEach { put ->
            if (put.path.isEmpty()) {
                return@forEach
            }
            val prev = byPath.put(put.path, put)
            if (prev != null && prev.implName != put.implName) {
                throw GradleException(
                    "Route path conflict: path=${put.path} " +
                            "${prev.implName} vs ${put.implName}"
                )
            }
        }
    }

    private class RoutePutCollector(
        private val out: MutableList<RoutePut>
    ) : MethodVisitor(Opcodes.ASM9) {
        private val strings = mutableListOf<String>()
        private var lastType: Type? = null

        override fun visitLdcInsn(value: Any?) {
            when (value) {
                is String -> strings.add(value)
                is Type -> lastType = value
            }
        }

        override fun visitMethodInsn(
            opcode: Int,
            owner: String,
            name: String,
            descriptor: String,
            isInterface: Boolean
        ) {
            if (ScanSetting.Router.isMapPut(owner, name)) {
                val path = strings.firstOrNull().orEmpty()
                val implName = lastType?.className.orEmpty()
                if (path.isNotEmpty() && implName.isNotEmpty()) {
                    out.add(RoutePut(path = path, implName = implName))
                }
            }
            strings.clear()
            lastType = null
        }
    }

    private data class RoutePut(
        val path: String,
        val implName: String
    )
}
