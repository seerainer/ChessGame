package io.github.seerainer.chess.ai;

import java.util.ArrayList;
import java.util.List;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;

import io.github.seerainer.chess.ai.evaluation.MaterialEvaluator;
import io.github.seerainer.chess.ai.utils.ChessUtils;

/**
 * Utility class for generating and filtering chess moves
 */
public class MoveGenerator {
	private static final int MAX_QUIESCENCE_CHECKS = 2;
	private static final int PROMOTION_BONUS = 800;

	/**
	 * Generate moves for quiescence search with smart filtering
	 */
	public static List<Move> generateQuiescenceMoves(final Board board, final int depth) {
		final var quiescenceMoves = new ArrayList<Move>();
		final var allMoves = board.legalMoves();

		var checksGenerated = 0;

		for (final var move : allMoves) {
			// Always include captures and promotions
			if (board.getPiece(move.getTo()) != Piece.NONE || move.getPromotion() != Piece.NONE) {
				quiescenceMoves.add(move);
				continue;
			}

			// Include checks only at deeper levels and limit count
			if (depth > 1 && checksGenerated < MAX_QUIESCENCE_CHECKS) {
				board.doMove(move);
				final var givesCheck = board.isKingAttacked();
				board.undoMove();

				if (givesCheck) {
					quiescenceMoves.add(move);
					checksGenerated++;
				}
			}

			// Include moves that escape from check (when in check)
			if (board.isKingAttacked()) {
				quiescenceMoves.add(move);
			}
		}

		return quiescenceMoves;
	}

	/**
	 * Generate tactical moves (captures, promotions, checks)
	 */
	public static List<Move> generateTacticalMoves(final Board board) {
		final var tacticalMoves = new ArrayList<Move>();

		board.legalMoves().stream().filter(move -> isTacticalMove(board, move)).forEach(tacticalMoves::add);

		return tacticalMoves;
	}

	/**
	 * Get the value of the biggest possible capture on the board
	 */
	public static int getBiggestCaptureValue(final Board board) {
		var maxValue = 0;

		for (final var square : Square.values()) {
			if (square != Square.NONE) {
				final var piece = board.getPiece(square);
				if (piece != Piece.NONE && piece.getPieceSide() != board.getSideToMove()) {
					final var value = Math.abs(MaterialEvaluator.getPieceValue(piece));
					maxValue = Math.max(maxValue, value);
				}
			}
		}

		return maxValue;
	}

	/**
	 * Check if move is a capture
	 */
	public static boolean isCaptureMove(final Board board, final Move move) {
		return board.getPiece(move.getTo()) != Piece.NONE;
	}

	/**
	 * Check if a square is attacked by the specified side
	 */
	public static boolean isSquareAttacked(final Board board, final Square square, final Side attackingSide) {
		// Use the utility class method instead of duplicating code
		return ChessUtils.isSquareAttacked(board, square, attackingSide);
	}

	/**
	 * Check if move is tactical
	 */
	public static boolean isTacticalMove(final Board board, final Move move) {
		// Captures or promotions
		if (board.getPiece(move.getTo()) != Piece.NONE || move.getPromotion() != Piece.NONE) {
			return true;
		}

		// Checks
		board.doMove(move);
		final var givesCheck = board.isKingAttacked();
		board.undoMove();

		return givesCheck;
	}

	/**
	 * Order quiescence moves with enhanced scoring
	 */
	public static List<Move> orderQuiescenceMoves(final Board board, final List<Move> moves) {
		moves.sort((move1, move2) -> {
			final var score1 = scoreQuiescenceMove(board, move1);
			final var score2 = scoreQuiescenceMove(board, move2);
			return Integer.compare(score2, score1);
		});
		return moves;
	}

	/**
	 * Order tactical moves by MVV-LVA
	 */
	public static List<Move> orderTacticalMoves(final Board board, final List<Move> tacticalMoves) {
		tacticalMoves.sort((move1, move2) -> {
			final var score1 = MoveOrdering.scoreTacticalMove(board, move1);
			final var score2 = MoveOrdering.scoreTacticalMove(board, move2);
			return Integer.compare(score2, score1);
		});
		return tacticalMoves;
	}

	/**
	 * Score moves specifically for quiescence search
	 */
	private static int scoreQuiescenceMove(final Board board, final Move move) {
		var score = 0;

		// Promotion bonus
		if (move.getPromotion() != Piece.NONE) {
			score += PROMOTION_BONUS;
			final var promotionValue = MaterialEvaluator.getPieceValue(move.getPromotion());
			score += Math.abs(promotionValue);
		}

		// Enhanced MVV-LVA for captures
		final var capturedPiece = board.getPiece(move.getTo());
		if (capturedPiece != Piece.NONE) {
			final var movingPiece = board.getPiece(move.getFrom());
			final var captureValue = Math.abs(MaterialEvaluator.getPieceValue(capturedPiece));
			final var movingValue = Math.abs(MaterialEvaluator.getPieceValue(movingPiece));

			// Base MVV-LVA score
			score += captureValue * 10 - movingValue;

			// Bonus for capturing higher value pieces
			if (captureValue >= 900) { // Queen
				score += 500;
			} else if (captureValue >= 500) { // Rook
				score += 300;
			} else if (captureValue >= 300) { // Bishop/Knight
				score += 100;
			}

			// SEE bonus/penalty
			final var seeScore = StaticExchangeEvaluator.calculateSEE(board, move);
			score += seeScore / 2; // Scale down SEE impact
		}

		// Check bonus (but lower priority than captures)
		board.doMove(move);
		if (board.isKingAttacked()) {
			score += 50;
		}
		board.undoMove();

		return score;
	}
}