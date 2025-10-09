# Audit Logging Test Scenarios

This document outlines test scenarios for verifying the audit logging functionality implemented in WhitelistService.kt.

## Test Scenarios

### Test 1: Player Addition to Whitelist
**Command:** `/cloudly whitelist add TestPlayer`  
**Expected Log Output:**
```
[INFO] [AUDIT] [timestamp] - WHITELIST_ADD - Target: [player-uuid] - Actor: [admin-uuid] - Details: Username: TestPlayer
```

### Test 2: Player Removal from Whitelist
**Command:** `/cloudly whitelist remove TestPlayer`  
**Expected Log Output:**
```
[INFO] [AUDIT] [timestamp] - WHITELIST_REMOVE - Target: [player-uuid] - Actor: [admin-uuid] - Details: Username: TestPlayer
```

### Test 3: Enable Whitelist
**Command:** `/cloudly whitelist on`  
**Expected Log Output:**
```
[INFO] [AUDIT] [timestamp] - WHITELIST_ENABLE - Target: 00000000-0000-0000-0000-000000000000 - Actor: [admin-uuid] - Details: State changed to: enabled
```

### Test 4: Disable Whitelist
**Command:** `/cloudly whitelist off`  
**Expected Log Output:**
```
[INFO] [AUDIT] [timestamp] - WHITELIST_DISABLE - Target: 00000000-0000-0000-0000-000000000000 - Actor: [admin-uuid] - Details: State changed to: disabled
```

### Test 5: Discord Connection Update
**Command:** `/cloudly connect TestDiscordUser`  
**Expected Log Output:**
```
[INFO] [AUDIT] [timestamp] - DISCORD_UPDATE - Target: [player-uuid] - Actor: [player-uuid] - Details: Discord: TestDiscordUser, Verified: true
```

### Test 6: Console Actions
**Command:** Execute any whitelist command from console  
**Expected Log Output:**
```
[INFO] [AUDIT] [timestamp] - [ACTION] - Target: [target-uuid] - Actor: 00000000-0000-0000-0000-000000000000 - Details: [details]
```
**Note:** Console actions use UUID(0,0,0,0...) as the actor identifier

### Test 7: Player Addition with Reason
**Command:** `/cloudly whitelist add TestPlayer --reason "Trusted member"`  
**Expected Log Output:**
```
[INFO] [AUDIT] [timestamp] - WHITELIST_ADD - Target: [player-uuid] - Actor: [admin-uuid] - Details: Username: TestPlayer, Reason: Trusted member
```

## Backward Compatibility Tests

### Test 8: Default Parameters Work
**Code Test:** Call methods without actor parameters (should still compile and work)
```kotlin
whitelistService.removePlayer(playerUuid)  // Should work without removedBy
whitelistService.enable(true)              // Should work without changedBy
whitelistService.updatePlayerDiscord(uuid, connection)  // Should work without updatedBy
```

### Test 9: Existing Code Compatibility
**Verification:** Ensure all existing method calls in the codebase continue to work
- CloudlyCommand.kt: All updated calls should compile
- CloudlyPaper.kt: initialize() and shutdown() calls should be unaffected

## Security Compliance

### Test 10: Audit Trail Completeness
**Verification:** All whitelist modification operations should be logged:
- ✅ Adding players (both overloads)
- ✅ Removing players
- ✅ Updating Discord connections
- ✅ Enabling/disabling whitelist

### Test 11: Actor Information Accuracy
**Verification:** Actor UUID should correctly identify:
- Player UUIDs for in-game player actions
- Console UUID (00000000-0000-0000-0000-000000000000) for console actions
- System UUID (00000000-0000-0000-0000-000000000000) for global state changes

## Expected Audit Log Format

All audit logs follow this consistent format:
```
[AUDIT] [ISO-8601 Timestamp] - [ACTION_TYPE] - Target: [UUID] - Actor: [UUID] - Details: [Context-specific information]
```

### Action Types:
- `WHITELIST_ADD` - Player added to whitelist
- `WHITELIST_REMOVE` - Player removed from whitelist
- `WHITELIST_ENABLE` - Whitelist system enabled
- `WHITELIST_DISABLE` - Whitelist system disabled
- `DISCORD_UPDATE` - Discord connection updated

## Test Success Criteria

1. ✅ All whitelist modification operations generate audit logs
2. ✅ Logs include accurate timestamp, action, target, actor, and details
3. ✅ Actor information correctly identifies who performed the action
4. ✅ Backward compatibility is maintained (existing code works without modification)
5. ✅ No breaking changes to method signatures (default parameters used)
6. ✅ Logs are written to plugin logger (visible in server logs)

## Manual Testing Steps

1. Start the Minecraft server with the CloudlyMC plugin
2. Execute each test scenario command
3. Check server logs for the corresponding audit log entries
4. Verify the log format matches the expected output
5. Confirm actor UUIDs are correctly recorded
6. Test with both player and console command execution

## Future Enhancements

As noted in the implementation, the audit logging system includes a placeholder for:
- Storing audit logs in a separate file
- Storing audit logs in a database
- Implementing log rotation
- Adding audit log query capabilities

These enhancements can be implemented in future iterations without modifying the existing API.
