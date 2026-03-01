package io.github.seerainer.chess.ai;

import java.util.concurrent.atomic.AtomicLong;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Side;

import io.github.seerainer.chess.ai.cache.EvaluationCache;
import io.github.seerainer.chess.ai.evaluation.EvaluationOrchestrator;
import io.github.seerainer.chess.ai.evaluation.MaterialEvaluator;
import io.github.seerainer.chess.config.ChessConfig;

/**
 * Refactored Position Evaluator following SOLID principles
 */
public class PositionEvaluator {
    // Thread-safe singleton orchestrator
    private static final ThreadLocal<EvaluationOrchestrator> ORCHESTRATOR = ThreadLocal
	    .withInitial(EvaluationOrchestrator::new);

    // Evaluation cache for performance - now using configuration values
    private static final EvaluationCache evaluationCache = ChessConfig.Performance.ENABLE_EVALUATION_CACHE
	    ? new EvaluationCache(ChessConfig.Performance.EVALUATION_CACHE_SIZE)
	    : null;

    // Performance monitoring - now using configuration values (atomic for
    // thread-safety in parallel search)
    private static final AtomicLong evaluationCount = new AtomicLong(0);
    private static final AtomicLong cacheHits = new AtomicLong(0);
    private static final boolean ENABLE_PERFORMANCE_MONITORING = ChessConfig.Debug.ENABLE_PERFORMANCE_MONITORING;

    // Private constructor to prevent instantiation
    private PositionEvaluator() {
	throw new AssertionError("Utility class should not be instantiated");
    }

    /**
     * Clear cache for memory management
     */
    public static void clearCache() {
	if (evaluationCache != null) {
	    evaluationCache.clear();
	}
	// Reset performance counters
	evaluationCount.set(0);
	cacheHits.set(0);
    }

    /**
     * Main evaluation method with performance monitoring
     */
    public static int evaluateBoard(final Board board, final Side sideToMove) {
	if (ENABLE_PERFORMANCE_MONITORING) {
	    final var count = evaluationCount.incrementAndGet();

	    // Check if we should log performance stats (using statistics interval)
	    if (count % ChessConfig.Debug.STATISTICS_REPORT_INTERVAL == 0) {
		logPerformanceStats();
	    }
	}

	// Try cache first if enabled
	if (evaluationCache != null) {
	    final var cachedValue = evaluationCache.get(board.getIncrementalHashKey());
	    if (cachedValue != null) {
		if (ENABLE_PERFORMANCE_MONITORING) {
		    cacheHits.incrementAndGet();
		}
		return cachedValue;
	    }
	}

	// Use the orchestrator for evaluation
	final var result = ORCHESTRATOR.get().evaluatePosition(board, sideToMove);

	// Cache the result if caching is enabled
	if (evaluationCache != null) {
	    evaluationCache.put(board.getIncrementalHashKey(), result);
	}

	return result;
    }

    /**
     * Get cache statistics
     */
    public static String getCacheStatistics() {
	if (evaluationCache == null) {
	    return "Evaluation cache disabled";
	}

	final var total = evaluationCount.get();
	final var hits = cacheHits.get();
	final var hitRate = total > 0 ? (double) hits / total * 100 : 0;
	return "Evaluation Cache: %.1f%% hit rate (%d/%d), size: %d".formatted(hitRate, hits, total,
		evaluationCache.size());
    }

    /**
     * Quick critical position detection
     */
    public static boolean isCriticalPosition(final Board board) {
	return board.isKingAttacked() || board.legalMoves().size() < 10;
    }

    /**
     * Quick endgame detection for backward compatibility
     */
    public static boolean isEndGame(final Board board) {
	return MaterialEvaluator.isEndgame(board);
    }

    /**
     * Quick opening phase detection
     */
    public static boolean isOpeningPhase(final Board board) {
	return board.getMoveCounter().intValue() < 16;
    }

    /**
     * Log performance statistics
     */
    private static void logPerformanceStats() {
	if (!ENABLE_PERFORMANCE_MONITORING) {
	    return;
	}
	final var total = evaluationCount.get();
	final var hits = cacheHits.get();
	final var hitRate = total > 0 ? (double) hits / total * 100 : 0;
	System.out.println(new StringBuilder().append("Evaluation Performance: ").append(total).append(" evaluations, ")
		.append("%.1f".formatted(hitRate)).append("% cache hit rate").toString());
    }

    // Legacy compatibility methods - delegating to appropriate evaluators

}
