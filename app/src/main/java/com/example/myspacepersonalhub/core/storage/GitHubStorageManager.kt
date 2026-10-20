package com.example.myspacepersonalhub.core.storage

import android.content.Context
import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.TimeUnit

data class GitHubRepoInfo(
    val fullName: String,
    val isPrivate: Boolean,
    val defaultBranch: String,
    val description: String?
)

data class GitHubUploadResult(
    val path: String,
    val sha: String,
    val rawUrl: String,
    val downloadUrl: String
)

class GitHubStorageManager(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(60, TimeUnit.SECONDS)
        .writeTimeout(60, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    suspend fun testConnection(
        token: String,
        owner: String,
        repo: String
    ): Result<GitHubRepoInfo> = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.github.com/repos/${owner.trim()}/${repo.trim()}"
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer ${token.trim()}")
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "MySpace-PersonalHub-Android")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val errorMsg = try {
                        JSONObject(bodyStr).optString("message", "HTTP ${response.code}")
                    } catch (_: Exception) {
                        "HTTP ${response.code}: $bodyStr"
                    }
                    return@withContext Result.failure(Exception("GitHub error: $errorMsg"))
                }

                val json = JSONObject(bodyStr)
                Result.success(
                    GitHubRepoInfo(
                        fullName = json.optString("full_name"),
                        isPrivate = json.optBoolean("private", true),
                        defaultBranch = json.optString("default_branch", "main"),
                        description = json.optString("description")
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private suspend fun getFileSha(
        path: String,
        token: String,
        owner: String,
        repo: String,
        branch: String
    ): String? = withContext(Dispatchers.IO) {
        try {
            val url = "https://api.github.com/repos/${owner.trim()}/${repo.trim()}/contents/${path.trim()}?ref=${branch.trim()}"
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer ${token.trim()}")
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "MySpace-PersonalHub-Android")
                .get()
                .build()

            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string() ?: return@use null
                    val json = JSONObject(bodyStr)
                    return@use json.optString("sha").ifBlank { null }
                }
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    suspend fun uploadFile(
        path: String,
        bytes: ByteArray,
        commitMessage: String,
        token: String,
        owner: String,
        repo: String,
        branch: String = "main"
    ): Result<GitHubUploadResult> = withContext(Dispatchers.IO) {
        try {
            val cleanPath = path.trim().removePrefix("/")
            val cleanBranch = branch.trim().ifBlank { "main" }
            val existingSha = getFileSha(cleanPath, token, owner, repo, cleanBranch)

            val base64Content = Base64.encodeToString(bytes, Base64.NO_WRAP)

            val payload = JSONObject().apply {
                put("message", commitMessage)
                put("content", base64Content)
                put("branch", cleanBranch)
                if (existingSha != null) {
                    put("sha", existingSha)
                }
            }

            val url = "https://api.github.com/repos/${owner.trim()}/${repo.trim()}/contents/$cleanPath"
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer ${token.trim()}")
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "MySpace-PersonalHub-Android")
                .put(payload.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                val bodyStr = response.body?.string() ?: ""
                if (!response.isSuccessful) {
                    val errorMsg = try {
                        JSONObject(bodyStr).optString("message", "HTTP ${response.code}")
                    } catch (_: Exception) {
                        "HTTP ${response.code}: $bodyStr"
                    }
                    return@withContext Result.failure(Exception("GitHub Upload Failed: $errorMsg"))
                }

                val json = JSONObject(bodyStr)
                val contentObj = json.optJSONObject("content")
                val sha = contentObj?.optString("sha") ?: ""
                val downloadUrl = contentObj?.optString("download_url") ?: ""
                val rawUrl = "https://raw.githubusercontent.com/${owner.trim()}/${repo.trim()}/$cleanBranch/$cleanPath"

                Result.success(
                    GitHubUploadResult(
                        path = cleanPath,
                        sha = sha,
                        rawUrl = rawUrl,
                        downloadUrl = downloadUrl.ifBlank { rawUrl }
                    )
                )
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun downloadFile(
        urlOrPath: String,
        destinationFile: File,
        token: String? = null,
        owner: String? = null,
        repo: String? = null,
        branch: String = "main"
    ): Result<File> = withContext(Dispatchers.IO) {
        try {
            val finalUrl = when {
                urlOrPath.startsWith("http://") || urlOrPath.startsWith("https://") -> urlOrPath
                owner != null && repo != null -> "https://raw.githubusercontent.com/${owner.trim()}/${repo.trim()}/$branch/${urlOrPath.trim().removePrefix("/")}"
                else -> return@withContext Result.failure(IllegalArgumentException("Invalid URL or path for GitHub download"))
            }

            val requestBuilder = Request.Builder()
                .url(finalUrl)
                .header("User-Agent", "MySpace-PersonalHub-Android")

            if (!token.isNullOrBlank()) {
                requestBuilder.header("Authorization", "Bearer ${token.trim()}")
            }

            client.newCall(requestBuilder.build()).execute().use { response ->
                if (!response.isSuccessful) {
                    return@withContext Result.failure(Exception("Failed to download from GitHub: HTTP ${response.code}"))
                }

                val body = response.body ?: return@withContext Result.failure(Exception("Empty body from GitHub"))
                destinationFile.parentFile?.mkdirs()
                FileOutputStream(destinationFile).use { out ->
                    body.byteStream().copyTo(out)
                }

                Result.success(destinationFile)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteFile(
        path: String,
        commitMessage: String,
        token: String,
        owner: String,
        repo: String,
        branch: String = "main"
    ): Result<Unit> = withContext(Dispatchers.IO) {
        try {
            val cleanPath = path.trim().removePrefix("/")
            val cleanBranch = branch.trim().ifBlank { "main" }
            val existingSha = getFileSha(cleanPath, token, owner, repo, cleanBranch)
                ?: return@withContext Result.success(Unit) // already doesn't exist

            val payload = JSONObject().apply {
                put("message", commitMessage)
                put("sha", existingSha)
                put("branch", cleanBranch)
            }

            val url = "https://api.github.com/repos/${owner.trim()}/${repo.trim()}/contents/$cleanPath"
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer ${token.trim()}")
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "MySpace-PersonalHub-Android")
                .delete(payload.toString().toRequestBody(jsonMediaType))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful && response.code != 404) {
                    val bodyStr = response.body?.string() ?: ""
                    return@withContext Result.failure(Exception("GitHub Delete Failed: HTTP ${response.code} $bodyStr"))
                }
                Result.success(Unit)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
