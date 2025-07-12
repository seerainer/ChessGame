package io.github.seerainer.chess.ai;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;

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

	// Performance monitoring - now using configuration values
	private static long evaluationCount = 0;
	private static long cacheHits = 0;
	private static final boolean ENABLE_PERFORMANCE_MONITORING = ChessConfig.Debug.ENABLE_PERFORMANCE_MONITORING;

	/**
	 * Legacy positional value calculation for backward compatibility
	 */
	private static int calculateLegacyPositionalValue(final Piece piece, final Square square, final boolean isEndGame) {
		// Simple center bonus system for backward compatibility
		final var file = square.getFile().ordinal();
		final var rank = square.getRank().ordinal();
		final var isWhite = piece.getPieceSide() == Side.WHITE;

		var bonus = 0;

		// Center bonus
		if (file >= 2 && file <= 5 && rank >= 2 && rank <= 5) {
			bonus = switch (piece.getPieceType()) {
			case PAWN -> 10;
			case KNIGHT -> 20;
			case BISHOP -> 15;
			case ROOK -> 5;
			case QUEEN -> 10;
			case KING -> isEndGame ? 20 : -10;
			default -> 0;
			};
		}

		return isWhite ? bonus : -bonus;
	}

	/**
	 * Clear cache for memory management
	 */
	public static void clearCache() {
		if (evaluationCache != null) {
			evaluationCache.clear();
		}
		// Reset performance counters
		evaluationCount = 0;
		cacheHits = 0;
	}

	/**
	 * Main evaluation method with performance monitoring
	 */
	public static int evaluateBoard(final Board board, final Side sideToMove) {
		if (ENABLE_PERFORMANCE_MONITORING) {
			evaluationCount++;

			// Check if we should log performance stats (using statistics interval)
			if (evaluationCount % ChessConfig.Debug.STATISTICS_REPORT_INTERVAL == 0) {
				logPerformanceStats();
			}
		}

		// Try cache first if enabled
		if (evaluationCache != null) {
			final var cachedValue = evaluationCache.get(board.getIncrementalHashKey());
			if (cachedValue != null) {
				if (ENABLE_PERFORMANCE_MONITORING) {
					cacheHits++;
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

		final var hitRate = evaluationCount > 0 ? (double) cacheHits / evaluationCount * 100 : 0;
		return "Evaluation Cache: %.1f%% hit rate (%d/%d), size: %d".formatted(hitRate, cacheHits, evaluationCount,
				evaluationCache.size());
	}

	/**
	 * Get detailed evaluation breakdown for debugging
	 */
	public static String getEvaluationBreakdown(final Board board, final Side aiSide) {
		return ORCHESTRATOR.get().getEvaluationBreakdown(board, aiSide);
	}

	// Legacy compatibility methods - delegating to appropriate evaluators

	/**
	 * @deprecated Use new component-based evaluation system
	 */
	@Deprecated
	public static int getPositionalValue(final Piece piece, final Square square, final boolean isEndGame) {
		// For backward compatibility, use a simple positional evaluation
		return calculateLegacyPositionalValue(piece, square, isEndGame);
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
		final var hitRate = evaluationCount > 0 ? (double) cacheHits / evaluationCount * 100 : 0;
		System.out.println("Evaluation Performance: " + evaluationCount + " evaluations, " + "%.1f".formatted(hitRate)
				+ "% cache hit rate");
	}

	// Private constructor to prevent instantiation
	private PositionEvaluator() {
		throw new AssertionError("Utility class should not be instantiated");
	}
}
