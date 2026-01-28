package com.example.aicompanion

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.content.ContextCompat
import com.example.aicompanion.ui.theme.AiCompanionTheme
import im.zego.zegoexpress.ZegoExpressEngine
import im.zego.zegoexpress.callback.IZegoEventHandler
import im.zego.zegoexpress.constants.ZegoRoomState
import im.zego.zegoexpress.constants.ZegoScenario
import im.zego.zegoexpress.constants.ZegoUpdateType
import im.zego.zegoexpress.entity.ZegoEngineProfile
import im.zego.zegoexpress.entity.ZegoRoomConfig
import im.zego.zegoexpress.entity.ZegoStream
import im.zego.zegoexpress.entity.ZegoUser
import okhttp3.*
import org.json.JSONObject
import java.io.IOException
import java.util.ArrayList
import android.graphics.Color
import android.view.Gravity
import android.view.View
import android.widget.ScrollView

class MainActivity : AppCompatActivity() {

    private val userId = "user_${(1000..9999).random()}"
    private val roomId = "ai_agent_room_1"
    private val streamId = "stream_$userId"

    private var sarahMessageBuffer=""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        findViewById<ImageButton>(R.id.startCallButton).setOnClickListener { startZego() }
        findViewById<ImageButton>(R.id.stopCallButton).setOnClickListener { stopCall() }

        if(ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)!= PackageManager.PERMISSION_GRANTED){
            registerForActivityResult(ActivityResultContracts.RequestPermission()){if(it)startZego()}.launch(
                Manifest.permission.RECORD_AUDIO)
        }
    }
    private fun startZego(){
        val profile = ZegoEngineProfile().apply {
            appID = Constant.AppId
            scenario = ZegoScenario.HIGH_QUALITY_CHATROOM
            application = this@MainActivity.application
        }
        ZegoExpressEngine.createEngine(profile, null)

        val engine = ZegoExpressEngine.getEngine()
        engine.enableAEC(true)
        engine.enableAGC(true)
        engine.enableANS(true)

        setHandlers()
        fetchToken()
    }

    private fun fetchToken(){
        val client = OkHttpClient()
        val url = "${Constant.ServerUrl}/api/token?user_id=$userId&room_id=$roomId"
        client.newCall(Request.Builder().url(url).build()).enqueue(object:Callback{
            override fun onResponse(call: Call, response: Response) {
                val token = JSONObject(response.body?.string()?:"").getString("token")
                runOnUiThread {
                    ZegoExpressEngine.getEngine().loginRoom(roomId, ZegoUser(userId,"User"),
                        ZegoRoomConfig().apply { this.token = token })
                }
            }

            override fun onFailure(call: Call, e: IOException) {}
        })
    }

    private fun setHandlers(){
        ZegoExpressEngine.getEngine().setEventHandler(object : IZegoEventHandler(){
            override fun onRoomStateUpdate(
                roomID: String?,
                state: ZegoRoomState?,
                errorCode: Int,
                extendedData: JSONObject?
            ) {
                if(state == ZegoRoomState.CONNECTED){
                    ZegoExpressEngine.getEngine().startPublishingStream(streamId)
                    ServerApi.startCall(roomId,userId,streamId){}
                }
            }

            override fun onRoomStreamUpdate(
                roomID: String?,
                updateType: ZegoUpdateType?,
                streamList: ArrayList<ZegoStream>?,
                extendedData: JSONObject?
            ) {
                if(updateType == ZegoUpdateType.ADD){
                    streamList?.forEach { if(it.user.userID.contains("agent")|| it.user.userID.contains("AI"))
                        ZegoExpressEngine.getEngine().startPlayingStream(it.streamID)
                    }
                }
            }

            override fun onRecvExperimentalAPI(content: String?) {
                try {
                    val msgJson = JSONObject(JSONObject(JSONObject(content!!).getString("params")).getString("msg_content"))
                    val cmd = msgJson.optInt("Cmd")
                    val data = msgJson.getJSONObject("Data")
                    val isFinished = data.optBoolean("EndFlag",false)
                    val text = data.optString("Text", "")

                    runOnUiThread {
                        when(cmd){
                            3->{
                                if(isFinished && text.isNotEmpty()){
                                    sarahMessageBuffer = ""
                                    addMessage(text, true)
                                }
                            }
                            4->{
                                sarahMessageBuffer += text
                                if(isFinished && sarahMessageBuffer.isNotEmpty()){
                                    addMessage(sarahMessageBuffer, false)
                                    sarahMessageBuffer = ""
                                }
                            }
                        }
                    }
                } catch (e: Exception) {}
            }
        })
    }

    private fun stopCall(){
        ZegoExpressEngine.getEngine().logoutRoom()
        ZegoExpressEngine.destroyEngine(null)
        findViewById<LinearLayout>(R.id.chatContainer).removeAllViews()
    }

    private fun addMessage(message: String, isUser: Boolean){
        val chatContainer = findViewById<LinearLayout>(R.id.chatContainer)
        val messageView = TextView(this).apply{
            text = message
            textSize = 16f
            setTextColor(Color.WHITE)
            setBackgroundResource(if(isUser)R.drawable.message_background_user else R.drawable.message_background_ai)
            layoutParams= LinearLayout.LayoutParams(LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT).apply {
                    gravity = if(isUser) Gravity.END else Gravity.START
                    setMargins(16,10,16,10)
            }
            setPadding(32,20,32,20)
        }
        chatContainer.addView(messageView)
        findViewById<ScrollView>(R.id.scrollView).post {
            findViewById<ScrollView>(R.id.scrollView).fullScroll(View.FOCUS_DOWN)
        }

    }
}




