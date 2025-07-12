package io.github.seerainer.chess.ai;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.PieceType;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;

import io.github.seerainer.chess.ai.search.OptimizedQuiescenceSearch;

/**
 * Core search algorithms for chess AI including minimax, PVS, and quiescence
 * search
 */
public class SearchAlgorithms {
	private static final int QUIESCENCE_MAX_DEPTH = 8;
	private static final int NULL_MOVE_REDUCTION = 3;
	private static final int NULL_MOVE_MIN_DEPTH = 3;
	private static final int REVERSE_FUTILITY_MARGIN = 1200; // Increased from 600
	private static final int RAZORING_MARGIN = 800; // Increased from 400
	private static final int MAX_REVERSE_FUTILITY_DEPTH = 3; // Reduced from 8
	private static final int MAX_RAZORING_DEPTH = 2; // Reduced from 4
	private static final int MAX_PROBCUT_DEPTH = 3; // Reduced from 5

	// Node type constants
	private static final int NODE_EXACT = 0;
	private static final int NODE_LOWER_BOUND = 1;
	private static final int NODE_UPPER_BOUND = 2;

	private static boolean hasNonPawnMaterial(final Board board, final Side side) {
		for (final var square : Square.values()) {
			if (square != Square.NONE) {
				final var piece = board.getPiece(square);
				if (piece != Piece.NONE && piece.getPieceSide() == side
						&& piece.getPieceType() != PieceType.PAWN
						&& piece.getPieceType() != PieceType.KING) {
					return true;
				}
			}
		}
		return false;
	}

	private static boolean isTerminalNode(final Board board, final int depth) {
		return depth == 0 || board.isMated() || board.isDraw();
	}

	private static boolean shouldAttemptNullMovePruning(final boolean nullMoveAllowed, final int depth,
			final Board board) {
		return nullMoveAllowed && depth >= NULL_MOVE_MIN_DEPTH && !board.isKingAttacked()
				&& hasNonPawnMaterial(board, board.getSideToMove());
	}

	// Simple placeholder implementations for missing methods
	private static boolean tryProbCut() {
		// Simplified ProbCut - would need full implementation
		return false;
	}

	private final TranspositionTable transpositionTable;

	private final SearchStatistics statistics;

	private final TimeManager timeManager;

	private Side aiSide;

	private boolean timeUp;

	public SearchAlgorithms(final TranspositionTable transpositionTable, final SearchStatistics statistics,
			final TimeManager timeManager) {
		this.transpositionTable = transpositionTable;
		this.statistics = statistics;
		this.timeManager = timeManager;
	}

	private int attemptNullMovePruning(final Board board, final int depth, final int beta, final boolean maximizing) {
		board.doNullMove();
		final var nullScore = -minimax(board, depth - NULL_MOVE_REDUCTION, -beta, -beta + 1, !maximizing, false);
		board.undoMove();
		return nullScore;
	}

	// Helper methods
	private boolean isTimeUp() {
		if (timeUp) {
			return true;
		}
		if (!timeManager.shouldStop()) {
			return false;
		}
		timeUp = true;
		return true;
	}

	// Additional helper methods would be implemented here...
	// (Continuing with more helper methods to keep the class focused)

