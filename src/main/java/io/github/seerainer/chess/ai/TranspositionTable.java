package io.github.seerainer.chess.ai;

import com.github.bhlangonijr.chesslib.move.Move;

/**
 * Enhanced transposition table with proper replacement strategy and memory
 * management
 */
public class TranspositionTable {
    // **CONSTANTS: Extracted magic numbers**
    private static final int DEFAULT_SIZE = 1_000_000;
    private static final int MAX_AGE = 64;
    private static final int AGE_INCREMENT = 1;
    // **NEW: Search generation tracking for better aging**
    private static final int MAX_SEARCH_GENERATION = 256;
    private final TTEntry[] table;
    private final int size;
    private int currentAge;
    private int storedEntries;
    // **NEW: Track search generations separately from age**
    private int searchGeneration;
    // **NEW: Statistics for replacement behavior**
    private long totalProbes;
    private long hitCount;
    private long replacementCount;

    public TranspositionTable() {
	this(DEFAULT_SIZE);
    }

    public TranspositionTable(final int size) {
	this.size = size;
	this.table = new TTEntry[size];
	this.currentAge = 0;
	this.storedEntries = 0;
    }

    /**
     * **ENHANCED: Clear with generation-based aging**
     */
    public void clear() {
	// **NEW: Increment search generation for better aging**
	searchGeneration = (searchGeneration + 1) % MAX_SEARCH_GENERATION;

	// Traditional age increment
	currentAge = Math.min(currentAge + AGE_INCREMENT, MAX_AGE);

	// Only do full clear if age has wrapped around or generation reset
	if (((currentAge < MAX_AGE) && (searchGeneration != 0))) {
	    return;
	}
	for (var i = 0; i < size; i++) {
	    table[i] = null;
	}
	currentAge = 0;
	storedEntries = 0;
	searchGeneration = 0;
    }

    /**
     * **IMPROVED: Probe with LRU tracking and statistics**
     */
    public TTEntry get(final long zobristKey) {
	totalProbes++;
	final var index = getIndex(zobristKey);
	final var entry = table[index];

	// Verify zobrist key matches to avoid hash collisions
	if (((entry == null) || (entry.zobristKey != zobristKey))) {
	    return null;
	}
	hitCount++;
	entry.markAccessed(currentAge); // **NEW: Update LRU information
	return entry;
    }

    /**
     * **NEW: Get hit rate for performance monitoring**
     */
    public double getHitRate() {
	return totalProbes > 0 ? (double) hitCount / totalProbes : 0.0;
    }

    /**
     * **IMPROVED: Hash function with better distribution**
     */
    private int getIndex(final long zobristKey) {
	// Use upper and lower 32 bits for better distribution
	final var hash = (int) (zobristKey ^ (zobristKey >>> 32));
	return Math.abs(hash) % size;
    }

    /**
     * **NEW: Get load factor for performance monitoring**
     */
    public double getLoadFactor() {
	return (double) storedEntries / size;
    }

    /**
     * **NEW: Get replacement statistics**
     */
    public String getReplacementStats() {
	return "Probes: %d, Hits: %d (%.1f%%), Replacements: %d, Load: %.1f%%".formatted(totalProbes, hitCount,
		getHitRate() * 100, replacementCount, getLoadFactor() * 100);
    }

    /**
     * **NEW: Start new search - increment generation for better cache management**
     */
    public void newSearch() {
	searchGeneration = (searchGeneration + 1) % MAX_SEARCH_GENERATION;
	currentAge++;

	// **NEW: Periodic cleanup of very old entries**
	if (searchGeneration % 10 == 0) {
	    performPeriodicCleanup();
	}
    }

    /**
     * **NEW: Periodic cleanup of very old entries**
     */
    private void performPeriodicCleanup() {
	var cleanedEntries = 0;
	final var cleanupThreshold = searchGeneration - 20; // Clean entries older than 20 generations

	for (var i = 0; i < size && cleanedEntries < size / 10; i++) { // Limit cleanup to 10% of table
	    final var entry = table[i];
	    if (entry != null && entry.searchGeneration < cleanupThreshold) {
		table[i] = null;
		storedEntries--;
		cleanedEntries++;
	    }
	}
    }

    /**
     * **ENHANCED: Check if replacement should occur with sophisticated logic**
     */
    private boolean shouldReplace(final TTEntry existing, final int newDepth, final int newNodeType) {
	// Always replace if slot is empty
	// Always replace if new search is significantly deeper
	if ((existing == null) || (newDepth > existing.depth + 2)) {
	    return true;
	}

	// Always keep if existing is much deeper
	if (existing.depth > newDepth + 4) {
	    return false;
	}

	// **NEW: Enhanced replacement decision based on multiple factors**
	final var existingPriority = existing.getReplacementPriority(currentAge, searchGeneration);

	// **NEW: Dynamic threshold based on table load factor**
	var threshold = 30; // Base threshold
	final var loadFactor = getLoadFactor();
	if (loadFactor > 0.8) {
	    threshold -= 10; // More aggressive replacement when table is full
	} else if (loadFactor < 0.5) {
	    threshold += 10; // More conservative when table has space
	}

	// **NEW: Bonus for exact nodes and entries with moves**
	if (newNodeType == 0) { // Exact node
	    threshold -= 5;
	}
	if (existing.bestMove != null && newNodeType != 0) {
	    threshold += 8; // Harder to replace entries with moves
	}

	// **NEW: Generation-based replacement preference**
	if (existing.searchGeneration < searchGeneration - 5) {
	    threshold -= 15; // Easier to replace very old entries
	}

	return existingPriority > threshold;
    }

