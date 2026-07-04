package net.kdt.pojavlaunch.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.BaseActivity
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.ui.theme.PojavTheme

@Composable
fun ProfileTypeSelectScreen(
    onBack: () -> Unit,
    onVanillaClick: () -> Unit,
    onOptifineClick: () -> Unit,
    onFabricClick: () -> Unit,
    onForgeClick: () -> Unit,
    onQuiltClick: () -> Unit,
    onNeoForgeClick: () -> Unit,
    onLegacyFabricClick: () -> Unit,
    onModpackClick: () -> Unit,
    onBTAClick: () -> Unit
) {
    val isPreview = LocalInspectionMode.current

    val hasBackground = LauncherPreferences.PREF_BACKGROUND_PATH_STATE.value != null || 
                        LauncherPreferences.PREF_BACKGROUND_VIDEO_PATH_STATE.value != null || isPreview
    val backgroundBitmap = if (isPreview) {
        try { BaseActivity.getBackgroundBitmap() } catch (e: Exception) { null }
    } else null

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = if (hasBackground) Color.Transparent else MaterialTheme.colorScheme.surface,
        tonalElevation = 3.dp
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            if (isPreview && backgroundBitmap != null) {
                Image(
                    bitmap = backgroundBitmap.asImageBitmap(),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = LauncherPreferences.PREF_CONTENT_TRANSPARENCY_STATE.value))
                )
            }

            Scaffold(
                containerColor = Color.Transparent
            ) { padding ->
                Column(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize()
                ) {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        item {
                            Text(
                                text = stringResource(id = R.string.create_profile_vanilla_like_versions),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                            )
                        }
                        
                        item {
                            ProfileTypeItem(
                                title = stringResource(id = R.string.create_instance_vanilla),
                                summary = "The original Minecraft experience",
                                icon = painterResource(id = R.drawable.ic_px_java),
                                onClick = onVanillaClick
                            )
                        }
                        
                        item {
                            ProfileTypeItem(
                                title = stringResource(id = R.string.mod_dl_install_optifine),
                                summary = "Performance and HD graphics",
                                icon = painterResource(id = R.drawable.ic_optifine),
                                onClick = onOptifineClick
                            )
                        }

                        item {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = stringResource(id = R.string.create_profile_modded_versions),
                                style = MaterialTheme.typography.labelLarge,
                                color = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.padding(start = 8.dp, bottom = 4.dp)
                            )
                        }

                        item {
                            ProfileTypeItem(
                                title = stringResource(id = R.string.modloader_dl_install_fabric_instance),
                                summary = "Lightweight and modular mod loader",
                                icon = painterResource(id = R.drawable.ic_fabric),
                                onClick = onFabricClick
                            )
                        }
                        
                        item {
                            ProfileTypeItem(
                                title = stringResource(id = R.string.modloader_dl_install_quilt_instance),
                                summary = "The open-source community-driven mod loader",
                                icon = painterResource(id = R.drawable.ic_quilt),
                                onClick = onQuiltClick
                            )
                        }
                        
                        item {
                            ProfileTypeItem(
                                title = stringResource(id = R.string.modloader_dl_install_legacy_fabric_instance),
                                summary = "Fabric for older Minecraft versions",
                                icon = painterResource(id = R.drawable.ic_fabric),
                                onClick = onLegacyFabricClick
                            )
                        }
                        
                        item {
                            ProfileTypeItem(
                                title = stringResource(id = R.string.modloader_dl_install_forge_instance),
                                summary = "The classic modding API",
                                icon = painterResource(id = R.drawable.ic_forge),
                                onClick = onForgeClick
                            )
                        }
                        
                        item {
                            ProfileTypeItem(
                                title = stringResource(id = R.string.modloader_dl_install_neoforge_instance),
                                summary = "Modern fork of the Forge project",
                                icon = painterResource(id = R.drawable.ic_neoforge),
                                onClick = onNeoForgeClick
                            )
                        }
                        
                        item {
                            ProfileTypeItem(
                                title = stringResource(id = R.string.modpack_install_instance_button),
                                summary = "Install from Modrinth or CurseForge",
                                icon = painterResource(id = R.drawable.ic_package),
                                onClick = onModpackClick
                            )
                        }
                        
                        item {
                            ProfileTypeItem(
                                title = stringResource(id = R.string.create_bta_instance),
                                summary = "Better Than Adventure! mod",
                                icon = painterResource(id = R.drawable.ic_px_java),
                                onClick = onBTAClick
                            )
                        }
                        
                        item {
                            Spacer(modifier = Modifier.height(16.dp))
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun ProfileTypeItem(
    title: String,
    summary: String? = null,
    icon: Painter,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = LauncherPreferences.PREF_CONTENT_TRANSPARENCY_STATE.value),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = LauncherPreferences.PREF_CONTENT_TRANSPARENCY_STATE.value * 1.25f),
                modifier = Modifier.size(48.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = icon,
                        contentDescription = null,
                        modifier = Modifier
                            .size(32.dp)
                            .clip(RoundedCornerShape(8.dp)),
                        tint = Color.Unspecified
                    )
                }
            }
            
            Spacer(modifier = Modifier.width(16.dp))
            
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                if (summary != null) {
                    Text(
                        text = summary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
            
            Icon(
                painter = painterResource(id = R.drawable.spinner_arrow_right),
                contentDescription = null,
                modifier = Modifier.size(18.dp).alpha(0.3f),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Preview(showBackground = true, device = "spec:width=1280dp,height=800dp,orientation=landscape")
@Composable
fun ProfileTypeSelectScreenPreview() {
    PojavTheme(dynamicColor = true) {
        ProfileTypeSelectScreen(
            onBack = {},
            onVanillaClick = {},
            onOptifineClick = {},
            onFabricClick = {},
            onForgeClick = {},
            onQuiltClick = {},
            onNeoForgeClick = {},
            onLegacyFabricClick = {},
            onModpackClick = {},
            onBTAClick = {}
        )
    }
}
