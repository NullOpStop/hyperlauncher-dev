package net.kdt.pojavlaunch.ui.screens

import android.annotation.SuppressLint
import android.net.Uri
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.BaseActivity
import net.kdt.pojavlaunch.authenticator.accounts.Accounts
import net.kdt.pojavlaunch.authenticator.AuthType
import net.kdt.pojavlaunch.authenticator.accounts.MinecraftAccount
import net.kdt.pojavlaunch.extra.ExtraConstants
import net.kdt.pojavlaunch.extra.ExtraCore
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.skin.AndroidSkinAnalyzer
import net.kdt.pojavlaunch.skin.SkinModelType
import net.kdt.pojavlaunch.skin.SkinUtils
import net.kdt.pojavlaunch.ui.theme.PojavTheme
import java.io.File
import java.io.FileOutputStream
import java.net.URLEncoder
import java.util.UUID

@Composable
fun AccountManagerOverlay(
    accounts: List<MinecraftAccount>,
    currentAccount: MinecraftAccount?,
    onAccountSelect: (MinecraftAccount) -> Unit,
    onAccountDelete: (MinecraftAccount) -> Unit,
    onAccountUpdate: () -> Unit,
    onMicrosoftClick: () -> Unit,
    onLocalClick: (String, String?, String?, SkinModelType) -> Unit,
    onElyByClick: () -> Unit,
    onBack: () -> Unit,
    startWithAddDialog: Boolean = false
) {
    var showAuthMethodDialog by rememberSaveable { mutableStateOf(startWithAddDialog) }
    var showLocalLoginDialog by rememberSaveable { mutableStateOf(false) }
    var accountToDelete by remember { mutableStateOf<MinecraftAccount?>(null) }
    var wardrobeAccount by remember { mutableStateOf<MinecraftAccount?>(null) }

    // Reset the dialog state if startWithAddDialog changes (e.g. clicking "Add" while already in manager)
    LaunchedEffect(startWithAddDialog) {
        if (startWithAddDialog) showAuthMethodDialog = true
    }

    BackHandler { 
        if (showLocalLoginDialog) {
            showLocalLoginDialog = false
            showAuthMethodDialog = true
        } else if (showAuthMethodDialog) {
            showAuthMethodDialog = false
        } else {
            onBack()
        }
    }

    val context = LocalContext.current
    val backgroundPath = LauncherPreferences.PREF_BACKGROUND_PATH_STATE.value
    val backgroundVideoPath = LauncherPreferences.PREF_BACKGROUND_VIDEO_PATH_STATE.value
    val hasBackground = backgroundPath != null || backgroundVideoPath != null || LocalInspectionMode.current

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = if (hasBackground) Color.Transparent else MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        if (accountToDelete != null) {
            AlertDialog(
                onDismissRequest = { accountToDelete = null },
                confirmButton = {
                    Button(
                        onClick = {
                            accountToDelete?.let { onAccountDelete(it) }
                            accountToDelete = null
                        },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text(stringResource(id = R.string.global_delete))
                    }
                },
                dismissButton = {
                    TextButton(onClick = { accountToDelete = null }) {
                        Text(stringResource(id = android.R.string.cancel))
                    }
                },
                title = { Text(stringResource(id = R.string.global_error)) },
                text = { Text(stringResource(id = R.string.warning_remove_account)) }
            )
        }

        if (wardrobeAccount != null) {
            WardrobeDialog(
                account = wardrobeAccount!!,
                onDismiss = { wardrobeAccount = null },
                onConfirm = { skin, cape, model ->
                    wardrobeAccount?.let {
                        it.skinPath = skin
                        it.capePath = cape
                        it.skinModel = model
                        it.save()
                        it.updateSkinFace(context.assets)
                        onAccountUpdate()
                    }
                    wardrobeAccount = null
                }
            )
        }

        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(horizontal = 24.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp)
            ) {
                // Left column: Player Model + Add button
                Column(
                    modifier = Modifier
                        .weight(0.42f)
                        .fillMaxHeight(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(20.dp))
                            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f)), RoundedCornerShape(20.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        if (currentAccount != null) {
                            val skinUrl = remember(currentAccount) { SkinUtils.getSkinUrl(currentAccount) }
                            val model = remember(currentAccount) { SkinUtils.getModelType(currentAccount) }

                            Skin3DViewer(
                                modifier = Modifier.fillMaxSize(),
                                skinUrl = skinUrl,
                                model = model
                            )
                        } else {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_px_image_renderer),
                                    contentDescription = null,
                                    modifier = Modifier.size(40.dp),
                                    tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f)
                                )
                                Spacer(Modifier.height(8.dp))
                                @Suppress("HardcodedText")
                                Text(
                                    "Select an account", 
                                    style = MaterialTheme.typography.bodySmall, 
                                    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Button(
                        onClick = { showAuthMethodDialog = true },
                        modifier = Modifier.fillMaxWidth().height(48.dp),
                        shape = CircleShape,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                    ) {
                        Icon(painterResource(id = R.drawable.ic_add), null, Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        @Suppress("HardcodedText")
                        Text("Add Account", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold)
                    }
                }

                // Right column: Accounts List
                Column(
                    modifier = Modifier
                        .weight(0.58f)
                        .fillMaxHeight()
                ) {
                    Box(modifier = Modifier.fillMaxWidth().padding(bottom = 8.dp, start = 4.dp)) {
                        @Suppress("HardcodedText")
                        Text(
                            "Accounts",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }

                    if (accounts.isEmpty()) {
                        Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            @Suppress("HardcodedText")
                            Text("No accounts added", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(bottom = 12.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(accounts, key = { it.mSaveLocation?.name ?: it.username }) { account ->
                                AccountItemZalith(
                                    account = account,
                                    isSelected = account.mSaveLocation?.name == currentAccount?.mSaveLocation?.name,
                                    onSelect = { onAccountSelect(account) },
                                    onDelete = { accountToDelete = account },
                                    onEditWardrobe = {
                                        if (account.authType == AuthType.LOCAL) {
                                            wardrobeAccount = account
                                        } else {
                                            Toast.makeText(context, "Wardrobe only for local accounts", Toast.LENGTH_SHORT).show()
                                        }
                                    }
                                )
                            }
                        }
                    }
                }
            }

            if (showAuthMethodDialog) {
                AuthMethodSelectionDialog(
                    onDismiss = { showAuthMethodDialog = false },
                    onMicrosoftClick = {
                        showAuthMethodDialog = false
                        onMicrosoftClick()
                    },
                    onElyByClick = {
                        showAuthMethodDialog = false
                        onElyByClick()
                    },
                    onLocalClick = {
                        showAuthMethodDialog = false
                        showLocalLoginDialog = true
                    }
                )
            }

            if (showLocalLoginDialog) {
                LocalLoginDialog(
                    onDismiss = { showLocalLoginDialog = false },
                    onConfirm = { u, s, c, m ->
                        showLocalLoginDialog = false
                        onLocalClick(u, s, c, m)
                    }
                )
            }
        }
    }
}

