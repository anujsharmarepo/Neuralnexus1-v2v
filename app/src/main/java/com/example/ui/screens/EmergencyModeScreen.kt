package com.example.ui.screens

import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.ui.viewmodel.AbhayaViewModel

@Composable
fun EmergencyModeScreen(
    viewModel: AbhayaViewModel,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isFlashlightOn by viewModel.isFlashlightOn.collectAsStateWithLifecycle()
    val isSirenOn by viewModel.isSirenOn.collectAsStateWithLifecycle()
    val isRecording by viewModel.isRecording.collectAsStateWithLifecycle()
    val recordingSeconds by viewModel.recordingSeconds.collectAsStateWithLifecycle()
    
    val smsStatus by viewModel.smsStatus.collectAsStateWithLifecycle()
    val liveLocationStatus by viewModel.liveLocationStatus.collectAsStateWithLifecycle()
    val callingStatus by viewModel.callingStatus.collectAsStateWithLifecycle()
    val firebaseStatus by viewModel.firebaseStatus.collectAsStateWithLifecycle()
    val gpsCoords by viewModel.gpsCoords.collectAsStateWithLifecycle()
    val correctPin by viewModel.sosPin.collectAsStateWithLifecycle()

    var showPinDialog by remember { mutableStateOf(false) }

    // Pulsating animation for emergency alert header
    val infiniteTransition = rememberInfiniteTransition(label = "beacon")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseScale"
    )

    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0.9f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulseAlpha"
    )

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xFF0B0104)) // Extremely deep dark red/black
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 1. HEADER SECTION (High-Contrast Pulsing Indicator)
            Spacer(modifier = Modifier.height(16.dp))
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(80.dp)
                    .scale(pulseScale)
                    .background(Color(0xFFFF004D).copy(alpha = pulseAlpha * 0.2f), CircleShape)
            ) {
                Box(
                    modifier = Modifier
                        .size(56.dp)
                        .background(Color(0xFFFF004D), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Warning,
                        contentDescription = "Emergency Beacon",
                        tint = Color.White,
                        modifier = Modifier.size(32.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "EMERGENCY ACTIVE",
                fontSize = 28.sp,
                fontWeight = FontWeight.Black,
                color = Color(0xFFFF004D),
                letterSpacing = 2.sp,
                textAlign = TextAlign.Center
            )
            Text(
                text = "Dynamic tracking & safety signals are armed",
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(28.dp))

            // 2. LIVE TELEMETRY / STATUS ITEMS CONTAINER
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1F050D)),
                shape = RoundedCornerShape(24.dp),
                border = BorderStroke(1.5.dp, Color(0xFFFF004D).copy(alpha = 0.3f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(rememberScrollState())
                        .padding(20.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Text(
                        text = "LIVE PROTOCOL TELEMETRY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF004D),
                        letterSpacing = 1.5.sp
                    )

                    Divider(color = Color(0xFFFF004D).copy(alpha = 0.15f))

                    // Status 1: Live Location Shared
                    StatusItemRow(
                        icon = Icons.Default.MyLocation,
                        title = "Live Location Shared",
                        description = liveLocationStatus,
                        active = true,
                        badge = "GPS ACTIVE",
                        extraContent = {
                            Text(
                                text = "Coordinates: ${gpsCoords.first}, ${gpsCoords.second}",
                                fontSize = 11.sp,
                                color = Color.White.copy(alpha = 0.5f),
                                fontWeight = FontWeight.SemiBold,
                                modifier = Modifier.padding(top = 2.dp)
                            )
                        }
                    )

                    // Status 2: SMS Sent Status
                    StatusItemRow(
                        icon = Icons.Default.Sms,
                        title = "SMS Broadcast",
                        description = smsStatus,
                        active = true,
                        badge = "SENT"
                    )

                    // Status 3: Calling Guardian
                    StatusItemRow(
                        icon = Icons.Default.PhoneInTalk,
                        title = "Calling Guardian",
                        description = callingStatus,
                        active = true,
                        badge = "DIALING"
                    )

                    // Status 4: Recording Audio
                    val minutes = recordingSeconds / 60
                    val seconds = recordingSeconds % 60
                    val timerStr = String.format("%02d:%02d", minutes, seconds)
                    StatusItemRow(
                        icon = Icons.Default.Mic,
                        title = "Audio Recording",
                        description = if (isRecording) "Continuous audio proof captured to local storage" else "Audio recorder disabled",
                        active = isRecording,
                        badge = "REC $timerStr",
                        badgeColor = Color(0xFFFF004D)
                    )

                    // Status 5: Firebase Sync
                    StatusItemRow(
                        icon = Icons.Default.CloudUpload,
                        title = "Firebase Database",
                        description = firebaseStatus,
                        active = true,
                        badge = "SECURED"
                    )

                    Divider(color = Color(0xFFFF004D).copy(alpha = 0.15f))

                    // Interactive Hardware Controls
                    Text(
                        text = "HARDWARE OVERRIDES",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF004D),
                        letterSpacing = 1.5.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        // Flashlight Toggle Button
                        Button(
                            onClick = { viewModel.toggleFlashlight() },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .testTag("toggle_flashlight_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isFlashlightOn) Color(0xFFFF004D) else Color(0xFF2D161C)
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = if (isFlashlightOn) Icons.Default.FlashlightOn else Icons.Default.FlashlightOff,
                                contentDescription = "Flashlight",
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isFlashlightOn) "Flashlight ON" else "Flashlight OFF",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }

                        // Siren Toggle Button
                        Button(
                            onClick = { viewModel.toggleSiren() },
                            modifier = Modifier
                                .weight(1f)
                                .height(56.dp)
                                .testTag("toggle_siren_btn"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSirenOn) Color(0xFFFF004D) else Color(0xFF2D161C)
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(
                                imageVector = if (isSirenOn) Icons.Default.VolumeUp else Icons.Default.VolumeMute,
                                contentDescription = "Siren",
                                tint = Color.White
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = if (isSirenOn) "Siren ON" else "Siren OFF",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // 3. I'M SAFE BUTTON
            Button(
                onClick = { showPinDialog = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .testTag("im_safe_button"),
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF10B981) // Emerald Green for safety
                ),
                shape = RoundedCornerShape(32.dp),
                elevation = ButtonDefaults.buttonElevation(defaultElevation = 8.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = "Safe Checkmark",
                        tint = Color.White,
                        modifier = Modifier.size(28.dp)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "I'M SAFE",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color.White,
                        letterSpacing = 1.5.sp
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
        }

        // 4. SECURE PIN DIALOG OVERLAY
        if (showPinDialog) {
            PinEntryDialog(
                correctPin = correctPin,
                onDismiss = { showPinDialog = false },
                onCorrectPinEntered = {
                    showPinDialog = false
                    viewModel.dismissSosAlert()
                    Toast.makeText(context, "Shield Secured. SOS Deactivated.", Toast.LENGTH_SHORT).show()
                }
            )
        }
    }
}

@Composable
fun StatusItemRow(
    icon: ImageVector,
    title: String,
    description: String,
    active: Boolean,
    badge: String,
    badgeColor: Color = Color(0xFF10B981),
    extraContent: @Composable (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .background(Color(0xFF2D161C), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (active) Color(0xFFFF004D) else Color.White.copy(alpha = 0.4f),
                modifier = Modifier.size(20.dp)
            )
        }

        Spacer(modifier = Modifier.width(16.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )

                Box(
                    modifier = Modifier
                        .background(badgeColor.copy(alpha = 0.15f), RoundedCornerShape(4.dp))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = badge,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Black,
                        color = badgeColor
                    )
                }
            }

            Spacer(modifier = Modifier.height(2.dp))
            
            Text(
                text = description,
                fontSize = 12.sp,
                color = Color.White.copy(alpha = 0.7f)
            )

            extraContent?.invoke()
        }
    }
}

@Composable
fun PinEntryDialog(
    correctPin: String,
    onDismiss: () -> Unit,
    onCorrectPinEntered: () -> Unit
) {
    var enteredPin by remember { mutableStateOf("") }
    var pinError by remember { mutableStateOf(false) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1A1A)),
            shape = RoundedCornerShape(24.dp),
            border = BorderStroke(1.dp, Color(0xFFFF004D).copy(alpha = 0.3f)),
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .background(Color(0xFFFF004D).copy(alpha = 0.1f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = null,
                        tint = Color(0xFFFF004D),
                        modifier = Modifier.size(24.dp)
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Security Verification",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White
                )

                Text(
                    text = "Enter 4-digit PIN to deactivate emergency mode",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.6f),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 4.dp)
                )

                Text(
                    text = "Default PIN: 1234",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFF004D),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(bottom = 16.dp)
                )

                // PIN Code Input Field
                OutlinedTextField(
                    value = enteredPin,
                    onValueChange = {
                        if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                            enteredPin = it
                            pinError = false
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("pin_input_field"),
                    textStyle = LocalTextStyle.current.copy(
                        color = Color.White,
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        textAlign = TextAlign.Center
                    ),
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    isError = pinError,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color(0xFFFF004D),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.2f),
                        errorBorderColor = Color(0xFFFF3333)
                    ),
                    shape = RoundedCornerShape(12.dp)
                )

                if (pinError) {
                    Text(
                        text = "Invalid Security PIN. Please try again.",
                        color = Color(0xFFFF3333),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    TextButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("CANCEL", color = Color.White.copy(alpha = 0.6f), fontWeight = FontWeight.Bold)
                    }

                    Button(
                        onClick = {
                            if (enteredPin == correctPin) {
                                onCorrectPinEntered()
                            } else {
                                pinError = true
                                enteredPin = ""
                            }
                        },
                        modifier = Modifier
                            .weight(1f)
                            .testTag("submit_pin_button"),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF004D)),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text("VERIFY", fontWeight = FontWeight.Bold, color = Color.White)
                    }
                }
            }
        }
    }
}
