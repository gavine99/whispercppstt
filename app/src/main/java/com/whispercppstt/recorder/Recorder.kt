package com.whispercppstt.recorder

import android.annotation.SuppressLint
import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log

import com.konovalov.vad.silero.VadSilero
import com.konovalov.vad.silero.config.FrameSize
import com.konovalov.vad.silero.config.Mode
import com.konovalov.vad.silero.config.SampleRate

import java.util.concurrent.atomic.AtomicInteger

class Recorder {
    private var recorderThread: AudioRecordThread? = null

    companion object {
        private val TAG: String = Recorder::class.java.simpleName

        const val STOP = 1
        const val CANCEL = 2
    }

    class Callbacks(
        val readyForSpeech: () -> Unit,
        val beginningOfSpeech: () -> Unit,
        val endOfSpeech: () -> Unit,
        val error: (Exception) -> Unit,
        val completed: (FloatArray) -> Unit
    )

    fun startRecording(appContext: Context,
                       audioData: MutableList<Float>,
                       callbacks: Callbacks) {
        Log.v(TAG, "startRecording()")

        recorderThread = AudioRecordThread(
            appContext,
            audioData,
            callbacks
        )
        recorderThread?.start()
    }

    fun stopRecording(stopOrCancel: Int = STOP) {
        if (recorderThread === null)
            return

        Log.d(TAG, "stopRecording()")

        val tmpRecorderThread = recorderThread
        recorderThread = null

        tmpRecorderThread?.stopRecording(stopOrCancel)
        tmpRecorderThread?.join()
    }
}

private class AudioRecordThread(
    private val appContext: Context,
    private val audioData: MutableList<Float>,
    private val callbacks: Recorder.Callbacks
) : Thread("AudioRecorder") {
    private var running = AtomicInteger(0)

    companion object {
        private val TAG: String = AudioRecordThread::class.java.simpleName

        const val STOP = 1
        const val CANCEL = 2

        private const val VAD_BUFFER_MULTIPLIER = 2
        private const val PRE_VAD_BUFFER_LENGTH = (1024 * 12)
    }

    @SuppressLint("MissingPermission")
    override fun run() {
        Log.d(TAG, "run()")

        try {
            val vad = VadSilero(
                sampleRate = SampleRate.SAMPLE_RATE_16K,
                frameSize = FrameSize.FRAME_SIZE_1024,
                mode = Mode.VERY_AGGRESSIVE,
                silenceDurationMs = 850,
                speechDurationMs = 50,
                context = appContext
            )

            val bufferSize = (vad.frameSize.value * VAD_BUFFER_MULTIPLIER)

            // if buffer size is less than the minimum required audio recorder buffer size, throw an exception
            if (bufferSize < AudioRecord.getMinBufferSize(
                    16000,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT
                )) {
                throw java.lang.Exception("$VAD_BUFFER_MULTIPLIER x VAD buffer size is smaller than minimum recorder buffer size")
            }

            val audioRecord = AudioRecord(
                MediaRecorder.AudioSource.MIC,
                16000,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                bufferSize
            )

            try {
                if (audioRecord.state != AudioRecord.STATE_INITIALIZED)
                    return

                running.set(0)

                val readBuffer = ShortArray(bufferSize)

                var detectedFirstSpeech = false

                audioRecord.startRecording()

                callbacks.readyForSpeech()

                while (running.get() == 0) {
//Log.d(TAG, "loop")
                    val read = audioRecord.read(readBuffer, 0, bufferSize)
                    if (read <= 0) {
                        throw java.lang.RuntimeException("audioRecord.read returned $read")
                    }

                    // speech detected or not?
                    var speechDetectedInBuffer = false
                    for (i in 0 until bufferSize step vad.frameSize.value) {
                        val range = IntRange(i, (i + vad.frameSize.value - 1))
//Log.d(TAG, "speech detect range: ${range.first} ${range.last}")
                        speechDetectedInBuffer = vad.isSpeech(readBuffer.sliceArray(range))
                        if (speechDetectedInBuffer)
                            break       // if speech is detected, break out of for loop
                    }

                    // if speech hasn't been detected yet
                    if (!detectedFirstSpeech) {
                        if (speechDetectedInBuffer) {
                            Log.v(TAG, "vad: first speech detected")
                            detectedFirstSpeech = true
                            callbacks.beginningOfSpeech()
                        }
                    // else, first speech has been detected, check for lack of speech
                    } else if (!speechDetectedInBuffer) {
                        Log.v(TAG, "vad: no speech detected")
                        callbacks.endOfSpeech()
                        break   // out of while loop that captures audio
                    }

                    for (i in 0 until read) {
                        // silero vad has a delay before detecting voice so we maintain a buffer of previously captured audio
                        if (!detectedFirstSpeech) {
                            if (audioData.size >= PRE_VAD_BUFFER_LENGTH) {
                                audioData.removeAt(0)
                            }
                        }
                        audioData.add((readBuffer[i] / 32767.0f).coerceIn(-1f..1f))
                    }
                }

                audioRecord.stop()

            } finally {
                audioRecord.release()
            }
        } catch (e: Exception) {
            callbacks.error(e)
        }

        if (running.get() != CANCEL) {
            callbacks.completed(audioData.toFloatArray())
        }
    }

    fun stopRecording(stopOrCancel: Int = STOP) {
        Log.d(TAG, "stopRecording($stopOrCancel)")
        running.set(stopOrCancel)
    }
}