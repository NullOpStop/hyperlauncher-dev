package net.kdt.pojavlaunch.ui.screens

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import net.ashmeet.hyperlauncher.R
import net.kdt.pojavlaunch.BaseActivity
import net.kdt.pojavlaunch.prefs.LauncherPreferences
import net.kdt.pojavlaunch.ui.components.PreferenceGroup
import net.kdt.pojavlaunch.ui.theme.PojavTheme

@Composable
fun ProfileTypeSelectScreen(
    onBack: () -> Unit, // Still keeping the callback in case it's needed for system back handling
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
        color = MaterialTheme.colorScheme.surface.copy(alpha = if (hasBackground) 0.85f else 1f),
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

            Scaffold(
                containerColor = Color.Transparent
            ) { padding ->
                LazyColumn(
                    modifier = Modifier
                        .padding(padding)
                        .fillMaxSize(),
                    contentPadding = PaddingValues(top = 16.dp, bottom = 24.dp)
                ) {
                    item {
                        PreferenceGroup(title = stringResource(id = R.string.create_profile_vanilla_like_versions)) {
                            ProfileTypeItem(
                                title = stringResource(id = R.string.create_instance_vanilla),
                                icon = painterResource(id = R.drawable.ic_px_java),
                                onClick = onVanillaClick
                            )
                            ProfileTypeItem(
                                title = stringResource(id = R.string.mod_dl_install_optifine),
                                icon = painterResource(id = R.drawable.ic_optifine),
                                onClick = { onOptifineClick() }
                            )
                        }
                    }

                    item {
                        PreferenceGroup(title = stringResource(id = R.string.create_profile_modded_versions)) {
                            ProfileTypeItem(
                                title = stringResource(id = R.string.modloader_dl_install_fabric_instance),
                                icon = painterResource(id = R.drawable.ic_fabric),
                                onClick = onFabricClick
                            )
                            ProfileTypeItem(
                                title = stringResource(id = R.string.modloader_dl_install_quilt_instance),
                                icon = painterResource(id = R.drawable.ic_quilt),
                                onClick = onQuiltClick
                            )
                            ProfileTypeItem(
                                title = stringResource(id = R.string.modloader_dl_install_legacy_fabric_instance),
                                icon = painterResource(id = R.drawable.ic_fabric),
                                onClick = onLegacyFabricClick
                            )
                            ProfileTypeItem(
                                title = stringResource(id = R.string.modloader_dl_install_forge_instance),
                                icon = painterResource(id = R.drawable.ic_forge),
                                onClick = onForgeClick
                            )
                            ProfileTypeItem(
                                title = stringResource(id = R.string.modloader_dl_install_neoforge_instance),
                                icon = painterResource(id = R.drawable.ic_neoforge),
                                onClick = onNeoForgeClick
                            )
                            ProfileTypeItem(
                                title = stringResource(id = R.string.modpack_install_instance_button),
                                icon = painterResource(id = R.drawable.ic_package),
                                onClick = onModpackClick
                            )
                            ProfileTypeItem(
                                title = stringResource(id = R.string.create_bta_instance),
                                icon = painterResource(id = R.drawable.ic_px_java),
                                onClick = onBTAClick
                            )
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
    icon: Painter,
    onClick: () -> Unit
) {
    ListItem(
        headlineContent = { Text(title, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface) },
        leadingContent = { 
            Icon(
                painter = icon, 
                contentDescription = null, 
                modifier = Modifier.size(32.dp),
                tint = Color.Unspecified
            ) 
        },
        modifier = Modifier
            .clickable { onClick() }
            .padding(horizontal = 4.dp, vertical = 2.dp),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent)
    )
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
