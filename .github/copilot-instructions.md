# CloudlyMC AI Instructions

[Coding Standards](./prompt-snippets/coding-standards.md)
[Response Personality](./prompt-snippets/copilot-personality.md)

## Project Overview
CloudlyMC is a high-performance Minecraft server plugin built with Kotlin, targeting Paper/Folia servers (1.20+). It provides server management capabilities with multi-language support and focuses on async operations for optimal performance.

### Key Architecture Components
- **Main Plugin Class**: `CloudlyPaper` - Singleton pattern with companion object access
- **Configuration System**: Dual-tier config (plugin config + language files) with hot-reload
- **Cross-Platform Scheduler**: `SchedulerUtils` automatically detects Paper vs Folia and adapts
- **Release Radar**: GitHub API integration with coroutines for update checking
- **Template System**: Build-time constant generation using Gradle's `processResources`

### Critical Build & Development Patterns

#### Build System
- Uses **Gradle Shadow Plugin** with JAR relocation to prevent dependency conflicts:
  ```kotlin
  relocate("kotlin", "de.cloudly.libs.kotlin")
  relocate("org.sqlite", "de.cloudly.libs.sqlite")
  ```
- **Template Generation**: `app/src/main/templates/` → `build/generated/sources/templates/`
- **VS Code Task**: Use `./gradlew prepareVSCode` to generate templates before development

#### Configuration Architecture
- Main config: `plugins/Cloudly/config.yml` (not in plugin jar directory)
- Languages: `plugins/Cloudly/languages/` folder with fallback chain (current → English)
- Hot-reload supports granular reloading: `config`, `lang`, or `all`

### Development Workflows

#### Testing & Deployment  
- **Local Testing**: Use `deploy.bat` for automatic build → clean server deployment
- **Server Compatibility**: Always test Paper AND Folia schedulers using `SchedulerUtils.isFolia()`
- **Database**: Supports both SQLite (lightweight) and MySQL (enterprise)

#### Key File Locations
- **Source**: `app/src/main/kotlin/de/cloudly/`
- **Resources**: `app/src/main/resources/` (config.yml, plugin.yml, lang/)
- **Generated**: Build-time templates in `app/src/main/templates/`
- **Configuration**: `plugins/Cloudly/` folder (NOT in plugin subfolder)

### Kotlin-Specific Patterns

#### Async Operations
Always use coroutines for I/O operations:
```kotlin
private val coroutineScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
// Use coroutineScope.launch for async work
```

#### Cross-Platform Compatibility
Use `SchedulerUtils` wrapper instead of direct Bukkit scheduler calls:
```kotlin
SchedulerUtils.runTask(plugin, Runnable { /* main thread work */ })
SchedulerUtils.runTaskAsynchronously(plugin, Runnable { /* async work */ })
```

#### Error Handling & Logging
- Use translated messages via `languageManager.getMessage(key, *placeholders)`
- Log errors with context: `plugin.logger.log(Level.WARNING, message, exception)`
- Fallback gracefully when language manager isn't available

### Common Gotchas
- **Config location**: Files go to `plugins/Cloudly/` not `plugins/cloudly/` 
- **Template builds**: Run `prepareVSCode` task if `BuildConstants` shows unresolved version
- **Scheduler detection**: Never assume Paper - always use `SchedulerUtils` wrapper
- **JAR conflicts**: New dependencies need relocation in `build.gradle.kts`
- **Language keys**: Check both current language and English fallback in `getMessage()`

### MCP Server Instructions
- **GitHub MCP Server**: Execute against repository: https://github.com/becloudly/CloudlyMC
- **Playwright MCP Server**: Generate playwright code alongside instructions

## Additional Instructions
- Reference actual file paths and existing patterns when suggesting changes
- Always consider both Paper and Folia compatibility for scheduler-related code  
- Validate language key existence in `app/src/main/resources/lang/en.yml`
- Check `Planned.md` for context on planned features and architectural decisions