package com.whispercppstt.ui

import android.Manifest
import android.app.PendingIntent
import android.app.PendingIntent.CanceledException
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.Message
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import android.widget.ImageView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources
import androidx.core.app.ActivityCompat
import com.whispercppstt.R
import java.lang.ref.WeakReference


class SpeechRecognizerActivity : AppCompatActivity() {
    private var speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

    private class SimpleMessageHandler(looper: Looper, activity: SpeechRecognizerActivity) :
        Handler(looper) {
        private val mRef = WeakReference(activity)

        override fun handleMessage(msg: Message) {
            val outerClass = mRef.get()
            if (outerClass != null) {
                val b = msg.data
                val msgAsString = b.getString(MSG)
                when (msg.what) {
                    MSG_TOAST -> outerClass.toast(msgAsString)
                    MSG_RESULT_ERROR -> outerClass.showError(msgAsString!!)
                    else -> {}
                }
            }
        }
    }

    private fun toast(message: String?) {
        Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
    }

    fun showError(msg: String) {
        Log.d(TAG, msg)
    }

    private fun setupRecognizer() {
        val speechRecognizerIntent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        speechRecognizerIntent.putExtra(
            RecognizerIntent.EXTRA_LANGUAGE_MODEL,
            RecognizerIntent.LANGUAGE_MODEL_FREE_FORM
        )
        speechRecognizer.startListening(speechRecognizerIntent)
    }

    private fun setMicrophoneIcon(resId: Int) {
        findViewById<ImageView>(R.id.microphone)?.setImageDrawable(AppCompatResources.getDrawable(
            baseContext,
            resId)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.speech_recognizer_activity)

        // click on main icon to stop listening in case VAD doesn't detect end of speech
        this@SpeechRecognizerActivity.findViewById<ImageView>(R.id.microphone)?.setOnClickListener {
            speechRecognizer.stopListening()
        }

        speechRecognizer.setRecognitionListener(object : RecognitionListener {
            override fun onRmsChanged(v: Float) { /* nothing */ }
            override fun onBufferReceived(bytes: ByteArray) { /* nothing */ }
            override fun onPartialResults(bundle: Bundle) { /* nothing */ }
            override fun onEvent(i: Int, bundle: Bundle) { /* nothing */ }
            override fun onReadyForSpeech(bundle: Bundle) {
                // white icon
                setMicrophoneIcon(R.drawable.ic_baseline_mic_24)
            }
            override fun onBeginningOfSpeech() {
                // green icon
                setMicrophoneIcon(R.drawable.ic_baseline_mic_24_green)
            }
            override fun onEndOfSpeech() {
                // transcribe icon
                setMicrophoneIcon(R.drawable.ic_baseline_transcribe)
                speechRecognizer.stopListening()
            }
            override fun onError(i: Int) {
                showError(i)
            }
            override fun onResults(bundle: Bundle) {
                Log.v(TAG, "onResults()")
                val results = bundle.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (results === null)
                    showError("Couldn't get results from speech recogniser service")
                else {
                    // bug out if returned results are blank
                    if (results[0].isBlank()) {
                        finish()
                    } else {
                        //                    Log.i(TAG, results[0])
                        returnResults(results)
                    }
                }
            }
        })
    }

    public override fun onStart() {
        super.onStart()
        Log.v(TAG, "onStart()")

        if (!hasPermissions(this, *PERMISSIONS))
            ActivityCompat.requestPermissions(this, PERMISSIONS, PERMISSION_ALL)
        else
            setupRecognizer()
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.v(TAG, "onDestroy()")
        speechRecognizer.destroy()
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        for (i in 0 until permissions.size - 1)
            if (grantResults[i] != PackageManager.PERMISSION_GRANTED) finish()

        setupRecognizer()
    }

    private fun returnResults(results: ArrayList<String>) {
        val handler: Handler = SimpleMessageHandler(Looper.getMainLooper(), this)

        val incomingIntent = intent
        Log.d(TAG, incomingIntent.toString())
        val extras = incomingIntent.extras ?: return

        val pendingIntent = extras.getParcelable<PendingIntent>(
            RecognizerIntent.EXTRA_RESULTS_PENDINGINTENT
        )

        if (pendingIntent == null) {
            Log.d(TAG, "No pending intent, setting result intent.")
            val intent = Intent()
            intent.putStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS, ArrayList(results))
            setResult(RESULT_OK, intent)
        } else {
            Log.d(TAG, pendingIntent.toString())

            var bundle = extras.getBundle(RecognizerIntent.EXTRA_RESULTS_PENDINGINTENT_BUNDLE)
            if (bundle == null)
                bundle = Bundle()

            val intent = Intent()
            intent.putExtras(bundle)
            handler.sendMessage(
                createMessage(
                    String.format(getString(R.string.recognized), results[0])
                )
            )

            try {
                Log.d(TAG, "Sending result via pendingIntent")
                pendingIntent.send(this, RESULT_OK, intent)
            } catch (e: CanceledException) {
                Log.e(TAG, e.message!!)
                handler.sendMessage(createMessage(e.message))
            }
        }

        finish()
    }

    private fun showError(i: Int) {
        toast("Error with recognizer: $i")
    }

    companion object {
        private val TAG: String = SpeechRecognizerActivity::class.java.simpleName

        private const val PERMISSION_ALL = 1

        private var PERMISSIONS: Array<String> = arrayOf(
            Manifest.permission.RECORD_AUDIO,
        )

        private const val MSG = "MSG"
        private const val MSG_TOAST = 1
        private const val MSG_RESULT_ERROR = 2

        private fun createMessage(str: String?): Message {
            val b = Bundle()
            b.putString(MSG, str)
            val msg = Message.obtain()
            msg.what = MSG_TOAST
            msg.data = b
            return msg
        }

        fun hasPermissions(context: Context?, vararg permissions: String): Boolean {
            if (context != null) {
                for (permission in permissions) {
                    if (ActivityCompat.checkSelfPermission(
                            context,
                            permission) != PackageManager.PERMISSION_GRANTED) {
                        return false
                    }
                }
            }
            return true
        }
    }
}