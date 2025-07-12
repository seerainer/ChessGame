package io.github.seerainer.chess;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.move.Move;

import io.github.seerainer.chess.ai.MoveOrdering;
import io.github.seerainer.chess.ai.OpeningBook;
import io.github.seerainer.chess.ai.PositionEvaluator;
import io.github.seerainer.chess.ai.SearchAlgorithms;
import io.github.seerainer.chess.ai.SearchStatistics;
import io.github.seerainer.chess.ai.TimeManager;
import io.github.seerainer.chess.ai.TranspositionTable;
import io.github.seerainer.chess.ai.ZobristHashing;
import io.github.seerainer.chess.ai.search.AdvancedSearchEngine;
import io.github.seerainer.chess.ai.search.ParallelSearchEngine;
import io.github.seerainer.chess.ai.search.TournamentSearchEngine;
import io.github.seerainer.chess.ai.utils.ResourceManager;
import io.github.seerainer.chess.config.ChessConfig;

/**
 * Refactored Chess AI with improved architecture using composition and
 * delegation Significantly reduced from 1252 lines to focus on coordination and
 * high-level logic
 */
public class ChessAI {
	/**
	 * Search engine type enumeration
	 */
	private enum SearchEngineType {
		STANDARD, ADVANCED, PARALLEL
	}

	/**
	 * Check if position is complex enough to warrant advanced search
	 */
	private static boolean isComplexPosition(final Board board) {
		try {
			final var legalMoves = board.legalMoves();

			// Complex if many legal moves
			// Complex if in check
			if ((legalMoves.size() > 25) || board.isKingAttacked()) {
				if (ChessConfig.Debug.ENABLE_DEBUG_LOGGING) {
					System.out.println("Complex position detected: " + legalMoves.size() + " moves, in check: "
							+ board.isKingAttacked());
				}
				return true;
			}

			// Simplified complexity check - avoid expensive move evaluation
			// Just check if there are captures available
			final var captureCount = legalMoves.stream().mapToLong(
					move -> (board.getPiece(move.getTo()) != Piece.NONE ? 1 : 0)).sum();

			return captureCount > 3;
		} catch (final Exception e) {
			System.err.println("Error checking position complexity: " + e.getMessage());
			// If we can't determine complexity, assume it's simple
			return false;
		}
	}

	/**
	 * Fallback move selection if search fails
	 */
	private static Move selectBestMoveByEvaluation(final Board board, final List<Move> legalMoves) {
		Move bestMove = null;
		var bestScore = Integer.MIN_VALUE;

		for (final var move : legalMoves) {
			// Safety check: validate move before executing
			if (move == null || move.getFrom() == null || move.getTo() == null) {
				System.err.println("Warning: Invalid move detected in selectBestMoveByEvaluation: " + move);
				continue;
			}

			// Safety check: ensure piece exists at source square
			final var piece = board.getPiece(move.getFrom());
			if (piece == null || piece == Piece.NONE) {
				System.err.println("Warning: No piece at source square " + move.getFrom() + " for move: " + move);
				continue;
			}

			try {
				board.doMove(move);
				final var score = -PositionEvaluator.evaluateBoard(board, board.getSideToMove());
				board.undoMove();

				if (score > bestScore) {
					bestScore = score;
					bestMove = move;
				}
			} catch (final Exception e) {
				System.err
						.println("Error executing move " + move + " in selectBestMoveByEvaluation: " + e.getMessage());
				// Continue with next move
			}
		}

		return bestMove;
	}

	// Core components using composition
	private final TranspositionTable transpositionTable;
	private final OpeningBook openingBook;
	private final TimeManager timeManager;
	private final SearchStatistics searchStatistics;

	private final SearchAlgorithms searchAlgorithms;
	private final ResourceManager resourceManager;

	// Advanced search engines
	private final AdvancedSearchEngine advancedSearchEngine;
	private final ParallelSearchEngine parallelSearchEngine;
	private final TournamentSearchEngine tournamentSearchEngine;

	// OptimizedQuiescenceSearch is static, so no instance needed
	private final Map<String, Move> bestMoveTable;
	// AI state
	private Side aiSide;

	private volatile boolean timeUp;

	private volatile boolean searchCancelled;

