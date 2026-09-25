package com.neomelt.mizuki

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

data class GitHubConfig(
    val owner: String,
    val repository: String,
    val branch: String,
)

class GitHubApi {
    suspend fun publish(
        config: GitHubConfig,
        token: String,
        path: String,
        markdown: String,
        message: String,
    ): String = withContext(Dispatchers.IO) {
        val encodedPath = path.split('/').joinToString("/") { encodeSegment(it) }
        val endpoint = "https://api.github.com/repos/${encodeSegment(config.owner)}/${encodeSegment(config.repository)}/contents/$encodedPath"
        val existingSha = getExistingSha(endpoint, config.branch, token)
        val body = JSONObject().apply {
            put("message", message)
            put("content", Base64.encodeToString(markdown.toByteArray(Charsets.UTF_8), Base64.NO_WRAP))
            put("branch", config.branch)
            existingSha?.let { put("sha", it) }
        }
        val response = request(
            method = "PUT",
            url = endpoint,
            token = token,
            body = body.toString(),
        )
        if (response.code !in 200..299) throw GitHubException(response.message(response.code))
        JSONObject(response.body).optJSONObject("content")?.optString("html_url").orEmpty()
    }

    suspend fun validate(config: GitHubConfig, token: String) = withContext(Dispatchers.IO) {
        val user = request("GET", "https://api.github.com/user", token)
        if (user.code !in 200..299) throw GitHubException(user.message(user.code))
        val repository = request(
            "GET",
            "https://api.github.com/repos/${encodeSegment(config.owner)}/${encodeSegment(config.repository)}",
            token,
        )
        if (repository.code !in 200..299) throw GitHubException(repository.message(repository.code))
        if (JSONObject(repository.body).optJSONObject("permissions")?.optBoolean("push", true) == false) {
            throw GitHubException("当前 token 没有这个仓库的写入权限。")
        }
    }

    private fun getExistingSha(endpoint: String, branch: String, token: String): String? {
        val response = request("GET", "$endpoint?ref=${encodeSegment(branch)}", token)
        return when (response.code) {
            200 -> JSONObject(response.body).optString("sha").ifBlank { null }
            404 -> null
            else -> throw GitHubException(response.message(response.code))
        }
    }

    private fun request(method: String, url: String, token: String, body: String? = null): Response {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = method
            connectTimeout = 15_000
            readTimeout = 30_000
            doInput = true
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("Authorization", "Bearer $token")
            setRequestProperty("X-GitHub-Api-Version", "2022-11-28")
            if (body != null) {
                doOutput = true
                setRequestProperty("Content-Type", "application/json")
            }
        }
        return try {
            if (body != null) connection.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            val code = connection.responseCode
            val stream = if (code in 200..399) connection.inputStream else connection.errorStream
            val responseBody = stream?.bufferedReader()?.use { it.readText() }.orEmpty()
            Response(code, responseBody)
        } finally {
            connection.disconnect()
        }
    }

    private data class Response(val code: Int, val body: String) {
        fun message(code: Int): String = runCatching {
            JSONObject(body).optString("message").ifBlank { "GitHub API 返回 HTTP $code" }
        }.getOrDefault("GitHub API 返回 HTTP $code")
    }

    private class GitHubException(message: String) : IllegalStateException(message)

    private fun encodeSegment(value: String): String =
        URLEncoder.encode(value, Charsets.UTF_8.name()).replace("+", "%20")
}
