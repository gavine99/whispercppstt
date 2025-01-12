package com.whispercppstt.ui.main

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.whispercppstt.R

@Composable
fun MainScreen(viewModel: MainScreenViewModel) {
    MainScreen(
        canBenchmark = viewModel.canBenchmark,
        messageLog = viewModel.dataLog,
        onBenchmarkTapped = viewModel::benchmark,
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainScreen(
    canBenchmark: Boolean,
    messageLog: String,
    onBenchmarkTapped: () -> Unit,
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text(stringResource(R.string.app_name)) })
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(16.dp)
        ) {
            Column(verticalArrangement = Arrangement.SpaceBetween) {
                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                    BenchmarkButton(enabled = canBenchmark, onClick = onBenchmarkTapped)
                }
            }
            MessageLog(messageLog)
        }
    }
}

@Composable
private fun MessageLog(log: String) {
    SelectionContainer {
        Text(modifier = Modifier.verticalScroll(rememberScrollState()), text = log)
    }
}

@Composable
private fun BenchmarkButton(enabled: Boolean, onClick: () -> Unit) {
    Button(onClick = onClick, enabled = enabled) {
        Text(stringResource(R.string.run_benchmark))
    }
}
