package io.github.seerainer.chess.ai.evaluation;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.PieceType;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;

/**
 * Simple tactical pattern evaluator to improve basic tactical recognition
 * Focuses on the most common 1-2 move tactics that were missing
 */
public class SimpleTacticalEvaluator implements EvaluationComponent {

	private static final int KNIGHT_FORK_BONUS = 600;
	private static final int PAWN_FORK_BONUS = 300;
	private static final int HANGING_PIECE_PENALTY = 400;
	private static final int BACK_RANK_MATE_THREAT = 800;
	private static final int DOUBLE_ATTACK_BONUS = 450;

	/**
	 * Check if a piece can attack a square
	 */
	private static boolean canPieceAttackSquare(final Board board, final Square fromSquare, final Square toSquare) {
		final var piece = board.getPiece(fromSquare);
		if (piece == Piece.NONE) {
			return false;
		}

		final var rankDiff = Math.abs(fromSquare.getRank().ordinal() - toSquare.getRank().ordinal());
		final var fileDiff = Math.abs(fromSquare.getFile().ordinal() - toSquare.getFile().ordinal());

		return switch (piece.getPieceType()) {
		case KNIGHT -> (rankDiff == 2 && fileDiff == 1) || (rankDiff == 1 && fileDiff == 2);
		case BISHOP -> rankDiff == fileDiff && rankDiff > 0;
		case ROOK -> (rankDiff == 0 && fileDiff > 0) || (fileDiff == 0 && rankDiff > 0);
		case QUEEN -> ((rankDiff == fileDiff && rankDiff > 0) || (rankDiff == 0 && fileDiff > 0)
				|| (fileDiff == 0 && rankDiff > 0));
		case PAWN -> {
			final var direction = (piece.getPieceSide() == Side.WHITE) ? 1 : -1;
			yield rankDiff == 1 && fileDiff == 1
					&& (toSquare.getRank().ordinal() - fromSquare.getRank().ordinal()) == direction;
		}
		case KING -> rankDiff <= 1 && fileDiff <= 1 && (rankDiff + fileDiff > 0);
		default -> false;
		};
	}

	/**
	 * Count attacks in a specific direction
	 */
	private static int countAttacksInDirection(final Board board, final Square fromSquare, final int rankDir,
			final int fileDir, final Side opponentSide) {
		final var fromRank = fromSquare.getRank().ordinal();
		final var fromFile = fromSquare.getFile().ordinal();

		for (var i = 1; i < 8; i++) {
			final var newRank = fromRank + rankDir * i;
			final var newFile = fromFile + fileDir * i;

			if (newRank < 0 || newRank > 7 || newFile < 0 || newFile > 7) {
				break;
			}

			final var square = getSquareFromCoords(newRank, newFile);
			if (square == Square.NONE) {
				break;
			}

			final var piece = board.getPiece(square);
			if (piece != Piece.NONE) {
				if (piece.getPieceSide() == opponentSide) {
					return 1; // Found an attack
				}
				break; // Piece blocks further attacks
			}
		}

		return 0;
	}

	/**
	 * Count diagonal attacks (for bishop/queen)
	 */
	private static int countDiagonalAttacks(final Board board, final Square fromSquare, final Side opponentSide) {
		var count = 0;
		final int[][] directions = { { 1, 1 }, { 1, -1 }, { -1, 1 }, { -1, -1 } };

		for (final int[] dir : directions) {
			count += countAttacksInDirection(board, fromSquare, dir[0], dir[1], opponentSide);
		}

		return count;
	}

	/**
	 * Count how many opponent pieces a knight attacks
	 */
	private static int countKnightAttacks(final Board board, final Square knightSquare, final Side opponentSide) {
		var count = 0;

		// Knight moves: 8 possible L-shaped moves
		final int[][] knightMoves = { { 2, 1 }, { 2, -1 }, { -2, 1 }, { -2, -1 }, { 1, 2 }, { 1, -2 }, { -1, 2 },
				{ -1, -2 } };

		final var knightRank = knightSquare.getRank().ordinal();
		final var knightFile = knightSquare.getFile().ordinal();

		for (final int[] move : knightMoves) {
			final var newRank = knightRank + move[0];
			final var newFile = knightFile + move[1];

			if (newRank >= 0 && newRank <= 7 && newFile >= 0 && newFile <= 7) {
				final var targetSquare = getSquareFromCoords(newRank, newFile);
				if (targetSquare != Square.NONE) {
					final var targetPiece = board.getPiece(targetSquare);
					if (targetPiece.getPieceSide() == opponentSide) {
						count++;
					}
				}
			}
		}

		return count;
	}

	/**
	 * Count linear attacks (for rook/queen)
	 */
	private static int countLinearAttacks(final Board board, final Square fromSquare, final Side opponentSide) {
		var count = 0;
		final int[][] directions = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };

