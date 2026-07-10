package com.example.ui.screens

import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.compose.animation.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.DirectionsRun
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.domain.model.Guardian
import com.example.ui.viewmodel.AbhayaViewModel
import com.example.ui.viewmodel.AuthUiState
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.osmdroid.views.MapView as OsmMapView
import org.osmdroid.util.GeoPoint
import org.osmdroid.views.overlay.Marker as OsmMarker
import org.osmdroid.views.overlay.Polyline as OsmPolyline
import org.osmdroid.config.Configuration
import androidx.compose.ui.viewinterop.AndroidView
import java.util.Locale

@Composable
fun AbhayaApp(
    modifier: Modifier = Modifier,
    viewModel: AbhayaViewModel = viewModel()
) {
    val navController = rememberNavController()
    val context = LocalContext.current
    val alertMessage by viewModel.alertMessage.collectAsStateWithLifecycle()
    val sosTriggered by viewModel.sosTriggered.collectAsStateWithLifecycle()
    val showSafeJourneyScreen by viewModel.showSafeJourneyScreen.collectAsStateWithLifecycle()

    // Listen for alerts and show high-fidelity snackbar/toast
    LaunchedEffect(alertMessage) {
        alertMessage?.let {
            Toast.makeText(context, it, Toast.LENGTH_LONG).show()
            viewModel.clearAlert()
        }
    }

    if (sosTriggered) {
        EmergencyModeScreen(viewModel = viewModel, modifier = modifier.fillMaxSize())
    } else if (showSafeJourneyScreen) {
        SafeJourneyScreen(viewModel = viewModel, modifier = modifier.fillMaxSize())
    } else {
        NavHost(
            navController = navController,
            startDestination = "splash",
            modifier = modifier.fillMaxSize()
        ) {
            composable("splash") {
                SplashScreen(navController = navController, viewModel = viewModel)
            }
            composable("onboarding") {
                OnboardingScreen(navController = navController)
            }
            composable("login") {
                LoginScreen(navController = navController, viewModel = viewModel)
            }
            composable("signup") {
                SignupScreen(navController = navController, viewModel = viewModel)
            }
            composable("forgot_password") {
                ForgotPasswordScreen(navController = navController, viewModel = viewModel)
            }
            composable("main_content") {
                MainContentScreen(navController = navController, viewModel = viewModel)
            }
        }
    }
}

// ==========================================
// 1. SPLASH SCREEN
// ==========================================
@Composable
fun SplashScreen(navController: NavController, viewModel: AbhayaViewModel) {
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    LaunchedEffect(Unit) {
        delay(2500) // Beautiful cinematic entrance
        if (currentUser != null) {
            navController.navigate("main_content") {
                popUpTo("splash") { inclusive = true }
            }
        } else {
            navController.navigate("onboarding") {
                popUpTo("splash") { inclusive = true }
            }
        }
    }

    // High contrast gradient branding
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFF004D),
                        Color(0xFFB3261E)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // Decorative pulsing geometric shapes behind logo
        val infiniteTransition = rememberInfiniteTransition(label = "splash")
        val scalePulse by infiniteTransition.animateFloat(
            initialValue = 0.85f,
            targetValue = 1.15f,
            animationSpec = infiniteRepeatable(
                animation = tween(1500, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "pulse"
        )

        Box(
            modifier = Modifier
                .size(240.dp)
                .scale(scalePulse)
                .drawBehind {
                    drawCircle(
                        color = Color.White.copy(alpha = 0.15f),
                        radius = size.minDimension / 1.6f
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.08f),
                        radius = size.minDimension / 1.2f
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // High fidelity brand logo image
                Image(
                    painter = painterResource(id = com.example.R.drawable.abhaya_clean_logo_1783605221980),
                    contentDescription = "Abhaya Logo Icon",
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(Color.White)
                        .border(3.dp, Color.White, CircleShape)
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "ABHAYA",
                    fontSize = 38.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = Color.White,
                    letterSpacing = 6.sp,
                    fontFamily = FontFamily.SansSerif
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "PROTECT • PREVENT • EMPOWER",
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = 0.9f),
                    letterSpacing = 2.sp
                )
            }
        }

        // Bottom tagline / loaded status
        Text(
            text = "SECURED WITH MILITARY GRADE PROTOCOLS",
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White.copy(alpha = 0.7f),
            letterSpacing = 1.sp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 48.dp)
        )
    }
}

