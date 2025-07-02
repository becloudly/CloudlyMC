# CloudlyMC Plugin Features Documentation

CloudlyMC is a high-performance Minecraft server plugin built with Kotlin for Paper-based servers (1.18+). This document provides a comprehensive overview of all features, commands, and permissions.

## 🌟 Core Features

### 🌐 Multi-Language Support
- **Supported Languages**: English (en), German (de), French (fr), Spanish (es), Portuguese (pt), Polish (pl), Russian (ru), Chinese (zh)
- **Configuration**: Set via `config.yml` with `language` option
- **Dynamic Messages**: All user-facing messages support multi-language

### 🛡️ Advanced Whitelist System
- **Custom Implementation**: Overrides Minecraft's vanilla whitelist
- **Database Backend**: SQLite (default) or MySQL support
- **High Performance**: Caching system with configurable duration (default: 30 minutes)
- **Async Operations**: All database operations are asynchronous
- **Logging**: Comprehensive activity logging for auditing

### 📋 Whitelist Application System
- **Player Applications**: Players can submit whitelist requests with reasons
- **Admin Review Interface**: Interactive GUI for reviewing applications
- **Freeze System**: Non-whitelisted players are frozen until approved
- **Notifications**: Real-time notifications for admins when applications are submitted
- **Application Management**: Approve/deny with reasons, automatic whitelist addition

### 🎮 Player Freeze Management 
- **Movement Restriction**: Prevents movement while allowing look-around
- **Interaction Blocking**: Blocks block breaking/placing, inventory access, PvP
- **Command Filtering**: Only allows essential commands for frozen players
- **Status Preservation**: Maintains original game mode and location
- **Visual Feedback**: Action bar messages and periodic reminders

### 🔧 Administrative Tools
- **Database Management**: Export/import whitelist data
- **Activity Logging**: Detailed logs of all whitelist actions
- **Purge Functions**: Remove inactive entries and cleanup
- **Real-time Status**: Live whitelist status and statistics
- **Bulk Operations**: Clear all entries with confirmation

---

## 📝 Commands Reference

### Main Plugin Command: `/cloudly`

| Subcommand | Description | Permission | Usage |
|------------|-------------|------------|-------|
| *none* | Show basic plugin info | `cloudly.command` | `/cloudly` |
| `info` | Show detailed system information | `cloudly.command.info` | `/cloudly info` |
| `reload` | Reload plugin configuration | `cloudly.admin` | `/cloudly reload` |

### Whitelist Management: `/whitelist`

#### Basic Whitelist Commands

| Subcommand | Description | Permission | Usage |
|------------|-------------|------------|-------|
| `add <player>` | Add player to whitelist | `cloudly.whitelist.add` | `/whitelist add PlayerName` |
| `remove <player>` | Remove player from whitelist | `cloudly.whitelist.remove` | `/whitelist remove PlayerName` |
| `enable` | Enable whitelist system | `cloudly.whitelist.toggle` | `/whitelist enable` |
| `disable` | Disable whitelist system | `cloudly.whitelist.toggle` | `/whitelist disable` |
| `list [page]` | List whitelisted players | `cloudly.whitelist.list` | `/whitelist list [1]` |
| `reload` | Reload whitelist data | `cloudly.whitelist.reload` | `/whitelist reload` |
| `apply <reason>` | Submit whitelist application | `cloudly.whitelist.apply` | `/whitelist apply I want to join!` |

#### Administrative Commands: `/whitelist admin`

| Subcommand | Description | Permission | Usage |
|------------|-------------|------------|-------|
| `info` | Show whitelist system information | `cloudly.whitelist.admin` | `/whitelist admin info` |
| `purge` | Remove inactive whitelist entries | `cloudly.whitelist.admin` | `/whitelist admin purge` |
| `logs [count]` | View whitelist activity logs | `cloudly.whitelist.admin` | `/whitelist admin logs [10]` |
| `clear confirm` | Clear entire whitelist | `cloudly.whitelist.admin` | `/whitelist admin clear confirm` |
| `export` | Export whitelist to JSON file | `cloudly.whitelist.admin` | `/whitelist admin export` |
| `import <file>` | Import whitelist from JSON file | `cloudly.whitelist.admin` | `/whitelist admin import whitelist.json` |
| `review` | Open application review GUI | `cloudly.whitelist.admin.review` | `/whitelist admin review` |
| `approve <player> [reason]` | Approve pending application | `cloudly.whitelist.admin.approve` | `/whitelist admin approve PlayerName Welcome!` |
| `deny <player> <reason>` | Deny pending application | `cloudly.whitelist.admin.deny` | `/whitelist admin deny PlayerName Insufficient info` |

---

## 🔑 Permissions System

### Core Plugin Permissions

| Permission | Description | Default | Children |
|------------|-------------|---------|----------|
| `cloudly.admin` | Full admin access to all features | `op` | All permissions |
| `cloudly.command` | Basic plugin command access | `true` | - |
| `cloudly.command.info` | View plugin information | `true` | - |

