package io.github.seerainer.chess.ai.search;

import java.util.ArrayList;
import java.util.List;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.move.Move;

import io.github.seerainer.chess.ai.MoveOrdering;
import io.github.seerainer.chess.ai.TranspositionTable;
import io.github.seerainer.chess.ai.evaluation.EvaluationOrchestrator;

/**
 * Enhanced search optimization with improved aspiration windows, better move
 * ordering integration, and stability improvements.
 *
 * This addresses the "Search Optimization - Could benefit from better move
 * ordering" improvement request by adding: - Adaptive aspiration windows -
 * Enhanced move ordering integration - Improved search stability - Better time
 * management
 */
public class SearchOptimizer {

	private static final int ASPIRATION_WINDOW_SIZE = 50;
	private static final int MAX_ASPIRATION_ATTEMPTS = 3;
	private static final double TIME_EXTENSION_FACTOR = 1.5;

	// Node type constants (matching TranspositionTable)
	private static final int EXACT = 0;
	private static final int LOWER_BOUND = 1;
	private static final int UPPER_BOUND = 2;

	/**
	 * Generate tactical moves for quiescence search
	 */
	private static List<Move> generateTacticalMoves(final Board board) {
		final List<Move> tacticalMoves = new ArrayList<>();

		for (final var move : board.legalMoves()) {
			// Captures
			// Promotions
			if (Piece.NONE != board.getPiece(move.getTo()) || Piece.NONE != move.getPromotion()) {
				tacticalMoves.add(move);
				continue;
			}

			// Checks (limited to avoid explosion)
			if (tacticalMoves.size() < 10) {
				board.doMove(move);
				if (board.isKingAttacked()) {
					tacticalMoves.add(move);
				}
				board.undoMove();
			}
		}

		return tacticalMoves;
	}

	private final TranspositionTable transpositionTable;
	private final EvaluationOrchestrator evaluator;
	private int searchDepth;
	private long startTime;
	private long timeLimit;
	private int nodesSearched;
	private Move bestMoveFound;

	private int bestScore;

	public SearchOptimizer(final TranspositionTable transpositionTable, final EvaluationOrchestrator evaluator) {
		this.transpositionTable = transpositionTable;
		this.evaluator = evaluator;
	}

	/**
	 * Evaluate position using the orchestrator
	 */
	private int evaluatePosition(final Board board, final Side side) {
		return evaluator.evaluatePosition(board, side);
	}

	/**
	 * Enhanced iterative deepening with aspiration windows
	 */
	public Move findBestMove(final Board board, final int maxDepth, final long timeLimit) {
		this.timeLimit = timeLimit;
		this.startTime = System.currentTimeMillis();
		this.nodesSearched = 0;
		this.bestMoveFound = null;
		this.bestScore = 0;

		// Get legal moves and order them
		final var moves = board.legalMoves();
		if (moves.isEmpty()) {
			return null;
		}

		// Initial shallow search for move ordering
		final var initialBest = performShallowSearch(board, moves);
		if (initialBest != null) {
			bestMoveFound = initialBest;
		}

		// Iterative deepening with aspiration windows
		for (var depth = 1; depth <= maxDepth; depth++) {
			if (isTimeUp()) {
				break;
			}

			this.searchDepth = depth;
			final var depthBest = searchWithAspirationWindows(board, depth, moves);

			if (depthBest != null) {
				bestMoveFound = depthBest;

				// Check if we found a mate or have sufficient time
				if (Math.abs(bestScore) > 9000 || shouldStopSearch(depth, maxDepth)) {
					break;
				}
			}
		}

		return bestMoveFound;
	}

	public int getBestScore() {
		return bestScore;
	}

	/**
	 * Get search statistics
	 */
	public int getNodesSearched() {
		return nodesSearched;
	}

	/**
	 * Check if time is up
	 */
	private boolean isTimeUp() {
		return System.currentTimeMillis() - startTime >= timeLimit;
	}

	/**
	 * Perform shallow search for initial move ordering
	 */
	private Move performShallowSearch(final Board board, final List<Move> moves) {
		var bestScore = Integer.MIN_VALUE;
		Move bestMove = null;

		for (final var move : moves) {
			if (isTimeUp()) {
				break;
			}

			board.doMove(move);
			final var score = -evaluatePosition(board, board.getSideToMove().flip());
			board.undoMove();

			if (score > bestScore) {
				bestScore = score;
				bestMove = move;
			}
		}

		return bestMove;
	}

