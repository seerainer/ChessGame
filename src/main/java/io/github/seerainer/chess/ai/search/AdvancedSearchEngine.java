package io.github.seerainer.chess.ai.search;

import java.util.List;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.move.Move;

import io.github.seerainer.chess.ai.PositionEvaluator;
import io.github.seerainer.chess.ai.SearchAlgorithms;
import io.github.seerainer.chess.config.ChessConfig;

/**
 * Advanced search engine with sophisticated pruning techniques
 */
public class AdvancedSearchEngine {

	/**
	 * Calculate Late Move Reduction amount
	 */
	private static int calculateLMRReduction(final int moveCount, final int depth, final boolean isPVNode,
			final Move move) {
		// Don't reduce important moves
		if (moveCount <= ChessConfig.Search.LMR_SKIP_MOVES || depth <= ChessConfig.Search.LMR_MIN_DEPTH
				|| isImportantMove(move)) {
			return 0;
		}

		// Basic LMR formula
		var reduction = 1;

		if (moveCount > ChessConfig.Search.LMR_AGGRESSIVE_THRESHOLD) {
			reduction += (moveCount - ChessConfig.Search.LMR_AGGRESSIVE_THRESHOLD) / 4;
		}

		if (depth > ChessConfig.Search.LMR_DEPTH_THRESHOLD) {
			reduction += (depth - ChessConfig.Search.LMR_DEPTH_THRESHOLD) / 3;
		}

		// Reduce less in PV nodes
		if (isPVNode) {
			reduction = Math.max(reduction - 1, 0);
		}

		return Math.min(reduction, ChessConfig.Search.LMR_MAX_REDUCTION);
	}

	/**
	 * Check if the position has non-pawn material (simplified)
	 */
	private static boolean hasNonPawnMaterial(final Board board) {
		// Simplified check - if there are pieces other than pawns and kings
		try {
			return board.legalMoves().size() > 0 && !board.isKingAttacked();
		} catch (final Exception e) {
			System.err.println("Error checking hasNonPawnMaterial for position: " + board.getFen());
			System.err.println("Error: " + e.getMessage());
			return false;
		}
	}

	/**
	 * Check if a move is important (shouldn't be reduced or pruned)
	 */
	private static boolean isImportantMove(final Move move) {
		// Captures, promotions, checks, and castling are important
		return move.getPromotion() != null || // Promotion
				move.toString().contains("x") || // Capture (simplified check)
				move.toString().contains("O"); // Castling
	}

	/**
	 * Check if late move pruning should be applied
	 */
	private static boolean shouldApplyLateMovePruning(final int moveCount, final int depth, final int alpha,
			final int beta) {
		return depth <= ChessConfig.Search.LMP_MAX_DEPTH && moveCount > ChessConfig.Search.LMP_MOVE_THRESHOLD
				&& Math.abs(alpha) < ChessConfig.Search.MATE_SCORE_THRESHOLD
				&& Math.abs(beta) < ChessConfig.Search.MATE_SCORE_THRESHOLD;
	}

	/**
	 * Null move pruning - skip a move to see if position is still good
	 */
	private static boolean shouldApplyNullMove(final Board board, final int depth, final boolean isMaximizing) {
		return depth >= ChessConfig.Search.NULL_MOVE_MIN_DEPTH && !board.isKingAttacked() && hasNonPawnMaterial(board)
				&& isMaximizing; // Only apply when we're maximizing
	}

	/**
	 * Razoring - prune nodes that are unlikely to improve alpha
	 */
	private static boolean shouldApplyRazoring(final Board board, final int depth, final boolean isMaximizing) {
		return depth <= ChessConfig.Search.RAZORING_MAX_DEPTH && !board.isKingAttacked() && isMaximizing;
	}

	/**
	 * Check if singular extension should be applied
	 */
	private static boolean shouldApplySingularExtension(final Board board, final Move move, final int depth) {
		// Simplified singular extension logic
		return depth >= ChessConfig.Search.SINGULAR_EXTENSION_MIN_DEPTH && !board.isKingAttacked()
				&& isImportantMove(move);
	}

