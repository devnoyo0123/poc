package com.example.batch.tasklet

import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.stereotype.Component
import java.io.File
import java.time.Duration
import java.time.Instant

@Component
class DeleteOldFilesTasklet : Tasklet {

    override fun execute(contribution: StepContribution, chunkContext: ChunkContext): RepeatStatus {
        val jobParameters = chunkContext.stepContext.stepExecution.jobParameters
        
        val basePath = jobParameters.getString("basePath") ?: "/tmp/logs"
        val daysOld = jobParameters.getLong("daysOld") ?: 7L

        val baseDir = File(basePath)
        if (!baseDir.exists() || !baseDir.isDirectory) {
            println("⚠️  Directory not found: $basePath")
            return RepeatStatus.FINISHED
        }

        println("🔍 Scanning directory: $basePath")
        println("⏰ Deleting files older than $daysOld days")

        val cutoffTime = Instant.now().minus(Duration.ofDays(daysOld))
        var deletedCount = 0
        var skippedCount = 0

        baseDir.listFiles()?.forEach { file ->
            if (file.isFile) {
                val lastModified = file.lastModified()
                val fileLastModifiedInstant = Instant.ofEpochMilli(lastModified)

                if (fileLastModifiedInstant.isBefore(cutoffTime)) {
                    val deleted = file.delete()
                    if (deleted) {
                        println("🗑️  Deleted: ${file.name} (last modified: ${fileLastModifiedInstant})")
                        deletedCount++
                    } else {
                        println("❌ Failed to delete: ${file.name}")
                    }
                } else {
                    skippedCount++
                }
            }
        }

        println("\n✅ Summary:")
        println("   - Deleted: $deletedCount files")
        println("   - Skipped: $skippedCount files")

        return RepeatStatus.FINISHED
    }
}
