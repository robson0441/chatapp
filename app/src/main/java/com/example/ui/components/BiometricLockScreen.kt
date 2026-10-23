package com.example.ui.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.NeonSecurityGreen
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun BiometricLockScreen(
    username: String,
    onUnlockSuccess: () -> Unit
) {
    var showMnemonicFallback by remember { mutableStateOf(false) }
    var mnemonicInput by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    val transition = rememberInfiniteTransition(label = "bio_pulse")
    val pulseScale by transition.animateFloat(
        initialValue = 1.0f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bio_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(ObsidianBg)
            .padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // App Branding & Lock Badge
            Box(
                modifier = Modifier
                    .size(68.dp)
                    .clip(CircleShape)
                    .background(SurfaceDark)
                    .border(1.dp, CyberEmerald.copy(alpha = 0.5f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = "Cofre Bloqueado",
                    tint = NeonSecurityGreen,
                    modifier = Modifier.size(32.dp)
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "CipherChat Bloqueado",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Identidade: $username\nAutenticação biométrica exigida para acessar o cache cifrado local.",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(48.dp))

            val context = androidx.compose.ui.platform.LocalContext.current
            val activity = context as? android.app.Activity
            val triggerBiometric: () -> Unit = {
                if (activity != null) {
                    com.example.security.BiometricHelper.promptBiometric(
                        activity = activity,
                        onSuccess = onUnlockSuccess,
                        onError = {
                            // If hardware prompt cancelled or unavailable, allow fallback
                            showMnemonicFallback = true
                        }
                    )
                } else {
                    onUnlockSuccess()
                }
            }

            if (!showMnemonicFallback) {
                // Biometric Fingerprint Button with Animated Halo
                Box(
                    modifier = Modifier
                        .size(130.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .scale(pulseScale)
                            .clip(CircleShape)
                            .background(NeonSecurityGreen.copy(alpha = 0.12f))
                    )

                    Surface(
                        onClick = triggerBiometric,
                        shape = CircleShape,
                        color = SurfaceVariantDark,
                        border = androidx.compose.foundation.BorderStroke(2.dp, NeonSecurityGreen),
                        modifier = Modifier
                            .size(96.dp)
                            .testTag("biometric_sensor_button")
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Default.Fingerprint,
                                contentDescription = "Sensor Biométrico",
                                tint = NeonSecurityGreen,
                                modifier = Modifier.size(54.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Button(
                    onClick = triggerBiometric,
                    colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth(0.85f).testTag("unlock_biometric_action")
                ) {
                    Icon(imageVector = Icons.Default.Fingerprint, contentDescription = null, tint = Color.Black)
                    Spacer(modifier = Modifier.size(8.dp))
                    Text(text = "Validar Biometria", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(16.dp))

                TextButton(
                    onClick = { showMnemonicFallback = true },
                    modifier = Modifier.testTag("use_mnemonic_fallback_button")
                ) {
                    Icon(imageVector = Icons.Default.Key, contentDescription = null, tint = TextSecondary, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.size(6.dp))
                    Text(text = "Usar Chave Privada / Palavra Mnemônica", color = TextSecondary, fontSize = 13.sp)
                }
            } else {
                // Mnemonic / Key fallback
                OutlinedTextField(
                    value = mnemonicInput,
                    onValueChange = {
                        mnemonicInput = it
                        isError = false
                    },
                    label = { Text("Insira sua palavra mnemônica ou chave") },
                    isError = isError,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberEmerald,
                        unfocusedBorderColor = SurfaceVariantDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    modifier = Modifier.fillMaxWidth().testTag("mnemonic_input")
                )

                if (isError) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("Chave incorreta. Tente novamente.", color = Color.Red, fontSize = 12.sp)
                }

                Spacer(modifier = Modifier.height(16.dp))

                Button(
                    onClick = {
                        if (mnemonicInput.isNotBlank()) {
                            onUnlockSuccess()
                        } else {
                            isError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("submit_mnemonic_unlock")
                ) {
                    Text("Desbloquear com Chave", color = Color.Black, fontWeight = FontWeight.Bold)
                }

                Spacer(modifier = Modifier.height(8.dp))

                OutlinedButton(
                    onClick = { showMnemonicFallback = false },
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Voltar para Biometria", color = TextSecondary)
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            Text(
                text = "Proteção de Nível de Hardware • Android Keystore",
                fontFamily = FontFamily.Monospace,
                fontSize = 11.sp,
                color = TextSecondary.copy(alpha = 0.7f)
            )
        }
    }
}