	/**
	 * Main minimax search with alpha-beta pruning and enhancements
	 */
	public int minimax(final Board board, final int depth, final int alpha, final int beta, final boolean maximizing,
			final boolean nullMoveAllowed) {
		if (isTimeUp()) {
			return PositionEvaluator.evaluateBoard(board, aiSide);
		}

		final var zobristKey = ZobristHashing.calculateZobristHash(board);

		// Transposition table lookup
		final var ttEntry = probeTranspositionTable(zobristKey, depth, alpha, beta);
		if (ttEntry != null) {
			return ttEntry.score;
		}

		// Terminal node check
		if (isTerminalNode(board, depth)) {
			final var eval = depth == 0 ? quiescenceSearch(board, alpha, beta, QUIESCENCE_MAX_DEPTH)
					: PositionEvaluator.evaluateBoard(board, aiSide);
			transpositionTable.store(zobristKey, eval, depth, null, NODE_EXACT);
			return eval;
		}

		// Static evaluation for futility pruning
		final var staticEval = PositionEvaluator.evaluateBoard(board, aiSide);
		final var adjustedEval = maximizing ? staticEval : -staticEval;

		// Reverse Futility Pruning
		if (depth <= MAX_REVERSE_FUTILITY_DEPTH && !board.isKingAttacked()
				&& adjustedEval >= beta + REVERSE_FUTILITY_MARGIN) {
			statistics.incrementReverseFutilityPrunes();
			return beta;
		}

		// Razoring
		if (depth <= MAX_RAZORING_DEPTH && !board.isKingAttacked() && adjustedEval + RAZORING_MARGIN < alpha) {
			final var razorScore = quiescenceSearch(board, alpha, beta, QUIESCENCE_MAX_DEPTH);
			if (razorScore < alpha) {
				statistics.incrementRazoringPrunes();
				return razorScore;
			}
		}

		final var condition = depth >= 4 && depth <= MAX_PROBCUT_DEPTH && !board.isKingAttacked()
				&& Math.abs(beta) < 25000 && tryProbCut();
		// ProbCut
		if (condition) {
			statistics.incrementProbcutPrunes();
			return beta;
		}

		// Null move pruning
		if (shouldAttemptNullMovePruning(nullMoveAllowed, depth, board)) {
			final var nullScore = attemptNullMovePruning(board, depth, beta, maximizing);
			if (nullScore >= beta) {
				return beta;
			}
		}

		return searchAllMoves(board, depth, alpha, beta, maximizing);
	}

	private TranspositionTable.TTEntry probeTranspositionTable(final long zobristKey, final int depth, final int alpha,
			final int beta) {
		final var ttEntry = transpositionTable.get(zobristKey);
		if (ttEntry == null || ttEntry.depth < depth) {
			return null;
		}

		switch (ttEntry.nodeType) {
		case NODE_EXACT -> {
			return ttEntry;
		}
		case NODE_LOWER_BOUND -> {
			if (ttEntry.score >= beta) {
				return ttEntry;
			}
		}
		case NODE_UPPER_BOUND -> {
			if (ttEntry.score <= alpha) {
				return ttEntry;
			}
		}
		default -> throw new IllegalArgumentException("Unexpected value: " + ttEntry.nodeType);
		}
		return null;
	}

	/**
	 * Principal Variation Search implementation
	 */
	public int pvSearch(final Board board, final int depth, final int alpha, final int beta, final boolean isPVNode,
			final boolean nullMoveAllowed) {
		if (isTimeUp()) {
			return PositionEvaluator.evaluateBoard(board, aiSide);
		}

		final var zobristKey = ZobristHashing.calculateZobristHash(board);

		// Transposition table lookup
		final var ttEntry = probeTranspositionTable(zobristKey, depth, alpha, beta);
		if (ttEntry != null) {
			return ttEntry.score;
		}

		// Terminal node check
		if (isTerminalNode(board, depth)) {
			final var eval = depth == 0 ? quiescenceSearch(board, alpha, beta, QUIESCENCE_MAX_DEPTH)
					: PositionEvaluator.evaluateBoard(board, aiSide);
			transpositionTable.store(zobristKey, eval, depth, null, NODE_EXACT);
			return eval;
		}

		PositionEvaluator.evaluateBoard(board, aiSide);

		// Null move pruning (only in non-PV nodes)
		if (!isPVNode && shouldAttemptNullMovePruning(nullMoveAllowed, depth, board)) {
			board.doNullMove();
			final var nullScore = -pvSearch(board, depth - NULL_MOVE_REDUCTION, -beta, -beta + 1, false, false);
			board.undoMove();

			if (nullScore >= beta) {
				return beta;
			}
		}

		return searchAllMovesWithPVS(board, depth, alpha, beta, isPVNode);
	}