	/**
	 * Enhanced quiescence search
	 */
	private int quiescenceSearch(final Board board, int alpha, final int beta, final int depth) {
		nodesSearched++;

		if (isTimeUp() || depth > 8) {
			return evaluatePosition(board, board.getSideToMove());
		}

		final var standPat = evaluatePosition(board, board.getSideToMove());

		if (standPat >= beta) {
			return standPat;
		}

		if (standPat > alpha) {
			alpha = standPat;
		}

		// Generate tactical moves (captures, promotions, checks)
		final var tacticalMoves = generateTacticalMoves(board);

		// Order tactical moves
		tacticalMoves.sort((m1, m2) -> Integer.compare(MoveOrdering.scoreTacticalMove(board, m2),
				MoveOrdering.scoreTacticalMove(board, m1)));

		for (final var move : tacticalMoves) {
			if (isTimeUp()) {
				break;
			}

			board.doMove(move);
			final var score = -quiescenceSearch(board, -beta, -alpha, depth + 1);
			board.undoMove();

			if (score >= beta) {
				return score;
			}

			if (score > alpha) {
				alpha = score;
			}
		}

		return alpha;
	}

	/**
	 * Main search with enhanced pruning
	 */
	private int search(final Board board, final int depth, int alpha, int beta, final boolean allowNullMove) {
		nodesSearched++;

		if (isTimeUp()) {
			return evaluatePosition(board, board.getSideToMove());
		}

		if (depth <= 0) {
			return quiescenceSearch(board, alpha, beta, 0);
		}

		// Check for draw by repetition or 50-move rule
		if (board.isDraw()) {
			return 0;
		}

		// Transposition table lookup
		final var ttEntry = transpositionTable.get(board.getZobristKey());
		if (ttEntry != null && ttEntry.depth >= depth) {
			final var ttScore = ttEntry.score;

			switch (ttEntry.nodeType) {
			case EXACT -> {
				return ttScore;
			}
			case LOWER_BOUND -> {
				if (ttScore >= beta) {
					return ttScore;
				}
				alpha = Math.max(alpha, ttScore);
			}
			case UPPER_BOUND -> {
				if (ttScore <= alpha) {
					return ttScore;
				}
				beta = Math.min(beta, ttScore);
			}
			default -> {
				// Unknown node type, treat as exact
				return ttScore;
			}
			}
		}

		// Null move pruning
		if (allowNullMove && depth >= 3 && !board.isKingAttacked()) {
			final var staticEval = evaluatePosition(board, board.getSideToMove());
			if (staticEval >= beta) {
				// Simulate null move by flipping side
				board.setSideToMove(board.getSideToMove().flip());
				final var nullScore = -search(board, depth - 3, -beta, -beta + 1, false);
				board.setSideToMove(board.getSideToMove().flip());

				if (nullScore >= beta) {
					return nullScore;
				}
			}
		}

		// Generate and order moves
		final var moves = board.legalMoves();
		if (moves.isEmpty()) {
			return board.isKingAttacked() ? -10000 + (searchDepth - depth) : 0;
		}

		final var orderedMoves = MoveOrdering.orderMovesAdvanced(board, moves, depth, ttEntry);

		var bestScore = Integer.MIN_VALUE;
		Move bestMove = null;
		var moveCount = 0;

		for (final var move : orderedMoves) {
			if (isTimeUp()) {
				break;
			}

			moveCount++;

			// Late Move Reduction (LMR)
			var reduction = 0;
			if (moveCount > 4 && depth > 2 && !board.isKingAttacked() && Piece.NONE == board.getPiece(move.getTo())) {
				reduction = 1;
				if (moveCount > 8) {
					reduction = 2;
				}
			}

			board.doMove(move);
			int score;

			if (moveCount == 1) {
				score = -search(board, depth - 1, -beta, -alpha, true);
			} else {
				// Search with reduction first
				score = -search(board, depth - 1 - reduction, -alpha - 1, -alpha, true);

				// If it improves alpha, re-search without reduction
				if (score > alpha && reduction > 0) {
					score = -search(board, depth - 1, -alpha - 1, -alpha, true);
				}

				// If it's still good, re-search with full window
				if (score > alpha && score < beta) {
					score = -search(board, depth - 1, -beta, -alpha, true);
				}
			}

			board.undoMove();

			if (score > bestScore) {
				bestScore = score;
				bestMove = move;

				if (score > alpha) {
					alpha = score;

					// Update move ordering
					MoveOrdering.updateHistoryTable(board.getSideToMove(), move, depth);
				}

				if (score >= beta) {
					// Update killer moves and countermoves
					MoveOrdering.updateKillerMoves(depth, move);

					// Store in transposition table
					transpositionTable.store(board.getZobristKey(), score, depth, move, LOWER_BOUND);

					return score;
				}
			}
		}

		// Store in transposition table
		final var nodeType = bestScore <= alpha ? UPPER_BOUND : EXACT;
		transpositionTable.store(board.getZobristKey(), bestScore, depth, bestMove, nodeType);

		return bestScore;
	}

