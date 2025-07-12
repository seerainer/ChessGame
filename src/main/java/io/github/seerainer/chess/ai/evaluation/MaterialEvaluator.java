package io.github.seerainer.chess.ai.evaluation;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.PieceType;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;

import io.github.seerainer.chess.config.ChessConfig;

/**
 * Specialized evaluator for material balance and piece values
 */
public class MaterialEvaluator implements EvaluationComponent {
	// Cached piece values for faster lookup - now using configuration values
	private static final int[] PIECE_VALUES = new int[Piece.values().length];

	static {
		for (final var piece : Piece.values()) {
			PIECE_VALUES[piece.ordinal()] = calculatePieceValue(piece);
		}
	}

	private static int calculatePieceValue(final Piece piece) {
		return switch (piece) {
		case WHITE_PAWN -> ChessConfig.Evaluation.PIECE_VALUES_PAWN;
		case WHITE_KNIGHT -> ChessConfig.Evaluation.PIECE_VALUES_KNIGHT;
		case WHITE_BISHOP -> ChessConfig.Evaluation.PIECE_VALUES_BISHOP;
		case WHITE_ROOK -> ChessConfig.Evaluation.PIECE_VALUES_ROOK;
		case WHITE_QUEEN -> ChessConfig.Evaluation.PIECE_VALUES_QUEEN;
		case WHITE_KING -> ChessConfig.Evaluation.PIECE_VALUES_KING;
		case BLACK_PAWN -> -ChessConfig.Evaluation.PIECE_VALUES_PAWN;
		case BLACK_KNIGHT -> -ChessConfig.Evaluation.PIECE_VALUES_KNIGHT;
		case BLACK_BISHOP -> -ChessConfig.Evaluation.PIECE_VALUES_BISHOP;
		case BLACK_ROOK -> -ChessConfig.Evaluation.PIECE_VALUES_ROOK;
		case BLACK_QUEEN -> -ChessConfig.Evaluation.PIECE_VALUES_QUEEN;
		case BLACK_KING -> -ChessConfig.Evaluation.PIECE_VALUES_KING;
		default -> 0;
		};
	}

	/**
	 * Count pieces of specific type for a side
	 */
	public static int countPieces(final Board board, final Side side, final PieceType pieceType) {
		var count = 0;

		for (final var square : Square.values()) {
			if (square != Square.NONE) {
				final var piece = board.getPiece(square);
				if (piece != Piece.NONE && piece.getPieceSide() == side && piece.getPieceType() == pieceType) {
					count++;
				}
			}
		}

		return count;
	}

	/**
	 * Calculate material balance for the board
	 */
	public static int evaluateMaterial(final Board board) {
		var materialScore = 0;

		for (final var square : Square.values()) {
			if (square != Square.NONE) {
				final var piece = board.getPiece(square);
				if (piece != Piece.NONE) {
					materialScore += PIECE_VALUES[piece.ordinal()];
				}
			}
		}

		return materialScore;
	}

	/**
	 * Calculate material advantage for a specific side
	 */
	public static int getMaterialAdvantage(final Board board, final Side side) {
		var advantage = 0;

		for (final var square : Square.values()) {
			if (square != Square.NONE) {
				final var piece = board.getPiece(square);
				if (piece != Piece.NONE) {
					final var value = Math.abs(PIECE_VALUES[piece.ordinal()]);
					if (piece.getPieceSide() == side) {
						advantage += value;
					} else {
						advantage -= value;
					}
				}
			}
		}

		return advantage;
	}

	/**
	 * Get piece value from cache
	 */
	public static int getPieceValue(final Piece piece) {
		return PIECE_VALUES[piece.ordinal()];
	}

	/**
	 * Calculate total material on board
	 */
	public static int getTotalMaterial(final Board board) {
		var total = 0;

		for (final var square : Square.values()) {
			if (square != Square.NONE) {
				final var piece = board.getPiece(square);
				if (piece != Piece.NONE && piece.getPieceType() != PieceType.KING) {
					total += Math.abs(PIECE_VALUES[piece.ordinal()]);
				}
			}
		}

		return total;
	}

	/**
	 * Check if position is endgame based on material
	 */
	public static boolean isEndgame(final Board board) {
		return getTotalMaterial(board) <= 2000; // Threshold for endgame
	}

	@Override
	public int evaluate(final EvaluationContext context) {
		// Use cached material score - but getMaterialScore already calls
		// evaluateMaterial
		// So we can just return it directly
		return context.getMaterialScore();
	}

	@Override
	public String getComponentName() {
		return "Material Balance";
	}

	@Override
	public double getWeight(final EvaluationContext context) {
		return 1.0; // Material is fundamental, so weight 1.0
	}
}