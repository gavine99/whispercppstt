package com.whispercppstt

import android.app.Application
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.whispercpp.whisper.WhisperContext
import com.whispercpp.whisper.WhisperContext.Companion.createContextFromAsset
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainScreenViewModel(private val application: Application) : ViewModel() {
    var canBenchmark by mutableStateOf(true)
        private set
    var dataLog by mutableStateOf("")
        private set

    init {
        viewModelScope.launch {
            printSystemInfo()
        }
    }

    private suspend fun printSystemInfo() {
        printMessage(
            String.format("System Info: %s\n", WhisperContext.getSystemInfo())
        )
    }

    private suspend fun printMessage(msg: String) = withContext(Dispatchers.Main) {
        dataLog += msg
    }

    fun benchmark() = viewModelScope.launch {
        runBenchmark(6)
    }

    private suspend fun runBenchmark(nthreads: Int) {
        if (!canBenchmark)
            return

        canBenchmark = false

        printMessage("loading model\n")

        val whisperContext = createContextFromAsset(
            application.assets,
            "models/ggml-tiny.en.bin"
        )

        printMessage("model loaded\n")

        printMessage("Running benchmark. This will take minutes...\n")
        printMessage(whisperContext.benchMemory(nthreads))

        printMessage("\n")

        printMessage(whisperContext.benchGgmlMulMat(nthreads))
        printMessage("\nDone\n")

        canBenchmark = true
    }

    companion object {
        fun factory() = viewModelFactory {
            initializer {
                val application =
                    this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as Application
                MainScreenViewModel(application)
            }
        }
    }
}
