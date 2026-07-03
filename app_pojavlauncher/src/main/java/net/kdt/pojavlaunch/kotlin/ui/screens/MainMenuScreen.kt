package net.kdt.pojavlaunch.ui.screens

import android.annotation.SuppressLint
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.drawable.Drawable
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.*
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.rounded.Send
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.authenticator.AuthType
import net.kdt.pojavlaunch.authenticator.accounts.Accounts
import net.kdt.pojavlaunch.authenticator.accounts.MinecraftAccount
import net.kdt.pojavlaunch.authenticator.accounts.SkinHeadRenderer
import net.kdt.pojavlaunch.extra.ExtraConstants
import net.kdt.pojavlaunch.extra.ExtraCore
import net.kdt.pojavlaunch.extra.ExtraListener
import net.kdt.pojavlaunch.instances.Instance
import net.kdt.pojavlaunch.instances.InstanceIconProvider
import net.kdt.pojavlaunch.instances.Instances
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper
import net.kdt.pojavlaunch.progresskeeper.TaskCountListener
import net.kdt.pojavlaunch.skin.SkinUtils
import net.kdt.pojavlaunch.ui.theme.PojavTheme

@Composable
fun rememberDrawablePainter(drawable: Drawable?): Painter {
    return remember(drawable) {
        if (drawable == null) {
            object : Painter() {
                override val intrinsicSize: Size get() = Size.Unspecified
                override fun DrawScope.onDraw() {}
            }
        } else {
            object : Painter() {
                override val intrinsicSize: Size
                    get() = Size(
                        drawable.intrinsicWidth.toFloat(),
                        drawable.intrinsicHeight.toFloat()
                    )

                override fun DrawScope.onDraw() {
                    drawIntoCanvas { canvas ->
                        drawable.setBounds(0, 0, size.width.toInt(), size.height.toInt())
                        drawable.draw(canvas.nativeCanvas)
                    }
                }
            }
        }
    }
}

