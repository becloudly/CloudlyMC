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

    // ========== Moderation & Admin Aktionen ==========
    object Moderation {
        const val NO_PERMISSION = "§c§l✗ §cDu hast keine Berechtigung für diese Aktion."
        const val DEFAULT_BAN_REASON = "Verstoß gegen Serverregeln"

        fun discordNotLinked(player: String) = "§e⚠ Spieler §f$player§e hat keine Discord-Verknüpfung."
        fun discordUnlinkSuccess(player: String) = "§a§l✓ §aDiscord-Verknüpfung von §f$player§a wurde entfernt."
        fun discordUnlinkFailed(player: String) = "§c§l✗ §cDiscord-Verknüpfung von §f$player§c konnte nicht entfernt werden."
        fun discordForceRelink(player: String) = "§a§l✓ §aDiscord-Verifizierung für §f$player§a wurde zurückgesetzt."
        const val DISCORD_FORCE_PLAYER = "§e⚠ Deine Discord-Verknüpfung wurde vom Team zurückgesetzt. Bitte führe §f/cloudly link <discord>§e erneut aus."

        fun kickSuccess(player: String) = "§a§l✓ §aSpieler §f$player§a wurde gekickt."
        fun kickOffline(player: String) = "§c§l✗ §cSpieler §f$player§c ist nicht online."
        const val KICK_MESSAGE = "§c§lDu wurdest vom Team gekickt. Bitte melde dich im Support."

        fun teleportToPlayerSuccess(player: String) = "§a§l✓ §aDu wurdest zu §f$player§a teleportiert."
        fun teleportPlayerHereSuccess(player: String) = "§a§l✓ §aSpieler §f$player§a wurde zu dir teleportiert."
        fun teleportTargetOffline(player: String) = "§c§l✗ §cSpieler §f$player§c ist nicht online."
        const val TELEPORT_SAME_PLAYER = "§c§l✗ §cDu kannst dich nicht zu dir selbst teleportieren."

        fun tempBanSuccess(player: String, duration: String) = "§a§l✓ §aSpieler §f$player§a wurde für §e$duration§a gebannt."
        fun permaBanSuccess(player: String) = "§4§l✓ §cSpieler §f$player§c wurde permanent gebannt und vollständig entfernt."
        fun banAlreadyActive(player: String) = "§e⚠ Spieler §f$player§e ist bereits gebannt."
        const val BAN_STORAGE_ERROR = "§c§l✗ §cDer Bann konnte nicht gespeichert werden. Bitte prüfe die Konsole."
        fun playerNotWhitelisted(player: String) = "§c§l✗ §cSpieler §f$player§c ist nicht in der Whitelist-Datenbank."
        fun unbanSuccess(player: String) = "§a§l✓ §aDer Bann von §f$player§a wurde aufgehoben."

        object Target {
            const val RELINK_NOTICE = "§e⚠ Deine Discord-Verknüpfung wurde zurückgesetzt. Bitte verifiziere dich erneut."
            fun tempBan(expiry: String, reason: String?): String {
                val reasonLine = reason?.let { "\n§7Grund: §f$it" } ?: ""
                return "§c§l✗ §cDu wurdest temporär gebannt.\n§7Endet am: §f$expiry$reasonLine"
            }
            fun permanentBan(reason: String?): String {
                val reasonLine = reason?.let { "\n§7Grund: §f$it" } ?: ""
                return "§c§l✗ §cDu wurdest permanent vom Server gebannt.$reasonLine"
            }
        }

        object Login {
            fun temporary(remaining: String, reason: String?): String {
                val reasonLine = reason?.let { "\n§7Grund: §f$it" } ?: ""
                return "§c§l✗ §cDu bist noch §e$remaining§c vom Server gebannt.$reasonLine"
            }
            fun permanent(reason: String?): String {
                val reasonLine = reason?.let { "\n§7Grund: §f$it" } ?: ""
                return "§c§l✗ §cDu wurdest permanent vom Server gebannt.$reasonLine"
            }
        }
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
            
            fun infoHeader(player: String) = "§8§m──────────────────§r\n§6§l Info §8│ §f$player\n§8§m──────────────────§r"
            fun infoAddedBy(name: String) = "  §e▪ §fHinzugefügt von§8: §7$name"
            fun infoAddedOn(date: String) = "  §e▪ §fHinzugefügt am§8: §7$date"
            const val INFO_FOOTER = "§8§m──────────────────§r"
        }

        object Admin {
            const val GUI_USAGE = "  §7Verwendung§8: §f/cloudly admin gui §8[§7seite§8]"
            const val PLAYERS_ONLY = "§c§l✗ §cDieser Befehl kann nur von Spielern verwendet werden"
        }
        
        // Discord Befehle
        object Discord {
            const val DISABLED = "§c§l✗ §cDiscord-Integration ist deaktiviert oder nicht korrekt konfiguriert"
            const val PLAYERS_ONLY = "§c§l✗ §cDieser Befehl kann nur von Spielern verwendet werden"
            const val NOT_WHITELISTED = "§c§l✗ §cDu musst auf der Whitelist stehen, um dein Discord-Konto zu verbinden"
            fun alreadyConnected(discordUsername: String) = "§c§l✗ §cDu hast bereits das Discord-Konto §e$discordUsername§c verbunden"
            const val LINK_USAGE = "  §7Verwendung§8: §f/cloudly link §8<§7discord_benutzername§8>"
            const val UNLINK_USAGE = "  §7Verwendung§8: §f/cloudly unlink"
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
            const val CODE_SENT = "§a§l✓ §aVerifizierungscode gesendet! §7Prüfe deine Discord-Nachrichten und gib den Code hier ein."
            const val CODE_SEND_FAILED = "§c§l✗ §cDer Verifizierungscode konnte nicht gesendet werden. Prüfe deine Discord-Direktnachrichten-Einstellungen und versuche es erneut."
            const val CODE_ALREADY_PENDING = "§e⚠ §eDu hast bereits einen aktiven Verifizierungscode. Gib ihn im Chat ein oder warte bis er abläuft."
            const val CODE_INVALID = "§c§l✗ §cDer eingegebene Code ist ungültig. Prüfe deine Discord-Nachrichten und versuche es erneut."
            const val CODE_EXPIRED = "§c§l✗ §cDieser Verifizierungscode ist abgelaufen. Verwende §f/cloudly link §7erneut, um einen neuen Code zu erhalten."
            const val ACCOUNT_ALREADY_IN_USE = "§c§l✗ §cDieses Discord-Konto ist bereits mit einem anderen Minecraft-Spieler verbunden."
            const val NO_PENDING_VERIFICATION = "§c§l✗ §cEs ist kein Verifizierungscode aktiv. Verwende §f/cloudly link §7erneut."
            fun unlinkedSuccessfully(discordUsername: String) = "§a§l✓ §aDiscord-Konto §e$discordUsername§a wurde getrennt."
            const val UNLINKED_NO_ACCOUNT = "§c§l✗ §cDu hast kein verknüpftes Discord-Konto."
            const val UNLINKED_PENDING_CANCELLED = "§e⚠ §eAktive Verifizierung wurde abgebrochen."
            const val UNLINK_FAILED = "§c§l✗ §cDie Trennung deines Discord-Kontos ist fehlgeschlagen. Bitte versuche es erneut."
            fun joinMissingRole(roleName: String?) = "§c§l✗ §cDein verknüpftes Discord-Konto ${roleName?.let { "besitzt die erforderliche Rolle '$it' nicht" } ?: "besitzt nicht die erforderliche Rolle"}."
            const val JOIN_NOT_MEMBER = "§c§l✗ §cDein verknüpftes Discord-Konto ist nicht mehr auf dem Discord-Server."
            const val DM_CONTENT = "Hey! Dein Verifizierungscode lautet: %s. Gib ihn innerhalb von 5 Minuten im Minecraft-Chat ein."
            
            // Discord Verifizierung
            const val VERIFICATION_REQUIRED = "§6§l⚠ §6Discord-Verifizierung erforderlich!\n§7Du musst deinen Discord-Account verbinden, um zu spielen.\n§7Du hast §e5 Minuten§7 Zeit zur Verifizierung."
            const val VERIFICATION_COMMAND = "§7Verwende §f/cloudly link <discord_username>§7 zur Verifizierung"
            const val VERIFICATION_SUCCESS = "§a§l✓ §aDiscord-Verifizierung erfolgreich! Du kannst jetzt spielen."
            const val VERIFICATION_TIMEOUT = "§c§l✗ §cDu wurdest gekickt, weil du deinen Discord-Account nicht innerhalb von 5 Minuten verifiziert hast"
            const val VERIFICATION_WARNING_3MIN = "§6§l⚠ §6Discord-Verifizierung Warnung: noch §e3 Minuten"
            const val VERIFICATION_WARNING_2MIN = "§6§l⚠ §6Discord-Verifizierung Warnung: noch §e2 Minuten"
            const val VERIFICATION_WARNING_30SEC = "§c§l⚠ §cDiscord-Verifizierung Warnung: noch §e30 Sekunden§c!"
            const val VERIFICATION_CHAT_BLOCKED = "§c§l✗ §cDu kannst nicht chatten, bis du deinen Discord-Account verifiziert hast"
            const val VERIFICATION_COMMAND_BLOCKED = "§c§l✗ §cDu kannst nur /cloudly link oder /cloudly unlink verwenden, bis du deinen Discord-Account verifiziert hast"
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
            const val INFO = "    §f/cloudly info\n      §7↳ Plugin-Info anzeigen"
            const val ADMIN_GUI = "    §f/cloudly admin gui\n      §7↳ Admin-Dashboard öffnen"
            const val WHITELIST_HEADER = "\n§e§l  📋 Whitelist"
            const val WHITELIST = "    §f/cloudly whitelist §8<§7unterbefehl§8>\n      §7↳ add, remove, list, on, off, info"
            const val DISCORD_HEADER = "\n§e§l  🔗 Discord"
            const val DISCORD_CONNECT = "    §f/cloudly link §8<§7discord_benutzername§8>\n      §7↳ Discord verbinden"
            const val DISCORD_UNLINK = "    §f/cloudly unlink\n      §7↳ Discord-Verknüpfung entfernen"
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
                return """
                    §8§m────────────────────────§r
                    §6§l⚠ §6Commandblock-Schutz aktiviert
                    §7Spieler§8: §f$player
                    §7Menge§8: §f$amount $noun
                    §7Aktion§8: §fDurch Stein ersetzt
                    §8§m────────────────────────§r
                """.trimIndent()
            }

            fun notifyAdminsLog(player: String, amount: Int): String {
                val noun = if (amount == 1) "Commandblock" else "Commandblöcke"
                return "[AntiCommandblock] Spieler $player hielt $amount $noun; ersetzt durch Stein"
            }
        }
    }

    // ========== GUI Nachrichten ==========
    object Gui {
        object Admin {
            fun title(count: Int) = "§6§lCloudly Admin §8- §7$count Spieler"
            const val REFRESHED = "§a✓ Admin-Dashboard wurde aktualisiert!"
            const val NO_PLAYERS_TRACKED = "§e⚠ Es sind aktuell keine Spieler im System erfasst"
            fun playerLabel(player: String) = "§a§l$player"
            fun playerRemoved(player: String) = "§a✓ Spieler §f$player§a wurde aus dem System entfernt"
            fun removeFailed(player: String) = "§c✗ Spieler §f$player§c konnte nicht entfernt werden"

            const val PREVIOUS_PAGE = "§a§lVorherige Seite"
            const val NEXT_PAGE = "§a§lNächste Seite"
            fun previousPageLore(page: Int) = "§7Klicke, um zu Seite $page zu wechseln"
            fun nextPageLore(page: Int) = "§7Klicke, um zu Seite $page zu wechseln"

            const val INFO_TITLE = "§6§lÜbersicht"
            fun infoUuid(uuid: String) = "§7UUID: §f$uuid"
            fun infoTotalPlayers(count: Int) = "§7Verwaltete Spieler: §f$count"
            fun infoCurrentPage(current: Int, total: Int) = "§7Aktuelle Seite: §f$current§7/§f$total"
            fun infoPlayersPerPage(count: Int) = "§7Spieler pro Seite: §f$count"
            const val INFO_ADD_COMMAND = "§e/cloudly whitelist add <spieler> §7um Spieler aufzunehmen"
            const val INFO_REMOVE_COMMAND = "§e/cloudly whitelist remove <spieler> §7um Spieler zu entfernen"

            const val PENDING_ATTEMPTS_BUTTON = "§c§lWhitelist-Anfragen"
            fun pendingAttemptsLore(count: Int) = if (count > 0) {
                "§7Offene Anfragen: §f$count"
            } else {
                "§7Keine offenen Anfragen"
            }

            fun playerAddedBy(name: String) = "§7Hinzugefügt von: §f$name"
            fun playerAddedOn(date: String) = "§7Hinzugefügt am: §f$date"
            fun playerReason(reason: String) = "§7Grund: §f$reason"
            fun playerDiscordVerified(username: String) = "§7Discord: §f$username §7(§aVerifiziert§7)"
            fun playerDiscordConnected(username: String) = "§7Discord: §f$username §7(§eVerbunden§7)"
            const val PLAYER_DISCORD_NOT_CONNECTED = "§7Discord: §cNicht verbunden"
            const val PLAYER_OP_STATUS = "§6⭐ Server-Operator"
            const val CONSOLE = "Konsole"
            const val UNKNOWN = "Unbekannt"

            const val ACTIONS_TITLE = "§e§lAktionen:"
            const val ACTION_LEFT_CLICK = "§7• §fLinksklick: Moderations-Tools öffnen"
            const val ACTION_RIGHT_CLICK = "§7• §cRechtsklick: Aus Whitelist entfernen"
            const val NO_PERMISSION_ADMIN = "§c✗ Du hast keine Berechtigung für diese Tools"

            const val REFRESH_BUTTON = "§e§lAktualisieren"
            const val REFRESH_LORE = "§7Klicke, um die Daten neu zu laden"
            const val SEARCH_BUTTON = "§b§lSpieler suchen"
            const val SEARCH_HINT_LEFT = "§7• §fLinksklick: Namen eingeben"
            const val SEARCH_HINT_RIGHT = "§7• §cRechtsklick: Filter zurücksetzen"
            fun searchActive(query: String) = "§7Aktiver Filter: §f${query.take(24)}"
            const val SEARCH_PROMPT = "§eBitte gib im Chat einen Spielernamen oder Teil eines Namens ein. Tippe 'cancel' zum Abbrechen."
            const val SEARCH_PROMPT_CANCEL = "§7(Die Nachricht wird nicht an andere Spieler gesendet)"
            fun searchApplied(query: String) = "§a✓ Filter gesetzt auf §f$query"
            const val SEARCH_CLEARED = "§a✓ Filter entfernt"
            const val SEARCH_CANCELLED = "§eSuche abgebrochen."
            fun searchNoResults(query: String) = "§e⚠ Keine Spieler gefunden für §f$query"
            const val SCOPE_TITLE = "§5§lAnsicht"
            const val SCOPE_HINT = "§7Linksklick: Ansicht wechseln"
            const val SCOPE_LABEL_ALL = "Alle Spieler"
            const val SCOPE_LABEL_ONLINE = "Nur Online"
            fun scopeStatusAll() = "§7Anzeigen: §fAlle Spieler"
            fun scopeStatusOnline() = "§7Anzeigen: §aNur Online"
            fun scopeChanged(label: String) = "§a✓ Ansicht gesetzt auf §f$label"
            const val SORT_TITLE = "§6§lSortierung"
            const val SORT_HINT = "§7Linksklick: Umschalten"
            const val SORT_LABEL_RECENT = "Neuste zuerst"
            const val SORT_LABEL_ALPHABETICAL = "Alphabetisch"
            fun sortStatusRecent() = "§7Sortierung: §fNeuste zuerst"
            fun sortStatusAlphabetical() = "§7Sortierung: §fAlphabetisch"
            fun sortChanged(label: String) = "§a✓ Sortierung gesetzt auf §f$label"

            const val STATS_TITLE = "§6§lDashboard"
            fun statsTotalPlayers(count: Int) = "§7Gesamt im System: §f$count"
            fun statsMatchingPlayers(count: Int) = "§7Gefiltert: §f$count"
            fun statsPending(count: Int) = if (count > 0) "§7Offene Anfragen: §c$count" else "§7Offene Anfragen: §f0"
            fun statsPage(current: Int, total: Int) = "§7Seite: §f$current§7/§f$total"
            fun statsScope(label: String) = "§7Ansicht: §f$label"
            fun statsActiveSearch(query: String) = "§7Suche aktiv: §f${query.take(24)}"

            const val EMPTY_STATE_TITLE = "§7Keine Spieler gefunden"
            fun emptyStateNoResult(query: String) = "§7Kein Treffer für §f$query"
            const val EMPTY_STATE_DEFAULT = "§7Keine Einträge vorhanden."
            const val EMPTY_STATE_SCOPE_ONLINE = "§7Es ist aktuell kein Spieler online."
            const val EMPTY_STATE_HINT = "§7Passe die Suche an."

            fun playerOnlineStatus(isOnline: Boolean) = if (isOnline) "§7Status: §aOnline" else "§7Status: §cOffline"
        }

        object PlayerAdmin {
            fun title(player: String) = "§6§lAdmin-Tools §8| §f$player"
            fun tempBanTitle(player: String) = "§c§lBann auswählen §8| §f$player"
            const val PLAYER_INFO_TITLE = "§e§lSpieler-Informationen"
            const val DISCORD_INFO_TITLE = "§9§lDiscord"
            const val DISCORD_NOT_LINKED = "§7Discord: §cNicht verbunden"
            const val STATUS_OVERVIEW_TITLE = "§6§lStatus"
            const val CATEGORY_OVERVIEW_TITLE = "§6§lÜbersicht"
            const val CATEGORY_OVERVIEW_DESCRIPTION = "§7Schnellzugriff auf alle Kategorien"
            const val CATEGORY_PLAYER_ACTIONS_TITLE = "§b§lPlayeractions"
            const val CATEGORY_PLAYER_ACTIONS_DESCRIPTION = "§7Teleport, Inventar & Tools"
            const val CATEGORY_PUNISHMENTS_TITLE = "§c§lBestrafung"
            const val CATEGORY_PUNISHMENTS_DESCRIPTION = "§7Kick- und Bannverwaltung"
            const val CATEGORY_DISCORD_TITLE = "§9§lDiscord"
            const val CATEGORY_DISCORD_DESCRIPTION = "§7Verknüpfung & Verifizierung"
            const val CATEGORY_OPEN_HINT = "§7Klicke, um die Kategorie zu öffnen"
            const val TAB_ACTIVE = "§a✔ Aktive Ansicht"
            const val TAB_HINT = "§7Klicke, um zu wechseln"
            fun discordLinked(username: String, verified: Boolean): String {
                val status = if (verified) "§aVerifiziert" else "§eVerbunden"
                return "§7Discord: §f$username §8(§7$status§8)"
            }
            fun discordLastSync(timestamp: String) = "§7Verbunden seit: §f$timestamp"
            fun discordVerifiedAt(timestamp: String) = "§7Verifiziert am: §f$timestamp"
            fun infoUuid(uuid: String) = "§7UUID: §f$uuid"
            fun infoAddedBy(name: String) = Admin.playerAddedBy(name)
            fun infoAddedOn(date: String) = Admin.playerAddedOn(date)
            fun infoReason(reason: String) = "§7Grund: §f$reason"
            fun infoOnline(isOnline: Boolean) = if (isOnline) "§7Status: §aOnline" else "§7Status: §cOffline"
            fun infoWorld(world: String?) = "§7Welt: §f${world ?: "Unbekannt"}"
            fun infoGamemode(mode: String?) = "§7Spielmodus: §f${mode ?: "Unbekannt"}"
            fun infoLocation(x: Int, y: Int, z: Int) = "§7Position: §f$x§7, §f$y§7, §f$z"
            fun infoPing(ping: Int) = "§7Ping: §f$ping ms"
            const val BAN_STATUS_CARD_TITLE = "§c§lBannstatus"
            const val BAN_STATUS_NONE = "§aKein aktiver Bann"
            const val BAN_STATUS_ACTIVE = "§cAktiver Bann"
            const val BAN_STATUS_PERMANENT = "§4Permanenter Bann"
            fun banStatusUntil(until: String) = "§7Läuft ab: §f$until"
            const val BUTTON_UNLINK = "§cDiscord trennen"
            const val BUTTON_UNLINK_LORE = "§7Entfernt die aktuelle Discord-Verknüpfung"
            const val BUTTON_FORCE_RELINK = "§6Neu verifizieren"
            const val BUTTON_FORCE_RELINK_LORE = "§7Setzt die Verifizierung zurück und fordert einen neuen Link an"
            const val BUTTON_KICK = "§c§lKick"
            const val BUTTON_KICK_LORE = "§7Wirft den Spieler sofort vom Server"
            const val BUTTON_TEMP_BAN = "§6§lTemporärer Bann"
            const val BUTTON_VIEW_INVENTORY = "§bInventar ansehen"
            const val BUTTON_VIEW_INVENTORY_LORE = "§7Öffnet das Inventar schreibgeschützt"
            const val BUTTON_TELEPORT_TO_PLAYER = "§dTeleport zu Spieler"
            const val BUTTON_TELEPORT_TO_PLAYER_LORE = "§7Teleportiert dich zum Spieler"
            const val BUTTON_TELEPORT_PLAYER_HERE = "§dSpieler her teleportieren"
            const val BUTTON_TELEPORT_PLAYER_HERE_LORE = "§7Teleportiert den Spieler zu dir"
            const val BUTTON_TEMP_BAN_LORE = "§7Öffnet Bann-Dauern zur Auswahl"
            const val BUTTON_PERMA_BAN = "§4§lPermanenter Bann"
            const val BUTTON_PERMA_BAN_LORE = "§7Entfernt den Spieler dauerhaft und löscht Daten"
            const val BUTTON_BACK = "§7Zurück zur Übersicht"
            const val BUTTON_BACK_LORE = "§7Klicke, um zur Übersicht zurückzukehren"
            const val BUTTON_BACK_TO_MAIN = "§7Zurück zu Kategorien"
            const val BUTTON_BACK_TO_MAIN_LORE = "§7Klicke, um zur Übersicht zurückzukehren"
            const val ACTION_NO_PERMISSION = "§cKeine Berechtigung"
            const val ACTION_TARGET_OFFLINE = "§cSpieler ist offline"
            fun tempBanOptionLabel(label: String) = "§e$label"
            const val TEMP_BAN_OPTION_LORE = "§7Klicke, um diesen Bann anzuwenden"
            fun tempBanDuration(label: String) = "§7Dauer: §f$label"
            fun tempBanHeader(player: String) = "§c§lBann auswählen §8| §f$player"
            const val TEMP_BAN_HEADER_HINT = "§7Wähle eine Dauer für den temporären Bann"
            const val TEMP_BAN_BACK = "§7Zurück"
            const val TEMP_BAN_BACK_LORE = "§7Zurück zu den Admin-Tools"
            fun inventoryTitle(player: String) = "§6§lInventar §8| §f$player"
            const val CONSOLE = "Konsole"
            const val UNKNOWN = "Unbekannt"
        }

        object PendingWhitelist {
            fun title(count: Int) = "§6§lWhitelist-Anfragen §8- §7$count Spieler"
            const val NO_ATTEMPTS = "§e⚠ Es liegen keine Join-Versuche vor"
            fun playerLabel(name: String) = "§c§l$name"
            fun infoUuid(uuid: String) = "§7UUID: §f$uuid"
            fun infoFirstAttempt(timestamp: String) = "§7Erster Versuch: §f$timestamp"
            fun infoLastAttempt(timestamp: String) = "§7Letzter Versuch: §f$timestamp"
            fun infoAttempts(count: Int) = "§7Versuche: §f$count"
            fun infoPage(current: Int, total: Int) = "§7Seite: §f$current§7/§f$total"
            fun infoAddress(address: String) = "§7Letzte Adresse: §f$address"
            const val ACTIONS_TITLE = "§e§lAktionen:"
            const val ACTION_LEFT_CLICK = "§7• §fLinksklick: Details anzeigen"
            const val ACTION_RIGHT_CLICK = "§7• §cRechtsklick: Eintrag entfernen"
            fun detailTitle(player: String) = "§6§lAnfrage §8| §f$player"
            const val BUTTON_ADD = "§aZur Whitelist hinzufügen"
            const val BUTTON_ADD_LORE = "§7Fügt den Spieler der Whitelist hinzu"
            const val BUTTON_DISMISS = "§cEintrag verwerfen"
            const val BUTTON_DISMISS_LORE = "§7Entfernt den Eintrag ohne weitere Aktion"
            const val BUTTON_BACK = "§7Zurück"
            const val BUTTON_BACK_LORE = "§7Klicke, um zur Übersicht zurückzukehren"
            const val DETAIL_NO_ADDRESS = "§7Keine Adresse verfügbar"
            const val DETAIL_META_TITLE = "§6§lVersuchsdaten"
            const val DETAIL_MESSAGE_HEADER = "§7Letzte Nachricht:"
            const val DETAIL_NO_MESSAGE = "§7Keine Nachricht verfügbar"
            const val ENTRY_DISMISSED = "§a✓ Eintrag wurde entfernt"
            fun buttonAddSuccess(player: String) = "§a✓ Spieler §f$player§a wurde hinzugefügt"
            fun buttonAddFailed(player: String) = "§c✗ Spieler §f$player§c konnte nicht hinzugefügt werden"
        }

        object Whitelist {
            fun title(count: Int) = "§6§lCloudly Whitelist §8- §7$count Spieler"
            const val PLAYER_OP_STATUS = "§6⭐ Server-Operator"
            const val CONSOLE = "Konsole"
            const val UNKNOWN = "Unbekannt"
            fun infoUuid(uuid: String) = "§7UUID: §f$uuid"
            fun playerAddedBy(name: String) = "§7Hinzugefügt von: §f$name"
            fun playerAddedOn(date: String) = "§7Hinzugefügt am: §f$date"
            fun playerReason(reason: String) = "§7Grund: §f$reason"
            fun playerDiscordVerified(username: String) = "§7Discord: §f$username §7(§aVerifiziert§7)"
            fun playerDiscordConnected(username: String) = "§7Discord: §f$username §7(§eVerbunden§7)"
            const val PLAYER_DISCORD_NOT_CONNECTED = "§7Discord: §cNicht verbunden"
            fun playerOnlineStatus(isOnline: Boolean) = if (isOnline) "§7Status: §aOnline" else "§7Status: §cOffline"
            const val ACTIONS_TITLE = "§e§lAktionen:"
            const val ACTION_LEFT_CLICK = "§7• §fLinksklick: Moderations-Tools öffnen"
            const val ACTION_RIGHT_CLICK = "§7• §cRechtsklick: Aus Whitelist entfernen"
            const val PREVIOUS_PAGE = "§a§lVorherige Seite"
            const val NEXT_PAGE = "§a§lNächste Seite"
            fun previousPageLore(page: Int) = "§7Klicke, um zu Seite $page zu wechseln"
            fun nextPageLore(page: Int) = "§7Klicke, um zu Seite $page zu wechseln"
            const val INFO_TITLE = "§6§lÜbersicht"
            fun infoTotalPlayers(count: Int) = "§7Whitelist-Einträge: §f$count"
            fun infoCurrentPage(current: Int, total: Int) = "§7Aktuelle Seite: §f$current§7/§f$total"
            fun infoPlayersPerPage(count: Int) = "§7Spieler pro Seite: §f$count"
            const val INFO_ADD_COMMAND = "§e/cloudly whitelist add <spieler>"
            const val INFO_REMOVE_COMMAND = "§e/cloudly whitelist remove <spieler>"
            const val REFRESH_BUTTON = "§e§lAktualisieren"
            const val REFRESH_LORE = "§7Klicke, um die Liste neu zu laden"
            const val REFRESHED = "§a✓ Whitelist-Ansicht aktualisiert"
            const val BUTTON_BACK = "§7Zurück"
            const val BUTTON_BACK_LORE = "§7Zurück zum Admin-Dashboard"
            fun playerRemoved(player: String) = "§a✓ Spieler §f$player§a wurde aus der Whitelist entfernt"
            fun removeFailed(player: String) = "§c✗ Spieler §f$player§c konnte nicht entfernt werden"
            const val NO_PERMISSION_ADMIN = "§c✗ Du hast keine Berechtigung für diese Tools"
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
