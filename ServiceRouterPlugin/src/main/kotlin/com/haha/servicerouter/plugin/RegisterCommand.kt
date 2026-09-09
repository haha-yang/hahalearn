package com.haha.servicerouter.plugin

import org.objectweb.asm.ClassReader
import org.objectweb.asm.ClassVisitor
import org.objectweb.asm.ClassWriter
import org.objectweb.asm.MethodVisitor
import org.objectweb.asm.Opcodes
import java.util.jar.JarOutputStream

/**
 * 往宿主方法 RETURN 前插入 `register(className)` 的两种调用方式：
 * - [Instance]：`this.register(name)`，对应 [Router]
 * - [Static]：`Xxx.register(name)`，对应 [ServiceLoaderInit]
 */
internal sealed class RegisterCommand {
    abstract val owner: String
    abstract val method: String
    abstract val descriptor: String

    fun emit(mv: MethodVisitor, className: String) {
        if (this is Instance) {
            mv.visitVarInsn(Opcodes.ALOAD, 0)
        }
        mv.visitLdcInsn(className)
        mv.visitMethodInsn(opcode, owner, method, descriptor, false)
    }

    private val opcode: Int
        get() = when (this) {
            is Instance -> Opcodes.INVOKESPECIAL
            is Static -> Opcodes.INVOKESTATIC
        }

    class Instance(
        override val owner: String,
        override val method: String,
        override val descriptor: String
    ) : RegisterCommand()

    class Static(
        override val owner: String,
        override val method: String,
        override val descriptor: String
    ) : RegisterCommand()
}

internal interface AutoRegisterChannel {
    fun claimHost(entryName: String, bytes: ByteArray): Boolean
    fun scan(entryName: String, bytes: ByteArray)
    fun finish(jos: JarOutputStream)
}

internal fun inspectClass(
    bytes: ByteArray,
    onMethod: (access: Int, name: String, descriptor: String) -> MethodVisitor?
): Pair<String, List<String>>? {
    var className: String? = null
    var interfaces: List<String> = emptyList()
    ClassReader(bytes).accept(object : ClassVisitor(Opcodes.ASM9) {
        override fun visit(
            version: Int,
            access: Int,
            name: String,
            signature: String?,
            superName: String?,
            ifaces: Array<out String>?
        ) {
            className = name
            interfaces = ifaces?.toList().orEmpty()
        }

        override fun visitMethod(
            access: Int,
            name: String,
            descriptor: String,
            signature: String?,
            exceptions: Array<out String>?
        ): MethodVisitor? = onMethod(access, name, descriptor)
    }, 0)
    val name = className ?: return null
    return name to interfaces
}

internal object RegisterInjector {

    fun inject(
        origin: ByteArray,
        methodName: String,
        methodDesc: String,
        command: RegisterCommand,
        classNames: List<String>
    ): ByteArray {
        if (classNames.isEmpty()) {
            return origin
        }
        val reader = ClassReader(origin)
        val writer = ClassWriter(reader, ClassWriter.COMPUTE_MAXS)
        reader.accept(object : ClassVisitor(Opcodes.ASM9, writer) {
            override fun visitMethod(
                access: Int,
                name: String,
                descriptor: String,
                signature: String?,
                exceptions: Array<out String>?
            ): MethodVisitor {
                val mv = super.visitMethod(access, name, descriptor, signature, exceptions)
                if (name != methodName || descriptor != methodDesc) {
                    return mv
                }
                return object : MethodVisitor(Opcodes.ASM9, mv) {
                    override fun visitInsn(opcode: Int) {
                        if (opcode == Opcodes.RETURN) {
                            classNames.forEach { command.emit(mv, it) }
                        }
                        super.visitInsn(opcode)
                    }
                }
            }
        }, 0)
        return writer.toByteArray()
    }
}