	public ChessAI() {
		this.transpositionTable = new TranspositionTable();
		this.openingBook = new OpeningBook();
		this.timeManager = new TimeManager();
		this.searchStatistics = new SearchStatistics();
		this.searchAlgorithms = new SearchAlgorithms(transpositionTable, searchStatistics, timeManager);
		this.resourceManager = new ResourceManager();
		this.bestMoveTable = new HashMap<>();

		// Initialize advanced search engines
		this.advancedSearchEngine = new AdvancedSearchEngine(searchAlgorithms);
		this.parallelSearchEngine = new ParallelSearchEngine(resourceManager.getExecutorService(), searchAlgorithms,
				transpositionTable);
		this.tournamentSearchEngine = new TournamentSearchEngine();
	}

	/**
	 * Calculate aspiration window bounds
	 */
	private int[] calculateAspirationWindow(final Board board) {
		final var zobristKey = ZobristHashing.calculateZobristHash(board);
		final var ttEntry = transpositionTable.get(zobristKey);
		final var prevScore = ttEntry != null ? ttEntry.score : 0;

		return new int[] { prevScore - ChessConfig.AI.ASPIRATION_WINDOW_SIZE,
				prevScore + ChessConfig.AI.ASPIRATION_WINDOW_SIZE };
	}

	/**
	 * Cancel any ongoing search
	 */
	public void cancelSearch() {
		searchCancelled = true;
		timeUp = true;
		searchAlgorithms.setTimeUp(true);
	}

	/**
	 * Cleanup resources when done
	 */
	public void cleanup() {
		try {
			resourceManager.close();
		} catch (final Exception e) {
			System.err.println("Error closing resource manager: " + e.getMessage());
		}
	}

	/**
	 * Clean up tables periodically
	 */
	private void cleanupTablesIfNeeded() {
		if (transpositionTable.size() <= ChessConfig.AI.TRANSPOSITION_TABLE_MAX_SIZE) {
			return;
		}
		transpositionTable.clear();
		bestMoveTable.clear();
	}

	/**
	 * Iterative deepening with better time management and search engine selection
	 */
	private Move executeIterativeDeepeningSearch(final Board board, final List<Move> legalMoves) {
		Move bestMove = null;
		var lastEngineUsed = SearchEngineType.STANDARD;

		for (var depth = 1; depth <= ChessConfig.AI.MAX_SEARCH_DEPTH && !timeUp && !searchCancelled; depth++) {
			final var result = searchAtDepth(board, depth);

			if (result != null && !timeManager.isTimeUp() && !searchCancelled) {
				bestMove = result;
				// Track which engine was used for this depth
				lastEngineUsed = selectSearchEngine(board, depth, legalMoves);
			}

			if (timeManager.shouldStop() || searchCancelled) {
				timeUp = true;
				searchAlgorithms.setTimeUp(true);
				break;
			}
		}

		// Use enhanced logging
		if (bestMove != null) {
			logEnhancedSearchStatistics(bestMove, lastEngineUsed);
		}

		return bestMove != null ? bestMove : selectBestMoveByEvaluation(board, legalMoves);
	}

