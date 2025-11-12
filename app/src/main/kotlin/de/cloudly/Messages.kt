package de.cloudly

/**
 * Deutsche Nachrichten für das CloudlyMC Plugin.
 * Alle Texte sind hardcodiert auf Deutsch für einen privaten deutschen Server.
 */
object Messages {
    
    // ========== Plugin-Nachrichten ==========
    object Plugin {
        fun enabled(version: String) = "Cloudly Plugin v$version auf Paper aktiviert!"
        const val DISABLED = "Cloudly Plugin deaktiviert"
        const val DEBUG_ENABLED = "Debug-Modus ist aktiviert"
    }
    
    // ========== Konfigurationsnachrichten ==========
    object Config {
        const val DIRECTORY_CREATED = "Cloudly-Konfigurationsverzeichnis erstellt"
        const val DEFAULT_CREATED = "Standard config.yml im cloudly-Ordner erstellt"
        const val COPY_FAILED = "Standard-Konfiguration konnte nicht kopiert werden"
        const val LOADED_SUCCESSFULLY = "Konfiguration erfolgreich geladen"
        const val DEFAULT_FILE_CREATED = "Standard-Konfigurationsdatei erstellt"
        const val CREATE_FAILED = "Standard-Konfigurationsdatei konnte nicht erstellt werden"
        const val SAVED_SUCCESSFULLY = "Konfiguration erfolgreich gespeichert"
        const val SAVE_FAILED = "Konfigurationsdatei konnte nicht gespeichert werden"
        const val RELOADED = "Konfiguration neu geladen"
    }
    
    // ========== Befehlsnachrichten ==========
    object Commands {
        const val NO_PERMISSION = "§c§l✗ §cDu hast keine Berechtigung, diesen Befehl zu verwenden."
        fun unknownSubcommand(subcommand: String) = 
            "§c§l✗ §cUnbekannter Unterbefehl§8: §f$subcommand\n  §7Verwende §f/cloudly help§7 für verfügbare Befehle."
        
        // Vanilla Whitelist Befehl
        object VanillaWhitelist {
            const val DISABLED = "§cDer Standard-Whitelist-Befehl wurde deaktiviert."
            const val USE_CLOUDLY = "§eBitte verwende stattdessen §f/cloudly whitelist§e."
            const val HELP_MESSAGE = "§7Verwende §f/cloudly whitelist help§7 für verfügbare Befehle."
        }
        
        // Whitelist Befehle
        object Whitelist {
            const val USAGE = "§c✗ Ungültiger Whitelist-Befehl. Verwende §f/cloudly help§c für verfügbare Befehle."
            const val INVALID_SUBCOMMAND = "§c✗ Ungültiger Whitelist-Unterbefehl. Verwende §f/cloudly help§c für verfügbare Befehle."
            const val ADD_USAGE = "  §7Verwendung§8: §f/cloudly whitelist add §8<§7spieler§8>"
            const val REMOVE_USAGE = "  §7Verwendung§8: §f/cloudly whitelist remove §8<§7spieler§8>"
            const val INFO_USAGE = "  §7Verwendung§8: §f/cloudly whitelist info §8<§7spieler§8>"
            
            fun playerAdded(player: String) = "§a§l✓ §aSpieler §e$player§a wurde zur Whitelist hinzugefügt"
            fun addFailed(player: String) = "§c§l✗ §cFehler beim Hinzufügen von Spieler §e$player§c zur Whitelist"
            fun playerRemoved(player: String) = "§a§l✓ §aSpieler §e$player§a wurde von der Whitelist entfernt"
            fun playerRemovedAndKicked(player: String) = "§a§l✓ §aSpieler §e$player§a wurde von der Whitelist entfernt und vom Server gekickt"
            const val PLAYER_REMOVED_KICK_MESSAGE = "§c§lDu wurdest von der Whitelist entfernt"
            fun playerNotWhitelisted(player: String) = "§c§l✗ §cSpieler §e$player§c ist nicht auf der Whitelist"
            
            const val LIST_EMPTY = "§e⚠ Die Whitelist ist leer"
            fun listHeader(count: Int) = "§8§m───────────────────§r\n§6§l Whitelist §8(§7$count§8)\n§8§m───────────────────§r"
            fun listEntry(username: String, date: String) = "  §f▪ §e$username §8│ §7Hinzugefügt: $date"
            const val LIST_FOOTER = "§8§m───────────────────§r"
            
            const val ENABLED = "§a✓ Whitelist wurde §l§aAKTIVIERT"
            const val DISABLED = "§a✓ Whitelist wurde §l§cDEAKTIVIERT"
            const val RELOADED = "§a✓ Whitelist wurde §l§aNEU GELADEN"
            
