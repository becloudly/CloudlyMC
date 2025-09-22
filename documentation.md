# 🌩️ CloudlyMC Plugin Dokumentation

**Version:** 0.0.1-alpha_10  
**Autor:** Phantom (Cloudly)  
**Minecraft Version:** 1.20+  
**Server Software:** Paper/Folia  

---

## 📋 Inhaltsverzeichnis

1. [🌟 Projektübersicht](#-projektübersicht)
2. [⚙️ Installation & Konfiguration](#️-installation--konfiguration)
3. [🎯 Befehlsreferenz](#-befehlsreferenz)
4. [🔐 Berechtigungssystem](#-berechtigungssystem)
5. [🏗️ Plugin-Architektur](#️-plugin-architektur)
6. [📁 Konfigurationsdateien](#-konfigurationsdateien)
7. [💡 Beispiele & Best Practices](#-beispiele--best-practices)

---

## 🌟 Projektübersicht

CloudlyMC ist ein leistungsstarkes Minecraft-Server-Plugin, das in Kotlin entwickelt wurde und speziell für Paper- und Folia-Server optimiert ist. Das Plugin bietet umfassende Serververwaltungsfunktionen mit Fokus auf asynchrone Operationen und Multi-Language-Support.

### ✨ Hauptfunktionen

- 🎯 **Cross-Platform Kompatibilität**: Automatische Erkennung von Paper vs. Folia mit adaptivem Scheduler
- 🌍 **Multi-Language Support**: Vollständige deutsche und englische Übersetzungen
- 📝 **Erweiterte Whitelist**: Benutzerdefinierte Whitelist mit GUI-Management
- 🔗 **Discord Integration**: Verifizierung und Verbindung mit Discord-Konten
- 🛡️ **Berechtigungssystem**: Vollständiges Gruppen- und Benutzerberechtigungsmanagement
- 🔄 **Hot-Reload**: Live-Neuladen von Konfigurationen ohne Server-Neustart
- 💾 **Flexible Speicherung**: Support für JSON, SQLite und MySQL
- 🎨 **GUI-Management**: Benutzerfreundliche grafische Oberflächen

### 🎯 Kompatibilität

- **Minecraft Version:** 1.20+
- **Server Software:** Paper, Spigot, Folia
- **Java Version:** 17+
- **Kotlin Version:** 1.9+

---

## ⚙️ Installation & Konfiguration

### 📦 Installation

1. **Plugin herunterladen**: Lade die neueste `cloudly-x.x.x.jar` Datei herunter
2. **In Server einfügen**: Kopiere die JAR-Datei in den `plugins/` Ordner deines Servers
3. **Server starten**: Starte deinen Server, um das Plugin zu initialisieren
4. **Konfiguration anpassen**: Bearbeite die Konfigurationsdateien nach deinen Bedürfnissen

### 📁 Verzeichnisstruktur

Nach der ersten Ausführung wird folgende Struktur erstellt:

```
plugins/
└── Cloudly/
    ├── config.yml           # Hauptkonfiguration
    ├── languages/
    │   ├── en.yml           # Englische Übersetzungen
    │   └── de.yml           # Deutsche Übersetzungen
    └── data/                # Speicherort für Daten (JSON/SQLite)
        ├── whitelist.json
        ├── permissions.db
        └── ...
```

### 🔧 Grundkonfiguration

Die wichtigsten Konfigurationsoptionen:

```yaml
# Plugin-Einstellungen
plugin:
  debug: false              # Debug-Modus aktivieren
  language: "de"            # Sprache (en/de)

# Discord-Integration
discord:
  enabled: true             # Discord-Features aktivieren
  bot_token: "YOUR_TOKEN"   # Discord Bot Token
  server_id: "SERVER_ID"    # Discord Server ID

# Berechtigungssystem
permissions:
  enabled: true             # Berechtigungssystem aktivieren
  default_group:
    name: "base"            # Standard-Gruppe
    weight: 1               # Gewichtung

# Whitelist
whitelist:
  enabled: true             # Benutzerdefinierte Whitelist
```

---

## 🎯 Befehlsreferenz

Das CloudlyMC Plugin verwendet einen zentralen Befehl `/cloudly` mit verschiedenen Unterbefehlen.

### 🏠 Hauptbefehl

```
/cloudly [subcommand]
```

### 🔧 Admin-Befehle

#### `/cloudly reload [target]`
**Berechtigung:** `cloudly.admin`  
**Beschreibung:** Lädt Plugin-Komponenten neu ohne Server-Neustart

**Parameter:**
- `config` - Nur Konfigurationsdateien neu laden
- `lang` - Nur Sprachdateien neu laden  
- `all` - Alle Komponenten neu laden (Standard)

**Beispiele:**
```bash
/cloudly reload config    # Nur Config neu laden
/cloudly reload lang      # Nur Sprachen neu laden
/cloudly reload          # Alles neu laden
```

#### `/cloudly info`
**Berechtigung:** `cloudly.admin`  
**Beschreibung:** Zeigt Plugin-Informationen und Status an

**Ausgabe:**
- Plugin-Version
- Aktuelle Sprache
- Debug-Status
- Server-Typ (Paper/Folia)
- Autor-Informationen

### 📝 Whitelist-Befehle

#### `/cloudly whitelist <subcommand>`
**Berechtigung:** `cloudly.whitelist`

| Unterbefehl | Parameter | Beschreibung |
|-------------|-----------|--------------|
| `add` | `<spieler>` | Spieler zur Whitelist hinzufügen |
| `remove` | `<spieler>` | Spieler von Whitelist entfernen |
| `list` | - | Alle Whitelist-Spieler anzeigen |
| `gui` | - | Whitelist-GUI öffnen (nur Spieler) |
| `on` | - | Whitelist aktivieren |
| `off` | - | Whitelist deaktivieren |
| `reload` | - | Whitelist neu laden |
| `info` | `<spieler>` | Spieler-Details anzeigen |

**Beispiele:**
```bash
/cloudly whitelist add Notch
/cloudly whitelist remove Herobrine
/cloudly whitelist info Phantom
/cloudly whitelist gui
```

### 🔗 Discord-Befehle

#### `/cloudly connect <discord_username>`
**Berechtigung:** Alle Spieler  
**Beschreibung:** Verbindet das Minecraft-Konto mit Discord

**Voraussetzungen:**
- Spieler muss auf der Whitelist stehen
- Discord-Integration muss aktiviert sein
- Benutzer muss Mitglied des konfigurierten Discord-Servers sein

**Beispiel:**
```bash
/cloudly connect PhantomCloudly
```

### 🛡️ Berechtigungs-Befehle

#### `/cloudly perms <subcommand>`
**Berechtigung:** `cloudly.permissions.*`

| Unterbefehl | Beschreibung |
|-------------|--------------|
| `group` | Gruppen-Verwaltung |
| `user` | Benutzer-Verwaltung |
| `help` | Hilfe anzeigen |
| `info` | System-Informationen |
| `reload` | Berechtigungen neu laden |

### 🏷️ Gruppen-Verwaltung

#### `/cloudly perms group <action> [parameter]`

**Aktionen:**

| Aktion | Parameter | Beschreibung |
|--------|-----------|--------------|
| `create` | `<name> [weight]` | Neue Gruppe erstellen |
| `delete` | `<name>` | Gruppe löschen |
| `list` | - | Alle Gruppen auflisten |
| `info` | `<name>` | Gruppen-Details anzeigen |
| `set` | `<name> <property> <value>` | Eigenschaft setzen |
| `permission` | `<name> <add\|remove\|list> [perm]` | Berechtigungen verwalten |
| `style` | `<set\|remove> <name> <prefix\|suffix> [value]` | Styling verwalten |

**Eigenschaften für `set`:**
- `weight` - Gruppen-Gewichtung (Zahl)
- `prefix` - Chat-Präfix
- `suffix` - Chat-Suffix

**Beispiele:**
```bash
# Gruppe erstellen
/cloudly perms group create admin 100

# Berechtigung hinzufügen
/cloudly perms group permission admin add cloudly.*

# Präfix setzen
/cloudly perms group style set admin prefix &c[Admin]&r

# Gruppe löschen
/cloudly perms group delete moderator
```

### 👤 Benutzer-Verwaltung

#### `/cloudly perms user <action> <spieler> [parameter]`

**Aktionen:**

| Aktion | Parameter | Beschreibung |
|--------|-----------|--------------|
| `info` | `<spieler>` | Benutzer-Details anzeigen |
| `group` | `<spieler> <add\|remove\|list> [gruppe]` | Gruppen verwalten |
| `permission` | `<spieler> <add\|remove\|list> [berechtigung]` | Direkte Berechtigungen |
| `cleanup` | `<spieler>` | Abgelaufene Berechtigungen bereinigen |

**Beispiele:**
```bash
# Benutzer zu Gruppe hinzufügen
/cloudly perms user group Phantom add admin

# Direkte Berechtigung geben
/cloudly perms user permission Phantom add cloudly.whitelist

# Temporäre Berechtigung (1 Tag)
/cloudly perms user permission Phantom add cloudly.admin 1d

# Benutzer-Info anzeigen
/cloudly perms user info Phantom
```

### ❓ Hilfe-Befehle

#### `/cloudly help`
**Berechtigung:** Alle  
**Beschreibung:** Zeigt verfügbare Befehle basierend auf Berechtigungen an

---

## 🔐 Berechtigungssystem

CloudlyMC verfügt über ein fortschrittliches Berechtigungssystem mit Gruppen, direkten Benutzerberechtigungen und temporären Berechtigungen.

### 🏷️ Gruppen-Konzept

```
    A[Benutzer] --> B[Gruppen] = Benutzer können in Gruppen zugeordnet sein
    B[Gruppen] --> C[Berechtigungen] = Gruppen haben Berechtigungen
    A[Benutzer] --> D[Direkte Berechtigungen] = Benutzer können auch direkte Berechtigungen haben
    
    E[Admin - Weight: 100] = Rollen haben gewichte / Prioritäten
    F[Moderator - Weight: 50]
    G[VIP - Weight: 25]
    H[Base - Weight: 1]

    E --> F = Moderator hat weniger Gewichtung als Admin
    F --> G = VIP hat weniger Gewichtung als Moderator
    G --> H = Base hat weniger Gewichtung als VIP
```

### 🔑 Standard-Berechtigungen

| Berechtigung | Beschreibung | Standard |
|--------------|--------------|----------|
| `cloudly.*` | Alle CloudlyMC Berechtigungen | OP |
| `cloudly.admin` | Admin-Befehle | OP |
| `cloudly.whitelist` | Whitelist-Verwaltung | OP |
| `cloudly.permissions.*` | Alle Berechtigungs-Befehle | OP |
| `cloudly.permissions.group.*` | Gruppen-Verwaltung | OP |
| `cloudly.permissions.user.*` | Benutzer-Verwaltung | OP |

### ⚖️ Gewichtungssystem

Gruppen haben Gewichtungen, die ihre Priorität bestimmen:
- **Höhere Gewichtung** = Höhere Priorität
- Bei Konflikten gewinnt die Gruppe mit höherer Gewichtung
- Standard-Gruppe "base" hat Gewichtung 1

### ⏰ Temporäre Berechtigungen

Unterstützte Zeitformate:
- `1d` - 1 Tag
- `2h` - 2 Stunden  
- `30m` - 30 Minuten
- `permanent` - Dauerhaft (Standard)

**Beispiel:**
```bash
/cloudly perms user permission [Spieler] add cloudly.admin 7d
```

---

## 📁 Konfigurationsdateien

### 🎛️ config.yml

Die Hauptkonfigurationsdatei mit allen Plugin-Einstellungen:

```yaml
# ☁️ Cloudly Plugin Configuration ☁️

# Plugin Settings
plugin:
  debug: false                    # Debug-Modus für verbose logging
  language: "de"                  # Verfügbare Sprachen: en, de

# Discord Settings  
discord:
  enabled: false                  # Discord-Integration aktivieren
  bot_token: "YOUR_BOT_TOKEN"     # Discord Bot Token
  server_id: "YOUR_SERVER_ID"     # Discord Server ID
  api_timeout: 10                 # API-Timeout in Sekunden
  cache_duration: 30              # Cache-Dauer in Minuten

# Player Connection Settings
player_connection:
  remove_default_messages: true   # Standard Join/Leave Nachrichten entfernen
  custom_messages:
    join:
      enabled: true               # Benutzerdefinierte Join-Nachrichten
      broadcast_to_chat: true     # Im Chat anzeigen
      broadcast_to_console: true  # In Konsole loggen
    leave:
      enabled: true               # Benutzerdefinierte Leave-Nachrichten
      broadcast_to_chat: true
      broadcast_to_console: true

# Global Storage Configuration
storage:
  default_type: "json"            # Standard: json, sqlite, mysql
  
  json:
    base_path: "data"             # Basis-Verzeichnis
    file_extension: ".json"       # Datei-Endung
    pretty_print: true            # Formatiertes JSON
  
  sqlite:
    base_path: "data"
    file_extension: ".db"
    journal_mode: "WAL"           # WAL für bessere Performance
    synchronous: "NORMAL"
  
  mysql:
    host: "localhost"
    port: 3306
    database: "cloudly_plugin"
    username: "root"
    password: ""
    table_prefix: "cloudly_"
    connection_timeout: 30000
    use_ssl: false
    pool_size: 10

# Permission System Settings
permissions:
  enabled: true                   # Berechtigungssystem aktivieren
  default_group:
    name: "base"                  # Standard-Gruppe
    weight: 1                     # Basis-Gewichtung

# Whitelist Settings
whitelist:
  enabled: false                  # Benutzerdefinierte Whitelist aktivieren
```

### 🌍 Sprachdateien

#### de.yml (Deutsche Übersetzungen)
Enthält alle deutschen Übersetzungen für:
- Plugin-Nachrichten
- Befehls-Ausgaben
- Fehlermeldungen
- GUI-Texte
- Berechtigungs-Nachrichten

#### en.yml (Englische Übersetzungen)
Fallback-Sprache mit englischen Übersetzungen.

**Platzhalter-System:**
```yaml
message_key: "Spieler {player} wurde hinzugefügt am {date}"
```

Verwendung im Code:
```kotlin
languageManager.getMessage("message_key", 
    "player" to playerName,
    "date" to currentDate
)
```

---

## 💡 Beispiele & Best Practices

### 🎯 Server-Setup Beispiel

1. **Grundkonfiguration:**
```yaml
plugin:
  debug: false
  language: "de"

permissions:
  enabled: true
  
whitelist:
  enabled: true
```

2. **Gruppen erstellen:**
```bash
# Admin-Gruppe
/cloudly perms group create admin 100
/cloudly perms group style set admin prefix &c[Admin]&r
/cloudly perms group permission admin add cloudly.*

# Moderator-Gruppe  
/cloudly perms group create moderator 50
/cloudly perms group style set moderator prefix &6[Mod]&r
/cloudly perms group permission moderator add cloudly.whitelist

# VIP-Gruppe
/cloudly perms group create vip 25
/cloudly perms group style set vip prefix &a[VIP]&r
```

3. **Benutzer zuweisen:**
```bash
/cloudly perms user group Phantom add admin
/cloudly perms user group ModeratorName add moderator
```

### 🔗 Discord-Integration Setup

1. **Discord Bot erstellen:**
   - Gehe zu [Discord Developer Portal](https://discord.com/developers/applications)
   - Erstelle eine neue Application
   - Erstelle einen Bot und kopiere den Token

2. **Bot-Berechtigungen:**
   - `Read Messages/View Channels`
   - `Send Messages`
   - `Read Message History`

3. **Konfiguration:**
```yaml
discord:
  enabled: true
  bot_token: "dein_bot_token_hier"
  server_id: "deine_server_id_hier"
```

4. **Spieler verbinden:**
```bash
/cloudly connect DiscordUsername#1234
```
---
---

## 📞 Support & Links

- **GitHub Repository:** [becloudly/CloudlyMC](https://github.com/becloudly/CloudlyMC)
- **Website:** [becloudly.eu](https://becloudly.eu)
- **Discord:** [Discord Server Link]
- **Issues:** [GitHub Issues](https://github.com/becloudly/CloudlyMC/issues)

---

*Diese Dokumentation wurde für CloudlyMC Version 0.0.1-alpha_10 erstellt. Für die neueste Version besuche das GitHub Repository.*