	/**
	 * Root search with enhanced move ordering
	 */
	private int searchRoot(final Board board, final int depth, int alpha, final int beta, final List<Move> moves) {
		var bestScore = Integer.MIN_VALUE;
		Move bestMove = null;

		// Get transposition table entry for better move ordering
		final var ttEntry = transpositionTable.get(board.getZobristKey());

		// Enhanced move ordering with all heuristics
		final var orderedMoves = MoveOrdering.orderMovesAdvanced(board, moves, depth, ttEntry);

		var moveCount = 0;
		for (final var move : orderedMoves) {
			if (isTimeUp()) {
				break;
			}

			moveCount++;

			// Make the move
			board.doMove(move);
			int score;

			// Principal Variation Search (PVS)
			if (moveCount == 1) {
				// Search first move with full window
				score = -search(board, depth - 1, -beta, -alpha, false);
			} else {
				// Search with reduced window first
				score = -search(board, depth - 1, -alpha - 1, -alpha, false);

				// If it improves alpha, re-search with full window
				if (score > alpha && score < beta) {
					score = -search(board, depth - 1, -beta, -alpha, false);
				}
			}

			board.undoMove();

			if (score > bestScore) {
				bestScore = score;
				bestMove = move;

				if (score > alpha) {
					alpha = score;

					// Update move ordering statistics
					MoveOrdering.updateHistoryTable(board.getSideToMove(), move, depth);

					// Update transposition table
					transpositionTable.store(board.getZobristKey(), score, depth, move,
							score >= beta ? LOWER_BOUND : EXACT);
				}

				if (score >= beta) {
					// Beta cutoff - update killer moves
					MoveOrdering.updateKillerMoves(depth, move);
					break;
				}
			}
		}

		if (bestMove != null) {
			bestMoveFound = bestMove;
		}

		return bestScore;
	}

	/**
	 * Search with adaptive aspiration windows
	 */
	private Move searchWithAspirationWindows(final Board board, final int depth, final List<Move> moves) {
		var alpha = bestScore - ASPIRATION_WINDOW_SIZE;
		var beta = bestScore + ASPIRATION_WINDOW_SIZE;

		// Retry with wider windows if needed
		for (var attempt = 0; attempt < MAX_ASPIRATION_ATTEMPTS; attempt++) {
			if (isTimeUp()) {
				return bestMoveFound;
			}

			final var score = searchRoot(board, depth, alpha, beta, moves);

			if (score <= alpha) {
				// Fail-low: score is worse than expected, widen lower bound
				alpha = score - ASPIRATION_WINDOW_SIZE * (1 << attempt);
				beta = score + ASPIRATION_WINDOW_SIZE;
			} else if (score >= beta) {
				// Fail-high: score is better than expected, widen upper bound
				alpha = score - ASPIRATION_WINDOW_SIZE;
				beta = score + ASPIRATION_WINDOW_SIZE * (1 << attempt);
			} else {
				// Score within window, search successful
				bestScore = score;
				return bestMoveFound;
			}
		}

		// Final attempt with infinite window
		searchRoot(board, depth, Integer.MIN_VALUE, Integer.MAX_VALUE, moves);
		return bestMoveFound;
	}

	/**
	 * Check if we should stop searching
	 */
	private boolean shouldStopSearch(final int currentDepth, final int maxDepth) {
		final var elapsed = System.currentTimeMillis() - startTime;
		final var remainingTime = timeLimit - elapsed;

		// If we have very little time left, stop

		// If we're at max depth, stop
		// If we found a mate, stop
		if ((remainingTime < timeLimit * 0.1) || (currentDepth >= maxDepth) || (Math.abs(bestScore) > 9000)) {
			return true;
		}

		// Estimate if we have time for the next iteration
		final var avgTimePerDepth = elapsed / currentDepth;
		if (remainingTime < avgTimePerDepth * TIME_EXTENSION_FACTOR) {
			return true;
		}

		return false;
	}
}
