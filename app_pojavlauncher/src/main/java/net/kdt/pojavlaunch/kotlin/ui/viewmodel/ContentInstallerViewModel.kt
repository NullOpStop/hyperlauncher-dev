package net.kdt.pojavlaunch.kotlin.ui.viewmodel

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.util.Log
import android.util.LruCache
import android.widget.Toast
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.kdt.mcgui.ProgressLayout
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import net.kdt.pojavlaunch.Tools
import net.kdt.pojavlaunch.JMinecraftVersionList
import net.kdt.pojavlaunch.extra.ExtraConstants
import net.kdt.pojavlaunch.extra.ExtraCore
import net.kdt.pojavlaunch.instances.Instances
import net.kdt.pojavlaunch.modloaders.modpacks.api.ApiHandler
import net.kdt.pojavlaunch.modloaders.modpacks.api.ModrinthApi
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModDetail
import net.kdt.pojavlaunch.modloaders.modpacks.models.ModItem
import net.kdt.pojavlaunch.modloaders.modpacks.models.Constants
import net.kdt.pojavlaunch.modrinth.ModrinthProject
import net.kdt.pojavlaunch.modrinth.ModrinthVersion
import net.kdt.pojavlaunch.progresskeeper.ProgressKeeper
import net.kdt.pojavlaunch.services.ProgressService
import net.kdt.pojavlaunch.ui.screens.ContentInstallerType
import net.kdt.pojavlaunch.utils.DownloadUtils
import net.kdt.pojavlaunch.utils.FileUtils
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.util.ArrayList
import java.util.HashMap
import java.util.Locale
import java.util.concurrent.atomic.AtomicInteger

class ContentInstallerViewModel : ViewModel() {
    private val mModrinthApi = ApiHandler("https://api.modrinth.com/v2")
    private val mSearchToken = AtomicInteger(0)

    private val mIconMemoryCache = LruCache<String, Bitmap>(100)
    private val mDownloadSemaphore = Semaphore(4)
    private val mIconLoadingJobs = mutableMapOf<String, Job>()

    var projects by mutableStateOf<List<ModrinthProject>>(emptyList())
    var isLoading by mutableStateOf(false)
    var statusText by mutableStateOf("")

    var versionFilter by mutableStateOf<String?>(null)
    var loaderFilter by mutableStateOf<String?>(null)

    var instanceVersion by mutableStateOf<String?>(null)
    var instanceLoader by mutableStateOf<String?>(null)

    var selectedType by mutableStateOf(ContentInstallerType.MODS)
    var viewingProject by mutableStateOf<ModrinthProject?>(null)
    var projectVersions by mutableStateOf<List<ModrinthVersion>>(emptyList())

    var availableProjectMCVersions by mutableStateOf<List<String>>(emptyList())
    var selectedProjectMCVersion by mutableStateOf<String?>(null)

    private var mSearchJob: Job? = null

