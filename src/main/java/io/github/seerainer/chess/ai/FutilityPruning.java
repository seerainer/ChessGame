package io.github.seerainer.chess.ai;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.move.Move;

import io.github.seerainer.chess.ai.evaluation.MaterialEvaluator;

/**
 * Futility pruning techniques for chess search optimization
 */
public class FutilityPruning {
	// Futility pruning constants
	private static final int[] FUTILITY_MARGINS = { 0, 200, 300, 500, 800, 1200, 1700 };
	private static final int REVERSE_FUTILITY_MARGIN = 600;
	private static final int RAZORING_MARGIN = 400;
	private static final int EXTENDED_FUTILITY_MARGIN = 150;
	private static final int PROBCUT_MARGIN = 200;
	private static final int FUTILITY_MOVE_COUNT_MULTIPLIER = 3;
	private static final int QUIESCENCE_FUTILITY_MARGIN = 150;

	// Depth limits
	private static final int MAX_FUTILITY_DEPTH = 6;
	private static final int MAX_REVERSE_FUTILITY_DEPTH = 8;
	private static final int MAX_RAZORING_DEPTH = 4;
	private static final int MAX_PROBCUT_DEPTH = 5;

	/**
	 * Get ProbCut margins
	 */
	public static int[] getProbCutMargins(final int beta, final int alpha) {
		return new int[] { beta + PROBCUT_MARGIN, alpha - PROBCUT_MARGIN };
	}

	/**
	 * Enhanced futility pruning with move count and depth considerations
	 */
	public static boolean shouldApplyFutilityPruning(final Board board, final Move move, final int depth,
			final int staticEval, final int alpha, final int beta, final boolean maximizing, final int moveCount) {

		// Don't prune in check, or for tactical moves, or if depth is too high
		if (board.isKingAttacked() || MoveGenerator.isTacticalMove(board, move) || depth > MAX_FUTILITY_DEPTH) {
			return false;
		}

		// Futility pruning based on static evaluation + margin
		if (depth < FUTILITY_MARGINS.length) {
			final var futilityMargin = FUTILITY_MARGINS[depth];
			if (maximizing ? staticEval + futilityMargin <= alpha : staticEval - futilityMargin >= beta) {
				return true;
			}
		}

		// Move count based futility pruning
		if (depth <= 4 && moveCount > depth * FUTILITY_MOVE_COUNT_MULTIPLIER) {
			return true;
		}

		// Extended futility pruning for very quiet positions
		if (depth <= 2 && Math.abs(staticEval) < EXTENDED_FUTILITY_MARGIN) {
			final var extendedMargin = EXTENDED_FUTILITY_MARGIN + depth * 50;
			if (maximizing ? staticEval + extendedMargin <= alpha : staticEval - extendedMargin >= beta) {
				return true;
			}
		}

		return false;
	}

	/**
	 * Check if quiescence futility pruning should be applied
	 */
	public static boolean shouldApplyQuiescenceFutilityPruning(final Board board, final Move move, final int standPat,
			final int alpha) {

		// Don't prune promotions or when in check
		if (move.getPromotion() != Piece.NONE || board.isKingAttacked()) {
			return false;
		}

		// Don't prune if move gives check
		board.doMove(move);
		final var givesCheck = board.isKingAttacked();
		board.undoMove();
		if (givesCheck) {
			return false;
		}

		// Futility pruning based on capture value
		final var capturedPiece = board.getPiece(move.getTo());
		if (capturedPiece == Piece.NONE) {
			// Prune quiet moves in quiescence (shouldn't happen with proper move
			// generation)
			return true;
		}
		final var captureValue = Math.abs(MaterialEvaluator.getPieceValue(capturedPiece));
		// Prune if even the best case (capturing + margin) can't improve alpha
		return standPat + captureValue + QUIESCENCE_FUTILITY_MARGIN <= alpha;
	}

	/**
	 * Check if razoring should be applied
	 */
	public static boolean shouldApplyRazoring(final Board board, final int depth, final int adjustedEval,
			final int alpha) {
		return depth <= MAX_RAZORING_DEPTH && !board.isKingAttacked() && adjustedEval + RAZORING_MARGIN < alpha;
	}

	/**
	 * Check if reverse futility pruning should be applied
	 */
	public static boolean shouldApplyReverseFutilityPruning(final Board board, final int depth, final int adjustedEval,
			final int beta) {
		return depth <= MAX_REVERSE_FUTILITY_DEPTH && !board.isKingAttacked()
				&& adjustedEval >= beta + REVERSE_FUTILITY_MARGIN;
	}

	/**
	 * Check if ProbCut should be attempted
	 */
	public static boolean shouldAttemptProbCut(final Board board, final int depth, final int beta) {
		return depth >= 4 && depth <= MAX_PROBCUT_DEPTH && !board.isKingAttacked() && Math.abs(beta) < 25000;
	}
}