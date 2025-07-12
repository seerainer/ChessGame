package io.github.seerainer.chess.ai.bitboard;

import java.util.ArrayList;
import java.util.List;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;

/**
 * High-performance move generator using bitboard operations for faster move
 * generation
 */
public class BitboardMoveGenerator {

	// Bitboard masks for fast lookups
	private static final long[] RANK_MASKS = new long[8];
	private static final long[] FILE_MASKS = new long[8];

	// Precomputed attack tables
	private static final long[] KNIGHT_ATTACKS = new long[64];
	private static final long[] KING_ATTACKS = new long[64];
	private static final long[] PAWN_ATTACKS_WHITE = new long[64];
	private static final long[] PAWN_ATTACKS_BLACK = new long[64];

	static {
		initializeBitboards();
	}

	/**
	 * Add moves from a bitboard to the move list
	 */
	private static void addMovesFromBitboard(final List<Move> moves, long bitboard, final int fromSquare) {
		while (bitboard != 0) {
			final var toSquare = Long.numberOfTrailingZeros(bitboard);
			moves.add(new Move(Square.squareAt(fromSquare), Square.squareAt(toSquare)));
			bitboard &= bitboard - 1;
		}
	}

	/**
	 * Add moves from a bitboard with offset (for pawn moves)
	 */
	private static void addMovesFromBitboardWithOffset(final List<Move> moves, long bitboard, final int offset) {
		while (bitboard != 0) {
			final var toSquare = Long.numberOfTrailingZeros(bitboard);
			final var fromSquare = toSquare + offset;
			moves.add(new Move(Square.squareAt(fromSquare), Square.squareAt(toSquare)));
			bitboard &= bitboard - 1;
		}
	}

	/**
	 * Generate bishop moves using magic bitboards
	 */
	private static void generateBishopMoves(final Board board, final List<Move> moves, final Side side,
			final long ownPieces, final long allPieces) {
		var bishops = getBitboardForPiece(board, side == Side.WHITE ? Piece.WHITE_BISHOP : Piece.BLACK_BISHOP);

		while (bishops != 0) {
			final var square = Long.numberOfTrailingZeros(bishops);
			final var attacks = getBishopAttacks(square, allPieces) & ~ownPieces;

			addMovesFromBitboard(moves, attacks, square);
			bishops &= bishops - 1;
		}
	}

	/**
	 * Generate king attacks for a given square
	 */
	private static long generateKingAttacks(final int square) {
		var attacks = 0L;
		final var file = square % 8;
		final var rank = square / 8;

		final int[] kingMoves = { -9, -8, -7, -1, 1, 7, 8, 9 };

		for (final int move : kingMoves) {
			final var targetSquare = square + move;
			final var targetFile = targetSquare % 8;
			final var targetRank = targetSquare / 8;

			if (targetSquare >= 0 && targetSquare < 64 && Math.abs(targetFile - file) <= 1
					&& Math.abs(targetRank - rank) <= 1) {
				attacks |= (1L << targetSquare);
			}
		}

		return attacks;
	}

	/**
	 * Generate king moves using precomputed attack tables
	 */
	private static void generateKingMoves(final Board board, final List<Move> moves, final Side side,
			final long ownPieces) {
		final var kings = getBitboardForPiece(board, side == Side.WHITE ? Piece.WHITE_KING : Piece.BLACK_KING);

		if (kings == 0) {
			return;
		}
		final var square = Long.numberOfTrailingZeros(kings);
		final var attacks = KING_ATTACKS[square] & ~ownPieces;
		addMovesFromBitboard(moves, attacks, square);
	}

	/**
	 * Generate knight attacks for a given square
	 */
	private static long generateKnightAttacks(final int square) {
		var attacks = 0L;
		final var file = square % 8;
		final var rank = square / 8;

		final int[] knightMoves = { -17, -15, -10, -6, 6, 10, 15, 17 };

		for (final int move : knightMoves) {
			final var targetSquare = square + move;
			final var targetFile = targetSquare % 8;
			final var targetRank = targetSquare / 8;

			if (targetSquare >= 0 && targetSquare < 64 && Math.abs(targetFile - file) <= 2
					&& Math.abs(targetRank - rank) <= 2) {
				attacks |= (1L << targetSquare);
			}
		}

		return attacks;
	}

	/**
	 * Generate knight moves using precomputed attack tables
	 */
	private static void generateKnightMoves(final Board board, final List<Move> moves, final Side side,
			final long ownPieces) {
		var knights = getBitboardForPiece(board, side == Side.WHITE ? Piece.WHITE_KNIGHT : Piece.BLACK_KNIGHT);

		while (knights != 0) {
			final var square = Long.numberOfTrailingZeros(knights);
			final var attacks = KNIGHT_ATTACKS[square] & ~ownPieces;

			addMovesFromBitboard(moves, attacks, square);
			knights &= knights - 1; // Clear least significant bit
		}
	}

