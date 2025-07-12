package io.github.seerainer.chess.ai.utils;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.PieceType;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;

/**
 * Utility class for common chess operations to avoid code duplication
 */
public class ChessUtils {

	/**
	 * Check if a piece can attack a specific square (simplified)
	 */
	public static boolean canPieceAttackSquare(final Piece piece, final Square from, final Square to) {
		final var pieceType = piece.getPieceType();
		final var fromFile = from.getFile().ordinal();
		final var fromRank = from.getRank().ordinal();
		final var toFile = to.getFile().ordinal();
		final var toRank = to.getRank().ordinal();
		final var fileDiff = Math.abs(toFile - fromFile);
		final var rankDiff = Math.abs(toRank - fromRank);

		return switch (pieceType) {
		case PAWN -> {
			// Simplified pawn attack (diagonal capture)
			final var direction = piece.getPieceSide() == Side.WHITE ? 1 : -1;
			yield fileDiff == 1 && (toRank - fromRank) == direction;
		}
		case ROOK -> fromFile == toFile || fromRank == toRank;
		case BISHOP -> fileDiff == rankDiff;
		case QUEEN -> fromFile == toFile || fromRank == toRank || fileDiff == rankDiff;
		case KNIGHT -> (fileDiff == 2 && rankDiff == 1) || (fileDiff == 1 && rankDiff == 2);
		case KING -> fileDiff <= 1 && rankDiff <= 1;
		default -> false;
		};
	}

	/**
	 * Overloaded method for piece type only
	 */
	public static boolean canPieceAttackSquare(final PieceType pieceType, final Square from, final Square to) {
		final var fromFile = from.getFile().ordinal();
		final var fromRank = from.getRank().ordinal();
		final var toFile = to.getFile().ordinal();
		final var toRank = to.getRank().ordinal();
		final var fileDiff = Math.abs(toFile - fromFile);
		final var rankDiff = Math.abs(toRank - fromRank);

		return switch (pieceType) {
		case PAWN -> false; // Pawn attacks are complex, skip for simplicity
		case ROOK -> fromFile == toFile || fromRank == toRank;
		case BISHOP -> fileDiff == rankDiff;
		case QUEEN -> fromFile == toFile || fromRank == toRank || fileDiff == rankDiff;
		case KNIGHT -> (fileDiff == 2 && rankDiff == 1) || (fileDiff == 1 && rankDiff == 2);
		case KING -> fileDiff <= 1 && rankDiff <= 1;
		default -> false;
		};
	}

	/**
	 * Check if a square is attacked by the specified side
	 */
	public static boolean isSquareAttacked(final Board board, final Square square, final Side attackingSide) {
		// Simplified attack detection - check if any piece of the attacking side can
		// move to the square
		for (final var attackerSquare : Square.values()) {
			if (attackerSquare != Square.NONE) {
				final var piece = board.getPiece(attackerSquare);
				if (piece != Piece.NONE && piece.getPieceSide() == attackingSide
						&& canPieceAttackSquare(piece, attackerSquare, square)) {
					return true;
				}
			}
		}
		return false;
	}

	// Private constructor to prevent instantiation
	private ChessUtils() {
		throw new AssertionError("Utility class should not be instantiated");
	}
}