    /**
     * **NEW: Get table size for monitoring**
     */
    public int size() {
	return storedEntries;
    }

    /**
     * **ENHANCED: Store with comprehensive replacement strategy and statistics**
     */
    public void store(final long zobristKey, final int score, final int depth, final Move bestMove,
	    final int nodeType) {
	final var index = getIndex(zobristKey);
	final var existing = table[index];

	// Check if we should replace the existing entry
	if (!shouldReplace(existing, depth, nodeType)) {
	    return;
	}
	// Count replacements for statistics
	if (existing != null) {
	    replacementCount++;
	} else {
	    storedEntries++;
	}
	// **NEW: Use current search generation for new entries**
	table[index] = new TTEntry(zobristKey, score, depth, bestMove, nodeType, currentAge, searchGeneration);
    }

    /**
     * **NEW: Statistics data class**
     */
    public static class TableStats {
	public final int totalEntries;
	public final int exactNodes;
	public final int lowerBoundNodes;
	public final int upperBoundNodes;
	public final double averageDepth;
	public final int entriesWithMoves;
	public final double loadFactor;

	public TableStats(final int totalEntries, final int exactNodes, final int lowerBoundNodes,
		final int upperBoundNodes, final double averageDepth, final int entriesWithMoves,
		final double loadFactor) {
	    this.totalEntries = totalEntries;
	    this.exactNodes = exactNodes;
	    this.lowerBoundNodes = lowerBoundNodes;
	    this.upperBoundNodes = upperBoundNodes;
	    this.averageDepth = averageDepth;
	    this.entriesWithMoves = entriesWithMoves;
	    this.loadFactor = loadFactor;
	}

	@Override
	public String toString() {
	    return "TT Stats: %d entries (%.1f%% full), %.1f avg depth, %d exact, %d lower, %d upper, %d with moves"
		    .formatted(totalEntries, loadFactor * 100, averageDepth, exactNodes, lowerBoundNodes,
			    upperBoundNodes, entriesWithMoves);
	}
    }

    // Transposition table entry class with enhanced data
    public static class TTEntry {
	public final long zobristKey;
	public final int score;
	public final int depth;
	public final Move bestMove;
	public final int nodeType; // 0 = exact, 1 = lower bound, 2 = upper bound
	public final int age;
	// **NEW: Search generation for more precise aging**
	public final int searchGeneration;
	// **NEW: Access count for frequency-based replacement**
	private int accessCount;
	// **NEW: Last access age for LRU-style replacement**
	private int lastAccessAge;

	public TTEntry(final long zobristKey, final int score, final int depth, final Move bestMove, final int nodeType,
		final int age, final int searchGeneration) {
	    this.zobristKey = zobristKey;
	    this.score = score;
	    this.depth = depth;
	    this.bestMove = bestMove;
	    this.nodeType = nodeType;
	    this.age = age;
	    this.searchGeneration = searchGeneration;
	    this.accessCount = 1;
	    this.lastAccessAge = age;
	}

	/**
	 * **ENHANCED: More sophisticated replacement priority calculation** Considers
	 * multiple factors for optimal cache behavior
	 */
	public int getReplacementPriority(final int currentAge, final int currentGeneration) {
	    var priority = 0;

	    // **NEW: Search generation factor (most important)**
	    final var generationDiff = currentGeneration - this.searchGeneration;
	    if (generationDiff > 0) {
		priority += generationDiff * 50; // High penalty for old generations
	    }

	    // Age factor within current generation
	    final var ageDiff = Math.min(currentAge - this.age, MAX_AGE);
	    priority += ageDiff * 3;

	    // **NEW: Depth factor with exponential scaling**
	    final var depthPenalty = Math.max(0, 25 - this.depth);
	    priority += depthPenalty * depthPenalty / 10; // Quadratic penalty for shallow searches

	    // **NEW: Node type factor with refined weights**
	    switch (this.nodeType) {
	    case 0 -> priority += 0; // Exact nodes are most valuable
	    case 1 -> priority += 8; // Lower bound nodes
	    case 2 -> priority += 12; // Upper bound nodes least valuable
	    default -> priority += 15; // Invalid node types
	    }

	    // **NEW: Access frequency factor (LFU)**
	    if (this.accessCount <= 1) {
		priority += 20; // Rarely accessed entries
	    } else if (this.accessCount <= 3) {
		priority += 10; // Moderately accessed
	    }
	    // Frequently accessed entries get no penalty

	    // **NEW: Last access age factor (LRU)**
	    final var lastAccessDiff = currentAge - this.lastAccessAge;
	    priority += Math.min(lastAccessDiff * 2, 30); // Cap at 30

	    // **NEW: Special bonuses for valuable entries**
	    if (this.bestMove != null) {
		priority -= 15; // Entries with moves are more valuable
	    }
	    if (this.depth >= 10) {
		priority -= 10; // Deep searches are more valuable
	    }

	    return Math.max(priority, 0); // Ensure non-negative
	}

	/**
	 * **NEW: Mark this entry as accessed for LRU tracking**
	 */
	public void markAccessed(final int currentAge) {
	    this.accessCount = Math.min(this.accessCount + 1, 255); // Cap to prevent overflow
	    this.lastAccessAge = currentAge;
	}
    }
}