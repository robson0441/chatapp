package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.crypto.CryptoEngine
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.NeonSecurityGreen
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun NewChatDialog(
    onAddContact: (peerUsername: String, peerPublicKey: String) -> Unit,
    onDismiss: () -> Unit
) {
    var peerUsername by remember { mutableStateOf("") }
    var peerPublicKey by remember { mutableStateOf("") }
    var isError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = SurfaceDark)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.PersonAdd,
                            contentDescription = null,
                            tint = NeonSecurityGreen
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Nova Conversa Cifrada",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = TextPrimary
                        )
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Fechar",
                            tint = TextSecondary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(14.dp))

                Text(
                    text = "Inicie contato anônimo inserindo o pseudônimo e a chave pública do destinatário.",
                    style = MaterialTheme.typography.bodySmall,
                    color = TextSecondary
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedTextField(
                    value = peerUsername,
                    onValueChange = {
                        peerUsername = it
                        isError = false
                    },
                    label = { Text("Nome de usuário (@peer)") },
                    prefix = { Text("@", color = NeonSecurityGreen) },
                    placeholder = { Text("bob_security, whistleblower_node") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberEmerald,
                        unfocusedBorderColor = SurfaceVariantDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth().testTag("new_chat_username_input")
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = peerPublicKey,
                    onValueChange = {
                        peerPublicKey = it
                        isError = false
                    },
                    label = { Text("Chave Pública (Base64 ou hash)") },
                    placeholder = { Text("MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8...") },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = CyberEmerald,
                        unfocusedBorderColor = SurfaceVariantDark,
                        focusedTextColor = TextPrimary,
                        unfocusedTextColor = TextPrimary
                    ),
                    maxLines = 3,
                    trailingIcon = {
                        IconButton(onClick = {
                            // Generate mock peer key if empty for testing
                            peerPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA" + System.currentTimeMillis()
                        }) {
                            Icon(
                                imageVector = Icons.Default.QrCodeScanner,
                                contentDescription = "Simular Scan QR",
                                tint = NeonSecurityGreen
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth().testTag("new_chat_pubkey_input")
                )

                if (isError) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "Informe ao menos um nome de usuário com 2+ caracteres.",
                        color = Color.Red,
                        fontSize = 12.sp
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quick Suggestions
                Text(
                    text = "Contatos Sugeridos da Rede:",
                    style = MaterialTheme.typography.labelSmall,
                    color = TextSecondary
                )
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SuggestedContactChip(name = "@edward_s") {
                        peerUsername = "edward_s"
                        peerPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAASnowdenVault..."
                    }
                    SuggestedContactChip(name = "@julian_a") {
                        peerUsername = "julian_a"
                        peerPublicKey = "MIIBIjANBgkqhkiG9w0BAQEFAAssangeVault..."
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = {
                        if (peerUsername.trim().isNotBlank()) {
                            val cleanUser = if (peerUsername.startsWith("@")) peerUsername.trim() else "@${peerUsername.trim()}"
                            val cleanKey = if (peerPublicKey.trim().isNotBlank()) peerPublicKey.trim() else "MIIBIjANBgkqhkiG9w0BAQEFAAOCAQ8AMIIBCgKCAQEA" + System.currentTimeMillis()
                            onAddContact(cleanUser, cleanKey)
                        } else {
                            isError = true
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = CyberEmerald),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth().testTag("confirm_new_chat_button")
                ) {
                    Text("Abrir Conversa E2EE", color = Color.Black, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

@Composable
private fun SuggestedContactChip(name: String, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        color = SurfaceVariantDark,
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3300E676))
    ) {
        Text(
            text = name,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            fontSize = 12.sp,
            fontFamily = FontFamily.Monospace,
            color = NeonSecurityGreen
        )
    }
}
