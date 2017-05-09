package me.tatarka.silentsupport.gradle

import com.android.build.gradle.AppExtension
import com.android.build.gradle.AppPlugin
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.Task

class SilentSupportPlugin : Plugin<Project> {
    override fun apply(project: Project) {
        project.plugins.withType(AppPlugin::class.java) { plugin ->
            val android = project.extensions.getByType(AppExtension::class.java)
            val transform = SilentSupportTransform(project)
            val tasks = HashSet<Task>()
            android.registerTransform(transform, tasks)
            android.applicationVariants.all { variant ->
                transform.putVariant(variant)

                val task = transform.getOrCreateSupportMetadataTask(variant)
                if (task != null) {
                    tasks.add(task)
                }
            }

            project.dependencies.add(
                    "compile",
                    if (isReleased) "me.tatarka.silentsupport:silent-support-lib:$version"
                    else project.project(":silent-support-lib")
            )

            android.lintOptions {
                it.disable("NewApi")
                it.enable("NewApiSupport")
            }
        }
    }

    private val isReleased: Boolean
        get() = javaClass.getPackage().implementationVersion != null

    private val version: String
        get() = javaClass.getPackage().implementationVersion
}

