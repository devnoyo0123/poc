package com.example.batch.tasklet

import com.example.batch.model.Difficulty
import org.springframework.batch.core.StepContribution
import org.springframework.batch.core.scope.context.ChunkContext
import org.springframework.batch.core.step.tasklet.Tasklet
import org.springframework.batch.repeat.RepeatStatus
import org.springframework.beans.factory.annotation.Value
import org.springframework.batch.core.configuration.annotation.StepScope
import org.springframework.stereotype.Component
import java.time.LocalDate

@Component
@StepScope
class ParameterDemoTasklet : Tasklet {

    @Value("#{jobParameters['name']}")
    private lateinit var name: String

    @Value("#{jobParameters['age']}")
    private var age: Int? = null

    @Value("#{jobParameters['targetDate']}")
    private var targetDate: LocalDate? = null

    @Value("#{jobParameters['difficulty']}")
    private var difficulty: Difficulty? = null

    override fun execute(contribution: StepContribution, chunkContext: ChunkContext): RepeatStatus {
        println("\n" + "=".repeat(50))
        println("📋 JobParameters Demonstration")
        println("=".repeat(50))
        
        println("\n🔍 Individual Parameters:")
        println("   Name: ${if (::name.isInitialized) name else "NOT PROVIDED"}")
        println("   Age: ${age ?: "NOT PROVIDED"}")
        println("   Target Date: ${targetDate ?: "NOT PROVIDED"}")
        println("   Difficulty: ${difficulty ?: "NOT PROVIDED"}")

        println("\n🎯 Usage Examples:")
        if (::name.isInitialized) {
            println("   Hello, $name!")
            if (age != null) {
                val birthYear = LocalDate.now().year - (age ?: 0)
                println("   You were born around $birthYear")
            }
        }

        if (targetDate != null) {
            val daysUntil = java.time.temporal.ChronoUnit.DAYS.between(
                LocalDate.now(),
                targetDate
            )
            println("   Days until target: $daysUntil days")
        }

        if (difficulty != null) {
            val message = when (difficulty!!) {
                Difficulty.EASY -> "😊 This will be a breeze!"
                Difficulty.MEDIUM -> "💪 Moderate challenge ahead!"
                Difficulty.HARD -> "🔥 Prepare for a tough fight!"
                Difficulty.INSANE -> "💀 You might not survive!"
            }
            println("   Mission Difficulty: $message")
        }

        println("\n" + "=".repeat(50))
        return RepeatStatus.FINISHED
    }
}
