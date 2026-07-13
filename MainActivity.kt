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
            
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VibeWorkspaceScreen(apiKey: String) {
    var prompt by remember { mutableStateOf("") }
    var messages by remember { mutableStateOf(listOf<Message>()) }
    var isLoading by remember { mutableStateOf(false) }
    
    // State for the Model Dropdown
    var expanded by remember { mutableStateOf(false) }
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
                            // Using a simple text button for the dropdown trigger if you don't have the MoreVert icon
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
                                    
                                    // Pass the selected model to the API request
                                    val request = ChatRequest(model = selectedModel, messages = fullContext)
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
Composable

import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.rememberScrollState

Composable
fun CodeBlockView(code: String, language: String) {
    val clipboardManager = LocalClipboardManager.current
    var copied by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp)
            .background(Color(0xFF1E1E1E), RoundedCornerShape(8.dp))
    ) {
        // Top bar of the code block
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
        
        // Scrollable code area
        Text(
            text = code,
            color = Color(0xFFD4D4D4), // Classic VSCode light gray
            fontFamily = FontFamily.Monospace,
            style = MaterialTheme.typography.bodySmall,
            modifier = Modifier
                .horizontalScroll(rememberScrollState())
                .padding(12.dp)
        )
    }
}
