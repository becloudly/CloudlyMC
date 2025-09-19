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
| Async Kotlin coroutines | CodeQL security scanning | Latest Kotlin 2.2.0 | Hot-reload configuration |
| Folia multi-threading | Dependency vulnerability checks | Paper/Folia support | Multi-language ready |
| Optimized JAR size | Safe null handling | Modern Java 17+ | Modular architecture |

</div>

## 📊 Project Activity

<div align="center">

[![Contributors](https://img.shields.io/github/contributors/becloudly/cloudlymc?style=for-the-badge)](https://github.com/becloudly/cloudlymc/graphs/contributors) [![Last Commit](https://img.shields.io/github/last-commit/becloudly/cloudlymc?style=for-the-badge)](https://github.com/becloudly/cloudlymc/commits/master) [![Open Issues](https://img.shields.io/github/issues-raw/becloudly/cloudlymc?style=for-the-badge)](https://github.com/becloudly/cloudlymc/issues) [![Open PRs](https://img.shields.io/github/issues-pr-raw/becloudly/cloudlymc?style=for-the-badge)](https://github.com/becloudly/cloudlymc/pulls)

</div>

## ✨ Features & Capabilities

### 🎮 **Core Server Management**
- **🔄 Hot-Reload System**: Real-time configuration and language file reloading without server restart
- **📋 Advanced Command System**: Comprehensive `/cloudly` command with reload, info, and help subcommands
- **🌐 Multi-Language Support**: Full internationalization with English and German (more languages planned)
- **⚡ Dual Server Compatibility**: Automatic detection and optimization for both Paper/Spigot and Folia servers

### 🛠️ **Technical Excellence**
- **🚀 Async Performance**: Built with Kotlin coroutines for non-blocking operations
- **💾 Database Integration**: Native support for SQLite and MySQL with optimized drivers
- **🔒 Type Safety**: Null-safe Kotlin implementation with comprehensive error handling
- **📦 Optimized Distribution**: Minimized JAR with dependency relocation and conflict prevention

### 🔔 **Automation & Monitoring**
- **📡 Release Radar**: Automated GitHub release monitoring with configurable channels (stable/pre-release)
- **🔧 Configuration Management**: Intelligent config validation and hot-reload capabilities
- **📊 Debug & Logging**: Comprehensive debug mode with detailed logging and error reporting

### 🏗️ **Developer Experience**
- **🧩 Modular Architecture**: Clean separation of concerns with dedicated packages for each feature
- **🔀 Template System**: Build-time constant generation for version management
- **📝 Comprehensive Documentation**: Detailed code documentation and user guides

<details>
<summary><strong>🔍 View Technical Specifications</strong></summary>

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

</details>

## 🚀 Quick Start

### 📋 Requirements

| Component | Requirement | Recommended |
|-----------|-------------|-------------|
| **Server Software** | Paper/Folia 1.20+ | Paper/Folia 1.21+ |
| **Java Version** | Java 17+ | Java 21 LTS |
| **RAM** | 1GB+ | 2GB+ |
| **Disk Space** | 50MB+ | 100MB+ |

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
  # Enable debug mode for verbose logging
  debug: false
  
  # Available languages: en (English), de (German)
  language: "en"
```

### 🎮 Command Usage

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

### 📋 Update Policy
- **Semantic Versioning**: Clear version numbering (Major.Minor.Patch)
- **Backward Compatibility**: Configuration compatibility maintained across minor versions
- **Security Patches**: Critical security updates released immediately
- **Changelog**: View detailed release notes at [becloudly.eu/changelog](https://becloudly.eu/changelog)

### 🔄 Update Process
2. **Download**: Get latest version from [Releases](https://github.com/becloudly/cloudlymc/releases)
3. **Hot-Reload**: Use `/cloudly reload` for configuration updates
4. **Full Update**: Replace JAR and restart for major updates

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
