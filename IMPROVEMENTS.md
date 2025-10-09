# 🔍 CloudlyMC Code Analysis & Improvement Suggestions

This document contains a comprehensive analysis of the CloudlyMC plugin codebase, identifying potential bugs, performance issues, security concerns, code quality improvements, and new features to implement.

**Analysis Date:** 2024  
**Version Analyzed:** 0.0.1-alpha_11

---

## 🐛 Potential Bugs & Errors

### 1. Uninitialized Property Access in CloudlyPaper
**Location:** `CloudlyPaper.kt` lines 33-49  
**Severity:** High  
**Issue:** Public getter methods don't check if `lateinit` properties are initialized before access.

```kotlin
fun getWhitelistService(): WhitelistService {
    return whitelistService  // Could throw UninitializedPropertyAccessException
}
```

**Impact:** If these methods are called before `onEnable()` completes, the plugin will crash.

**Suggested Fix:**
```kotlin
fun getWhitelistService(): WhitelistService {
    if (!::whitelistService.isInitialized) {
        throw IllegalStateException("Plugin not fully initialized")
    }
    return whitelistService
}
```

---

### 2. ~~Cache Memory Leak in DiscordService~~ ✅ FIXED
**Location:** `DiscordService.kt` line 295  
**Severity:** Medium  
**Issue:** The `isExpired()` method checks cache expiration but doesn't clean up expired entries.

**Impact:** Cache grows indefinitely, consuming memory unnecessarily.

**Status:** ✅ **FIXED** - Implemented automatic cache cleanup in version 0.0.1-alpha_11
- Added `startCacheCleanup()` method that schedules periodic cleanup task
- Task runs every minute (1200 ticks) to remove expired entries
- Properly cancelled in `shutdown()` to prevent resource leaks
- Uses `SchedulerUtils.runTaskTimerAsynchronously` for cross-platform compatibility

**Implementation:**
```kotlin
// Add periodic cleanup task
private fun startCacheCleanup() {
    cacheCleanupTask = SchedulerUtils.runTaskTimerAsynchronously(plugin, Runnable {
        val removed = userCache.entries.removeIf { it.value.isExpired(cacheDuration) }
        if (removed && configManager.getBoolean("plugin.debug", false)) {
            plugin.logger.info("Discord cache cleanup: removed expired entries")
        }
    }, 20 * 60, 20 * 60) // Every minute (1200 ticks)
}
```

---

### 3. Concurrent Write Issues in JsonDataStorage
**Location:** `JsonDataStorage.kt` line 61  
**Severity:** High  
**Issue:** `saveData()` is called on every `store()` operation without file locking. Multiple concurrent writes can corrupt data.

**Impact:** Data corruption in high-concurrency scenarios.

**Suggested Fix:** Implement write batching or file locking:
```kotlin
private val writeLock = ReentrantReadWriteLock()

private fun saveData() {
    writeLock.writeLock().lock()
    try {
        // existing save logic
    } finally {
        writeLock.writeLock().unlock()
    }
}
```

---

### 4. Reflection Performance in SchedulerUtils
**Location:** `SchedulerUtils.kt` lines 84-94  
**Severity:** Medium  
**Issue:** Method lookups via reflection occur on every scheduler call, causing performance overhead.

**Impact:** Reduced performance for frequently called scheduling operations.

**Suggested Fix:** Cache reflected Method objects:
```kotlin
private var runMethod: Method? = null

fun runTask(plugin: JavaPlugin, task: Runnable): BukkitTask? {
    initialize()
    
    return if (isFolia == true) {
        if (runMethod == null) {
            runMethod = foliaGlobalRegionScheduler?.javaClass?.getMethod(...)
        }
        // Use cached method
    }
}
```

---

### 5. Event Listener Double Registration
**Location:** `WhitelistService.kt` line 181  
**Severity:** Low  
**Issue:** Try-catch block silently ignores `IllegalArgumentException` when registering event listener.

**Impact:** Could hide other registration errors.

