package de.cloudly.utils

import org.bukkit.plugin.java.JavaPlugin
import java.io.BufferedWriter
import java.io.File
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.UUID
import java.util.logging.Level

/**
 * Dedicated audit logger that writes security-relevant events to a separate log file.
 * Provides thread-safe logging for compliance and security tracking.
 * 
 * @param plugin The JavaPlugin instance for accessing the data folder
 */
class AuditLogger(private val plugin: JavaPlugin) {
    
    private val auditFile: File = File(plugin.dataFolder, "logs/audit.log")
    private var writer: BufferedWriter? = null
    private val lock = Any()
    
    // Timestamp formatter using system default timezone for better readability
    private val timestampFormatter = DateTimeFormatter
        .ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault())
    
    init {
        try {
            // Create logs directory if it doesn't exist
            auditFile.parentFile.mkdirs()
            
            // Open the audit log file in append mode
            writer = auditFile.bufferedWriter(charset = Charsets.UTF_8, bufferSize = 8192).apply {
                // Write a startup marker
                val timestamp = timestampFormatter.format(Instant.now())
                val startupMessage = "[AUDIT] $timestamp - AUDIT_LOG_STARTED - Target: 00000000-0000-0000-0000-000000000000 - Actor: null - Details: Audit logging initialized\n"
                write(startupMessage)
                flush()
            }
            
            plugin.logger.info("Audit logger initialized: ${auditFile.absolutePath}")
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to initialize audit logger", e)
            writer = null
        }
    }
    
    /**
     * Log an audit event to the dedicated audit log file.
     * This method is thread-safe and will not block if the file writer is unavailable.
     * 
     * @param action The action performed (e.g., WHITELIST_ADD, WHITELIST_REMOVE)
     * @param target The UUID of the affected player (or system UUID for global actions)
     * @param actor The UUID of the player or admin who performed the action
     * @param details Additional details about the action
     */
    fun log(action: String, target: UUID, actor: UUID?, details: String?) {
        val timestamp = timestampFormatter.format(Instant.now())
        val logLine = "[AUDIT] $timestamp - $action - Target: $target - Actor: $actor - Details: $details\n"
        
        synchronized(lock) {
            try {
                writer?.let {
                    it.write(logLine)
                    it.flush()
                }
            } catch (e: Exception) {
                plugin.logger.log(Level.WARNING, "Failed to write to audit log", e)
            }
        }
    }
    
    /**
     * Close the audit logger and release resources.
     * Should be called when the plugin is shutting down or reloading.
     */
    fun close() {
        synchronized(lock) {
            try {
                writer?.let {
                    // Write a shutdown marker
                    val timestamp = timestampFormatter.format(Instant.now())
                    val shutdownMessage = "[AUDIT] $timestamp - AUDIT_LOG_STOPPED - Target: 00000000-0000-0000-0000-000000000000 - Actor: null - Details: Audit logging shutdown\n"
                    it.write(shutdownMessage)
                    it.flush()
                    it.close()
                }
                writer = null
                
                if (plugin.config.getBoolean("plugin.debug", false)) {
                    plugin.logger.info("Audit logger closed")
                }
            } catch (e: Exception) {
                plugin.logger.log(Level.WARNING, "Error closing audit logger", e)
            }
        }
    }
}