	private final SearchAlgorithms searchAlgorithms;

	// Search statistics
	private int nodesSearched;

	private int lateMovePrunings;

	private int nullMovePrunings;

	private int singularExtensions;

	private int razorPrunings;

	public AdvancedSearchEngine(final SearchAlgorithms searchAlgorithms) {
		this.searchAlgorithms = searchAlgorithms;
	}

	/**
	 * Calculate search extensions
	 */
	private int calculateExtension(final Board board, final Move move, final int depth) {
		var extension = 0;

		// Check extension
		if (board.isKingAttacked()) {
			extension += 1;
		}

		// Singular extension
		if (shouldApplySingularExtension(board, move, depth)) {
			extension += 1;
			singularExtensions++;
		}

		// Promotion extension
		if (move.getPromotion() != null) {
			extension += 1;
		}

		return Math.min(extension, ChessConfig.Search.MAX_EXTENSION);
	}

	/**
	 * Enhanced search that integrates advanced techniques with safety limits
	 */
	public Move getBestMove(final Board board, final int depth, final int alpha, final int beta) {
		resetStatistics();

		final List<Move> moves;
		try {
			moves = board.legalMoves();
		} catch (final Exception e) {
			System.err.println("Error generating legal moves in AdvancedSearchEngine for position: " + board.getFen());
			System.err.println("Error: " + e.getMessage());
			e.printStackTrace();
			return null;
		}

		if (moves.isEmpty()) {
			return null;
		}

		Move bestMove = null;
		var bestScore = Integer.MIN_VALUE;
		var currentAlpha = alpha;

		// Safety limit for move evaluation - but don't limit in early game
		final var maxMovesToEvaluate = moves.size(); // Evaluate ALL moves for best play

		for (var i = 0; i < maxMovesToEvaluate; i++) {
			final var move = moves.get(i);

			// Safety check for excessive computation
			if (nodesSearched > 50000) {
				System.out.println("Advanced search: Node limit reached, returning best move found");
				break;
			}

			board.doMove(move);
			final var score = -search(board, Math.min(depth - 1, 6), -beta, -currentAlpha, true); // Fixed: use standard
																									// minimax pattern
			board.undoMove();

			if (score > bestScore) {
				bestScore = score;
				bestMove = move;
				currentAlpha = Math.max(currentAlpha, score);

				// Alpha-beta cutoff
				if (currentAlpha >= beta) {
					break;
				}
			}
		}

		if (ChessConfig.Debug.ENABLE_STATISTICS) {
			System.out.println("Advanced search statistics: " + getStatistics());
		}

		return bestMove;
	}

	/**
	 * Get search statistics
	 */
	public String getStatistics() {
		return "Nodes: %d, LMP: %d, NMP: %d, SE: %d, Razor: %d".formatted(nodesSearched, lateMovePrunings,
				nullMovePrunings, singularExtensions, razorPrunings);
	}

	private int performNullMove(final Board board, final int depth, final int beta, final boolean isMaximizing) {
		// Ensure we don't go too deep with null moves
		if (depth <= 1) {
			return PositionEvaluator.evaluateBoard(board, board.getSideToMove());
		}

		// Make null move (pass turn)
		board.doNullMove();

		final var reduction = ChessConfig.Search.NULL_MOVE_REDUCTION;
		final var nullDepth = Math.max(depth - 1 - reduction, 0);

		final var score = -search(board, nullDepth, -beta, -beta + 1, !isMaximizing);

		board.undoMove(); // Undo null move

		return score;
	}