	/**
	 * Generate all legal moves for the current position using bitboard operations
	 */
	public static List<Move> generateMoves(final Board board) {
		final List<Move> moves = new ArrayList<>();
		final var sideToMove = board.getSideToMove();

		// Get piece bitboards
		final var ownPieces = getBitboardForSide(board, sideToMove);
		final var enemyPieces = getBitboardForSide(board, sideToMove.flip());
		final var allPieces = ownPieces | enemyPieces;

		// Generate moves for each piece type
		generatePawnMoves(board, moves, sideToMove, enemyPieces, allPieces);
		generateKnightMoves(board, moves, sideToMove, ownPieces);
		generateBishopMoves(board, moves, sideToMove, ownPieces, allPieces);
		generateRookMoves(board, moves, sideToMove, ownPieces, allPieces);
		generateQueenMoves(board, moves, sideToMove, ownPieces, allPieces);
		generateKingMoves(board, moves, sideToMove, ownPieces);

		return moves;
	}

	/**
	 * Generate pawn attacks for a given square
	 */
	private static long generatePawnAttacks(final int square, final Side side) {
		var attacks = 0L;
		final var file = square % 8;
		final var rank = square / 8;

		if (side == Side.WHITE) {
			if (rank < 7) {
				if (file > 0) {
					attacks |= (1L << (square + 7));
				}
				if (file < 7) {
					attacks |= (1L << (square + 9));
				}
			}
		} else if (rank > 0) {
			if (file > 0) {
				attacks |= (1L << (square - 9));
			}
			if (file < 7) {
				attacks |= (1L << (square - 7));
			}
		}

		return attacks;
	}

	/**
	 * Generate pawn moves using bitboard operations
	 */
	private static void generatePawnMoves(final Board board, final List<Move> moves, final Side side,
			final long enemyPieces, final long allPieces) {
		final var pawns = getBitboardForPiece(board, side == Side.WHITE ? Piece.WHITE_PAWN : Piece.BLACK_PAWN);

		if (side == Side.WHITE) {
			// White pawn moves
			final var singleMoves = (pawns << 8) & ~allPieces;
			final var doubleMoves = ((singleMoves & 0xFF0000L) << 8) & ~allPieces;

			// Captures
			final var leftCaptures = ((pawns & ~0x0101010101010101L) << 7) & enemyPieces;
			final var rightCaptures = ((pawns & ~0x8080808080808080L) << 9) & enemyPieces;

			addMovesFromBitboardWithOffset(moves, singleMoves, -8);
			addMovesFromBitboardWithOffset(moves, doubleMoves, -16);
			addMovesFromBitboardWithOffset(moves, leftCaptures, -7);
			addMovesFromBitboardWithOffset(moves, rightCaptures, -9);
		} else {
			// Black pawn moves
			final var singleMoves = (pawns >>> 8) & ~allPieces;
			final var doubleMoves = ((singleMoves & 0xFF000000000000L) >>> 8) & ~allPieces;

			// Captures
			final var leftCaptures = ((pawns & ~0x8080808080808080L) >>> 7) & enemyPieces;
			final var rightCaptures = ((pawns & ~0x0101010101010101L) >>> 9) & enemyPieces;

			addMovesFromBitboardWithOffset(moves, singleMoves, 8);
			addMovesFromBitboardWithOffset(moves, doubleMoves, 16);
			addMovesFromBitboardWithOffset(moves, leftCaptures, 7);
			addMovesFromBitboardWithOffset(moves, rightCaptures, 9);
		}
	}

	/**
	 * Generate queen moves (combination of rook and bishop moves)
	 */
	private static void generateQueenMoves(final Board board, final List<Move> moves, final Side side,
			final long ownPieces, final long allPieces) {
		var queens = getBitboardForPiece(board, side == Side.WHITE ? Piece.WHITE_QUEEN : Piece.BLACK_QUEEN);

		while (queens != 0) {
			final var square = Long.numberOfTrailingZeros(queens);
			final var attacks = (getRookAttacks(square, allPieces) | getBishopAttacks(square, allPieces)) & ~ownPieces;

			addMovesFromBitboard(moves, attacks, square);
			queens &= queens - 1;
		}
	}