            fun infoHeader(player: String) = "§8§m──────────────────§r\n§6§l Info §8│ §f$player\n§8§m──────────────────§r"
            fun infoAddedBy(name: String) = "  §e▪ §fHinzugefügt von§8: §7$name"
            fun infoAddedOn(date: String) = "  §e▪ §fHinzugefügt am§8: §7$date"
            const val INFO_FOOTER = "§8§m──────────────────§r"
        }
        
        // Discord Befehle
        object Discord {
            const val DISABLED = "§c§l✗ §cDiscord-Integration ist deaktiviert oder nicht korrekt konfiguriert"
            const val PLAYERS_ONLY = "§c§l✗ §cDieser Befehl kann nur von Spielern verwendet werden"
            const val NOT_WHITELISTED = "§c§l✗ §cDu musst auf der Whitelist stehen, um dein Discord-Konto zu verbinden"
            fun alreadyConnected(discordUsername: String) = "§c§l✗ §cDu hast bereits das Discord-Konto §e$discordUsername§c verbunden"
            const val CONNECT_USAGE = "  §7Verwendung§8: §f/cloudly connect §8<§7discord_benutzername§8>"
            const val INVALID_USERNAME = "§c§l✗ §cUngültiger Discord-Benutzername. Muss 2-32 Zeichen lang sein"
            fun verifying(discordUsername: String) = "§e⏳ Verifiziere Discord-Konto §f$discordUsername§e..."
            const val VERIFICATION_ERROR = "§c§l✗ §cEin Fehler ist bei der Verifizierung deines Discord-Kontos aufgetreten. Bitte versuche es erneut"
            fun connectedSuccessfully(discordUsername: String) = "§a§l✓ §aDiscord-Konto §e$discordUsername§a erfolgreich verbunden!"
            const val CONNECTION_FAILED = "§c§l✗ §cFehler beim Speichern der Discord-Verbindung. Bitte versuche es erneut"
            fun userNotFound(discordUsername: String) = "§c§l✗ §cDiscord-Benutzer §e$discordUsername§c nicht gefunden oder für den Bot nicht erreichbar"
            fun notServerMember(discordUsername: String) = "§c§l✗ §cDiscord-Benutzer §e$discordUsername§c ist kein Mitglied des konfigurierten Servers"
            fun missingRole(discordUsername: String) = "§c§l✗ §cDiscord-Benutzer §e$discordUsername§c hat nicht die erforderliche Rolle auf dem Server"
            const val API_ERROR = "§c§l✗ §cDiscord API-Fehler. Bitte versuche es später erneut"
            fun cooldown(seconds: Int) = "§6§l⏳ §6Bitte warte §e$seconds§6 Sekunden, bevor du diesen Befehl erneut verwendest"
            
            // Discord Verifizierung
            const val VERIFICATION_REQUIRED = "§6§l⚠ §6Discord-Verifizierung erforderlich!\n§7Du musst deinen Discord-Account verbinden, um zu spielen.\n§7Du hast §e5 Minuten§7 Zeit zur Verifizierung."
            const val VERIFICATION_COMMAND = "§7Verwende §f/cloudly connect <discord_username>§7 zur Verifizierung"
            const val VERIFICATION_SUCCESS = "§a§l✓ §aDiscord-Verifizierung erfolgreich! Du kannst jetzt spielen."
            const val VERIFICATION_TIMEOUT = "§c§l✗ §cDu wurdest gekickt, weil du deinen Discord-Account nicht innerhalb von 5 Minuten verifiziert hast"
            const val VERIFICATION_WARNING_3MIN = "§6§l⚠ §6Discord-Verifizierung Warnung: noch §e3 Minuten"
            const val VERIFICATION_WARNING_2MIN = "§6§l⚠ §6Discord-Verifizierung Warnung: noch §e2 Minuten"
            const val VERIFICATION_WARNING_30SEC = "§c§l⚠ §cDiscord-Verifizierung Warnung: noch §e30 Sekunden§c!"
            const val VERIFICATION_CHAT_BLOCKED = "§c§l✗ §cDu kannst nicht chatten, bis du deinen Discord-Account verifiziert hast"
            const val VERIFICATION_COMMAND_BLOCKED = "§c§l✗ §cDu kannst nur /cloudly connect verwenden, bis du deinen Discord-Account verifiziert hast"
        }
        
