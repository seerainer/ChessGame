package io.github.seerainer.chess.ai.evaluation;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.PieceType;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;

/**
 * Enhanced piece protection evaluator to prevent hanging pieces and ensure
 * important pieces are properly defended. This addresses the issue where the
 * engine leaves important pieces unprotected.
 */
public class PieceProtectionEvaluator implements EvaluationComponent {

	private static final int HANGING_PIECE_PENALTY = -1000;
	private static final int WEAK_PIECE_PENALTY = -300;
	private static final int PROTECTION_BONUS = 50;
	private static final int OVERPROTECTION_BONUS = 25;
	private static final int CENTRALIZED_PIECE_PROTECTION_BONUS = 75;

	// Piece values for protection calculations
	private static final int[] PIECE_VALUES = { 0, // NONE
			100, // PAWN
			320, // KNIGHT
			330, // BISHOP
			500, // ROOK
			900, // QUEEN
			20000 // KING
	};

	/**
	 * Check if a piece can attack a specific square
	 */
	private static boolean canPieceAttackSquare(final Board board, final Square from, final Square to) {
		// Check if the move is legal (simplified check)
		final var legalMoves = board.legalMoves();
		return legalMoves.stream().filter(move -> move.getFrom() == from && move.getTo() == to).findFirst()
				.map(_ -> true).orElse(false);
	}

	/**
	 * Evaluate hanging important pieces with extra penalty
	 */
	private static int evaluateHangingImportantPieces(final Board board, final Side side) {
		var score = 0;

		for (final var square : Square.values()) {
			if (square == Square.NONE) {
				continue;
			}

			final var piece = board.getPiece(square);
			// Only check important pieces (not pawns)
			if (piece == Piece.NONE || piece.getPieceSide() != side || (piece.getPieceType() == PieceType.PAWN)) {
				continue;
			}

			final var pieceValue = PIECE_VALUES[piece.getPieceType().ordinal()];

			// Check if this important piece is hanging
			final var isAttacked = board.squareAttackedBy(square, side.flip()) != 0L;
			final var isDefended = board.squareAttackedBy(square, side) != 0L;

			if (isAttacked && !isDefended) {
				// CRITICAL: Important piece is hanging!
				score += HANGING_PIECE_PENALTY * 2 * (pieceValue / 100);

				// Extra penalty for queen and rook hanging
				if (piece.getPieceType() == PieceType.QUEEN) {
					score += HANGING_PIECE_PENALTY * 3;
				} else if (piece.getPieceType() == PieceType.ROOK) {
					score += HANGING_PIECE_PENALTY * 2;
				}
			}
		}

		return score;
	}

	/**
	 * Evaluate how well pieces are protected
	 */
	private static int evaluatePieceProtection(final Board board, final Side side) {
		var score = 0;

		for (final var square : Square.values()) {
			if (square == Square.NONE) {
				continue;
			}

			final var piece = board.getPiece(square);
			if (piece == Piece.NONE || piece.getPieceSide() != side) {
				continue;
			}

			// Skip pawns for performance unless they're advanced
			if (piece.getPieceType() == PieceType.PAWN) {
				final var rank = square.getRank().ordinal();
				if ((side == Side.WHITE && rank < 4) || (side == Side.BLACK && rank > 3)) {
					continue;
				}
			}

			final var pieceValue = PIECE_VALUES[piece.getPieceType().ordinal()];

			// Check if piece is attacked
			final var isAttacked = board.squareAttackedBy(square, side.flip()) != 0L;

			if (isAttacked) {
				// Check if piece is defended
				final var isDefended = board.squareAttackedBy(square, side) != 0L;

				if (!isDefended) {
					// HANGING PIECE - massive penalty
					score += HANGING_PIECE_PENALTY * (pieceValue / 100);
				} else {
					// Piece is attacked but defended - check exchange value
					final var attackerValue = getLowestAttackerValue(board, square, side.flip());
					if (attackerValue > 0 && attackerValue < pieceValue) {
						// Losing exchange - penalty
						score += WEAK_PIECE_PENALTY * ((pieceValue - attackerValue) / 100);
					}
				}
			} else {
				// Piece is not attacked - check if it's protected anyway
				final var isDefended = board.squareAttackedBy(square, side) != 0L;
				if (isDefended) {
					score += PROTECTION_BONUS;

					// Bonus for protecting important pieces
					if (pieceValue >= 500) {
						score += PROTECTION_BONUS;
					}

					// Bonus for protecting centralized pieces
					if (isCentralSquare(square)) {
						score += CENTRALIZED_PIECE_PROTECTION_BONUS;
					}

					// Check for overprotection (multiple defenders)
					final var defenderCount = Long.bitCount(board.squareAttackedBy(square, side));
					if (defenderCount > 1) {
						score += OVERPROTECTION_BONUS * Math.min(defenderCount - 1, 2);
					}
				}
			}
		}

		return score;
	}

