package com.prsnl.storage

import com.prsnl.document.model.Element
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.io.File

class PageFileStorage(private val baseDir: File) {

    private val pagesDir = File(baseDir, "pages").apply { if (!exists()) mkdirs() }
    private val json = Json {
        prettyPrint = false
        ignoreUnknownKeys = true
    }

    fun savePageElements(pageId: String, elements: List<Element>): String {
        val file = File(pagesDir, "$pageId.json")
        val jsonText = json.encodeToString(elements)
        file.writeText(jsonText)
        return "pages/$pageId.json"
    }

    fun loadPageElements(relativePath: String): List<Element> {
        val file = File(baseDir, relativePath)
        if (!file.exists()) return emptyList()
        val jsonText = file.readText()
        return try {
            json.decodeFromString<List<Element>>(jsonText)
        } catch (e: Exception) {
            emptyList()
        }
    }

    fun deletePageElements(relativePath: String): Boolean {
        val file = File(baseDir, relativePath)
        return if (file.exists()) file.delete() else false
    }
}
