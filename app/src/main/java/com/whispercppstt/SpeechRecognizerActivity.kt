package com.whispercppstt

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.BlendMode
import android.graphics.BlendModeColorFilter
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat

class SpeechRecognizerActivity : AppCompatActivity() {
    private var speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

    private var microphoneIcon: ImageView? = null

    private fun setupRecognizer() {
        speechRecognizer.startListening(
            Intent(
                RecognizerIntent.ACTION_RECOGNIZE_SPEECH
            ).apply {
                putExtra(
                    RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                    RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
                )
            }
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.speech_recognizer_activity)
        microphoneIcon = findViewById<ImageView>(R.id.microphone)

        // click on main icon to stop listening in case VAD doesn't detect end of speech
        microphoneIcon?.apply {
            setOnClickListener { speechRecognizer.stopListening() }
            colorFilter = BlendModeColorFilter(
                getColor(R.color.mic_red),
                BlendMode.SRC_ATOP
            )
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onRmsChanged(v: Float) { /* nothing */ }
            override fun onBufferReceived(bytes: ByteArray) { /* nothing */ }
            override fun onPartialResults(bundle: Bundle) { /* nothing */ }
            override fun onEvent(i: Int, bundle: Bundle) { /* nothing */ }
            override fun onReadyForSpeech(bundle: Bundle) {
                // white icon
                microphoneIcon?.colorFilter = BlendModeColorFilter(
                    getColor(R.color.mic_white),
                    BlendMode.SRC_ATOP
                )
            }
            override fun onBeginningOfSpeech() {
                // green icon
                microphoneIcon?.colorFilter = BlendModeColorFilter(
                    getColor(R.color.mic_green),
                    BlendMode.SRC_ATOP
                )
            }
            override fun onEndOfSpeech() {
                // transcribe icon
                microphoneIcon?.apply {
                    colorFilter = null
                    setImageDrawable(
                        AppCompatResources.getDrawable(
                            this@SpeechRecognizerActivity, R.drawable.ic_transcribe
                        )
                    )

                    // clear on click listener
                    setOnClickListener { }
                }
            }
            override fun onError(err: Int) {
                val msg = when (err) {
                    3 -> "Audio recording error"
                    5 -> "Client error"
                    6 -> "Speech timeout"
                    7 -> return
                    8 -> "Recognizer busy"
                    10 -> "Too many requests"
                    else -> "Other error"
                }

                "Speech recognizer error: ($err) $msg".run {
                    Toast.makeText(applicationContext, this, Toast.LENGTH_LONG).show()
                    Log.d(TAG, this)
                }

                // finish the activity
                finish()
            }
            override fun onResults(bundle: Bundle) {
                Log.v(TAG, "onResults()")

                val results = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (results === null)
                    Log.d(TAG, "Couldn't get results from speech recogniser service")
                // bug out if returned results are blank
                else if (results[0].isBlank())
                    Log.d(TAG, "Empty result from speech recogniser service")
                else
                    // Log.i(TAG, results[0])
                    returnResults(results)

                // finish the activity
                finish()
            }
        })
    }

    fun hasPermissions(vararg permissions: String): Boolean {
        for (permission in permissions)
            if (ActivityCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED)
                return false

        return true
    }

    public override fun onStart() {
        super.onStart()

        if (!hasPermissions(*PERMISSIONS))
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL)
        else
            setupRecognizer()
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer.destroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        for (i in 0 until permissions.size - 1)
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED)
                finish()

        setupRecognizer()
    }

    private fun returnResults(results: ArrayList<String>) {
        Log.d(TAG, intent.toString())

        val extras = intent.extras ?: return

        val pendingIntent = extras.getParcelable(
            RecognizerIntent.EXTRA_RESULTS_PENDINGINTENT,
            PendingIntent::class.java
        )

        if (pendingIntent == null) {
            Log.d(TAG, "No pending intent, setting result intent.")

            setResult(RESULT_OK, Intent().apply {
                putStringArrayListExtra(
                    RecognizerIntent.EXTRA_RESULTS,
                    ArrayList(results)
                )
            })
        } else {
            Log.d(TAG, pendingIntent.toString())

//            Toast.makeText(
//                applicationContext,
//                "Recognized: ${results[0]}",
//                Toast.LENGTH_LONG).show()

            try {
                Log.d(TAG, "Sending result via pendingIntent")
                pendingIntent.send(this, RESULT_OK, Intent(). apply {
                    putExtras(
                        extras.getBundle(RecognizerIntent.EXTRA_RESULTS_PENDINGINTENT_BUNDLE)
                            ?: Bundle()
                    )
                })
            } catch (e: PendingIntent.CanceledException) {
                "Error sending results to app: ${e.message!!}".run {
                    Toast.makeText(applicationContext, this, Toast.LENGTH_LONG).show()
                    Log.d(TAG, this)
                }
            }
        }
    }

    companion object {
        private val TAG: String = SpeechRecognizerActivity::class.java.simpleName

        private const val PERMISSION_ALL = 1

        private var PERMISSIONS: Array<String> = arrayOf(
            Manifest.permission.RECORD_AUDIO,
            Manifest.permission.MODIFY_AUDIO_SETTINGS,
            Manifest.permission.BLUETOOTH,
            Manifest.permission.FOREGROUND_SERVICE,
            Manifest.permission.FOREGROUND_SERVICE_MICROPHONE,
        )
    }
}