	/**
	 * Quiescence search for tactical positions - now using
	 * OptimizedQuiescenceSearch
	 */
	public int quiescenceSearch(final Board board, final int alpha, final int beta, final int depth) {
		statistics.incrementQNodes();

		// Time check
		if (isTimeUp()) {
			return PositionEvaluator.evaluateBoard(board, aiSide);
		}

		// Use the optimized quiescence search with checks for better tactical
		// evaluation
		return OptimizedQuiescenceSearch.quiescenceSearchWithChecks(board, alpha, beta, depth);
	}

	private int searchAllMoves(final Board board, final int depth, final int alpha, final int beta,
			final boolean maximizing) {
		// Get all legal moves for the current position
		final var legalMoves = board.legalMoves();

		// If no legal moves, it's checkmate or stalemate
		if (legalMoves.isEmpty()) {
			if (board.isKingAttacked()) {
				// Checkmate - return a very low score (losing position)
				// Adjust score based on depth to prefer later checkmates over earlier ones
				return maximizing ? -20000 + depth : 20000 - depth;
			}
			// Stalemate - return 0 (draw)
			return 0;
		}

		// Initialize best score based on whether we're maximizing or minimizing
		var bestScore = maximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;
		var currentAlpha = alpha;
		var currentBeta = beta;

		// Try each legal move
		for (final var move : legalMoves) {
			// Make the move
			board.doMove(move);

			// Recursively search the resulting position
			final var score = minimax(board, depth - 1, currentAlpha, currentBeta, !maximizing, // Flip maximizing for
																								// the opponent
					true // Allow null move pruning
			);

			// Undo the move
			board.undoMove();

			// Update best score and alpha/beta values
			if (maximizing) {
				if (score > bestScore) {
					bestScore = score;
				}
				currentAlpha = Math.max(currentAlpha, bestScore);
			} else {
				if (score < bestScore) {
					bestScore = score;
				}
				currentBeta = Math.min(currentBeta, bestScore);
			}
			// Alpha-beta pruning
			if (currentBeta <= currentAlpha) {
				break; // Beta cutoff
			}
		}

		return bestScore;
	}

	private int searchAllMovesWithPVS(final Board board, final int depth, final int alpha, final int beta,
			final boolean isPVNode) {
		// Get all legal moves for the current position
		final var legalMoves = board.legalMoves();

		// If no legal moves, it's checkmate or stalemate
		if (legalMoves.isEmpty()) {
			return board.isKingAttacked() ? -20000 + depth : 0;
		}

		// Initialize search values
		var bestScore = Integer.MIN_VALUE;
		var currentAlpha = alpha;
		var firstMove = true;

		// Try each legal move
		for (final var move : legalMoves) {
			// Make the move
			board.doMove(move);

			var score = 0;

			if (firstMove) {
				// Search first move with full window
				score = -pvSearch(board, depth - 1, -beta, -currentAlpha, isPVNode, // Maintain PV node status
						true // Allow null move pruning
				);
				firstMove = false;
			} else {
				// Search subsequent moves with null window first
				score = -pvSearch(board, depth - 1, -currentAlpha - 1, -currentAlpha, false, // Not a PV node for null
																								// window search
						true // Allow null move pruning
				);

				// If it beats alpha, re-search with full window
				if (score > currentAlpha && score < beta) {
					score = -pvSearch(board, depth - 1, -beta, -currentAlpha, true, // Now it's a PV node
							true // Allow null move pruning
					);
				}
			}

			// Undo the move
			board.undoMove();

			// Update best score and alpha
			if (score > bestScore) {
				bestScore = score;
				currentAlpha = Math.max(currentAlpha, score);
			}

			// Alpha-beta pruning
			if (currentAlpha >= beta) {
				// Beta cutoff
				break;
			}
		}

		return bestScore;
	}

	public void setAiSide(final Side aiSide) {
		this.aiSide = aiSide;
	}

	public void setTimeUp(final boolean timeUp) {
		this.timeUp = timeUp;
	}
}