@Composable
fun SelectAuthScreen(
    onMicrosoftClick: () -> Unit,
    onLocalClick: (String, String?, String?, SkinModelType) -> Unit,
    onElyByClick: () -> Unit
) {
    // Legacy support for Fragment usage, wraps the new unified experience but filtered
    val isPreview = LocalInspectionMode.current
    var accounts by remember { mutableStateOf(if (isPreview) emptyList() else Accounts.load().accounts.filterNotNull()) }
    var currentAccount by remember { mutableStateOf(if (isPreview) null else Accounts.getCurrent()) }

    val backgroundPath = LauncherPreferences.PREF_BACKGROUND_PATH_STATE.value
    val backgroundVideoPath = LauncherPreferences.PREF_BACKGROUND_VIDEO_PATH_STATE.value
    val hasBackground = backgroundPath != null || backgroundVideoPath != null || isPreview

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = if (hasBackground) Color.Transparent else MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        AccountManagerOverlay(
            accounts = accounts,
            currentAccount = currentAccount,
            onAccountSelect = { 
                Accounts.setCurrent(it)
                currentAccount = it
            },
            onAccountDelete = { 
                Accounts.delete(it)
                accounts = Accounts.load().accounts.filterNotNull()
                if (currentAccount?.mSaveLocation?.name == it.mSaveLocation?.name) {
                    currentAccount = Accounts.getCurrent()
                }
            },
            onAccountUpdate = {
                accounts = Accounts.load().accounts.filterNotNull()
                currentAccount = Accounts.getCurrent()
            },
            onMicrosoftClick = onMicrosoftClick,
            onLocalClick = onLocalClick,
            onElyByClick = onElyByClick,
            onBack = {} // Not applicable in fragment usage
        )
    }
}

