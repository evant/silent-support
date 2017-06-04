package me.tatarka.silentsupport.gradle

import com.android.build.api.transform.*
import com.android.build.gradle.api.BaseVariant
import com.android.builder.model.Variant
import me.tatarka.silentsupport.SilentSupportProcessor
import me.tatarka.silentsupport.SupportCompat
import org.gradle.api.Project
import org.gradle.api.ProjectConfigurationException
import org.gradle.api.file.FileCollection
import org.gradle.api.logging.LogLevel
import java.io.File

class SilentSupportTransform(private val project: Project) : Transform() {

    private val variantMap = HashMap<String, BaseVariant>()
    private val supportMetadataMap = HashMap<SupportCompat, GenerateSupportMetadataTask>()

    override fun getName(): String = "silentSupport"

    override fun transform(transformInvocation: TransformInvocation) {
        val context = transformInvocation.context
        val outputProvider = transformInvocation.outputProvider
        val inputs = transformInvocation.inputs
        context.logging.captureStandardOutput(LogLevel.INFO)

        val outputDir = outputProvider.getContentLocation(name, inputTypes, scopes, Format.DIRECTORY)
        val cacheDir = File(project.buildDir, "intermediates/lint-cache")

        val variant = variantMap[context.variantName]!!
        val classpath = getClasspath(variant)
        val apiLevel = variant.mergedFlavor.minSdkVersion.apiLevel
        val generateSupportMetadataTask = getOrCreateSupportMetadataTask(variant)

        if (generateSupportMetadataTask == null) {
            // No support lib, just copy to output without processing
            project.copy {
                it.from(inputs.flatMap { it.directoryInputs }.map { it.file })
                it.to(outputDir)
            }
            return
        }

        val processor = SilentSupportProcessor(
                classpath.files,
                generateSupportMetadataTask.metadataFile!!,
                apiLevel,
                cacheDir)

        for (input in inputs) {
            for (dirInput in input.directoryInputs) {
                val inputDir = dirInput.file
                if (transformInvocation.isIncremental) {
                    for ((file, status) in dirInput.changedFiles) {
                        if (status == Status.ADDED || status == Status.CHANGED) {
                            transformFile(processor, apiLevel, inputDir, outputDir, file)
                        } else if (status == Status.REMOVED) {
                            toOutput(inputDir, outputDir, file).delete()
                        }
                    }
                } else {
                    outputProvider.deleteAll()
                    for (file in dirInput.file.walk()) {
                        if (file.extension == "class") {
                            transformFile(processor, apiLevel, inputDir, outputDir, file)
                        } else if (!file.isDirectory) {
                            val output = toOutput(inputDir, outputDir, file)
                            output.parentFile.mkdirs()
                            file.copyTo(output)
                        }
                    }
                }
            }
        }
    }

    fun putVariant(variant: BaseVariant) {
        variantMap[variant.name] = variant
    }

    fun getOrCreateSupportMetadataTask(variant: BaseVariant): GenerateSupportMetadataTask? {
        val config = project.configurations.findByName("compile")
        val supportCompat = SupportCompat.fromClasspath(config.files) ?: return null
        return supportMetadataMap.getOrPut(supportCompat) {
            project.tasks.create("generateSupportMetadata${supportCompat.version}", GenerateSupportMetadataTask::class.java) {
                it.supportCompatFile = supportCompat.file
                it.supportCompatVersion = supportCompat.version
                it.outputDir = File(project.buildDir, "intermediates/lint-cache")
            }
        }
    }

    private fun transformFile(processor: SilentSupportProcessor, apiLevel: Int, inputDir: File, outputDir: File, file: File) {
        val outputFile = toOutput(inputDir, outputDir, file)
        outputFile.parentFile.mkdirs()
        file.inputStream().buffered().use { input ->
            outputFile.outputStream().buffered().use { output ->
                processor.process(input, apiLevel, output)
            }
        }
    }

    private fun toOutput(inputDir: File, outputDir: File, file: File): File =
            outputDir.toPath().resolve(inputDir.toPath().relativize(file.toPath())).toFile()

    private fun getClasspath(variant: BaseVariant): FileCollection {
        @Suppress("DEPRECATION")
        var classpathFiles = variant.javaCompile.classpath
        // bootClasspath isn't set until the last possible moment because it's expensive to look
        // up the android sdk path.
        @Suppress("DEPRECATION")
        val bootClasspath = variant.javaCompile.options.bootClasspath
        if (bootClasspath != null) {
            classpathFiles = classpathFiles.plus(project.files(bootClasspath.split(File.pathSeparator)))
        } else {
            // If this is null it means the javaCompile task didn't need to run, however, we still
            // need to run but can't without the bootClasspath. Just fail and ask the user to rebuild.
            throw ProjectConfigurationException("Unable to obtain the bootClasspath. This may happen if your javaCompile tasks didn't run but $name did. You must rebuild your project or otherwise force javaCompile to run.", null)
        }
        return classpathFiles
    }

    override fun isIncremental(): Boolean = true

    override fun getInputTypes(): Set<QualifiedContent.ContentType> = setOf(QualifiedContent.DefaultContentType.CLASSES)

    override fun getScopes(): MutableSet<QualifiedContent.Scope> = mutableSetOf(QualifiedContent.Scope.PROJECT)

    override fun getReferencedScopes(): MutableSet<QualifiedContent.Scope> = mutableSetOf()
}
