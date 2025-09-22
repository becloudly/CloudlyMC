package de.cloudly.storage.core

/**
 * Interface for data that can be serialized and deserialized for storage.
 * All data types that need to be stored using the generic storage system
 * should implement this interface.
 */
interface Serializable {
    
    /**
     * Serialize the object to a string representation.
     * This method should convert the object to a format that can be stored
     * (e.g., JSON, XML, or custom format).
     * 
     * @return The serialized string representation of the object
     */
    fun serialize(): String
    
    /**
     * Get the type identifier for this serializable object.
     * This is used to identify the type when deserializing.
     * 
     * @return A unique string identifier for this type
     */
    fun getTypeId(): String
    
    companion object {
        /**
         * Deserialize a string representation back to an object.
         * This method should be implemented by each data class to handle
         * its own deserialization logic.
         * 
         * @param data The serialized string data
         * @param typeId The type identifier
         * @return The deserialized object, or null if deserialization failed
         */
        fun deserialize(data: String, typeId: String): Serializable? {
            // This will be implemented by each specific data class
            throw NotImplementedError("Each data class must implement its own deserialize method")
        }
    }
}
