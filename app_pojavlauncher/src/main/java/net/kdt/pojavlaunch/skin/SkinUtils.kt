package net.kdt.pojavlaunch.skin

import android.content.Context
import android.graphics.*
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.produceState
import androidx.compose.ui.platform.LocalContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import net.kdt.pojavlaunch.authenticator.AuthType
import net.kdt.pojavlaunch.authenticator.accounts.MinecraftAccount
import net.kdt.pojavlaunch.authenticator.accounts.SkinHeadRenderer
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

object SkinUtils {

    private const val TAG = "SkinUtils"

    /**
     * Consolidates skin URL generation for different account types.
     */
    fun getSkinUrl(account: MinecraftAccount?): String? {
        if (account == null) return null

        return when (account.authType) {
            AuthType.LOCAL -> {
                if (account.skinPath != null) "file://${account.skinPath}"
                else null
            }
            AuthType.ELY_BY -> {
                "https://skinsystem.ely.by/skins/${account.username}.png"
            }
            AuthType.MICROSOFT -> {
                val idToUse = if (account.profileId != null && !account.profileId.contains("00000000")) {
                    account.profileId
                } else {
                    account.username
                }
                "https://minotar.net/skin/$idToUse"
            }
            else -> null
        }
    }

    /**
     * Determines the model type for the skin viewer.
     */
    fun getModelType(account: MinecraftAccount?): String {
        return when (account?.skinModel) {
            SkinModelType.ALEX -> "slim"
            else -> "default"
        }
    }

    /**
     * Renders a 3D isometric head from a skin bitmap or file.
     */
    suspend fun renderHead(context: Context, account: MinecraftAccount?): Bitmap? = withContext(Dispatchers.IO) {
        if (account == null) return@withContext loadSteveHead(context)

        try {
            val cachedFace = account.skinFace
            if (cachedFace != null && !cachedFace.isRecycled) {
                return@withContext cachedFace
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to get skinFace for ${account.username}", e)
        }

        if (account.authType == AuthType.LOCAL && account.skinPath != null) {
            val file = File(account.skinPath)
            if (file.exists()) {
                val bitmap = try {
                    BitmapFactory.decodeFile(file.absolutePath)
                } catch (e: Exception) {
                    Log.e(TAG, "Failed to decode skin file ${file.absolutePath}", e)
                    null
                }

                if (bitmap != null) {
                    val head = try {
                        SkinHeadRenderer().render(120, bitmap)
                    } catch (e: Exception) {
                        Log.e(TAG, "Renderer error for local skin", e)
                        null
                    }

                    if (head != null) {
                        if (head != bitmap) bitmap.recycle()
                        return@withContext head
                    }

                    if (bitmap.width == bitmap.height) return@withContext bitmap

                    bitmap.recycle()
                }
            }
        }

        return@withContext loadSteveHead(context)
    }

    /**
     * Renders a 2D front face head from a skin bitmap or file.
     */
    suspend fun renderHead2D(context: Context, account: MinecraftAccount?): Bitmap? = withContext(Dispatchers.IO) {
        val skinUrl = getSkinUrl(account)
        val skinBitmap = if (skinUrl == null) {
            try { context.assets.open("steve.png").use { BitmapFactory.decodeStream(it) } } catch (e: Exception) { null }
        } else if (skinUrl.startsWith("file://")) {
            val path = skinUrl.substring(7)
            try { BitmapFactory.decodeFile(path) } catch (e: Exception) { null }
        } else {
            downloadBitmap(skinUrl)
        } ?: try { context.assets.open("steve.png").use { BitmapFactory.decodeStream(it) } } catch (e: Exception) { null }

        if (skinBitmap == null) return@withContext null

        val ratio = skinBitmap.width / 64
        val result = Bitmap.createBitmap(8 * ratio, 8 * ratio, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(result)

        // Draw base head (8, 8, 8x8)
        canvas.drawBitmap(skinBitmap, Rect(8 * ratio, 8 * ratio, 16 * ratio, 16 * ratio), Rect(0, 0, 8 * ratio, 8 * ratio), null)
        // Draw overlay (40, 8, 8x8)
        canvas.drawBitmap(skinBitmap, Rect(40 * ratio, 8 * ratio, 48 * ratio, 16 * ratio), Rect(0, 0, 8 * ratio, 8 * ratio), null)

        if (skinBitmap.width != 64 || skinBitmap.height != 64) {
             // If we downloaded from minotar or something that might return a head instead of a full skin
             // we should check if it's already a square and looks like a head.
             // But Minotar /skin/ returns full skin.
        }

        skinBitmap.recycle()
        return@withContext result
    }

    private fun downloadBitmap(urlString: String): Bitmap? {
        return try {
            val url = URL(urlString)
            val connection = url.openConnection() as HttpURLConnection
            connection.doInput = true
            connection.connect()
            connection.inputStream.use { BitmapFactory.decodeStream(it) }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to download skin from $urlString", e)
            null
        }
    }

    private fun loadSteveHead(context: Context): Bitmap? {
        try {

            val steveBitmap = try {
                context.assets.open("steve.png").use { inputStream ->
                    BitmapFactory.decodeStream(inputStream)
                }
            } catch (e: Exception) {
                Log.w(TAG, "steve.png not found in assets, trying drawable fallback")
                null
            }

            if (steveBitmap != null) {
                val head = try {
                    SkinHeadRenderer().render(120, steveBitmap)
                } catch (e: Exception) {
                    Log.e(TAG, "Renderer failed to render steve.png", e)
                    null
                }

                if (head != null) {
                    if (head != steveBitmap) steveBitmap.recycle()
                    return head
                }

                if (steveBitmap.width == steveBitmap.height) {
                    return steveBitmap
                }
                steveBitmap.recycle()
            }

            val resId = context.resources.getIdentifier("head_steve", "drawable", context.packageName)
            if (resId != 0) {
                return BitmapFactory.decodeResource(context.resources, resId)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Fatal error loading steve head", e)
        }
        return null
    }

    /**
     * Composable helper to get a skin head state.
     */
    @Composable
    fun rememberSkinHead(account: MinecraftAccount?): State<Bitmap?> {
        val context = LocalContext.current
        // Use a more stable key to prevent re-rendering when the account object is replaced with the same user
        val stableKey = "${account?.profileId}_${account?.skinPath}_${account?.username}"
        return produceState<Bitmap?>(initialValue = null, stableKey) {
            value = renderHead(context, account)
        }
    }

    /**
     * Composable helper to get a 2D skin head state.
     */
    @Composable
    fun rememberSkinHead2D(account: MinecraftAccount?): State<Bitmap?> {
        val context = LocalContext.current
        // Use a more stable key to prevent re-rendering when the account object is replaced with the same user
        val stableKey = "${account?.profileId}_${account?.skinPath}_${account?.username}_2D"
        return produceState<Bitmap?>(initialValue = null, stableKey) {
            value = renderHead2D(context, account)
        }
    }
}
