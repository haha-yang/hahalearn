package com.haha.servicerouter.plugin

import org.objectweb.asm.Opcodes

/**
 * 与 Router / ServiceLoader 运行时类名对齐。插件不能依赖 Android 模块，
 * 扫描路径、宿主类、插桩方法、register 调用方式都集中在这里。
 */
internal object ScanSetting {

    const val CLASS_SUFFIX = ".class"
    const val VOID_DESC = "()V"
    const val REGISTER_METHOD = "register"
    const val REGISTER_DESC = "(Ljava/lang/String;)V"

    fun isClassFile(entryName: String): Boolean = entryName.endsWith(CLASS_SUFFIX)

    fun toDotted(internalName: String): String = internalName.replace('/', '.')

    object Router {
        const val TAG = "DOFRouter"
        const val OWNER = "com/haha/servicerouter/core/Router"
        const val CLASS_FILE = "$OWNER${CLASS_SUFFIX}"
        const val LOAD_METHOD = "loadRouterMap"
        const val SCAN_DIR = "com/haha/servicerouter/routes/"
        const val IROUTE_LOADER = "com/haha/servicerouter/interfaces/IRouteLoader"
        const val IINTERCEPTOR_LOADER = "com/haha/servicerouter/interfaces/IInterceptorLoader"
        const val LOAD_INTO = "loadInto"
        const val LOAD_INTO_DESC = "(Ljava/util/Map;)V"
        const val MAP_PUT = "put"
        const val JAVA_MAP = "java/util/Map"
        const val HASH_MAP_SUFFIX = "HashMap"

        val registerCommand: RegisterCommand = RegisterCommand.Instance(
            owner = OWNER,
            method = REGISTER_METHOD,
            descriptor = REGISTER_DESC
        )

        fun isHost(entryName: String): Boolean = entryName == CLASS_FILE

        fun matchesScan(entryName: String): Boolean =
            isClassFile(entryName) && entryName.startsWith(SCAN_DIR)

        fun isLoadInto(name: String, descriptor: String, access: Int): Boolean =
            name == LOAD_INTO &&
                    descriptor == LOAD_INTO_DESC &&
                    access and Opcodes.ACC_STATIC == 0

        fun isMapPut(owner: String, name: String): Boolean =
            name == MAP_PUT && (owner == JAVA_MAP || owner.endsWith(HASH_MAP_SUFFIX))
    }

    object Service {
        const val TAG = "DOFService"
        const val OWNER = "com/haha/service/impl/generated/ServiceLoaderInit"
        const val CLASS_FILE = "$OWNER${CLASS_SUFFIX}"
        const val LOAD_METHOD = "loadServiceMap"
        const val SCAN_DIR = "com/haha/service/impl/generated/service/"
        const val ISERVICE_INIT = "com/haha/service/impl/service/IServiceInit"
        const val INIT_METHOD = "init"
        const val LOADER_OWNER = "com/haha/service/impl/service/ServiceLoader"
        const val PUT_NAME = "put"

        val registerCommand: RegisterCommand = RegisterCommand.Static(
            owner = OWNER,
            method = REGISTER_METHOD,
            descriptor = REGISTER_DESC
        )

        fun isHost(entryName: String): Boolean = entryName == CLASS_FILE

        fun matchesScan(entryName: String): Boolean =
            isClassFile(entryName) && entryName.startsWith(SCAN_DIR)

        fun isInit(name: String, descriptor: String, access: Int): Boolean =
            name == INIT_METHOD &&
                    descriptor == VOID_DESC &&
                    access and Opcodes.ACC_STATIC == 0

        fun isPut(owner: String, name: String): Boolean =
            name == PUT_NAME && owner == LOADER_OWNER
    }
}
