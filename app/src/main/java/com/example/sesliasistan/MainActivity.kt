package com.example.sesliasistan

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import java.util.*

class MainActivity : ComponentActivity() {
    private var speechRecognizer: SpeechRecognizer? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(this)

        setContent {
            ModernVoiceApp(
                onStartListening = { onResult, onError ->
                    startListening(onResult, onError)
                },
                onStopListening = {
                    speechRecognizer?.stopListening()
                }
            )
        }
    }

    private fun startListening(onResult: (String) -> Unit, onError: () -> Unit) {
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, "tr-TR")
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) {}
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() {}
            override fun onError(error: Int) { onError() }
            override fun onResults(results: Bundle?) {
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    onResult(matches[0])
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {
                val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                if (!matches.isNullOrEmpty()) {
                    onResult(matches[0])
                }
            }
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer?.startListening(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        speechRecognizer?.destroy()
    }
}

// Yerel Basit Kural Tabanlı Temizleme Motoru
fun cleanAndRefineTurkish(text: String): String {
    if (text.isBlank()) return ""
    var cleaned = text.lowercase(Locale("tr"))
    val fillers = listOf("yani", "şey", "ııı", "eee", "hımm", "mesela", "falan", "filan")
    for (filler in fillers) {
        cleaned = cleaned.replace("\\b$filler\\b".toRegex(), "")
    }
    cleaned = cleaned.replace("\\s+".toRegex(), " ").trim()
    return cleaned.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale("tr")) else it.toString() } + "."
}

@Composable
fun ModernVoiceApp(
    onStartListening: (onResult: (String) -> Unit, onError: () -> Unit) -> Unit,
    onStopListening: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = LocalClipboardManager.current
    var isRecording by remember { mutableStateOf(false) }
    var rawText by remember { mutableStateOf("") }
    var refinedText by remember { mutableStateOf("") }

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            isRecording = true
            onStartListening(
                { text ->
                    rawText = text
                    refinedText = cleanAndRefineTurkish(text)
                },
                { isRecording = false }
            )
        } else {
            Toast.makeText(context, "Mikrofon izni gerekli", Toast.LENGTH_SHORT).show()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    val bgGradient = Brush.verticalGradient(
        colors = listOf(Color(0xFF1A1C29), Color(0xFF0E1017))
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgGradient)
            .padding(20.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Spacer(modifier = Modifier.height(30.dp))
                Text(
                    text = "Sesli Asistan",
                    fontSize = 28.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Text(
                    text = if (isRecording) "Sizi dinliyorum..." else "Konuşmak için mikrofona dokunun",
                    fontSize = 14.sp,
                    color = Color(0xFF9E9E9E),
                    modifier = Modifier.padding(top = 8.dp)
                )
            }

            // Mikrofon Butonu
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.padding(vertical = 40.dp)
            ) {
                if (isRecording) {
                    Box(
                        modifier = Modifier
                            .size(130.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(Color(0xFFFF5252).copy(alpha = 0.25f))
                    )
                }

                IconButton(
                    onClick = {
                        if (!isRecording) {
                            val permission = Manifest.permission.RECORD_AUDIO
                            if (ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED) {
                                isRecording = true
                                onStartListening(
                                    { text ->
                                        rawText = text
                                        refinedText = cleanAndRefineTurkish(text)
                                    },
                                    { isRecording = false }
                                )
                            } else {
                                permissionLauncher.launch(permission)
                            }
                        } else {
                            isRecording = false
                            onStopListening()
                        }
                    },
                    modifier = Modifier
                        .size(90.dp)
                        .shadow(12.dp, CircleShape)
                        .clip(CircleShape)
                        .background(
                            if (isRecording) Brush.linearGradient(listOf(Color(0xFFFF5252), Color(0xFFFF1744)))
                            else Brush.linearGradient(listOf(Color(0xFF6C63FF), Color(0xFF3F3D56)))
                        )
                ) {
                    Icon(
                        imageVector = if (isRecording) Icons.Default.Stop else Icons.Default.Mic,
                        contentDescription = "Mikrofon",
                        tint = Color.White,
                        modifier = Modifier.size(40.dp)
                    )
                }
            }

            // Metin Kartları
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Ham Metin Kartı
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF252836)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Algılanan Ham Ses",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF8F94A8)
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (rawText.isEmpty()) "Henüz bir ses algılanmadı..." else rawText,
                            fontSize = 15.sp,
                            color = Color(0xFFD1D5DB)
                        )
                    }
                }

                // Düzeltilmiş Metin Kartı
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF2C2F48)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "✨ Anlaşılır ve Düzenli Çıktı",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF82B1FF)
                            )
                            if (refinedText.isNotEmpty()) {
                                IconButton(
                                    onClick = {
                                        clipboardManager.setText(AnnotatedString(refinedText))
                                        Toast.makeText(context, "Kopyalandı!", Toast.LENGTH_SHORT).show()
                                    },
                                    modifier = Modifier.size(24.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.ContentCopy,
                                        contentDescription = "Kopyala",
                                        tint = Color(0xFF82B1FF)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = if (refinedText.isEmpty()) "Düzenlenmiş metin burada görünecek..." else refinedText,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White
                        )
                    }
                }
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