**Suggested Fix:** Track registration state explicitly:
```kotlin
private var listenerRegistered = false

fun enable(enable: Boolean) {
    // ...
    if (enable && !listenerRegistered) {
        plugin.server.pluginManager.registerEvents(this, plugin)
        listenerRegistered = true
    }
}
```

---

### 6. Magic Number Clarity Issue
**Location:** `SchedulerUtils.kt` line 20  
**Severity:** Low  
**Issue:** Comment says "Replace 50 with correct value if needed" - unclear if 50 is correct.

**Impact:** Confusion about correct timing values.

**Suggested Fix:** Clarify with better documentation:
```kotlin
// 1 Minecraft tick = 50ms (20 ticks per second)
const val TICKS_TO_MILLISECONDS = 50L
```

---

## ⚡ Performance Issues

### 1. Inefficient File I/O in JSON Storage
**Location:** `JsonDataStorage.kt`  
**Severity:** High  
**Issue:** Every `store()` call writes the entire file to disk.

**Impact:** Severe performance degradation with frequent writes.

**Suggested Fix:** Implement write-back caching:
```kotlin
private val pendingWrites = AtomicBoolean(false)

override fun store(key: String, item: String): Boolean {
    data[key] = item
    pendingWrites.set(true)
    return true
}

// Flush periodically
private fun startPeriodicFlush() {
    SchedulerUtils.runTaskTimerAsynchronously(plugin, Runnable {
        if (pendingWrites.compareAndSet(true, false)) {
            saveData()
        }
    }, 20 * 5, 20 * 5) // Every 5 seconds
}
```

---

### 2. Missing Connection Pooling in MySQL
**Location:** `MysqlDataStorage.kt`  
**Severity:** High  
**Issue:** Uses single connection without pooling.

**Impact:** Poor performance and connection exhaustion under load.

**Suggested Fix:** Implement HikariCP connection pooling:
```kotlin
dependencies {
    implementation("com.zaxxer:HikariCP:5.1.0")
}

private lateinit var dataSource: HikariDataSource

override fun initialize(): Boolean {
    val config = HikariConfig().apply {
        jdbcUrl = buildConnectionUrl()
        username = this@MysqlDataStorage.username
        password = this@MysqlDataStorage.password
        maximumPoolSize = 10
        minimumIdle = 2
    }
    dataSource = HikariDataSource(config)
}
```

---

### 3. No Batch Operations in Storage Layer
**Location:** All storage implementations  
**Severity:** Medium  
**Issue:** No support for batch insert/update operations.

**Impact:** Inefficient when processing multiple records.

**Suggested Fix:** Add batch operation methods:
```kotlin
interface DataStorage<T> {
    fun storeAll(items: Map<String, T>): Boolean
    fun removeAll(keys: Set<String>): Boolean
}
```

---

### 4. Discord API Rate Limiting Missing ✅ **RESOLVED**
**Location:** `DiscordService.kt`  
**Severity:** Medium  
**Status:** ✅ Implemented in commit 997c81c  
**Issue:** No rate limiting or exponential backoff for API calls.

**Impact:** Could hit Discord rate limits and get blocked.

**Resolution:** Rate limiting has been successfully implemented with:
- Semaphore(5) for max 5 concurrent requests
- AtomicLong for thread-safe timestamp tracking
- rateLimit() suspend function wrapping all Discord API calls
- Minimum 200ms delay between requests

---

### 5. Unoptimized getAllPlayers() Query
**Location:** `WhitelistService.kt` line 143  
**Severity:** Medium  
**Issue:** Loads entire whitelist into memory at once.

**Impact:** Memory issues with large whitelists (1000+ players).

**Suggested Fix:** Add pagination support:
```kotlin
fun getPlayers(offset: Int, limit: Int): List<WhitelistPlayer>
fun getPlayerCount(): Long
```

---

## 🔒 Security Concerns

### 1. Sensitive Token Logging
**Location:** `DiscordService.kt` line 63  
**Severity:** Medium  
**Issue:** Logs token length which could leak information.