### Whitelist Permissions

#### Basic Whitelist Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `cloudly.whitelist` | Basic whitelist command access | `op` |
| `cloudly.whitelist.*` | All whitelist permissions | `op` |
| `cloudly.whitelist.add` | Add players to whitelist | `op` |
| `cloudly.whitelist.remove` | Remove players from whitelist | `op` |
| `cloudly.whitelist.toggle` | Enable/disable whitelist | `op` |
| `cloudly.whitelist.list` | View whitelisted players | `op` |
| `cloudly.whitelist.reload` | Reload whitelist data | `op` |
| `cloudly.whitelist.apply` | Submit whitelist applications | `true` |

#### Administrative Permissions

| Permission | Description | Default |
|------------|-------------|---------|
| `cloudly.whitelist.admin` | Access to admin commands | `op` |
| `cloudly.whitelist.admin.review` | Review applications via GUI | `op` |
| `cloudly.whitelist.admin.approve` | Approve whitelist applications | `op` |
| `cloudly.whitelist.admin.deny` | Deny whitelist applications | `op` |

---

## ⚙️ Configuration Options

### Main Configuration (`config.yml`)

```yaml
plugin:
  language: "en"  # Language for messages

features:
  # Feature toggles for future expansions

whitelist:
  enabled: false  # Enable custom whitelist system
  cache:
    duration: 30    # Cache duration in minutes
    max-size: 1000  # Maximum cache entries
  kick-message: "&cYou are not whitelisted on this server!"

database:
  type: "sqlite"  # Database type: "sqlite" or "mysql"
  sqlite:
    file: "cloudly.db"
  mysql:
    host: "localhost"
    port: 3306
    database: "cloudly"
    username: "username"
    password: "password"
```

---

## 🗄️ Database Schema

### Whitelist Tables
- `whitelist_users` - Stores whitelisted players
- `whitelist_settings` - Stores whitelist configuration
- `whitelist_logs` - Activity logging for audit trails

### Application Tables
- `whitelist_applications` - Pending/processed applications
- Application status tracking (PENDING, APPROVED, DENIED)

---

## 🎯 Player Experience Features

### Application Flow
1. **Application Submission**: Players use `/whitelist apply <reason>`
2. **Login Handling**: Non-whitelisted players with pending applications can join but are frozen
3. **Freeze System**: Restricted movement and interactions until approval
4. **Admin Notification**: Real-time notifications and GUI review system
5. **Automatic Processing**: Approved applications automatically add to whitelist

### Freeze System Restrictions
- ❌ Movement (except looking around)
- ❌ Block breaking/placing
- ❌ Inventory interaction
- ❌ Item dropping/picking
- ❌ PvP (attacking/being attacked)
- ❌ Most commands (exceptions: `/whitelist`, `/help`, `/spawn`, etc.)
- ✅ Chat communication
- ✅ Essential commands

---

## 🚀 Performance Features

### Optimization
- **Async Operations**: All database operations are non-blocking
- **Caching System**: Configurable cache for frequent lookups
- **Memory Efficient**: Concurrent data structures for thread safety
- **Error Handling**: Comprehensive error handling with fallback mechanisms

### Monitoring
- **Activity Logging**: Complete audit trail of all actions
- **Performance Metrics**: Built-in performance monitoring
- **Database Health**: Connection pooling and health checks
- **Memory Management**: Automatic cache cleanup and optimization

---

## 🔮 Planned Features (Future Updates)

### Discord Integration
- Discord role-based whitelist verification
- Real-time sync between Discord and in-game whitelist
- `/whitelist verify <username>` command for Discord linking

### Token System
- One-time use whitelist codes/tokens
- `/whitelist admin createtoken <player>` for code generation
- `/whitelist redeem <code>` for token redemption
- Event and giveaway support

### Enhanced Notifications
- Server full notifications for whitelisted players
- Detailed kick messages with application instructions
- Smart player guidance system

---

## 📊 Technical Specifications

- **Minecraft Version**: 1.18+ (Paper/Purpur/Pufferfish compatible)
- **Java Version**: 17+
- **Language**: Kotlin (100% async)
- **Database**: SQLite (default) or MySQL
- **API Support**: Folia-supported for next-generation servers
- **Architecture**: Plugin-based with modular components

---

## 🛠️ Development Information

- **Plugin Type**: Server management and whitelist system
- **Author**: gerrxt07
- **License**: MIT License
- **Repository**: [GitHub](https://github.com/gerrxt/CloudlyMC)
- **Documentation**: [GitBook](https://gerrxt.gitbook.io/cloudlymc)
- **Community**: [Discord Server](https://phantomcommunity.de/discord)

This documentation covers CloudlyMC version 1.0.0.0 and all currently implemented features. For the latest updates and feature requests, please visit the GitHub repository or join the Discord community.
