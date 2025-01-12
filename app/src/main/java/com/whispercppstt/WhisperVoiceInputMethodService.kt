package com.whispercppstt

import android.inputmethodservice.InputMethodService
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources

class WhisperVoiceInputMethodService : InputMethodService() {
    private var whisperRecognition = WhisperRecognition(this)
    private var microphone: ImageView? = null
    private var callbacks: WhisperRecognition.Callbacks = WhisperRecognition.Callbacks(
        { /* white icon */ setMicrophoneIcon(R.drawable.ic_baseline_mic_24) },
        { /* green icon */ setMicrophoneIcon(R.drawable.ic_baseline_mic_24_green) },
        { /* transcribe icon */  setMicrophoneIcon(R.drawable.ic_baseline_transcribe) },
        { /* error */  e -> showError(e.toString()); switchToPreviousInputMethod() },
        { /* results */ bundle ->
            Log.v(TAG, "onResults()")
            val results = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (results === null)
                showError("Couldn't get results from speech recogniser service")
            else if (results[0].isNotBlank()) {
                //                    Log.i(TAG, results[0])
                getCurrentInputConnection().commitText(results[0],1)
            }

            switchToPreviousInputMethod()
        }
    )

    companion object {
        private val TAG: String = WhisperVoiceInputMethodService::class.java.simpleName
    }

    private fun showError(msg: String) {
        Log.d(TAG, msg)
    }

    private fun setMicrophoneIcon(resId: Int) {
        microphone?.setImageDrawable(AppCompatResources.getDrawable(
            baseContext,
            resId)
        )
    }

    override fun onCreate() {
        super.onCreate()
        whisperRecognition.onCreate()
    }

    override fun onDestroy() {
        super.onDestroy()
        whisperRecognition.onDestroy()
    }

    override fun onCreateInputView(): View? {
        val view = layoutInflater.inflate(R.layout.speech_recognizer_activity, null)
        microphone = view.findViewById(R.id.microphone)

        // click on main icon to stop listening in case VAD doesn't detect end of speech
        microphone?.setOnClickListener {
            whisperRecognition.onStopListening()
            this@WhisperVoiceInputMethodService.switchToPreviousInputMethod()
        }

        whisperRecognition.onStartListening(callbacks)

        return view
    }
}