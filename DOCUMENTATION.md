# CloudlyMC Plugin Documentation

![Cloudly Banner](content/Cloudly_PreviewBanner.png)

*☁️ A high-performance Minecraft server plugin for Paper-based servers built with Kotlin to manage your server with various integrations, highly configurable and multi-language support.*

![Java](https://img.shields.io/badge/Java-17%2B-orange)
![Minecraft](https://img.shields.io/badge/Minecraft-1.18%2B-green)
![Language](https://img.shields.io/badge/Language-Kotlin-blue)
![License](https://img.shields.io/badge/License-MIT-yellow)
![Version](https://img.shields.io/badge/Version-1.0.0.0-brightgreen)

---

## 📋 Table of Contents

- [🌟 Overview](#-overview)
- [✨ Features](#-features)
- [💻 Commands](#-commands)
- [🔐 Permissions](#-permissions)
- [⚙️ Configuration](#️-configuration)
- [🌐 Multi-Language Support](#-multi-language-support)
- [🛡️ Whitelist System](#️-whitelist-system)
- [🚀 Installation](#-installation)
- [📋 Requirements](#-requirements)
- [📞 Support & Contributing](#-support--contributing)
- [📜 License](#-license)

---

## 🌟 Overview

CloudlyMC is a modern, high-performance Minecraft plugin designed for Paper-based servers (Minecraft 1.18+). Built entirely in Kotlin with async operations and modern standards, it provides server management capabilities with a focus on performance, security, and user experience.

### Key Highlights

- ☕ Fully written in **Async Kotlin** for optimal performance
- 🛡️ Modern security standards and null-safe implementations
- 🌐 **8 language support** out of the box
- 📋 Highly configurable with YAML configuration files
- 🎮 Support for **Paper-based servers** (PaperMC, Purpur, Pufferfish, CanvasMC)
- 🔄 **Folia compatible** for multi-threaded server support

---

## ✨ Features

### 🌐 Multi-Language Support

- **8 supported languages**: English, German, French, Spanish, Portuguese, Polish, Russian, Chinese
- Dynamic language switching without server restart
- Consistent message formatting across all languages
- Customizable language files for server-specific modifications

### ⚙️ Configuration Management

- **YAML-based configuration** with comprehensive settings
- **Hot-reload capability** - changes apply without server restart
- Database support for **SQLite** and **MySQL**
- Feature toggles for modular functionality

### 🔧 Server Management

- **Plugin information display** with detailed system metrics
- **Real-time performance monitoring** (memory usage, uptime, player count)
- **Reload functionality** for configuration and language files
- **Async operations** to prevent server lag

### 🛡️ Security & Stability

- **Null-safe implementations** throughout the codebase
- **Permission-based access control** with granular permissions
- **Error handling** with graceful degradation
- **Comprehensive logging** for debugging and monitoring

### 🎯 Performance Optimization

- **Async/await pattern** for all operations
- **Memory-efficient** caching system
- **Fast startup** and shutdown procedures
- **Minimal resource footprint**

### 🛡️ Advanced Whitelist System

- **Database-powered whitelist** that overrides Minecraft's default system
- **High-performance caching** for instant player validation
- **Async operations** that don't block server performance
- **Multi-database support** (SQLite/MySQL) for scalability
- **Real-time management** with instant enable/disable functionality
- **Comprehensive logging** and error handling

---

## 💻 Commands

### Primary Command: `/cloudly`

The main command interface for interacting with the Cloudly plugin.

#### Basic Usage

```bash
/cloudly
```

**Description:** Shows basic plugin information including current version.  
**Permission:** `cloudly.command`  
**Available to:** All players (by default)

#### Subcommands

##### 📊 Info Command

```bash
/cloudly info
```

**Description:** Displays comprehensive plugin and server information including:

- Plugin version
- Server type and version
- Current language setting
- Memory usage statistics with performance rating
- Server uptime
- Online player count
- Java version information
- Operating system details
- Performance assessment

**Permission:** `cloudly.command.info`  
**Available to:** All players (by default)

**Example Output:**

```
--------- Cloudly System Information ---------
Plugin Version: 1.0.0.0
Server: PaperMC 1.20.4
Language: English
Memory Usage: 512MB / 2048MB
Server Uptime: 2h 34m 16s
Players Online: 15/100
Java Version: 17.0.2
Operating System: Linux (amd64)
Performance Rating: Good
--------- CloudlyMC Plugin ---------
```

##### 🔄 Reload Command

```bash
/cloudly reload
```

**Description:** Reloads the plugin configuration and language files without requiring a server restart.

**Permission:** `cloudly.admin`  
**Available to:** Operators and users with admin permission only

**Features:**

- Reloads `config.yml` settings
- Refreshes language files
- Updates database configuration
- Preserves active connections and data

---

## 🔐 Permissions

CloudlyMC uses a hierarchical permission system for granular access control.

### Permission Hierarchy

```
cloudly.admin (includes all permissions below)
├── cloudly.command
    ├── cloudly.command.info
    └── cloudly.command.reload (requires cloudly.admin)
```

### Permission Details

| Permission | Description | Default | Commands |
|------------|-------------|---------|----------|
| `cloudly.admin` | **Full administrative access** to all Cloudly features | `op` | All commands |
| `cloudly.command` | **Basic access** to Cloudly commands | `true` | `/cloudly`, `/cloudly info` |
| `cloudly.command.info` | **Access** to view plugin information | `true` | `/cloudly info` |

### Permission Notes

- **Default `true`** means all players have this permission by default
- **Default `op`** means only server operators have this permission by default
- **Console always has all permissions** regardless of permission settings
- **Operators automatically have all permissions** even without explicit assignment

### Setting Permissions

#### Using LuckPerms (Recommended)

```bash
# Grant admin access
/lp user <username> permission set cloudly.admin true

# Grant basic command access only
/lp user <username> permission set cloudly.command true

# Remove admin access
/lp user <username> permission unset cloudly.admin
```

#### Using Built-in Permissions (permissions.yml)

```yaml
permissions:
  cloudly.admin:
    description: Full Cloudly administration
    default: op
  cloudly.command:
    description: Basic Cloudly commands
    default: true
```

---

## ⚙️ Configuration

### Main Configuration File (`config.yml`)

```yaml
# Cloudly Plugin Configuration
# Version: 1.0.0.0

plugin:
  # Language setting for the plugin
  # Available: en, de, fr, es, pt, pl, ru, zh
  language: "en"

features:
  # Future feature toggles will be added here
  # example-feature: true

database:
  type: "sqlite"

  sqlite:
    file: "cloudly.db"

  mysql:  
    host: "localhost"
    port: 3306
    database: "cloudly"
    username: "username"
    password: "password"
```

### Configuration Options

#### Plugin Settings

- **`language`**: Sets the default language for messages
  - **Options:** `en`, `de`, `fr`, `es`, `pt`, `pl`, `ru`, `zh`
  - **Default:** `en` (English)

#### Database Configuration

- **`type`**: Database type to use
  - **Options:** `sqlite`, `mysql`
  - **Default:** `sqlite`
  - **SQLite:** Simple file-based database (recommended for small servers)
  - **MySQL:** External database server (recommended for large networks)

#### SQLite Settings

- **`file`**: Database file name
  - **Default:** `cloudly.db`
  - **Location:** Plugin data folder

#### MySQL Settings

- **`host`**: MySQL server hostname or IP
- **`port`**: MySQL server port (usually 3306)
- **`database`**: Database name
- **`username`**: Database username
- **`password`**: Database password

---

## 🌐 Language Support

CloudlyMC supports 8 languages with complete message translations.

### Supported Languages

| Language | Code | File | Status |
|----------|------|------|--------|
| **English** | `en` | `en.yml` | ✅ Complete |
| **German** | `de` | `de.yml` | ✅ Complete |
| **French** | `fr` | `fr.yml` | ✅ Complete |
| **Spanish** | `es` | `es.yml` | ✅ Complete |
| **Portuguese** | `pt` | `pt.yml` | ✅ Complete |
| **Polish** | `pl` | `pl.yml` | ✅ Complete |
| **Russian** | `ru` | `ru.yml` | ✅ Complete |
| **Chinese** | `zh` | `zh.yml` | ✅ Complete |

### Language File Structure

Each language file contains organized message categories:

```yaml
# Common messages used throughout the plugin
common:
  prefix: "&8[&bCloudly&8]&r "
  no-permission: "&cYou don't have permission to use this command."
  player-only: "&cThis command can only be used by players."
  reload-success: "&aCloudly has been reloaded successfully!"
  error-occurred: "&cAn error occurred while executing this command."

# System messages
system:
  startup: "&aCloudly v{0} is starting up..."
  enabled-success: "&aCloudly has been enabled successfully!"
  shutdown: "&aCloudly has been disabled successfully!"

# Command-specific messages
commands:
  cloudly:
    info: "&aCloudly is running on Version: {0}!"
    info-command:
      header: "&8&m-----&r &b&lCloudly Information &8&m-----"
      version: "&7Version: &f{0}"
      server: "&7Server: &f{0} {1}"
```

### Changing Language

1. **Edit configuration:**
   
   ```yaml
   plugin:
     language: "de"  # Change to desired language code
   ```

2. **Reload the plugin:**
   
   ```bash
   /cloudly reload
   ```

### Custom Messages

You can customize any message by editing the language files in `/plugins/Cloudly/lang/`. Changes apply immediately with `/cloudly reload`.

---

## 🛡️ Whitelist System

CloudlyMC provides an advanced whitelist system that completely replaces Minecraft's default whitelist functionality with a high-performance, database-driven solution.

### ✨ Key Features

- **🚀 High Performance**: Database-powered with intelligent caching
- **⚡ Async Operations**: Never blocks server performance
- **🔄 Real-time Management**: Instant enable/disable without restart
- **💾 Multi-Database Support**: Works with SQLite and MySQL
- **🌐 Language Integration**: All messages support multi-language
- **🛡️ Override Protection**: Completely replaces vanilla whitelist

### 📋 Commands

#### Add Player to Whitelist

```bash
/whitelist add <player>
```

**Description:** Adds a player to the whitelist database.  
**Permission:** `cloudly.whitelist.add`  
**Example:** `/whitelist add Notch`

#### Remove Player from Whitelist

```bash
/whitelist remove <player>
```

**Description:** Removes a player from the whitelist. If the player is online and whitelist is enabled, they will be kicked.  
**Permission:** `cloudly.whitelist.remove`  
**Example:** `/whitelist remove Notch`

#### Enable Whitelist

```bash
/whitelist enable
```

**Description:** Enables the whitelist system. All non-whitelisted players will be kicked immediately.  
**Permission:** `cloudly.whitelist.toggle`

#### Disable Whitelist

```bash
/whitelist disable
```

**Description:** Disables the whitelist system. All players can join regardless of whitelist status.  
**Permission:** `cloudly.whitelist.toggle`

#### List Whitelisted Players

```bash
/whitelist list [page]
```

**Description:** Shows a paginated list of all whitelisted players with details about who added them.  
**Permission:** `cloudly.whitelist.list`  
**Example:** `/whitelist list 2`

#### Reload Whitelist

```bash
/whitelist reload
```

**Description:** Reloads whitelist data from the database and refreshes cache.  
**Permission:** `cloudly.whitelist.reload`

### 🔐 Permissions

#### Core Permissions

- **`cloudly.whitelist`**: Base permission for whitelist commands
- **`cloudly.whitelist.*`**: All whitelist permissions

#### Specific Permissions

- **`cloudly.whitelist.add`**: Permission to add players
- **`cloudly.whitelist.remove`**: Permission to remove players  
- **`cloudly.whitelist.toggle`**: Permission to enable/disable whitelist
- **`cloudly.whitelist.list`**: Permission to view whitelisted players
- **`cloudly.whitelist.reload`**: Permission to reload whitelist data

**Default:** All whitelist permissions default to `op` (operators only)

### ⚙️ Configuration

The whitelist system can be configured in `config.yml`:

```yaml
# Whitelist System Configuration
whitelist:
  # Enable/disable the custom whitelist system (overrides vanilla whitelist)
  enabled: false
  
  # Cache settings for performance optimization
  cache:
    # How long to cache whitelist data (in minutes)
    duration: 30
    # Maximum number of entries to cache
    max-size: 1000
  
  # Kick message for non-whitelisted players (supports color codes)
  kick-message: "&cYou are not whitelisted on this server!"
```

#### Configuration Options

- **`enabled`**: Whether the whitelist system is active
  - **Type:** Boolean
  - **Default:** `false`

- **`cache.duration`**: How long to cache whitelist data (minutes)
  - **Type:** Integer
  - **Default:** `30`
  - **Range:** 1-1440 (24 hours max recommended)

- **`cache.max-size`**: Maximum entries to keep in cache
  - **Type:** Integer  
  - **Default:** `1000`
  - **Range:** 100-10000

- **`kick-message`**: Message shown to non-whitelisted players
  - **Type:** String
  - **Default:** `"&cYou are not whitelisted on this server!"`
  - **Supports:** Color codes (`&a`, `&c`, etc.`)

### 🗄️ Database Schema

The whitelist system uses two database tables:

#### `whitelist_users`

| Column | Type | Description |
|--------|------|-------------|
| `id` | INT/INTEGER | Primary key |
| `uuid` | VARCHAR(36) | Player UUID (unique) |
| `username` | VARCHAR(16) | Player username |
| `added_by` | VARCHAR(16) | Who added the player |
| `added_at` | BIGINT | Timestamp when added |
| `active` | BOOLEAN | Whether entry is active |

#### `whitelist_settings`

| Column | Type | Description |
|--------|------|-------------|
| `id` | INT/INTEGER | Primary key (always 1) |
| `enabled` | BOOLEAN | Whether whitelist is enabled |
| `last_modified` | BIGINT | Last modification timestamp |
| `modified_by` | VARCHAR(16) | Who last modified settings |

### 🚀 Performance Features

- **⚡ Intelligent Caching**: Keeps frequently accessed data in memory
- **🔄 Async Operations**: All database operations are non-blocking
- **📊 Batch Processing**: Efficient handling of multiple operations
- **🎯 UUID-Based**: Uses UUID for reliable player identification
- **💾 Connection Pooling**: Optimized database connections

### 🔒 Security Features

- **🛡️ SQL Injection Protection**: All queries use prepared statements
- **✅ Input Validation**: Comprehensive validation of all inputs
- **🚫 Permission Enforcement**: Strict permission checking
- **📝 Audit Trail**: Complete logging of all whitelist changes
- **🔐 Safe Defaults**: Secure configuration out of the box

### 🌐 Multi-Language Support

All whitelist messages support the full language system:

- **English** (`en`)
- **German** (`de`) 
- **French** (`fr`)
- **Spanish** (`es`)
- **Portuguese** (`pt`)
- **Polish** (`pl`)
- **Russian** (`ru`)
- **Chinese** (`zh`)

Messages can be customized in each language file under `commands.whitelist.*`.

---

## 🚀 Installation

### Prerequisites

- **Server Type:** Paper-based server (PaperMC, Purpur, Pufferfish, CanvasMC)
- **Minecraft Version:** 1.18 or higher
- **Java Version:** Java 17 or higher

### Installation Steps

1. **Download the Plugin**
   - Get the latest version from [GitHub Releases](https://github.com/gerrxt07/cloudlymc/releases)
   - Download the `.jar` file

2. **Install the Plugin**
   
   ```bash
   # Place the JAR file in your server's plugins folder
   cp Cloudly-1.0.0.0.jar /path/to/your/server/plugins/
   ```

3. **Start/Restart Server**
   - Restart your server or use a plugin manager to load Cloudly
   - The plugin will create default configuration files

4. **Configure the Plugin**
   - Edit `/plugins/Cloudly/config.yml` to your preferences
   - Modify language files if needed
   - Use `/cloudly reload` to apply changes

### First-Time Setup

After installation, Cloudly will automatically:

- Create configuration files with default settings
- Set up the language files for all supported languages
- Initialize the database (SQLite by default)
- Register all commands and permissions

---

## 📋 Requirements

### Minimum Requirements

- **Minecraft:** 1.18+
- **Server Software:** Paper-based (PaperMC, Purpur, Pufferfish, CanvasMC, etc.)
- **Java:** Java 17+
- **RAM:** 512MB minimum (1GB+ recommended)

### Recommended Setup

- **Java:** Java 21 (latest LTS)
- **RAM:** 2GB+ for optimal performance
- **Storage:** SSD for better database performance
- **Network:** Stable connection for MySQL (if used)

### Compatibility

#### ✅ Supported Server Types

- **PaperMC** (Primary target)
- **Purpur**
- **Pufferfish**
- **CanvasMC**
- **Folia** (experimental multi-threaded support)

#### ❌ Unsupported Server Types

- **Bukkit/CraftBukkit** (lacks required APIs)
- **Spigot** (missing Paper-specific features)
- **Fabric/Forge** (different modding platform)

### Plugin Dependencies

- **None required** - Cloudly is completely standalone
- **Optional:** Permission plugin (LuckPerms recommended)

---

## 📞 Support & Contributing

### Getting Help

- 📖 **Documentation:** [GitBook Documentation](https://gerrxt.gitbook.io/cloudlymc)
- 🎫 **Issues:** [GitHub Issues](https://github.com/gerrxt07/cloudlymc/issues)
- 💬 **Discord:** [Community Discord](https://phantomcommunity.de/discord)

### Development

- 🗂️ **Project Board:** [Trello Board](https://trello.com/b/GMKCYKXv/cloudly)
- 📝 **Source Code:** [GitHub Repository](https://github.com/gerrxt/CloudlyMC)

### Security

For security-related issues, please email: `report@phantomcommunity.de`

---

## 📜 License

This project is licensed under the MIT License. See the [LICENSE](LICENSE) file for details.

### Copyright

Copyright © 2025 Gerrit Schlinkmann / Gerrxt

---

*Last updated: June 1, 2025*  
*Plugin Version: 1.0.0.0*
