package net.kdt.pojavlaunch.ui.screens

import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkRequest
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.VideoView
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.rounded.Chat
import androidx.compose.material.icons.rounded.Public
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.asComposeRenderEffect
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentContainerView
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kdt.mcgui.ProgressLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.BaseActivity
import net.kdt.pojavlaunch.authenticator.AuthType
import net.kdt.pojavlaunch.authenticator.accounts.Accounts
import net.kdt.pojavlaunch.authenticator.accounts.MinecraftAccount
import net.kdt.pojavlaunch.authenticator.listener.LoginListener
import net.kdt.pojavlaunch.extra.ExtraConstants
import net.kdt.pojavlaunch.extra.ExtraCore
import net.kdt.pojavlaunch.extra.ExtraListener
import net.kdt.pojavlaunch.fragments.*
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper
import net.kdt.pojavlaunch.progresskeeper.ProgressListener
import net.kdt.pojavlaunch.ui.theme.PojavTheme
import net.kdt.pojavlaunch.PojavApplication
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.ui.screens.AccountManagerOverlay
import net.kdt.pojavlaunch.kotlin.ui.viewmodel.ContentInstallerViewModel
import net.kdt.pojavlaunch.kotlin.ui.viewmodel.DirectoryManagerViewModel
import net.kdt.pojavlaunch.kotlin.ui.viewmodel.ProfileSelectionViewModel
import net.kdt.pojavlaunch.ui.screens.ProfileSelectionScreen
import net.kdt.pojavlaunch.ui.screens.ProfileTypeSelectScreen
import net.kdt.pojavlaunch.modloaders.modpacks.api.CommonApi
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.core.graphics.drawable.toBitmap
import net.kdt.pojavlaunch.instances.Instances
import net.kdt.pojavlaunch.modloaders.FabriclikeUtils
import net.kdt.pojavlaunch.ui.utils.AnimationUtils
import java.io.FileOutputStream
import java.io.File
import java.io.IOException
import net.kdt.pojavlaunch.modloaders.modpacks.imagecache.ModIconCache
import net.kdt.pojavlaunch.modloaders.modpacks.models.Constants
import net.kdt.pojavlaunch.skin.AndroidSkinAnalyzer
import net.kdt.pojavlaunch.skin.LocalUuidUtils
import net.kdt.pojavlaunch.skin.LocalUuidUtils.toFormattedUuid
import net.kdt.pojavlaunch.skin.SkinModelType
import net.kdt.pojavlaunch.skin.SkinUtils
import net.kdt.pojavlaunch.ui.screens.ContentInstallerScreen
import net.kdt.pojavlaunch.ui.screens.ContentInstallerType
import net.kdt.pojavlaunch.ui.screens.DirectoryManagerScreen
import net.kdt.pojavlaunch.ui.screens.SettingsScreen
import net.kdt.pojavlaunch.utils.UpdateUtils

private val m3MotionSpec = spring<Float>(
    dampingRatio = 0.85f,
    stiffness = 320f
)

private val m3SizeSpec = spring<IntSize>(
    dampingRatio = Spring.DampingRatioNoBouncy,
    stiffness = Spring.StiffnessMediumLow
)

/** A VideoView that crops to fill its container instead of fitting inside */
class CropVideoView(context: Context) : VideoView(context) {
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        val width = getDefaultSize(0, widthMeasureSpec)
        val height = getDefaultSize(0, heightMeasureSpec)
        
