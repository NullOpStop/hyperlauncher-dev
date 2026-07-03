package net.kdt.pojavlaunch.ui.screens

import net.ashmeet.hyperlauncher.R

enum class ContentInstallerType(val projectType: String, val labelRes: Int, val iconRes: Int, val category: String? = null) {
    MODS("mod", R.string.installer_mods, R.drawable.add_row_below_40px),
    MODPACKS("modpack", R.string.installer_modpacks, R.drawable.ic_package),
    SHADERS("shader", R.string.installer_shaders, R.drawable.lightbulb_2_40px),
    RESOURCES("resourcepack", R.string.installer_packs, R.drawable.box_edit_40px),
    DATAPACKS("datapack", R.string.installer_datapacks, R.drawable.ic_px_java, "datapack")
}