@Composable
fun LocalLoginScreen(
    onLoginClick: (String, String?, String?, SkinModelType) -> Unit
) {
    // Legacy support for Fragment usage
    val isPreview = LocalInspectionMode.current
    val backgroundPath = LauncherPreferences.PREF_BACKGROUND_PATH_STATE.value
    val backgroundVideoPath = LauncherPreferences.PREF_BACKGROUND_VIDEO_PATH_STATE.value
    val hasBackground = backgroundPath != null || backgroundVideoPath != null || isPreview

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = if (hasBackground) Color.Transparent else MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        LocalLoginDialog(
            onDismiss = {},
            onConfirm = onLoginClick
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AuthMethodSelectionDialog(
    onDismiss: () -> Unit,
    onMicrosoftClick: () -> Unit,
    onElyByClick: () -> Unit,
    onLocalClick: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Select Login Method", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                AuthActionButton(
                    text = stringResource(id = R.string.auth_select_microsoft),
                    icon = R.drawable.ic_auth_ms,
                    onClick = onMicrosoftClick,
                    tint = Color.Unspecified
                )
                AuthActionButton(
                    text = stringResource(id = R.string.auth_select_elyby),
                    icon = R.drawable.ic_auth_elyby,
                    onClick = onElyByClick,
                    tint = Color.Unspecified
                )
                AuthActionButton(
                    text = stringResource(id = R.string.auth_select_local),
                    icon = R.drawable.ic_px_home,
                    onClick = onLocalClick,
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = android.R.string.cancel))
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun LocalLoginDialog(
    onDismiss: () -> Unit,
    onConfirm: (String, String?, String?, SkinModelType) -> Unit
) {
    var username by remember { mutableStateOf("") }
    var selectedSkinPath by remember { mutableStateOf<String?>(null) }
    var selectedCapePath by remember { mutableStateOf<String?>(null) }
    var selectedSkinModel by remember { mutableStateOf(SkinModelType.STEVE) }

    val context = LocalContext.current
    val skinLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val file = copyUriToInternal(context, it, "skin")
            if (file != null) {
                val bytes = file.readBytes()
                if (AndroidSkinAnalyzer.validate(bytes)) {
                    selectedSkinPath = file.absolutePath
                    selectedSkinModel = AndroidSkinAnalyzer.detectModel(bytes)
                } else {
                    Toast.makeText(context, "Invalid skin dimensions!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val capeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val file = copyUriToInternal(context, it, "cape")
            selectedCapePath = file?.absolutePath
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(id = R.string.auth_select_local), fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                OutlinedTextField(
                    value = username,
                    onValueChange = { username = it },
                    modifier = Modifier.fillMaxWidth(),
                    label = { Text("Username") },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp)
                )

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { skinLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(if (selectedSkinPath == null) "Pick Skin" else "Skin OK", fontSize = 11.sp)
                    }
                    Button(
                        onClick = { capeLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(if (selectedCapePath == null) "Pick Cape" else "Cape OK", fontSize = 11.sp)
                    }
                }

                SkinModelSelector(
                    selectedModel = selectedSkinModel,
                    onModelSelected = { selectedSkinModel = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (username.isNotBlank()) {
                        onConfirm(username, selectedSkinPath, selectedCapePath, selectedSkinModel)
                    }
                },
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Login", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = android.R.string.cancel))
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun WardrobeDialog(
    account: MinecraftAccount,
    onDismiss: () -> Unit,
    onConfirm: (String?, String?, SkinModelType) -> Unit
) {
    var selectedSkinPath by remember { mutableStateOf(account.skinPath) }
    var selectedCapePath by remember { mutableStateOf(account.capePath) }
    var selectedSkinModel by remember { mutableStateOf(account.skinModel ?: SkinModelType.STEVE) }

    val context = LocalContext.current
    val skinLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val file = copyUriToInternal(context, it, "skin")
            if (file != null) {
                val bytes = file.readBytes()
                if (AndroidSkinAnalyzer.validate(bytes)) {
                    selectedSkinPath = file.absolutePath
                    selectedSkinModel = AndroidSkinAnalyzer.detectModel(bytes)
                } else {
                    Toast.makeText(context, "Invalid skin dimensions!", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    val capeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val file = copyUriToInternal(context, it, "cape")
            selectedCapePath = file?.absolutePath
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edit Wardrobe", fontWeight = FontWeight.Bold) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text("Editing: ${account.username}", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
                
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { skinLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(if (selectedSkinPath == null) "Pick Skin" else "Skin OK", fontSize = 11.sp)
                    }
                    Button(
                        onClick = { capeLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer, contentColor = MaterialTheme.colorScheme.onSecondaryContainer),
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text(if (selectedCapePath == null) "Pick Cape" else "Cape OK", fontSize = 11.sp)
                    }
                }

                SkinModelSelector(
                    selectedModel = selectedSkinModel,
                    onModelSelected = { selectedSkinModel = it },
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    onConfirm(selectedSkinPath, selectedCapePath, selectedSkinModel)
                },
                shape = RoundedCornerShape(16.dp)
            ) {
                Text("Save", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(id = android.R.string.cancel))
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}

private fun copyUriToInternal(context: android.content.Context, uri: Uri, prefix: String): File? {
    return try {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            val fileName = "${prefix}_${UUID.randomUUID()}.png"
            val file = File(context.filesDir, fileName)
            FileOutputStream(file).use { outputStream ->
                inputStream.copyTo(outputStream)
            }
            file
        }
    } catch (e: Exception) {
        null
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun Skin3DViewer(
    modifier: Modifier = Modifier,
    skinUrl: String? = null,
    model: String = "default"
) {
    var isPageLoaded by remember { mutableStateOf(false) }
    
    val transitionAlpha by animateFloatAsState(
        targetValue = if (isPageLoaded) 1f else 0f,
        animationSpec = tween(600),
        label = "skinAlpha"
    )

    val transitionScale by animateFloatAsState(
        targetValue = 1f,
        label = "skinScale"
    )

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        AndroidView(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    alpha = transitionAlpha
                    scaleX = transitionScale
                    scaleY = transitionScale
                },
            factory = { context ->
                WebView(context).apply {
                    settings.javaScriptEnabled = true
                    settings.allowFileAccess = true
                    @Suppress("DEPRECATION")
                    settings.allowFileAccessFromFileURLs = true
                    @Suppress("DEPRECATION")
                    settings.allowUniversalAccessFromFileURLs = true
                    settings.cacheMode = WebSettings.LOAD_DEFAULT
                    settings.domStorageEnabled = true
                    setBackgroundColor(0)

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            isPageLoaded = true
                            val skin = if (!skinUrl.isNullOrEmpty()) skinUrl else "steve.png"
                            view?.evaluateJavascript("loadSkin('$skin', '$model'); startAnim('NewIdle');", null)
                        }
                    }

                    val encodedUrl = try { URLEncoder.encode(skinUrl ?: "", "UTF-8") } catch (_: Exception) { "" }
                    val finalUrl = "file:///android_asset/skinview.html" + (if (encodedUrl.isNotEmpty()) "?skin=$encodedUrl&model=$model" else "")
                    loadUrl(finalUrl)
                }
            },
            update = { webView ->
                if (isPageLoaded) {
                    val skin = if (!skinUrl.isNullOrEmpty()) skinUrl else "steve.png"
                    webView.evaluateJavascript("loadSkin('$skin', '$model');", null)
                }
            },
            onRelease = { webView ->
                webView.stopLoading()
                webView.loadUrl("about:blank")
                webView.clearHistory()
                webView.removeAllViews()
                webView.destroy()
            }
        )

        AnimatedVisibility(
            visible = !isPageLoaded,
            exit = fadeOut(tween(500))
        ) {
            Box(
                modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(modifier = Modifier.size(32.dp), strokeWidth = 2.dp)
            }
        }
    }
}

@Composable
fun AuthActionButton(
    text: String,
    icon: Int,
    onClick: () -> Unit,
    tint: Color = Color.Unspecified
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(min = 40.dp),
        shape = CircleShape,
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.primary,
            contentColor = MaterialTheme.colorScheme.onPrimary
        ),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center
        ) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = tint
            )
            Spacer(modifier = Modifier.width(12.dp))
            Text(
                text = text,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun SkinModelSelector(
    selectedModel: SkinModelType,
    onModelSelected: (SkinModelType) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = listOf("Wide", "Slim")
    SingleChoiceSegmentedButtonRow(
        modifier = modifier
            .fillMaxWidth()
            .height(36.dp)
            .padding(2.dp)
    ) {
        options.forEachIndexed { index, label ->
            val model = if (index == 0) SkinModelType.STEVE else SkinModelType.ALEX
            SegmentedButton(
                shape = SegmentedButtonDefaults.itemShape(index = index, count = options.size),
                onClick = { onModelSelected(model) },
                selected = selectedModel == model,
                label = { Text(label, fontSize = 11.sp, fontWeight = FontWeight.Bold) },
                colors = SegmentedButtonDefaults.colors(
                    activeContainerColor = MaterialTheme.colorScheme.primary,
                    activeContentColor = MaterialTheme.colorScheme.onPrimary,
                    inactiveContainerColor = Color.Transparent,
                    inactiveContentColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                ),
                border = BorderStroke(0.dp, Color.Transparent),
                icon = {}
            )
        }
    }
}

@Composable
private fun AccountItemZalith(
    account: MinecraftAccount,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onDelete: () -> Unit,
    onEditWardrobe: () -> Unit
) {
    val headBitmap by SkinUtils.rememberSkinHead2D(account)

    Surface(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
        border = BorderStroke(1.dp, if (isSelected) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Front Head 2D
            Surface(
                modifier = Modifier.size(40.dp),
                shape = RoundedCornerShape(6.dp),
                color = Color.Transparent
            ) {
                if (headBitmap != null) {
                    Image(
                        bitmap = headBitmap!!.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Fit
                    )
                } else {
                    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Icon(painterResource(id = R.drawable.ic_px_home), null, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f))
                    }
                }
            }

            Spacer(modifier = Modifier.width(10.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = account.username,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val authIcon = account.authType.iconResource
                    if (authIcon != 0) {
                        Icon(
                            painter = painterResource(id = authIcon),
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = Color.Unspecified
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    Text(
                        text = account.authType.name,
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(
                    onClick = onEditWardrobe,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_px_image),
                        contentDescription = "Edit Wardrobe",
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_px_trash),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp),
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f)
                    )
                }
            }
        }
    }
}
