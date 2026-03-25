package com.hits.recipebook.server

import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json
import java.io.File

class JsonFileStore<T>(
    private val file: File,
    private val serializer: kotlinx.serialization.KSerializer<T>,
) {
    private val json = Json { prettyPrint = true; ignoreUnknownKeys = true }

    fun readAll(): MutableList<T> {
        if (!file.exists()) return mutableListOf()
        val content = file.readText()
        if (content.isBlank()) return mutableListOf()
        return json.decodeFromString(ListSerializer(serializer), content).toMutableList()
    }

    fun writeAll(items: List<T>) {
        file.parentFile?.mkdirs()
        file.writeText(json.encodeToString(ListSerializer(serializer), items))
    }
}

class RecipeRepository(dataDir: File = File("server-data")) {
    private val productStore = JsonFileStore(File(dataDir, "products.json"), Product.serializer())
    private val dishStore = JsonFileStore(File(dataDir, "dishes.json"), Dish.serializer())

    fun products(): MutableList<Product> = productStore.readAll()
    fun dishes(): MutableList<Dish> = dishStore.readAll()

    fun saveProducts(items: List<Product>) = productStore.writeAll(items)
    fun saveDishes(items: List<Dish>) = dishStore.writeAll(items)
}
