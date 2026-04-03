package fr.istic.mob.networkXY.repository

import android.content.Context
import fr.istic.mob.networkXY.model.Graph
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class NetworkRepository(private val context: Context) {
    private val filename = "network_graph.json"
    private val json = Json { 
        prettyPrint = true
        ignoreUnknownKeys = true
    }
    
    suspend fun saveGraph(graph: Graph): Boolean = withContext(Dispatchers.IO) {
        try {
            val jsonString = json.encodeToString(graph)
            context.openFileOutput(filename, Context.MODE_PRIVATE).use {
                it.write(jsonString.toByteArray())
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    suspend fun loadGraph(): Graph? = withContext(Dispatchers.IO) {
        try {
            val jsonString = context.openFileInput(filename).bufferedReader().use { it.readText() }
            json.decodeFromString<Graph>(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    fun hasSavedGraph(): Boolean {
        return context.fileList().contains(filename)
    }

    fun exportGraphToJson(graph: Graph): String {
        return json.encodeToString(graph)
    }

    fun importGraphFromJson(jsonString: String): Graph? {
        return try {
            json.decodeFromString<Graph>(jsonString)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
