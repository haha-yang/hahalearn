package com.haha.servicerouter.plugin

import com.android.build.api.artifact.ScopedArtifact
import com.android.build.api.variant.AndroidComponentsExtension
import com.android.build.api.variant.ScopedArtifacts
import org.gradle.api.Plugin
import org.gradle.api.Project

/**
 * 打包期自动注册插件（对齐 ARouter arouter-register）：
 * 扫描 RouteLoader / InterceptorLoader / IServiceInit，
 * ASM 注入 Router.loadRouterMap 与 ServiceLoaderInit.loadServiceMap。
 */
class AutoRegisterPlugin : Plugin<Project> {

    override fun apply(project: Project) {
        val androidComponents =
            project.extensions.findByType(AndroidComponentsExtension::class.java)
        if (androidComponents == null) {
            project.logger.warn("[AutoRegister] skip plugin: AndroidComponentsExtension not found")
            return
        }
        androidComponents.onVariants { variant ->
            val taskName = "dofAutoRegister${variant.name.replaceFirstChar { it.uppercase() }}"
            val taskProvider =
                project.tasks.register(taskName, AutoRegisterTask::class.java) { task ->
                    task.group = "dof"
                    task.description =
                        "Inject Router / ServiceLoader auto-register for ${variant.name}"
                }
            variant.artifacts
                .forScope(ScopedArtifacts.Scope.ALL)
                .use(taskProvider)
                .toTransform(
                    ScopedArtifact.CLASSES,
                    AutoRegisterTask::allJars,
                    AutoRegisterTask::allDirectories,
                    AutoRegisterTask::output
                )
        }
    }
}