        // Reload Befehl
        object Reload {
            const val STARTING_FULL = "§e⏳ Starte vollständiges Hot-Reload aller Plugin-Komponenten..."
            const val STARTING_CONFIG = "§e⏳ Starte Neuladen der Konfiguration..."
            const val RELOADING_CONFIG = "  §7▪ Lade Konfigurationsdateien neu..."
            const val RELOADING_COMPONENTS = "  §7▪ Lade Plugin-Komponenten neu..."
            const val CONFIG_RELOADED = "Konfiguration erfolgreich neu geladen"
            const val COMPONENTS_RELOADED = "Plugin-Komponenten erfolgreich neu geladen"
            const val CONFIG_SUCCESS = "\n§a§l✓ §aKonfiguration erfolgreich neu geladen!"
            const val SUCCESS = "\n§a§l✓ §aHot-Reload erfolgreich abgeschlossen!"
            const val SUCCESS_LOG = "Hot-Reload erfolgreich abgeschlossen"
            const val CONFIG_FAILED = "\n§c§l✗ §cFehler beim Neuladen der Konfigurationsdateien! Details in der Konsole"
            const val COMPONENTS_FAILED = "\n§c§l✗ §cFehler beim Neuladen der Plugin-Komponenten! Details in der Konsole"
            const val PARTIAL_FAILURE = "\n§6§l⚠ §6Hot-Reload mit einigen Fehlern abgeschlossen. Details in der Konsole"
            const val PARTIAL_FAILURE_LOG = "Hot-Reload mit einigen Fehlern abgeschlossen"
            fun invalidTarget(target: String) = "§c✗ Ungültiges Reload-Ziel§8: §f$target"
            const val USAGE = "  §7Verwendung§8: §f/cloudly reload §8[§7config§8|§7all§8]"
        }
        
        // Info Befehl
        object Info {
            const val HEADER = "§6§l Plugin Info"
            fun version(version: String) = "  §e▪ §fVersion§8: §7$version"
            fun debug(enabled: Boolean) = "  §e▪ §fDebug-Modus§8: §7${if (enabled) "Aktiviert" else "Deaktiviert"}"
            const val SERVER_TYPE = "  §e▪ §fServer-Typ§8: §7Paper"
            const val AUTHOR = "  §e▪ §fAutor§8: §7Cloudly\n    §8↳ §bhttps://becloudly.eu"
            const val FOOTER = "§8§m───────────────§r"
        }
        
        // Help Befehl
        object Help {
            const val HEADER = "§6§l Befehls-Hilfe"
            const val SEPARATOR = "§8§m───────────────§r"
            const val ADMIN_HEADER = "\n§e§l  ⚙ Administration"
            const val RELOAD = "    §f/cloudly reload §8[§7config§8|§7all§8]\n      §7↳ Komponenten neu laden"
            const val INFO = "    §f/cloudly info\n      §7↳ Plugin-Info anzeigen"
            const val WHITELIST_HEADER = "\n§e§l  📋 Whitelist"
            const val WHITELIST = "    §f/cloudly whitelist §8<§7unterbefehl§8>\n      §7↳ add, remove, list, gui, on, off, reload, info"
            const val DISCORD_HEADER = "\n§e§l  🔗 Discord"
            const val DISCORD_CONNECT = "    §f/cloudly connect §8<§7discord_benutzername§8>\n      §7↳ Discord verbinden"
            const val GENERAL_HEADER = "\n§e§l  ℹ Allgemein"
            const val HELP = "    §f/cloudly help\n      §7↳ Dieses Menü anzeigen"
        }
    }
    
    // ========== Schutzmechanismen ==========
    object Protections {
        object CommandBlock {
            const val INTERACT_BLOCKED = "§c§l✗ §cCommandblöcke sind für dich deaktiviert"
            fun itemsReplaced(amount: Int): String {
                val noun = if (amount == 1) "Commandblock" else "Commandblöcke"
                return "§c§l✗ §c$amount $noun wurden durch Stein ersetzt"
            }
            fun notifyAdmins(player: String, amount: Int): String {
                val noun = if (amount == 1) "Commandblock" else "Commandblöcke"
                return "§6§l⚠ §6Spieler §e$player§6 hatte §e$amount§6 $noun im Inventar - ersetzt durch Stein"
            }
        }
    }

    // ========== GUI Nachrichten ==========
    object Gui {
        object Whitelist {
            fun title(count: Int) = "§6§lCloudly Whitelist §8- §7$count Spieler"
            const val REFRESHED = "§a✓ Whitelist GUI wurde aktualisiert!"
            fun playerDetails(player: String) = "§eZeige Details für §f$player§e:"
            fun playerRemoved(player: String) = "§a✓ Spieler §f$player§a wurde von der Whitelist entfernt!"
            fun removeFailed(player: String) = "§c✗ Fehler beim Entfernen von Spieler §f$player§c von der Whitelist!"
            
