package com.whispercppstt

import android.content.Intent
import android.os.Bundle
import android.speech.RecognitionService

class WhisperRecognitionService : RecognitionService() {
    private var whisperRecognition = WhisperRecognition(this)

    override fun onCreate() {
        whisperRecognition.onCreate()
    }

    override fun onDestroy() {
        super.onDestroy()
        whisperRecognition.onDestroy()
    }

    override fun onStartListening(intent: Intent, recognitionCallback: Callback) {
        whisperRecognition.onStartListening(
            recognitionCallback.callingAttributionSource,
            WhisperRecognition.Callbacks(
                { recognitionCallback.readyForSpeech(Bundle()) },
                { recognitionCallback.beginningOfSpeech() },
                { recognitionCallback.endOfSpeech() },
                { e -> recognitionCallback.error(e.hashCode()) },
                { bundle -> recognitionCallback.results(bundle) }
            )
        )
    }

    override fun onCancel(recognitionCallback: Callback) {
        whisperRecognition.onCancel()
    }

    override fun onStopListening(recognitionCallback: Callback) {
        whisperRecognition.onStopListening()
    }
}
