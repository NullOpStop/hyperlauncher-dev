package net.kdt.pojavlaunch.ui.screens

import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.horizontalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.BaseActivity
import net.kdt.pojavlaunch.kotlin.ui.viewmodel.ContentInstallerViewModel
import net.kdt.pojavlaunch.modrinth.ModrinthProject
import net.kdt.pojavlaunch.modrinth.ModrinthVersion
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.ui.theme.PojavTheme
import net.kdt.pojavlaunch.ui.utils.AnimationUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ContentInstallerScreen(
    onBack: () -> Unit,
    onOpenDownloads: () -> Unit,
    onInstallLocal: () -> Unit,
    onSearch: (String, ContentInstallerType, version: String?, loader: String?) -> Unit,
    onProjectClick: (ModrinthProject) -> Unit,
    onVersionClick: (ModrinthVersion) -> Unit,
    projects: List<ModrinthProject>,
    isLoading: Boolean,
    statusText: String,
    selectedVersion: String?,
    selectedLoader: String?,
    onVersionFilterChange: (String?) -> Unit,
    onLoaderFilterChange: (String?) -> Unit,
    instanceVersion: String?,
    instanceLoader: String?,
    viewingProject: ModrinthProject? = null,
    selectedType: ContentInstallerType = ContentInstallerType.MODS,
    projectVersions: List<ModrinthVersion> = emptyList(),
    availableProjectMCVersions: List<String> = emptyList(),
    selectedProjectMCVersion: String? = null,
    onProjectMCVersionClick: (String) -> Unit = {},
    onBackToProjects: () -> Unit = {}
) {
    var searchQuery by remember { mutableStateOf("") }

    val filterScrollState = rememberScrollState()
    val isPreview = LocalInspectionMode.current
    val viewModel: ContentInstallerViewModel = viewModel()

    val backgroundBitmap = if (isPreview) BaseActivity.getBackgroundBitmap() else null
    val hasBackground = LauncherPreferences.PREF_BACKGROUND_PATH_STATE.value != null ||
            LauncherPreferences.PREF_BACKGROUND_VIDEO_PATH_STATE.value != null || isPreview

    val transitionSpec = AnimationUtils.getTransitionSpec()

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
                 Box(
                     modifier = Modifier
                         .fillMaxSize()
                         .background(Color.Black.copy(alpha = if (hasBackground) 0.4f else 0f))
                 )
             }

             Row(
                 modifier = Modifier
                     .fillMaxSize()
                     .padding(8.dp)
             ) {
                 ContentTypeRail(
                     selectedType = selectedType,
                     onTypeSelected = { type ->
                         onSearch(searchQuery, type, selectedVersion, selectedLoader)
                     },
                     modifier = Modifier.fillMaxHeight()
                 )

                 Spacer(modifier = Modifier.width(8.dp))

                 Box(
                     modifier = Modifier
                         .weight(1f)
                         .fillMaxHeight()
                 ) {
                      AnimatedContent(
                          targetState = viewingProject,
                          transitionSpec = transitionSpec,
                          label = "installer_viewing_anim"
                      ) { project ->
                         if (project == null) {
                             Row(
                                 modifier = Modifier.fillMaxSize()
                             ) {
                                 Surface(
                                     modifier = Modifier
                                         .weight(1f)
                                         .fillMaxHeight(),
                                     color = Color.Transparent
                                 ) {
                                     AnimatedContent(
                                         targetState = selectedType,
                                         transitionSpec = transitionSpec,
                                         label = "installer_type_anim",
                                         modifier = Modifier.fillMaxSize()
                                     ) { targetType ->
                                         key(targetType) {
                                             if (isLoading && projects.isEmpty()) {
                                                 Box(
                                                     modifier = Modifier.fillMaxSize(),
                                                     contentAlignment = Alignment.Center
                                                 ) {
                                                     CircularProgressIndicator(
                                                         modifier = Modifier.size(36.dp),
                                                         color = MaterialTheme.colorScheme.primary
                                                     )
                                                 }
                                             } else {
                                                 LazyColumn(
                                                     state = rememberLazyListState(),
                                                     modifier = Modifier.fillMaxSize(),
                                                     contentPadding = PaddingValues(bottom = 8.dp)
                                                 ) {
                                                     items(projects, key = { it.id }) { p ->
                                                         ProjectItemView(
                                                             project = p,
                                                             onClick = { onProjectClick(p) },
                                                             onVisible = { viewModel.requestIcon(p) }
                                                         )
                                                         Spacer(modifier = Modifier.height(6.dp))
                                                     }
                                                 }
                                             }
                                         }
                                     }
                                 }

                                 Spacer(modifier = Modifier.width(8.dp))

                                 Box(
                                     modifier = Modifier
                                         .widthIn(min = 200.dp, max = 260.dp)
                                         .fillMaxHeight()
                                 ) {
                                     Column(
                                         modifier = Modifier
                                             .fillMaxSize()
                                             .verticalScroll(filterScrollState)
                                             .padding(4.dp)
                                     ) {
                                         OutlinedTextField(
                                             value = searchQuery,
                                             onValueChange = {
                                                 searchQuery = it
                                                 onSearch(it, selectedType, selectedVersion, selectedLoader)
                                             },
                                             modifier = Modifier
                                                 .fillMaxWidth()
                                                 .height(52.dp),
                                             placeholder = { Text(stringResource(id = R.string.installer_search_hint), fontSize = 13.sp) },
                                             leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                             singleLine = true,
                                             shape = RoundedCornerShape(14.dp),
                                             colors = OutlinedTextFieldDefaults.colors(
                                                 focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                                 unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
                                                 focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                                 unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                                 focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                                 unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                             ),
                                             keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                             keyboardActions = KeyboardActions(onSearch = {
                                                 onSearch(searchQuery, selectedType, selectedVersion, selectedLoader)
                                             })
                                         )

                                         Spacer(modifier = Modifier.height(12.dp))

                                         VerticalFilterMenu(
                                             title = "Minecraft Version",
                                             options = listOf("Any") + (if (instanceVersion != null) listOf(instanceVersion) else emptyList()),
                                             selectedOption = selectedVersion ?: "Any",
                                             onOptionSelected = {
                                                 val newValue = if (it == "Any") null else it
                                                 onVersionFilterChange(newValue)
                                                 onSearch(searchQuery, selectedType, newValue, selectedLoader)
                                             }
                                         )

                                         if (selectedType == ContentInstallerType.MODS || selectedType == ContentInstallerType.MODPACKS) {
                                             Spacer(modifier = Modifier.height(12.dp))
                                             VerticalFilterMenu(
                                                 title = "Mod Loader",
                                                 options = listOf("Any", "fabric", "forge", "quilt", "neoforge"),
                                                 labels = listOf("Any", "Fabric", "Forge", "Quilt", "NeoForge"),
                                                 selectedOption = selectedLoader ?: "Any",
                                                 onOptionSelected = {
                                                     val newValue = if (it == "Any") null else it
                                                     onLoaderFilterChange(newValue)
                                                     onSearch(searchQuery, selectedType, selectedVersion, newValue)
                                                 }
                                             )
                                         }
                                     }
                                 }
                             }
                         } else {
                             VersionOverlayPanel(
                                 viewingProject = project,
                                 projectVersions = projectVersions,
                                 availableProjectMCVersions = availableProjectMCVersions,
                                 selectedProjectMCVersion = selectedProjectMCVersion,
                                 instanceVersion = instanceVersion,
                                 instanceLoader = instanceLoader,
                                 isLoading = isLoading,
                                 transitionSpec = transitionSpec,
                                 onBackToProjects = onBackToProjects,
                                 onProjectMCVersionClick = onProjectMCVersionClick,
                                 onVersionClick = onVersionClick
                             )
                         }
                     }
                 }
             }
         }
    }
}