```kotlin
plugin.logger.info("Discord config loaded - Token length: ${botToken?.length ?: 0}")
```

**Impact:** Information leakage in logs.

**Suggested Fix:** Remove token-related logging:
```kotlin
plugin.logger.info("Discord config loaded successfully")
```

---

### 2. Plain Text Credential Storage
**Location:** `ConfigManager.kt`  
**Severity:** High  
**Issue:** Discord tokens and MySQL passwords stored in plain text.

**Impact:** Credentials exposed if config file is compromised.

**Suggested Fix:** 
1. Support environment variables: `${DISCORD_TOKEN}`
2. Add encryption option for sensitive values
3. Warn if file permissions are too open (readable by others)

---

### 3. SQL Injection in Backup Method
**Location:** `MysqlDataStorage.kt` lines 243-256  
**Severity:** Medium  
**Issue:** Table name is not sanitized in backup SQL generation.

```kotlin
backupData.append("INSERT INTO $tableName ...")
```

**Impact:** If tableName is user-controlled, SQL injection is possible.

**Suggested Fix:** Validate table name against allowed characters:
```kotlin
private fun validateTableName(name: String): String {
    require(name.matches(Regex("[a-zA-Z0-9_]+"))) {
        "Invalid table name: $name"
    }
    return name
}
```

---

### 4. Missing Command Cooldowns
**Location:** `CloudlyCommand.kt`  
**Severity:** Low  
**Issue:** Discord verification commands have no rate limiting.

**Impact:** API abuse through command spam.

**Suggested Fix:** Implement per-player cooldowns:
```kotlin
private val cooldowns = ConcurrentHashMap<UUID, Long>()

fun checkCooldown(player: Player, seconds: Int): Boolean {
    val now = System.currentTimeMillis()
    val lastUse = cooldowns[player.uniqueId] ?: 0
    return if (now - lastUse >= seconds * 1000) {
        cooldowns[player.uniqueId] = now
        true
    } else {
        false
    }
}
```

---

### 5. No Audit Logging for Whitelist Changes
**Location:** `WhitelistService.kt`  
**Severity:** Medium  
**Issue:** Whitelist modifications aren't logged with actor information.

**Impact:** Can't trace who made changes for security/compliance.

**Suggested Fix:** Add comprehensive audit logging:
```kotlin
private fun logAuditEvent(action: String, target: UUID, actor: UUID?, details: String?) {
    val timestamp = Instant.now()
    plugin.logger.info("[AUDIT] $timestamp - $action - Target: $target - Actor: $actor - Details: $details")
    // Also store in audit log file or database
}
```

---

## 🏗️ Code Quality & Maintainability

### 1. Resource Cleanup Issues
**Location:** `CloudlyCommand.kt`, `CloudlyPaper.kt`  
**Severity:** Medium  
**Issue:** `CloudlyCommand` creates its own `CoroutineScope` that's never cancelled.

**Impact:** Coroutines may continue running after plugin disable.

**Suggested Fix:** Track and cancel all coroutine scopes:
```kotlin
class CloudlyCommand(private val plugin: CloudlyPaper) : CommandExecutor, TabCompleter {
    private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    fun shutdown() {
        coroutineScope.cancel()
    }
}

// In CloudlyPaper.onDisable():
cloudlyCommand.shutdown()
```

---

### 2. Inconsistent Error Handling
**Location:** Throughout codebase  
**Severity:** Low  
**Issue:** Some methods return `Boolean`, some throw exceptions, some return `null`.

**Impact:** Difficult to handle errors consistently.

**Suggested Fix:** Standardize on sealed class result types:
```kotlin
sealed class Result<out T> {
    data class Success<T>(val value: T) : Result<T>()
    data class Failure(val error: StorageException) : Result<Nothing>()
}
```

---

### 3. Missing KDoc Documentation
**Location:** Multiple files  
**Severity:** Low  
**Issue:** Many complex methods lack documentation.

