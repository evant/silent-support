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

    override fun transform(context: Context,
                           inputs: Collection<TransformInput>,
                           referencedInputs: Collection<TransformInput>,
                           outputProvider: TransformOutputProvider,
                           isIncremental: Boolean) {
        context.logging.captureStandardOutput(LogLevel.INFO)

        val outputDir = outputProvider.getContentLocation(name, inputTypes, scopes, Format.DIRECTORY)
        val cacheDir = File(project.buildDir, "intermediates/lint-cache")

        val variant = getVariant(outputDir)
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
                if (isIncremental) {
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

    private fun getVariant(outputDir: File): BaseVariant {
        // Extract the variant from the output path assuming it's in the form like:
        // - '*/intermediates/transforms/silentSupport/<VARIANT>
        // - '*/intermediates/transforms/silentSupport/<VARIANT>/folders/1/1/silentSupport
        // This will no longer be needed when the transform api supports per-variant transforms
        val parts = outputDir.toURI().path.split(Regex("/intermediates/transforms/$name/|/folders/[0-9]+"))

        if (parts.size < 2) {
            throw ProjectConfigurationException("Could not extract variant from output dir: $outputDir", null)
        }

        val variantName = parts[1]
        val variant = variantMap[variantName] ?:
                throw ProjectConfigurationException("Missing variant: ' + variantName + ' from output dir: $outputDir", null)

        return variant
    }

    private fun getClasspath(variant: BaseVariant): FileCollection {
        var classpathFiles = variant.javaCompile.classpath
        // bootClasspath isn't set until the last possible moment because it's expensive to look
        // up the android sdk path.
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

    override fun getScopes(): Set<QualifiedContent.Scope> = setOf(QualifiedContent.Scope.PROJECT)

    override fun getReferencedScopes(): Set<QualifiedContent.Scope> = emptySet()
}