@Composable
private fun VersionOverlayPanel(
    viewingProject: ModrinthProject,
    projectVersions: List<ModrinthVersion>,
    availableProjectMCVersions: List<String>,
    selectedProjectMCVersion: String?,
    instanceVersion: String?,
    instanceLoader: String?,
    isLoading: Boolean,
    transitionSpec: AnimatedContentTransitionScope<*>.() -> ContentTransform,
    onBackToProjects: () -> Unit,
    onProjectMCVersionClick: (String) -> Unit,
    onVersionClick: (ModrinthVersion) -> Unit
) {
    val viewModel: ContentInstallerViewModel = viewModel()
    Row(
        modifier = Modifier.fillMaxSize()
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onBackToProjects,
                    modifier = Modifier.size(32.dp)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = MaterialTheme.colorScheme.onSurface
                    )
                }
                Spacer(Modifier.width(8.dp))
                @Suppress("DEPRECATION")
                Text(
                    text = if (selectedProjectMCVersion == null) "Select Minecraft Version" else "Select File",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            HorizontalDivider(color = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))

            AnimatedContent(
                targetState = if (isLoading) "loading" else selectedProjectMCVersion ?: "versions",
                transitionSpec = transitionSpec,
                label = "overlay_content_anim",
                modifier = Modifier.fillMaxSize()
            ) { state ->
                when (state) {
                    "loading" -> {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(32.dp),
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    "versions" -> {
                        val listState = rememberLazyListState()
                        LaunchedEffect(availableProjectMCVersions) {
                            val index = availableProjectMCVersions.indexOfFirst { v ->
                                instanceVersion != null && v.contains(instanceVersion, ignoreCase = true)
                            }
                            if (index != -1) listState.scrollToItem(index + 1)
                        }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(8.dp)
                        ) {
                            items(availableProjectMCVersions) { v ->
                                val isCompatible = instanceVersion != null && v.contains(instanceVersion, ignoreCase = true)
                                @Suppress("DEPRECATION")
                                SubVersionItemView(
                                    text = v,
                                    isCompatible = isCompatible,
                                    onClick = { onProjectMCVersionClick(v) }
                                )
                                Spacer(Modifier.height(6.dp))
                            }
                        }
                    }
                    else -> {
                        // 'state' is the selected version string
                        val listState = rememberLazyListState()
                        val filteredVersions = remember(projectVersions, state) {
                            projectVersions.filter { it.gameVersions.contains(state) }
                        }

                        LaunchedEffect(filteredVersions) {
                            val index = filteredVersions.indexOfFirst { version ->
                                instanceVersion != null && version.gameVersions.any { it.contains(instanceVersion, ignoreCase = true) } &&
                                        (instanceLoader == null || version.loaders.any { it.equals(instanceLoader, ignoreCase = true) })
                            }
                            if (index != -1) listState.scrollToItem(index + 1)
                        }

                        LazyColumn(
                            state = listState,
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = PaddingValues(8.dp)
                        ) {
                            item {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 8.dp)
                                ) {
                                    @Suppress("DEPRECATION")
                                    TextButton(onClick = { onProjectMCVersionClick("") }) {
                                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                                        Spacer(Modifier.width(4.dp))
                                        Text("Change Version", fontSize = 12.sp)
                                    }
                                    @Suppress("DEPRECATION")
                                    Text(
                                        "Files for $state",
                                        color = MaterialTheme.colorScheme.onSurface,
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.Bold,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                            items(filteredVersions) { version ->
                                val isCompatible = instanceVersion != null && version.gameVersions.any { it.contains(instanceVersion, ignoreCase = true) } &&
                                        (instanceLoader == null || version.loaders.any { it.equals(instanceLoader, ignoreCase = true) })
                                @Suppress("DEPRECATION")
                                VersionItemView(
                                    version = version,
                                    isCompatible = isCompatible,
                                    onClick = { onVersionClick(version) }
                                )
                                Spacer(Modifier.height(6.dp))
                            }
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.width(8.dp))

        ProjectInfoPanel(
            project = viewingProject,
            viewModel = viewModel,
            modifier = Modifier
                .widthIn(min = 200.dp, max = 260.dp)
                .fillMaxHeight()
        )
    }
}



@Composable
private fun ContentTypeRail(
    selectedType: ContentInstallerType,
    onTypeSelected: (ContentInstallerType) -> Unit,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    Column(
        modifier = modifier
            .width(IntrinsicSize.Min)
            .verticalScroll(scrollState)
            .padding(start = 4.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Spacer(modifier = Modifier.height(12.dp))

        ContentInstallerType.entries.forEach { type ->
            NavigationRailItem(
                selected = selectedType == type,
                onClick = {
                    if (selectedType != type) onTypeSelected(type)
                },
                icon = {
                    Icon(
                        painter = painterResource(id = type.iconRes),
                        contentDescription = stringResource(id = type.labelRes),
                        modifier = Modifier.size(22.dp)
                    )
                },
                label = {
                    Text(
                        modifier = Modifier.basicMarquee(iterations = Int.MAX_VALUE),
                        text = stringResource(id = type.labelRes),
                        maxLines = 1,
                        style = MaterialTheme.typography.labelMedium
                    )
                },
                colors = NavigationRailItemDefaults.colors(
                    selectedIconColor = MaterialTheme.colorScheme.primary,
                    selectedTextColor = MaterialTheme.colorScheme.primary,
                    indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                    unselectedIconColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
                    unselectedTextColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f)
                )
            )

            Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

@Composable
fun VerticalFilterMenu(
    title: String,
    options: List<String>,
    labels: List<String>? = null,
    selectedOption: String,
    onOptionSelected: (String) -> Unit
) {
    var isExpanded by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f)), RoundedCornerShape(14.dp))
            .padding(vertical = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable { isExpanded = !isExpanded }
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            @Suppress("DEPRECATION")
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.5f),
                modifier = Modifier.weight(1f)
            )
            Icon(
                imageVector = if (isExpanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                contentDescription = null,
                modifier = Modifier.size(18.dp),
                tint = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.4f)
            )
        }

        AnimatedVisibility(
            visible = isExpanded,
            enter = expandVertically() + fadeIn(),
            exit = shrinkVertically() + fadeOut()
        ) {
            Column(modifier = Modifier.padding(bottom = 4.dp)) {
                options.forEachIndexed { index, option ->
                    val label = labels?.get(index) ?: option
                    val isSelected = option.equals(selectedOption, ignoreCase = true) || label.equals(selectedOption, ignoreCase = true)

                    Surface(
                        onClick = { onOptionSelected(option) },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(34.dp)
                            .padding(horizontal = 4.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f) else Color.Transparent,
                        contentColor = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        ) {
                            RadioButton(
                                selected = isSelected,
                                onClick = null, // Handled by Surface
                                modifier = Modifier.size(14.dp),
                                colors = RadioButtonDefaults.colors(
                                    selectedColor = MaterialTheme.colorScheme.primary,
                                    unselectedColor = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.3f)
                                )
                            )
                            Spacer(Modifier.width(8.dp))
                            @Suppress("DEPRECATION")
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun SubVersionItemView(
    text: String,
    isCompatible: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        color = if (isCompatible) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, if (isCompatible) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
    ) {
        Box(contentAlignment = Alignment.CenterStart, modifier = Modifier.padding(16.dp)) {
            @Suppress("DEPRECATION")
            Text(
                text = text,
                fontSize = 14.sp,
                fontWeight = if (isCompatible) FontWeight.Bold else FontWeight.Medium,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun InstallerImageButton(
    onClick: () -> Unit,
    icon: Int,
    contentDescription: String,
    modifier: Modifier = Modifier
) {
    Surface(
        onClick = onClick,
        modifier = modifier.height(44.dp),
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f))
    ) {
        Box(contentAlignment = Alignment.Center) {
            Icon(
                painter = painterResource(id = icon),
                contentDescription = contentDescription,
                modifier = Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun ProjectIcon(project: ModrinthProject, size: androidx.compose.ui.unit.Dp = 48.dp) {
    val iconBitmap = project.iconBitmap
    val isLoading = project.isIconLoading

    Box(
        modifier = Modifier
            .size(size)
            .clip(RoundedCornerShape(8.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(size / 2),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
        } else if (iconBitmap != null) {
            Image(
                bitmap = iconBitmap.asImageBitmap(),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        } else {
            Icon(
                painter = painterResource(id = R.drawable.ic_px_java),
                contentDescription = null,
                modifier = Modifier.size(size * 0.8f),
                tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
fun ProjectItemView(
    project: ModrinthProject,
    onClick: () -> Unit,
    onVisible: () -> Unit = {}
) {
    LaunchedEffect(project.id) {
        onVisible()
    }

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            @Suppress("DEPRECATION")
            ProjectIcon(project)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                @Suppress("DEPRECATION")
                Text(
                    text = project.title,
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                @Suppress("DEPRECATION")
                Text(
                    text = project.description,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

@Composable
fun VersionItemView(
    version: ModrinthVersion,
    isCompatible: Boolean,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = if (isCompatible) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.4f),
        border = BorderStroke(1.dp, if (isCompatible) MaterialTheme.colorScheme.primary.copy(alpha = 0.4f) else MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                painter = painterResource(id = R.drawable.ic_px_download),
                contentDescription = null,
                modifier = Modifier.size(24.dp),
                tint = if (isCompatible) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondary
            )

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                @Suppress("DEPRECATION")
                Text(
                    text = version.name,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                val infoText = buildString {
                    if (version.loaders.isNotEmpty()) {
                        append(version.loaders.joinToString(", "))
                        append("  •  ")
                    }
                    append(version.gameVersions.joinToString(", "))
                }
                @Suppress("DEPRECATION")
                Text(
                    text = infoText,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Clip
                )
            }
        }
    }
}

@Composable
fun ProjectInfoPanel(
    project: ModrinthProject,
    viewModel: ContentInstallerViewModel,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.2f))
            .border(BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.05f)), RoundedCornerShape(14.dp))
            .verticalScroll(scrollState)
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        @Suppress("DEPRECATION")
        ProjectIcon(project, size = 64.dp)
        
        Spacer(modifier = Modifier.height(12.dp))
        
        @Suppress("DEPRECATION")
        Text(
            text = project.title,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurface,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        @Suppress("DEPRECATION")
        Text(
            text = project.fullDescription ?: project.description,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 16.sp
        )
        
        if (project.gallery.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            @Suppress("DEPRECATION")
            Text(
                text = "Gallery",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            Spacer(modifier = Modifier.height(8.dp))
            
            val galleryScrollState = rememberScrollState()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(galleryScrollState)
            ) {
                project.gallery.forEach { imageUrl ->
                    SimpleAsyncImage(
                        url = imageUrl,
                        viewModel = viewModel,
                        modifier = Modifier
                            .size(160.dp, 90.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                }
            }
        }
    }
}

@Composable
fun SimpleAsyncImage(
    url: String,
    viewModel: ContentInstallerViewModel,
    modifier: Modifier = Modifier
) {
    var bitmap by remember { mutableStateOf<android.graphics.Bitmap?>(null) }
    LaunchedEffect(url) {
        viewModel.requestImage(url) { bitmap = it }
    }

    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f)),
        contentAlignment = Alignment.Center
    ) {
        if (bitmap != null) {
            Image(
                bitmap = bitmap!!.asImageBitmap(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            CircularProgressIndicator(
                modifier = Modifier.size(24.dp),
                strokeWidth = 2.dp,
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
            )
        }
    }
}

@Preview(showBackground = true, widthDp = 800, heightDp = 400)
@Composable
fun ContentInstallerScreenPreview() {
    PojavTheme(dynamicColor = true) {
        ContentInstallerScreen(
            onBack = {},
            onOpenDownloads = {},
            onInstallLocal = {},
            onSearch = { _, _, _, _ -> },
            onProjectClick = {},
            onVersionClick = {},
            projects = listOf(
                ModrinthProject("1", "Sodium", "A search for performance", null),
                ModrinthProject("2", "Iris Shaders", "A modern shaders mod", null)
            ),
            isLoading = false,
            statusText = "Found 2 results",
            selectedVersion = "1.20.1",
            selectedLoader = "Fabric",
            onVersionFilterChange = {},
            onLoaderFilterChange = {},
            instanceVersion = "1.20.1",
            instanceLoader = "fabric",
            selectedType = ContentInstallerType.MODS
        )
    }
}