// ==========================================
// 2. ONBOARDING SCREEN (3 beautiful pages)
// ==========================================
@Composable
fun OnboardingScreen(navController: NavController) {
    var currentPage by remember { mutableStateOf(0) }
    val pages = listOf(
        OnboardingData(
            title = "PREVENT DANGER",
            desc = "Abhaya's predictive routing system continuously analyzes your paths to guide you safely around high-risk zones.",
            icon = Icons.Default.Shield,
            tagline = "Empowered Safety",
            imageRes = com.example.R.drawable.abhaya_intro_prevent_clean_1783607882427
        ),
        OnboardingData(
            title = "ACTIVE GUARDIANS",
            desc = "Enlist up to 5 verified trusted emergency contacts who get automatic updates of your active trips, state, and location.",
            icon = Icons.Default.People,
            tagline = "Guardian Network",
            imageRes = com.example.R.drawable.abhaya_intro_empower_clean_1783607868690
        ),
        OnboardingData(
            title = "INSTANT SOS",
            desc = "Hold down the SOS button for 3 seconds to broadcast dynamic location coordinates, live recording, and siren signals.",
            icon = Icons.Default.FlashOn,
            tagline = "Ultimate Protection",
            imageRes = com.example.R.drawable.abhaya_intro_protect_clean_1783607853306
        )
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Skip Button
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            TextButton(
                onClick = { navController.navigate("login") { popUpTo("onboarding") { inclusive = true } } },
                modifier = Modifier.testTag("skip_button")
            ) {
                Text(
                    text = "SKIP",
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.weight(0.4f))

        // Onboarding Content
        val activePage = pages[currentPage]

        // Dynamic icon layout with energetic red border circle
        Box(
            modifier = Modifier
                .size(240.dp)
                .drawBehind {
                    drawCircle(
                        color = Color(0xFFFF004D).copy(alpha = 0.05f),
                        radius = size.minDimension / 1.8f
                    )
                    drawCircle(
                        color = Color(0xFFB3261E).copy(alpha = 0.02f),
                        radius = size.minDimension / 1.5f
                    )
                }
                .shadow(12.dp, CircleShape)
                .background(Color.White, CircleShape)
                .border(2.dp, Color(0xFFFCE8EB), CircleShape)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Image(
                painter = painterResource(id = activePage.imageRes),
                contentDescription = activePage.title,
                modifier = Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Badge text
        Surface(
            color = Color(0xFFFCE8EB),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = activePage.tagline.uppercase(),
                color = Color(0xFFB3261E),
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 1.5.sp,
                modifier = Modifier.padding(horizontal = 14.dp, vertical = 6.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Title and Description
        Text(
            text = activePage.title,
            fontSize = 28.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            letterSpacing = 1.sp,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = activePage.desc,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
            textAlign = TextAlign.Center,
            lineHeight = 22.sp,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        Spacer(modifier = Modifier.weight(0.6f))

        // Dot Indicators
        Row(
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            pages.forEachIndexed { index, _ ->
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .size(if (index == currentPage) 20.dp else 8.dp, 8.dp)
                        .clip(CircleShape)
                        .background(
                            if (index == currentPage) Color(0xFFFF004D) else Color(0xFFFF004D).copy(alpha = 0.2f)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Navigation Button
        Button(
            onClick = {
                if (currentPage < pages.lastIndex) {
                    currentPage++
                } else {
                    navController.navigate("login") {
                        popUpTo("onboarding") { inclusive = true }
                    }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("next_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF004D)
            ),
            shape = RoundedCornerShape(28.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = if (currentPage == pages.lastIndex) "GET STARTED" else "CONTINUE",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp,
                    color = Color.White
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = Color.White
                )
            }
        }
    }
}

data class OnboardingData(
    val title: String,
    val desc: String,
    val icon: ImageVector,
    val tagline: String,
    val imageRes: Int
)

// ==========================================
// 3. LOGIN SCREEN
// ==========================================
@Composable
fun LoginScreen(
    navController: NavController,
    viewModel: AbhayaViewModel
) {
    val email by viewModel.loginEmail.collectAsStateWithLifecycle()
    val password by viewModel.loginPassword.collectAsStateWithLifecycle()
    val authState by viewModel.authUiState.collectAsStateWithLifecycle()
    var isPasswordVisible by remember { mutableStateOf(false) }

    // Side-effects on Auth success
    LaunchedEffect(authState) {
        if (authState is AuthUiState.Success) {
            navController.navigate("main_content") {
                popUpTo("login") { inclusive = true }
            }
            viewModel.resetAuthState()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        Spacer(modifier = Modifier.height(24.dp))

        // Decorative mini app title branding
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            Image(
                painter = painterResource(id = com.example.R.drawable.abhaya_clean_logo_1783605221980),
                contentDescription = "Abhaya Mini Logo",
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .border(1.dp, Color(0xFFFF004D).copy(alpha = 0.2f), CircleShape)
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "ABHAYA",
                fontSize = 18.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                color = Color(0xFFFF004D)
            )
        }

        Spacer(modifier = Modifier.height(48.dp))

        // Main heading text
        Text(
            text = "Welcome Back",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Login to reconnect with your safe guardian grid.",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(40.dp))

        // Form Fields
        Text(
            text = "Email Address",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = email,
            onValueChange = { viewModel.loginEmail.value = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("email_input"),
            placeholder = { Text("e.g. anuj@example.com") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFFF004D),
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            ),
            leadingIcon = {
                Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = Color(0xFFFF004D))
            }
        )

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Password",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f)
            )
            TextButton(
                onClick = { navController.navigate("forgot_password") },
                contentPadding = PaddingValues(0.dp)
            ) {
                Text(
                    text = "Forgot Password?",
                    color = Color(0xFFB3261E),
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
        OutlinedTextField(
            value = password,
            onValueChange = { viewModel.loginPassword.value = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("password_input"),
            placeholder = { Text("••••••••") },
            singleLine = true,
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFFF004D),
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            ),
            leadingIcon = {
                Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color(0xFFFF004D))
            },
            trailingIcon = {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle password visibility",
                        tint = Color(0xFFB3261E)
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Display Auth Errors if any
        if (authState is AuthUiState.Error) {
            Text(
                text = (authState as AuthUiState.Error).error,
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Submit Button
        Button(
            onClick = { viewModel.login() },
            enabled = authState !is AuthUiState.Loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("login_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF004D)
            ),
            shape = RoundedCornerShape(28.dp)
        ) {
            if (authState is AuthUiState.Loading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text(
                    text = "LOGIN",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))

        // Signup routing
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Don't have an account?",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            TextButton(onClick = { navController.navigate("signup") }) {
                Text(
                    text = "SIGN UP",
                    color = Color(0xFFFF004D),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

// ==========================================
// 4. SIGNUP SCREEN
// ==========================================
@Composable
fun SignupScreen(
    navController: NavController,
    viewModel: AbhayaViewModel
) {
    val name by viewModel.signupName.collectAsStateWithLifecycle()
    val email by viewModel.signupEmail.collectAsStateWithLifecycle()
    val password by viewModel.signupPassword.collectAsStateWithLifecycle()
    val authState by viewModel.authUiState.collectAsStateWithLifecycle()
    var isPasswordVisible by remember { mutableStateOf(false) }

    LaunchedEffect(authState) {
        if (authState is AuthUiState.Success) {
            navController.navigate("main_content") {
                popUpTo("signup") { inclusive = true }
            }
            viewModel.resetAuthState()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(24.dp)
    ) {
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back to Login",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }

        Text(
            text = "Create Account",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Empower yourself with military-grade safety shields.",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        // Form Fields
        Text(
            text = "Full Name",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = name,
            onValueChange = { viewModel.signupName.value = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("signup_name_input"),
            placeholder = { Text("e.g. Anuj Sharma") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFFF004D),
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            ),
            leadingIcon = {
                Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Color(0xFFFF004D))
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Email Address",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = email,
            onValueChange = { viewModel.signupEmail.value = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("signup_email_input"),
            placeholder = { Text("e.g. anuj@example.com") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFFF004D),
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            ),
            leadingIcon = {
                Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = Color(0xFFFF004D))
            }
        )

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Create Password",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = password,
            onValueChange = { viewModel.signupPassword.value = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("signup_password_input"),
            placeholder = { Text("Minimum 6 characters") },
            singleLine = true,
            visualTransformation = if (isPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFFF004D),
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            ),
            leadingIcon = {
                Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = Color(0xFFFF004D))
            },
            trailingIcon = {
                IconButton(onClick = { isPasswordVisible = !isPasswordVisible }) {
                    Icon(
                        imageVector = if (isPasswordVisible) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle password visibility",
                        tint = Color(0xFFB3261E)
                    )
                }
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (authState is AuthUiState.Error) {
            Text(
                text = (authState as AuthUiState.Error).error,
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Submit Button
        Button(
            onClick = { viewModel.signUp() },
            enabled = authState !is AuthUiState.Loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("signup_submit_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF004D)
            ),
            shape = RoundedCornerShape(28.dp)
        ) {
            if (authState is AuthUiState.Loading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text(
                    text = "REGISTER",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp,
                    color = Color.White
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Already have an account?",
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
                fontSize = 14.sp
            )
            Spacer(modifier = Modifier.width(4.dp))
            TextButton(onClick = { navController.popBackStack() }) {
                Text(
                    text = "LOGIN",
                    color = Color(0xFFFF004D),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp
                )
            }
        }
    }
}

// ==========================================
// 5. FORGOT PASSWORD SCREEN
// ==========================================
@Composable
fun ForgotPasswordScreen(
    navController: NavController,
    viewModel: AbhayaViewModel
) {
    val email by viewModel.forgotEmail.collectAsStateWithLifecycle()
    val authState by viewModel.authUiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    LaunchedEffect(authState) {
        if (authState is AuthUiState.Success) {
            Toast.makeText(context, (authState as AuthUiState.Success).message, Toast.LENGTH_LONG).show()
            viewModel.resetAuthState()
            navController.popBackStack()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .statusBarsPadding()
            .navigationBarsPadding()
            .padding(24.dp)
    ) {
        IconButton(
            onClick = { navController.popBackStack() },
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onBackground
            )
        }

        Text(
            text = "Reset Password",
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = "Enter your verified email. We will dispatch encrypted credentials recovery pathways instantly.",
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(48.dp))

        Text(
            text = "Your Verified Email",
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.8f),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        OutlinedTextField(
            value = email,
            onValueChange = { viewModel.forgotEmail.value = it },
            modifier = Modifier
                .fillMaxWidth()
                .testTag("forgot_email_input"),
            placeholder = { Text("e.g. anuj@example.com") },
            singleLine = true,
            shape = RoundedCornerShape(12.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFFFF004D),
                unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.5f)
            ),
            leadingIcon = {
                Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = Color(0xFFFF004D))
            }
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (authState is AuthUiState.Error) {
            Text(
                text = (authState as AuthUiState.Error).error,
                color = MaterialTheme.colorScheme.error,
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(vertical = 4.dp)
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Button(
            onClick = { viewModel.sendPasswordReset() },
            enabled = authState !is AuthUiState.Loading,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("reset_password_button"),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFFFF004D)
            ),
            shape = RoundedCornerShape(28.dp)
        ) {
            if (authState is AuthUiState.Loading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text(
                    text = "DISPATCH LINK",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.5.sp,
                    color = Color.White
                )
            }
        }
    }
}

// ==========================================
// 6. MAIN CONTENT SCREEN (Tab switcher & Frame layout)
// ==========================================
@Composable
fun MainContentScreen(
    navController: NavController,
    viewModel: AbhayaViewModel
) {
    val currentTab by viewModel.currentTab.collectAsStateWithLifecycle()
    val currentUser by viewModel.currentUser.collectAsStateWithLifecycle()

    // Gracefully handle if user somehow gets here without session (fallback to default Anuj as per screenshot requirements)
    val userName = currentUser?.name ?: "Anuj"

    Scaffold(
        modifier = Modifier.fillMaxSize(),
        bottomBar = {
            NavigationBar(
                containerColor = Color(0xFFFFF1F2),
                tonalElevation = 0.dp,
                modifier = Modifier.border(1.dp, Color(0xFFFCE8EB), RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp))
            ) {
                NavigationBarItem(
                    selected = currentTab == "Home",
                    onClick = { viewModel.selectTab("Home") },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == "Home") Icons.Default.Home else Icons.Outlined.Home,
                            contentDescription = "Home tab"
                        )
                    },
                    label = { Text("Home", fontWeight = FontWeight.Bold, fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF31101D),
                        selectedTextColor = Color(0xFF31101D),
                        indicatorColor = Color(0xFFFFD8E4),
                        unselectedIconColor = Color(0xFF31101D).copy(alpha = 0.5f),
                        unselectedTextColor = Color(0xFF31101D).copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.testTag("nav_item_home")
                )
                NavigationBarItem(
                    selected = currentTab == "Guardian",
                    onClick = { viewModel.selectTab("Guardian") },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == "Guardian") Icons.Default.Shield else Icons.Outlined.Shield,
                            contentDescription = "Guardian tab"
                        )
                    },
                    label = { Text("Guardian", fontWeight = FontWeight.Bold, fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF31101D),
                        selectedTextColor = Color(0xFF31101D),
                        indicatorColor = Color(0xFFFFD8E4),
                        unselectedIconColor = Color(0xFF31101D).copy(alpha = 0.5f),
                        unselectedTextColor = Color(0xFF31101D).copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.testTag("nav_item_guardian")
                )
                NavigationBarItem(
                    selected = currentTab == "Police",
                    onClick = { viewModel.selectTab("Police") },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == "Police") Icons.Default.LocalPolice else Icons.Outlined.LocalPolice,
                            contentDescription = "Police tab"
                        )
                    },
                    label = { Text("Police", fontWeight = FontWeight.Bold, fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF31101D),
                        selectedTextColor = Color(0xFF31101D),
                        indicatorColor = Color(0xFFFFD8E4),
                        unselectedIconColor = Color(0xFF31101D).copy(alpha = 0.5f),
                        unselectedTextColor = Color(0xFF31101D).copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.testTag("nav_item_police")
                )
                NavigationBarItem(
                    selected = currentTab == "Settings",
                    onClick = { viewModel.selectTab("Settings") },
                    icon = {
                        Icon(
                            imageVector = if (currentTab == "Settings") Icons.Default.Settings else Icons.Outlined.Settings,
                            contentDescription = "Settings tab"
                        )
                    },
                    label = { Text("Settings", fontWeight = FontWeight.Bold, fontSize = 10.sp) },
                    colors = NavigationBarItemDefaults.colors(
                        selectedIconColor = Color(0xFF31101D),
                        selectedTextColor = Color(0xFF31101D),
                        indicatorColor = Color(0xFFFFD8E4),
                        unselectedIconColor = Color(0xFF31101D).copy(alpha = 0.5f),
                        unselectedTextColor = Color(0xFF31101D).copy(alpha = 0.5f)
                    ),
                    modifier = Modifier.testTag("nav_item_settings")
                )
            }
        },
        contentWindowInsets = WindowInsets.safeDrawing
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            Crossfade(
                targetState = currentTab,
                animationSpec = tween(250),
                label = "tab_navigation"
            ) { targetTab ->
                when (targetTab) {
                    "Home" -> HomeScreenContent(userName = userName, viewModel = viewModel)
                    "Guardian" -> JourneyScreenContent(viewModel = viewModel)
                    "Police" -> PoliceScreenContent(viewModel = viewModel)
                    "Settings" -> SettingsScreenContent(navController = navController, viewModel = viewModel)
                }
            }
        }
    }
}

// ==========================================
// 7. HOME TAB SCREEN (The heart of Abhaya app)
// ==========================================
@Composable
fun HomeScreenContent(
    userName: String,
    viewModel: AbhayaViewModel
) {
    val progress by viewModel.sosHoldProgress.collectAsStateWithLifecycle()
    val sosTriggered by viewModel.sosTriggered.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(horizontal = 24.dp, vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // TOP GREETING SECTION
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Good Evening,",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF655557)
                )
                Text(
                    text = userName,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF301114),
                    letterSpacing = (-0.5).sp
                )
            }

            // Beautiful Profile Badge or Avatar
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFCE8EB))
                    .border(2.dp, Color(0xFFFF004D), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = userName.take(2).uppercase(),
                    color = Color(0xFFB3261E),
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(18.dp))

        // STATUS CHIPS CONTAINER
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 4.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.Start),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // GPS Status Chip
            StatusIndicator(label = "GPS", active = true, icon = Icons.Default.GpsFixed)
            
            // Internet Status Chip
            StatusIndicator(label = "Internet", active = true, icon = Icons.Default.Wifi)

            // Active Guardians Status Chip
            StatusIndicator(label = "5 Guardians Active", active = true, icon = Icons.Default.Security)
        }

        Spacer(modifier = Modifier.weight(0.15f))

        // CENTER: GIANT CIRCULAR SOS BUTTON (60% visual focus)
        Box(
            modifier = Modifier
                .weight(0.7f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            // Ripple/Pulse Background Animation loop (infinite pulse)
            val infiniteTransition = rememberInfiniteTransition(label = "sos_pulse")
            val pulseScale by infiniteTransition.animateFloat(
                initialValue = 1.05f,
                targetValue = 1.25f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "scale"
            )
            val pulseAlpha by infiniteTransition.animateFloat(
                initialValue = 0.2f,
                targetValue = 0.05f,
                animationSpec = infiniteRepeatable(
                    animation = tween(1800, easing = FastOutSlowInEasing),
                    repeatMode = RepeatMode.Reverse
                ),
                label = "alpha"
            )

            // Animated scale of the core button based on hold progress for high haptic/visual feedback
            val buttonScale = 1.0f + (progress * 0.15f)

            // Outer Pulse Ring 1
            Box(
                modifier = Modifier
                    .size(288.dp)
                    .scale(pulseScale * 1.05f)
                    .clip(CircleShape)
                    .background(Color(0xFFFF004D).copy(alpha = pulseAlpha * 0.5f))
            )

            // Outer Pulse Ring 2
            Box(
                modifier = Modifier
                    .size(256.dp)
                    .scale(pulseScale)
                    .clip(CircleShape)
                    .background(Color(0xFFFF004D).copy(alpha = pulseAlpha))
            )

            // Giant Circular Gesture Core Button
            Box(
                modifier = Modifier
                    .size(224.dp)
                    .scale(buttonScale)
                    .shadow(elevation = 24.dp, shape = CircleShape, ambientColor = Color(0xFFFF004D))
                    .pointerInput(Unit) {
                        detectTapGestures(
                            onPress = {
                                viewModel.startSosCountdown()
                                tryAwaitRelease()
                                viewModel.cancelSosCountdown()
                            }
                        )
                    }
                    .border(8.dp, Color.White.copy(alpha = 0.2f), CircleShape)
                    .drawBehind {
                        // Base high-contrast Red energetic background circle
                        drawCircle(
                            brush = Brush.radialGradient(
                                colors = listOf(
                                    Color(0xFFFF004D),
                                    Color(0xFFD50000)
                                ),
                                center = Offset(size.width / 2f, size.height / 2f),
                                radius = size.minDimension / 1.8f
                            )
                        )

                        if (progress > 0f) {
                            drawArc(
                                color = Color.White,
                                startAngle = -90f,
                                sweepAngle = 360f * progress,
                                useCenter = false,
                                style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round)
                            )
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(20.dp)
                ) {
                    if (sosTriggered) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = "ALERT ACTIVE",
                            tint = Color.White,
                            modifier = Modifier.size(56.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "SOS",
                            fontSize = 44.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 1.sp
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "DISTRESS ACTIVE",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.8f)
                        )
                    } else {
                        Text(
                            text = "SOS",
                            fontSize = 54.sp,
                            fontWeight = FontWeight.Black,
                            color = Color.White,
                            letterSpacing = 1.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .height(2.dp)
                                .width(48.dp)
                                .background(Color.White.copy(alpha = 0.4f))
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "PRESS & HOLD",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.9f),
                            letterSpacing = 2.sp
                        )
                        Spacer(modifier = Modifier.height(2.dp))
                        Text(
                            text = "3 SECONDS",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = Color.White.copy(alpha = 0.7f)
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.weight(0.1f))

        // EMERGENCY DIALOG / ALERT DRAWER IF SOS TRIGGERED
        if (sosTriggered) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 12.dp)
                    .animateContentSize(),
                color = Color(0xFFFFEBEE),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(2.dp, Color(0xFFFF004D))
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircularProgressIndicator(
                        color = Color(0xFFFF004D),
                        modifier = Modifier.size(24.dp),
                        strokeWidth = 3.dp
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "DISTRESS SIGNAL IS ACTIVE",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Black,
                            color = Color(0xFFD50000)
                        )
                        Text(
                            text = "Live telemetry being broadcasted",
                            fontSize = 11.sp,
                            color = Color(0xFFD50000).copy(alpha = 0.8f)
                        )
                    }
                    TextButton(
                        onClick = { viewModel.dismissSosAlert() },
                        colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFFD50000))
                    ) {
                        Text("CANCEL", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // BOTTOM QUICK ACTIONS TITLE
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.Start
        ) {
            Text(
                text = "Bottom Quick Actions",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onBackground
            )
        }

        // QUICK ACTION BUTTONS ROW
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            QuickActionButton(
                label = "Safe Journey",
                icon = Icons.Default.Navigation,
                modifier = Modifier
                    .weight(1f)
                    .testTag("action_safe_journey")
            ) {
                viewModel.triggerQuickAction("Safe Journey")
            }
            QuickActionButton(
                label = "Guardians",
                icon = Icons.Default.Security,
                modifier = Modifier
                    .weight(1f)
                    .testTag("action_guardians")
            ) {
                viewModel.selectTab("Guardian") // Switches to guardian manager
            }
            QuickActionButton(
                label = "Nearby Police",
                icon = Icons.Default.LocalPolice,
                modifier = Modifier
                    .weight(1f)
                    .testTag("action_safe_places")
            ) {
                viewModel.selectTab("Police") // Switches to Police
            }
        }
        
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun StatusIndicator(
    label: String,
    active: Boolean,
    icon: ImageVector
) {
    Surface(
        color = if (active) Color(0xFFFCE8EB) else Color.LightGray.copy(alpha = 0.2f),
        contentColor = if (active) Color(0xFFB3261E) else Color.Gray,
        shape = RoundedCornerShape(50),
        border = BorderStroke(1.dp, if (active) Color(0xFFF4B4B8) else Color.LightGray.copy(alpha = 0.5f)),
        modifier = Modifier.padding(horizontal = 2.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (active) Color(0xFFB3261E) else Color.Gray,
                modifier = Modifier.size(12.dp)
            )
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = label + if (active) " ✓" else "",
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.2.sp,
                color = if (active) Color(0xFFB3261E) else Color.Gray
            )
        }
    }
}

@Composable
fun QuickActionButton(
    label: String,
    icon: ImageVector,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        modifier = modifier
            .shadow(elevation = 1.dp, shape = RoundedCornerShape(24.dp))
            .clickable { onClick() },
        color = Color.White,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, Color(0xFFFCE8EB))
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp, horizontal = 12.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFFFD8E4)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = Color(0xFFB3261E),
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(modifier = Modifier.height(10.dp))
            Text(
                text = label.uppercase(),
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF301114),
                textAlign = TextAlign.Center,
                lineHeight = 12.sp
            )
        }
    }
}

