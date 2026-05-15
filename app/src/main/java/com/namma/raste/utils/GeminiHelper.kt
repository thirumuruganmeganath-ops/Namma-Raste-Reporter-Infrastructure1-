package com.namma.raste.utils

import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.net.HttpURLConnection
import java.net.URL

data class GeminiResult(
    val issueType: String,      // "POTHOLE" or "STREETLIGHT"
    val severity: String,       // "LOW", "MEDIUM", "HIGH"
    val confidence: Int,        // 0-100
    val description: String     // short AI description
)

object GeminiHelper {

    private const val API_URL =
        "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent"
    // ← Replace with your actual key when you paste it
    var apiKey: String = "AIzaSyD1KP7X4IhK9ZoXzvBlrNBjrrTor_nZxHQ"

    suspend fun analyzeImage(photoPath: String): GeminiResult = withContext(Dispatchers.IO) {
        // 1. Load & compress image to base64
        val bitmap = BitmapFactory.decodeFile(photoPath)
            ?: throw Exception("Could not read image")
        val resized = Bitmap.createScaledBitmap(bitmap, 512, 512, true)
        val baos = ByteArrayOutputStream()
        resized.compress(Bitmap.CompressFormat.JPEG, 80, baos)
        val base64Image = Base64.encodeToString(baos.toByteArray(), Base64.NO_WRAP)

        // 2. Build prompt
        val prompt = """
            You are an infrastructure issue classifier for Indian roads.
            Analyze this image and respond ONLY in this exact JSON format with no extra text:
            {
              "issueType": "POTHOLE" or "STREETLIGHT",
              "severity": "LOW" or "MEDIUM" or "HIGH",
              "confidence": number between 0 and 100,
              "description": "one short sentence describing the issue"
            }
            
            Rules:
            - issueType = POTHOLE if you see road damage, potholes, cracks, broken road surface
            - issueType = STREETLIGHT if you see broken/dark street lights, lamp posts, electrical issues
            - severity LOW = minor issue, not urgent
            - severity MEDIUM = noticeable issue, needs attention soon  
            - severity HIGH = dangerous, needs immediate repair
            - If neither issue is clear, pick the closest match
        """.trimIndent()

        // 3. Build request body
        val requestBody = JSONObject().apply {
            put("contents", JSONArray().apply {
                put(JSONObject().apply {
                    put("parts", JSONArray().apply {
                        put(JSONObject().apply {
                            put("text", prompt)
                        })
                        put(JSONObject().apply {
                            put("inline_data", JSONObject().apply {
                                put("mime_type", "image/jpeg")
                                put("data", base64Image)
                            })
                        })
                    })
                })
            })
            put("generationConfig", JSONObject().apply {
                put("temperature", 0.1)
                put("maxOutputTokens", 200)
            })
        }

        // 4. Make API call
        val url = URL("$API_URL?key=$apiKey")
        val connection = url.openConnection() as HttpURLConnection
        connection.requestMethod = "POST"
        connection.setRequestProperty("Content-Type", "application/json")
        connection.doOutput = true
        connection.connectTimeout = 15000
        connection.readTimeout = 15000

        connection.outputStream.use { it.write(requestBody.toString().toByteArray()) }

        val responseCode = connection.responseCode
        val responseText = if (responseCode == 200) {
            connection.inputStream.bufferedReader().readText()
        } else {
            val error = connection.errorStream?.bufferedReader()?.readText() ?: "Unknown error"
            throw Exception("API error $responseCode: $error")
        }

        // 5. Parse response
        val json       = JSONObject(responseText)
        val candidates = json.getJSONArray("candidates")
        val content    = candidates.getJSONObject(0)
            .getJSONObject("content")
            .getJSONArray("parts")
            .getJSONObject(0)
            .getString("text")
            .trim()
            .removePrefix("```json")
            .removeSuffix("```")
            .trim()

        val result = JSONObject(content)
        GeminiResult(
            issueType   = result.optString("issueType",   "POTHOLE"),
            severity    = result.optString("severity",    "MEDIUM"),
            confidence  = result.optInt("confidence",     70),
            description = result.optString("description", "Infrastructure issue detected")
        )
    }
}
