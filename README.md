# Real-Time AI Companion 🤖🎙️

A full-stack AI Voice Assistant built using **Kotlin**, **Node.js**, and **ZEGOCLOUD's AI Agent SDK**. This project features 100% text-to-voice synchronization and ultra-low latency interaction.

## 🚀 Features
* **Real-Time Voice Interaction**: Powered by ZEGOCLOUD for seamless streaming.
* **100% Text Accuracy**: Custom buffering logic in Kotlin ensures chat bubbles perfectly match the spoken audio.
* **Secure Backend**: Node.js server for safe Token 04 generation and AI Agent management.
* **Modern UI**: iOS-style dynamic chat bubbles with auto-scroll functionality.

## 🛠️ Tech Stack
* **Android**: Kotlin, ZegoExpressEngine, OkHttp.
* **Backend**: Node.js, Express, Crypto.
* **AI Engine**: ZEGOCLOUD AI Agent (LLM + TTS + ASR).

⚙️ Setup Instructions
1. Backend Setup (Node.js)
Navigate to the server/ folder.

Run npm install to install dependencies (Express, Axios, Dotenv, Crypto).
Create a .env file from .env.example and add your ZEGOCLOUD AppID and ServerSecret.

Start the server:
Bash
node index.js

2. Android Setup (Kotlin)
Open the android/ folder in Android Studio.
Go to Constant.kt and update the SERVER_URL to your computer's IPv4 address (e.g., http://YourIPAddress:8080).
Build and Run on a real device.

🌟 Acknowledgments
Special thanks to ZEGOCLOUD for their robust AI Agent SDK that made this 3rd collaboration a success!

Developed by Hasan - ITech Aspirant
