package com.example.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AttachFile
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Done
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.Image
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.InsertDriveFile
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VerifiedUser
import androidx.compose.material.icons.filled.Videocam
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.ConversationEntity
import com.example.data.MessageEntity
import com.example.ui.components.DisappearingFileCard
import com.example.ui.components.SafetyNumberDialog
import com.example.ui.theme.BlueEncryption
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.IncomingBubble
import com.example.ui.theme.NeonSecurityGreen
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.OutgoingBubble
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.ui.theme.WarningAmber
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ChatDetailScreen(
    conversation: ConversationEntity,
    messages: List<MessageEntity>,
    myPublicKey: String,
    isOffline: Boolean,
    onBack: () -> Unit,
    onSendMessage: (content: String, type: String, isBurn: Boolean, burnSeconds: Int, fileName: String?) -> Unit,
    onStartVoiceCall: () -> Unit,
    onStartVideoCall: () -> Unit,
    onBurnExpired: (messageId: String) -> Unit,
    onToggleSafetyVerification: (Boolean) -> Unit
) {
    var inputText by remember { mutableStateOf("") }
    var showSafetyDialog by remember { mutableStateOf(false) }
    var showAttachmentSheet by remember { mutableStateOf(false) }
    var selectedBurnTimer by remember { mutableIntStateOf(conversation.disappearingTimerSeconds) }
    var showMenu by remember { mutableStateOf(false) }

    val photoPickerLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        if (uri != null) {
            val fileName = "foto_cifrada_${System.currentTimeMillis() % 10000}.jpg"
            onSendMessage(
                "Foto Criptografada",
                "IMAGE",
                selectedBurnTimer > 0,
                if (selectedBurnTimer > 0) selectedBurnTimer else 25,
                fileName
            )
        }
    }

    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    if (showSafetyDialog) {
        SafetyNumberDialog(
            peerUsername = conversation.peerUsername,
            myPublicKey = myPublicKey,
            peerPublicKey = conversation.peerPublicKey,
            peerFingerprint = conversation.peerFingerprint,
            isVerified = conversation.safetyVerified,
            onToggleVerification = onToggleSafetyVerification,
            onDismiss = { showSafetyDialog = false }
        )
    }

    Scaffold(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
            .imePadding(),
        containerColor = ObsidianBg,
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable { showSafetyDialog = true }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(40.dp)
                                .clip(CircleShape)
                                .background(SurfaceVariantDark)
                                .border(
                                    1.dp,
                                    if (conversation.safetyVerified) NeonSecurityGreen else CyberEmerald,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = conversation.peerUsername.take(2).uppercase().replace("@", ""),
                                fontWeight = FontWeight.Bold,
                                color = NeonSecurityGreen,
                                fontSize = 16.sp
                            )
                        }

                        Spacer(modifier = Modifier.width(10.dp))

                        Column {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = conversation.peerUsername,
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = TextPrimary
                                )
                                if (conversation.safetyVerified) {
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Icon(
                                        imageVector = Icons.Default.VerifiedUser,
                                        contentDescription = "Chave Verificada",
                                        tint = NeonSecurityGreen,
                                        modifier = Modifier.size(15.dp)
                                    )
                                }
                            }
                            Text(
                                text = if (isOffline) "Modo Offline (Lendo Cache Local)" else "E2EE AES-256-GCM • P2P",
                                style = MaterialTheme.typography.bodySmall,
                                fontSize = 10.sp,
                                fontFamily = FontFamily.Monospace,
                                color = if (isOffline) WarningAmber else NeonSecurityGreen
                            )
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack, modifier = Modifier.testTag("chat_back_button")) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Voltar",
                            tint = TextPrimary
                        )
                    }
                },
                actions = {
                    IconButton(onClick = onStartVideoCall, modifier = Modifier.testTag("video_call_button")) {
                        Icon(
                            imageVector = Icons.Default.Videocam,
                            contentDescription = "Chamada de Vídeo",
                            tint = NeonSecurityGreen
                        )
                    }
                    IconButton(onClick = onStartVoiceCall, modifier = Modifier.testTag("voice_call_button")) {
                        Icon(
                            imageVector = Icons.Default.Call,
                            contentDescription = "Chamada de Voz",
                            tint = NeonSecurityGreen
                        )
                    }
                    IconButton(onClick = { showMenu = !showMenu }, modifier = Modifier.testTag("chat_menu_button")) {
                        Icon(
                            imageVector = Icons.Default.MoreVert,
                            contentDescription = "Mais opções",
                            tint = TextPrimary
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Ver Número de Segurança") },
                            leadingIcon = { Icon(Icons.Default.Shield, contentDescription = null, tint = NeonSecurityGreen) },
                            onClick = {
                                showMenu = false
                                showSafetyDialog = true
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Autodestruição: 10s") },
                            leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null, tint = WarningAmber) },
                            onClick = {
                                showMenu = false
                                selectedBurnTimer = 10
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Autodestruição: 30s") },
                            leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null, tint = WarningAmber) },
                            onClick = {
                                showMenu = false
                                selectedBurnTimer = 30
                            }
                        )
                        DropdownMenuItem(
                            text = { Text("Desativar Autodestruição") },
                            leadingIcon = { Icon(Icons.Default.Timer, contentDescription = null, tint = TextSecondary) },
                            onClick = {
                                showMenu = false
                                selectedBurnTimer = 0
                            }
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = SurfaceDark)
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Self-Destruction status banner if active
            if (selectedBurnTimer > 0) {
                Surface(
                    color = Color(0x33FFB74D),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Timer,
                            contentDescription = null,
                            tint = WarningAmber,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Novos arquivos e mensagens se autodestruirão em ${selectedBurnTimer}s",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = WarningAmber
                        )
                    }
                }
            }

            // Messages LazyColumn
            LazyColumn(
                state = listState,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item {
                    // Security introduction header
                    SecurityPillHeader(
                        peerUsername = conversation.peerUsername,
                        fingerprint = conversation.peerFingerprint,
                        onVerifyClick = { showSafetyDialog = true }
                    )
                }

                items(messages, key = { it.id }) { msg ->
                    if (msg.isSelfDestructing || msg.type == "IMAGE" || msg.type == "FILE") {
                        DisappearingFileCard(
                            message = msg,
                            onBurnExpired = onBurnExpired,
                            modifier = Modifier.fillMaxWidth(if (msg.isFromMe) 0.88f else 0.88f)
                        )
                    } else {
                        MessageBubble(message = msg)
                    }
                }

                item {
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // Attachment selection bottom sheet
            AnimatedVisibility(visible = showAttachmentSheet) {
                AttachmentPickerSheet(
                    onSendPhoto = {
                        showAttachmentSheet = false
                        try {
                            photoPickerLauncher.launch(
                                androidx.activity.result.PickVisualMediaRequest(
                                    androidx.activity.result.contract.ActivityResultContracts.PickVisualMedia.ImageOnly
                                )
                            )
                        } catch (e: Exception) {
                            // Fallback if photo picker service is not present
                            onSendMessage(
                                "Foto Criptografada",
                                "IMAGE",
                                selectedBurnTimer > 0,
                                if (selectedBurnTimer > 0) selectedBurnTimer else 25,
                                "evidencia_foto_${System.currentTimeMillis() % 1000}.png"
                            )
                        }
                    },
                    onSendFile = {
                        showAttachmentSheet = false
                        onSendMessage(
                            "Documento Criptografado",
                            "FILE",
                            selectedBurnTimer > 0,
                            if (selectedBurnTimer > 0) selectedBurnTimer else 30,
                            "relatorio_sigiloso_${System.currentTimeMillis() % 1000}.pdf"
                        )
                    },
                    onSelectBurnTimer = { seconds ->
                        selectedBurnTimer = seconds
                        showAttachmentSheet = false
                    }
                )
            }

            // Message Composer Input Bar
            Surface(
                color = SurfaceDark,
                modifier = Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 8.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { showAttachmentSheet = !showAttachmentSheet },
                        modifier = Modifier.testTag("chat_attachment_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.AttachFile,
                            contentDescription = "Anexar Arquivo com Autodestruição",
                            tint = if (showAttachmentSheet) NeonSecurityGreen else TextSecondary
                        )
                    }

                    OutlinedTextField(
                        value = inputText,
                        onValueChange = { inputText = it },
                        placeholder = {
                            Text(
                                text = if (selectedBurnTimer > 0) "Mensagem (${selectedBurnTimer}s burn)..." else "Mensagem cifrada...",
                                color = TextMuted,
                                fontSize = 14.sp
                            )
                        },
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = CyberEmerald,
                            unfocusedBorderColor = Color.Transparent,
                            focusedTextColor = TextPrimary,
                            unfocusedTextColor = TextPrimary,
                            focusedContainerColor = SurfaceVariantDark,
                            unfocusedContainerColor = SurfaceVariantDark
                        ),
                        shape = RoundedCornerShape(24.dp),
                        maxLines = 4,
                        modifier = Modifier
                            .weight(1f)
                            .testTag("chat_message_input")
                    )

                    Spacer(modifier = Modifier.width(6.dp))

                    if (inputText.isNotBlank()) {
                        FilledIconButton(
                            onClick = {
                                val text = inputText.trim()
                                inputText = ""
                                onSendMessage(
                                    text,
                                    "TEXT",
                                    selectedBurnTimer > 0,
                                    selectedBurnTimer,
                                    null
                                )
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = CyberEmerald),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("chat_send_button")
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Send,
                                contentDescription = "Enviar Cifrado",
                                tint = Color.Black
                            )
                        }
                    } else {
                        // Quick Voice Note simulation
                        FilledIconButton(
                            onClick = {
                                onSendMessage(
                                    "Áudio Criptografado (0:08)",
                                    "VOICE",
                                    selectedBurnTimer > 0,
                                    selectedBurnTimer,
                                    "nota_voz.opus"
                                )
                            },
                            colors = IconButtonDefaults.filledIconButtonColors(containerColor = CyberEmerald),
                            modifier = Modifier
                                .size(48.dp)
                                .testTag("chat_voice_note_button")
                        ) {
                            Icon(
                                imageVector = Icons.Default.Mic,
                                contentDescription = "Nota de Voz Cifrada",
                                tint = Color.Black
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun MessageBubble(message: MessageEntity) {
    val isMe = message.isFromMe
    val timeFormat = SimpleDateFormat("HH:mm", Locale.getDefault())
    val formattedTime = timeFormat.format(Date(message.timestamp))

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isMe) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(0.82f),
            colors = CardDefaults.cardColors(
                containerColor = if (isMe) OutgoingBubble else IncomingBubble
            ),
            shape = RoundedCornerShape(
                topStart = 14.dp,
                topEnd = 14.dp,
                bottomStart = if (isMe) 14.dp else 2.dp,
                bottomEnd = if (isMe) 2.dp else 14.dp
            )
        ) {
            Column(modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                Text(
                    text = message.decryptedContent,
                    color = TextPrimary,
                    style = MaterialTheme.typography.bodyMedium,
                    fontSize = 15.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "E2EE",
                        tint = NeonSecurityGreen.copy(alpha = 0.7f),
                        modifier = Modifier.size(11.dp)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formattedTime,
                        fontSize = 11.sp,
                        color = TextMuted
                    )
                    if (isMe) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Icon(
                            imageVector = when (message.status) {
                                "READ" -> Icons.Default.DoneAll
                                "DELIVERED" -> Icons.Default.DoneAll
                                else -> Icons.Default.Done
                            },
                            contentDescription = message.status,
                            tint = if (message.status == "READ") BlueEncryption else TextMuted,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun SecurityPillHeader(
    peerUsername: String,
    fingerprint: String,
    onVerifyClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        contentAlignment = Alignment.Center
    ) {
        Surface(
            color = Color(0xAA182229),
            shape = RoundedCornerShape(12.dp),
            border = androidx.compose.foundation.BorderStroke(1.dp, Color(0x3300E676)),
            modifier = Modifier.clickable { onVerifyClick() }
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Default.Lock,
                    contentDescription = null,
                    tint = NeonSecurityGreen,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Column {
                    Text(
                        text = "Mensagens cifradas de ponta a ponta.",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextPrimary
                    )
                    Text(
                        text = "Toque para verificar os números de segurança de $peerUsername",
                        fontSize = 10.sp,
                        color = NeonSecurityGreen
                    )
                }
            }
        }
    }
}

@Composable
private fun AttachmentPickerSheet(
    onSendPhoto: () -> Unit,
    onSendFile: () -> Unit,
    onSelectBurnTimer: (Int) -> Unit
) {
    Surface(
        color = SurfaceVariantDark,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Compartilhamento com Autodestruição",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = TextPrimary
            )
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                AttachmentOption(
                    icon = Icons.Default.Image,
                    label = "Foto Cifrada",
                    badge = "Autodestrói",
                    onClick = onSendPhoto
                )
                AttachmentOption(
                    icon = Icons.Default.InsertDriveFile,
                    label = "Documento",
                    badge = "AES-256",
                    onClick = onSendFile
                )
                AttachmentOption(
                    icon = Icons.Default.Timer,
                    label = "Tempo: 30s",
                    badge = "Ajustar",
                    onClick = { onSelectBurnTimer(30) }
                )
            }
        }
    }
}

@Composable
private fun AttachmentOption(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    badge: String,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(52.dp)
                .clip(CircleShape)
                .background(CyberEmerald.copy(alpha = 0.2f))
                .border(1.dp, NeonSecurityGreen.copy(alpha = 0.4f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(imageVector = icon, contentDescription = null, tint = NeonSecurityGreen)
        }
        Spacer(modifier = Modifier.height(6.dp))
        Text(text = label, fontSize = 12.sp, color = TextPrimary, fontWeight = FontWeight.SemiBold)
        Text(text = badge, fontSize = 10.sp, color = WarningAmber, fontFamily = FontFamily.Monospace)
    }
}
