package ar.edu.unicen.exa.bconmanager.Service

import android.util.Log
import ar.edu.unicen.exa.bconmanager.Model.JsonMap
import com.google.gson.Gson
import java.io.BufferedReader
import java.io.File

class JsonUtility {

    companion object {
        fun saveToFile(path: String, jsonMap: JsonMap) {
            var gson = Gson()
            var jsonString:String = gson.toJson(jsonMap)
            Log.d("SAVING", jsonString)
            val file= File(path)
            file.writeText(jsonString)
        }

        fun readFromFile(path: String) : JsonMap {
            var gson = Gson()
            val bufferedReader: BufferedReader = File(path).bufferedReader()
            val inputString = bufferedReader.use { it.readText() }
            var post = gson.fromJson(inputString, JsonMap::class.java)
            Log.d("LOADING", inputString)
            return post
        }
    }
}