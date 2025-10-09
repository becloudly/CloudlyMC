# Batch Operations Usage Guide

This document demonstrates how to use the newly added batch operations in the storage layer.

## Overview

Two new methods have been added to the `DataStorage<T>` interface:
- `storeAll(items: Map<String, T>): Boolean` - Store multiple items at once
- `removeAll(keys: Set<String>): Boolean` - Remove multiple items at once

These methods are implemented in all storage backends (JSON, SQLite, MySQL) with optimized batch processing.

## Benefits

### Performance Improvements
- **JSON Storage**: Updates the in-memory map for all items, then writes to disk once (instead of writing once per item)
- **SQLite Storage**: Uses JDBC batch operations with transactions, reducing disk I/O and improving throughput
- **MySQL Storage**: Uses batch prepared statements with transactions, reducing network round-trips

### Example Performance Gains
- Storing 100 items individually: 100 disk/network operations
- Storing 100 items with `storeAll()`: 1 batch operation (up to 100x faster)

## Usage Examples

### Example 1: Batch Store
```kotlin
// Before: Inefficient individual stores
val storage = JsonDataStorage(plugin, "data.json")
storage.initialize()

data.forEach { (key, value) ->
    storage.store(key, value)  // Writes to disk each time
}

// After: Efficient batch store
val itemsToStore = mapOf(
    "player1" to "data1",
    "player2" to "data2",
    "player3" to "data3"
)
storage.storeAll(itemsToStore)  // Single write operation
```

### Example 2: Batch Remove
```kotlin
// Before: Inefficient individual removes
val keysToRemove = setOf("player1", "player2", "player3")
keysToRemove.forEach { key ->
    storage.remove(key)  // Multiple operations
}

// After: Efficient batch remove
storage.removeAll(keysToRemove)  // Single batch operation
```

### Example 3: SQLite with Transaction Safety
```kotlin
val storage = SqliteDataStorage(plugin, "database.db")
storage.initialize()

// Batch operations use transactions automatically
val bulkData = mapOf(
    "key1" to "value1",
    "key2" to "value2",
    // ... up to thousands of items
)

// If any item fails, the entire batch is rolled back
val success = storage.storeAll(bulkData)
if (success) {
    plugin.logger.info("Successfully stored ${bulkData.size} items")
} else {
    plugin.logger.warning("Batch store failed, no data was modified")
}
```

### Example 4: MySQL Batch Insert/Update
```kotlin
val storage = MysqlDataStorage(
    plugin, "localhost", 3306, 
    "mydb", "user", "password", "data_table"
)
storage.initialize()

// Batch insert with automatic duplicate key handling
val playerData = mapOf(
    "uuid1" to """{"name":"Player1","score":100}""",
    "uuid2" to """{"name":"Player2","score":200}""",
    "uuid3" to """{"name":"Player3","score":300}"""
)

// Uses ON DUPLICATE KEY UPDATE - existing records are updated
storage.storeAll(playerData)

// Later, batch remove players
val playersToRemove = setOf("uuid1", "uuid2")
storage.removeAll(playersToRemove)
```

### Example 5: Handling Empty Collections
```kotlin
// Both methods handle empty collections gracefully
val emptyMap = emptyMap<String, String>()
storage.storeAll(emptyMap)  // Returns true immediately

val emptySet = emptySet<String>()
storage.removeAll(emptySet)  // Returns true immediately
```

## Implementation Details

### JSON Storage
- Uses `ConcurrentHashMap.putAll()` for thread-safe batch updates
- Single `saveData()` call after all items are added
- Counts removed items and only saves if changes were made

### SQLite Storage
- Disables auto-commit and uses explicit transactions
- Batches all statements with `addBatch()` and `executeBatch()`
- Rolls back on error to maintain data integrity
- Restores original auto-commit setting

### MySQL Storage
- Same transaction approach as SQLite
- Uses `ON DUPLICATE KEY UPDATE` for upsert behavior
- Batch statements reduce network latency significantly

## Error Handling

All implementations:
1. Check if storage is initialized before proceeding
2. Log detailed error messages with the number of items
3. Return `false` on failure (database implementations rollback changes)
4. Maintain existing auto-commit settings

## Migration Guide

To migrate from individual operations to batch operations:

```kotlin
// Old pattern
fun saveMultiplePlayers(players: Map<String, String>) {
    players.forEach { (uuid, data) ->
        if (!storage.store(uuid, data)) {
            plugin.logger.warning("Failed to store player $uuid")
        }
    }
}

// New pattern
fun saveMultiplePlayers(players: Map<String, String>) {
    if (!storage.storeAll(players)) {
        plugin.logger.warning("Failed to store ${players.size} players")
    } else {
        plugin.logger.info("Successfully stored ${players.size} players")
    }
}
```

## Best Practices

1. **Use batch operations when processing multiple items** - Even for just 2-3 items, batch operations are more efficient
2. **Consider memory constraints** - For very large datasets (10,000+ items), consider batching in chunks
3. **Check return values** - Database implementations use transactions, so partial failures won't corrupt data
4. **Log appropriately** - The implementations log at FINE level for success, SEVERE for errors

## Performance Testing Recommendations

When testing these operations:
1. Measure time for 100, 1000, and 10000 items
2. Compare individual operations vs batch operations
3. Test with different storage backends
4. Monitor disk I/O and network traffic
5. Test rollback behavior by simulating errors

## Future Enhancements

Potential improvements for the future:
- Add `storeAllAsync()` and `removeAllAsync()` for non-blocking operations
- Add progress callbacks for large batch operations
- Implement automatic chunking for extremely large datasets
- Add batch `retrieve()` operation
