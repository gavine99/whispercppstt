package com.whispercppstt

import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.inputmethodservice.InputMethodService
import android.speech.SpeechRecognizer
import android.util.Log
import android.view.View
import android.widget.ImageView
import androidx.appcompat.content.res.AppCompatResources

class WhisperVoiceInputMethodService : InputMethodService() {
    private var whisperRecognition = WhisperRecognition(this)
    private var microphoneIcon: ImageView? = null

    companion object {
        private val TAG: String = WhisperVoiceInputMethodService::class.java.simpleName
    }

    override fun onCreate() {
        super.onCreate()
        whisperRecognition.onCreate()
    }

    override fun onDestroy() {
        super.onDestroy()
        whisperRecognition.onDestroy()
        switchToPreviousInputMethod()
    }

    override fun onFinishInputView(finishingInput: Boolean) {
        super.onFinishInputView(finishingInput)
        switchToPreviousInputMethod()
    }

    override fun onCreateInputView(): View? {
        val view = layoutInflater.inflate(R.layout.speech_recognizer_activity, null)

        microphoneIcon = view.findViewById(R.id.microphone)

        // click on main icon to stop listening in case VAD doesn't detect end of speech
        microphoneIcon?.setOnClickListener {
            whisperRecognition.onStopListening()
        }

        whisperRecognition.onStartListening(
            null,
            WhisperRecognition.Callbacks(
                {
                    // white icon
                    microphoneIcon?.colorFilter = BlendModeColorFilter(
                        getColor(R.color.mic_white),
                        BlendMode.SRC_ATOP)
                },
                {
                    // green icon
                    microphoneIcon?.colorFilter = BlendModeColorFilter(
                        getColor(R.color.mic_green),
                        BlendMode.SRC_ATOP)
                },
                {
                    // transcribe icon
                    microphoneIcon?.apply {
                        colorFilter = null
                        setImageDrawable(
                            AppCompatResources.getDrawable(view.context, R.drawable.ic_transcribe)
                        )

                        // clear on click listener
                        setOnClickListener { }
                    }
                },
                {
                    // error
                    e -> Log.e(TAG, e.toString())
                    switchToPreviousInputMethod()
                },
                {
                    // results
                    bundle ->
                    Log.v(TAG, "onResults()")

                    val results = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                    if (results === null)
                        Log.d(TAG, "Couldn't get results from speech recogniser service")
                    else if (results[0].isNotBlank()) {
                        //                    Log.i(TAG, results[0])
                        getCurrentInputConnection().commitText(results[0],1)
                    }

                    switchToPreviousInputMethod()
                }
            )
        )

        return view
    }
}