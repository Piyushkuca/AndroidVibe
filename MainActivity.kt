package com.piyushkuca.androidvibe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AndroidVibeApp()
                }
            }
        }
    }
}

@Composable
fun AndroidVibeApp() {
    // State to hold the API key. In a production app, save this to DataStore/SharedPreferences.
    var apiKey by remember { mutableStateOf("") }
    var isKeySaved by remember { mutableStateOf(false) }

    if (!isKeySaved) {
        ApiKeyScreen(
            onKeySubmitted = { key ->
                apiKey = key
                isKeySaved = true
            }
        )
    } else {
        VibeWorkspaceScreen(apiKey = apiKey)
    }
}

@Composable
fun ApiKeyScreen(onKeySubmitted: (String) -> Unit) {
    var keyInput by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Welcome to AndroidVibe",
            style = MaterialTheme.typography.headlineMedium,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Text(
            text = "Enter your AI API Key to start vibe coding.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        OutlinedTextField(
            value = keyInput,
            onValueChange = { keyInput = it },
            label = { Text("API Key") },
            visualTransformation = PasswordVisualTransformation(),
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Button(
            onClick = { if (keyInput.isNotBlank()) onKeySubmitted(keyInput) },
            modifier = Modifier.fillMaxWidth(),
            enabled = keyInput.isNotBlank()
        ) {
            Text("Start Building")
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VibeWorkspaceScreen(apiKey: String) {
    var prompt by remember { mutableStateOf("") }
    // A simple list to hold the conversation history
    var messages by remember { mutableStateOf(listOf<Pair<String, Boolean>>()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vibe Workspace") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        bottomBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                OutlinedTextField(
                    value = prompt,
                    onValueChange = { prompt = it },
                    placeholder = { Text("Describe what you want to build...") },
                    modifier = Modifier.weight(1f),
                    maxLines = 3
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (prompt.isNotBlank()) {
                            // Add user prompt to list
                            messages = messages + Pair(prompt, true)
                            // TODO: Trigger network call to AI here using the apiKey
                            // Mocking an AI response for now
                            messages = messages + Pair("// Generating code for: $prompt\n\nfun myGeneratedCode() {\n    // Magic happens here\n}", false)
                            prompt = ""
                        }
                    },
                    modifier = Modifier.background(
                        MaterialTheme.colorScheme.primary, 
                        shape = RoundedCornerShape(12.dp)
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Send,
                        contentDescription = "Send Prompt",
                        tint = MaterialTheme.colorScheme.onPrimary
                    )
                }
            }
        }
    ) { innerPadding ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
            contentPadding = PaddingValues(vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            items(messages) { (text, isUser) ->
                ChatBubble(text = text, isUser = isUser)
            }
        }
    }
}

@Composable
fun ChatBubble(text: String, isUser: Boolean) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Surface(
            color = if (isUser) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant,
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.widthIn(max = 300.dp)
        ) {
            Text(
                text = text,
                modifier = Modifier.padding(12.dp),
                color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                // Use monospace font for AI code responses to make it look like an IDE
                fontFamily = if (isUser) FontFamily.Default else FontFamily.Monospace,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
