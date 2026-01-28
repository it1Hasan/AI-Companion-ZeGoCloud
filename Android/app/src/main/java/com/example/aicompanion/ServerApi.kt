package com.example.aicompanion

import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import okhttp3.*
import java.io.IOException

object ServerApi {
    private val client = OkHttpClient()

    fun startCall(roomId: String, userId:String,streamId:String,callback:(String?)->Unit){
        val json = JSONObject().apply {
            put("roomId", roomId)
            put("userId", userId)
            put("streamId", streamId)
        }
        val body = json.toString().toRequestBody("application/json".toMediaTypeOrNull())
        val request = Request.Builder().url("${Constant.ServerUrl}/start-call").post(body).build()
        client.newCall(request).enqueue(object : Callback{
            override fun onResponse(call: Call, response: Response) {
                val id = JSONObject(response.body?.string()?:"{}").optString("agentInstanceId")
                callback(id)
            }

            override fun onFailure(call: Call, e: IOException) = callback(null)
        })
    }
}