	/**
	 * Principal Variation Search with Late Move Reductions
	 */
	private int performPVSearch(final Board board, final List<Move> moves, final int depth, int alpha, int beta,
			final boolean isMaximizing) {
		var bestScore = isMaximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;
		final var isPVNode = (beta - alpha > 1);
		var moveCount = 0;

		for (final var move : moves) {
			moveCount++;
			board.doMove(move);

			int score;
			final var extension = calculateExtension(board, move, depth);
			final var newDepth = depth - 1 + extension;

			if (moveCount == 1) {
				// Search first move with full window
				score = -search(board, newDepth, -beta, -alpha, !isMaximizing);
			} else {
				// Late Move Reduction (LMR)
				final var reduction = calculateLMRReduction(moveCount, depth, isPVNode, move);
				final var reducedDepth = Math.max(newDepth - reduction, 0);

				// Search with null window
				score = -search(board, reducedDepth, -alpha - 1, -alpha, !isMaximizing);

				// Re-search if necessary
				if (score > alpha && (score < beta || reduction > 0)) {
					score = -search(board, newDepth, -beta, -alpha, !isMaximizing);
				}
			}

			board.undoMove();

			if (isMaximizing) {
				if (score > bestScore) {
					bestScore = score;
					if (score > alpha) {
						alpha = score;
						if (alpha >= beta) {
							break; // Alpha-beta cutoff
						}
					}
				}
			} else if (score < bestScore) {
				bestScore = score;
				if (score < beta) {
					beta = score;
					if (alpha >= beta) {
						break; // Alpha-beta cutoff
					}
				}
			}

			// Late move pruning
			if (shouldApplyLateMovePruning(moveCount, depth, alpha, beta)) {
				lateMovePrunings++;
				break;
			}
		}

		return bestScore;
	}

	private int performRazoring(final Board board, final int depth, final int alpha, final int beta) {
		final var staticEval = PositionEvaluator.evaluateBoard(board, board.getSideToMove());
		final var razorMargin = ChessConfig.Search.RAZORING_MARGIN * depth;

		if (staticEval + razorMargin <= alpha) {
			// Perform quiescence search to verify
			return searchAlgorithms.quiescenceSearch(board, alpha, beta, 0);
		}

		return staticEval;
	}

	/**
	 * Reset search statistics
	 */
	public void resetStatistics() {
		nodesSearched = 0;
		lateMovePrunings = 0;
		nullMovePrunings = 0;
		singularExtensions = 0;
		razorPrunings = 0;
	}

	/**
	 * Advanced search with multiple pruning techniques
	 */
	public int search(final Board board, final int depth, final int alpha, final int beta, final boolean isMaximizing) {
		nodesSearched++;

		// Check for terminal conditions first
		// Prevent excessive search depth
		if ((depth <= 0) || board.isDraw() || board.isMated() || (nodesSearched > 10000)) { // Reduced from 100000
			return PositionEvaluator.evaluateBoard(board, Side.WHITE);
		}

		// Null move pruning
		if (shouldApplyNullMove(board, depth, isMaximizing)) {
			final var nullMoveScore = performNullMove(board, depth, beta, isMaximizing);
			if (nullMoveScore >= beta) {
				nullMovePrunings++;
				return beta;
			}
		}

		// Razoring
		if (shouldApplyRazoring(board, depth, isMaximizing)) {
			final var razorScore = performRazoring(board, depth, alpha, beta);
			if (razorScore <= alpha) {
				razorPrunings++;
				return alpha;
			}
		}

		final List<Move> moves;
		try {
			moves = board.legalMoves();
		} catch (final Exception e) {
			System.err.println(
					"Error generating legal moves in AdvancedSearchEngine.search for position: " + board.getFen());
			System.err.println("Error: " + e.getMessage());
			return PositionEvaluator.evaluateBoard(board, Side.WHITE);
		}

		if (!moves.isEmpty()) {
			// Principal variation search with late move reduction
			return performPVSearch(board, moves, depth, alpha, beta, isMaximizing);
		}
		// Checkmate or stalemate
		if (board.isKingAttacked()) {
			return isMaximizing ? -30000 + nodesSearched : 30000 - nodesSearched;
		}
		return 0; // Stalemate
	}
}
