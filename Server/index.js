import express from 'express';
import axios from 'axios';
import crypto from 'crypto';
import dotenv from 'dotenv';
import {generateToken04 } from './token_helper.js';

dotenv.config();
const app = express();
app.use(express.json());

const PORT = 8080;
const APP_ID = Number(process.env.ZEGO_APP_ID);
const SERVER_SECRET = process.env.ZEGO_SERVER_SECRET;
const ZEGO_API_URL = process.env.ZEGO_API_BASE_URL;

let REGISTER_AGENT_ID = null;

function zegoQuery(action){
    const ts = Math.floor(Date.now()/1000).toString();
    const nonce = crypto.randomBytes(8).toString("hex");
    const signBase = String(APP_ID) + nonce + SERVER_SECRET +ts;
    const signature = crypto.createHash("md5").update(signBase).digest("hex");
    return {Action: action, AppId: APP_ID, SignatureNonce: nonce, SignatureVersion: "2.0", Timestamp: ts, Signature: signature};
}

async function zegoCall(action, body = {}){
    const url = `${ZEGO_API_URL}?${new URLSearchParams(zegoQuery(action)).toString()}`;
    try{
        const r = await axios.post(url, body);
        if(r.data.Code !==0) throw new Error(r.data.Message);
        return r.data;
    } catch (err){ throw err;}
}

async function getOrRegisterAgent(){
    if(REGISTER_AGENT_ID) return REGISTER_AGENT_ID;
    
    const agentId = `companion_${Date.now()}`;
        const agentConfig = {
        AgentId: agentId,
        Name: "Mara",
        LLM: {
            Url: "https://ark.cn-beijing.volces.com/api/v3/chat/completions",
            ApiKey: "zego_test",
            Model: "doubao-1-5-pro-32k-250115",
            SystemPrompt: "You are Mara, a friendly Companion of Hasan. Keep replies natural, playful, empathetic and sweet under 30 wprds and use emojies."
        },
        TTS: {
            Vendor: "ByteDance",
            Params: {
                "app": {
                    "appid": "zego_test",
                    "token": "zego_test",
                    "cluster": "volcano_tts"
                },
                "audio": {
                    "voice_type": "zh_female_wanwanxiaohe_moon_bigtts"
                }
            }
        },
        "ASR": {
            "Vendor": "AliyunGummy",
            "Params": {
                "payload": {
                    "model": "gummy-realtime-v1",
                    "parameters": {
                        "source_language": "en"
                    }
                }
            }
        }

    };
    
    await zegoCall("RegisterAgent", agentConfig);
    REGISTER_AGENT_ID = agentId;
    return agentId;
}

app.post("/start-call", async (req, res) =>{
    const {roomId, userId, streamId} = req.body;
    try{
        const agentId = await getOrRegisterAgent();
        const agentUid = `agent_${roomId}`;
        const agentToken = generateToken04(APP_ID, agentUid, SERVER_SECRET, 3600, 
                                           JSON.stringify({room_id: roomId, privilege: {1:1, 2:1}}));
        
        const instanceConfig = {
            Token: agentToken,
            AgentId: agentId,
            UserId: userId,
            RTC: {RoomId: roomId, AgentUserId: agentUid, AgentStreamId: `agent_stream_${roomId}`, UserStreamId: streamId}, MessageHistory: { SyncMode: 1, Messages: [], WindowSize: 10}, 
            CallbackConfig: {ASRResult: 1, LLMResult: 1, Exception: 1, Interrupted: 1, UserSpeakAction: 1, AgentSpeakAction: 1},
            AdvanceConfig: { InterruptMode: 1}
        };
        
        const createResp = await zegoCall("CreateAgentInstance", instanceConfig);
        res.json({success: true, instanceId: createResp.Data.AgentInstanceId});
    } catch (err){res.status(500).json({error: err.message});}
});

app.get("/api/token", (req, res) =>{
    const token = generateToken04(APP_ID, req.query.user_id, SERVER_SECRET, 3600, JSON.stringify({room_id: req.query.room_id, privilege: {1:1, 2:1}}));
    res.json({ token });
});

app.listen(PORT, "0.0.0.0", () => console.log(`Server running on: ${PORT}`));















