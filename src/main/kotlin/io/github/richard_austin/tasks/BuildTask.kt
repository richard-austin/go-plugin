package io.github.richard_austin.tasks

import org.gradle.api.file.ConfigurableFileCollection
import org.gradle.api.provider.ListProperty
import org.gradle.api.provider.MapProperty
import org.gradle.api.provider.Property
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.Classpath
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.OutputFile
import java.io.File

@CacheableTask
abstract class BuildTask : GoTask() {

    @get:Input
    abstract val os: Property<String>

    @get:Input
    abstract val arch: Property<String>

    @get:Input
    abstract val ldFlagsConfig: MapProperty<String, String>

    @get:Input
    abstract val cgoEnabledInt: Property<Int>

    @get:Input
    abstract val extraBuildArgs: ListProperty<String>

    @get:Input
    abstract val projectRootDir: Property<File>

    @get:Classpath
    @get:InputFiles
    abstract val inputFiles: ConfigurableFileCollection

    @get:Input
    abstract val outputBinary: Property<String>

    @get:OutputFile
    abstract val outputBinaryFile: Property<File>

    override fun exec() {
// Setup task environment safely using properties
        goTaskEnv["GOOS"] = os.get()
        goTaskEnv["GOARCH"] = arch.get()
        goTaskEnv["CGO_ENABLED"] = cgoEnabledInt.get().toString()
// Setup build dir safely using injected DirectoryProperty
        val binaryFile = outputBinaryFile.get()
        binaryFile.parentFile.mkdirs()

// Configure build args
        val buildArgs = mutableListOf("build")

// Configure ldFlags
        var ldFlags = ""
        val flagsMap = ldFlagsConfig.get()
        if (flagsMap.isNotEmpty()) {
            ldFlags += "-ldflags="
            flagsMap.forEach { (key, value) ->
                ldFlags += " -X '$key=$value' "
            }
            buildArgs.add(ldFlags)
        }

// Configure output path
        buildArgs.addAll(listOf("-o", binaryFile.absolutePath))

// Add extraBuildArgs safely
        extraBuildArgs.get().forEach { testArg ->
            buildArgs.add(testArg)
        }

// Configure Project DIR
        buildArgs.add(projectRootDir.get().absolutePath)

        goTaskArgs = buildArgs
        super.exec()

    }

}