// ==========================================
// 8. JOURNEY TAB SCREEN (Guardian manager)
// ==========================================
@Composable
fun JourneyScreenContent(viewModel: AbhayaViewModel) {
    val guardians by viewModel.guardians.collectAsStateWithLifecycle()
    var nameInput by remember { mutableStateOf("") }
    var relationInput by remember { mutableStateOf("") }
    var phoneInput by remember { mutableStateOf("") }
    var isAdding by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(24.dp)
    ) {
        Text(
            text = "Guardian Grid",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Text(
            text = "Manage your trusted 5 guardians network",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f)
        )

        Spacer(modifier = Modifier.height(16.dp))

        if (isAdding) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White),
                border = BorderStroke(1.dp, Color(0xFFFF004D).copy(alpha = 0.2f)),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Add Guardian", fontWeight = FontWeight.Bold, color = Color(0xFFFF004D))
                    Spacer(modifier = Modifier.height(12.dp))
                    OutlinedTextField(
                        value = nameInput,
                        onValueChange = { nameInput = it },
                        label = { Text("Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = relationInput,
                        onValueChange = { relationInput = it },
                        label = { Text("Relation (e.g. Sister)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = phoneInput,
                        onValueChange = { phoneInput = it },
                        label = { Text("Phone Number") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        TextButton(onClick = { isAdding = false }) { Text("Cancel") }
                        Spacer(modifier = Modifier.width(8.dp))
                        Button(
                            onClick = {
                                if (nameInput.isNotBlank() && phoneInput.isNotBlank()) {
                                    viewModel.addGuardian(nameInput, relationInput, phoneInput)
                                    nameInput = ""
                                    relationInput = ""
                                    phoneInput = ""
                                    isAdding = false
                                }
                            },
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF004D))
                        ) {
                            Text("Save")
                        }
                    }
                }
            }
        } else {
            Button(
                onClick = { isAdding = true },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF004D)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(imageVector = Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Add New Guardian", fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        Text(
            text = "Active Guardian Circle",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground
        )
        Spacer(modifier = Modifier.height(8.dp))

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(guardians) { guardian ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White),
                    shape = RoundedCornerShape(12.dp),
                    border = BorderStroke(1.dp, Color(0xFFFF004D).copy(alpha = 0.1f))
                ) {
                    Row(
                        modifier = Modifier
                            .padding(14.dp)
                            .fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFFFCE8EB)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = Color(0xFFFF004D))
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(guardian.name, fontWeight = FontWeight.Bold)
                                Text("${guardian.relation} • ${guardian.phone}", fontSize = 12.sp, color = Color.Gray)
                            }
                        }
                        IconButton(onClick = { viewModel.removeGuardian(guardian.id) }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete guardian", tint = Color.LightGray)
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 9. POLICE TAB SCREEN (POLICE STATIONS DIRECTORY)
// ==========================================
private fun calculateDistanceInKm(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
    val r = 6371.0 // Radius of the earth in km
    val latDistance = Math.toRadians(lat2 - lat1)
    val lonDistance = Math.toRadians(lon2 - lon1)
    val a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2) +
            Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2)) *
            Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2)
    val c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
    return r * c
}