**Impact:** Harder for contributors to understand code.

**Suggested Fix:** Add comprehensive KDoc:
```kotlin
/**
 * Verifies if a Discord user exists and is a member of the configured server.
 * 
 * This method performs the following steps:
 * 1. Checks cache for recent verification results
 * 2. Searches Discord guild for user by username
 * 3. Verifies user membership in configured guild
 * 
 * @param discordUsername The Discord username to verify (case-insensitive)
 * @return [DiscordVerificationResult] indicating success or specific failure reason
 * @throws IOException if Discord API is unreachable
 */
suspend fun verifyDiscordUser(discordUsername: String): DiscordVerificationResult
```

---

### 4. Tight Coupling
**Location:** Throughout codebase  
**Severity:** Medium  
**Issue:** Services directly access plugin instance methods.

**Impact:** Hard to test in isolation.

**Suggested Fix:** Use dependency injection:
```kotlin
interface ConfigProvider {
    fun getBoolean(path: String, default: Boolean): Boolean
    fun getString(path: String, default: String): String
}

class WhitelistService(
    private val plugin: JavaPlugin,
    private val configProvider: ConfigProvider
) {
    // Now testable with mock ConfigProvider
}
```

---

### 5. No Configuration Validation
**Location:** `ConfigManager.kt`  
**Severity:** Medium  
**Issue:** Config values are read but not validated.

**Impact:** Invalid values can cause runtime errors.

**Suggested Fix:** Add validation layer:
```kotlin
class ConfigValidator {
    fun validateInt(value: Int, min: Int, max: Int, path: String) {
        require(value in min..max) {
            "Config value at '$path' must be between $min and $max, got $value"
        }
    }
    
    fun validateString(value: String, pattern: Regex, path: String) {
        require(value.matches(pattern)) {
            "Config value at '$path' has invalid format"
        }
    }
}
```

---

## 🚀 New Features & Enhancements

### 1. Monitoring & Metrics System
**Priority:** Medium  
**Description:** Add metrics collection for monitoring plugin health and usage.

**Implementation Ideas:**
- Track command usage statistics
- Monitor API call latency and error rates
- Storage operation metrics (reads/writes per second)
- Active player counts and whitelist size
- Integration with Micrometer or custom metrics endpoint

**Example:**
```kotlin
interface MetricsCollector {
    fun incrementCounter(name: String, value: Long = 1)
    fun recordTiming(name: String, durationMs: Long)
    fun setGauge(name: String, value: Double)
}
```

---

### 2. Automated Backup System
**Priority:** High  
**Description:** Implement scheduled backups with retention policies.

**Implementation Ideas:**
- Configurable backup schedule (hourly, daily, weekly)
- Retention policy (keep last N backups)
- Backup to multiple locations (local, FTP, S3)
- Backup verification and integrity checks
- Automatic restoration on corruption detection

**Example Configuration:**
```yaml
backup:
  enabled: true
  schedule: "0 0 * * *"  # Daily at midnight
  retention:
    daily: 7
    weekly: 4
    monthly: 3
  destinations:
    - type: local
      path: "backups/"
    - type: ftp
      host: "backup.example.com"
```

---

### 3. Data Migration System
**Priority:** High  
**Description:** Support for migrating between storage types and versions.

**Implementation Ideas:**
- Detect current storage version
- Automatic schema upgrades
- Convert between storage types (JSON → MySQL)
- Rollback capability
- Migration progress tracking

**Example:**
```kotlin
interface DataMigration {
    val fromVersion: Int
    val toVersion: Int
    suspend fun migrate(context: MigrationContext): Boolean
}

class MigrationManager {
    fun registerMigration(migration: DataMigration)
    suspend fun migrateToLatest(): Boolean
    fun getCurrentVersion(): Int
}
```

---

### 4. Public Plugin API
**Priority:** Medium  
**Description:** Expose API for other plugins to integrate with CloudlyMC.

