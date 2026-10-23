package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.Chat
import androidx.compose.material.icons.filled.Devices
import androidx.compose.material.icons.filled.Key
import androidx.compose.material3.Badge
import androidx.compose.material3.BadgedBox
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.components.BiometricLockScreen
import com.example.ui.components.CallScreen
import com.example.ui.screens.CallsScreen
import com.example.ui.screens.ChatDetailScreen
import com.example.ui.screens.ChatListScreen
import com.example.ui.screens.DevicesSyncScreen
import com.example.ui.screens.NewChatDialog
import com.example.ui.screens.OnboardingScreen
import com.example.ui.screens.VaultSettingsScreen
import com.example.ui.theme.CyberEmerald
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.theme.NeonSecurityGreen
import com.example.ui.theme.ObsidianBg
import com.example.ui.theme.SurfaceDark
import com.example.ui.theme.SurfaceVariantDark
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary
import com.example.viewmodel.MainViewModel

class MainActivity : ComponentActivity() {
    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Production hardening: Protect screen against screenshots and task switcher previews
        window.setFlags(
            android.view.WindowManager.LayoutParams.FLAG_SECURE,
            android.view.WindowManager.LayoutParams.FLAG_SECURE
        )
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                CipherChatApp(viewModel)
            }
        }
    }

    override fun onStop() {
        super.onStop()
        // If biometric is enabled, auto lock on app background
        if (viewModel.userProfile.value?.biometricLockEnabled == true) {
            viewModel.lockApp()
        }
    }
}

