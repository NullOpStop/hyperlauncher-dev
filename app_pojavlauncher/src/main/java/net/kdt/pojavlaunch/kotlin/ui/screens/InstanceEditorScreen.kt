package net.kdt.pojavlaunch.ui.screens

import android.graphics.drawable.Drawable
import androidx.activity.compose.BackHandler
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.BitmapPainter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.BaseActivity
import net.kdt.pojavlaunch.modloaders.ComparableVersionString
import net.kdt.pojavlaunch.ui.utils.AnimationUtils
import net.kdt.pojavlaunch.multirt.Runtime
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.ui.components.*

enum class InstanceEditorPage(val title: String, val iconRes: Int) {
    GENERAL("General", R.drawable.ic_px_settings),
    JAVA("Java Settings", R.drawable.ic_px_java),
    RENDERING("Rendering", R.drawable.ic_px_image_renderer),
    FOLDERS("Instance Folders", R.drawable.ic_px_folder),
    DANGER("Danger Zone", R.drawable.ic_px_trash)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InstanceEditorScreen(
    onBack: () -> Unit,
    onSave: () -> Unit,
    onDelete: () -> Unit,
    onIconClick: () -> Unit,
    onVersionClick: () -> Unit,
    onControlClick: () -> Unit,
    onCustomDirectoryClick: () -> Unit,
    onOpenLogs: () -> Unit,
    onOpenConfig: () -> Unit,
    onOpenMods: () -> Unit,
    onOpenShaderPacks: () -> Unit,
    onOpenResourcePacks: () -> Unit,

    instanceIcon: Drawable?,
    name: String,
    onNameChange: (String) -> Unit,
    versionId: String,
    controlLayout: String,
    jvmArgs: String,
    onJvmArgsChange: (String) -> Unit,
    argsMode: Int,
    onArgsModeChange: (Int) -> Unit,
    sharedData: Boolean,
    onSharedDataChange: (Boolean) -> Unit,
    customDirectory: String,

    availableRuntimes: List<Runtime>,
    selectedRuntime: Runtime?,
    onRuntimeSelected: (Runtime) -> Unit,

    rendererDisplayNames: List<String>,
    selectedRendererIndex: Int,
    onRendererSelected: (Int) -> Unit,

    preferredBackend: String,
    onPreferredBackendChange: (String) -> Unit,

    isNewInstance: Boolean = false
) {
    val isPreview = LocalInspectionMode.current
    var currentPage by rememberSaveable { mutableStateOf(InstanceEditorPage.GENERAL) }
    var isMainPage by rememberSaveable { mutableStateOf(false) }
    val railScrollState = rememberScrollState()

    val animPreset = LauncherPreferences.PREF_TRANSITION_ANIMATION_STATE.value
    val animDuration = LauncherPreferences.PREF_TRANSITION_DURATION_STATE.intValue
    val animIntensity = LauncherPreferences.PREF_TRANSITION_INTENSITY_STATE.value

    val transitionSpec = AnimationUtils.getTransitionSpec()

    // Dirty state tracking for the floating save button
    val initialName = remember { name }
    val initialVersionId = remember { versionId }
    val initialControlLayout = remember { controlLayout }
    val initialJvmArgs = remember { jvmArgs }
    val initialArgsMode = remember { argsMode }
    val initialSharedData = remember { sharedData }
    val initialCustomDirectory = remember { customDirectory }
    val initialRuntimeName = remember { selectedRuntime?.name }
    val initialRendererIndex = remember { selectedRendererIndex }
    val initialPreferredBackend = remember { preferredBackend }

    val isDirty = name != initialName ||
            versionId != initialVersionId ||
            controlLayout != initialControlLayout ||
            jvmArgs != initialJvmArgs ||
            argsMode != initialArgsMode ||
            sharedData != initialSharedData ||
            customDirectory != initialCustomDirectory ||
            selectedRuntime?.name != initialRuntimeName ||
            selectedRendererIndex != initialRendererIndex ||
            preferredBackend != initialPreferredBackend

    val hasBackground = LauncherPreferences.PREF_BACKGROUND_PATH_STATE.value != null || 
                        LauncherPreferences.PREF_BACKGROUND_VIDEO_PATH_STATE.value != null || isPreview
    val backgroundBitmap = if (isPreview) BaseActivity.getBackgroundBitmap() else null

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = if (hasBackground) Color.Transparent else MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isPreview) {
                if (backgroundBitmap != null) {
                    Image(
                        bitmap = backgroundBitmap.asImageBitmap(),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background))
                }
                Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = if (hasBackground) 0.4f else 0f)))
            }

            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val isWide = maxWidth > 600.dp

                if (isWide) {
                    Row(modifier = Modifier.fillMaxSize()) {
                        NavigationRail(
                            modifier = Modifier.fillMaxHeight().width(80.dp),
                            containerColor = Color.Transparent,
                            contentColor = MaterialTheme.colorScheme.onSurface,
                            windowInsets = WindowInsets(0, 0, 0, 0)
                        ) {
                            Spacer(modifier = Modifier.height(12.dp))
                            Column(
                                modifier = Modifier
                                    .fillMaxHeight()
                                    .verticalScroll(railScrollState),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                InstanceEditorPage.entries.forEach { page ->
                                    NavigationRailItem(
                                        selected = currentPage == page && !isMainPage,
                                        onClick = {
                                            currentPage = page
                                            isMainPage = false
                                        },
                                        icon = { Icon(painterResource(page.iconRes), contentDescription = null, modifier = Modifier.size(24.dp)) },
                                        label = { Text(page.title.substringBefore(" "), fontSize = 10.sp) },
                                        alwaysShowLabel = true,
                                        colors = NavigationRailItemDefaults.colors(
                                            selectedIconColor = MaterialTheme.colorScheme.onPrimaryContainer,
                                            indicatorColor = MaterialTheme.colorScheme.primaryContainer,
                                            unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                                            selectedTextColor = MaterialTheme.colorScheme.primary,
                                            unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                                        )
                                    )
                                }
                            }
                        }

                        VerticalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))

                        Scaffold(
                            containerColor = Color.Transparent,
                            floatingActionButton = {
                                AnimatedVisibility(
                                    visible = isNewInstance || isDirty,
                                    enter = fadeIn(),
                                    exit = fadeOut()
                                ) {
                                    FloatingActionButton(
                                        onClick = onSave,
                                        containerColor = MaterialTheme.colorScheme.primary,
                                        contentColor = MaterialTheme.colorScheme.onPrimary,
                                        shape = CircleShape
                                    ) {
                                        Icon(Icons.Default.Done, contentDescription = "Save")
                                    }
                                }
                            }
                        ) { padding ->
                            Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                                AnimatedContent(
                                    targetState = currentPage,
                                    transitionSpec = transitionSpec,
                                    label = "instance_editor_content_anim"
                                ) { page ->
                                    InstanceEditorContent(
                                        page = page,
                                        instanceIcon = instanceIcon,
                                        name = name,
                                        onNameChange = onNameChange,
                                        versionId = versionId,
                                        controlLayout = controlLayout,
                                        jvmArgs = jvmArgs,
                                        onJvmArgsChange = onJvmArgsChange,
                                        argsMode = argsMode,
                                        onArgsModeChange = onArgsModeChange,
                                        sharedData = sharedData,
                                        onSharedDataChange = onSharedDataChange,
                                        customDirectory = customDirectory,
                                        availableRuntimes = availableRuntimes,
                                        selectedRuntime = selectedRuntime,
                                        onRuntimeSelected = onRuntimeSelected,
                                        rendererDisplayNames = rendererDisplayNames,
                                        selectedRendererIndex = selectedRendererIndex,
                                        onRendererSelected = onRendererSelected,
                                        preferredBackend = preferredBackend,
                                        onPreferredBackendChange = onPreferredBackendChange,
                                        onIconClick = onIconClick,
                                        onVersionClick = onVersionClick,
                                        onControlClick = onControlClick,
                                        onCustomDirectoryClick = onCustomDirectoryClick,
                                        onDelete = onDelete,
                                        onOpenLogs = onOpenLogs,
                                        onOpenConfig = onOpenConfig,
                                        onOpenMods = onOpenMods,
                                        onOpenShaderPacks = onOpenShaderPacks,
                                        onOpenResourcePacks = onOpenResourcePacks
                                    )
                                }
                            }
                        }
                    }
                } else {
                    AnimatedContent(
                        targetState = isMainPage,
                        transitionSpec = transitionSpec,
                        label = "instance_editor_nav_anim"
                    ) { mainPage ->
                        if (mainPage) {
                            Scaffold(
                                containerColor = Color.Transparent,
                                topBar = {
                                    @Suppress("DEPRECATION")
                                    TopAppBar(
                                        title = { Text("Edit Instance", fontWeight = FontWeight.Bold) },
                                        navigationIcon = {
                                            IconButton(onClick = onBack) {
                                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                                            }
                                        },
                                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                                    )
                                },
                                floatingActionButton = {
                                    AnimatedVisibility(
                                        visible = isNewInstance || isDirty,
                                        enter = fadeIn(),
                                        exit = fadeOut()
                                    ) {
                                        FloatingActionButton(
                                            onClick = onSave,
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary,
                                            shape = CircleShape
                                        ) {
                                            Icon(Icons.Default.Done, contentDescription = "Save")
                                        }
                                    }
                                }
                            ) { padding ->
                                BackHandler { onBack() }
                                LazyColumn(modifier = Modifier.padding(padding).fillMaxSize()) {
                                    item {
                                        PreferenceGroup(title = "Categories") {
                                            InstanceEditorPage.entries.forEach { page ->
                                                PreferenceItem(
                                                    title = page.title,
                                                    icon = painterResource(page.iconRes),
                                                    onClick = {
                                                        currentPage = page
                                                        isMainPage = false
                                                    }
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        } else {
                            @Suppress("DEPRECATION")
                            Scaffold(
                                containerColor = Color.Transparent,
                                topBar = {
                                    @Suppress("DEPRECATION")
                                    TopAppBar(
                                        title = { Text(currentPage.title, fontWeight = FontWeight.Bold) },
                                        navigationIcon = {
                                            // Back button removed from category name as requested
                                        },
                                        colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                                    )
                                },
                                floatingActionButton = {
                                    AnimatedVisibility(
                                        visible = isNewInstance || isDirty,
                                        enter = fadeIn(),
                                        exit = fadeOut()
                                    ) {
                                        FloatingActionButton(
                                            onClick = onSave,
                                            containerColor = MaterialTheme.colorScheme.primary,
                                            contentColor = MaterialTheme.colorScheme.onPrimary,
                                            shape = CircleShape
                                        ) {
                                            Icon(Icons.Default.Done, contentDescription = "Save")
                                        }
                                    }
                                }
                            ) { padding ->
                                BackHandler { isMainPage = true }
                                Box(modifier = Modifier.padding(padding).fillMaxSize()) {
                                    InstanceEditorContent(
                                        page = currentPage,
                                        instanceIcon = instanceIcon,
                                        name = name,
                                        onNameChange = onNameChange,
                                        versionId = versionId,
                                        controlLayout = controlLayout,
                                        jvmArgs = jvmArgs,
                                        onJvmArgsChange = onJvmArgsChange,
                                        argsMode = argsMode,
                                        onArgsModeChange = onArgsModeChange,
                                        sharedData = sharedData,
                                        onSharedDataChange = onSharedDataChange,
                                        customDirectory = customDirectory,
                                        availableRuntimes = availableRuntimes,
                                        selectedRuntime = selectedRuntime,
                                        onRuntimeSelected = onRuntimeSelected,
                                        rendererDisplayNames = rendererDisplayNames,
                                        selectedRendererIndex = selectedRendererIndex,
                                        onRendererSelected = onRendererSelected,
                                        preferredBackend = preferredBackend,
                                        onPreferredBackendChange = onPreferredBackendChange,
                                        onIconClick = onIconClick,
                                        onVersionClick = onVersionClick,
                                        onControlClick = onControlClick,
                                        onCustomDirectoryClick = onCustomDirectoryClick,
                                        onDelete = onDelete,
                                        onOpenLogs = onOpenLogs,
                                        onOpenConfig = onOpenConfig,
                                        onOpenMods = onOpenMods,
                                        onOpenShaderPacks = onOpenShaderPacks,
                                        onOpenResourcePacks = onOpenResourcePacks
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
fun InstanceEditorContent(
    page: InstanceEditorPage,
    instanceIcon: Drawable?,
    name: String,
    onNameChange: (String) -> Unit,
    versionId: String,
    controlLayout: String,
    jvmArgs: String,
    onJvmArgsChange: (String) -> Unit,
    argsMode: Int,
    onArgsModeChange: (Int) -> Unit,
    sharedData: Boolean,
    onSharedDataChange: (Boolean) -> Unit,
    customDirectory: String,
    availableRuntimes: List<Runtime>,
    selectedRuntime: Runtime?,
    onRuntimeSelected: (Runtime) -> Unit,
    rendererDisplayNames: List<String>,
    selectedRendererIndex: Int,
    onRendererSelected: (Int) -> Unit,
    preferredBackend: String,
    onPreferredBackendChange: (String) -> Unit,
    onIconClick: () -> Unit,
    onVersionClick: () -> Unit,
    onControlClick: () -> Unit,
    onCustomDirectoryClick: () -> Unit,
    onDelete: () -> Unit,
    onOpenLogs: () -> Unit,
    onOpenConfig: () -> Unit,
    onOpenMods: () -> Unit,
    onOpenShaderPacks: () -> Unit,
    onOpenResourcePacks: () -> Unit
) {
    when (page) {
        InstanceEditorPage.GENERAL -> GeneralSettings(
            instanceIcon = instanceIcon,
            name = name,
            onNameChange = onNameChange,
            versionId = versionId,
            onVersionClick = onVersionClick,
            customDirectory = customDirectory,
            onCustomDirectoryClick = onCustomDirectoryClick,
            sharedData = sharedData,
            onSharedDataChange = onSharedDataChange,
            onIconClick = onIconClick
        )
        InstanceEditorPage.JAVA -> JavaSettings(
            jvmArgs = jvmArgs,
            onJvmArgsChange = onJvmArgsChange,
            argsMode = argsMode,
            onArgsModeChange = onArgsModeChange,
            availableRuntimes = availableRuntimes,
            selectedRuntime = selectedRuntime,
            onRuntimeSelected = onRuntimeSelected
        )
        InstanceEditorPage.RENDERING -> RenderingSettings(
            versionId = versionId,
            rendererDisplayNames = rendererDisplayNames,
            selectedRendererIndex = selectedRendererIndex,
            onRendererSelected = onRendererSelected,
            preferredBackend = preferredBackend,
            onPreferredBackendChange = onPreferredBackendChange
        )
        InstanceEditorPage.FOLDERS -> FolderShortcuts(
            onOpenLogs = onOpenLogs,
            onOpenConfig = onOpenConfig,
            onOpenMods = onOpenMods,
            onOpenShaderPacks = onOpenShaderPacks,
            onOpenResourcePacks = onOpenResourcePacks
        )
        InstanceEditorPage.DANGER -> DangerSettings(
            controlLayout = controlLayout,
            onControlClick = onControlClick,
            onDelete = onDelete
        )
    }
}

@Composable
fun FolderShortcuts(
    onOpenLogs: () -> Unit,
    onOpenConfig: () -> Unit,
    onOpenMods: () -> Unit,
    onOpenShaderPacks: () -> Unit,
    onOpenResourcePacks: () -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(vertical = 12.dp)) {
        item {
            PreferenceGroup(title = "Files and Folders") {
                PreferenceItem(
                    title = "Logs Folder",
                    summary = "Open instance logs directory",
                    icon = painterResource(id = R.drawable.ic_px_console),
                    onClick = onOpenLogs
                )
                PreferenceItem(
                    title = "Config Folder",
                    summary = "Open configuration files directory",
                    icon = painterResource(id = R.drawable.ic_px_settings),
                    onClick = onOpenConfig
                )
                PreferenceItem(
                    title = "Mods Folder",
                    summary = "Open installed mods directory",
                    icon = painterResource(id = R.drawable.ic_package),
                    onClick = onOpenMods
                )
                PreferenceItem(
                    title = "Resource Packs",
                    summary = "Open resource packs directory",
                    icon = painterResource(id = R.drawable.ic_px_file),
                    onClick = onOpenResourcePacks
                )
                PreferenceItem(
                    title = "Shader Packs",
                    summary = "Open shader packs directory",
                    icon = painterResource(id = R.drawable.ic_px_image),
                    onClick = onOpenShaderPacks
                )
            }
        }
    }
}

@Composable
fun GeneralSettings(
    instanceIcon: Drawable?,
    name: String,
    onNameChange: (String) -> Unit,
    versionId: String,
    onVersionClick: () -> Unit,
    customDirectory: String,
    onCustomDirectoryClick: () -> Unit,
    sharedData: Boolean,
    onSharedDataChange: (Boolean) -> Unit,
    onIconClick: () -> Unit
) {
    var showNameDialog by remember { mutableStateOf(false) }
    if (showNameDialog) {
        var tempName by remember { mutableStateOf(name) }
        AlertDialog(
            onDismissRequest = { showNameDialog = false },
            title = { Text("Instance Name") },
            text = {
                OutlinedTextField(
                    value = tempName,
                    onValueChange = { tempName = it },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                @Suppress("DEPRECATION")
                TextButton(onClick = {
                    onNameChange(tempName)
                    showNameDialog = false
                }) { Text("OK") }
            },
            dismissButton = {
                @Suppress("DEPRECATION")
                TextButton(onClick = { showNameDialog = false }) { Text("Cancel") }
            }
        )
    }

    LazyColumn(contentPadding = PaddingValues(vertical = 12.dp)) {
        item {
            PreferenceGroup(title = "Profile") {
                val iconPainter = if (instanceIcon != null) {
                    val bitmap = remember(instanceIcon) { instanceIcon.toBitmap().asImageBitmap() }
                    BitmapPainter(bitmap)
                } else {
                    painterResource(id = R.drawable.ic_px_java)
                }

                PreferenceItem(
                    title = "Change Icon",
                    summary = "Select a custom image for this instance",
                    icon = iconPainter,
                    onClick = onIconClick
                )

                PreferenceItem(
                    title = "Instance Name",
                    summary = name.ifEmpty { stringResource(id = R.string.unnamed) },
                    icon = painterResource(id = R.drawable.ic_px_edit),
                    onClick = { showNameDialog = true }
                )
            }
        }

        item {
            PreferenceGroup(title = "Structure") {
                PreferenceItem(
                    title = "Version",
                    summary = versionId.ifEmpty { stringResource(id = R.string.version_select_hint) },
                    icon = painterResource(id = R.drawable.ic_px_file),
                    onClick = onVersionClick
                )

                PreferenceItem(
                    title = "Custom Directory",
                    summary = customDirectory.ifEmpty { stringResource(id = R.string.use_global_default) },
                    icon = painterResource(id = R.drawable.ic_px_folder),
                    onClick = onCustomDirectoryClick
                )

                PreferenceSwitch(
                    title = "Shared Data",
                    summary = if (sharedData) stringResource(id = R.string.instance_shared_data_on) else stringResource(id = R.string.instance_shared_data_off),
                    icon = painterResource(id = R.drawable.ic_px_settings),
                    checked = sharedData,
                    onCheckedChange = onSharedDataChange
                )
            }
        }
    }
}

@Composable
fun JavaSettings(
    jvmArgs: String,
    onJvmArgsChange: (String) -> Unit,
    argsMode: Int,
    onArgsModeChange: (Int) -> Unit,
    availableRuntimes: List<Runtime>,
    selectedRuntime: Runtime?,
    onRuntimeSelected: (Runtime) -> Unit
) {
    var showJvmArgsDialog by remember { mutableStateOf(false) }
    if (showJvmArgsDialog) {
        var tempJvmArgs by remember { mutableStateOf(jvmArgs) }
        AlertDialog(
            onDismissRequest = { showJvmArgsDialog = false },
            title = { Text("JVM Arguments") },
            text = {
                OutlinedTextField(
                    value = tempJvmArgs,
                    onValueChange = { tempJvmArgs = it },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                @Suppress("DEPRECATION")
                TextButton(onClick = {
                    onJvmArgsChange(tempJvmArgs)
                    showJvmArgsDialog = false
                }) { Text("OK") }
            },
            dismissButton = {
                @Suppress("DEPRECATION")
                TextButton(onClick = { showJvmArgsDialog = false }) { Text("Cancel") }
            }
        )
    }

    LazyColumn(contentPadding = PaddingValues(vertical = 12.dp)) {
        item {
            PreferenceGroup(title = "Launch Arguments") {
                PreferenceList(
                    title = "Launch Arguments Mode",
                    entries = arrayOf("Replace Global", "Merge (Global First)", "Merge (Instance First)"),
                    entryValues = arrayOf("0", "1", "2"),
                    selectedValue = argsMode.toString(),
                    onValueChange = { onArgsModeChange(it.toInt()) },
                    icon = painterResource(id = R.drawable.ic_px_alt_sliders)
                )

                PreferenceItem(
                    title = "JVM Arguments",
                    summary = jvmArgs.ifEmpty { stringResource(id = R.string.use_global_default) },
                    icon = painterResource(id = R.drawable.ic_px_console),
                    onClick = { showJvmArgsDialog = true }
                )
            }
        }

        item {
            PreferenceGroup(title = "Runtime") {
                PreferenceList(
                    title = "Java Runtime",
                    entries = availableRuntimes.map { it.name }.toTypedArray(),
                    entryValues = availableRuntimes.map { it.name }.toTypedArray(),
                    selectedValue = selectedRuntime?.name ?: "<Default>",
                    onValueChange = { name ->
                        availableRuntimes.find { it.name == name }?.let { onRuntimeSelected(it) }
                    },
                    icon = painterResource(id = R.drawable.ic_px_runtime_mgr)
                )
            }
        }
    }
}

@Composable
fun RenderingSettings(
    versionId: String,
    rendererDisplayNames: List<String>,
    selectedRendererIndex: Int,
    onRendererSelected: (Int) -> Unit,
    preferredBackend: String,
    onPreferredBackendChange: (String) -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(vertical = 12.dp)) {
        item {
            PreferenceGroup(title = "Display") {
                PreferenceList(
                    title = "Renderer",
                    entries = rendererDisplayNames.toTypedArray(),
                    entryValues = rendererDisplayNames.indices.map { it.toString() }.toTypedArray(),
                    selectedValue = if (selectedRendererIndex in rendererDisplayNames.indices) selectedRendererIndex.toString() else "-1",
                    onValueChange = { onRendererSelected(it.toInt()) },
                    icon = painterResource(id = R.drawable.ic_px_image_renderer)
                )

                if (isBackendSupported(versionId)) {
                    PreferenceList(
                        title = stringResource(R.string.mcl_setting_title_preferred_graphics_backend),
                        summary = stringResource(R.string.mcl_setting_subtitle_preferred_graphics_backend),
                        entries = stringArrayResource(R.array.graphics_backend_names),
                        entryValues = stringArrayResource(R.array.graphics_backend_values),
                        selectedValue = preferredBackend,
                        onValueChange = onPreferredBackendChange,
                        icon = painterResource(id = R.drawable.ic_px_image_renderer)
                    )
                }
            }
        }
    }
}

private fun isBackendSupported(versionId: String): Boolean {
    if (versionId.isEmpty() || versionId == "latest_release" || versionId == "latest_snapshot") return true

    val match = Regex("""(\d+\.\d+(\.\d+)?)""").find(versionId)
    if (match != null) {
        val version = match.value
        val cvs = ComparableVersionString.parse(version)
        if (cvs.isValid) {
            // Threshold set to 1.20.2 as it's the logical match for backend features
            return cvs.compareTo(ComparableVersionString.parse("1.20.2")) >= 0
        }
    }
    return true
}

@Composable
fun DangerSettings(
    controlLayout: String,
    onControlClick: () -> Unit,
    onDelete: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Instance") },
            text = { Text("Are you sure you want to delete this instance? This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        onDelete()
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
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancel") }
            }
        )
    }

    LazyColumn(contentPadding = PaddingValues(vertical = 12.dp)) {
        item {
            PreferenceGroup(title = "Controls") {
                PreferenceItem(
                    title = "Control Layout",
                    summary = controlLayout.ifEmpty { stringResource(id = R.string.use_global_default) },
                    icon = painterResource(id = R.drawable.ic_px_gamepad),
                    onClick = onControlClick
                )
            }
        }

        item {
            PreferenceGroup(title = "Danger Zone") {
                PreferenceItem(
                    title = "Delete Instance",
                    summary = "Permanently remove this instance and all its files",
                    icon = painterResource(id = R.drawable.ic_px_trash),
                    onClick = { showDeleteConfirm = true }
                )
            }
        }
    }
}
