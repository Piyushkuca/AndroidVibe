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
import kotlinx.coroutines.launch
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.Body
import retrofit2.http.Header
import retrofit2.http.POST
import retrofit2.Response

// --- NETWORK MODELS & API INTERFACE ---

data class ChatRequest(
    val model: String = "meta/llama-3.1-405b-instruct", // High-tier NVIDIA NIM model
    val messages: List<Message>,
    val max_tokens: Int = 1024,
    val temperature: Double = 0.2
)

data class Message(
    val role: String,
    val content: String
)

data class ChatResponse(
    val choices: List<Choice>?
)

data class Choice(
    val message: Message
)

interface NvidiaApiService {
    @POST("v1/chat/completions")
    suspend fun generateCode(
        @Header("Authorization") authHeader: String,
        @Body request: ChatRequest
    ): Response<ChatResponse>
}

// Retrofit singleton setup
val retrofit = Retrofit.Builder()
    .baseUrl("https://integrate.api.nvidia.com/")
    .addConverterFactory(GsonConverterFactory.create())
    .build()

val nvidiaApi = retrofit.create(NvidiaApiService::class.java)

// --- MAIN ACTIVITY ---

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
    
    // Coroutine scope for network calls
    val coroutineScope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Vibe Workspace (NVIDIA)") },
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
                                    // Send the system prompt + user history to NVIDIA
                                    val fullContext = listOf(
                                        Message(role = "system", content = "You are an expert Android Jetpack Compose developer. Only output valid Kotlin code without markdown wrapping.")
                                    ) + messages
                                    
                                    val request = ChatRequest(messages = fullContext)
                                    val response = nvidiaApi.generateCode("Bearer $apiKey", request)
                                    
                                    if (response.isSuccessful) {
                                        val aiResponse = response.body()?.choices?.firstOrNull()?.message?.content ?: "Error: No response generated."
                                        messages = messages + Message(role = "assistant", content = aiResponse)
                                    } else {
                                        messages = messages + Message(role = "assistant", content = "Error: ${response.code()} - ${response.message()}")
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
            Text(
                text = text,
                modifier = Modifier.padding(12.dp),
                color = if (isUser) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
                fontFamily = if (isUser) FontFamily.Default else FontFamily.Monospace,
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