@Composable
fun CipherChatApp(viewModel: MainViewModel) {
    val userProfile by viewModel.userProfile.collectAsState()
    val isAppLocked by viewModel.isAppLocked.collectAsState()
    val activeCall by viewModel.activeCall.collectAsState()
    val activeConversationId by viewModel.activeConversationId.collectAsState()
    val conversations by viewModel.conversations.collectAsState()
    val activeMessages by viewModel.activeMessages.collectAsState()
    val linkedDevices by viewModel.linkedDevices.collectAsState()
    val callRecords by viewModel.callRecords.collectAsState()

    var currentTab by remember { mutableIntStateOf(0) }
    var showNewChatDialog by remember { mutableStateOf(false) }

    val callPermissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { /* Permissions result processed */ }

    val safeStartCall = { peer: String, isVideo: Boolean ->
        val permissions = if (isVideo) {
            arrayOf(android.Manifest.permission.CAMERA, android.Manifest.permission.RECORD_AUDIO)
        } else {
            arrayOf(android.Manifest.permission.RECORD_AUDIO)
        }
        callPermissionLauncher.launch(permissions)
        viewModel.startCall(peer, isVideo)
    }

    // 1. Onboarding / Key generation screen if not registered
    if (userProfile == null) {
        OnboardingScreen(
            onGenerateIdentity = { username, enableBiometric ->
                viewModel.registerIdentity(username, enableBiometric)
            }
        )
        return
    }

    val profile = userProfile!!

    // 2. Biometric Lock Screen gate
    if (isAppLocked) {
        BiometricLockScreen(
            username = profile.username,
            onUnlockSuccess = { viewModel.unlockApp() }
        )
        return
    }

    // 3. Active Encrypted Audio/Video Call Screen
    if (activeCall != null) {
        BackHandler {
            viewModel.endCall(0)
        }
        CallScreen(
            peerUsername = activeCall!!.peerUsername,
            isVideoCall = activeCall!!.isVideo,
            onEndCall = { duration ->
                viewModel.endCall(duration)
            }
        )
        return
    }

    // 4. Active Chat Conversation Room
    if (activeConversationId != null) {
        val activeConv = conversations.find { it.id == activeConversationId }
        if (activeConv != null) {
            BackHandler {
                viewModel.closeConversation()
            }
            ChatDetailScreen(
                conversation = activeConv,
                messages = activeMessages,
                myPublicKey = profile.publicKeyBase64,
                isOffline = profile.isAirGapOffline,
                onBack = { viewModel.closeConversation() },
                onSendMessage = { content, type, isBurn, burnSeconds, fileName ->
                    viewModel.sendMessage(
                        conversationId = activeConv.id,
                        content = content,
                        type = type,
                        isBurn = isBurn,
                        burnSeconds = burnSeconds,
                        fileName = fileName
                    )
                },
                onStartVoiceCall = {
                    safeStartCall(activeConv.peerUsername, false)
                },
                onStartVideoCall = {
                    safeStartCall(activeConv.peerUsername, true)
                },
                onBurnExpired = { messageId ->
                    viewModel.burnMessage(messageId)
                },
                onToggleSafetyVerification = { verified ->
                    viewModel.toggleSafetyVerification(activeConv.id, verified)
                }
            )
            return
        }
    }

    // 5. Main Screen with Navigation Tabs
    if (showNewChatDialog) {
        NewChatDialog(
            onAddContact = { username, pubKey ->
                viewModel.addNewContact(username, pubKey)
                showNewChatDialog = false
            },
            onDismiss = { showNewChatDialog = false }
        )
    }

    val totalUnread = conversations.sumOf { it.unreadCount }

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        containerColor = ObsidianBg,
        bottomBar = {
            NavigationBar(
                containerColor = SurfaceDark,
                tonalElevation = 8.dp,
                modifier = Modifier
                    .navigationBarsPadding()
                    .testTag("main_bottom_nav")
            ) {
                // Chats Tab
                NavigationBarItem(
                    selected = currentTab == 0,
                    onClick = { currentTab = 0 },
                    icon = {
                        if (totalUnread > 0) {
                            BadgedBox(badge = { Badge { Text("$totalUnread") } }) {
                                Icon(Icons.Default.Chat, contentDescription = "Conversas")
                            }
                        } else {
                            Icon(Icons.Default.Chat, contentDescription = "Conversas")
                        }
                    },
                    label = { Text("Conversas", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonSecurityGreen,
                        selectedTextColor = NeonSecurityGreen,
                        indicatorColor = SurfaceVariantDark,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("nav_chats_tab")
                )

                // Calls Tab
                NavigationBarItem(
                    selected = currentTab == 1,
                    onClick = { currentTab = 1 },
                    icon = { Icon(Icons.Default.Call, contentDescription = "Chamadas") },
                    label = { Text("Chamadas", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonSecurityGreen,
                        selectedTextColor = NeonSecurityGreen,
                        indicatorColor = SurfaceVariantDark,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("nav_calls_tab")
                )

                // Sync Tab
                NavigationBarItem(
                    selected = currentTab == 2,
                    onClick = { currentTab = 2 },
                    icon = { Icon(Icons.Default.Devices, contentDescription = "Desktop Sync") },
                    label = { Text("Dispositivos", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonSecurityGreen,
                        selectedTextColor = NeonSecurityGreen,
                        indicatorColor = SurfaceVariantDark,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("nav_sync_tab")
                )

                // Vault & Keys Tab
                NavigationBarItem(
                    selected = currentTab == 3,
                    onClick = { currentTab = 3 },
                    icon = { Icon(Icons.Default.Key, contentDescription = "Cofre") },
                    label = { Text("Cofre", fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = NeonSecurityGreen,
                        selectedTextColor = NeonSecurityGreen,
                        indicatorColor = SurfaceVariantDark,
                        unselectedIconColor = TextSecondary,
                        unselectedTextColor = TextSecondary
                    ),
                    modifier = Modifier.testTag("nav_vault_tab")
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (currentTab) {
                0 -> ChatListScreen(
                    conversations = conversations,
                    isOfflineAirGap = profile.isAirGapOffline,
                    unreadTotal = totalUnread,
                    onSelectConversation = { id -> viewModel.openConversation(id) },
                    onToggleOffline = { viewModel.toggleAirGap(!profile.isAirGapOffline) },
                    onLockApp = { viewModel.lockApp() },
                    onNewChatClick = { showNewChatDialog = true }
                )
                1 -> CallsScreen(
                    calls = callRecords,
                    onStartCall = { peer, isVideo -> safeStartCall(peer, isVideo) }
                )
                2 -> DevicesSyncScreen(
                    devices = linkedDevices,
                    myUsername = profile.username,
                    myPublicKey = profile.publicKeyBase64,
                    onSyncNow = { viewModel.syncDevicesNow() },
                    onRevokeDevice = { id -> viewModel.revokeDevice(id) },
                    onAddDevice = { name, type -> viewModel.addLinkedDevice(name, type) }
                )
                3 -> VaultSettingsScreen(
                    profile = profile,
                    onToggleBiometric = { viewModel.toggleBiometric(it) },
                    onToggleOfflineAirGap = { viewModel.toggleAirGap(it) },
                    onChangeBurnTimer = { viewModel.setDefaultBurnTimer(it) },
                    onLockAppNow = { viewModel.lockApp() }
                )
            }
        }
    }
}
