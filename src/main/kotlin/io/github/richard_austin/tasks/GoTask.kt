// Modified by Richard Austin in 2026
package io.github.richard_austin.tasks

import io.github.richard_austin.GO_BINARY
import io.github.richard_austin.GO_INSTALL_TASK
import io.github.richard_austin.GO_SETUP_DIR
import io.github.richard_austin.GRADLE_FILES_DIR
import io.github.richard_austin.utils.PluginUtils.ext
import io.github.richard_austin.utils.PluginUtils.goBinary
import javax.inject.Inject
import org.gradle.api.model.ObjectFactory
import org.gradle.api.provider.Property
import org.gradle.api.tasks.AbstractExecTask
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.api.tasks.TaskAction
import org.gradle.process.internal.ExecActionFactory
import java.io.File

// 1. Declare the class as abstract to let Gradle safely proxy the fields
@CacheableTask
abstract class GoTask : AbstractExecTask<GoTask>(GoTask::class.java) {
    @get:Input abstract val goVersion: Property<String>
    @get:Input abstract val defaultGoVersion: Property<String>
    @get:Input abstract val rootDir: Property<File>

    // 2. Delegate getters to Gradle's internal service injection container
    @Inject
    abstract override fun getObjectFactory(): ObjectFactory

    @Inject
    abstract override fun getExecActionFactory(): ExecActionFactory

    @Input
    var goTaskArgs: MutableList<String> = mutableListOf()

    @Internal
    var goTaskEnv: MutableMap<String, Any> = mutableMapOf()

    init {
        goVersion.set(project.ext.goVersion)
        defaultGoVersion.set(project.ext.defaultGoVersion)
        rootDir.set(project.rootDir)
        dependsOn(GO_INSTALL_TASK)
    }

    @TaskAction
    override fun exec()
    {
        val golangVersion = goVersion.get().ifEmpty {
            defaultGoVersion.get()
        }
        val goBinary = goBinary(goVersion.get(), defaultGoVersion.get(), rootDir.get())
        logger.info("goBinary: $goBinary")
        logger.info("goVersion: $golangVersion")
        // Configure GOROOT (if needed)
        if (goBinary != GO_BINARY) {
            goTaskEnv["GOROOT"] = "${rootDir.get()}/$GRADLE_FILES_DIR/$GO_SETUP_DIR-$golangVersion/go"
        }
        executable = goBinary
        args = goTaskArgs
        goTaskEnv.forEach { (key, value) ->
            environment(key, value)
        }

        logger.info("goTaskEnv: $goTaskEnv")
        logger.info("goTaskArgs: $goTaskArgs")

        super.exec()
    }
}
