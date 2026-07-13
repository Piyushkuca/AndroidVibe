package com.piyushkuca.androidvibe

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Send
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

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
            text = "Enter your NVIDIA API Key to start coding.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(bottom = 24.dp)
        )
        
        OutlinedTextField(
            value = keyInput,
            onValueChange = { keyInput = it },
            label = { Text("nvapi-...") },
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
    var messages by remember { mutableStateOf(listOf<Message>()) }
    var isLoading by remember { mutableStateOf(false) }
    
    // Model Dropdown State
    var expanded by remember { mutableStateOf(false) }
    // Ensure Constants.kt exists with SUPPORTED_MODELS as defined earlier!
    var selectedModel by remember { mutableStateOf(Constants.SUPPORTED_MODELS.first()) }
    
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Column {
                        Text("Vibe Workspace", style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "Model: ${selectedModel.split("/").last()}", 
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.7f)
                        )
                    }
                },
                actions = {
                    Box {
                        IconButton(onClick = { expanded = true }) {
                            Text("⚙️", style = MaterialTheme.typography.titleLarge)
                        }
                        DropdownMenu(
                            expanded = expanded,
                            onDismissRequest = { expanded = false }
                        ) {
                            Constants.SUPPORTED_MODELS.forEach { model ->
                                DropdownMenuItem(
                                    text = { Text(model.split("/").last()) },
                                    onClick = {
                                        selectedModel = model
                                        expanded = false
                                    }
                                )
                            }
                        }
                    }
                },
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
                    placeholder = { Text("Build a login screen...") },
                    modifier = Modifier.weight(1f),
                    maxLines = 3,
                    enabled = !isLoading
                )
                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = {
                        if (prompt.isNotBlank()) {
                            val userMessage = Message(role = "user", content = prompt)
                            messages = messages + userMessage
                            val currentPrompt = prompt
                            prompt = ""
                            isLoading = true
                            
                            coroutineScope.launch {
                                try {
                                    val fullContext = listOf(
                                        Message(role = "system", content = "You are an expert Android Jetpack Compose developer. Only output valid Kotlin code without markdown wrapping.")
                                    ) + messages
                                    
                                    val request = ChatRequest(model = selectedModel, messages = fullContext)
                                    val response = nvidiaApi.generateCode("Bearer $apiKey", request)
                                    
                                    if (response.isSuccessful) {
                                        val aiResponse = response.body()?.choices?.firstOrNull()?.message?.content ?: "Error: No response generated."
                                        messages = messages + Message(role = "assistant", content = aiResponse)
                                    } else {
                                        messages = messages + Message(role = "assistant", content = "Error: ${response.code()} -${response.message()}")
                                    }
                                } catch (e: Exception) {
                                    messages = messages + Message(role = "assistant", content = "Network Error: ${e.localizedMessage}")
                                } finally {
                                    isLoading = false
                                }
                            }
                        }
                    },
                    modifier = Modifier.background(
                        if (isLoading) Color.Gray else MaterialTheme.colorScheme.primary, 
                        shape = RoundedCornerShape(12.dp)
                    ),
                    enabled = !isLoading
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
            items(messages) { message ->
                ChatBubble(
                    text = message.content, 
                    isUser = message.role == "user"
                )
            }
            if (isLoading) {
                item {
                    Text(
                        "Generating code...", 
                        color = Color.Gray, 
                        modifier = Modifier.padding(8.dp)
                    )
                }
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
            modifier = Modifier.widthIn(max = 320.dp)
        ) {
            if (isUser) {
                Text(
                    text = text,
                    modifier = Modifier.padding(12.dp),
                    color = MaterialTheme.colorScheme.onPrimary,
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                Column(modifier = Modifier.padding(12.dp)) {
                    val segments = parseMessage(text)
                    segments.forEach { segment ->
                        when (segment) {
                            is MessageSegment.Text -> {
                                Text(
                                    text = segment.content,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                            is MessageSegment.Code -> {
                                CodeBlockView(code = segment.content, language = segment.language)
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun CodeBlockView(code: String, language: String) {
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFF2D2D2D), RoundedCornerShape(topStart = 8.dp, topEnd = 8.dp))
                .padding(horizontal = 12.dp, vertical = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = language.ifBlank { "code" },
                color = Color.LightGray,
                style = MaterialTheme.typography.labelSmall
            )
            
            TextButton(
                onClick = {
                    clipboardManager.setText(AnnotatedString(code))
                    copied = true
                },
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier.height(24.dp)
            ) {
                Text(
                    text = if (copied) "Copied!" else "Copy",
                    color = MaterialTheme.colorScheme.primary,
                    style = MaterialTheme.typography.labelSmall
                )
            }
        }
        
        Text(
            text = code,
            color = Color(0xFFD4D4D4),
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(12.dp)
        )
    }
}

// --- MARKDOWN PARSER UTILS ---

sealed class MessageSegment {
    data class Text(val content: String) : MessageSegment()
    data class Code(val language: String, val content: String) : MessageSegment()
}

fun parseMessage(message: String): List<MessageSegment> {
    val segments = mutableListOf<MessageSegment>()
    val parts = message.split("```")
    
    for ((index, part) in parts.withIndex()) {
        if (index % 2 == 1) { 
            val lines = part.lines()
            val language = lines.firstOrNull()?.trim() ?: ""
            val code = lines.drop(1).joinToString("\n").trim()
            segments.add(MessageSegment.Code(language, code))
        } else {
            if (part.isNotBlank()) {
                segments.add(MessageSegment.Text(part.trim()))
            }
        }
    }
    return segments
}
