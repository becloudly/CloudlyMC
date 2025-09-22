package de.cloudly.storage.core

/**
 * Custom exceptions for the storage system.
 */

/**
 * Base exception class for all storage-related errors.
 */
open class StorageException(message: String, cause: Throwable? = null) : Exception(message, cause)

/**
 * Exception thrown when storage initialization fails.
 */
class StorageInitializationException(message: String, cause: Throwable? = null) : StorageException(message, cause)

/**
 * Exception thrown when a storage operation fails.
 */
class StorageOperationException(message: String, cause: Throwable? = null) : StorageException(message, cause)

/**
 * Exception thrown when serialization or deserialization fails.
 */
class SerializationException(message: String, cause: Throwable? = null) : StorageException(message, cause)

/**
 * Exception thrown when a required configuration is missing or invalid.
 */
class StorageConfigurationException(message: String, cause: Throwable? = null) : StorageException(message, cause)

/**
 * Exception thrown when attempting to access storage that hasn't been initialized.
 */
class StorageNotInitializedException(message: String = "Storage has not been initialized") : StorageException(message)

/**
 * Exception thrown when a storage connection fails.
 */
class StorageConnectionException(message: String, cause: Throwable? = null) : StorageException(message, cause)