		for (final int[] dir : directions) {
			count += countAttacksInDirection(board, fromSquare, dir[0], dir[1], opponentSide);
		}

		return count;
	}

	/**
	 * Count how many opponent pieces a pawn attacks
	 */
	private static int countPawnAttacks(final Board board, final Square pawnSquare, final Side opponentSide) {
		var count = 0;

		final var pawn = board.getPiece(pawnSquare);
		final var pawnSide = pawn.getPieceSide();

		final var direction = (pawnSide == Side.WHITE) ? 1 : -1;
		final var pawnRank = pawnSquare.getRank().ordinal();
		final var pawnFile = pawnSquare.getFile().ordinal();
		final var attackRank = pawnRank + direction;

		if (attackRank >= 0 && attackRank <= 7) {
			// Check both diagonal attacks
			final int[] attackFiles = { pawnFile - 1, pawnFile + 1 };

			for (final int attackFile : attackFiles) {
				if (attackFile >= 0 && attackFile <= 7) {
					final var attackSquare = getSquareFromCoords(attackRank, attackFile);
					if (attackSquare != Square.NONE) {
						final var targetPiece = board.getPiece(attackSquare);
						if (targetPiece.getPieceSide() == opponentSide) {
							count++;
						}
					}
				}
			}
		}

		return count;
	}

	/**
	 * Count total attacks from a piece
	 */
	private static int countTotalAttacks(final Board board, final Square fromSquare, final Side opponentSide) {
		final var piece = board.getPiece(fromSquare);

		return switch (piece.getPieceType()) {
		case KNIGHT -> countKnightAttacks(board, fromSquare, opponentSide);
		case PAWN -> countPawnAttacks(board, fromSquare, opponentSide);
		case BISHOP -> countDiagonalAttacks(board, fromSquare, opponentSide);
		case ROOK -> countLinearAttacks(board, fromSquare, opponentSide);
		case QUEEN ->
			countDiagonalAttacks(board, fromSquare, opponentSide) + countLinearAttacks(board, fromSquare, opponentSide);
		default -> 0;
		};
	}

	/**
	 * Evaluate back rank mate threats
	 */
	private static int evaluateBackRankThreats(final Board board, final Side side) {
		var score = 0;

		final var opponentKing = findKingSquare(board, side.flip());
		if (opponentKing == null) {
			return 0;
		}

		final var kingRank = opponentKing.getRank().ordinal();
		final var opponentSide = side.flip();

		// Check if opponent king is on back rank
		if ((opponentSide == Side.WHITE && kingRank == 0) || (opponentSide == Side.BLACK && kingRank == 7)) {

			// Check if we have a rook or queen on the same rank
			for (final var square : Square.values()) {
				if (square == Square.NONE) {
					continue;
				}

				final var piece = board.getPiece(square);
				if ((piece.getPieceSide() == side
						&& (piece.getPieceType() == PieceType.ROOK || piece.getPieceType() == PieceType.QUEEN))
						&& (square.getRank().ordinal() == kingRank)) {
					score += BACK_RANK_MATE_THREAT;
				}
			}
		}

		return score;
	}

	/**
	 * Evaluate double attacks (one piece attacking two targets)
	 */
	private static int evaluateDoubleAttacks(final Board board, final Side side) {
		var score = 0;

		for (final var square : Square.values()) {
			if (square == Square.NONE) {
				continue;
			}

			final var piece = board.getPiece(square);
			if (piece.getPieceSide() != side) {
				continue;
			}

			final var attacks = countTotalAttacks(board, square, side.flip());
			if (attacks >= 2) {
				score += DOUBLE_ATTACK_BONUS;
			}
		}

		return score;
	}

	/**
	 * Evaluate hanging pieces (undefended pieces)
	 */
	private static int evaluateHangingPieces(final Board board, final Side side) {
		var score = 0;

		for (final var square : Square.values()) {
			if (square == Square.NONE) {
				continue;
			}

			final var piece = board.getPiece(square);
			if (piece.getPieceSide() != side) {
				continue;
			}

			// Check if piece is hanging (attacked but not defended)
			if (isSquareAttacked(board, square, side.flip()) && !isSquareDefended(board, square, side)) {
				score -= HANGING_PIECE_PENALTY * getPieceValue(piece) / 100;
			}
		}

		return score;
	}

	/**
	 * Evaluate knight fork opportunities
	 */
	private static int evaluateKnightForks(final Board board, final Side side) {
		var score = 0;

		for (final var square : Square.values()) {
			if (square == Square.NONE) {
				continue;
			}

			final var piece = board.getPiece(square);
			if (piece.getPieceSide() != side || piece.getPieceType() != PieceType.KNIGHT) {
				continue;
			}

			// Check if knight can fork (attack 2+ pieces)
			final var attacks = countKnightAttacks(board, square, side.flip());
			if (attacks >= 2) {
				score += KNIGHT_FORK_BONUS;

				// Extra bonus if attacking king and another piece
				if (knightAttacksKing(board, square, side.flip())) {
					score += KNIGHT_FORK_BONUS;
				}
			}
		}

		return score;
	}

	/**
	 * Evaluate pawn fork opportunities
	 */
	private static int evaluatePawnForks(final Board board, final Side side) {
		var score = 0;

		for (final var square : Square.values()) {
			if (square == Square.NONE) {
				continue;
			}

			final var piece = board.getPiece(square);
			if (piece.getPieceSide() != side || piece.getPieceType() != PieceType.PAWN) {
				continue;
			}

			// Check if pawn can fork (attack 2 pieces diagonally)
			final var attacks = countPawnAttacks(board, square, side.flip());
			if (attacks >= 2) {
				score += PAWN_FORK_BONUS;
			}
		}

		return score;
	}

	/**
	 * Find king square
	 */
	private static Square findKingSquare(final Board board, final Side side) {
		for (final var square : Square.values()) {
			if (square == Square.NONE) {
				continue;
			}

			final var piece = board.getPiece(square);
			if (piece.getPieceSide() == side && piece.getPieceType() == PieceType.KING) {
				return square;
			}
		}
		return null;
	}

	/**
	 * Get piece value
	 */
	private static int getPieceValue(final Piece piece) {
		return switch (piece.getPieceType()) {
		case PAWN -> 100;
		case KNIGHT -> 320;
		case BISHOP -> 330;
		case ROOK -> 500;
		case QUEEN -> 900;
		case KING -> 20000;
		default -> 0;
		};
	}

	/**
	 * Get square from coordinates
	 */
	private static Square getSquareFromCoords(final int rank, final int file) {
		if (rank < 0 || rank > 7 || file < 0 || file > 7) {
			return Square.NONE;
		}

		final var fileChar = (char) ('a' + file);
		final var rankChar = (char) ('1' + rank);
		final var squareName = "" + fileChar + rankChar;

		try {
			return Square.valueOf(squareName.toUpperCase());
		} catch (final IllegalArgumentException e) {
			return Square.NONE;
		}
	}

	/**
	 * Check if a square is attacked
	 */
	private static boolean isSquareAttacked(final Board board, final Square square, final Side attackingSide) {
		// Simple check - see if any attacking piece can reach this square
		for (final var move : board.legalMoves()) {
			if (move.getTo() == square) {
				final var movingPiece = board.getPiece(move.getFrom());
				if (movingPiece.getPieceSide() == attackingSide) {
					return true;
				}
			}
		}
		return false;
	}

	/**
	 * Check if a square is defended
	 */
	private static boolean isSquareDefended(final Board board, final Square square, final Side defendingSide) {
		// Make a temporary move to see if the square is still attacked
		// This is a simplified check
		for (final var defenderSquare : Square.values()) {
			if (defenderSquare == Square.NONE || defenderSquare == square) {
				continue;
			}

			final var defender = board.getPiece(defenderSquare);
			// Check if this piece can "see" the defended square
			if ((defender.getPieceSide() == defendingSide) && canPieceAttackSquare(board, defenderSquare, square)) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check if knight attacks the opponent's king
	 */
	private static boolean knightAttacksKing(final Board board, final Square knightSquare, final Side opponentSide) {
		final var kingSquare = findKingSquare(board, opponentSide);
		if (kingSquare == null) {
			return false;
		}

		final var knightRank = knightSquare.getRank().ordinal();
		final var knightFile = knightSquare.getFile().ordinal();
		final var kingRank = kingSquare.getRank().ordinal();
		final var kingFile = kingSquare.getFile().ordinal();

		final var rankDiff = Math.abs(knightRank - kingRank);
		final var fileDiff = Math.abs(knightFile - kingFile);

		return (rankDiff == 2 && fileDiff == 1) || (rankDiff == 1 && fileDiff == 2);
	}

	@Override
	public int evaluate(final EvaluationContext context) {
		final var board = context.getBoard();
		final var side = context.getEvaluatingSide();

		var score = 0;

		// Evaluate tactical opportunities for our side
		score += evaluateKnightForks(board, side);
		score += evaluatePawnForks(board, side);
		score += evaluateHangingPieces(board, side);
		score += evaluateBackRankThreats(board, side);
		score += evaluateDoubleAttacks(board, side);

		// Subtract opponent's tactical threats
		score -= evaluateKnightForks(board, side.flip());
		score -= evaluatePawnForks(board, side.flip());
		score -= evaluateHangingPieces(board, side.flip());
		score -= evaluateBackRankThreats(board, side.flip());
		score -= evaluateDoubleAttacks(board, side.flip());

		return score;
	}

	@Override
	public String getComponentName() {
		return "SimpleTactical";
	}
}