    fun init(context: Context) {
        val inst = Instances.loadSelectedInstance() ?: return
        var iv = inst.versionId ?: return

        if (iv == "latest_release" || iv == "latest_snapshot") {
            val versionList = ExtraCore.getValue(ExtraConstants.RELEASE_TABLE) as? JMinecraftVersionList
            if (versionList != null && versionList.latest != null) {
                val key = if (iv == "latest_release") "release" else "snapshot"
                val resolved = versionList.latest[key]
                if (resolved != null) iv = resolved
            }
        }

        val parts = iv.split("-").toTypedArray()
        instanceVersion = null
        instanceLoader = null

        // Primary search: Look for standard Minecraft versions (1.x.x) or snapshots
        for (part in parts) {
            if (part.matches("1\\.\\d+(\\.\\d+)?".toRegex()) || part.matches("\\d{2}w\\d{2}[a-z]".toRegex())) {
                instanceVersion = part
                break
            }
        }

        // Secondary search: Fallback to original logic if no standard version found
        if (instanceVersion == null) {
            for (i in parts.indices.reversed()) {
                val part = parts[i]
                if (part.matches("\\d+\\.\\d+(\\.\\d+)?".toRegex())) {
                    instanceVersion = part
                    break
                }
            }
        }

        val ivLower = iv.lowercase(Locale.getDefault())
        if (ivLower.contains("fabric")) instanceLoader = "fabric"
        else if (ivLower.contains("forge")) instanceLoader = "forge"
        else if (ivLower.contains("quilt")) instanceLoader = "quilt"
        else if (ivLower.contains("neoforge")) instanceLoader = "neoforge"

        // Avoid passing non-version strings to Modrinth
        versionFilter = if (instanceVersion != null && (
                    instanceVersion!!.matches("1\\.\\d+(\\.\\d+)?".toRegex()) ||
                    instanceVersion!!.matches("\\d{2}w\\d{2}[a-z]".toRegex()) ||
                    instanceVersion!!.matches("\\d+\\.\\d+(\\.\\d+)?".toRegex())
                )) instanceVersion else null
        
        loaderFilter = instanceLoader
        
        val defaultType = ExtraCore.consumeValue(ExtraConstants.DEFAULT_CONTENT_TYPE) as? ContentInstallerType
        selectedType = defaultType ?: ContentInstallerType.MODS

        triggerSearch("", selectedType)
    }