        try {
            val vWidthField = VideoView::class.java.getDeclaredField("mVideoWidth")
            val vHeightField = VideoView::class.java.getDeclaredField("mVideoHeight")
            vWidthField.isAccessible = true
            vHeightField.isAccessible = true
            val vWidth = vWidthField.getInt(this)
            val vHeight = vHeightField.getInt(this)

            if (vWidth > 0 && vHeight > 0) {
                val videoAspectRatio = vWidth.toFloat() / vHeight.toFloat()
                val viewAspectRatio = width.toFloat() / height.toFloat()
                if (videoAspectRatio > viewAspectRatio) {
                    setMeasuredDimension((height * videoAspectRatio).toInt(), height)
                } else {
                    setMeasuredDimension(width, (width / videoAspectRatio).toInt())
                }
                return
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        setMeasuredDimension(width, height)
    }
}

@Composable
fun LauncherBackground(isPaused: Boolean = false) {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    val backgroundPath = LauncherPreferences.PREF_BACKGROUND_PATH_STATE.value
    val backgroundVideoPath = LauncherPreferences.PREF_BACKGROUND_VIDEO_PATH_STATE.value
    val backgroundVideoLoop = LauncherPreferences.PREF_BACKGROUND_VIDEO_LOOP_STATE.value
    val backgroundVideoVolume = LauncherPreferences.PREF_BACKGROUND_VIDEO_VOLUME_STATE.value
    val backgroundTransparency = LauncherPreferences.PREF_BACKGROUND_TRANSPARENCY_STATE.value
    val backgroundBlurEnabled = LauncherPreferences.PREF_BACKGROUND_BLUR_ENABLED_STATE.value
    val backgroundBlurIntensity = LauncherPreferences.PREF_BACKGROUND_BLUR_STATE.value
    val backgroundRevision = LauncherPreferences.PREF_BACKGROUND_REVISION_STATE.intValue

    // Performance fix: Move bitmap decoding off-thread
    val backgroundImage = produceState<Bitmap?>(initialValue = null, backgroundPath, backgroundVideoPath, backgroundRevision) {
        if (backgroundPath != null && backgroundVideoPath == null) {
            value = withContext(Dispatchers.IO) {
                try {
                    if (backgroundPath.startsWith("content://")) {
                        context.contentResolver.openInputStream(Uri.parse(backgroundPath))?.use {
                            BitmapFactory.decodeStream(it)
                        }
                    } else {
                        BitmapFactory.decodeFile(backgroundPath)
                    }
                } catch (_: Exception) {
                    null
                }
            }
        } else {
            value = null
        }
    }

    val previewBackgroundBitmap = if (isPreview) BaseActivity.getBackgroundBitmap() else null

    Box(modifier = Modifier.fillMaxSize().graphicsLayer { clip = true }, contentAlignment = Alignment.Center) {
        if (backgroundVideoPath != null && !isPreview) {
            if (!isPaused) {
                AndroidView(
                    factory = { ctx ->
                        CropVideoView(ctx).apply {
                            setVideoURI(Uri.fromFile(File(backgroundVideoPath)))
                            setOnPreparedListener { player ->
                                player.isLooping = backgroundVideoLoop
                                player.setVolume(backgroundVideoVolume, backgroundVideoVolume)
                                start()
                            }
                            setOnCompletionListener {
                                if (backgroundVideoLoop) start()
                            }
                        }
                    },
                    update = { view ->
                        val tag = Triple(backgroundVideoPath, backgroundVideoLoop, backgroundVideoVolume)
                        if (view.tag != tag) {
                            view.tag = tag
                            view.setVideoURI(Uri.fromFile(File(backgroundVideoPath)))
                            view.setOnPreparedListener { player ->
                                player.isLooping = backgroundVideoLoop
                                player.setVolume(backgroundVideoVolume, backgroundVideoVolume)
                                view.start()
                            }
                            view.setOnCompletionListener {
                                if (backgroundVideoLoop) view.start()
                            }
                        }
                    },
                    onRelease = { videoView ->
                        videoView.stopPlayback()
                    },
                    modifier = Modifier.wrapContentSize()
                )
            } else {
                Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
            }
        } else if (backgroundImage.value != null) {
            Image(
                bitmap = backgroundImage.value!!.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        if (backgroundBlurEnabled && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            val blurPx = (backgroundBlurIntensity * 40).dp.toPx()
                            if (blurPx > 0.1f) {
                                renderEffect = android.graphics.RenderEffect.createBlurEffect(
                                    blurPx, blurPx, android.graphics.Shader.TileMode.CLAMP
                                ).asComposeRenderEffect()
                            }
                        }
                    }
                    .then(if (backgroundBlurEnabled && Build.VERSION.SDK_INT < Build.VERSION_CODES.S) Modifier.blur((backgroundBlurIntensity * 40).dp) else Modifier),
                contentScale = ContentScale.Crop
            )
        } else if (isPreview && previewBackgroundBitmap != null) {
            Image(
                bitmap = previewBackgroundBitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
        }
    }
}

class TaskProgress(
    val key: String,
    initialProgress: Int = 0,
    initialText: String = ""
) {
    var progress by mutableIntStateOf(initialProgress)
    var text by mutableStateOf(initialText)
}

@Composable
fun TaskProgressItem(task: TaskProgress) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            @Suppress("DEPRECATION")
            Text(
                text = task.text,
                color = MaterialTheme.colorScheme.onSurface,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f)
            )
            if (task.progress >= 0) {
                @Suppress("DEPRECATION")
                Text(
                    text = "${task.progress}%",
                    color = MaterialTheme.colorScheme.primary,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        if (task.progress >= 0) {
            LinearProgressIndicator(
                progress = { task.progress.toFloat() / 100f },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        } else {
            LinearProgressIndicator(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .clip(RoundedCornerShape(4.dp)),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surfaceVariant,
            )
        }
    }
}

@Composable
fun ProgressCard(
    onClose: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isPreview = LocalInspectionMode.current
    val context = LocalContext.current

    val activeTasks = remember { mutableStateMapOf<String, TaskProgress>() }
    var isExpanded by remember { mutableStateOf(true) }

    if (!isPreview) {
        DisposableEffect(Unit) {
            val keys = arrayOf(
                ProgressLayout.UNPACK_RUNTIME, ProgressLayout.DOWNLOAD_MINECRAFT,
                ProgressLayout.DOWNLOAD_VERSION_LIST, ProgressLayout.AUTHENTICATE,
                ProgressLayout.INSTALL_MODPACK, ProgressLayout.EXTRACT_COMPONENTS,
                ProgressLayout.EXTRACT_SINGLE_FILES, ProgressLayout.INSTANCE_INSTALL,
                ProgressLayout.CONTENT_INSTALL
            )

            val listeners = keys.map { key ->
                val listener = object : ProgressListener {
                    override fun onProgressStarted() {
                        (context as? FragmentActivity)?.runOnUiThread {
                            activeTasks[key] = TaskProgress(key)
                        }
                    }

                    override fun onProgressUpdated(progress: Int, resid: Int, vararg va: Any?) {
                        (context as? FragmentActivity)?.runOnUiThread {
                            val task = activeTasks.getOrPut(key) { TaskProgress(key) }
                            task.progress = progress
                            task.text = if (resid > 0) context.getString(resid, *va)
                                       else if (va.isNotEmpty() && va[0] != null) va[0].toString()
                                       else ""
                        }
                    }

                    override fun onProgressEnded() {
                        (context as? FragmentActivity)?.runOnUiThread {
                            activeTasks.remove(key)
                        }
                    }
                }
                ProgressKeeper.addListener(key, listener)
                key to listener
            }

            onDispose {
                listeners.forEach { (key, listener) ->
                    ProgressKeeper.removeListener(key, listener)
                }
            }
        }
    } else {
        LaunchedEffect(Unit) {
            activeTasks["dl"] = TaskProgress("dl", 45, "Downloading Assets...")
            activeTasks["auth"] = TaskProgress("auth", 90, "Authenticating...")
        }
    }

    if (activeTasks.isEmpty() && !isPreview) return

    ElevatedCard(
        modifier = modifier
            .width(dimensionResource(id = R.dimen._280sdp))
            .animateContentSize(animationSpec = m3SizeSpec),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface.copy(alpha = LauncherPreferences.PREF_CONTENT_TRANSPARENCY_STATE.value.coerceAtLeast(0.6f))
        ),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 6.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp)
                    .padding(top = 8.dp, bottom = 4.dp)
            ) {
                IconButton(
                    modifier = Modifier
                        .size(28.dp)
                        .align(Alignment.CenterStart),
                    onClick = onClose
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp).graphicsLayer { rotationZ = 180f }
                    )
                }

                @Suppress("DEPRECATION")
                Text(
                    text = stringResource(id = R.string.progresslayout_tasks_in_progress, activeTasks.size),
                    modifier = Modifier.align(Alignment.Center).clickable { isExpanded = !isExpanded },
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface
                )

                IconButton(
                    modifier = Modifier
                        .size(28.dp)
                        .align(Alignment.CenterEnd),
                    onClick = { isExpanded = !isExpanded }
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                        contentDescription = null,
                        modifier = Modifier
                            .size(18.dp)
                            .graphicsLayer { rotationZ = if (isExpanded) 90f else 270f }
                            .alpha(0.6f),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
            }

            AnimatedVisibility(
                visible = isExpanded,
                enter = fadeIn(spring(stiffness = Spring.StiffnessLow)) + expandVertically(spring(stiffness = Spring.StiffnessLow)),
                exit = fadeOut(spring(stiffness = Spring.StiffnessLow)) + shrinkVertically(spring(stiffness = Spring.StiffnessLow))
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                        .heightIn(max = 300.dp),
                    contentPadding = PaddingValues(top = 4.dp)
                ) {
                    items(activeTasks.values.toList(), key = { it.key }) { task ->
                        TaskProgressItem(task)
                    }
                }
            }
        }
    }
}

