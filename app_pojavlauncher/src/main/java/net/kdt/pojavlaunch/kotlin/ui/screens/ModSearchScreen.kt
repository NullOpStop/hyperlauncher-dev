package net.kdt.pojavlaunch.ui.screens

import android.graphics.Bitmap
import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.Spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.BaseActivity
import net.kdt.pojavlaunch.modloaders.modpacks.imagecache.ModIconCache
import net.kdt.pojavlaunch.modloaders.modpacks.models.Constants
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModDetail
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModItem
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.ui.theme.PojavTheme
import net.kdt.pojavlaunch.ui.utils.AnimationUtils

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ModSearchScreen(
    searchQuery: String,
    isLoading: Boolean,
    statusVisible: Boolean,
    statusText: String,
    statusColor: Color,
    items: List<ModItem>,
    expandedItemId: String?,
    expandedDetail: ModDetail?,
    detailLoading: Boolean,
    selectedVersionIndex: Int,
    lastPage: Boolean,
    tasksRunning: Boolean,
    onSearchQueryChange: (String) -> Unit,
    onSearchSubmit: () -> Unit,
    onFilterClick: () -> Unit,
    onImportClick: () -> Unit,
    onItemClick: (ModItem) -> Unit,
    onLoadMore: () -> Unit,
    onVersionSelected: (Int) -> Unit,
    onInstallClick: (ModItem) -> Unit
) {
    val listState = rememberLazyListState()
    val leftScrollState = rememberScrollState()
    val isPreview = LocalInspectionMode.current

    val hasBackground = LauncherPreferences.PREF_BACKGROUND_PATH_STATE.value != null || 
                        LauncherPreferences.PREF_BACKGROUND_VIDEO_PATH_STATE.value != null || isPreview
    val backgroundBitmap = if (isPreview) BaseActivity.getBackgroundBitmap() else null

    val transitionSpec = AnimationUtils.getTransitionSpec()
    val contentAlpha by LauncherPreferences.PREF_CONTENT_TRANSPARENCY_STATE

    LaunchedEffect(listState, items.size, lastPage, isLoading) {
        snapshotFlow { listState.layoutInfo.visibleItemsInfo.lastOrNull()?.index ?: -1 }.collect { lastVisibleIndex ->
            if (!lastPage && !isLoading && items.isNotEmpty() && lastVisibleIndex >= items.lastIndex - 1) {
                onLoadMore()
            }
        }
    }

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

                Box(modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = if (hasBackground) 0.4f else 0f))
                )
            }

            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(8.dp)
            ) {

                Surface(
                    modifier = Modifier
                        .weight(0.9f)
                        .fillMaxHeight()
                        .padding(end = 8.dp),
                    color = Color.Transparent
                ) {
                    val selectedItem = remember(expandedItemId, items) { items.find { it.id == expandedItemId } }

                    if (selectedItem == null) {
                        Column(
                            modifier = Modifier
                                .fillMaxSize()
                                .verticalScroll(leftScrollState)
                                .padding(4.dp)
                        ) {
                            OutlinedTextField(
                                value = searchQuery,
                                onValueChange = {
                                    onSearchQueryChange(it)
                                    if (it.isEmpty()) onSearchSubmit()
                                },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(52.dp),
                                placeholder = { Text("Search modpacks", fontSize = 13.sp) },
                                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, modifier = Modifier.size(18.dp)) },
                                trailingIcon = {
                                    IconButton(onClick = onFilterClick) {
                                        Icon(painter = painterResource(id = R.drawable.ic_filter), contentDescription = null, modifier = Modifier.size(18.dp))
                                    }
                                },
                                singleLine = true,
                                shape = RoundedCornerShape(14.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = contentAlpha),
                                    unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = contentAlpha),
                                    focusedBorderColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                    unfocusedBorderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.1f),
                                    focusedTextColor = MaterialTheme.colorScheme.onSurface,
                                    unfocusedTextColor = MaterialTheme.colorScheme.onSurface
                                ),
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                                keyboardActions = KeyboardActions(onSearch = { onSearchSubmit() })
                            )

                            if (statusVisible) {
                                Spacer(modifier = Modifier.height(8.dp))
                                @Suppress("DEPRECATION")
                                Text(
                                    text = statusText,
                                    color = statusColor,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    modifier = Modifier.padding(horizontal = 4.dp)
                                )
                            }

                            Spacer(modifier = Modifier.weight(1f))

                            Button(
                                onClick = onImportClick,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(44.dp),
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                            ) {
                                @Suppress("DEPRECATION")
                                Text("Import Local Modpack", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    } else {

                        Box(modifier = Modifier.fillMaxSize().padding(4.dp)) {
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .verticalScroll(leftScrollState)
                                    .padding(bottom = 60.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                ModIcon(item = selectedItem, size = 64.dp)

                                Spacer(modifier = Modifier.height(12.dp))

                                @Suppress("DEPRECATION")
                                Text(
                                    text = selectedItem.title.orEmpty(),
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.onSurface,
                                    textAlign = TextAlign.Center,
                                    maxLines = 2,
                                    overflow = TextOverflow.Ellipsis
                                )

                                Spacer(modifier = Modifier.height(8.dp))

                                @Suppress("DEPRECATION")
                                Text(
                                    text = selectedItem.description.orEmpty(),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )

                                Spacer(modifier = Modifier.height(12.dp))

                                Surface(
                                    shape = RoundedCornerShape(999.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
                                ) {
                                    @Suppress("DEPRECATION")
                                    Text(
                                        text = when (selectedItem.apiSource) {
                                            Constants.SOURCE_CURSEFORGE -> "CurseForge"
                                            Constants.SOURCE_MODRINTH -> "Modrinth"
                                            else -> "Source"
                                        },
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }

                            Button(
                                onClick = { onItemClick(selectedItem) }, 
                                modifier = Modifier
                                    .align(Alignment.BottomCenter)
                                    .height(40.dp)
                                    .fillMaxWidth(0.9f),
                                shape = CircleShape,
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.primary,
                                    contentColor = MaterialTheme.colorScheme.onPrimary
                                ),
                                elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
                            ) {
                                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                @Suppress("DEPRECATION")
                                Text("Back", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                            }
                        }
                    }
                }

                Surface(
                    modifier = Modifier
                        .weight(1.1f)
                        .fillMaxHeight(),
                    color = Color.Transparent
                ) {
                    AnimatedContent(
                        targetState = expandedItemId != null,
                        transitionSpec = transitionSpec,
                        label = "resultsTransition"
                    ) { isDetail ->
                        Box(modifier = Modifier.fillMaxSize()) {
                            if (isDetail) {
                                val selectedItem = items.find { it.id == expandedItemId }
                                if (detailLoading) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(32.dp).align(Alignment.Center),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else if (expandedDetail != null && selectedItem != null) {
                                    Column(
                                        modifier = Modifier.fillMaxSize().padding(8.dp),
                                        verticalArrangement = Arrangement.Center
                                    ) {
                                        @Suppress("DEPRECATION")
                                        Text(
                                            "Select Version",
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onSurface
                                        )

                                        Spacer(modifier = Modifier.height(12.dp))

                                        val versionNames = expandedDetail.versionNames?.filterNotNull().orEmpty()
                                        VersionDropdown(
                                            options = versionNames,
                                            selectedIndex = if (selectedVersionIndex in versionNames.indices) selectedVersionIndex else 0,
                                            enabled = versionNames.isNotEmpty(),
                                            onSelectedIndex = onVersionSelected
                                        )

                                        Spacer(modifier = Modifier.height(24.dp))

                                        Button(
                                            onClick = { onInstallClick(selectedItem) },
                                            enabled = versionNames.isNotEmpty() && !tasksRunning,
                                            modifier = Modifier.fillMaxWidth().height(52.dp),
                                            shape = RoundedCornerShape(14.dp),
                                            elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                                        ) {
                                            Icon(
                                                painter = painterResource(id = R.drawable.ic_px_download),
                                                contentDescription = null,
                                                modifier = Modifier.size(20.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            @Suppress("DEPRECATION")
                                            Text("Install Modpack", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                                        }
                                    }
                                } else {
                                    @Suppress("DEPRECATION")
                                    Text(
                                        "Unable to load details.",
                                        color = MaterialTheme.colorScheme.error,
                                        modifier = Modifier.align(Alignment.Center)
                                    )
                                }
                            } else {
                                if (isLoading && items.isEmpty()) {
                                    CircularProgressIndicator(
                                        modifier = Modifier.size(32.dp).align(Alignment.Center),
                                        color = MaterialTheme.colorScheme.primary
                                    )
                                } else if (items.isEmpty()) {
                                    Column(
                                        modifier = Modifier.fillMaxSize(),
                                        verticalArrangement = Arrangement.Center,
                                        horizontalAlignment = Alignment.CenterHorizontally
                                    ) {
                                        Icon(
                                            Icons.Default.Search,
                                            contentDescription = null,
                                            modifier = Modifier.size(48.dp),
                                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha)
                                        )
                                        Spacer(modifier = Modifier.height(16.dp))
                                        @Suppress("DEPRECATION")
                                        Text(
                                            "Search for modpacks to see results",
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = contentAlpha * 1.5f)
                                        )
                                    }
                                } else {
                                    LazyColumn(
                                        state = listState,
                                        modifier = Modifier.fillMaxSize(),
                                        contentPadding = PaddingValues(bottom = 8.dp)
                                    ) {
                                        items(items, key = { it.iconCacheTag }) { item ->
                                            ModItemView(
                                                item = item,
                                                onClick = { onItemClick(item) }
                                            )
                                            Spacer(modifier = Modifier.height(6.dp))
                                        }

                                        if (!lastPage && items.isNotEmpty()) {
                                            item {
                                                Box(
                                                    modifier = Modifier.fillMaxWidth().padding(8.dp),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    CircularProgressIndicator(modifier = Modifier.size(24.dp))
                                                }
                                            }
                                        }
                                    }
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
fun ModItemView(
    item: ModItem,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = LauncherPreferences.PREF_CONTENT_TRANSPARENCY_STATE.value),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ModIcon(item = item, size = 48.dp)

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                @Suppress("DEPRECATION")
                Text(
                    text = item.title.orEmpty(),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                @Suppress("DEPRECATION")
                Text(
                    text = item.description.orEmpty(),
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
private fun ModIcon(item: ModItem, size: androidx.compose.ui.unit.Dp = 44.dp) {
    var bitmap by remember(item.id) { mutableStateOf<Bitmap?>(null) }
    val iconCache = remember { ModIconCache() }

    LaunchedEffect(item.id) {
        iconCache.getImage({ bm ->
            bitmap = bm
        }, item.iconCacheTag, item.imageUrl)
    }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = LauncherPreferences.PREF_CONTENT_TRANSPARENCY_STATE.value * 0.75f),
        modifier = Modifier.size(size)
    ) {
        Box(contentAlignment = Alignment.Center) {
            if (bitmap != null) {
                Image(
                    bitmap = bitmap!!.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
            } else {
                Icon(
                    painter = painterResource(id = when (item.apiSource) {
                        Constants.SOURCE_CURSEFORGE -> R.drawable.ic_curseforge
                        Constants.SOURCE_MODRINTH -> R.drawable.ic_modrinth
                        else -> R.drawable.ic_px_java
                    }),
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f),
                    modifier = Modifier.size(size * 0.6f)
                )
            }
        }
    }
}

@Composable
private fun VersionDropdown(
    options: List<String>,
    selectedIndex: Int,
    enabled: Boolean,
    onSelectedIndex: (Int) -> Unit
) {
    var expanded by remember(options, selectedIndex) { mutableStateOf(false) }
    val currentText = options.getOrNull(selectedIndex).orEmpty()

    Box(modifier = Modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = currentText,
            onValueChange = {},
            readOnly = true,
            enabled = enabled,
            modifier = Modifier
                .fillMaxWidth()
                .clickable(enabled = enabled) { expanded = true },
            trailingIcon = {
                IconButton(onClick = { expanded = !expanded }, enabled = enabled) {
                    Icon(
                        imageVector = Icons.Default.ArrowDropDown,
                        contentDescription = null
                    )
                }
            },
            placeholder = { Text("Select version") },
            shape = RoundedCornerShape(14.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                unfocusedContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                disabledContainerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.15f)
            )
        )

        DropdownMenu(
            expanded = expanded && enabled && options.isNotEmpty(),
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .fillMaxWidth(0.6f)
                .background(MaterialTheme.colorScheme.surface)
        ) {
            options.forEachIndexed { index, option ->
                DropdownMenuItem(
                    text = {
                        @Suppress("DEPRECATION")
                        Text(
                            text = option,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    onClick = {
                        onSelectedIndex(index)
                        expanded = false
                    }
                )
            }
        }
    }
}

@Preview(showBackground = true, widthDp = 800, heightDp = 400)
@Composable
fun ModSearchScreenPreview() {
    PojavTheme(dynamicColor = true) {
        ModSearchScreen(
            searchQuery = "",
            isLoading = false,
            statusVisible = true,
            statusText = "Found 2 results",
            statusColor = Color.Gray,
            items = listOf(
                ModItem(Constants.SOURCE_CURSEFORGE, true, "1", "RLCraft", "A very hard modpack", null),
                ModItem(Constants.SOURCE_MODRINTH, true, "2", "Fabulously Optimized", "Better performance", null)
            ),
            expandedItemId = null,
            expandedDetail = null,
            detailLoading = false,
            selectedVersionIndex = 0,
            lastPage = true,
            tasksRunning = false,
            onSearchQueryChange = {},
            onSearchSubmit = {},
            onFilterClick = {},
            onImportClick = {},
            onItemClick = {},
            onLoadMore = {},
            onVersionSelected = {},
            onInstallClick = {}
        )
    }
}
