package de.cloudly.storage.migration

import de.cloudly.storage.core.DataRepository
import de.cloudly.storage.factory.StorageFactory
import de.cloudly.storage.config.StorageConfig
import de.cloudly.whitelist.model.WhitelistPlayer
import de.cloudly.whitelist.storage.WhitelistStorage
import org.bukkit.plugin.java.JavaPlugin
import java.util.UUID
import java.util.logging.Level

/**
 * Migration utility to help transition from the old whitelist-specific storage
 * to the new generic storage system.
 */
class WhitelistMigration(private val plugin: JavaPlugin) {
    
    /**
     * Migrate data from old WhitelistStorage to new generic storage system.
     * 
     * @param oldStorage The existing whitelist storage to migrate from
     * @param storageConfig The new storage configuration
     * @param storageFactory The storage factory to create new repositories
     * @return The new whitelist repository, or null if migration failed
     */
    fun migrateWhitelistStorage(
        oldStorage: WhitelistStorage,
        storageConfig: StorageConfig,
        storageFactory: StorageFactory
    ): DataRepository<WhitelistPlayer>? {
        
        plugin.logger.info("Starting whitelist storage migration...")
        
        try {
            // Create new repository for whitelist players
            val deserializer: (String, String) -> WhitelistPlayer? = { data, _ ->
                WhitelistPlayer.deserialize(data)
            }
            
            val newRepository = storageFactory.createRepository("whitelist", storageConfig, deserializer)
            
            if (!newRepository.initialize()) {
                plugin.logger.severe("Failed to initialize new whitelist repository")
                return null
            }
            
            // Get all players from old storage
            val oldPlayers = oldStorage.getAllPlayers()
            plugin.logger.info("Migrating ${oldPlayers.size} whitelist players...")
            
            var migratedCount = 0
            var failedCount = 0
            
            // Migrate each player
            oldPlayers.forEach { player ->
                try {
                    val key = player.uuid.toString()
                    if (newRepository.store(key, player)) {
                        migratedCount++
                    } else {
                        plugin.logger.warning("Failed to migrate player: ${player.username} (${player.uuid})")
                        failedCount++
                    }
                } catch (e: Exception) {
                    plugin.logger.log(Level.WARNING, "Error migrating player: ${player.username} (${player.uuid})", e)
                    failedCount++
                }
            }
            
            plugin.logger.info("Migration completed: $migratedCount players migrated, $failedCount failed")
            
            return if (failedCount == 0) {
                newRepository
            } else {
                plugin.logger.warning("Migration completed with errors. New repository created but some data may be missing.")
                newRepository
            }
            
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to migrate whitelist storage", e)
            return null
        }
    }
    
    /**
     * Create a backup of the old storage before migration.
     * 
     * @param oldStorage The existing whitelist storage
     * @param backupPath The path where the backup should be stored
     * @return true if backup was successful, false otherwise
     */
    fun backupOldStorage(oldStorage: WhitelistStorage, backupPath: String): Boolean {
        return try {
            // This would depend on the old storage implementation
            // For now, we'll create a simple JSON backup
            val players = oldStorage.getAllPlayers()
            val backupData = players.map { player ->
                mapOf(
                    "uuid" to player.uuid.toString(),
                    "username" to player.username,
                    "addedBy" to player.addedBy?.toString(),
                    "addedAt" to player.addedAt.epochSecond,
                    "reason" to player.reason,
                    "discordConnection" to player.discordConnection?.let { discord ->
                        mapOf(
                            "discordId" to discord.discordId,
                            "discordUsername" to discord.discordUsername,
                            "verified" to discord.verified,
                            "connectedAt" to discord.connectedAt.epochSecond,
                            "verifiedAt" to discord.verifiedAt?.epochSecond
                        )
                    }
                )
            }
            
            val backupFile = java.io.File(plugin.dataFolder, backupPath)
            backupFile.parentFile?.mkdirs()
            
            val gson = com.google.gson.GsonBuilder().setPrettyPrinting().create()
            val json = gson.toJson(backupData)
            
            java.nio.file.Files.write(backupFile.toPath(), json.toByteArray())
            plugin.logger.info("Created backup of old whitelist storage: ${backupFile.absolutePath}")
            true
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Failed to backup old storage", e)
            false
        }
    }
    
    /**
     * Verify that the migration was successful by comparing data.
     * 
     * @param oldStorage The old storage to compare against
     * @param newRepository The new repository to verify
     * @return true if all data matches, false otherwise
     */
    fun verifyMigration(
        oldStorage: WhitelistStorage,
        newRepository: DataRepository<WhitelistPlayer>
    ): Boolean {
        
        try {
            val oldPlayers = oldStorage.getAllPlayers()
            val newPlayers = newRepository.getAll()
            
            plugin.logger.info("Verifying migration: ${oldPlayers.size} old players, ${newPlayers.size} new players")
            
            if (oldPlayers.size != newPlayers.size) {
                plugin.logger.warning("Player count mismatch: ${oldPlayers.size} != ${newPlayers.size}")
                return false
            }
            
            var mismatchCount = 0
            
            oldPlayers.forEach { oldPlayer ->
                val key = oldPlayer.uuid.toString()
                val newPlayer = newRepository.retrieve(key)
                
                if (newPlayer == null) {
                    plugin.logger.warning("Player not found in new storage: ${oldPlayer.username} (${oldPlayer.uuid})")
                    mismatchCount++
                } else if (!comparePlayerData(oldPlayer, newPlayer)) {
                    plugin.logger.warning("Player data mismatch: ${oldPlayer.username} (${oldPlayer.uuid})")
                    mismatchCount++
                }
            }
            
            if (mismatchCount == 0) {
                plugin.logger.info("Migration verification successful: All data matches")
                return true
            } else {
                plugin.logger.warning("Migration verification failed: $mismatchCount mismatches found")
                return false
            }
            
        } catch (e: Exception) {
            plugin.logger.log(Level.SEVERE, "Error during migration verification", e)
            return false
        }
    }
    
    /**
     * Compare two WhitelistPlayer objects for equality.
     */
    private fun comparePlayerData(player1: WhitelistPlayer, player2: WhitelistPlayer): Boolean {
        return player1.uuid == player2.uuid &&
                player1.username == player2.username &&
                player1.addedBy == player2.addedBy &&
                player1.addedAt == player2.addedAt &&
                player1.reason == player2.reason &&
                compareDiscordConnection(player1.discordConnection, player2.discordConnection)
    }
    
    /**
     * Compare two DiscordConnection objects for equality.
     */
    private fun compareDiscordConnection(
        discord1: de.cloudly.whitelist.model.DiscordConnection?,
        discord2: de.cloudly.whitelist.model.DiscordConnection?
    ): Boolean {
        return when {
            discord1 == null && discord2 == null -> true
            discord1 == null || discord2 == null -> false
            else -> discord1.discordId == discord2.discordId &&
                    discord1.discordUsername == discord2.discordUsername &&
                    discord1.verified == discord2.verified &&
                    discord1.connectedAt == discord2.connectedAt &&
                    discord1.verifiedAt == discord2.verifiedAt
        }
    }
}
