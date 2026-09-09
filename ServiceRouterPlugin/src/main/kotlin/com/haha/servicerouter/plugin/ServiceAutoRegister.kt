package com.haha.servicerouter.plugin

import org.gradle.api.GradleException
import org.gradle.api.logging.Logger
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import org.objectweb.asm.Type
import java.util.jar.JarOutputStream

internal class ServiceAutoRegister(
    private val logger: Logger
) : AutoRegisterChannel {
    private val serviceInits = linkedSetOf<String>()
    private val servicePuts = mutableListOf<ServicePut>()
    private var hostBytes: ByteArray? = null

    override fun claimHost(entryName: String, bytes: ByteArray): Boolean {
        if (!ScanSetting.Service.isHost(entryName)) {
            return false
        }
        hostBytes = bytes
        return true
    }

    override fun scan(entryName: String, bytes: ByteArray) {
        if (!ScanSetting.Service.matchesScan(entryName)) {
            return
        }
        val (className, interfaces) = inspectClass(bytes) { access, name, descriptor ->
            if (ScanSetting.Service.isInit(name, descriptor, access)) {
                PutCollector(servicePuts)
            } else {
                null
            }
        } ?: return
        if (ScanSetting.Service.ISERVICE_INIT in interfaces) {
            serviceInits.add(ScanSetting.toDotted(className))
        }
    }

    override fun finish(jos: JarOutputStream) {
        checkConflicts()
        logger.lifecycle("[${ScanSetting.Service.TAG}] Auto-register inits: $serviceInits")
        val origin = hostBytes ?: return
        if (serviceInits.isEmpty()) {
            logger.lifecycle(
                "[${ScanSetting.Service.TAG}] no IServiceInit found, keep original ${ScanSetting.Service.LOAD_METHOD}"
            )
            jos.putClass(ScanSetting.Service.CLASS_FILE, origin)
            return
        }
        val rewritten = RegisterInjector.inject(
            origin,
            ScanSetting.Service.LOAD_METHOD,
            ScanSetting.VOID_DESC,
            ScanSetting.Service.registerCommand,
            serviceInits.toList()
        )
        logger.lifecycle(
            "[${ScanSetting.Service.TAG}] injected ${serviceInits.size} ${ScanSetting.REGISTER_METHOD}() " +
                    "into ServiceLoaderInit.${ScanSetting.Service.LOAD_METHOD}"
        )
        jos.putClass(ScanSetting.Service.CLASS_FILE, rewritten)
    }

    private fun checkConflicts() {
        val byKey = linkedMapOf<String, ServicePut>()
        val byDefault = linkedMapOf<String, ServicePut>()
        servicePuts.forEach { put ->
            if (put.key.isNotEmpty()) {
                val mapKey = "${put.interfaceName}#${put.key}"
                val prev = byKey.put(mapKey, put)
                if (prev != null && prev.implName != put.implName) {
                    throw GradleException(
                        "Service key conflict: interface=${put.interfaceName} key=${put.key} " +
                                "${prev.implName} vs ${put.implName}"
                    )
                }
            }
            if (put.defaultImpl) {
                val prev = byDefault.put(put.interfaceName, put)
                if (prev != null && prev.implName != put.implName) {
                    throw GradleException(
                        "Service defaultImpl conflict: interface=${put.interfaceName} " +
                                "${prev.implName} vs ${put.implName}"
                    )
                }
            }
        }
    }

    private class PutCollector(
        private val out: MutableList<ServicePut>
    ) : MethodVisitor(Opcodes.ASM9) {
        private val args = mutableListOf<Any?>()

        override fun visitLdcInsn(value: Any?) {
            args.add(value)
        }

        override fun visitInsn(opcode: Int) {
            when (opcode) {
                Opcodes.ICONST_0 -> args.add(false)
                Opcodes.ICONST_1 -> args.add(true)
                Opcodes.ICONST_2 -> args.add(2)
                Opcodes.ICONST_3 -> args.add(3)
                Opcodes.ICONST_4 -> args.add(4)
                Opcodes.ICONST_5 -> args.add(5)
                Opcodes.ICONST_M1 -> args.add(-1)
            }
        }

        override fun visitIntInsn(opcode: Int, operand: Int) {
            if (opcode == Opcodes.BIPUSH || opcode == Opcodes.SIPUSH) {
                args.add(operand)
            }
        }

        override fun visitMethodInsn(
            opcode: Int,
            owner: String,
            name: String,
            descriptor: String,
            isInterface: Boolean
        ) {
            if (ScanSetting.Service.isPut(owner, name)) {
                val types = args.filterIsInstance<Type>()
                val strings = args.filterIsInstance<String>()
                val bools = args.filterIsInstance<Boolean>()
                if (types.size >= 2) {
                    out.add(
                        ServicePut(
                            interfaceName = types[0].className,
                            implName = types[1].className,
                            key = strings.getOrNull(0).orEmpty(),
                            defaultImpl = bools.getOrNull(1) ?: false
                        )
                    )
                }
            }
            args.clear()
        }
    }

    private data class ServicePut(
        val interfaceName: String,
        val implName: String,
        val key: String,
        val defaultImpl: Boolean
    )
}
