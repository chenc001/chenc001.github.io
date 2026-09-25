package com.neomelt.mizuki

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.content.FileProvider
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject

data class AppUpdate(
    val version: String,
    val assetName: String,
    val downloadUrl: String,
    val sizeBytes: Long,
)

object AppUpdateService {
    private const val RELEASE_API = "https://api.github.com/repos/chenc001/chenc001.github.io/releases/latest"
    private const val FILE_PROVIDER_SUFFIX = ".fileprovider"

    suspend fun checkForUpdate(): AppUpdate? = withContext(Dispatchers.IO) {
        runCatching {
            val response = request(RELEASE_API)
            if (response.code !in 200..299) return@runCatching null
            val release = JSONObject(response.body)
            if (release.optBoolean("draft") || release.optBoolean("prerelease")) return@runCatching null
            val version = release.optString("tag_name")
            if (!isNewer(version, BuildConfig.VERSION_NAME)) return@runCatching null
            val assets = release.optJSONArray("assets") ?: return@runCatching null
            for (index in 0 until assets.length()) {
                val asset = assets.optJSONObject(index) ?: continue
                val name = asset.optString("name")
                val url = asset.optString("browser_download_url")
                if (name.endsWith(".apk", ignoreCase = true) && url.startsWith("https://")) {
                    return@runCatching AppUpdate(version, name, url, asset.optLong("size"))
                }
            }
            null
        }.getOrNull()
    }

    suspend fun download(
        context: Context,
        update: AppUpdate,
        onProgress: suspend (Int) -> Unit,
    ): Uri = withContext(Dispatchers.IO) {
        val directory = File(context.cacheDir, "updates").apply { mkdirs() }
        val target = File(directory, update.assetName)
        val connection = (URL(update.downloadUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 60_000
            setRequestProperty("Accept", "application/vnd.android.package-archive")
        }
        try {
            val code = connection.responseCode
            if (code !in 200..299) throw IllegalStateException("下载更新失败：HTTP $code")
            val total = connection.contentLengthLong.takeIf { it > 0 } ?: update.sizeBytes
            var completed = 0L
            var lastProgress = -1
            connection.inputStream.use { input ->
                FileOutputStream(target).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val count = input.read(buffer)
                        if (count < 0) break
                        output.write(buffer, 0, count)
                        completed += count
                        if (total > 0) {
                            val progress = ((completed * 100) / total).toInt().coerceIn(0, 100)
                            if (progress != lastProgress) {
                                lastProgress = progress
                                withContext(Dispatchers.Main) { onProgress(progress) }
                            }
                        }
                    }
                }
            }
            withContext(Dispatchers.Main) { onProgress(100) }
            FileProvider.getUriForFile(context, context.packageName + FILE_PROVIDER_SUFFIX, target)
        } catch (error: Throwable) {
            target.delete()
            throw error
        } finally {
            connection.disconnect()
        }
    }

    fun canInstallPackages(context: Context): Boolean =
        Build.VERSION.SDK_INT < Build.VERSION_CODES.O || context.packageManager.canRequestPackageInstalls()

    fun openInstallPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startActivity(
                Intent(
                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                    Uri.parse("package:${context.packageName}"),
                ).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            )
        }
    }

    fun install(context: Context, uri: Uri) {
        context.startActivity(
            Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(uri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_GRANT_READ_URI_PERMISSION)
            },
        )
    }

    private fun request(url: String): Response {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 20_000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
        }
        return try {
            val code = connection.responseCode
            val stream = if (code in 200..399) connection.inputStream else connection.errorStream
            Response(code, stream?.bufferedReader()?.use { it.readText() }.orEmpty())
        } finally {
            connection.disconnect()
        }
    }

    private fun isNewer(remote: String, current: String): Boolean {
        fun parse(value: String): List<Int> = value.removePrefix("v").split(".").map { part ->
            part.takeWhile { it.isDigit() }.toIntOrNull() ?: 0
        }
        val remoteParts = parse(remote)
        val currentParts = parse(current)
        for (index in 0 until maxOf(remoteParts.size, currentParts.size)) {
            val remotePart = remoteParts.getOrElse(index) { 0 }
            val currentPart = currentParts.getOrElse(index) { 0 }
            if (remotePart != currentPart) return remotePart > currentPart
        }
        return false
    }

    private data class Response(val code: Int, val body: String)
}