**Implementation Ideas:**
- Custom Bukkit events (WhitelistAddEvent, etc.)
- Service provider interface
- Hook system for extensions
- API documentation with examples
- Version compatibility guarantees

**Example:**
```kotlin
// Event system
class WhitelistPlayerAddEvent(
    val player: WhitelistPlayer,
    val addedBy: UUID?
) : Event() {
    var isCancelled = false
}

// API interface
interface CloudlyAPI {
    fun getWhitelistService(): WhitelistService
    fun getDiscordService(): DiscordService
    fun addEventListener(listener: CloudlyEventListener)
}
```

---

### 5. Discord Integration Enhancements
**Priority:** High  
**Description:** Expand Discord features beyond basic verification.

**Implementation Ideas:**
- Role-based whitelist (sync Discord roles)
- Slash commands for server management from Discord
- Webhook notifications (joins, leaves, whitelist changes)
- Two-factor authentication via Discord
- OAuth2 authentication flow
- Discord embed messages with rich formatting

**Example Features:**
```kotlin
// Role-based whitelist
discord:
  role_whitelist:
    enabled: true
    required_role: "Whitelisted"
    sync_interval: 300  # seconds
    
// Webhook notifications
discord:
  webhooks:
    - url: "https://discord.com/api/webhooks/..."
      events:
        - player_join
        - player_leave
        - whitelist_add
        - whitelist_remove
```

---

### 6. Advanced Query System
**Priority:** Medium  
**Description:** Add filtering, sorting, and pagination for storage operations.

**Implementation Ideas:**
- Query builder pattern
- Support for complex filters (AND, OR, NOT)
- Multiple sort keys
- Pagination with cursor-based navigation
- Full-text search capability

**Example:**
```kotlin
val query = WhitelistQuery.builder()
    .filter { player ->
        player.username.startsWith("A") && 
        player.discordConnection != null
    }
    .sortBy(WhitelistPlayer::username)
    .limit(20)
    .offset(40)
    .build()

val results = whitelistService.query(query)
```

---

### 7. GUI Pagination & Search
**Priority:** Medium  
**Description:** Improve GUI to handle large whitelists.

**Implementation Ideas:**
- Pagination controls (next/prev page buttons)
- Search bar (type in chat, filters GUI)
- Sort buttons (alphabetical, date added)
- Bulk selection mode
- Filter by Discord connection status

**Example Features:**
- Page size: 45 items (5 rows × 9 columns)
- Search updates GUI in real-time
- Click to sort by column
- Shift-click for bulk selection

---

### 8. Temporary Whitelist Entries
**Priority:** Medium  
**Description:** Support time-limited whitelist entries.

**Implementation Ideas:**
- Add expiration timestamp to WhitelistPlayer
- Automatic removal when expired
- Renewal commands
- Notifications before expiration
- Configurable grace period

**Example:**
```kotlin
data class WhitelistPlayer(
    // ... existing fields
    val expiresAt: Instant? = null,
    val temporary: Boolean = false
)

// Command usage
/cloudly whitelist add Player123 --duration 7d
/cloudly whitelist extend Player123 --duration 7d
```

---

### 9. Multi-Language Per Player
**Priority:** Low  
**Description:** Allow each player to choose their preferred language.

**Implementation Ideas:**
- Store language preference in player data
- Command to change language: `/cloudly language <code>`
- Per-player message rendering
- Fallback to server default
- Auto-detect from client locale

**Example:**
```kotlin
class PlayerLanguageManager {
    fun setPlayerLanguage(player: UUID, language: String)
    fun getPlayerLanguage(player: UUID): String
    fun getMessageForPlayer(player: Player, key: String): String
}
```

---

### 10. Economy Integration
**Priority:** Low  
**Description:** Support economy plugins for paid whitelist.

**Implementation Ideas:**
- Vault API integration
- Configurable whitelist cost
- Temporary whitelist purchases
- Refunds on removal
- Transaction logging

**Example Configuration:**
```yaml
economy:
  enabled: true
  whitelist_cost: 1000.0
  temporary_costs:
    "7d": 500.0
    "30d": 1500.0
```

