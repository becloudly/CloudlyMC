package de.cloudly.queue.model

/**
 * Priority levels for the connection queue system.
 * Higher priority values mean the player will be processed first.
 */
enum class QueuePriority(val priority: Int) {
    /**
     * Operators have the highest priority and bypass the queue entirely.
     */
    OPERATOR(100),
    
    /**
     * Whitelisted players have second priority.
     */
    WHITELISTED(50),
    
    /**
     * First-time joiners (never played before) have third priority.
     */
    FIRST_JOIN(25),
    
    /**
     * Regular players have the lowest priority.
     */
    REGULAR(0);
    
    companion object {
        /**
         * Compare two priorities and return which one should go first.
         * @return negative if p1 has higher priority, positive if p2 has higher priority, 0 if equal
         */
        fun compare(p1: QueuePriority, p2: QueuePriority): Int {
            return p2.priority - p1.priority
        }
    }
}