@Composable
fun MainMenuRevamp(
    onEditProfileClick: () -> Unit,
    onCustomControlsClick: () -> Unit,
    onInstallJarClick: () -> Unit,
    onShareLogsClick: () -> Unit,
    onOpenFilesClick: () -> Unit,
    onWikiClick: () -> Unit,
    onSocialMediaClick: () -> Unit,
    onPlayClick: () -> Unit,
    onTerminateClick: () -> Unit,
    onInstanceSelect: () -> Unit,
    onAccountManagerClick: () -> Unit
) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current

    val hasBackground = LauncherPreferences.PREF_BACKGROUND_PATH_STATE.value != null || 
                        LauncherPreferences.PREF_BACKGROUND_VIDEO_PATH_STATE.value != null || isPreview
    val backgroundTransparency = LauncherPreferences.PREF_BACKGROUND_TRANSPARENCY_STATE.value
    val hideActionButtons = LauncherPreferences.PREF_HIDE_MAIN_ACTION_BUTTONS_STATE.value

    var selectedInstance by remember {
        mutableStateOf<Instance?>(
            if (isPreview) null
            else try { Instances.loadSelectedInstance() } catch (e: Exception) { null }
        )
    }

    SideEffect {
        if (!isPreview) {
            val instance = try { Instances.loadSelectedInstance() } catch (e: Exception) { null }
            if (selectedInstance != instance) {
                selectedInstance = instance
            }
        }
    }

    var currentAccount by remember {
        mutableStateOf<MinecraftAccount?>(if (isPreview) null else Accounts.getCurrent())
    }

    var taskCount by remember { mutableIntStateOf(if (isPreview) 0 else ProgressKeeper.getTaskCount()) }
    var isLaunching by remember { mutableStateOf(false) }

    val isSomethingRunning by remember(taskCount, isLaunching) {
        derivedStateOf { taskCount > 0 || isLaunching }
    }

    DisposableEffect(Unit) {
        if (isPreview) return@DisposableEffect onDispose {}

        val accountListener = ExtraListener<Boolean> { _, _ ->
            val newAccount = Accounts.getCurrent()
            // Check if the account has actually changed to avoid unnecessary head refreshes
            if (newAccount?.profileId != currentAccount?.profileId || 
                newAccount?.skinPath != currentAccount?.skinPath ||
                newAccount?.username != currentAccount?.username) {
                currentAccount = newAccount
            }
            false
        }
        val launchListener = ExtraListener<Boolean> { _, value ->
            isLaunching = value
            false
        }
        val taskListener = TaskCountListener { count ->
            taskCount = count
            false
        }

        ExtraCore.addExtraListener(ExtraConstants.REFRESH_ACCOUNT_SPINNER, accountListener)
        ExtraCore.addExtraListener(ExtraConstants.LAUNCH_GAME, launchListener)
        ProgressKeeper.addTaskCountListener(taskListener)

        onDispose {
            ExtraCore.removeExtraListenerFromValue(ExtraConstants.REFRESH_ACCOUNT_SPINNER, accountListener)
            ExtraCore.removeExtraListenerFromValue(ExtraConstants.LAUNCH_GAME, launchListener)
            ProgressKeeper.removeTaskCountListener(taskListener)
        }
    }

    val skinHead by SkinUtils.rememberSkinHead2D(currentAccount)

    val instanceIcon = remember(selectedInstance) {
        if (!isPreview && selectedInstance != null)
            InstanceIconProvider.fetchIcon(context.resources, selectedInstance!!)
        else null
    }

    val headInteractionSource = remember { MutableInteractionSource() }
    val isHeadPressed by headInteractionSource.collectIsPressedAsState()
    val headScale by animateFloatAsState(
        targetValue = if (isHeadPressed) 0.92f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "headScale"
    )

    var terminateRotationAngle by remember { mutableFloatStateOf(0f) }
    val animatedTerminateRotation by animateFloatAsState(
        targetValue = terminateRotationAngle,
        animationSpec = tween(durationMillis = 600),
        label = "terminateRotation"
    )

    var showTerminateConfirm by remember { mutableStateOf(false) }

    if (showTerminateConfirm) {
        AlertDialog(
            onDismissRequest = { showTerminateConfirm = false },
            title = {
                @Suppress("DEPRECATION")
                Text(
                    text = "Terminate Game",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
            },
            text = {
                @Suppress("DEPRECATION")
                Text(
                    text = stringResource(id = R.string.mcn_exit_confirm),
                    style = MaterialTheme.typography.bodyMedium
                )
            },
            confirmButton = {
                @Suppress("DEPRECATION")
                @SuppressLint("LocalContextGetResourceValueCall")
                Button(
                    onClick = {
                        showTerminateConfirm = false
                        isLaunching = false
                        onTerminateClick()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    @Suppress("DEPRECATION")
                    @SuppressLint("LocalContextGetResourceValueCall")
                    Text("Terminate", fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                @Suppress("DEPRECATION")
                @SuppressLint("LocalContextGetResourceValueCall")
                TextButton(
                    onClick = { showTerminateConfirm = false },
                    shape = RoundedCornerShape(12.dp)
                ) {
                    @Suppress("DEPRECATION")
                    @SuppressLint("LocalContextGetResourceValueCall")
                    Text(stringResource(id = android.R.string.cancel))
                }
            },
            shape = RoundedCornerShape(28.dp),
            containerColor = MaterialTheme.colorScheme.surface,
            tonalElevation = 6.dp
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = if (hasBackground) Color.Transparent else MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Transparent)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {

                if (!hideActionButtons) {
                    Column(
                        modifier = Modifier
                            .weight(0.66f)
                            .fillMaxHeight()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        ActionCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            title = stringResource(id = R.string.mcl_tab_wiki),
                            icon = Icons.Rounded.Info,
                            onClick = onWikiClick
                        )
                        ActionCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            title = stringResource(id = R.string.mcl_button_social_media),
                            icon = Icons.Rounded.Share,
                            onClick = onSocialMediaClick
                        )

                        ActionCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            title = stringResource(id = R.string.mcl_option_customcontrol),
                            icon = Icons.Rounded.Build,
                            onClick = onCustomControlsClick
                        )
                        ActionCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            title = stringResource(id = R.string.main_install_jar_file),
                            icon = Icons.Rounded.Add,
                            onClick = onInstallJarClick
                        )
                        ActionCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            title = stringResource(id = R.string.main_share_logs),
                            icon = Icons.AutoMirrored.Rounded.Send,
                            onClick = onShareLogsClick
                        )
                        ActionCard(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(52.dp),
                            title = stringResource(id = R.string.mcl_button_open_directory),
                            icon = Icons.Rounded.Search,
                            onClick = onOpenFilesClick
                        )
                    }
                } else {
                    Spacer(modifier = Modifier.weight(0.66f))
                }

                Surface(
                    modifier = Modifier
                        .weight(0.34f)
                        .fillMaxHeight(),
                    shape = RoundedCornerShape(32.dp),
                    color = if (hasBackground) MaterialTheme.colorScheme.surface.copy(alpha = backgroundTransparency)
                            else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f),
                    tonalElevation = 0.dp,
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {

                        Column(
                            modifier = Modifier.weight(1f),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .scale(headScale)
                                    .clip(RoundedCornerShape(12.dp))
                                    .clickable(
                                        interactionSource = headInteractionSource,
                                        indication = null,
                                        onClick = onAccountManagerClick
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                if (currentAccount != null) {
                                    if (skinHead != null) {
                                        Image(
                                            bitmap = skinHead!!.asImageBitmap(),
                                            contentDescription = null,
                                            modifier = Modifier.fillMaxSize(),
                                            contentScale = ContentScale.Fit,
                                            filterQuality = FilterQuality.None
                                        )
                                    } else {
                                        CircularProgressIndicator(
                                            modifier = Modifier.size(28.dp),
                                            strokeWidth = 3.dp,
                                            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                                        )
                                    }
                                } else {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Add Account",
                                        modifier = Modifier.size(32.dp),
                                        tint = MaterialTheme.colorScheme.primary
                                    )
                                }
                            }
                        }

                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            color = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.onSurface
                        ) {
                            Row(
                                modifier = Modifier
                                    .padding(start = 12.dp, end = 6.dp, top = 6.dp, bottom = 6.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                Row(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(12.dp))
                                        .clickable(onClick = onInstanceSelect)
                                        .padding(4.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(32.dp)
                                            .clip(RoundedCornerShape(8.dp)),
                                        contentAlignment = Alignment.Center
                                    ) {
                                        if (instanceIcon != null) {
                                            Image(
                                                painter = rememberDrawablePainter(instanceIcon),
                                                contentDescription = null,
                                                modifier = Modifier.fillMaxSize(),
                                                contentScale = ContentScale.Crop
                                            )
                                        } else {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_px_home),
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp),
                                                tint = MaterialTheme.colorScheme.primary
                                            )
                                        }
                                    }

                                    Column(modifier = Modifier.weight(1f)) {
                                        val name = selectedInstance?.name
                                        val instanceDisplayName = if (selectedInstance == null) {
                                            stringResource(id = R.string.no_instance)
                                        } else if (name.isNullOrBlank()) {
                                            "UNNAMED"
                                        } else {
                                            name
                                        }

                                        @Suppress("DEPRECATION")
                                        Text(
                                            text = instanceDisplayName,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.SemiBold,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )
                                        @Suppress("DEPRECATION")
                                        Text(
                                            text = selectedInstance?.versionId ?: stringResource(id = R.string.version_select_hint),
                                            style = MaterialTheme.typography.bodySmall.copy(fontSize = 9.sp),
                                            color = MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                IconButton(
                                    onClick = onEditProfileClick,
                                    modifier = Modifier.size(44.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Settings,
                                        contentDescription = "Edit Profile",
                                        modifier = Modifier.size(24.dp),
                                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth().animateContentSize(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically) {
                            Button(
                                onClick = onPlayClick,
                                modifier = Modifier
                                    .weight(1f)
                                    .height(48.dp),
                                shape = CircleShape,
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.Center
                                ) {
                                    @Suppress("DEPRECATION")
                                    Text(
                                        text = "Launch",
                                        style = MaterialTheme.typography.labelLarge,
                                        fontWeight = FontWeight.Bold,
                                        letterSpacing = 1.sp
                                    )
                                }
                            }

                            AnimatedVisibility(
                                visible = isSomethingRunning,
                                enter = fadeIn(),
                                exit = fadeOut()
                            ) {
                                Button(
                                    onClick = {
                                        terminateRotationAngle += 360f
                                        showTerminateConfirm = true
                                    },
                                    modifier = Modifier.size(48.dp),
                                    shape = CircleShape,
                                    colors = ButtonDefaults.buttonColors(
                                        containerColor = MaterialTheme.colorScheme.error,
                                        contentColor = MaterialTheme.colorScheme.onError
                                    ),
                                    contentPadding = PaddingValues(0.dp),
                                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                                ) {
                                    Icon(
                                        Icons.Rounded.Clear,
                                        contentDescription = "Terminate",
                                        modifier = Modifier
                                            .size(22.dp)
                                            .rotate(animatedTerminateRotation)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ActionCard(
    modifier: Modifier = Modifier,
    title: String,
    icon: ImageVector,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        modifier = modifier,
        shape = CircleShape,
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        contentPadding = PaddingValues(horizontal = 14.dp),
    )
    {
        Row(
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        )
        {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = LocalContentColor.current
            )
            @Suppress("DEPRECATION")
            @SuppressLint("LocalContextGetResourceValueCall")
            Text(
                text = title,
                style = MaterialTheme.typography.labelMedium,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Preview(showBackground = true, device = "spec:width=1280dp,height=800dp,orientation=landscape")
@Composable
fun MainMenuRevampPreview() {
    PojavTheme(dynamicColor = true) {
        MainMenuRevamp(
            onEditProfileClick = {},
            onCustomControlsClick = {},
            onInstallJarClick = {},
            onShareLogsClick = {},
            onOpenFilesClick = {},
            onWikiClick = {},
            onSocialMediaClick = {},
            onPlayClick = {},
            onTerminateClick = {},
            onInstanceSelect = {},
            onAccountManagerClick = {}
        )
    }
}
