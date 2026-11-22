package com.example.ui.screens

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Fingerprint
import androidx.compose.material.icons.filled.Groups
import androidx.compose.material.icons.filled.Login
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.credentials.CredentialManager
import com.example.R
import com.example.auth.attemptAutoSignIn
import com.example.auth.onGoogleSignInClicked
import com.example.ui.components.CyberPanel
import com.example.ui.components.NeonStatusBadge
import com.example.ui.theme.ChakraPetchFontFamily
import com.example.ui.theme.CrimsonLive
import com.example.ui.theme.CyanAccent
import com.example.ui.theme.CyberBorder
import com.example.ui.theme.CyberCard
import com.example.ui.theme.GoldTrophy
import com.example.ui.theme.NeonGreen
import com.example.ui.theme.PitchBlack
import com.example.ui.theme.RussoOneFontFamily
import com.example.ui.theme.TextPrimary
import com.example.ui.theme.TextSecondary

@Composable
fun AuthScreen(
    onAuthSuccess: () -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val credentialManager = remember(context) { CredentialManager.create(context) }

    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }

    LaunchedEffect(Unit) {
        attemptAutoSignIn(
            context = context,
            credentialManager = credentialManager,
            onAuthSuccess = onAuthSuccess,
            onUnauthenticated = {},
            scope = scope
        )
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PitchBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 20.dp, vertical = 16.dp)
                .widthIn(max = 540.dp)
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(210.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .border(1.5.dp, NeonGreen.copy(alpha = 0.6f), RoundedCornerShape(20.dp))
            ) {
                Image(
                    painter = painterResource(id = R.drawable.img_gamerzone_banner),
                    contentDescription = stringResource(id = R.string.cd_banner_image),
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, PitchBlack.copy(alpha = 0.92f))
                            )
                        )
                )
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    NeonStatusBadge(text = "PAKISTAN ESPORTS ARENA", color = NeonGreen)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = stringResource(R.string.brand_title),
                        fontFamily = RussoOneFontFamily,
                        fontSize = 28.sp,
                        letterSpacing = 1.5.sp,
                        color = TextPrimary
                    )
                }
            }

            Spacer(modifier = Modifier.height(22.dp))

            Text(
                text = "COMPETE. DOMINATE. WIN PKR.",
                fontFamily = ChakraPetchFontFamily,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                letterSpacing = 2.sp,
                color = NeonGreen,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(8.dp))

            Text(
                text = "Join verified Free Fire Solo, Duo, Squad & Clash Squad tournaments with real-time room credentials and live leaderboards.",
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(20.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                AuthHighlightTile(
                    icon = Icons.Filled.EmojiEvents,
                    title = "Daily PKR",
                    subtitle = "Instant Prize Pools",
                    accent = GoldTrophy,
                    modifier = Modifier.weight(1f)
                )
                AuthHighlightTile(
                    icon = Icons.Filled.Bolt,
                    title = "Live Rooms",
                    subtitle = "Real-time Sync",
                    accent = NeonGreen,
                    modifier = Modifier.weight(1f)
                )
                AuthHighlightTile(
                    icon = Icons.Filled.Security,
                    title = "Anti-Cheat",
                    subtitle = "Verified UIDs",
                    accent = CyanAccent,
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (errorMessage != null) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = CrimsonLive.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, CrimsonLive.copy(alpha = 0.6f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 12.dp)
                ) {
                    Text(
                        text = errorMessage ?: "",
                        style = MaterialTheme.typography.bodyMedium,
                        color = CrimsonLive,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            Button(
                onClick = {
                    errorMessage = null
                    isLoading = true
                    onGoogleSignInClicked(
                        context = context,
                        credentialManager = credentialManager,
                        onAuthSuccess = {
                            isLoading = false
                            onAuthSuccess()
                        },
                        onAuthError = { msg ->
                            isLoading = false
                            errorMessage = msg
                        },
                        scope = scope,
                        onAuthCancelled = { isLoading = false }
                    )
                },
                enabled = !isLoading,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonGreen,
                    contentColor = PitchBlack
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .testTag("google_sign_in_button")
            ) {
                if (isLoading) {
                    CircularProgressIndicator(
                        color = PitchBlack,
                        strokeWidth = 2.5.dp,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Icon(imageVector = Icons.Filled.Login, contentDescription = null)
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = stringResource(R.string.sign_in_google).uppercase(),
                        fontFamily = RussoOneFontFamily,
                        fontSize = 15.sp,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Secured by Firebase Authentication & Google Sign-In",
                style = MaterialTheme.typography.bodyMedium,
                color = TextSecondary
            )
        }
    }
}

@Composable
private fun AuthHighlightTile(
    icon: ImageVector,
    title: String,
    subtitle: String,
    accent: Color,
    modifier: Modifier = Modifier
) {
    CyberPanel(
        borderColor = accent.copy(alpha = 0.4f),
        modifier = modifier
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(22.dp)
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = title,
            style = MaterialTheme.typography.titleMedium,
            color = TextPrimary,
            maxLines = 1
        )
        Text(
            text = subtitle,
            style = MaterialTheme.typography.bodyMedium,
            fontSize = 11.sp,
            color = TextSecondary,
            maxLines = 1
        )
    }
}

@Composable
fun CompleteProfileScreen(
    initialName: String,
    isBusy: Boolean,
    onSubmitProfile: (inGameName: String, freeFireUid: String, teamName: String, whatsapp: String) -> Unit,
    onSignOut: () -> Unit
) {
    var ign by remember { mutableStateOf(initialName.take(24)) }
    var ffUid by remember { mutableStateOf("") }
    var teamName by remember { mutableStateOf("") }
    var whatsapp by remember { mutableStateOf("") }
    var localError by remember { mutableStateOf<String?>(null) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(PitchBlack)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(20.dp)
                .widthIn(max = 520.dp)
                .align(Alignment.TopCenter),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            NeonStatusBadge(text = "STEP 2 OF 2 • PLAYER SIGN UP", color = NeonGreen)
            Text(
                text = stringResource(R.string.complete_profile_title),
                style = MaterialTheme.typography.displayMedium,
                color = TextPrimary
            )
            Text(
                text = stringResource(R.string.complete_profile_subtitle),
                style = MaterialTheme.typography.bodyLarge,
                color = TextSecondary
            )

            CyberPanel(borderColor = NeonGreen.copy(alpha = 0.45f)) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    ProfileInputField(
                        value = ign,
                        onValueChange = { ign = it },
                        label = "Free Fire In-Game Name (IGN) *",
                        icon = Icons.Filled.Person,
                        testTag = "signup_ign_input"
                    )
                    ProfileInputField(
                        value = ffUid,
                        onValueChange = { ffUid = it.filter { ch -> ch.isDigit() } },
                        label = "Free Fire UID (Numeric) *",
                        icon = Icons.Filled.Fingerprint,
                        keyboardType = KeyboardType.Number,
                        testTag = "signup_ffuid_input"
                    )
                    ProfileInputField(
                        value = teamName,
                        onValueChange = { teamName = it },
                        label = "Squad / Guild Name *",
                        icon = Icons.Filled.Groups,
                        testTag = "signup_team_input"
                    )
                    ProfileInputField(
                        value = whatsapp,
                        onValueChange = { whatsapp = it },
                        label = "WhatsApp Number (Optional)",
                        icon = Icons.Filled.Phone,
                        keyboardType = KeyboardType.Phone,
                        testTag = "signup_whatsapp_input"
                    )
                }
            }

            if (localError != null) {
                Text(
                    text = localError ?: "",
                    style = MaterialTheme.typography.bodyMedium,
                    color = CrimsonLive
                )
            }

            Button(
                onClick = {
                    val cleanIgn = ign.trim()
                    val cleanUid = ffUid.trim()
                    val cleanTeam = teamName.trim()
                    when {
                        cleanIgn.length < 2 -> localError = "Enter an In-Game Name (at least 2 characters)."
                        cleanUid.length < 4 -> localError = "Enter a valid Free Fire UID (4-20 digits)."
                        cleanTeam.isEmpty() -> localError = "Enter your Squad or Clan name."
                        else -> {
                            localError = null
                            onSubmitProfile(cleanIgn, cleanUid, cleanTeam, whatsapp.trim())
                        }
                    }
                },
                enabled = !isBusy,
                colors = ButtonDefaults.buttonColors(
                    containerColor = NeonGreen,
                    contentColor = PitchBlack
                ),
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
                    .testTag("complete_signup_button")
            ) {
                if (isBusy) {
                    CircularProgressIndicator(
                        color = PitchBlack,
                        strokeWidth = 2.dp,
                        modifier = Modifier.size(22.dp)
                    )
                } else {
                    Text(
                        text = "ENTER GAMERZONE PK",
                        fontFamily = RussoOneFontFamily,
                        fontSize = 15.sp
                    )
                }
            }

            Surface(
                onClick = onSignOut,
                color = Color.Transparent,
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .height(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center, modifier = Modifier.padding(horizontal = 16.dp)) {
                    Text(
                        text = "Use a different Google account",
                        style = MaterialTheme.typography.labelLarge,
                        color = TextSecondary
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileInputField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    icon: ImageVector,
    testTag: String,
    keyboardType: KeyboardType = KeyboardType.Text
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        leadingIcon = { Icon(imageVector = icon, contentDescription = null, tint = NeonGreen) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        colors = OutlinedTextFieldDefaults.colors(
            focusedBorderColor = NeonGreen,
            unfocusedBorderColor = CyberBorder,
            focusedContainerColor = CyberCard,
            unfocusedContainerColor = CyberCard,
            focusedLabelColor = NeonGreen
        ),
        modifier = Modifier
            .fillMaxWidth()
            .testTag(testTag)
    )
}
