package com.example.batch.chunk.reader

import org.springframework.batch.item.ItemReader
import org.springframework.batch.item.file.FlatFileItemReader
import org.springframework.batch.item.file.builder.FlatFileItemReaderBuilder
import org.springframework.core.io.PathResource
import org.springframework.stereotype.Component

@Component
class FileItemReader {

    fun reader(inputFilePath: String): FlatFileItemReader<Map<String, String>> {
        return FlatFileItemReaderBuilder<Map<String, String>>()
            .name("fileItemReader")
            .resource(PathResource(inputFilePath))
            .delimited()
            .delimiter(",")
            .names("id", "name", "email", "score")
            .fieldSetMapper { fieldSet ->
                mapOf(
                    "id" to fieldSet.readString("id"),
                    "name" to fieldSet.readString("name"),
                    "email" to fieldSet.readString("email"),
                    "score" to fieldSet.readString("score")
                )
            }
            .build()
    }
}
