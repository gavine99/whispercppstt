package com.whispercppstt

import android.content.AttributionSource
import android.media.MediaPlayer
import android.os.Bundle
import android.speech.SpeechRecognizer
import android.util.Log
import com.whispercpp.whisper.WhisperContext
import com.whispercpp.whisper.WhisperContext.Companion.createContextFromAsset
import com.whispercppstt.recorder.Recorder
import kotlinx.coroutines.runBlocking

class WhisperRecognition(private val context: android.content.Context) {
    private var whisperContext: WhisperContext? = null
    private var recorder: Recorder = Recorder()
    private var mediaPlayer: MediaPlayer? = null

    companion object {
        private val TAG: String = WhisperRecognition::class.java.simpleName
    }

    class Callbacks(
        val readyForSpeech: () -> Unit,
        val beginningOfSpeech: () -> Unit,
        val endOfSpeech: () -> Unit,
        val error: (Exception) -> Unit,
        val results: (Bundle) -> Unit,
    )

    fun onCreate() {
        mediaPlayer = MediaPlayer.create(context, R.raw.start_speech_effect)
    }

    fun onDestroy() {
        Log.d(TAG, "onDestroy()")

        recorder.stopRecording(Recorder.CANCEL)

        mediaPlayer?.release()

        runBlocking {
            whisperContext?.release()
        }
    }

    fun onStartListening(callingAttributionSource: AttributionSource?, callbacks: Callbacks) {
        Log.d(TAG, "onStartListening()")

        try {
            // load model first time through
            if (whisperContext == null) {
                Log.v(TAG, "loading model into whisper context")

                whisperContext = createContextFromAsset(context.assets, "models/ggml-tiny.en.bin")

                Log.v(TAG, "model load done")
            }

            recorder.startRecording(
                context,
                callingAttributionSource,
                mutableListOf(),
                Recorder.Callbacks(
                    { callbacks.readyForSpeech(); mediaPlayer?.start() },
                    { callbacks.beginningOfSpeech() },
                    { callbacks.endOfSpeech(); mediaPlayer?.start() },
                    { e: Exception -> callbacks.error(e) },
                    { audioData -> transcribeAndReturnResult(callbacks, audioData) },
                    { }
                )
            )
        } catch (e: Exception) {
            callbacks.error(e)
        }
    }

    fun onCancel() {
        Log.d(TAG, "onCancel()")
        recorder.stopRecording(Recorder.CANCEL)
    }

    fun onStopListening() {
        Log.d(TAG, "onStopListening()")
        recorder.stopRecording()
    }

    private fun transcribeAndReturnResult(callback: Callbacks, audioData: FloatArray) {
        Log.d(TAG, "transcribeAndReturnResult()")

        try {
            val resultArray = ArrayList<String>()

            if (audioData.isNotEmpty()) {
                // whisper.cpp uses coroutines. create a coroutine scope
                runBlocking {
                    val result = whisperContext?.transcribeData(audioData, false)
                    if (result != null)
                        resultArray.add(result)
                }

                // replace special strings in place
                val iterator = resultArray.listIterator()
                while (iterator.hasNext())
                    iterator.set(performStringReplacements(iterator.next().trim()))
                }

            callback.results(Bundle().apply {
                putStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION, resultArray)
            })
        } catch (e: Exception) {
            callback.error(e)
        }
    }

    private fun performStringReplacements(resultString: String): String {
        var retString = resultString

        // remove anything enclosed in square brackets
        retString = retString.replace(Regex("[\\[.*\\]]", RegexOption.IGNORE_CASE), "")
        retString = retString.replace(Regex("[\\[.*\\]]", RegexOption.IGNORE_CASE), "")

        retString = retString.replace(Regex("[ ,]*new *line[ ,]*", RegexOption.IGNORE_CASE), "\n")
        retString = retString.replace(Regex("[ ,]*new lion[ ,]*", RegexOption.IGNORE_CASE), "\n")
        retString = retString.replace(Regex("[. ]*dot dot dot", RegexOption.IGNORE_CASE), "...")
        retString = retString.replace(Regex("kis+[-, ]kis+[.,]*", RegexOption.IGNORE_CASE), "\uD83D\uDE18")
        retString = retString.replace("full stop", ".", ignoreCase = true)
        retString = retString.replace("half stop", ",", ignoreCase = true)
        retString = retString.replace(Regex("double quotes*", RegexOption.IGNORE_CASE), "\"")
        retString = retString.replace(Regex("open quotes*", RegexOption.IGNORE_CASE), "'")
        retString = retString.replace(Regex("close quotes*", RegexOption.IGNORE_CASE), "'")
        retString = retString.replace(Regex("thumbs[- ]up icon", RegexOption.IGNORE_CASE), "\uD83D\uDC4D")
        retString = retString.replace(Regex("kis+ icon", RegexOption.IGNORE_CASE), "\uD83D\uDE18")
        retString = retString.replace(Regex("pr*o+[hf]* icon", RegexOption.IGNORE_CASE), "\uD83D\uDCA9")
        retString = retString.replace(Regex("pr*o+.*[- ]face", RegexOption.IGNORE_CASE), "\uD83D\uDCA9\uD83D\uDE42")
        retString = retString.replace(Regex("who.* face", RegexOption.IGNORE_CASE), "\uD83D\uDCA9\uD83D\uDE42")
        retString = retString.replace(Regex("through[-, ]face", RegexOption.IGNORE_CASE), "\uD83D\uDCA9\uD83D\uDE42")
        retString = retString.replace("smile icon", "\uD83D\uDE42", ignoreCase = true)
        retString = retString.replace("face icon", "\uD83D\uDE42", ignoreCase = true)
        retString = retString.replace("love icon", "❤\uFE0F", ignoreCase = true)
        retString = retString.replace("heart icon", "❤\uFE0F", ignoreCase = true)
        retString = retString.replace("laugh icon", "\uD83E\uDD23", ignoreCase = true)
        retString = retString.replace("swear icon", "\uD83E\uDD2C", ignoreCase = true)
        retString = retString.replace("crazy icon", "\uD83E\uDD2A", ignoreCase = true)
        retString = retString.replace("sad icon", "\uD83D\uDE41", ignoreCase = true)
        retString = retString.replace("nervous icon", "\uD83D\uDE2C", ignoreCase = true)

        if (retString.matches(Regex("^thumbs[- ]up[.,]*$", RegexOption.IGNORE_CASE))) retString = "\uD83D\uDC4D"
        if ((retString.matches(Regex("^love[.,]*$", RegexOption.IGNORE_CASE))) ||
            (retString.matches(Regex("^love.*heart[.,]*$", RegexOption.IGNORE_CASE))) ||
            (retString.matches(Regex("^heart[.,]*$", RegexOption.IGNORE_CASE)))) retString = "❤\uFE0F"
        if (retString.matches(Regex("^laughing[.,]*$", RegexOption.IGNORE_CASE))) retString = "\uD83E\uDD23"
        if ((retString.matches(Regex("^kissing[.,]*$", RegexOption.IGNORE_CASE))) ||
            (retString.matches(Regex("^kis+[.,]*$", RegexOption.IGNORE_CASE)))) retString = "\uD83D\uDE18"
        if (retString.matches(Regex("^nervous[.,]*$", RegexOption.IGNORE_CASE))) retString = "\uD83D\uDE2C"

        return retString
    }
}
