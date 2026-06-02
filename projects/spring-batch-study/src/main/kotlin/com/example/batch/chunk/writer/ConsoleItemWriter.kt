package com.example.batch.chunk.writer

import org.springframework.batch.item.Chunk
import org.springframework.batch.item.ItemWriter
import org.springframework.stereotype.Component

@Component
class ConsoleItemWriter : ItemWriter<Map<String, String>> {

    override fun write(chunk: Chunk<out Map<String, String>>) {
        val items = chunk.items
        println("\n" + "=".repeat(60))
        println("📝 Writing chunk of ${items.size} items")
        println("=".repeat(60))

        items.forEach { item ->
            println("✅ ID: ${item["id"]}")
            println("   Name: ${item["name"]}")
            println("   Email: ${item["email"]}")
            println("   Score: ${item["score"]}")
            println("   Grade: ${item["grade"]}")
            println()
        }

        println("💾 Total items in this chunk: ${items.size}")
        println("=".repeat(60) + "\n")
    }
}
