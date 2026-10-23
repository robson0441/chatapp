package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CallEnd
import androidx.compose.material.icons.filled.Cameraswitch
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MicOff
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material.icons.filled.VideocamOff
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.crypto.CryptoEngine
import com.example.ui.theme.BlueEncryption
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.DangerRed
import com.example.ui.theme.NeonSecurityGreen
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import kotlinx.coroutines.delay

@Composable
fun CallScreen(
    peerUsername: String,
    isVideoCall: Boolean,
    onEndCall: (durationSeconds: Int) -> Unit
) {
    var callSeconds by remember { mutableIntStateOf(0) }
    var isMuted by remember { mutableStateOf(false) }
    var isCameraActive by remember { mutableStateOf(isVideoCall) }
    var isFrontCamera by remember { mutableStateOf(true) }
    var isSpeakerOn by remember { mutableStateOf(true) }

    val (sasEmojis, sasCode) = remember(peerUsername) {
        CryptoEngine.computeCallSas("SESSION_SEED_$peerUsername")
    }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            callSeconds++
        }
    }

    val minutes = callSeconds / 60
    val seconds = callSeconds % 60
    val timeFormatted = "%02d:%02d".format(minutes, seconds)

    val transition = rememberInfiniteTransition(label = "audio_anim")
    val pulseAlpha by transition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .statusBarsPadding()
    ) {
        // Video background if video is active
        if (isCameraActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color(0xFF0F2027),
                                Color(0xFF203A43),
                                Color(0xFF2C5364)
                            )
                        )
                    )
            ) {
                // Video simulation grid with cybersecurity scanning effect
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val step = 40.dp.toPx()
                    for (x in 0..(size.width / step).toInt()) {
                        drawLine(
                            color = Color(0x1500E676),
                            start = Offset(x * step, 0f),
                            end = Offset(x * step, size.height),
                            strokeWidth = 1f
                        )
                    }
                    for (y in 0..(size.height / step).toInt()) {
                        drawLine(
                            color = Color(0x1500E676),
                            start = Offset(0f, y * step),
                            end = Offset(size.width, y * step),
                            strokeWidth = 1f
                        )
                    }
                }

                // Peer Camera simulation overlay
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .padding(24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(140.dp)
                            .clip(CircleShape)
                            .border(3.dp, NeonSecurityGreen, CircleShape)
                            .background(SurfaceDark),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = peerUsername.take(2).uppercase(),
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Bold,
                            color = NeonSecurityGreen
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Surface(
                        color = Color(0x99000000),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = if (isFrontCamera) "Câmera Frontal E2EE • 1080p @ 60fps" else "Câmera Traseira E2EE • 1080p @ 60fps",
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                            style = MaterialTheme.typography.bodySmall,
                            fontFamily = FontFamily.Monospace,
                            color = NeonSecurityGreen
                        )
                    }
                }
            }
        } else {
            // Voice Call Mode - Large Animated Avatar
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 100.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .border(4.dp, CyberEmerald.copy(alpha = pulseAlpha), CircleShape)
                        .background(SurfaceDark),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = peerUsername.take(2).uppercase(),
                        fontSize = 52.sp,
                        fontWeight = FontWeight.Bold,
                        color = CyberEmerald
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Dynamic Audio Waveform Indicator
                Row(
                    horizontalArrangement = Arrangement.spacedBy(4.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.height(36.dp)
                ) {
                    val heights = listOf(14.dp, 28.dp, 36.dp, 22.dp, 32.dp, 18.dp, 30.dp, 12.dp)
                    heights.forEachIndexed { index, dpHeight ->
                        val animatedHeight = dpHeight * (0.4f + (pulseAlpha * (index % 3 + 1) * 0.25f))
                        Box(
                            modifier = Modifier
                                .width(5.dp)
                                .height(animatedHeight)
                                .clip(RoundedCornerShape(3.dp))
                                .background(NeonSecurityGreen)
                        )
                    }
                }
            }
        }

        // Top Bar: Peer Name, Time, E2EE status
        Column(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Surface(
                color = Color(0xCC111B21),
                shape = RoundedCornerShape(20.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3300E676))
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Criptografia E2EE",
                        tint = NeonSecurityGreen,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "CHAMADA CRIPTOGRAFADA PONTA A PONTA",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        fontSize = 10.sp,
                        letterSpacing = 1.sp,
                        color = NeonSecurityGreen
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = peerUsername,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = timeFormatted,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                color = TextSecondary
            )

            Spacer(modifier = Modifier.height(10.dp))

            // SAS Verification Badge (ZRTP 4 security emojis)
            Surface(
                color = Color(0xDD0B141A),
                shape = RoundedCornerShape(12.dp),
                border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x22FFFFFF))
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "Código de Confirmação SAS (Anti-MITM):",
                        style = MaterialTheme.typography.labelSmall,
                        color = TextSecondary,
                        fontSize = 11.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        sasEmojis.forEach { emoji ->
                            Text(text = emoji, fontSize = 20.sp)
                        }
                        Text(
                            text = sasCode,
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = NeonSecurityGreen
                        )
                    }
                }
            }
        }

        // Bottom Controls Bar
        Column(
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .padding(bottom = 40.dp, start = 20.dp, end = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Telemetry snippet
            Surface(
                color = Color(0xAA0B141A),
                shape = RoundedCornerShape(8.dp)
            ) {
                Text(
                    text = "SRTP AES-GCM 256 • 0 Servidores • P2P Direct Mesh",
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp),
                    fontFamily = FontFamily.Monospace,
                    fontSize = 10.sp,
                    color = TextSecondary
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Mic Mute Toggle
                IconButton(
                    onClick = { isMuted = !isMuted },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(if (isMuted) DangerRed.copy(alpha = 0.3f) else SurfaceVariantDark)
                        .testTag("call_mic_toggle")
                ) {
                    Icon(
                        imageVector = if (isMuted) Icons.Default.MicOff else Icons.Default.Mic,
                        contentDescription = "Mudo",
                        tint = if (isMuted) DangerRed else TextPrimary
                    )
                }

                // Video Toggle
                IconButton(
                    onClick = { isCameraActive = !isCameraActive },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(if (!isCameraActive) SurfaceVariantDark else CyberEmerald.copy(alpha = 0.3f))
                        .testTag("call_video_toggle")
                ) {
                    Icon(
                        imageVector = if (isCameraActive) Icons.Default.Videocam else Icons.Default.VideocamOff,
                        contentDescription = "Câmera",
                        tint = if (isCameraActive) NeonSecurityGreen else TextSecondary
                    )
                }

                // Switch Camera (only if camera active)
                IconButton(
                    onClick = { isFrontCamera = !isFrontCamera },
                    enabled = isCameraActive,
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(SurfaceVariantDark)
                        .testTag("call_camera_switch")
                ) {
                    Icon(
                        imageVector = Icons.Default.Cameraswitch,
                        contentDescription = "Alternar Câmera",
                        tint = if (isCameraActive) TextPrimary else TextSecondary.copy(alpha = 0.4f)
                    )
                }

                // Speaker Toggle
                IconButton(
                    onClick = { isSpeakerOn = !isSpeakerOn },
                    modifier = Modifier
                        .size(52.dp)
                        .clip(CircleShape)
                        .background(if (isSpeakerOn) BlueEncryption.copy(alpha = 0.3f) else SurfaceVariantDark)
                        .testTag("call_speaker_toggle")
                ) {
                    Icon(
                        imageVector = Icons.Default.VolumeUp,
                        contentDescription = "Alto-falante",
                        tint = if (isSpeakerOn) BlueEncryption else TextSecondary
                    )
                }

                // End Call FAB
                FloatingActionButton(
                    onClick = { onEndCall(callSeconds) },
                    containerColor = DangerRed,
                    contentColor = Color.White,
                    modifier = Modifier
                        .size(64.dp)
                        .testTag("call_end_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.CallEnd,
                        contentDescription = "Encerrar Chamada",
                        modifier = Modifier.size(32.dp)
                    )
                }
            }
        }
    }
}