            // Navigation
            const val PREVIOUS_PAGE = "§a§lVorherige Seite"
            const val NEXT_PAGE = "§a§lNächste Seite"
            fun previousPageLore(page: Int) = "§7Klicke um zu Seite $page zu gehen"
            fun nextPageLore(page: Int) = "§7Klicke um zu Seite $page zu gehen"
            
            // Info Panel
            const val INFO_TITLE = "§6§lWhitelist Informationen"
            fun infoTotalPlayers(count: Int) = "§7Spieler insgesamt: §f$count"
            fun infoCurrentPage(current: Int, total: Int) = "§7Aktuelle Seite: §f$current§7/§f$total"
            fun infoPlayersPerPage(count: Int) = "§7Spieler pro Seite: §f$count"
            const val INFO_ADD_COMMAND = "§eVerwende §f/cloudly whitelist add <spieler>§e um Spieler hinzuzufügen"
            const val INFO_REMOVE_COMMAND = "§eVerwende §f/cloudly whitelist remove <spieler>§e um Spieler zu entfernen"
            
            // Spieler Details
            fun playerAddedBy(name: String) = "§7Hinzugefügt von: §f$name"
            fun playerAddedOn(date: String) = "§7Hinzugefügt am: §f$date"
            fun playerDiscordVerified(username: String) = "§7Discord: §f$username §7(§aVerifiziert§7)"
            fun playerDiscordConnected(username: String) = "§7Discord: §f$username §7(§eVerbunden§7)"
            const val PLAYER_DISCORD_NOT_CONNECTED = "§7Discord: §cNicht verbunden"
            const val PLAYER_OP_STATUS = "§6⭐ Server-Operator"
            const val PLAYER_ADMIN_STATUS = "§c⚡ Administrator"
            
            // Aktionen
            const val ACTIONS_TITLE = "§e§lAktionen:"
            const val ACTION_LEFT_CLICK = "§7• §fLinksklick: Details anzeigen"
            const val ACTION_RIGHT_CLICK = "§7• §cRechtsklick: Von Whitelist entfernen"
            
            // Sonstige
            const val ONLY_PLAYERS = "§c✗ Dieser Befehl kann nur von Spielern verwendet werden"
            const val NO_PERMISSION_REMOVE = "§c✗ Du hast keine Berechtigung, Spieler von der Whitelist zu entfernen"
            const val REFRESH_BUTTON = "§e§lAktualisieren"
            const val REFRESH_LORE = "§7Klicke um die Whitelist zu aktualisieren"
            const val CONSOLE = "Konsole"
            const val UNKNOWN = "Unbekannt"
        }
    }
    
    // ========== Fehlernachrichten ==========
    object Error {
        const val UNKNOWN = "Ein unbekannter Fehler ist aufgetreten"
        const val FILE_NOT_FOUND = "Datei nicht gefunden"
        const val PERMISSION_DENIED = "Zugriff verweigert"
    }
    
    // ========== Spielerverbindung ==========
    object PlayerConnection {
        object Join {
            fun chat(playerName: String) = "§8[§a+§8] §f$playerName §7hat den Server betreten"
            fun console(playerName: String) = "Spieler $playerName hat den Server betreten"
        }
        
        object Leave {
            fun chat(playerName: String) = "§8[§c-§8] §f$playerName §7hat den Server verlassen"
            fun console(playerName: String) = "Spieler $playerName hat den Server verlassen"
        }
    }
    
    // ========== Warteschlange ==========
    object Queue {
        const val OPERATOR_BYPASS = "§a§l✓ §aWarteschlange umgehen (Operator-Privileg)"
        fun positionWhitelisted(position: Int, total: Int) = 
            "§6§lWarteschlangenposition: §e$position§6/§e$total\n§7Du bist auf der Whitelist - Bevorzugte Warteschlange\n§7Bitte warte, du wirst bald verbunden..."
        fun positionFirstJoin(position: Int, total: Int) = 
            "§6§lWarteschlangenposition: §e$position§6/§e$total\n§7Erster Beitritt - Mittlere Priorität\n§7Bitte warte, du wirst bald verbunden..."
        fun positionRegular(position: Int, total: Int) = 
            "§6§lWarteschlangenposition: §e$position§6/§e$total\n§7Bitte warte, du wirst bald verbunden..."
    }
}