---

### 11. PlaceholderAPI Integration
**Priority:** Low  
**Description:** Provide placeholders for other plugins to use.

**Implementation Ideas:**
- `%cloudly_whitelisted%` - true/false
- `%cloudly_whitelist_size%` - total count
- `%cloudly_discord_connected%` - true/false
- `%cloudly_added_by%` - who added player
- Custom placeholder expansion

---

### 12. Update Checker
**Priority:** Low  
**Description:** Automatic update notifications from GitHub.

**Implementation Ideas:**
- Check GitHub releases API
- Compare version numbers
- Notify admins on join
- Command to check updates: `/cloudly update check`
- Download and install updates (optional)

**Example:**
```kotlin
class UpdateChecker(private val plugin: CloudlyPaper) {
    suspend fun checkForUpdates(): UpdateInfo? {
        val response = httpClient.newCall(
            Request.Builder()
                .url("https://api.github.com/repos/becloudly/CloudlyMC/releases/latest")
                .build()
        ).execute()
        // Parse and compare versions
    }
}
```

---

### 13. Whitelist Import/Export
**Priority:** Medium  
**Description:** Import from vanilla whitelist and export to various formats.

**Implementation Ideas:**
- Import from vanilla `whitelist.json`
- Export to JSON, CSV, Excel
- Backup entire whitelist as file
- Restore from backup file
- Merge with existing whitelist

**Example Commands:**
```bash
/cloudly whitelist import vanilla
/cloudly whitelist export json backup.json
/cloudly whitelist export csv whitelist.csv
```

---

### 14. Advanced Logging System
**Priority:** Low  
**Description:** Enhanced logging with levels and separate log files.

**Implementation Ideas:**
- Separate log files per component
- Configurable log levels per component
- Performance timing logs
- Audit trail in separate file
- Log rotation and compression

**Example Configuration:**
```yaml
logging:
  levels:
    storage: INFO
    discord: DEBUG
    commands: INFO
  files:
    audit: "logs/cloudly-audit.log"
    performance: "logs/cloudly-perf.log"
  rotation:
    max_size: "10MB"
    max_files: 5
```

---

### 15. Health Check System
**Priority:** Medium  
**Description:** Monitor plugin health and external service status.

**Implementation Ideas:**
- Periodic health checks for Discord API
- Database connection health
- Storage system health
- Exposed health endpoint
- Automatic recovery attempts

**Example:**
```kotlin
interface HealthCheck {
    val name: String
    fun check(): HealthStatus
}

data class HealthStatus(
    val healthy: Boolean,
    val message: String,
    val details: Map<String, Any> = emptyMap()
)

class HealthCheckManager {
    fun registerCheck(check: HealthCheck)
    fun performAllChecks(): Map<String, HealthStatus>
}
```

---

## 📝 Configuration Improvements

### 1. Config Schema Validation
**Description:** Validate config.yml against a schema on load.

**Benefits:**
- Catch configuration errors early
- Provide helpful error messages
- Suggest correct values
- Prevent runtime errors

---

### 2. Environment Variable Support
**Description:** Allow environment variables in config values.

**Example:**
```yaml
discord:
  bot_token: "${DISCORD_BOT_TOKEN}"
  
mysql:
  password: "${MYSQL_PASSWORD:defaultpassword}"
```

---

### 3. Config Version & Migration
**Description:** Track config version and migrate old configs automatically.

**Implementation:**
```yaml
# Config version
config_version: 2

# Migrator detects old version and upgrades
```

---

### 4. Hot Reload Improvements
**Description:** Better propagation of config changes to all components.

**Implementation Ideas:**
- Config change listeners for all services
- Validate before applying changes
- Rollback on failure
- Notify affected systems

---

### 5. Language System Enhancements
**Description:** More powerful message formatting.

**Features:**
- ICU MessageFormat support
- Pluralization rules
- Number and date formatting
- Gender-aware messages
- Nested placeholders