	/**
	 * Evaluate if pieces need protection before we move them
	 */
	private static int evaluateProtectionBeforeMoving(final Board board, final Side side) {
		var score = 0;

		// Check if any of our pieces are currently protecting important pieces
		// and if those pieces would be hanging after our move
		for (final var square : Square.values()) {
			if (square == Square.NONE) {
				continue;
			}

			final var piece = board.getPiece(square);
			// Skip pawns and king
			if (piece == Piece.NONE || piece.getPieceSide() != side || piece.getPieceType() == PieceType.PAWN
					|| piece.getPieceType() == PieceType.KING) {
				continue;
			}

			final var pieceValue = PIECE_VALUES[piece.getPieceType().ordinal()];

			// Check if this piece is defended only by one defender
			final var isAttacked = board.squareAttackedBy(square, side.flip()) != 0L;
			if (isAttacked) {
				final var defenders = board.squareAttackedBy(square, side);
				if (Long.bitCount(defenders) == 1) {
					// Only one defender - if that defender moves, piece hangs
					score += WEAK_PIECE_PENALTY * (pieceValue / 100);
				}
			}
		}

		return score;
	}

	/**
	 * Get the value of the lowest attacker
	 */
	private static int getLowestAttackerValue(final Board board, final Square square, final Side attackingSide) {
		var lowestValue = Integer.MAX_VALUE;

		for (final var attackerSquare : Square.values()) {
			if (attackerSquare == Square.NONE) {
				continue;
			}

			final var piece = board.getPiece(attackerSquare);
			if (piece == Piece.NONE || piece.getPieceSide() != attackingSide) {
				continue;
			}

			// Check if this piece can attack the target square
			if (canPieceAttackSquare(board, attackerSquare, square)) {
				final var pieceValue = PIECE_VALUES[piece.getPieceType().ordinal()];
				lowestValue = Math.min(lowestValue, pieceValue);
			}
		}

		return lowestValue == Integer.MAX_VALUE ? 0 : lowestValue;
	}

	/**
	 * Check if a square is in the center of the board
	 */
	private static boolean isCentralSquare(final Square square) {
		final var rank = square.getRank().ordinal();
		final var file = square.getFile().ordinal();

		// Center squares are d4, d5, e4, e5 (files 3,4 and ranks 3,4)
		return (file >= 2 && file <= 5) && (rank >= 2 && rank <= 5);
	}

	@Override
	public int evaluate(final EvaluationContext context) {
		final var board = context.getBoard();
		final var evaluatingSide = context.getEvaluatingSide();

		var score = 0;

		// Evaluate piece protection for both sides
		score += evaluatePieceProtection(board, evaluatingSide);
		score -= evaluatePieceProtection(board, evaluatingSide.flip());

		// Extra penalty for hanging important pieces
		score += evaluateHangingImportantPieces(board, evaluatingSide);

		// Check for pieces that need protection before moving
		score += evaluateProtectionBeforeMoving(board, evaluatingSide);

		return score;
	}

	@Override
	public String getComponentName() {
		return "Piece Protection";
	}

	@Override
	public double getWeight(final EvaluationContext context) {
		// Very high weight to prevent hanging pieces
		return 8.0;
	}
}