    fun triggerSearch(query: String, type: ContentInstallerType = selectedType) {
        mSearchJob?.cancel()
        mIconLoadingJobs.values.forEach { it.cancel() }
        mIconLoadingJobs.clear()

        selectedType = type
        viewingProject = null
        selectedProjectMCVersion = null
        projects = emptyList()

        val token = mSearchToken.incrementAndGet()
        isLoading = true
        statusText = "Searching..."
        ProgressKeeper.submitProgress(ProgressLayout.CONTENT_INSTALL, 0, 0, statusText)

        mSearchJob = viewModelScope.launch(Dispatchers.IO) {
            try {
                // Add a small delay to debounce typing
                if (query.isNotEmpty()) delay(300)

                val results = searchProjects(query, type)

                withContext(Dispatchers.Main) {
                    if (token != mSearchToken.get()) return@withContext
                    projects = results
                    isLoading = false
                    statusText =
                        if (results.isEmpty()) "No results found" else "Found ${results.size} projects"
                    ProgressKeeper.submitProgress(
                        ProgressLayout.CONTENT_INSTALL,
                        100,
                        0,
                        statusText
                    )

                    launch {
                        delay(2000)
                        if (token == mSearchToken.get()) ProgressKeeper.submitProgress(
                            ProgressLayout.CONTENT_INSTALL,
                            -1,
                            -1
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e("ContentInstaller", "Search failed", e)
                withContext(Dispatchers.Main) {
                    if (token != mSearchToken.get()) return@withContext
                    isLoading = false
                    statusText = "Failed to load"
                    ProgressKeeper.submitProgress(
                        ProgressLayout.CONTENT_INSTALL,
                        100,
                        0,
                        statusText
                    )
                    launch {
                        delay(2000)
                        ProgressKeeper.submitProgress(ProgressLayout.CONTENT_INSTALL, -1, -1)
                    }
                }
            }
        }
    }

    /** Triggered by UI when an item is visible */
    fun requestIcon(project: ModrinthProject) {
        val url = project.iconUrl ?: return
        if (project.iconBitmap != null || project.isIconLoading) return
        if (mIconLoadingJobs.containsKey(project.id)) return

        val job = viewModelScope.launch(Dispatchers.IO) {
            project.isIconLoadingState.value = true
            val bitmap = getIcon(url)
            withContext(Dispatchers.Main) {
                project.iconBitmapState.value = bitmap
                project.isIconLoadingState.value = false
            }
            mIconLoadingJobs.remove(project.id)
        }
        mIconLoadingJobs[project.id] = job
    }

    fun requestImage(url: String, onBitmapLoaded: (Bitmap?) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val bitmap = getIcon(url)
            withContext(Dispatchers.Main) {
                onBitmapLoaded(bitmap)
            }
        }
    }

    private suspend fun getIcon(url: String): Bitmap? = withContext(Dispatchers.IO) {
        mIconMemoryCache.get(url)?.let { return@withContext it }

        val cacheFile =
            File(Tools.DIR_CACHE, "modrinth_icons/" + url.hashCode().toString() + ".png")
        if (cacheFile.exists()) {
            try {
                val bitmap = BitmapFactory.decodeFile(cacheFile.absolutePath)
                if (bitmap != null) {
                    mIconMemoryCache.put(url, bitmap)
                    return@withContext bitmap
                }
            } catch (e: Exception) {
                cacheFile.delete()
            }
        }

        return@withContext mDownloadSemaphore.withPermit {
            try {
                val connection = URL(url).openConnection()
                connection.setRequestProperty("User-Agent", "PojavLauncher/1.0 (HyperLauncher)")
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                val bitmap = connection.getInputStream().use { BitmapFactory.decodeStream(it) }

                if (bitmap != null) {
                    mIconMemoryCache.put(url, bitmap)

                    FileUtils.ensureDirectorySilently(cacheFile.parentFile)
                    try {
                        FileOutputStream(cacheFile).use {
                            bitmap.compress(Bitmap.CompressFormat.PNG, 90, it)
                        }
                    } catch (e: Exception) {
                        Log.w("ContentInstaller", "Failed to disk cache icon")
                    }
                }
                bitmap
            } catch (e: Exception) {
                Log.w("ContentInstaller", "Icon download failed for $url")
                null
            }
        }
    }

    private fun searchProjects(query: String, type: ContentInstallerType): List<ModrinthProject> {
        val params = HashMap<String?, Any?>()
        params["query"] = query
        params["limit"] = 50
        params["index"] = if (query.isEmpty()) "downloads" else "relevance"
        params["facets"] = buildFacets(type)

        val response = mModrinthApi.get("search", params, JsonObject::class.java) ?: return emptyList()
        val hits = response.getAsJsonArray("hits") ?: return emptyList()

        val items = ArrayList<ModrinthProject>(hits.size())
        for (i in 0 until hits.size()) {
            val hit = hits.get(i).asJsonObject
            val id = if (hit.has("project_id")) hit.get("project_id").asString else null
            val title = if (hit.has("title")) hit.get("title").asString else "(untitled)"
            val desc = if (hit.has("description")) hit.get("description").asString else ""
            val iconUrl = if (hit.has("icon_url") && !hit.get("icon_url").isJsonNull) hit.get("icon_url").asString else null
            if (id != null) items.add(ModrinthProject(id, title, desc, iconUrl))
        }
        return items
    }

    private fun buildFacets(type: ContentInstallerType): String {
        val sb = StringBuilder("[")
        sb.append(String.format("[\"project_type:%s\"]", type.projectType))
        if (!versionFilter.isNullOrBlank()) sb.append(String.format(",[\"versions:%s\"]", versionFilter))
        if ((type == ContentInstallerType.MODS || type == ContentInstallerType.MODPACKS) && !loaderFilter.isNullOrBlank()) {
            sb.append(String.format(",[\"categories:%s\"]", loaderFilter))
        }
        if (type.category != null) {
            sb.append(String.format(",[\"categories:%s\"]", type.category))
        }
        sb.append("]")
        return sb.toString()
    }

    fun loadVersions(project: ModrinthProject) {
        val token = mSearchToken.incrementAndGet()
        viewingProject = project
        projectVersions = emptyList()
        availableProjectMCVersions = emptyList()
        selectedProjectMCVersion = null
        isLoading = true
        statusText = "Loading details..."
        ProgressKeeper.submitProgress(ProgressLayout.CONTENT_INSTALL, 0, 0, statusText)

        viewModelScope.launch(Dispatchers.IO) {
            try {
                // Fetch project details for gallery and description
                val projectDetails = mModrinthApi.get("project/${project.id}", JsonObject::class.java)
                val updatedProject = if (projectDetails != null) {
                    val gallery = mutableListOf<String>()
                    if (projectDetails.has("gallery") && projectDetails.get("gallery").isJsonArray) {
                        val arr = projectDetails.getAsJsonArray("gallery")
                        for (i in 0 until arr.size()) {
                            val item = arr.get(i).asJsonObject
                            if (item.has("url")) gallery.add(item.get("url").asString)
                        }
                    }
                    val fullDesc = if (projectDetails.has("body")) projectDetails.get("body").asString else null
                    
                    project.copy(gallery = gallery, fullDescription = fullDesc)
                } else {
                    project
                }

                val raw = mModrinthApi.get("project/${project.id}/version", JsonArray::class.java)
                val versions = if (raw != null) parseVersions(raw) else emptyList()

                withContext(Dispatchers.Main) {
                    if (token != mSearchToken.get()) return@withContext
                    isLoading = false
                    viewingProject = updatedProject
                    projectVersions = versions
                    availableProjectMCVersions =
                        versions.flatMap { it.gameVersions }.distinct().sortedDescending()

                    statusText =
                        if (versions.isEmpty()) "No downloadable versions found" else "Found ${versions.size} versions"
                    ProgressKeeper.submitProgress(
                        ProgressLayout.CONTENT_INSTALL,
                        100,
                        0,
                        statusText
                    )
                    launch {
                        delay(2000)
                        if (token == mSearchToken.get()) ProgressKeeper.submitProgress(
                            ProgressLayout.CONTENT_INSTALL,
                            -1,
                            -1
                        )
                    }
                }

                if (project.iconBitmap == null && project.iconUrl != null) {
                    requestIcon(project)
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    if (token != mSearchToken.get()) return@withContext
                    isLoading = false
                    statusText = "Failed to load versions"
                    ProgressKeeper.submitProgress(
                        ProgressLayout.CONTENT_INSTALL,
                        100,
                        0,
                        statusText
                    )
                    launch {
                        delay(2000)
                        ProgressKeeper.submitProgress(ProgressLayout.CONTENT_INSTALL, -1, -1)
                    }
                }
            }
        }
    }

    private fun parseVersions(versions: JsonArray): List<ModrinthVersion> {
        val items = ArrayList<ModrinthVersion>(versions.size())
        for (i in 0 until versions.size()) {
            val v = versions.get(i).asJsonObject ?: continue
            val name = if (v.has("name")) v.get("name").asString else "Version"

            val gameVersions = mutableListOf<String>()
            if (v.has("game_versions") && v.get("game_versions").isJsonArray) {
                val arr = v.getAsJsonArray("game_versions")
                for (j in 0 until arr.size()) gameVersions.add(arr.get(j).asString)
            }

            val loaders = mutableListOf<String>()
            if (v.has("loaders") && v.get("loaders").isJsonArray) {
                val arr = v.getAsJsonArray("loaders")
                for (j in 0 until arr.size()) loaders.add(arr.get(j).asString)
            }

            var url: String? = null
            var filename: String? = null
            var sha1: String? = null
            if (v.has("files") && v.get("files").isJsonArray) {
                val files = v.getAsJsonArray("files")
                if (files.size() > 0) {
                    val f = files.get(0).asJsonObject
                    if (f != null) {
                        if (f.has("url")) url = f.get("url").asString
                        if (f.has("filename")) filename = f.get("filename").asString
                        if (f.has("hashes") && f.get("hashes").isJsonObject) {
                            val hashes = f.getAsJsonObject("hashes")
                            if (hashes.has("sha1")) sha1 = hashes.get("sha1").asString
                        }
                    }
                }
            }
            if (url != null) {
                items.add(ModrinthVersion(name, url, filename, gameVersions, loaders, sha1))
            }
        }
        return items
    }

    fun downloadVersion(context: Context, version: ModrinthVersion, type: ContentInstallerType) {
        if (type == ContentInstallerType.MODPACKS) {
            val project = viewingProject ?: return

            // Copy icon to mod_icons cache for the instance installer to find it
            if (project.iconUrl != null) {
                val iconTag = "${Constants.SOURCE_MODRINTH}_${project.id}"
                val modIconsDir = File(Tools.DIR_CACHE, "mod_icons")
                FileUtils.ensureDirectorySilently(modIconsDir)
                val destIcon = File(modIconsDir, "$iconTag.ca")
                val sourceIcon = File(Tools.DIR_CACHE, "modrinth_icons/" + project.iconUrl.hashCode().toString() + ".png")

                if (sourceIcon.exists()) {
                    try {
                        sourceIcon.copyTo(destIcon, overwrite = true)
                    } catch (e: Exception) {
                        Log.e("ContentInstaller", "Failed to copy icon to mod_icons", e)
                    }
                }
            }

            // Construct models to use the centralized modpack installer
            val modItem = ModItem(
                Constants.SOURCE_MODRINTH,
                true,
                project.id,
                project.title,
                project.description,
                project.iconUrl
            )

            val modDetail = ModDetail(
                modItem,
                arrayOf(version.name),
                arrayOf(version.gameVersions.firstOrNull() ?: ""),
                arrayOf(version.url),
                arrayOf(version.sha1)
            )

            Toast.makeText(context, "Installing modpack as instance...", Toast.LENGTH_SHORT).show()
            ProgressService.startService(context)

            viewModelScope.launch(Dispatchers.IO) {
                try {
                    val api = ModrinthApi()
                    api.installModpack(modDetail, 0)
                    withContext(Dispatchers.Main) {
                        Toast.makeText(context, "Modpack installed!", Toast.LENGTH_LONG).show()
                    }
                } catch (e: Exception) {
                    Log.e("ContentInstaller", "Modpack installation failed", e)
                    withContext(Dispatchers.Main) {
                        Tools.showError(context, e)
                    }
                }
            }
            return
        }

        val target = File(getTargetDir(context, type), version.filename ?: "download")

        Toast.makeText(context, "Downloading in background...", Toast.LENGTH_SHORT).show()
        ProgressService.startService(context)
        ProgressKeeper.submitProgress(
            ProgressLayout.CONTENT_INSTALL,
            0,
            0,
            "Downloading: ${target.name}"
        )

        viewModelScope.launch(Dispatchers.IO) {
            try {
                DownloadUtils.downloadFile(version.url, target)
                withContext(Dispatchers.Main) {
                    ProgressKeeper.submitProgress(
                        ProgressLayout.CONTENT_INSTALL,
                        100,
                        0,
                        "Downloaded: ${target.name}"
                    )
                    Toast.makeText(context, "Saved: ${target.name}", Toast.LENGTH_LONG).show()
                    delay(3000)
                    ProgressKeeper.submitProgress(ProgressLayout.CONTENT_INSTALL, -1, -1)
                }
            } catch (e: IOException) {
                withContext(Dispatchers.Main) {
                    ProgressKeeper.submitProgress(
                        ProgressLayout.CONTENT_INSTALL,
                        100,
                        0,
                        "Failed: ${target.name}"
                    )
                    Tools.showError(context, e)
                    delay(3000)
                    ProgressKeeper.submitProgress(ProgressLayout.CONTENT_INSTALL, -1, -1)
                }
            }
        }
    }

    private fun getTargetDir(context: Context, type: ContentInstallerType): File {
        val instance = Instances.loadSelectedInstance() ?: return context.cacheDir
        val base = instance.gameDirectory ?: return context.cacheDir
        val dotMc = File(base, ".minecraft")
        val finalBase = if (dotMc.exists() && dotMc.isDirectory) dotMc else base

        val subfolder = when (type) {
            ContentInstallerType.MODS -> "mods"
            ContentInstallerType.MODPACKS -> "modpacks"
            ContentInstallerType.SHADERS -> "shaderpacks"
            ContentInstallerType.RESOURCES -> "resourcepacks"
            ContentInstallerType.DATAPACKS -> "datapacks"
        }

        val target = File(finalBase, subfolder)
        FileUtils.ensureDirectorySilently(target)
        return target
    }
}