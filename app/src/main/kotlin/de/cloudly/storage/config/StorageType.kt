package de.cloudly.storage.config

/**
 * Enum representing the available storage types.
 */
enum class StorageType {
    JSON,
    SQLITE,
    MYSQL;
    
    companion object {
        /**
         * Convert a string to a StorageType.
         * @param type The string representation of the storage type
         * @return The corresponding StorageType, or JSON if the string is invalid
         */
        fun fromString(type: String?): StorageType {
            return when (type?.lowercase()) {
                "sqlite" -> SQLITE
                "mysql" -> MYSQL
                else -> JSON
            }
        }
    }
}
