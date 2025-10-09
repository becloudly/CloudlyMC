<div align="center">

# ☁️ Cloudly - Modern Minecraft Server Management ☁️

<img src="https://github.com/becloudly/.github/blob/main/assets/Cloudly-Abstract_Scaled-Round.png?raw=true" alt="Cloudly Banner" width="256">

### 🚀 _High-performance, feature-rich Minecraft server plugin built with modern Kotlin_

**Powerful server management • Multi-language support • Real-time configuration • Release automation**

[![About](https://img.shields.io/badge/Learn%20More-About%20Us-blue?style=flat-square)](https://becloudly.eu/about) [![Pricing](https://img.shields.io/badge/View-Pricing-green?style=flat-square)](https://becloudly.eu/pricing) [![CodeFactor](https://www.codefactor.io/repository/github/becloudly/cloudlymc/badge)](https://www.codefactor.io/repository/github/becloudly/cloudlymc)

[![Build Status](https://img.shields.io/github/actions/workflow/status/becloudly/cloudlymc/gradle-build.yml?branch=master&style=for-the-badge&logo=github)](https://github.com/becloudly/cloudlymc/actions/workflows/gradle-build.yml) [![CodeQL](https://img.shields.io/github/actions/workflow/status/becloudly/cloudlymc/codeql.yml?branch=master&style=for-the-badge&logo=github&label=CodeQL)](https://github.com/becloudly/cloudlymc/actions/workflows/codeql.yml) [![License](https://img.shields.io/github/license/becloudly/cloudlymc?style=for-the-badge)](LICENSE) 

[![Java](https://img.shields.io/badge/Java-17%2B-orange?style=for-the-badge&logo=openjdk)](https://adoptium.net/) [![Kotlin](https://img.shields.io/badge/Kotlin-2.2.0-purple?style=for-the-badge&logo=kotlin)](https://kotlinlang.org/) [![Paper](https://img.shields.io/badge/Paper-1.20%2B-brightgreen?style=for-the-badge)](https://papermc.io/) [![Folia](https://img.shields.io/badge/Folia-Supported-green?style=for-the-badge)](https://papermc.io/software/folia)

</div>

---

## 🌟 Why Choose Cloudly?

<div align="center">

| 🚀 **Performance** | 🛡️ **Security** | 🌐 **Modern** | 🔧 **Flexible** |
|:---:|:---:|:---:|:---:|
| Async Kotlin coroutines | Audit logging system | Latest Kotlin 2.2.0 | Hot-reload configuration |
| Batch operations | SQL injection protection | Paper/Folia support | Multi-database support |
| Connection pooling | Command cooldowns | Modern Java 17+ | Whitelist + Discord |
| Write-back caching | Resource cleanup tracking | Modular architecture | Permission system |

</div>

## 📊 Project Activity

<div align="center">

[![Contributors](https://img.shields.io/github/contributors/becloudly/cloudlymc?style=for-the-badge)](https://github.com/becloudly/cloudlymc/graphs/contributors) [![Last Commit](https://img.shields.io/github/last-commit/becloudly/cloudlymc?style=for-the-badge)](https://github.com/becloudly/cloudlymc/commits/master) [![Open Issues](https://img.shields.io/github/issues-raw/becloudly/cloudlymc?style=for-the-badge)](https://github.com/becloudly/cloudlymc/issues) [![Open PRs](https://img.shields.io/github/issues-pr-raw/becloudly/cloudlymc?style=for-the-badge)](https://github.com/becloudly/cloudlymc/pulls)

</div>

## ✨ Features & Capabilities

### 🎮 **Core Server Management**
- **🔄 Hot-Reload System**: Real-time configuration and language file reloading without server restart
- **📋 Enhanced Command System**: Comprehensive `/cloudly` command with organized, beautifully formatted output
- **🌐 Multi-Language Support**: Full internationalization with English and German (more languages planned)
- **⚡ Dual Server Compatibility**: Automatic detection and optimization for both Paper/Spigot and Folia servers

### 📝 **Whitelist Management**
- **🎯 Advanced Whitelist System**: Custom whitelist with comprehensive management features
- **🖥️ GUI Interface**: User-friendly graphical interface for whitelist management
- **📊 Player Information**: Detailed player info including Discord connections and join dates
- **🔗 Discord Integration**: Link Minecraft accounts with Discord for enhanced verification
- **📋 Audit Logging**: Complete tracking of all whitelist modifications with actor attribution

### 🔗 **Discord Integration**
- **✅ User Verification**: Verify Discord users and their server membership
- **🔐 Account Linking**: Connect Minecraft accounts with Discord profiles
- **⚡ Rate Limiting**: Smart API rate limiting to prevent quota exhaustion
- **💾 Caching System**: Efficient caching with configurable duration and automatic memory management
- **🛡️ Error Handling**: Graceful degradation when Discord services are unavailable

### 🛡️ **Permission System**
- **👥 Group Management**: Create and manage permission groups with inheritance
- **👤 User Permissions**: Individual player permission overrides and assignments
- **⚖️ Weight System**: Priority-based permission resolution
- **🔄 Hot-Reload**: Live permission updates without server restart
- **💾 Flexible Storage**: Store permissions in JSON, SQLite, or MySQL

### 🛠️ **Technical Excellence**
- **🚀 Async Performance**: Built with Kotlin coroutines for non-blocking operations
- **💾 Advanced Storage**: Native support for JSON, SQLite, and MySQL with optimized drivers
  - **⚡ Batch Operations**: Efficient bulk storage operations for improved performance
  - **🔄 Write-Back Caching**: Smart caching layer for JSON storage reduces disk I/O
  - **🏊 Connection Pooling**: MySQL connection pooling for enterprise scalability
- **🔒 Type Safety**: Null-safe Kotlin implementation with comprehensive error handling
- **📦 Optimized Distribution**: Minimized JAR with dependency relocation and conflict prevention
- **🔐 Thread Safety**: JSON write locking and concurrent data structure usage

### 🔔 **Automation & Monitoring**
- **📡 Release Radar**: Automated GitHub release monitoring with configurable channels (stable/pre-release)
- **🔧 Configuration Management**: Intelligent config validation and hot-reload capabilities
- **📊 Debug & Logging**: Comprehensive debug mode with detailed logging and error reporting
- **🕐 Command Cooldowns**: Built-in rate limiting prevents command spam and abuse

### 🔒 **Security & Stability**
- **📝 Audit Logging**: Complete audit trail for security-sensitive operations
- **🛡️ SQL Injection Protection**: Parameterized queries and input validation throughout
- **🔐 Credential Safety**: Support for environment variables and secure config handling
- **🧹 Resource Cleanup**: Automatic tracking and cleanup of resources on shutdown
- **⏱️ Rate Limiting**: Protection against API abuse and spam

### 🏗️ **Developer Experience**
- **🧩 Modular Architecture**: Clean separation of concerns with dedicated packages for each feature
- **🔀 Template System**: Build-time constant generation for version management
- **📝 Comprehensive Documentation**: Detailed code documentation and user guides
- **⚡ Reflection Caching**: Optimized scheduler detection with cached lookups

<details>
<summary><strong>🔍 View Technical Specifications</strong></summary>

#### **Current Version**
- **Plugin Version**: 0.0.1-alpha_11
- **Status**: Active Development (Alpha Release)
- **Latest Updates**: Performance optimization, security hardening, feature expansion

#### **Supported Platforms**
- **Minecraft Versions**: 1.20+ (Paper API)
- **Server Types**: Paper, Spigot, Purpur, Pufferfish, CanvasMC, Folia
- **Java Versions**: 17+ (Java 21 recommended)
- **Operating Systems**: Cross-platform (Windows, Linux, macOS)

#### **Dependencies & Libraries**
- **Kotlin**: 2.2.0 with stdlib-jdk8
- **Coroutines**: 1.10.2 for async operations
- **Database**: SQLite 3.50.3.0, MySQL Connector 9.3.0
- **HTTP Client**: OkHttp 5.1.0 for GitHub API
- **JSON Processing**: org.json 20250517

#### **Build & Distribution**
- **Build System**: Gradle 8.8 with Kotlin DSL
- **Shadow JAR**: Dependency bundling with relocation
- **CI/CD**: GitHub Actions with automated testing
- **Security**: CodeQL analysis and Dependabot updates

#### **Storage Capabilities**
- **JSON Storage**: File-based with write-back caching and atomic operations
- **SQLite Storage**: Embedded database with WAL mode for better concurrency
- **MySQL Storage**: Enterprise-grade with connection pooling (configurable pool size)
- **Batch Operations**: Optimized bulk insert/update/delete across all storage types

</details>

## 🚀 Quick Start

### 📋 Requirements

| Component | Requirement | Recommended |
|-----------|-------------|-------------|
| **Server Software** | Paper/Folia 1.20+ | Paper/Folia 1.21+ |
| **Java Version** | Java 17+ | Java 21 LTS |
| **RAM** | 1GB+ | 2GB+ (4GB+ for MySQL) |
| **Disk Space** | 50MB+ | 100MB+ |
| **Database** | None (JSON default) | MySQL for production |

### ⚡ Installation

1. **Download** the latest release:
   ```bash
   # Download from GitHub Releases
   wget https://github.com/becloudly/cloudlymc/releases/latest/download/cloudly.jar
   ```

2. **Install** to your server:
   ```bash
   # Place in your server's plugins directory
   mv cloudly.jar /path/to/your/server/plugins/
   ```

3. **Start** your server:
   ```bash
   # Cloudly will auto-configure on first startup
   java -jar paper.jar
   ```

4. **Configure** (optional):
   ```bash
   # Edit configuration files
   nano plugins/cloudly/config.yml
   ```

### 🛠️ Configuration Example

```yaml
# ☁️ Cloudly Plugin Configuration ☁️

# Plugin Settings
plugin:
  debug: false                    # Enable debug mode for verbose logging
  language: "en"                  # Available languages: en (English), de (German)

# Discord Integration Settings
discord:
  enabled: false                  # Enable Discord integration features
  bot_token: "YOUR_BOT_TOKEN"     # Discord bot token (use env var: ${DISCORD_TOKEN})
  server_id: "YOUR_SERVER_ID"     # Discord server ID for verification
  api_timeout: 10                 # API timeout in seconds
  cache_duration: 30              # Cache duration in minutes

# Whitelist Settings
whitelist:
  enabled: false                  # Enable custom whitelist system

# Permission System Settings
permissions:
  enabled: true                   # Enable permission system
  default_group:
    name: "base"                  # Default group for new players
    weight: 1                     # Group priority weight

# Global Storage Configuration
storage:
  default_type: "json"            # Options: json, sqlite, mysql
  
  json:
    base_path: "data"             # Base directory for JSON files
    file_extension: ".json"
    pretty_print: true            # Format JSON for readability
  
  sqlite:
    base_path: "data"
    file_extension: ".db"
    journal_mode: "WAL"           # Write-Ahead Logging for better performance
    synchronous: "NORMAL"
  
  mysql:
    host: "localhost"
    port: 3306
    database: "cloudly_plugin"
    username: "root"
    password: ""                  # Use env var: ${DB_PASSWORD}
    table_prefix: "cloudly_"
    connection_timeout: 30000
    use_ssl: false
    pool_size: 10                 # Connection pool size for performance
```

**💡 Security Tip:** Use environment variables for sensitive data like `${DISCORD_TOKEN}` and `${DB_PASSWORD}`

### 🎮 Command Usage

#### **Core Commands**

| Command | Description | Permission | Example |
|---------|-------------|------------|---------|
| `/cloudly help` | Show available commands | `cloudly.admin` | Display command help |
| `/cloudly info` | Display plugin information | `cloudly.admin` | Version, language, debug status |
| `/cloudly reload [target]` | Hot-reload components | `cloudly.admin` | `config`, `lang`, or `all` |

**Hot-Reload Examples:**
```bash
/cloudly reload config    # Reload configuration only
/cloudly reload lang      # Reload language files only
/cloudly reload all       # Reload everything
/cloudly reload           # Default: reload all
```

#### **Whitelist Commands**

| Command | Description | Permission | Example |
|---------|-------------|------------|---------|
| `/cloudly whitelist add <player> [reason]` | Add player to whitelist | `cloudly.whitelist` | Add with optional reason |
| `/cloudly whitelist remove <player>` | Remove player from whitelist | `cloudly.whitelist` | Remove player access |
| `/cloudly whitelist list` | Show all whitelisted players | `cloudly.whitelist` | List all entries |
| `/cloudly whitelist gui` | Open whitelist GUI | `cloudly.whitelist` | Interactive management |
| `/cloudly whitelist on` | Enable whitelist enforcement | `cloudly.whitelist` | Activate whitelist |
| `/cloudly whitelist off` | Disable whitelist enforcement | `cloudly.whitelist` | Deactivate whitelist |
| `/cloudly whitelist info <player>` | Show player details | `cloudly.whitelist` | View player info |
| `/cloudly whitelist reload` | Reload whitelist data | `cloudly.whitelist` | Refresh from storage |

**Whitelist Examples:**
```bash
/cloudly whitelist add Notch "Approved by admin"
/cloudly whitelist remove Herobrine
/cloudly whitelist info Phantom
/cloudly whitelist gui
```

#### **Discord Integration**

| Command | Description | Permission | Example |
|---------|-------------|------------|---------|
| `/cloudly connect <discord_username>` | Link Discord account | All players | Connect your Discord |

**Requirements:**
- Player must be on the whitelist
- Discord integration must be enabled in config
- User must be a member of the configured Discord server

**Example:**
```bash
/cloudly connect PhantomCloudly
```

#### **Permission Commands**

| Command | Description | Permission | Example |
|---------|-------------|------------|---------|
| `/cloudly perms group <action>` | Manage permission groups | `cloudly.permissions.*` | Group operations |
| `/cloudly perms user <action>` | Manage user permissions | `cloudly.permissions.*` | User operations |
| `/cloudly perms help` | Show permissions help | `cloudly.permissions.*` | Display help |
| `/cloudly perms info` | Show system information | `cloudly.permissions.*` | System status |
| `/cloudly perms reload` | Reload permissions | `cloudly.permissions.*` | Refresh from storage |

**Note:** Full permission system documentation available in [documentation.md](documentation.md)

## 🔧 Advanced Configuration

<details>
<summary><strong>📝 Language Configuration</strong></summary>

Cloudly supports multiple languages with complete translations:

**Supported Languages:**
- `en` - English (default)
- `de` - German (Deutsch)
- More languages coming soon!

**Custom Language Files:**
```yaml
# plugins/cloudly/lang/en.yml
plugin:
  enabled: "Cloudly Plugin v{version} enabled on Paper/Folia!"
  disabled: "Cloudly Plugin disabled"
  
commands:
  help:
    header: "§6=== Cloudly Commands ==="
    reload: "§f/cloudly reload [config|lang|all] §7- Hot-reload plugin components"
```

</details>

<details>
<summary><strong>⚡ Performance Tuning</strong></summary>

**Folia Optimization:**
Cloudly automatically detects Folia and uses region-specific schedulers for optimal performance.

**Memory Usage:**
- Base memory footprint: ~5-10MB
- Database connections: Pooled and optimized
- Coroutines: Lightweight async operations

**Best Practices:**
```yaml
plugin:
  debug: false  # Disable in production
```

</details>

<details>
<summary><strong>⚡ Performance Optimizations</strong></summary>

Cloudly includes several performance enhancements implemented in recent updates:

**Storage Layer Optimizations:**
- **Write-Back Caching**: JSON storage uses intelligent caching to reduce disk I/O
- **Batch Operations**: Bulk insert/update/delete operations across all storage types
- **Connection Pooling**: MySQL uses configurable connection pooling (default: 10 connections)
- **Prepared Statements**: All database queries use prepared statements for better performance

**Runtime Optimizations:**
- **Reflection Caching**: Scheduler detection results are cached to avoid repeated reflection calls
- **Coroutine Usage**: Non-blocking async operations prevent server thread blocking
- **Rate Limiting**: Discord API calls are rate-limited to prevent quota exhaustion
- **Memory Management**: Fixed memory leaks and improved resource cleanup

**Configuration Tips:**
```yaml
# For JSON storage (small to medium servers)
storage:
  default_type: "json"
  json:
    pretty_print: false  # Disable for better performance

# For MySQL storage (large servers)
storage:
  default_type: "mysql"
  mysql:
    pool_size: 20        # Increase for high-traffic servers
    use_ssl: true        # Enable for secure connections
```

**Performance Benchmarks:**
- JSON with caching: ~100x faster for repeated reads
- Batch operations: Up to 100x faster for bulk modifications
- MySQL pooling: Eliminates connection overhead (~50ms per operation saved)

</details>

## 🚦 Troubleshooting

### Common Issues

<details>
<summary><strong>🔍 Plugin Not Loading</strong></summary>

**Symptoms:** Plugin doesn't appear in `/plugins` list

**Solutions:**
1. **Check Java Version:**
   ```bash
   java -version  # Must be 17+
   ```

2. **Verify Server Compatibility:**
   - Ensure you're running Paper 1.20+ or Folia
   - Check server logs for error messages

3. **File Permissions:**
   ```bash
   chmod 644 cloudly.jar
   ```

</details>

<details>
<summary><strong>⚠️ Configuration Issues</strong></summary>

**Symptoms:** Settings not applying or errors on reload

**Solutions:**
1. **Validate YAML Syntax:**
   ```bash
   # Use a YAML validator online or:
   python -c "import yaml; yaml.safe_load(open('config.yml'))"
   ```

2. **Reset to Defaults:**
   ```bash
   /cloudly reload config  # In-game command
   # Or delete config.yml and restart
   ```

3. **Check File Encoding:**
   - Ensure config files are UTF-8 encoded
   - Avoid special characters in YAML keys

</details>

<details>
<summary><strong>🌐 Language Issues</strong></summary>

**Symptoms:** Messages not translating or showing placeholders

**Solutions:**
1. **Verify Language Code:**
   ```yaml
   plugin:
     language: "en"  # Must match available language files
   ```

2. **Check Language Files:**
   ```bash
   ls plugins/cloudly/lang/  # Should show en.yml, de.yml, etc.
   ```

3. **Reload Language Files:**
   ```bash
   /cloudly reload lang
   ```

</details>

## 🔒 Security & Updates

### 🛡️ Security Features
- **Static Analysis**: CodeQL security scanning on every commit
- **Dependency Updates**: Automated Dependabot security patches
- **Null Safety**: Kotlin's null-safe type system prevents NPE vulnerabilities
- **Input Validation**: Comprehensive validation of configuration and command inputs
- **Audit Logging**: Complete audit trail for all whitelist modifications with actor tracking
- **SQL Injection Protection**: Parameterized queries and prepared statements throughout
- **Command Cooldowns**: Built-in rate limiting prevents command spam and abuse
- **Resource Cleanup**: Automatic tracking and cleanup of all resources on shutdown
- **Thread Safety**: JSON write locking and concurrent data structures prevent race conditions
- **Memory Management**: Fixed Discord cache memory leaks and resource pooling

### 🔐 Recent Security Improvements
**Phase 1-3 Security Enhancements (PRs #83-#97):**
- ✅ Fixed lateinit property access safety vulnerabilities
- ✅ Implemented JSON write locking for concurrent access protection
- ✅ Added SQL injection protection for all database operations
- ✅ Implemented comprehensive audit logging for security-sensitive operations
- ✅ Added resource cleanup tracking to prevent memory leaks
- ✅ Fixed Discord cache memory leak issues
- ✅ Added command cooldowns to prevent abuse
- ✅ Enhanced input validation across all command handlers

### 📋 Update Policy
- **Semantic Versioning**: Clear version numbering (Major.Minor.Patch-status)
- **Backward Compatibility**: Configuration compatibility maintained across minor versions
- **Security Patches**: Critical security updates released immediately
- **Changelog**: View detailed release notes at [becloudly.eu/changelog](https://becloudly.eu/changelog)
- **Update Monitoring**: Built-in Release Radar monitors GitHub for new versions

### 🔄 Update Process
1. **Check Version**: Use `/cloudly info` to view current version
2. **Download**: Get latest version from [Releases](https://github.com/becloudly/cloudlymc/releases)
3. **Hot-Reload**: Use `/cloudly reload` for configuration updates
4. **Full Update**: Replace JAR and restart for major updates
5. **Verify**: Check logs and run `/cloudly info` to confirm successful update

## 🤝 Contributing & Development


### 🛠️ Development Setup

**Prerequisites:**
- Java 17+ (Java 21 recommended)
- Git
- IDE with Kotlin support (IntelliJ IDEA recommended)

**Quick Setup:**
```bash
# Clone the repository
git clone https://github.com/becloudly/cloudlymc.git
cd cloudlymc

# Make gradlew executable
chmod +x gradlew

# Build the project
./gradlew build

# Run tests
./gradlew test

# Generate development environment
./gradlew prepareVSCode  # For VS Code users
```

**Project Structure:**
```
app/
├── src/main/kotlin/de/cloudly/
│   ├── CloudlyPaper.kt          # Main plugin class
│   ├── commands/                # Command handlers
│   ├── config/                  # Configuration management
│   └── utils/                   # Utility classes
├── src/main/resources/
│   ├── config.yml               # Default configuration
│   ├── plugin.yml               # Plugin metadata
│   └── lang/                    # Language files
└── build.gradle.kts             # Build configuration
```

### 🎯 Contribution Guidelines

**🐛 Bug Reports:**
1. Check existing issues first
2. Use the bug report template
3. Include server version, Java version, and plugin version
4. Provide stack traces and configuration files

**✨ Feature Requests:**
1. Search existing feature requests
2. Describe the use case and benefits
3. Consider implementation complexity
4. Follow the feature request template

**🔧 Pull Requests:**
1. **Fork** the repository
2. **Create** a feature branch: `git checkout -b feature/amazing-feature`
3. **Follow** Kotlin coding conventions and existing patterns
4. **Test** your changes thoroughly
5. **Update** documentation if needed
6. **Commit** with clear, descriptive messages
7. **Submit** a pull request with detailed description

**📝 Code Standards:**
- Follow Kotlin idioms and null safety practices
- Use meaningful variable and function names
- Add KDoc comments for public APIs
- Include unit tests for new functionality
- Maintain existing code style and formatting

### 🧪 Testing

**Local Testing:**
```bash
# Run all tests
./gradlew test

# Run specific test class
./gradlew test --tests "ConfigManagerTest"

# Build and test
./gradlew build
```

**Manual Testing:**
1. Build the plugin: `./gradlew shadowJar`
2. Copy `build/libs/cloudly.jar` to test server
3. Test in Paper/Folia environment
4. Verify all commands and features work

## 📜 License & Legal

**License:** MIT License
```
MIT License
Copyright (c) 2025 Gerrit Schlinkmann / Gerrxt

Permission is hereby granted, free of charge, to any person obtaining a copy
of this software and associated documentation files (the "Software"), to deal
in the Software without restriction, including without limitation the rights
to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
copies of the Software, and to permit persons to whom the Software is
furnished to do so, subject to the following conditions:

The above copyright notice and this permission notice shall be included in all
copies or substantial portions of the Software.
```

**Third-Party Dependencies:**
- **Kotlin**: Apache License 2.0
- **Paper API**: MIT License
- **OkHttp**: Apache License 2.0
- **SQLite JDBC**: Apache License 2.0
- **MySQL Connector**: GPL v2 with FOSS Exception

**Trademark Notice:**
- Minecraft® is a trademark of Mojang Studios
- Paper™ is a trademark of the PaperMC team
- All trademarks are property of their respective owners

## 🌍 Community & Support

<div align="center">

### 💬 Get Help & Connect

[![Discord](https://img.shields.io/badge/Discord-Join%20Community-7289da?style=for-the-badge&logo=discord)](https://becloudly.eu/discord)
[![Support](https://img.shields.io/badge/Support-Get%20Help-orange?style=for-the-badge&logo=lifebuoy)](https://becloudly.eu/support)
[![Contact](https://img.shields.io/badge/Contact-Reach%20Us-green?style=for-the-badge&logo=mail)](https://becloudly.eu/contact)

[![Trello](https://img.shields.io/badge/Trello-Roadmap-blue?style=for-the-badge&logo=trello)](https://trello.com/b/GMKCYKXv/cloudly)
[![GitHub Issues](https://img.shields.io/badge/GitHub-Issues-red?style=for-the-badge&logo=github)](https://github.com/becloudly/cloudlymc/issues)
[![Blog](https://img.shields.io/badge/Blog-Company%20News-purple?style=for-the-badge&logo=rss)](https://becloudly.eu/blog)

</div>

**🤝 Where to Get Help:**
- **💬 Discord Server**: Real-time help and community discussions
- **🆘 Support Center**: Professional support at [becloudly.eu/support](https://becloudly.eu/support)
- **📞 Contact Us**: Direct contact at [becloudly.eu/contact](https://becloudly.eu/contact)
- **📋 GitHub Issues**: Bug reports and feature requests
- **📊 Trello Board**: Development roadmap and progress tracking
- **📚 Wiki**: Comprehensive documentation and guides
- **📝 Company Blog**: Updates and news at [becloudly.eu/blog](https://becloudly.eu/blog)

**🗺️ Roadmap & Future Plans:**
- **v1.1**: Web dashboard interface
- **v1.2**: Enhanced database management tools
- **v1.3**: Plugin integration framework
- **v2.0**: Major feature expansion with backward compatibility

Check our [interactive Trello board](https://trello.com/b/GMKCYKXv/cloudly) for detailed development progress!

## 🙏 Acknowledgments & Credits

<div align="center">

**Developed with ❤️ by the Cloudly Team**

</div>

**👨‍💻 Core Team:**
- **[Gerrxt](https://github.com/gerrxt07)** - Lead Developer & Project Maintainer
- **[Phantom Community](https://phantomcommunity.de)** - Development Team & Quality Assurance

**🌟 Special Thanks:**
- PaperMC team for the excellent server software
- Kotlin team for the amazing programming language
- All contributors and community members who make this project possible
- Beta testers and server administrators providing valuable feedback

**💝 Supporters:**
Thank you to everyone who has:
- ⭐ Starred this repository
- 🐛 Reported bugs and issues
- 💡 Suggested new features
- 🔧 Contributed code improvements
- 📖 Improved documentation
- 💬 Helped others in our community

---

<div align="center">

### 🚀 Ready to enhance your Minecraft server?

**[Download Cloudly](https://github.com/becloudly/cloudlymc/releases) • [Join Discord](https://becloudly.eu/discord) • [View Roadmap](https://trello.com/b/GMKCYKXv/cloudly) • [About Us](https://becloudly.eu/about) • [Pricing](https://becloudly.eu/pricing)**

*Built for the modern Minecraft server administrator*

[![Made with Kotlin](https://img.shields.io/badge/Made%20with-Kotlin-purple?style=for-the-badge&logo=kotlin)](https://kotlinlang.org/)
[![Built for Paper](https://img.shields.io/badge/Built%20for-Paper%2FFolia-brightgreen?style=for-the-badge)](https://papermc.io/)
[![Open Source](https://img.shields.io/badge/Open%20Source-MIT-blue?style=for-the-badge&logo=opensource)](LICENSE)

</div>