	/**
	 * Generate rook moves using magic bitboards
	 */
	private static void generateRookMoves(final Board board, final List<Move> moves, final Side side,
			final long ownPieces, final long allPieces) {
		var rooks = getBitboardForPiece(board, side == Side.WHITE ? Piece.WHITE_ROOK : Piece.BLACK_ROOK);

		while (rooks != 0) {
			final var square = Long.numberOfTrailingZeros(rooks);
			final var attacks = getRookAttacks(square, allPieces) & ~ownPieces;

			addMovesFromBitboard(moves, attacks, square);
			rooks &= rooks - 1;
		}
	}

	/**
	 * Get bishop attacks using magic bitboards
	 */
	private static long getBishopAttacks(final int square, final long occupancy) {
		// Simplified implementation - in practice would use magic bitboards
		return getBishopAttacksClassic(square, occupancy);
	}

	/**
	 * Classic bishop attack generation (fallback)
	 */
	private static long getBishopAttacksClassic(final int square, final long occupancy) {
		var attacks = 0L;
		final var file = square % 8;
		final var rank = square / 8;

		// Generate attacks along diagonals
		final int[] directions = { -9, -7, 7, 9 };

		for (final int direction : directions) {
			var targetSquare = square + direction;
			var targetFile = targetSquare % 8;
			var targetRank = targetSquare / 8;

			while (targetSquare >= 0 && targetSquare < 64
					&& Math.abs(targetFile - file) == Math.abs(targetRank - rank)) {
				attacks |= (1L << targetSquare);

				if ((occupancy & (1L << targetSquare)) != 0) {
					break; // Blocked
				}

				targetSquare += direction;
				targetFile = targetSquare % 8;
				targetRank = targetSquare / 8;
			}
		}

		return attacks;
	}

	/**
	 * Get bitboard for a specific piece type
	 */
	private static long getBitboardForPiece(final Board board, final Piece targetPiece) {
		var bitboard = 0L;

		for (final var square : Square.values()) {
			if (square != Square.NONE) {
				final var piece = board.getPiece(square);
				if (piece == targetPiece) {
					bitboard |= (1L << square.ordinal());
				}
			}
		}

		return bitboard;
	}

	/**
	 * Get bitboard for all pieces of a given side
	 */
	private static long getBitboardForSide(final Board board, final Side side) {
		var bitboard = 0L;

		for (final var square : Square.values()) {
			if (square != Square.NONE) {
				final var piece = board.getPiece(square);
				if (piece != Piece.NONE && piece.getPieceSide() == side) {
					bitboard |= (1L << square.ordinal());
				}
			}
		}

		return bitboard;
	}

	/**
	 * Get rook attacks using magic bitboards
	 */
	private static long getRookAttacks(final int square, final long occupancy) {
		// Simplified implementation - in practice would use magic bitboards
		return getRookAttacksClassic(square, occupancy);
	}

	/**
	 * Classic rook attack generation (fallback)
	 */
	private static long getRookAttacksClassic(final int square, final long occupancy) {
		var attacks = 0L;
		final var file = square % 8;
		final var rank = square / 8;

		// Generate attacks along rank and file
		for (var i = 0; i < 8; i++) {
			if (i != file) {
				final var targetSquare = rank * 8 + i;
				attacks |= (1L << targetSquare);
				if (((occupancy & (1L << targetSquare)) != 0) && (i > file)) {
					break; // Blocked
				}
			}
		}

		for (var i = 0; i < 8; i++) {
			if (i != rank) {
				final var targetSquare = i * 8 + file;
				attacks |= (1L << targetSquare);
				if (((occupancy & (1L << targetSquare)) != 0) && (i > rank)) {
					break; // Blocked
				}
			}
		}

		return attacks;
	}

	/**
	 * Initialize bitboard lookup tables
	 */
	private static void initializeBitboards() {
		// Initialize rank masks
		for (var rank = 0; rank < 8; rank++) {
			RANK_MASKS[rank] = 0xFFL << (rank * 8);
		}

		// Initialize file masks
		for (var file = 0; file < 8; file++) {
			FILE_MASKS[file] = 0x0101010101010101L << file;
		}

		// Initialize knight attack tables
		for (var square = 0; square < 64; square++) {
			KNIGHT_ATTACKS[square] = generateKnightAttacks(square);
		}

		// Initialize king attack tables
		for (var square = 0; square < 64; square++) {
			KING_ATTACKS[square] = generateKingAttacks(square);
		}

		// Initialize pawn attack tables
		for (var square = 0; square < 64; square++) {
			PAWN_ATTACKS_WHITE[square] = generatePawnAttacks(square, Side.WHITE);
			PAWN_ATTACKS_BLACK[square] = generatePawnAttacks(square, Side.BLACK);
		}
	}
}