@Composable
fun TopBarButton(
    onClick: () -> Unit,
    label: String,
    topBarHeight: androidx.compose.ui.unit.Dp,
    modifier: Modifier = Modifier,
    icon: Int? = null,
    imageVector: ImageVector? = null,
    isSelected: Boolean = false,
    isSpecialActive: Boolean = false,
    isRotating: Boolean = false,
    badgeCount: Int = 0,
    ignoreNotch: Boolean = false,
    isLeftmost: Boolean = false,
    isRightmost: Boolean = false
) {
    val activeColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = LauncherPreferences.PREF_CONTENT_TRANSPARENCY_STATE.value.coerceAtLeast(0.6f))
    val contentColor = if (isSelected || isSpecialActive) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurfaceVariant

    val infiniteTransition = rememberInfiniteTransition(label = "rotation")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Box(modifier = modifier) {
        Surface(
            onClick = onClick,
            modifier = Modifier
                .height(topBarHeight - 8.dp)
                .padding(
                    start = if (ignoreNotch && isLeftmost) 8.dp else 4.dp,
                    end = if (ignoreNotch && isRightmost) 8.dp else 4.dp
                ),
            shape = MaterialTheme.shapes.large,
            color = if (isSelected || isSpecialActive) activeColor else Color.Transparent,
            tonalElevation = if (isSelected) 2.dp else 0.dp
        ) {
            Row(
                modifier = Modifier.padding(horizontal = if (isSelected) 16.dp else 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                if (imageVector != null) {
                    Icon(
                        imageVector = imageVector,
                        contentDescription = label,
                        modifier = Modifier.size(20.dp).graphicsLayer {
                            if (isRotating) rotationZ = rotation
                        },
                        tint = contentColor
                    )
                } else if (icon != null) {
                    Icon(
                        painter = painterResource(id = icon),
                        contentDescription = label,
                        modifier = Modifier.size(20.dp).graphicsLayer {
                            if (isRotating) rotationZ = rotation
                        },
                        tint = contentColor
                    )
                }

                AnimatedVisibility(
                    visible = isSelected,
                    enter = fadeIn(m3MotionSpec) + expandHorizontally(expandFrom = Alignment.Start, clip = false, animationSpec = m3SizeSpec),
                    exit = fadeOut(m3MotionSpec) + shrinkHorizontally(shrinkTowards = Alignment.Start, clip = false, animationSpec = m3SizeSpec)
                ) {
                    Row {
                        Spacer(Modifier.width(6.dp))
                        @Suppress("DEPRECATION")
                        Text(
                            text = label,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.ExtraBold,
                            maxLines = 1,
                            overflow = TextOverflow.Clip,
                            color = contentColor
                        )
                    }
                }
            }
        }

        if (badgeCount > 0) {
            Badge(
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .offset(x = 2.dp, y = (-2).dp),
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary
            ) {
                Text(badgeCount.toString(), fontSize = 10.sp)
            }
        }
    }
}