	/**
	 * Find best move at specific depth using enhanced search with advanced engines
	 */
	private Move findBestMoveAtDepth(final Board board, final int depth, int alpha, final int beta,
			final List<Move> legalMoves) {
		final var zobristKey = ZobristHashing.calculateZobristHash(board);
		final var orderedMoves = MoveOrdering.orderMovesAdvanced(board, legalMoves, depth,
				transpositionTable.get(zobristKey));

		// Select appropriate search engine
		final var engineType = selectSearchEngine(board, depth, orderedMoves);

		Move bestMove = null;
		var bestScore = Integer.MIN_VALUE;

		switch (engineType) {
		case PARALLEL:
			try {
				// Use parallel search engine with error handling
				final var parallelResult = parallelSearchEngine.parallelSearch(board, depth, alpha, beta);
				if (parallelResult != null && parallelResult.move != null) {
					bestMove = parallelResult.move;
					bestScore = parallelResult.score;
				}
			} catch (final Exception e) {
				System.err.println("Parallel search failed, falling back to advanced search: " + e.getMessage());
				// Fall back to advanced search
				bestMove = advancedSearchEngine.getBestMove(board, depth, alpha, beta);
				if (bestMove != null) {
					board.doMove(bestMove);
					bestScore = -PositionEvaluator.evaluateBoard(board, board.getSideToMove());
					board.undoMove();
				}
			}
			break;

		case ADVANCED:
			try {
				// Use TOURNAMENT search engine for best play
				bestMove = tournamentSearchEngine.getBestMove(board, depth);
				if (bestMove != null) {
					// Get score by making the move and evaluating
					board.doMove(bestMove);
					bestScore = -PositionEvaluator.evaluateBoard(board, board.getSideToMove());
					board.undoMove();
				}
			} catch (final Exception e) {
				System.err.println("Tournament search failed, falling back to advanced search: " + e.getMessage());
				// Fall back to advanced search
				bestMove = advancedSearchEngine.getBestMove(board, depth, alpha, beta);
				if (bestMove != null) {
					board.doMove(bestMove);
					bestScore = -PositionEvaluator.evaluateBoard(board, board.getSideToMove());
					board.undoMove();
				}
			}
			break;

		case STANDARD:
		default:
			// Use standard minimax search - AI is maximizing player
			for (final var move : orderedMoves) {
				if (timeUp) {
					break;
				}

				// Safety check: validate move before executing
				if (move == null || move.getFrom() == null || move.getTo() == null) {
					System.err.println("Warning: Invalid move detected: " + move);
					continue;
				}

				// Additional safety check: ensure piece exists at source square
				final var piece = board.getPiece(move.getFrom());
				if (piece == null || piece == Piece.NONE) {
					System.err.println("Warning: No piece at source square: " + move);
					continue;
				}

				try {
					board.doMove(move);
					// AI is maximizing, so call minimax with maximizing=true for opponent's turn
					final var score = searchAlgorithms.minimax(board, depth - 1, alpha, beta, false, true);
					board.undoMove();

					if (score > bestScore) {
						bestScore = score;
						bestMove = move;
					}

					alpha = Math.max(alpha, score);
					if (alpha >= beta) {
						break; // Beta cutoff
					}
				} catch (final Exception e) {
					System.err.println("Error processing move " + move + ": " + e.getMessage());
					// Continue with next move
				}
			}
			break;
		}

		return bestMove;
	}

	/**
	 * Main search method with debug logging
	 */
	public Move getBestMove(final Board board) {
		if (ChessConfig.Debug.ENABLE_DEBUG_LOGGING) {
			System.out.println("Starting search for position: " + board.getFen());
		}

		final var startTime = System.currentTimeMillis();

		final List<Move> legalMoves;
		try {
			legalMoves = board.legalMoves();
		} catch (final Exception e) {
			System.err.println("Error generating legal moves at top level for position: " + board.getFen());
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace();
			return null;
		}

		if (legalMoves.isEmpty()) {
			return null;
		}

		// Check opening book first
		final var bookMove = openingBook.getBookMove(board);
		if (bookMove != null) {
			return bookMove;
		}

		initializeSearch(board);
		cleanupTablesIfNeeded();

		// Start new search generation for better aging
		transpositionTable.newSearch();
		resetStatistics();

		final var bestMove = executeIterativeDeepeningSearch(board, legalMoves);

		final var endTime = System.currentTimeMillis();
		if (ChessConfig.Debug.ENABLE_PERFORMANCE_MONITORING) {
			System.out.println("Search completed in " + (endTime - startTime) + "ms");
		}

		return bestMove != null ? bestMove : selectBestMoveByEvaluation(board, legalMoves);
	}

	/**
	 * Get best move asynchronously with proper thread management
	 */
	public CompletableFuture<Move> getBestMoveAsync(final Board board) {
		// Copy the board to avoid concurrency issues
		final var boardCopy = new Board();
		boardCopy.loadFromFen(board.getFen());

		return CompletableFuture.supplyAsync(() -> {
			try {
				return getBestMove(boardCopy);
			} catch (final Exception e) {
				System.err.println("Error in AI calculation: " + e.getMessage());
				e.printStackTrace();
				return null;
			}
		}, resourceManager.getExecutorService()).exceptionally(throwable -> {
			System.err.println("AI search failed: " + throwable.getMessage());
			return null;
		});
	}

	/**
	 * Initialize search parameters
	 */
	private void initializeSearch(final Board board) {
		aiSide = board.getSideToMove();
		timeUp = false;
		searchCancelled = false;
		timeManager.startSearch(TimeManager.calculateTimeLimit(board));
		searchAlgorithms.setAiSide(aiSide);
		searchAlgorithms.setTimeUp(false);
	}

	/**
	 * Check if search is cancelled
	 */
	public boolean isSearchCancelled() {
		return searchCancelled;
	}

