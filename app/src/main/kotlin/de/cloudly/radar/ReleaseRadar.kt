package de.cloudly.radar

import de.cloudly.CloudlyPaper
import de.cloudly.config.ConfigManager
import de.cloudly.config.LanguageManager
import kotlinx.coroutines.*
import okhttp3.*
import org.bukkit.Bukkit
import org.bukkit.scheduler.BukkitTask
import org.json.JSONArray
import java.io.File
import java.io.IOException
import java.time.Instant
import java.util.concurrent.TimeUnit
import java.util.logging.Level

/**
 * Manages the release radar functionality for checking GitHub releases.
 */
class ReleaseRadar(
    private val plugin: CloudlyPaper,
    private val configManager: ConfigManager,
    private val languageManager: LanguageManager
) {
    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(30, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    
    private var radarTask: BukkitTask? = null
    private var lastKnownRelease: GitHubRelease? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    companion object {
        private const val GITHUB_API_BASE = "https://api.github.com"
        private const val LAST_RELEASE_FILE = "last-release.json"
        
        // Hardcoded repository information
        private const val REPOSITORY_OWNER = "becloudly"
        private const val REPOSITORY_NAME = "cloudlymc"
        
        // Fixed check interval - 6 hours in ticks (20 ticks = 1 second)
        private const val CHECK_INTERVAL_TICKS = 6 * 60 * 60 * 20L // 6 hours
    }

    /**
     * Initialize the release radar.
     */
    fun initialize() {
        val enabled = configManager.getBoolean("release-radar.enabled", true)
        
        if (!enabled) {
            plugin.logger.info(languageManager.getMessage("release_radar.disabled"))
            return
        }

        // Load last known release from file
        loadLastKnownRelease()
        
        // Start the radar
        startRadar()
    }

    /**
     * Stop the release radar.
     */
    fun shutdown() {
        radarTask?.cancel()
        radarTask = null
        coroutineScope.cancel()
        httpClient.dispatcher.executorService.shutdown()
    }

    /**
     * Start the release radar with periodic checking.
     */
    private fun startRadar() {
        // Cancel existing task if any
        radarTask?.cancel()
        
        // Check immediately on startup
        checkForUpdates()
        
        // Schedule periodic task every 6 hours
        radarTask = Bukkit.getScheduler().runTaskTimerAsynchronously(
            plugin,
            Runnable { checkForUpdates() },
            CHECK_INTERVAL_TICKS, // Start after 6 hours
            CHECK_INTERVAL_TICKS // Repeat every 6 hours
        )
    }

    /**
     * Check for new releases from GitHub.
     */
    private fun checkForUpdates() {
        coroutineScope.launch {
            try {
                val releases = fetchReleases()
                val channel = getReleaseChannel()
                val latestRelease = findLatestRelease(releases, channel)
                
                if (latestRelease != null && latestRelease.isNewerThan(lastKnownRelease)) {
                    // New release found - only notify if there's actually a new release
                    notifyNewRelease(latestRelease)
                    lastKnownRelease = latestRelease
                    saveLastKnownRelease(latestRelease)
                }
            } catch (e: Exception) {
                plugin.logger.log(
                    Level.WARNING,
                    languageManager.getMessage("release_radar.check_failed", "error" to (e.message ?: "Unknown error")),
                    e
                )
            }
        }
    }

    /**
     * Get the configured release channel.
     */
    private fun getReleaseChannel(): ReleaseChannel {
        val channelStr = configManager.getString("release-radar.channel", "release")
        return ReleaseChannel.fromString(channelStr)
    }

    /**
     * Fetch releases from GitHub API.
     */
    private suspend fun fetchReleases(): List<GitHubRelease> {
        return withContext(Dispatchers.IO) {
            val url = "${GITHUB_API_BASE}/repos/${REPOSITORY_OWNER}/${REPOSITORY_NAME}/releases"
            val request = Request.Builder()
                .url(url)
                .header("Accept", "application/vnd.github.v3+json")
                .header("User-Agent", "CloudlyMC-Plugin/${plugin.description.version}")
                .build()

            val response = httpClient.newCall(request).execute()
            
            if (!response.isSuccessful) {
                throw IOException("GitHub API request failed: ${response.code} ${response.message}")
            }

            val responseBody = response.body?.string()
                ?: throw IOException("Empty response from GitHub API")

            val jsonArray = JSONArray(responseBody)
            val releases = mutableListOf<GitHubRelease>()

            for (i in 0 until jsonArray.length()) {
                val releaseJson = jsonArray.getJSONObject(i)
                // Skip drafts
                if (!releaseJson.getBoolean("draft")) {
                    releases.add(GitHubRelease.fromJson(releaseJson))
                }
            }

            releases.sortedByDescending { it.publishedAt }
        }
    }

    /**
     * Find the latest release based on the channel configuration.
     */
    private fun findLatestRelease(releases: List<GitHubRelease>, channel: ReleaseChannel): GitHubRelease? {
        return when (channel) {
            ReleaseChannel.RELEASE -> releases.firstOrNull { !it.prerelease }
            ReleaseChannel.PRE_RELEASE -> releases.firstOrNull()
        }
    }

    /**
     * Notify about a new release.
     */
    private fun notifyNewRelease(release: GitHubRelease) {
        // Schedule notification on main thread
        Bukkit.getScheduler().runTask(plugin, Runnable {
            val messageKey = if (release.prerelease) {
                "release_radar.new_prerelease_available"
            } else {
                "release_radar.new_release_available"
            }
            
            val message = languageManager.getMessage(
                messageKey,
                "version" to release.tagName,
                "url" to release.htmlUrl
            )
            
            // Log to console only
            plugin.logger.info(message)
        })
    }

    /**
     * Load the last known release from file.
     */
    private fun loadLastKnownRelease() {
        val file = File(plugin.dataFolder.parentFile, "Cloudly/$LAST_RELEASE_FILE")
        
        if (!file.exists()) {
            return
        }
        
        try {
            val content = file.readText()
            val json = org.json.JSONObject(content)
            lastKnownRelease = GitHubRelease.fromJson(json)
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Failed to load last known release", e)
        }
    }

    /**
     * Save the last known release to file.
     */
    private fun saveLastKnownRelease(release: GitHubRelease) {
        val file = File(plugin.dataFolder.parentFile, "Cloudly/$LAST_RELEASE_FILE")
        
        try {
            file.parentFile.mkdirs()
            val json = org.json.JSONObject().apply {
                put("id", release.id)
                put("tag_name", release.tagName)
                put("name", release.name)
                put("body", release.body)
                put("html_url", release.htmlUrl)
                put("published_at", release.publishedAt.toString())
                put("prerelease", release.prerelease)
                put("draft", release.draft)
            }
            file.writeText(json.toString(2))
        } catch (e: Exception) {
            plugin.logger.log(Level.WARNING, "Failed to save last known release", e)
        }
    }
}