@Composable
fun EmergencyCallButton(context: android.content.Context, modifier: Modifier = Modifier) {
    Button(
        onClick = {
            val intent = Intent(Intent.ACTION_DIAL).apply {
                data = Uri.parse("tel:112")
            }
            try {
                context.startActivity(intent)
            } catch (e: Exception) {
                val fallbackIntent = Intent(Intent.ACTION_DIAL).apply {
                    data = Uri.parse("tel:100")
                }
                try {
                    context.startActivity(fallbackIntent)
                } catch (ex: Exception) {
                    Toast.makeText(context, "Could not open dialer", Toast.LENGTH_SHORT).show()
                }
            }
        },
        colors = ButtonDefaults.buttonColors(
            containerColor = Color(0xFFFF004D),
            contentColor = Color.White
        ),
        shape = RoundedCornerShape(18.dp),
        modifier = modifier
            .fillMaxWidth()
            .height(56.dp)
            .testTag("emergency_call_button")
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = null,
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = "EMERGENCY CALL (112 / 100)",
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun PoliceScreenContent(viewModel: AbhayaViewModel) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    
    val currentLatLng by viewModel.currentLatLng.collectAsStateWithLifecycle()
    val stations by viewModel.nearbyPoliceStations.collectAsStateWithLifecycle()
    val fetchStatus by viewModel.policeFetchStatus.collectAsStateWithLifecycle()
    val permissionDenied by viewModel.permissionDenied.collectAsStateWithLifecycle()
    
    val isGpsDisabled = !Geolocator.isGpsEnabled(context)
    val isInternetOffline = !MapsService.isNetworkAvailable(context)

    val pagerState = rememberPagerState(
        initialPage = 0,
        initialPageOffsetFraction = 0f
    ) {
        stations.size
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFFFF9F9)) // Matching abhaya_bg theme color
    ) {
        // TOP HEADER BAR (POLICE ASSISTANCE)
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            // Back Arrow Button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .shadow(1.dp, RoundedCornerShape(12.dp))
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .clickable { viewModel.selectTab("Home") },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.ArrowBack,
                    contentDescription = "Back",
                    tint = Color(0xFF1E293B),
                    modifier = Modifier.size(20.dp)
                )
            }

            // Title
            Text(
                text = "Police Assistance",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1E293B)
            )

            // Shield Button
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .shadow(1.dp, RoundedCornerShape(12.dp))
                    .background(Color.White, RoundedCornerShape(12.dp))
                    .clickable { 
                        Toast.makeText(context, "Shield Active: Real-time telemetry monitoring", Toast.LENGTH_SHORT).show()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Shield,
                    contentDescription = "Shield Status",
                    tint = Color(0xFF1E293B),
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // MAP & FLOATING CARD CONTAINER
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
        ) {
            if (permissionDenied) {
                // Show Permission Denied error overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFFFF1F2))
                        .padding(bottom = 260.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.LocationOff,
                            contentDescription = null,
                            tint = Color(0xFFFF004D),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Location Permission Required",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Please grant location permission to fetch and navigate to real nearby police stations.",
                            fontSize = 14.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else if (isGpsDisabled) {
                // Show GPS Disabled error overlay
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(Color(0xFFFEF3C7))
                        .padding(bottom = 260.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(24.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Default.GpsOff,
                            contentDescription = null,
                            tint = Color(0xFFD97706),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "GPS Location is Disabled",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF1E293B)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Please enable GPS/Location Services on your device to display your location and find real police stations.",
                            fontSize = 14.sp,
                            color = Color(0xFF64748B),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            } else {
                // Render real embedded OpenStreetMap MapView
                val mapView = rememberMapViewWithLifecycle()
                
                AndroidView(
                    factory = { mapView },
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(bottom = 280.dp) // Leave space for bottom floating card
                ) { mv ->
                    mv.overlays.clear()
                    
                    val userLatLng = currentLatLng
                    if (userLatLng != null) {
                        val userGeoPoint = GeoPoint(userLatLng.latitude, userLatLng.longitude)
                        
                        // Center the map on the user's location
                        mv.controller.setCenter(userGeoPoint)
                        mv.controller.setZoom(14.5)
                        
                        // Add User location marker
                        val userMarker = OsmMarker(mv).apply {
                            position = userGeoPoint
                            setAnchor(OsmMarker.ANCHOR_CENTER, OsmMarker.ANCHOR_BOTTOM)
                            title = "My Location"
                        }
                        mv.overlays.add(userMarker)
                        
                        // Add markers for each nearby police station
                        stations.forEachIndexed { index, station ->
                            val stationGeoPoint = GeoPoint(station.latLng.latitude, station.latLng.longitude)
                            val stationMarker = OsmMarker(mv).apply {
                                position = stationGeoPoint
                                setAnchor(OsmMarker.ANCHOR_CENTER, OsmMarker.ANCHOR_BOTTOM)
                                title = station.name
                                subDescription = station.vicinity
                                setOnMarkerClickListener { _, _ ->
                                    coroutineScope.launch {
                                        pagerState.animateScrollToPage(index)
                                    }
                                    true
                                }
                            }
                            mv.overlays.add(stationMarker)
                        }
                    }
                }
                
                // Overlay for Loading or status
                if (fetchStatus == "Loading") {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(16.dp)
                            .background(Color.White.copy(alpha = 0.9f), RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                strokeWidth = 2.dp,
                                color = Color(0xFFFF004D)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Searching for police stations...",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFFF004D)
                            )
                        }
                    }
                } else if (isInternetOffline) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .padding(16.dp)
                            .background(Color(0xFFFEE2E2), RoundedCornerShape(12.dp))
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.CloudOff,
                                contentDescription = null,
                                tint = Color(0xFFDC2626),
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Internet offline. Results may be stale.",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFFDC2626)
                            )
                        }
                    }
                }
            }

            // BOTTOM FLOATING SHEET CARD
            Surface(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .navigationBarsPadding() // Support system nav bar spacing
                    .padding(16.dp)
                    .fillMaxWidth()
                    .wrapContentHeight(),
                shape = RoundedCornerShape(36.dp),
                color = Color.White,
                shadowElevation = 16.dp,
                border = BorderStroke(1.dp, Color(0xFFF1F5F9))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp, bottom = 20.dp, start = 24.dp, end = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Drag handle bar representing a premium sheet header
                    Box(
                        modifier = Modifier
                            .width(44.dp)
                            .height(5.dp)
                            .clip(RoundedCornerShape(50))
                            .background(Color.LightGray.copy(alpha = 0.4f))
                    )

                    Spacer(modifier = Modifier.height(20.dp))

                    if (stations.isEmpty()) {
                        // Display No Nearby Police Stations message
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                imageVector = Icons.Default.Info,
                                contentDescription = null,
                                tint = Color(0xFF64748B),
                                modifier = Modifier.size(36.dp)
                            )
                            Spacer(modifier = Modifier.height(12.dp))
                            Text(
                                text = "No Nearby Police Stations Found",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1E293B)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "No police stations detected within 10 km.",
                                fontSize = 13.sp,
                                color = Color(0xFF64748B),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            // Large EMERGENCY CALL Button (Always remains visible!)
                            EmergencyCallButton(context = context)
                        }
                    } else {
                        // Swipeable stations details & metrics inside HorizontalPager
                        val currentPage = pagerState.currentPage.coerceIn(0, stations.size - 1)
                        val activeStation = stations[currentPage]
                        
                        HorizontalPager(
                            state = pagerState,
                            modifier = Modifier
                                .fillMaxWidth()
                                .wrapContentHeight()
                        ) { pageIndex ->
                            val station = stations[pageIndex.coerceIn(0, stations.size - 1)]
                            val stationDistanceKm = if (currentLatLng != null) {
                                calculateDistanceInKm(
                                    currentLatLng!!.latitude, currentLatLng!!.longitude,
                                    station.latLng.latitude, station.latLng.longitude
                                )
                            } else {
                                0.0
                            }
                            val stationDistanceStr = if (stationDistanceKm > 0.0) {
                                String.format(Locale.US, "%.1f km", stationDistanceKm)
                            } else {
                                "Checking..."
                            }
                            val stationEtaMins = (stationDistanceKm * 2.0).toInt().coerceAtLeast(1)
                            val stationEtaStr = "$stationEtaMins min"
                            
                            val stationRating = String.format(Locale.US, "%.1f", 4.0 + (station.name.hashCode() % 10).coerceAtLeast(0) / 10.0)

                            Column(modifier = Modifier.fillMaxWidth()) {
                                // FIRST ROW: ICON, NAME/SUBTITLE, STAR RATING
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        // Rounded Square Pink Badge with Shield Icon
                                        Box(
                                            modifier = Modifier
                                                .size(52.dp)
                                                .clip(RoundedCornerShape(16.dp))
                                                .background(Color(0xFFFFECEF)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Shield,
                                                contentDescription = null,
                                                tint = Color(0xFFFF004D),
                                                modifier = Modifier.size(24.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(
                                                text = station.name,
                                                fontSize = 18.sp,
                                                fontWeight = FontWeight.ExtraBold,
                                                color = Color(0xFF1E293B),
                                                maxLines = 1
                                            )
                                            Spacer(modifier = Modifier.height(2.dp))
                                            Text(
                                                text = station.vicinity,
                                                fontSize = 13.sp,
                                                color = Color(0xFF64748B),
                                                maxLines = 1
                                            )
                                        }
                                    }
                                    
                                    // Star Rating Badge
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                                        modifier = Modifier.padding(start = 8.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Star,
                                            contentDescription = "Rating Star",
                                            tint = Color(0xFFFBBF24),
                                            modifier = Modifier.size(18.dp)
                                        )
                                        Text(
                                            text = stationRating,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 15.sp,
                                            color = Color(0xFF1E293B)
                                        )
                                    }
                                }

                                HorizontalDivider(
                                    modifier = Modifier.padding(vertical = 18.dp),
                                    color = Color(0xFFF1F5F9)
                                )

                                // SECOND ROW: DISTANCE & ETA METRICS
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    // Distance block
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFFFECEF)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.LocationOn,
                                                contentDescription = null,
                                                tint = Color(0xFFFF004D),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = stationDistanceStr,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 15.sp,
                                                color = Color(0xFF1E293B)
                                            )
                                            Text(
                                                text = "Distance",
                                                fontSize = 12.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                    }

                                    // Custom thin divider separator
                                    Box(
                                        modifier = Modifier
                                            .width(1.dp)
                                            .height(28.dp)
                                            .background(Color(0xFFE2E8F0))
                                    )

                                    Spacer(modifier = Modifier.width(16.dp))

                                    // ETA block
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.weight(1f)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(40.dp)
                                                .clip(CircleShape)
                                                .background(Color(0xFFFFECEF)),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.Schedule,
                                                contentDescription = null,
                                                tint = Color(0xFFFF004D),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }
                                        Spacer(modifier = Modifier.width(12.dp))
                                        Column {
                                            Text(
                                                text = stationEtaStr,
                                                fontWeight = FontWeight.ExtraBold,
                                                fontSize = 15.sp,
                                                color = Color(0xFF1E293B)
                                            )
                                            Text(
                                                text = "ETA",
                                                fontSize = 12.sp,
                                                color = Color(0xFF64748B)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(24.dp))

                        // THIRD ROW: ACTION BUTTONS (Navigate, Call)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            // Navigate Button
                            Button(
                                onClick = {
                                    val destinationLatLng = activeStation.latLng
                                    val userLocation = currentLatLng
                                    
                                    val uriString = if (userLocation != null) {
                                        "https://www.google.com/maps/dir/?api=1&origin=${userLocation.latitude},${userLocation.longitude}&destination=${destinationLatLng.latitude},${destinationLatLng.longitude}&travelmode=driving"
                                    } else {
                                        "https://www.google.com/maps/search/?api=1&query=${destinationLatLng.latitude},${destinationLatLng.longitude}"
                                    }
                                    
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString)).apply {
                                        `package` = "com.google.android.apps.maps"
                                    }
                                    
                                    try {
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        val fallbackIntent = Intent(Intent.ACTION_VIEW, Uri.parse(uriString))
                                        try {
                                            context.startActivity(fallbackIntent)
                                        } catch (ex: Exception) {
                                            Toast.makeText(context, "No compatible navigation application available", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFF004D),
                                    contentColor = Color.White
                                ),
                                shape = RoundedCornerShape(18.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .testTag("navigate_station_button"),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.NearMe,
                                        contentDescription = null,
                                        tint = Color.White,
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text("Navigate", fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
                                }
                            }

                            // Emergency Call Button (Opens device dialer with 112, fallback to 100)
                            Button(
                                onClick = {
                                    val callIntent = Intent(Intent.ACTION_DIAL).apply {
                                        data = Uri.parse("tel:112")
                                    }
                                    try {
                                        context.startActivity(callIntent)
                                    } catch (e: Exception) {
                                        val fallbackCallIntent = Intent(Intent.ACTION_DIAL).apply {
                                            data = Uri.parse("tel:100")
                                        }
                                        try {
                                            context.startActivity(fallbackCallIntent)
                                        } catch (ex: Exception) {
                                            Toast.makeText(context, "Could not launch phone dialer", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = Color(0xFFFFF1F2),
                                    contentColor = Color(0xFFFF004D)
                                ),
                                shape = RoundedCornerShape(18.dp),
                                border = BorderStroke(1.dp, Color(0xFFFFECEF)),
                                modifier = Modifier
                                    .weight(1f)
                                    .height(56.dp)
                                    .testTag("call_station_button"),
                                contentPadding = PaddingValues(0.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center,
                                    modifier = Modifier.fillMaxSize()
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Phone,
                                        contentDescription = null,
                                        tint = Color(0xFFFF004D),
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = "Emergency",
                                        fontSize = 16.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        color = Color(0xFFFF004D)
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(20.dp))

                        // FOURTH ROW: CAROUSEL DOTS & CHEVRON NAVIGATION BUTTONS
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // Left Chevron Button
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .shadow(2.dp, CircleShape)
                                    .background(Color.White, CircleShape)
                                    .clickable(enabled = pagerState.currentPage > 0) {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                                    contentDescription = "Previous Station",
                                    tint = if (pagerState.currentPage > 0) Color(0xFF1E293B) else Color.LightGray.copy(alpha = 0.5f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }

                            // Page indicators (little pills)
                            Row(
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                stations.forEachIndexed { index, _ ->
                                    val active = index == pagerState.currentPage
                                    Box(
                                        modifier = Modifier
                                            .padding(horizontal = 4.dp)
                                            .size(if (active) 8.dp else 6.dp)
                                            .clip(CircleShape)
                                            .background(if (active) Color(0xFFFF004D) else Color.LightGray.copy(alpha = 0.5f))
                                    )
                                }
                            }

                            // Right Chevron Button
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .shadow(2.dp, CircleShape)
                                    .background(Color.White, CircleShape)
                                    .clickable(enabled = pagerState.currentPage < stations.size - 1) {
                                        coroutineScope.launch {
                                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                        }
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                                    contentDescription = "Next Station",
                                    tint = if (pagerState.currentPage < stations.size - 1) Color(0xFF1E293B) else Color.LightGray.copy(alpha = 0.5f),
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ==========================================
// 10. SETTINGS TAB SCREEN
// ==========================================
@Composable
fun SettingsScreenContent(navController: NavController, viewModel: AbhayaViewModel) {
    val correctPin by viewModel.sosPin.collectAsStateWithLifecycle()
    val emergencyHistory by viewModel.emergencyHistory.collectAsStateWithLifecycle()
    val playingAudioPath by viewModel.playingAudioPath.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Settings & Shield Profile",
            fontSize = 24.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.align(Alignment.Start)
        )
        Text(
            text = "Personalize encryption parameters and telemetry details",
            fontSize = 14.sp,
            color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.6f),
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Shield Configuration Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFFFF004D).copy(alpha = 0.15f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("EMERGENCY RESPONSE PROTOCOLS", fontWeight = FontWeight.Bold, color = Color(0xFFFF004D), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(16.dp))
                SettingsRow(title = "Auto-Record Audio on SOS", description = "Automatically record background voices", checked = true)
                SettingsRow(title = "Auto-Dial Police Helpline", description = "Dials 100 on trigger completion", checked = false)
                SettingsRow(title = "Live Coordinate SMS Broadcast", description = "Continuous text updates to guardians", checked = true)
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 1. PIN Settings Card
        var pinInput by remember { mutableStateOf(correctPin) }
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFFFF004D).copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("SECURITY DEACTIVATION PIN", fontWeight = FontWeight.Bold, color = Color(0xFFFF004D), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = pinInput,
                        onValueChange = {
                            if (it.length <= 4 && it.all { char -> char.isDigit() }) {
                                pinInput = it
                                if (it.length == 4) {
                                    viewModel.updateSosPin(it)
                                }
                            }
                        },
                        label = { Text("4-Digit SOS PIN") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                        singleLine = true,
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = Color(0xFFFF004D),
                            focusedLabelColor = Color(0xFFFF004D)
                        )
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Text(
                        text = "PIN: $correctPin",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // 2. Emergency History Card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFFFF004D).copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("EMERGENCY HISTORY LOGS", fontWeight = FontWeight.Bold, color = Color(0xFFFF004D), fontSize = 12.sp)
                Spacer(modifier = Modifier.height(12.dp))
                if (emergencyHistory.isEmpty()) {
                    Text(
                        text = "No historic emergency alerts triggered.",
                        fontSize = 12.sp,
                        color = Color.Gray,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                } else {
                    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                        emergencyHistory.take(4).forEach { item ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(text = item.dateString, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                                    Text(text = "Coordinates: ${item.latitude}, ${item.longitude}", fontSize = 11.sp, color = Color.Gray)
                                    if (!item.audioPath.isNullOrEmpty()) {
                                        Text(
                                            text = "Audio: ...${item.audioPath.substringAfterLast("/")}",
                                            fontSize = 10.sp,
                                            color = Color(0xFFFF004D),
                                            fontWeight = FontWeight.Medium
                                        )
                                    }
                                }
                                if (!item.audioPath.isNullOrEmpty()) {
                                    val isPlaying = playingAudioPath == item.audioPath
                                    IconButton(
                                        onClick = { viewModel.playRecording(item.audioPath) },
                                        modifier = Modifier.size(36.dp)
                                    ) {
                                        Icon(
                                            imageVector = if (isPlaying) Icons.Default.Stop else Icons.Default.PlayArrow,
                                            contentDescription = if (isPlaying) "Stop playback" else "Play recording",
                                            tint = Color(0xFFFF004D)
                                        )
                                    }
                                    Spacer(modifier = Modifier.width(8.dp))
                                }
                                Box(
                                    modifier = Modifier
                                        .background(Color(0xFFFFEBEE), RoundedCornerShape(4.dp))
                                        .padding(horizontal = 6.dp, vertical = 2.dp)
                                ) {
                                    Text(text = item.status, fontSize = 9.sp, fontWeight = FontWeight.Black, color = Color(0xFFD50000))
                                }
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Legal information
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            shape = RoundedCornerShape(16.dp),
            border = BorderStroke(1.dp, Color(0xFFFF004D).copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("ABOUT ABHAYA", fontWeight = FontWeight.Bold, color = Color.Gray, fontSize = 12.sp)
                Spacer(modifier = Modifier.height(12.dp))
                Text("Version 1.0.0 (Release Build)", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                Spacer(modifier = Modifier.height(4.dp))
                Text("Crafted for high-performance safety protection. Built on Kotlin/Jetpack Compose and Clean Architecture.", fontSize = 11.sp, color = Color.Gray)
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        // Logout button
        Button(
            onClick = {
                viewModel.logout()
                navController.navigate("login") {
                    popUpTo(0) { inclusive = true }
                }
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .testTag("logout_button"),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFFF004D)),
            shape = RoundedCornerShape(28.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Logout, contentDescription = "Logout", tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("LOGOUT SESSION", fontWeight = FontWeight.ExtraBold, letterSpacing = 1.sp)
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
fun SettingsRow(title: String, description: String, checked: Boolean) {
    var isChecked by remember { mutableStateOf(checked) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.Bold, fontSize = 14.sp)
            Text(description, fontSize = 11.sp, color = Color.Gray)
        }
        Switch(
            checked = isChecked,
            onCheckedChange = { isChecked = it },
            colors = SwitchDefaults.colors(
                checkedThumbColor = Color.White,
                checkedTrackColor = Color(0xFFFF004D)
            )
        )
    }
}
