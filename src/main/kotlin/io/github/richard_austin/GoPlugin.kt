package io.github.richard_austin

import io.github.richard_austin.tasks.BuildTask
import io.github.richard_austin.tasks.DownloadGoTask
import io.github.richard_austin.tasks.InstallGoTask
import io.github.richard_austin.tasks.TestTask
import io.github.richard_austin.utils.PluginUtils.ext
import io.github.richard_austin.utils.PluginUtils.toInt
import org.gradle.api.Action
import org.gradle.api.tasks.util.PatternFilterable

class GoPlugin : org.gradle.api.Plugin<org.gradle.api.Project> {
    override fun apply(project: org.gradle.api.Project) {
// Create plugin extension
        project.extensions.create(GO_PLUGIN_EXTENSION, PluginExtension::class.java)

// Apply the base plugin
        project.plugins.apply("base")

// Named configuration cache safe lookup (returns TaskProvider)
        val checkTask = project.tasks.named("check")
        val assembleTask = project.tasks.named("assemble")

// Register download task lazily
        val downloadGoTask = project.tasks.register(GO_DOWNLOAD_TASK, DownloadGoTask::class.java) { downloadTask ->
            downloadTask.group = GO_PLUGIN_GROUP
            downloadTask.description = "Download Golang"
            downloadTask.goVersion.set(project.provider { project.ext.goVersion })
            downloadTask.defaultGoVersion.set(project.provider { project.ext.defaultGoVersion })
            downloadTask.rootDir.set(project.rootDir)
        }

// Register install task lazily
        val installGoTask = project.tasks.register(GO_INSTALL_TASK, InstallGoTask::class.java) { installTask ->
            installTask.group = GO_PLUGIN_GROUP
            installTask.description = "Install Golang"
            installTask.goVersion.set(project.provider { project.ext.goVersion })
            installTask.defaultGoVersion.set(project.provider { project.ext.defaultGoVersion })
            installTask.rootDir.set(project.rootDir)

            // Explicit parameter targets ensure task context resolves cleanly
            installTask.dependsOn(downloadGoTask)
        }

// Register build tasks lazily based on collections
        project.ext.os.forEach { osType ->
            project.ext.arch.forEach { archType ->
                // Safe standard string mutations bypassing internal package dependencies
                val osName = osType.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                val archName = archType.replaceFirstChar { if (it.isLowerCase()) it.titlecase() else it.toString() }
                val taskName = "goBuild${osName}${archName}"

                val buildTask = project.tasks.register(taskName, BuildTask::class.java) { goBuildTask ->
                    goBuildTask.group = GO_PLUGIN_GROUP
                    goBuildTask.description = "Build${osType}${archType}"

                    // Wire configuration cache safe values and providers
                    goBuildTask.os.set(osType)
                    goBuildTask.arch.set(archType)
                    goBuildTask.ldFlagsConfig.set(project.provider { project.ext.ldFlags })
                    goBuildTask.cgoEnabledInt.set(project.provider { project.ext.cgoEnabled.toInt() })
                    goBuildTask.extraBuildArgs.set(project.provider { project.ext.extraBuildArgs })
                    goBuildTask.projectRootDir.set(project.rootDir)

                    // Forces action execution inside Gradle's pattern file tree matching DSL block scope
                    goBuildTask.inputFiles.from(
                        project.fileTree(project.rootDir).matching(object : Action<PatternFilterable> {
                            override fun execute(pattern: PatternFilterable) {
                                pattern.include("**/**.go")
                            }
                        })
                    )

                    // Calculate module name lazily
                    val moduleNameProvider = project.provider {
                        if (project.ext.moduleName.isEmpty()) project.name else project.ext.moduleName
                    }

                    goBuildTask.outputBinary.set(moduleNameProvider.map { name -> "${name}-${osType}-${archType}" })

                    // Fixed: Mapped RegularFile to Java File using .map { it.asFile } to match type signatures
                    goBuildTask.outputBinaryFile.set(
                        project.layout.buildDirectory.file(goBuildTask.outputBinary.map { it }).map { it.asFile }
                    )
                }

                // Fixed: Explicit naming forces Kotlin to clear receiver targets
                assembleTask.configure { assemble ->
                    assemble.dependsOn(buildTask)
                }
            }
        }

// Register test task lazily
        val testTask = project.tasks.register("test", TestTask::class.java) { tTask ->
            tTask.group = GO_PLUGIN_GROUP
            tTask.description = "Run tests"
        }

// Fixed: Explicit naming forces Kotlin to clear receiver targets
        checkTask.configure { check ->
            check.dependsOn(testTask)
        }

    }

}