**Example:**
```yaml
messages:
  whitelist_count: "{count, plural, =0{No players} =1{One player} other{# players}} whitelisted"
```

---

## 🏃 Folia-Specific Optimizations

### 1. Region-Aware Scheduling
**Description:** Use entity/location-specific schedulers when possible.

**Benefits:**
- Better performance on Folia
- Reduced cross-region synchronization
- More efficient resource usage

---

### 2. Async-First Architecture
**Description:** Make more operations async to work better with Folia.

**Implementation:**
- Async storage operations
- Async Discord API calls
- Async command processing where possible

---

## 🧪 Testing Infrastructure

### 1. Unit Tests
**Description:** Add comprehensive unit tests for core logic.

**Coverage:**
- Storage implementations
- Config management
- Discord service
- Whitelist service
- Utility classes

---

### 2. Integration Tests
**Description:** Test integration between components.

**Areas:**
- Config → Service initialization
- Storage → Repository operations
- Discord API → Verification flow
- Command → Service interactions

---

### 3. Mock Framework
**Description:** Create mocks for Paper/Folia APIs.

**Benefits:**
- Test without running Minecraft server
- Faster test execution
- More reliable CI/CD

---

## 📊 Priority Matrix

| Feature | Priority | Effort | Impact | Category |
|---------|----------|--------|--------|----------|
| Fix Lateinit Safety | High | Low | High | Bug Fix |
| JSON Write Locking | High | Medium | High | Bug Fix |
| MySQL Connection Pooling | High | Medium | High | Performance |
| Credential Encryption | High | High | High | Security |
| Automated Backups | High | Medium | High | Feature |
| Discord Role Sync | High | High | High | Feature |
| Storage Query System | Medium | High | High | Feature |
| GUI Pagination | Medium | Medium | Medium | Feature |
| Update Checker | Low | Low | Low | Feature |
| PlaceholderAPI | Low | Low | Low | Feature |

---

## 🎯 Recommended Implementation Order

### Phase 1: Critical Fixes (1-2 weeks)
1. Fix lateinit property access safety
2. Add JSON write locking
3. Implement MySQL connection pooling
4. ~~Fix Discord cache memory leak~~ ✅ **COMPLETED**
5. Add command cooldowns

### Phase 2: Security & Stability (2-3 weeks)
1. Implement credential encryption
2. Add audit logging
3. Add configuration validation
4. Improve error handling consistency
5. Add resource cleanup tracking

### Phase 3: Performance (2-3 weeks)
1. Implement write-back caching for JSON
2. Add batch operations to storage
3. Cache reflection lookups in SchedulerUtils
4. Add rate limiting to Discord API
5. Optimize database queries

### Phase 4: Features (4-6 weeks)
1. Automated backup system
2. Discord role-based whitelist
3. GUI pagination and search
4. Temporary whitelist entries
5. Import/export functionality

### Phase 5: Polish (2-3 weeks)
1. Add comprehensive documentation
2. Create unit and integration tests
3. Add update checker
4. Implement metrics system
5. Create developer API

---

## 📚 Additional Resources

### Useful Libraries to Consider
- **HikariCP**: Connection pooling for MySQL
- **Micrometer**: Metrics and monitoring
- **Caffeine**: High-performance caching
- **Resilience4j**: Rate limiting and circuit breakers
- **JDA (Java Discord API)**: More complete Discord integration

### Documentation References
- [Paper API Documentation](https://docs.papermc.io/)
- [Folia Documentation](https://docs.papermc.io/folia)
- [Kotlin Coroutines Guide](https://kotlinlang.org/docs/coroutines-guide.html)
- [Discord API Documentation](https://discord.com/developers/docs)

---

## 🤝 Contributing

This document should be updated as improvements are implemented. When working on an item:

1. Create an issue referencing this document
2. Implement the improvement
3. Update this document to mark as complete
4. Add any lessons learned or new issues discovered

---

**Last Updated:** Initial Creation  
**Maintainer:** CloudlyMC Development Team