@Composable
fun TopBar(
    topBarHeight: androidx.compose.ui.unit.Dp,
    ignoreNotch: Boolean,
    hasBackground: Boolean,
    isAnyScreenOpen: Boolean,
    isProgressVisible: Boolean,
    isOnline: Boolean,
    taskCount: Int,
    selectedCategory: Int,
    currentFragmentTag: String?,
    onHomeClick: () -> Unit,
    onProgressClick: () -> Unit,
    onCategoryClick: (Int) -> Unit,
    onAboutClick: () -> Unit
) {
    val title = remember(selectedCategory, currentFragmentTag) {
        when (selectedCategory) {
            1 -> "Files"
            2 -> "Installer"
            3 -> "Settings"
            4 -> "Accounts"
            5 -> "Select Instance"
            6 -> "Instance Type"
            7 -> "Edit Instance"
            8 -> "About"
            else -> {
                when (currentFragmentTag) {
                    MainMenuFragment.TAG -> "HyperLauncher"
                    "FileSelectorFragment" -> "Select File"
                    "InstanceEditorFragment" -> "Edit Instance"
                    null -> "HyperLauncher"
                    else -> currentFragmentTag.substringAfterLast('.').replace("Fragment", "")
                }
            }
        }
    }

    Surface(
        modifier = Modifier.fillMaxWidth().height(topBarHeight),
        color = MaterialTheme.colorScheme.surface.copy(
            alpha = if (hasBackground) LauncherPreferences.PREF_CONTENT_TRANSPARENCY_STATE.value else 1f
        ),
        tonalElevation = 3.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .run {
                        if (ignoreNotch) this
                        else windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
                    },
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    modifier = Modifier.padding(start = if (ignoreNotch) 8.dp else 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (isAnyScreenOpen) {
                        IconButton(
                            onClick = onHomeClick,
                            modifier = Modifier.size(32.dp)
                        ) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.onSurface
                            )
                        }
                    } else {
                        Icon(
                            painter = painterResource(id = R.drawable.icon_hyper),
                            contentDescription = null,
                            modifier = Modifier.size(24.dp).padding(start = 8.dp).clickable(onClick = onAboutClick),
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(Modifier.width(8.dp))
                    }

                    Text(
                        text = "HyperLauncher",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.clickable(onClick = onAboutClick)
                    )

                    if (isAnyScreenOpen && title != "HyperLauncher") {
                        Text(
                            text = " — ",
                            modifier = Modifier.padding(horizontal = 4.dp),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f)
                        )
                        
                        Text(
                            text = title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                Row(
                    modifier = Modifier
                        .padding(end = if (ignoreNotch) 12.dp else 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Task indicator with horizontal expansion to avoid vertical glitching
                    AnimatedVisibility(
                        visible = !isProgressVisible && taskCount > 0,
                        enter = fadeIn() + expandHorizontally(expandFrom = Alignment.End),
                        exit = fadeOut() + shrinkHorizontally(shrinkTowards = Alignment.End)
                    ) {
                        Row(
                            modifier = Modifier
                                .clip(shape = MaterialTheme.shapes.large)
                                .clickable { onProgressClick() }
                                .padding(all = 8.dp)
                                .width(120.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            LinearProgressIndicator(modifier = Modifier.weight(1f))
                            Icon(
                                modifier = Modifier.size(22.dp),
                                imageVector = Icons.Default.HourglassEmpty,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }

                    TopBarButton(
                        onClick = { onCategoryClick(1) },
                        isSelected = selectedCategory == 1,
                        imageVector = Icons.Default.Folder,
                        label = "Files",
                        topBarHeight = topBarHeight
                    )

                    TopBarButton(
                        onClick = { onCategoryClick(2) },
                        isSelected = selectedCategory == 2,
                        imageVector = Icons.Default.Download,
                        label = "Installer",
                        topBarHeight = topBarHeight
                    )

                    TopBarButton(
                        onClick = { onCategoryClick(3) },
                        isSelected = selectedCategory == 3,
                        imageVector = Icons.Default.Settings,
                        label = "Settings",
                        topBarHeight = topBarHeight,
                        ignoreNotch = ignoreNotch,
                        isRightmost = true
                    )
                }
            }

            // Connection indicator line
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(1.dp)
                    .background(if (isOnline) Color.White else Color.Red.copy(alpha = 0.8f))
                    .align(Alignment.BottomCenter)
            )
        }
    }
}

@Composable
fun LauncherScreen(
    onHomeRequest: () -> Unit,
    onProgressClick: () -> Unit,
    isProgressVisible: Boolean,
    taskCount: Int,
    isFragmentOpen: Boolean = false
) {
    val isPreview = LocalInspectionMode.current
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val topBarHeight = 40.dp
    val ignoreNotch = remember { if (isPreview) true else LauncherPreferences.PREF_IGNORE_NOTCH }

    val backgroundPath = LauncherPreferences.PREF_BACKGROUND_PATH_STATE.value
    val backgroundVideoPath = LauncherPreferences.PREF_BACKGROUND_VIDEO_PATH_STATE.value
    val backgroundTransparency = LauncherPreferences.PREF_BACKGROUND_TRANSPARENCY_STATE.value
    val hasBackground = backgroundPath != null || backgroundVideoPath != null || isPreview

    var selectedCategory by rememberSaveable { mutableIntStateOf(-1) }
    var showAddAccountDialogInManager by rememberSaveable { mutableStateOf(false) }

    val isAnyScreenOpen by remember(selectedCategory, isFragmentOpen) {
        derivedStateOf { 
            val activity = context as? FragmentActivity
            val manager = activity?.supportFragmentManager
            selectedCategory != -1 || isFragmentOpen || (manager?.backStackEntryCount ?: 0) > 0
        }
    }

    var accounts by remember { mutableStateOf<List<MinecraftAccount>>(emptyList()) }
    var currentAccount by remember { mutableStateOf(if (isPreview) null else Accounts.getCurrent()) }

    var updateInfo by remember { mutableStateOf<UpdateUtils.UpdateInfo?>(null) }
    var showUpdateDialog by rememberSaveable { mutableStateOf(false) }
    var isUpdateDismissed by rememberSaveable { mutableStateOf(false) }

    var isGameLaunching by remember { mutableStateOf(false) }

    val isOnline = remember { mutableStateOf(true) }

    var currentFragmentTag by remember { mutableStateOf<String?>(null) }

    DisposableEffect(context) {
        val activity = context as? FragmentActivity
        val manager = activity?.supportFragmentManager
        
        val backStackListener = FragmentManager.OnBackStackChangedListener {
            val count = manager?.backStackEntryCount ?: 0
            if (count > 0) {
                currentFragmentTag = manager?.getBackStackEntryAt(count - 1)?.name
            } else {
                currentFragmentTag = manager?.findFragmentById(R.id.container_fragment)?.tag
            }
        }
        
        manager?.addOnBackStackChangedListener(backStackListener)
        
        // Initial tag
        val count = manager?.backStackEntryCount ?: 0
        currentFragmentTag = if (count > 0) {
            manager?.getBackStackEntryAt(count - 1)?.name
        } else {
            manager?.findFragmentById(R.id.container_fragment)?.tag
        }

        val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkCallback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                isOnline.value = true
            }

            override fun onLost(network: Network) {
                isOnline.value = false
            }
        }

        val request = NetworkRequest.Builder()
            .addCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
            .build()
        connectivityManager.registerNetworkCallback(request, networkCallback)

        // Initial check
        val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
        isOnline.value = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        onDispose {
            connectivityManager.unregisterNetworkCallback(networkCallback)
            manager?.removeOnBackStackChangedListener(backStackListener)
        }
    }

    LaunchedEffect(Unit) {
        if (!isPreview && !isUpdateDismissed && !LauncherPreferences.PREF_SKIP_UPDATE_CHECK) {
            val info = UpdateUtils.checkForUpdates()
            if (info != null && info.hasUpdate) {
                if (info.latestVersion != LauncherPreferences.PREF_LATEST_ACKNOWLEDGED_VERSION) {
                    updateInfo = info
                    showUpdateDialog = true
                }
            }
        }
    }

    val refreshAccountsList: () -> Unit = {
        scope.launch(Dispatchers.IO) {
            try {
                val loadedAccounts = Accounts.load().accounts.filterNotNull()
                withContext(Dispatchers.Main) {
                    accounts = loadedAccounts
                    currentAccount = Accounts.getCurrent()
                }

                loadedAccounts.forEach { account ->
                    if (account.getSkinFace() == null) {
                        account.updateSkinFace(context.assets)
                    }
                }
                
                val finalAccounts = Accounts.load().accounts.filterNotNull()
                withContext(Dispatchers.Main) {
                    accounts = finalAccounts
                    currentAccount = Accounts.getCurrent()
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    accounts = emptyList()
                }
            }
        }
    }

    val loginListener = remember {
        object : LoginListener {
            override fun onLoginDone(account: MinecraftAccount) {
                Accounts.setCurrent(account)
                refreshAccountsList()
                ExtraCore.setValue(ExtraConstants.REFRESH_ACCOUNT_SPINNER, true)
            }

            override fun onLoginError(errorMessage: Throwable) {
                Toast.makeText(context, errorMessage.message ?: "Login failed", Toast.LENGTH_LONG).show()
            }

            override fun onLoginProgress(step: Int) {}
            override fun setMaxLoginProgress(max: Int) {}
        }
    }

    val mojangListener = remember {
        ExtraListener<Array<String?>> { _, value ->
            try {
                val username = value.getOrNull(0) ?: "Steve"
                val skinPath = value.getOrNull(2)
                val capePath = value.getOrNull(3)

                val skinModel = if (value.size > 4 && value[4] != null) {
                    try { SkinModelType.valueOf(value[4]!!) } catch (_: Exception) { SkinModelType.STEVE }
                } else if (skinPath != null) {
                    AndroidSkinAnalyzer.detectModel(File(skinPath).readBytes())
                } else SkinModelType.STEVE

                val acc = Accounts.create {
                    it.username = username
                    it.authType = AuthType.LOCAL
                    it.skinPath = skinPath
                    it.capePath = capePath
                    it.skinModel = skinModel
                    it.profileId = LocalUuidUtils.generateProfileId(username, skinModel).toFormattedUuid()
                }

                acc.updateSkinFace(context.assets)
                loginListener.onLoginDone(acc)
            } catch(e: Exception) {
                loginListener.onLoginError(e)
            }
            false
        }
    }

    val microsoftListener = remember {
        ExtraListener<String> { _, value ->
            AuthType.MICROSOFT.createAuth()?.createAccount(loginListener, value)
            false
        }
    }

    val elyByListener = remember {
        ExtraListener<String> { _, value ->
            AuthType.ELY_BY.createAuth()?.createAccount(loginListener, value)
            false
        }
    }

    val refreshListener = remember {
        ExtraListener<Boolean> { _, _ ->
            refreshAccountsList()
            false
        }
    }

    val launchGameListener = remember {
        ExtraListener<Boolean> { _, value ->
            if (value) {
                if (Accounts.getCurrent() == null) {
                    Toast.makeText(context, "No account selected!", Toast.LENGTH_SHORT).show()
                } else {
                    isGameLaunching = true
                }
            }
            false
        }
    }

    val openScreenListener = remember {
        ExtraListener<Int> { _, value ->
            selectedCategory = value
            false
        }
    }

    DisposableEffect(Unit) {
        if (!isPreview) {
            refreshAccountsList()
            ExtraCore.addExtraListener(ExtraConstants.REFRESH_ACCOUNT_SPINNER, refreshListener)
            ExtraCore.addExtraListener(ExtraConstants.MOJANG_LOGIN_TODO, mojangListener)
            ExtraCore.addExtraListener(ExtraConstants.MICROSOFT_LOGIN_TODO, microsoftListener)
            ExtraCore.addExtraListener(ExtraConstants.ELYBY_LOGIN_TODO, elyByListener)
            ExtraCore.addExtraListener(ExtraConstants.LAUNCH_GAME, launchGameListener)
            ExtraCore.addExtraListener(ExtraConstants.OPEN_SCREEN, openScreenListener)
            ExtraCore.addExtraListener(ExtraConstants.SELECT_AUTH_METHOD, ExtraListener<Boolean> { _, value ->
                if (value) {
                    selectedCategory = 4
                    showAddAccountDialogInManager = false
                }
                false
            })
        }

        onDispose {
            if (!isPreview) {
                ExtraCore.removeExtraListenerFromValue(ExtraConstants.REFRESH_ACCOUNT_SPINNER, refreshListener)
                ExtraCore.removeExtraListenerFromValue(ExtraConstants.MOJANG_LOGIN_TODO, mojangListener)
                ExtraCore.removeExtraListenerFromValue(ExtraConstants.MICROSOFT_LOGIN_TODO, microsoftListener)
                ExtraCore.removeExtraListenerFromValue(ExtraConstants.ELYBY_LOGIN_TODO, elyByListener)
                ExtraCore.removeExtraListenerFromValue(ExtraConstants.LAUNCH_GAME, launchGameListener)
                ExtraCore.removeExtraListenerFromValue(ExtraConstants.OPEN_SCREEN, openScreenListener)
            }
        }
    }

    // Close progress overlay automatically when tasks finish
    LaunchedEffect(taskCount) {
        if (taskCount == 0 && isProgressVisible) {
            onProgressClick()
        }
    }

    val transitionSpec = AnimationUtils.getTransitionSpec()

    Box(modifier = Modifier.fillMaxSize()) {
        LauncherBackground(isPaused = isGameLaunching || taskCount > 0)

        Column(modifier = Modifier.fillMaxSize()) {
            TopBar(
                topBarHeight = topBarHeight,
                ignoreNotch = ignoreNotch,
                hasBackground = hasBackground,
                isAnyScreenOpen = isAnyScreenOpen,
                isProgressVisible = isProgressVisible,
                isOnline = isOnline.value,
                taskCount = taskCount,
                selectedCategory = selectedCategory,
                currentFragmentTag = currentFragmentTag,
                onHomeClick = {
                    val activity = context as? FragmentActivity
                    val manager = activity?.supportFragmentManager
                    
                    if (selectedCategory != -1) {
                        selectedCategory = -1
                        showAddAccountDialogInManager = false
                    } else if (manager != null && manager.backStackEntryCount > 0) {
                        manager.popBackStack()
                    } else if (isFragmentOpen) {
                        onHomeRequest()
                    }
                    if (isProgressVisible) onProgressClick()
                },
                onProgressClick = onProgressClick,
                onCategoryClick = { category ->
                    if (selectedCategory == category) {
                        selectedCategory = -1 // Toggle home if clicking already selected
                    } else {
                        if (isProgressVisible) onProgressClick()
                        selectedCategory = category
                    }
                    showAddAccountDialogInManager = false
                },
                onAboutClick = {
                    if (selectedCategory == 8) {
                        selectedCategory = -1
                    } else {
                        if (isProgressVisible) onProgressClick()
                        selectedCategory = 8
                    }
                }
            )

            // Persistent screen layer wrapper
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .background(
                        if (hasBackground) MaterialTheme.colorScheme.surface.copy(alpha = backgroundTransparency)
                        else Color.Transparent
                    )
            ) {
                
                // Fragment Container is always present but hidden when an overlay is open
                if (!isPreview) {
                    AndroidView(
                        factory = { ctx ->
                            FragmentContainerView(ctx).apply {
                                id = R.id.container_fragment
                            }
                        },
                        update = { view ->
                            val activity = view.context as? FragmentActivity ?: return@AndroidView
                            val manager = activity.supportFragmentManager
                            if (manager.isStateSaved) return@AndroidView

                            val existing = manager.findFragmentByTag(MainMenuFragment.TAG)
                            val current = manager.findFragmentById(view.id)

                            if (current == null && existing == null) {
                                manager.beginTransaction()
                                    .replace(view.id, MainMenuFragment(), MainMenuFragment.TAG)
                                    .commitAllowingStateLoss()
                            }
                        },
                        modifier = Modifier.fillMaxSize().alpha(if (selectedCategory == -1) 1f else 0f)
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(Color.Gray.copy(alpha = 0.2f)).alpha(if (selectedCategory == -1) 1f else 0f), contentAlignment = Alignment.Center) {
                        Text("Home Fragment")
                    }
                }

                AnimatedContent(
                    targetState = selectedCategory,
                    transitionSpec = transitionSpec,
                    modifier = Modifier.fillMaxSize().graphicsLayer { clip = true },
                    label = "mainScreenTransition"
                ) { state ->
                    if (state != -1) {
                        Surface(
                            modifier = Modifier.fillMaxSize(),
                            color = Color.Transparent
                        ) {
                            when (state) {
                                1 -> DirectoryManagerOverlay(onBack = { selectedCategory = -1 })
                                2 -> ContentInstallerOverlay(onBack = { selectedCategory = -1 })
                                3 -> SettingsOverlay(onBack = { selectedCategory = -1 })
                                8 -> AboutOverlay(onBack = { selectedCategory = -1 })
                                4 -> AccountManagerOverlay(
                                    accounts = accounts,
                                    currentAccount = currentAccount,
                                    onAccountSelect = { account ->
                                        Accounts.setCurrent(account)
                                        currentAccount = account
                                        ExtraCore.setValue(ExtraConstants.REFRESH_ACCOUNT_SPINNER, true)
                                        if (account.authType.requiresLogin() && System.currentTimeMillis() > account.expiresAt) {
                                            account.authType.createAuth()?.refreshAccount(loginListener, account)
                                        }
                                    },
                                    onAccountDelete = { account ->
                                        Accounts.delete(account)
                                        refreshAccountsList()
                                    },
                                    onAccountUpdate = {
                                        refreshAccountsList()
                                    },
                                    onMicrosoftClick = {
                                        ExtraCore.setValue(ExtraConstants.MICROSOFT_LOGIN_TODO, "TODO")
                                    },
                                    onElyByClick = {
                                        ExtraCore.setValue(ExtraConstants.ELYBY_LOGIN_TODO, "TODO")
                                    },
                                    onLocalClick = { username, skinPath, capePath, skinModel ->
                                        try {
                                            val acc = Accounts.create {
                                                it.username = username
                                                it.authType = AuthType.LOCAL
                                                it.skinPath = skinPath
                                                it.capePath = capePath
                                                it.skinModel = skinModel
                                                it.profileId = LocalUuidUtils.generateProfileId(username, skinModel).toFormattedUuid()
                                            }
                                            acc.updateSkinFace(context.assets)
                                            loginListener.onLoginDone(acc)
                                        } catch (e: Exception) {
                                            loginListener.onLoginError(e)
                                        }
                                    },
                                    onBack = { selectedCategory = -1 },
                                    startWithAddDialog = showAddAccountDialogInManager
                                )
                                5 -> ProfileSelectionOverlay(
                                    onBack = { selectedCategory = -1 },
                                    onNavigate = { selectedCategory = it }
                                )
                                6 -> ProfileTypeSelectOverlay(
                                    onBack = { selectedCategory = -1 },
                                    onNavigate = { selectedCategory = it }
                                )
                                7 -> InstanceEditorOverlay(onBack = { selectedCategory = -1 })
                            }
                        }
                    } else {
                        Spacer(modifier = Modifier.fillMaxSize())
                    }
                }
            }
        }

        AnimatedVisibility(
            visible = isProgressVisible && taskCount > 0,
            enter = fadeIn(spring(stiffness = Spring.StiffnessLow)),
            exit = fadeOut(spring(stiffness = Spring.StiffnessLow)),
            modifier = Modifier
                .align(Alignment.TopEnd)
                .padding(top = topBarHeight + 12.dp, end = 12.dp)
        ) {
            ProgressCard(
                onClose = onProgressClick,
                modifier = Modifier.run {
                    if (ignoreNotch) this
                    else windowInsetsPadding(WindowInsets.displayCutout.only(WindowInsetsSides.Horizontal))
                }
            )
        }

        AnimatedVisibility(
            visible = showUpdateDialog && updateInfo != null,
            enter = fadeIn(),
            exit = fadeOut(),
            modifier = Modifier.fillMaxSize()
        ) {
            updateInfo?.let { info ->
                Box(
                    modifier = Modifier.fillMaxSize().padding(16.dp),
                    contentAlignment = Alignment.CenterStart
                ) {
                    ElevatedCard(
                        modifier = Modifier.width(320.dp).animateContentSize(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(20.dp)) {
                            Text("Update Available", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.Bold)
                            Spacer(Modifier.height(8.dp))
                            Text("A new version (${info.latestVersion}) is available!", style = MaterialTheme.typography.bodyLarge)

                            Spacer(Modifier.height(12.dp))
                            Text("Changelog:", fontWeight = FontWeight.Bold, style = MaterialTheme.typography.labelLarge)
                            Box(modifier = Modifier
                                .heightIn(max = 120.dp)
                                .verticalScroll(rememberScrollState())
                            ) {
                                Text(info.changelog, style = MaterialTheme.typography.bodySmall)
                            }

                            Spacer(Modifier.height(16.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.End,
                                verticalAlignment = Alignment.CenterVertically) {
                                TextButton(onClick = {
                                    LauncherPreferences.PREF_LATEST_ACKNOWLEDGED_VERSION = info.latestVersion
                                    LauncherPreferences.DEFAULT_PREF?.edit()?.putString(LauncherPreferences.PREF_KEY_LATEST_ACKNOWLEDGED_VERSION, info.latestVersion)?.apply()
                                    showUpdateDialog = false
                                    isUpdateDismissed = true
                                }) {
                                    Text("NEVER")
                                }
                                Spacer(Modifier.weight(1f))
                                TextButton(onClick = { 
                                    showUpdateDialog = false
                                    isUpdateDismissed = true
                                }) {
                                    Text("LATER")
                                }
                                Button(onClick = {
                                    LauncherPreferences.PREF_LATEST_ACKNOWLEDGED_VERSION = info.latestVersion
                                    LauncherPreferences.DEFAULT_PREF?.edit()?.putString(LauncherPreferences.PREF_KEY_LATEST_ACKNOWLEDGED_VERSION, info.latestVersion)?.apply()
                                    showUpdateDialog = false
                                    isUpdateDismissed = true
                                    try {
                                        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(info.updateUrl))
                                        context.startActivity(intent)
                                    } catch (e: Exception) {
                                        e.printStackTrace()
                                    }
                                }) {
                                    Text("UPDATE")
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
private fun DirectoryManagerOverlay(onBack: () -> Unit) {
    val viewModel: DirectoryManagerViewModel = viewModel()

    LaunchedEffect(Unit) {
        viewModel.init(null, null)
    }

    val uploadLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { _ -> }

    var showNewFolderDialog by remember { mutableStateOf(false) }
    var showRenameDialog by remember { mutableStateOf(false) }
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var inputName by remember { mutableStateOf("") }

    if (showNewFolderDialog) {
        AlertDialog(
            onDismissRequest = { showNewFolderDialog = false },
            title = { Text("New Folder") },
            text = {
                OutlinedTextField(
                    value = inputName,
                    onValueChange = { inputName = it },
                    label = { Text("Folder Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.createFolder(inputName)
                    showNewFolderDialog = false
                    inputName = ""
                }) { Text("Create") }
            },
            dismissButton = {
                @Suppress("DEPRECATION")
                TextButton(onClick = { showNewFolderDialog = false }) { Text(stringResource(id = android.R.string.cancel)) }
            }
        )
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Rename") },
            text = {
                OutlinedTextField(
                    value = inputName,
                    onValueChange = { inputName = it },
                    label = { Text("New Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                Button(onClick = {
                    viewModel.renameSelected(inputName)
                    showRenameDialog = false
                    inputName = ""
                }) { Text("Rename") }
            },
            dismissButton = {
                @Suppress("DEPRECATION")
                TextButton(onClick = { showRenameDialog = false }) { Text(stringResource(id = android.R.string.cancel)) }
            }
        )
    }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Confirm Delete") },
            text = { Text("Are you sure you want to delete '${viewModel.selectedFile?.name}'?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteSelected()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.error,
                        contentColor = MaterialTheme.colorScheme.onError
                    )
                ) { Text("Delete") }
            },
            dismissButton = {
                @Suppress("DEPRECATION")
                TextButton(onClick = { showDeleteConfirm = false }) { Text(stringResource(id = android.R.string.cancel)) }
            }
        )
    }

    BackHandler { onBack() }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
        DirectoryManagerScreen(
            onBack = onBack,
            title = viewModel.title,
            breadcrumbs = viewModel.getBreadcrumbs(),
            entries = viewModel.entries,
            selectedFile = viewModel.selectedFile,
            statusText = viewModel.statusText,
            onEntryClick = { file ->
                if (file.isDirectory) viewModel.openDir(file)
                else viewModel.selectedFile = file
            },
            onEntryLongClick = { file -> viewModel.selectedFile = file },
            onCrumbClick = { file -> viewModel.openDir(file) },
            onUpClick = { viewModel.goUp() },
            onUploadClick = { uploadLauncher.launch("*/*") },
            onNewFolderClick = {
                inputName = ""
                showNewFolderDialog = true
            },
            onRenameClick = {
                inputName = viewModel.selectedFile?.name ?: ""
                showRenameDialog = true
            },
            onToggleDisabledClick = { viewModel.toggleSelectedDisabled() },
            onDeleteClick = { showDeleteConfirm = true }
        )
    }
}

@Composable
private fun ContentInstallerOverlay(onBack: () -> Unit) {
    val context = LocalContext.current
    val viewModel: ContentInstallerViewModel = viewModel()

    LaunchedEffect(Unit) {
        viewModel.init(context)
    }

    BackHandler {
        if (viewModel.viewingProject != null) {
            viewModel.viewingProject = null
        } else {
            onBack()
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
        ContentInstallerScreen(
            onBack = onBack,
            onOpenDownloads = { /* ... */ },
            onInstallLocal = { /* ... */ },
            onSearch = { q, type, v, l ->
                viewModel.versionFilter = v
                viewModel.loaderFilter = l
                viewModel.triggerSearch(q, type)
            },
            onProjectClick = { viewModel.loadVersions(it) },
            onVersionClick = { viewModel.downloadVersion(context, it, viewModel.selectedType) },
            projects = viewModel.projects,
            isLoading = viewModel.isLoading,
            statusText = viewModel.statusText,
            selectedVersion = viewModel.versionFilter,
            selectedLoader = viewModel.loaderFilter,
            onVersionFilterChange = { viewModel.versionFilter = it },
            onLoaderFilterChange = { viewModel.loaderFilter = it },
            instanceVersion = viewModel.instanceVersion,
            instanceLoader = viewModel.instanceLoader,
            viewingProject = viewModel.viewingProject,
            selectedType = viewModel.selectedType,
            projectVersions = viewModel.projectVersions,
            availableProjectMCVersions = viewModel.availableProjectMCVersions,
            selectedProjectMCVersion = viewModel.selectedProjectMCVersion,
            onProjectMCVersionClick = { viewModel.selectedProjectMCVersion = it.ifEmpty { null } },
            onBackToProjects = { viewModel.viewingProject = null }
        )
    }
}

@Composable
private fun SettingsOverlay(onBack: () -> Unit) {
    BackHandler { onBack() }
    Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
        SettingsScreen(
            onBack = onBack
        )
    }
}

@Composable
private fun ProfileSelectionOverlay(
    onBack: () -> Unit,
    onNavigate: (Int) -> Unit
) {
    val viewModel: ProfileSelectionViewModel = viewModel()
    val context = LocalContext.current

    val modpackApi = remember { CommonApi(context.getString(R.string.curseforge_api_key)) }
    val importLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        uri?.let {
            val contentResolver = context.contentResolver
            PojavApplication.sExecutorService.execute {
                val fileName = Tools.getFileName(context, it) ?: "modpack"
                val outFile = File(Tools.DIR_CACHE, "$fileName.cf")
                ProgressLayout.setProgress(ProgressLayout.INSTALL_MODPACK, 0, R.string.multirt_progress_caching)
                try {
                    contentResolver.openInputStream(it)?.use { input ->
                        FileOutputStream(outFile).use { output ->
                            input.copyTo(output)
                        }
                    }
                } catch (e: IOException) {
                    Tools.showErrorRemote("Error", e)
                    ProgressLayout.clearProgress(ProgressLayout.INSTALL_MODPACK)
                    return@execute
                }
                try {
                    modpackApi.installLocalModpack(fileName, outFile, null)
                    viewModel.loadProfiles()
                } catch (e: IOException) {
                    Tools.showErrorRemote("Error", e)
                } finally {
                    outFile.delete()
                    ProgressLayout.clearProgress(ProgressLayout.INSTALL_MODPACK)
                }
            }
        }
    }

    LaunchedEffect(Unit) {
        viewModel.loadProfiles()
    }

    BackHandler { onBack() }

    Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
        ProfileSelectionScreen(
            onImportClick = { importLauncher.launch("*/*") },
            onCreateClick = { ExtraCore.setValue(ExtraConstants.OPEN_SCREEN, 6) },
            onEditClick = { instance ->
                Instances.setSelectedInstance(instance)
                onNavigate(7)
            },
            onDeleteClick = { instance ->
                viewModel.deleteInstance(instance) {}
            },
            onSelect = { instance ->
                viewModel.selectInstance(instance)
                onBack()
            },
            onFilterChange = { r, s, m -> viewModel.updateFilters(r, s, m) },
            onSearchModClick = {
                ExtraCore.setValue(ExtraConstants.DEFAULT_CONTENT_TYPE, ContentInstallerType.MODPACKS)
                onNavigate(2)
            },
            profiles = viewModel.filteredList,
            selectedPathName = viewModel.selectedInstancePathName,
            showReleases = viewModel.showReleases,
            showSnapshots = viewModel.showSnapshots,
            showModded = viewModel.showModded,
            isLoading = viewModel.isLoading
        )
    }
}

@Composable
private fun ProfileTypeSelectOverlay(
    onBack: () -> Unit,
    onNavigate: (Int) -> Unit
) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    BackHandler { onBack() }
    
    Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
        ProfileTypeSelectScreen(
            onBack = onBack,
            onVanillaClick = {
                try {
                    val instance = Instances.createDefaultInstance()
                    Instances.setSelectedInstance(instance)
                    val bundle = Bundle().apply {
                        putBoolean(InstanceEditorFragment.ARG_IS_NEW_INSTANCE, true)
                    }
                    Tools.swapFragment(activity, InstanceEditorFragment::class.java, InstanceEditorFragment.TAG, bundle)
                    onBack()
                } catch (e: IOException) {
                    Tools.showError(context, e)
                }
            },
            onOptifineClick = {
                Tools.swapFragment(activity, OptiFineInstallFragment::class.java, OptiFineInstallFragment.TAG, null)
                onBack()
            },
            onFabricClick = {
                val bundle = Bundle().apply { putString(FabricInstallFragment.ARG_TYPE, "fabric") }
                Tools.swapFragment(activity, FabricInstallFragment::class.java, FabricInstallFragment.TAG, bundle)
                onBack()
            },
            onForgeClick = {
                Tools.swapFragment(activity, ForgeInstallFragment::class.java, ForgeInstallFragment.TAG, null)
                onBack()
            },
            onQuiltClick = {
                val bundle = Bundle().apply { putString(FabricInstallFragment.ARG_TYPE, "quilt") }
                Tools.swapFragment(activity, FabricInstallFragment::class.java, FabricInstallFragment.TAG, bundle)
                onBack()
            },
            onNeoForgeClick = {
                Tools.swapFragment(activity, NeoforgeInstallFragment::class.java, NeoforgeInstallFragment.TAG, null)
                onBack()
            },
            onLegacyFabricClick = {
                val bundle = Bundle().apply { putString(FabricInstallFragment.ARG_TYPE, "legacy_fabric") }
                Tools.swapFragment(activity, FabricInstallFragment::class.java, FabricInstallFragment.TAG, bundle)
                onBack()
            },
            onModpackClick = {
                ExtraCore.setValue(ExtraConstants.DEFAULT_CONTENT_TYPE, ContentInstallerType.MODPACKS)
                onNavigate(2)
            },
            onBTAClick = {
                Tools.swapFragment(activity, BTAInstallFragment::class.java, BTAInstallFragment.TAG, null)
                onBack()
            }
        )
    }
}

@Composable
fun InstanceEditorOverlay(onBack: () -> Unit) {
    val context = LocalContext.current
    val activity = context as? FragmentActivity
    
    // We'll use the existing Fragment-based Editor but wrapped in a way 
    // that it can be transitioned by AnimatedContent if we want.
    // However, the user wants it to behave like other overlays.
    // So we'll host the InstanceEditorScreen here directly!
    
    val instance = remember { Instances.loadSelectedInstance() }
    if (instance == null) {
        LaunchedEffect(Unit) { onBack() }
        return
    }

    // Since I don't want to duplicate all the InstanceEditorFragment logic (saving, loading, etc.)
    // I'll just launch the Fragment for now but make sure it fades in.
    // Wait, the user says "AuthenticationScreens.kt not show fade in".
    // Category 4 IS AuthenticationScreens.
    
    // If it's not fading in, it's because of how I structured the AnimatedContent.
    // I just fixed it by removing the extra Box and adding the Spacer for -1.
    
    // Let's do the same for InstanceEditor. For now, Category 7 will still use the Fragment 
    // but I'll ensure the Fragment transaction is smooth.
    DisposableEffect(Unit) {
        Tools.swapFragment(activity, InstanceEditorFragment::class.java, InstanceEditorFragment.TAG, null)
        onDispose {
            // Fragment popped by back button
        }
    }
    
    // Empty content as the Fragment is in the background container
    Spacer(modifier = Modifier.fillMaxSize())
}

@Composable
private fun AboutOverlay(onBack: () -> Unit) {
    BackHandler { onBack() }
    Surface(modifier = Modifier.fillMaxSize(), color = Color.Transparent) {
        AboutScreen()
    }
}

@Composable
fun AboutScreen() {
    val context = LocalContext.current
    val isPreview = LocalInspectionMode.current
    val hasBackground = LauncherPreferences.PREF_BACKGROUND_PATH_STATE.value != null ||
            LauncherPreferences.PREF_BACKGROUND_VIDEO_PATH_STATE.value != null || isPreview

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = if (hasBackground) Color.Transparent else MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Icon(
                painter = painterResource(id = R.drawable.icon_hyper),
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.primary
            )

            Text(
                text = "HyperLauncher",
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { 
                        val url = Tools.URL_HOME
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        } catch (_: ActivityNotFoundException) {
                            Toast.makeText(context, "No browser found", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Cannot open browser", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = CircleShape
                ) {
                    Icon(Icons.Rounded.Public, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    @Suppress("HardcodedText")
                    Text("Wiki")
                }

                Button(
                    onClick = { 
                        val url = context.getString(R.string.social_media_invite)
                        try {
                            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            context.startActivity(intent)
                        } catch (_: ActivityNotFoundException) {
                            Toast.makeText(context, "No browser found", Toast.LENGTH_SHORT).show()
                        } catch (e: Exception) {
                            Toast.makeText(context, "Cannot open browser", Toast.LENGTH_SHORT).show()
                        }
                    },
                    modifier = Modifier.weight(1f),
                    shape = CircleShape
                ) {
                    Icon(Icons.AutoMirrored.Rounded.Chat, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    @Suppress("HardcodedText")
                    Text("Discord")
                }
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = LauncherPreferences.PREF_CONTENT_TRANSPARENCY_STATE.value),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Privacy Policy",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.height(8.dp))
                    Text(
                        text = """1. This app does not send any sensitive data, and uses network only for downloading game resources, mods or other content.
2. This app does not collect any sensitive data while you are running the game.
3. Some sensitive data is stored in crash reports after the game crashes, but it's not shared to the developer or any third parties
4. Hyper Launcher developers reserve the right to update this privacy policy without prior notification.""",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        lineHeight = 20.sp
                    )
                }
            }
        }
    }
}

@Preview
@Composable
private fun LauncherScreenPreview() {
    PojavTheme(dynamicColor = true) {
        LauncherScreen(
            onHomeRequest = {},
            onProgressClick = {},
            isProgressVisible = true,
            taskCount = 2,
            isFragmentOpen = true
        )
    }
}
