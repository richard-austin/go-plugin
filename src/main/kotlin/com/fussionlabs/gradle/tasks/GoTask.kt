package com.fussionlabs.gradle.tasks

import com.fussionlabs.gradle.GO_BINARY
import com.fussionlabs.gradle.GO_INSTALL_TASK
import com.fussionlabs.gradle.GO_SETUP_DIR
import com.fussionlabs.gradle.GRADLE_FILES_DIR
import com.fussionlabs.gradle.utils.PluginUtils.ext
import com.fussionlabs.gradle.utils.PluginUtils.goBinary
import javax.inject.Inject
import org.gradle.api.model.ObjectFactory
import org.gradle.api.tasks.AbstractExecTask
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.Internal
import org.gradle.process.internal.ExecActionFactory

// 1. Declare the class as abstract to let Gradle safely proxy the fields
@CacheableTask
abstract class GoTask : AbstractExecTask<GoTask>(GoTask::class.java) {

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
        dependsOn(GO_INSTALL_TASK)
    }

    override fun exec()
    {
        val goBinary = goBinary(project)
        val goVersion = project.ext.goVersion.ifEmpty {
            project.ext.defaultGoVersion
        }

        // Configure GOROOT (if needed)
        if (goBinary != GO_BINARY) {
            goTaskEnv["GOROOT"] = "${project.rootDir}/$GRADLE_FILES_DIR/$GO_SETUP_DIR-$goVersion/go"
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