	/**
	 * Enhanced search statistics logging
	 */
	private void logEnhancedSearchStatistics(final Move bestMove, final SearchEngineType engineUsed) {
		if (bestMove == null) {
			return;
		}

		System.out.println("=== Enhanced Search Statistics ===");
		System.out.println("Search engine used: " + engineUsed);
		System.out.println("Best move found: " + bestMove);

		// Standard statistics
		System.out.println(MoveOrdering.getStatistics());
		System.out.println(transpositionTable.getReplacementStats());
		System.out.println(searchStatistics.getFutilityStats());
		System.out.println(searchStatistics.getQuiescenceStats());
		System.out.println(searchStatistics.getPVSStats());

		// Advanced engine statistics
		if (engineUsed == SearchEngineType.ADVANCED) {
			System.out.println("Advanced search: " + advancedSearchEngine.getStatistics());
		}

		// Quiescence search statistics
		System.out.println(tournamentSearchEngine.getStatistics());
		System.out.println("====================================");
	}

	/**
	 * Reset all statistics for new search
	 */
	private void resetStatistics() {
		MoveOrdering.clearStatistics();
		searchStatistics.reset();
	}

	/**
	 * Search at specific depth with aspiration windows
	 */
	private Move searchAtDepth(final Board board, final int depth) {
		final var initialFen = board.getFen();

		final List<Move> legalMoves;
		try {
			legalMoves = board.legalMoves();
		} catch (final Exception e) {
			System.err.println("Error generating legal moves for position: " + board.getFen());
			System.err.println("Initial position was: " + initialFen);
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace();
			return null;
		}

		if (legalMoves.isEmpty()) {
			return null;
		}

		// Use aspiration windows for deeper searches
		if (depth < ChessConfig.AI.ASPIRATION_MIN_DEPTH) {
			final var result = findBestMoveAtDepth(board, depth, Integer.MIN_VALUE + 1, Integer.MAX_VALUE - 1,
					legalMoves);

			// Verify board state hasn't been corrupted
			if (!board.getFen().equals(initialFen)) {
				System.err.println("WARNING: Board state corrupted during search!");
				System.err.println("Initial: " + initialFen);
				System.err.println("Final: " + board.getFen());
				// Try to restore the board state
				board.loadFromFen(initialFen);
			}

			return result;
		}

		final var window = calculateAspirationWindow(board);
		final var result = findBestMoveAtDepth(board, depth, window[0], window[1], legalMoves);

		// Verify board state hasn't been corrupted
		if (!board.getFen().equals(initialFen)) {
			System.err.println("WARNING: Board state corrupted during aspiration window search!");
			System.err.println("Initial: " + initialFen);
			System.err.println("Final: " + board.getFen());
			// Try to restore the board state
			board.loadFromFen(initialFen);
		}

		// If search fails, research with full window
		final var fallbackResult = result != null ? result
				: findBestMoveAtDepth(board, depth, Integer.MIN_VALUE + 1, Integer.MAX_VALUE - 1, legalMoves);

		// Final verification
		if (!board.getFen().equals(initialFen)) {
			System.err.println("WARNING: Board state corrupted during fallback search!");
			System.err.println("Initial: " + initialFen);
			System.err.println("Final: " + board.getFen());
			// Try to restore the board state
			board.loadFromFen(initialFen);
		}

		return fallbackResult;
	}

	/**
	 * Determine which search engine to use based on position and search parameters
	 */
	private SearchEngineType selectSearchEngine(final Board board, final int depth, final List<Move> legalMoves) {
		// Re-enabled parallel search with slightly less conservative settings
		if (ChessConfig.AI.ENABLE_PARALLEL_SEARCH && depth >= 7 && // Slightly reduced depth requirement
				legalMoves.size() >= 14 && // Slightly reduced move count requirement
				ChessConfig.AI.PARALLEL_SEARCH_THREADS > 1 && !resourceManager.isClosed() && // Check if resource
																								// manager is still
																								// active
				!isComplexPosition(board) && // Avoid parallel search in complex positions
				board.getHalfMoveCounter() > 8) { // Only after opening phase
			return SearchEngineType.PARALLEL;
		}

		// Use advanced search for complex positions or deep searches - fixed bugs
		if (depth >= 5 || isComplexPosition(board)) {
			return SearchEngineType.ADVANCED;
		}

		// Use standard search for simple/shallow positions
		return SearchEngineType.STANDARD;
	}
}
