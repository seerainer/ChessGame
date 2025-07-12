package io.github.seerainer.chess.ai;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.move.Move;

import io.github.seerainer.chess.ai.evaluation.MaterialEvaluator;

/**
 * Static Exchange Evaluation (SEE) for chess moves
 */
public class StaticExchangeEvaluator {

	/**
	 * Simplified Static Exchange Evaluation (SEE) Calculates the material gain/loss
	 * from a capture sequence
	 */
	public static int calculateSEE(final Board board, final Move move) {
		// Simplified SEE implementation
		final var targetPiece = board.getPiece(move.getTo());
		if (targetPiece == Piece.NONE) {
			return 0; // Not a capture
		}

		final var capturingPiece = board.getPiece(move.getFrom());
		final var captureValue = Math.abs(MaterialEvaluator.getPieceValue(targetPiece));
		final var capturingValue = Math.abs(MaterialEvaluator.getPieceValue(capturingPiece));

		// Basic SEE calculation
		var seeScore = captureValue;

		// Check if the capturing piece is defended after the capture
		board.doMove(move);
		final var isDefended = MoveGenerator.isSquareAttacked(board, move.getTo(), board.getSideToMove());
		board.undoMove();

		if (isDefended) {
			// If defended, we might lose our capturing piece
			seeScore -= capturingValue;

			// Further refinement based on piece values
			if (capturingValue > captureValue) {
				// Bad trade - capturing piece worth more than captured piece
				seeScore -= (capturingValue - captureValue) / 2; // Penalty for bad trade
			}
		}

		return seeScore;
	}

	/**
	 * Check if a capture is likely to be favorable based on simple SEE
	 */
	public static boolean isGoodCapture(final Board board, final Move move) {
		return calculateSEE(board, move) >= 0;
	}

	/**
	 * Check if a capture should be pruned based on SEE threshold
	 */
	public static boolean shouldPruneBadCapture(final Board board, final Move move, final int threshold) {
		return calculateSEE(board, move) < threshold;
	}
}