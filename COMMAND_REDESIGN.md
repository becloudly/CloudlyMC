# Command Structure Redesign

This document illustrates the improvements made to the command structure and messaging system.

## Design Goals

1. **Consistent Visual Hierarchy** - Clear organization of information
2. **Better Readability** - Improved spacing and formatting
3. **Professional Appearance** - Polished look with consistent styling
4. **Category Organization** - Commands grouped by purpose
5. **Enhanced Feedback** - Better success/error indicators

## Design Patterns

### Color Scheme
- `§6§l` - **Bold Gold** for main headers
- `§e§l` - **Bold Yellow** for section headers
- `§a§l` - **Bold Green** for success messages
- `§c§l` - **Bold Red** for error messages
- `§6§l` - **Bold Yellow** for warnings
- `§f` - **White** for primary text/commands
- `§7` - **Gray** for descriptions and secondary info
- `§8` - **Dark Gray** for decorative elements
- `§e` - **Yellow** for highlighted values (player names, usernames)

### Visual Elements
- `§8§m───────────§r` - Separator lines
- `⚙` - Administration commands
- `📋` - Whitelist management
- `🔗` - Discord integration
- `ℹ` - General/help
- `▪` - List bullets
- `│` - Vertical separator
- `↳` - Command description arrow
- `✓` - Success indicator
- `✗` - Error indicator
- `⏳` - Loading/processing indicator
- `⚠` - Warning indicator

## Before and After Examples

### Help Command

**BEFORE:**
```
Available Commands
  /cloudly reload [config|lang|all] - Hot-reload plugin components
  /cloudly info - Show plugin information
  /cloudly help - Show this help menu
  /cloudly whitelist <add|remove|list|gui|on|off|reload|info> - Manage whitelist
  /cloudly connect <discord_username> - Connect your Discord account
```

**AFTER:**
```
────────────────────────────────────────────────
    Cloudly Command Help
────────────────────────────────────────────────

  ⚙ Administration Commands
    /cloudly reload [config|lang|all]
      ↳ Hot-reload plugin components
    /cloudly info
      ↳ Show plugin information

  📋 Whitelist Management
    /cloudly whitelist <subcommand>
      ↳ add, remove, list, gui, on, off, reload, info

  🔗 Discord Integration
    /cloudly connect <discord_username>
      ↳ Link your Discord account

  ℹ General Commands
    /cloudly help
      ↳ Show this help menu
────────────────────────────────────────────────
```

### Info Command

**BEFORE:**
```
Cloudly Plugin Information
  • Version: 0.0.1-alpha_11
  • Language: en
  • Debug Mode: false
  • Server Type: Paper/Spigot
  • Author: Cloudly - https://becloudly.eu
```

**AFTER:**
```
────────────────────────────────────────────────
    Plugin Information
────────────────────────────────────────────────
  ▪ Version: 0.0.1-alpha_11
  ▪ Language: en
  ▪ Debug Mode: false
  ▪ Server Type: Paper/Spigot
  ▪ Author: Cloudly
    ↳ https://becloudly.eu
────────────────────────────────────────────────
```

### Whitelist List Command

**BEFORE:**
```
Whitelisted Players (5 total):
  • PlayerOne (Added: Mon Jan 01 12:00:00 UTC 2024)
  • PlayerTwo (Added: Mon Jan 01 13:00:00 UTC 2024)
  • PlayerThree (Added: Mon Jan 01 14:00:00 UTC 2024)
```

**AFTER:**
```
────────────────────────────────────────────────
    Whitelisted Players (5 total)
────────────────────────────────────────────────
  ▪ PlayerOne │ Added: Mon Jan 01 12:00:00 UTC 2024
  ▪ PlayerTwo │ Added: Mon Jan 01 13:00:00 UTC 2024
  ▪ PlayerThree │ Added: Mon Jan 01 14:00:00 UTC 2024
────────────────────────────────────────────────
```

### Whitelist Info Command

**BEFORE:**
```
Whitelist Information - PlayerOne:
  • Added by: Admin
  • Added on: Mon Jan 01 12:00:00 UTC 2024
  - Discord: username#1234 (Verified)
```

**AFTER:**
```
────────────────────────────────────────────────
    Whitelist Info │ PlayerOne
────────────────────────────────────────────────
  ▪ Added by: Admin
  ▪ Added on: Mon Jan 01 12:00:00 UTC 2024
  ▪ Discord: username#1234 (Verified)
────────────────────────────────────────────────
```

### Success/Error Messages

**BEFORE:**
```
✓ Player PlayerOne has been added to the whitelist
✗ Player PlayerTwo is not on the whitelist
✗ You don't have permission to use this command.
```

**AFTER:**
```
✓ Player PlayerOne has been added to the whitelist
✗ Player PlayerTwo is not on the whitelist
✗ You don't have permission to use this command.
```
_(Success and error messages now use bold formatting for icons)_

### Discord Connect Messages

**BEFORE:**
```
⏳ Verifying Discord account username...
✓ Successfully connected Discord account username!
✗ Discord user username not found or not accessible by the bot
```

**AFTER:**
```
⏳ Verifying Discord account username...
✓ Successfully connected Discord account username!
✗ Discord user username not found or not accessible by the bot
⏳ Please wait 30 seconds before using this command again
```
_(Enhanced with bold formatting and better value highlighting)_

## Implementation Details

### Files Modified
1. `app/src/main/resources/lang/en.yml` - English language file
2. `app/src/main/resources/lang/de.yml` - German language file
3. `app/src/main/kotlin/de/cloudly/commands/CloudlyCommand.kt` - Command handler

### Key Changes
- **Minimal code changes** - Only added message keys and separators
- **Language file focused** - Most changes are in YAML files
- **Backward compatible** - All existing message keys maintained
- **Consistent structure** - Same pattern across all commands
- **Bilingual support** - English and German translations match

## Testing Recommendations

1. Test each command with different permission levels
2. Verify separator lines render correctly in Minecraft client
3. Check text wrapping in different client window sizes
4. Test with both English and German language settings
5. Verify color codes display correctly
6. Test on both Paper and Folia servers

## Future Enhancements

Potential improvements for future iterations:
- Add hover text for commands (using Adventure API)
- Clickable commands in help menu
- Pagination for long lists
- Custom formatting profiles (compact/detailed modes)
- Per-player language preferences
- Rich formatting in GUI tooltips
