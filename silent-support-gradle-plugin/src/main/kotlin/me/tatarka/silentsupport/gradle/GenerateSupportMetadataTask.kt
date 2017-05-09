package me.tatarka.silentsupport.gradle

import me.tatarka.silentsupport.SupportCompat
import me.tatarka.silentsupport.SupportMetadataProcessor
import org.gradle.api.DefaultTask
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFile
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction
import java.io.File

open class GenerateSupportMetadataTask : DefaultTask() {
    @InputFile
    lateinit var supportCompatFile: File
    @Input
    lateinit var supportCompatVersion: String
    @OutputDirectory
    lateinit var outputDir: File

    val metadataFile: File?
        get() = processor.metadataFile

    private val processor: SupportMetadataProcessor by lazy {
        SupportMetadataProcessor(SupportCompat(supportCompatFile, supportCompatVersion), outputDir)
    }

    @TaskAction
    fun action() {
        processor.process()
    }
}

