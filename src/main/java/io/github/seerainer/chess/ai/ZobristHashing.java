package io.github.seerainer.chess.ai;

import java.security.SecureRandom;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.CastleRight;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;

/**
 * Zobrist hashing implementation for chess positions
 */
public class ZobristHashing {
	// Zobrist hashing tables
	private static final long[][][] ZOBRIST_PIECES = new long[2][6][64]; // [color][piece][square]
	private static final long[] ZOBRIST_CASTLING = new long[16]; // Castling rights
	private static final long[] ZOBRIST_EN_PASSANT = new long[8]; // En passant files
	private static final long ZOBRIST_SIDE_TO_MOVE;

	static {
		final var random = new SecureRandom();
		random.setSeed(123456789L); // Fixed seed for reproducibility

		// Initialize piece hashes
		for (var color = 0; color < 2; color++) {
			for (var piece = 0; piece < 6; piece++) {
				for (var square = 0; square < 64; square++) {
					ZOBRIST_PIECES[color][piece][square] = random.nextLong();
				}
			}
		}

		// Initialize castling hashes
		for (var i = 0; i < 16; i++) {
			ZOBRIST_CASTLING[i] = random.nextLong();
		}

		// Initialize en passant hashes
		for (var i = 0; i < 8; i++) {
			ZOBRIST_EN_PASSANT[i] = random.nextLong();
		}

		ZOBRIST_SIDE_TO_MOVE = random.nextLong();
	}

	public static long calculateZobristHash(final Board board) {
		var hash = 0L;

		// Hash pieces
		for (final var square : Square.values()) {
			if (square != Square.NONE) {
				final var piece = board.getPiece(square);
				if (piece != Piece.NONE) {
					final var color = piece.getPieceSide() == Side.WHITE ? 0 : 1;
					final var pieceType = piece.getPieceType().ordinal();
					final var squareIndex = square.ordinal();
					hash ^= ZOBRIST_PIECES[color][pieceType][squareIndex];
				}
			}
		}

		// Hash side to move
		if (board.getSideToMove() == Side.BLACK) {
			hash ^= ZOBRIST_SIDE_TO_MOVE;
		}

		// FIXED: Proper castling rights handling
		var castling = 0;
		final var whiteCastling = board.getCastleRight(Side.WHITE);
		final var blackCastling = board.getCastleRight(Side.BLACK);

		// White castling rights
		if (whiteCastling == CastleRight.KING_SIDE || whiteCastling == CastleRight.KING_AND_QUEEN_SIDE) {
			castling |= 1;
		}
		if (whiteCastling == CastleRight.QUEEN_SIDE || whiteCastling == CastleRight.KING_AND_QUEEN_SIDE) {
			castling |= 2;
		}

		// Black castling rights
		if (blackCastling == CastleRight.KING_SIDE || blackCastling == CastleRight.KING_AND_QUEEN_SIDE) {
			castling |= 4;
		}
		if (blackCastling == CastleRight.QUEEN_SIDE || blackCastling == CastleRight.KING_AND_QUEEN_SIDE) {
			castling |= 8;
		}

		hash ^= ZOBRIST_CASTLING[castling];

		// Hash en passant
		if (board.getEnPassant() != Square.NONE) {
			hash ^= ZOBRIST_EN_PASSANT[board.getEnPassant().getFile().ordinal()];
		}

		return hash;
	}
}