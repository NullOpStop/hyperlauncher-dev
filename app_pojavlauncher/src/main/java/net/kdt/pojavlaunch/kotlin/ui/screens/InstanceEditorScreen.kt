package net.kdt.pojavlaunch.ui.screens

import android.graphics.drawable.Drawable
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons

import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
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

enum class InstanceEditorPage(val title: String, val imageVector: ImageVector) {
    GENERAL("General", Icons.Default.Settings),
    CONFIGURATION("Configuration", Icons.Default.Code)
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

            Scaffold(
                containerColor = Color.Transparent,
                topBar = {
                    TabRow(
                        selectedTabIndex = currentPage.ordinal,
                        containerColor = Color.Transparent,
                        contentColor = MaterialTheme.colorScheme.primary,
                        indicator = { tabPositions ->
                            if (currentPage.ordinal < tabPositions.size) {
                                TabRowDefaults.SecondaryIndicator(
                                    modifier = Modifier.tabIndicatorOffset(tabPositions[currentPage.ordinal]),
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }
                        },
                        divider = {
                            HorizontalDivider(
                                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f)
                            )
                        },
                        modifier = Modifier.padding(horizontal = 16.dp).height(52.dp)
                    ) {
                        InstanceEditorPage.entries.forEach { page ->
                            Tab(
                                selected = currentPage == page,
                                onClick = { currentPage = page },
                                text = { Text(page.title, fontSize = 12.sp, fontWeight = FontWeight.Bold) },
                                icon = { Icon(page.imageVector, contentDescription = null, modifier = Modifier.size(18.dp)) }
                            )
                        }
                    }
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
            onIconClick = onIconClick,
            onOpenLogs = onOpenLogs,
            onOpenConfig = onOpenConfig,
            onOpenMods = onOpenMods,
            onOpenShaderPacks = onOpenShaderPacks,
            onOpenResourcePacks = onOpenResourcePacks
        )
        InstanceEditorPage.CONFIGURATION -> ConfigurationSettings(
            jvmArgs = jvmArgs,
            onJvmArgsChange = onJvmArgsChange,
            argsMode = argsMode,
            onArgsModeChange = onArgsModeChange,
            availableRuntimes = availableRuntimes,
            selectedRuntime = selectedRuntime,
            onRuntimeSelected = onRuntimeSelected,
            versionId = versionId,
            rendererDisplayNames = rendererDisplayNames,
            selectedRendererIndex = selectedRendererIndex,
            onRendererSelected = onRendererSelected,
            preferredBackend = preferredBackend,
            onPreferredBackendChange = onPreferredBackendChange,
            controlLayout = controlLayout,
            onControlClick = onControlClick,
            onDelete = onDelete
        )
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun FolderShortcuts(
    onOpenLogs: () -> Unit,
    onOpenConfig: () -> Unit,
    onOpenMods: () -> Unit,
    onOpenShaderPacks: () -> Unit,
    onOpenResourcePacks: () -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        OutlinedButton(onClick = onOpenLogs, shape = CircleShape, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
            Icon(Icons.Filled.Terminal, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Logs", fontSize = 12.sp)
        }
        OutlinedButton(onClick = onOpenConfig, shape = CircleShape, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
            Icon(Icons.Default.Settings, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Config", fontSize = 12.sp)
        }
        OutlinedButton(onClick = onOpenMods, shape = CircleShape, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
            Icon(Icons.Default.Extension, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Mods", fontSize = 12.sp)
        }
        OutlinedButton(onClick = onOpenResourcePacks, shape = CircleShape, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
            Icon(Icons.Default.Description, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Resources", fontSize = 12.sp)
        }
        OutlinedButton(onClick = onOpenShaderPacks, shape = CircleShape, contentPadding = PaddingValues(horizontal = 12.dp, vertical = 4.dp)) {
            Icon(Icons.Default.Image, null, modifier = Modifier.size(18.dp))
            Spacer(Modifier.width(8.dp))
            Text("Shaders", fontSize = 12.sp)
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
    onIconClick: () -> Unit,
    onOpenLogs: () -> Unit,
    onOpenConfig: () -> Unit,
    onOpenMods: () -> Unit,
    onOpenShaderPacks: () -> Unit,
    onOpenResourcePacks: () -> Unit
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
            Column(
                modifier = Modifier.fillMaxWidth().padding(bottom = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .clip(RoundedCornerShape(28.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f))
                        .clickable { onIconClick() },
                    contentAlignment = Alignment.Center
                ) {
                    if (instanceIcon != null) {
                        Image(
                            bitmap = instanceIcon.toBitmap().asImageBitmap(),
                            contentDescription = null,
                            modifier = Modifier.size(120.dp).clip(RoundedCornerShape(28.dp)),
                            contentScale = ContentScale.Crop
                        )
                    } else {
                        Icon(
                            imageVector = Icons.Default.Code,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                    Surface(
                        modifier = Modifier.align(Alignment.BottomEnd).padding(4.dp),
                        shape = CircleShape,
                        color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    ) {
                        Icon(Icons.Default.Edit, null, modifier = Modifier.padding(6.dp).size(16.dp))
                    }
                }
                
                Spacer(Modifier.height(12.dp))
                
                @Suppress("DEPRECATION")
                Text(
                    text = name.ifEmpty { stringResource(id = R.string.unnamed) },
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.clickable { showNameDialog = true }
                )
            }
        }

        item {
            FolderShortcuts(
                onOpenLogs = onOpenLogs,
                onOpenConfig = onOpenConfig,
                onOpenMods = onOpenMods,
                onOpenShaderPacks = onOpenShaderPacks,
                onOpenResourcePacks = onOpenResourcePacks
            )
        }

        item {
            PreferenceGroup(title = "Structure") {
                PreferenceItem(
                    title = "Version",
                    summary = versionId.ifEmpty { stringResource(id = R.string.version_select_hint) },
                    imageVector = Icons.Default.Description,
                    onClick = onVersionClick
                )

                PreferenceItem(
                    title = "Custom Directory",
                    summary = customDirectory.ifEmpty { stringResource(id = R.string.use_global_default) },
                    imageVector = Icons.Default.Folder,
                    onClick = onCustomDirectoryClick
                )

                PreferenceSwitch(
                    title = "Shared Data",
                    summary = if (sharedData) stringResource(id = R.string.instance_shared_data_on) else stringResource(id = R.string.instance_shared_data_off),
                    imageVector = Icons.Default.Settings,
                    checked = sharedData,
                    onCheckedChange = onSharedDataChange
                )
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ConfigurationSettings(
    jvmArgs: String,
    onJvmArgsChange: (String) -> Unit,
    argsMode: Int,
    onArgsModeChange: (Int) -> Unit,
    availableRuntimes: List<Runtime>,
    selectedRuntime: Runtime?,
    onRuntimeSelected: (Runtime) -> Unit,
    versionId: String,
    rendererDisplayNames: List<String>,
    selectedRendererIndex: Int,
    onRendererSelected: (Int) -> Unit,
    preferredBackend: String,
    onPreferredBackendChange: (String) -> Unit,
    controlLayout: String,
    onControlClick: () -> Unit,
    onDelete: () -> Unit
) {
    LazyColumn(contentPadding = PaddingValues(vertical = 12.dp)) {
        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = 2
            ) {
                Box(modifier = Modifier.weight(1f).padding(vertical = 4.dp)) {
                    PreferenceGroup(title = "Java Settings") {
                        JavaSettingsItems(
                            jvmArgs = jvmArgs,
                            onJvmArgsChange = onJvmArgsChange,
                            argsMode = argsMode,
                            onArgsModeChange = onArgsModeChange,
                            availableRuntimes = availableRuntimes,
                            selectedRuntime = selectedRuntime,
                            onRuntimeSelected = onRuntimeSelected
                        )
                    }
                }

                Box(modifier = Modifier.weight(1f).padding(vertical = 4.dp)) {
                    PreferenceGroup(title = "Rendering") {
                        RenderingSettingsItems(
                            versionId = versionId,
                            rendererDisplayNames = rendererDisplayNames,
                            selectedRendererIndex = selectedRendererIndex,
                            onRendererSelected = onRendererSelected,
                            preferredBackend = preferredBackend,
                            onPreferredBackendChange = onPreferredBackendChange
                        )
                    }
                }
            }
        }

        item {
            FlowRow(
                modifier = Modifier.fillMaxWidth().padding(horizontal = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                maxItemsInEachRow = 2
            ) {
                Box(modifier = Modifier.weight(1f).padding(vertical = 4.dp)) {
                    PreferenceGroup(title = "Controls") {
                PreferenceItem(
                    title = "Control Layout",
                    summary = controlLayout.ifEmpty { stringResource(id = R.string.use_global_default) },
                    imageVector = Icons.Default.Gamepad,
                    onClick = onControlClick
                )
            }
                }

                Box(modifier = Modifier.weight(1f).padding(vertical = 4.dp)) {
                    PreferenceGroup(title = "Danger Zone") {
                        DangerSettingsItems(onDelete = onDelete)
                    }
                }
            }
        }
    }
}

@Composable
fun JavaSettingsItems(
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

    PreferenceList(
        title = "Launch Arguments Mode",
        entries = arrayOf("Replace Global", "Merge (Global First)", "Merge (Instance First)"),
        entryValues = arrayOf("0", "1", "2"),
        selectedValue = argsMode.toString(),
        onValueChange = { onArgsModeChange(it.toInt()) },
        imageVector = Icons.Default.Tune
    )

    PreferenceItem(
        title = "JVM Arguments",
        summary = jvmArgs.ifEmpty { stringResource(id = R.string.use_global_default) },
        imageVector = Icons.Filled.Terminal,
        onClick = { showJvmArgsDialog = true }
    )

    PreferenceList(
        title = "Java Runtime",
        entries = availableRuntimes.map { it.name }.toTypedArray(),
        entryValues = availableRuntimes.map { it.name }.toTypedArray(),
        selectedValue = selectedRuntime?.name ?: "<Default>",
        onValueChange = { name ->
            availableRuntimes.find { it.name == name }?.let { onRuntimeSelected(it) }
        },
        imageVector = Icons.Default.Layers
    )
}

@Composable
fun RenderingSettingsItems(
    versionId: String,
    rendererDisplayNames: List<String>,
    selectedRendererIndex: Int,
    onRendererSelected: (Int) -> Unit,
    preferredBackend: String,
    onPreferredBackendChange: (String) -> Unit
) {
    PreferenceList(
        title = "Renderer",
        entries = rendererDisplayNames.toTypedArray(),
        entryValues = rendererDisplayNames.indices.map { it.toString() }.toTypedArray(),
        selectedValue = if (selectedRendererIndex in rendererDisplayNames.indices) selectedRendererIndex.toString() else "-1",
        onValueChange = { onRendererSelected(it.toInt()) },
        imageVector = Icons.Default.Image
    )

    if (isBackendSupported(versionId)) {
        PreferenceList(
            title = stringResource(R.string.mcl_setting_title_preferred_graphics_backend),
            summary = stringResource(R.string.mcl_setting_subtitle_preferred_graphics_backend),
            entries = stringArrayResource(R.array.graphics_backend_names),
            entryValues = stringArrayResource(R.array.graphics_backend_values),
            selectedValue = preferredBackend,
            onValueChange = onPreferredBackendChange,
            imageVector = Icons.Default.Image
        )
    }
}

@Composable
fun DangerSettingsItems(onDelete: () -> Unit) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Delete Instance") },
            text = { Text("Are you sure you want to delete this instance? This action cannot be undone.") },
            confirmButton = {
                @Suppress("DEPRECATION")
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

    PreferenceItem(
        title = "Delete Instance",
        summary = "Permanently remove this instance and all its files",
        imageVector = Icons.Default.Delete,
        onClick = { showDeleteConfirm = true }
    )
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
