package io.github.seerainer.chess.ai.evaluation;

import java.util.ArrayDeque;
import java.util.Deque;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.PieceType;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;

import io.github.seerainer.chess.config.ChessConfig;

/**
 * Incremental evaluation system that updates evaluation scores efficiently
 * instead of recalculating from scratch each time
 */
public class IncrementalEvaluator {

	/**
	 * Complete evaluation state snapshot
	 */
	private static class EvaluationState {
		int materialScore;
		int positionalScore;
		int pawnStructureScore;
		int kingSafetyScore;
		int mobilityScore;
		int totalScore;

		EvaluationState copy() {
			final var copy = new EvaluationState();
			copy.materialScore = this.materialScore;
			copy.positionalScore = this.positionalScore;
			copy.pawnStructureScore = this.pawnStructureScore;
			copy.kingSafetyScore = this.kingSafetyScore;
			copy.mobilityScore = this.mobilityScore;
			copy.totalScore = this.totalScore;
			return copy;
		}
	}

	// Position-specific piece square tables (simplified)
	private static final int[][] PIECE_SQUARE_TABLE = new int[6][64];

	static {
		initializePieceSquareTables();
	}

	private static int calculateKingSafetyScore() {
		// Simplified king safety evaluation
		return 0; // Placeholder
	}

	private static int calculateMaterialScore(final Board board) {
		var score = 0;
		for (final var square : Square.values()) {
			if (square != Square.NONE) {
				final var piece = board.getPiece(square);
				if (piece != Piece.NONE) {
					final var value = MaterialEvaluator.getPieceValue(piece);
					score += piece.getPieceSide() == Side.WHITE ? value : -value;
				}
			}
		}
		return score;
	}

	private static int calculateMobilityScore(final Board board) {
		// Simplified mobility evaluation
		return board.legalMoves().size();
	}

	private static int calculatePawnStructureScore() {
		// Simplified pawn structure evaluation
		return 0; // Placeholder
	}

	private static int calculatePositionalScore(final Board board) {
		var score = 0;
		for (final var square : Square.values()) {
			if (square != Square.NONE) {
				final var piece = board.getPiece(square);
				if (piece != Piece.NONE) {
					final var pieceIndex = piece.getPieceType().ordinal();
					final var squareValue = PIECE_SQUARE_TABLE[pieceIndex][square.ordinal()];
					score += piece.getPieceSide() == Side.WHITE ? squareValue : -squareValue;
				}
			}
		}
		return score;
	}

	/**
	 * Initialize piece square tables
	 */
	private static void initializePieceSquareTables() {
		// Initialize with center preference for most pieces
		for (var piece = 0; piece < 6; piece++) {
			for (var square = 0; square < 64; square++) {
				final var file = square % 8;
				final var rank = square / 8;

				// Center bonus
				final var centerDistanceDouble = Math.max(Math.abs(file - 3.5), Math.abs(rank - 3.5));
				final var centerBonus = (int) ((4 - centerDistanceDouble) * 5);

				PIECE_SQUARE_TABLE[piece][square] = centerBonus;
			}
		}

		// Special adjustments for specific pieces can be added here
	}

	/**
	 * Check if square is near a king
	 */
	private static boolean isNearKing() {
		// Simple check - within 2 squares of king position
		// This would need to be implemented with actual king positions
		return false; // Simplified for now
	}

	// Evaluation state
	private EvaluationState currentState;

	private final Deque<EvaluationState> stateStack = new ArrayDeque<>();

	public IncrementalEvaluator() {
		this.currentState = new EvaluationState();
	}

	/**
	 * Get current evaluation score
	 */
	public int getCurrentScore(final Side perspective) {
		return perspective == Side.WHITE ? currentState.totalScore : -currentState.totalScore;
	}

	/**
	 * Get detailed evaluation breakdown
	 */
	public String getEvaluationBreakdown() {
		return "Material: %d, Positional: %d, Pawns: %d, King: %d, Mobility: %d, Total: %d".formatted(
				currentState.materialScore, currentState.positionalScore, currentState.pawnStructureScore,
				currentState.kingSafetyScore, currentState.mobilityScore, currentState.totalScore);
	}

	/**
	 * Initialize evaluation from board position
	 */
	public void initializeFromBoard(final Board board) {
		currentState = new EvaluationState();

		// Calculate initial evaluation components
		currentState.materialScore = calculateMaterialScore(board);
		currentState.positionalScore = calculatePositionalScore(board);
		currentState.pawnStructureScore = calculatePawnStructureScore();
		currentState.kingSafetyScore = calculateKingSafetyScore();
		currentState.mobilityScore = calculateMobilityScore(board);

		updateTotalScore();
	}

	/**
	 * Update evaluation incrementally when making a move
	 */
	public void makeMove(final Board board, final Move move) {
		// Save current state
		stateStack.push(currentState.copy());

		// Get move details
		final var movingPiece = board.getPiece(move.getFrom());
		final var capturedPiece = board.getPiece(move.getTo());
		final var fromSquare = move.getFrom();
		final var toSquare = move.getTo();

		// Update material score
		if (capturedPiece != Piece.NONE) {
			updateMaterialForCapture(capturedPiece);
		}

		// Update positional score
		updatePositionalForMove(movingPiece, fromSquare, toSquare);

		// Handle special moves
		if (move.getPromotion() != Piece.NONE) {
			updateMaterialForPromotion(movingPiece, move.getPromotion());
			updatePositionalForPromotion(move.getPromotion(), toSquare);
		}

		// Incremental updates for other components
		updatePawnStructureIncremental(board, move);
		updateKingSafetyIncremental(board, move);
		updateMobilityIncremental(board);

		updateTotalScore();
	}

	/**
	 * Restore evaluation when unmaking a move
	 */
	public void unmakeMove() {
		if (!stateStack.isEmpty()) {
			currentState = stateStack.pop();
		}
	}

	/**
	 * Incremental update for king safety (simplified)
	 */
	private void updateKingSafetyIncremental(final Board board, final Move move) {
		// For moves affecting king safety, recalculate
		final var movingPiece = board.getPiece(move.getFrom());
		if (movingPiece.getPieceType() == PieceType.KING || isNearKing() || isNearKing()) {
			currentState.kingSafetyScore = calculateKingSafetyScore();
		}
	}

	// Calculation methods for full evaluation (used for initialization)

	/**
	 * Update material score for capture
	 */
	private void updateMaterialForCapture(final Piece capturedPiece) {
		final var pieceValue = MaterialEvaluator.getPieceValue(capturedPiece);

		if (capturedPiece.getPieceSide() == Side.WHITE) {
			currentState.materialScore -= Math.abs(pieceValue);
		} else {
			currentState.materialScore += Math.abs(pieceValue);
		}
	}

	/**
	 * Update material score for promotion
	 */
	private void updateMaterialForPromotion(final Piece pawn, final Piece promotedPiece) {
		final var pawnValue = MaterialEvaluator.getPieceValue(pawn);
		final var promotedValue = MaterialEvaluator.getPieceValue(promotedPiece);
		final var gain = Math.abs(promotedValue) - Math.abs(pawnValue);

		if (pawn.getPieceSide() == Side.WHITE) {
			currentState.materialScore += gain;
		} else {
			currentState.materialScore -= gain;
		}
	}

	/**
	 * Incremental update for mobility (simplified)
	 */
	private void updateMobilityIncremental(final Board board) {
		// Simplified: recalculate mobility (can be optimized with incremental updates)
		currentState.mobilityScore = calculateMobilityScore(board);
	}

	/**
	 * Incremental update for pawn structure (simplified)
	 */
	private void updatePawnStructureIncremental(final Board board, final Move move) {
		// For now, recalculate if pawn move (can be optimized further)
		final var movingPiece = board.getPiece(move.getFrom());
		if (movingPiece.getPieceType() == PieceType.PAWN) {
			// Recalculate pawn structure (this can be made more incremental)
			currentState.pawnStructureScore = calculatePawnStructureScore();
		}
	}

	/**
	 * Update positional score for piece movement
	 */
	private void updatePositionalForMove(final Piece piece, final Square from, final Square to) {
		final var pieceIndex = piece.getPieceType().ordinal();
		final var isWhite = piece.getPieceSide() == Side.WHITE;

		// Remove piece from old square
		final var oldSquareValue = PIECE_SQUARE_TABLE[pieceIndex][from.ordinal()];
		if (isWhite) {
			currentState.positionalScore -= oldSquareValue;
		} else {
			currentState.positionalScore += oldSquareValue;
		}

		// Add piece to new square
		final var newSquareValue = PIECE_SQUARE_TABLE[pieceIndex][to.ordinal()];
		if (isWhite) {
			currentState.positionalScore += newSquareValue;
		} else {
			currentState.positionalScore -= newSquareValue;
		}
	}

	/**
	 * Update positional score for promotion
	 */
	private void updatePositionalForPromotion(final Piece promotedPiece, final Square square) {
		final var pieceIndex = promotedPiece.getPieceType().ordinal();
		final var isWhite = promotedPiece.getPieceSide() == Side.WHITE;

		// Remove pawn positional value and add promoted piece value
		final var pawnValue = PIECE_SQUARE_TABLE[PieceType.PAWN.ordinal()][square.ordinal()];
		final var promotedValue = PIECE_SQUARE_TABLE[pieceIndex][square.ordinal()];
		final var valueDiff = promotedValue - pawnValue;

		if (isWhite) {
			currentState.positionalScore += valueDiff;
		} else {
			currentState.positionalScore -= valueDiff;
		}
	}

	/**
	 * Update total score from components
	 */
	private void updateTotalScore() {
		currentState.totalScore = currentState.materialScore * ChessConfig.Evaluation.PIECE_VALUES_PAWN / 100
				+ currentState.positionalScore * ChessConfig.Evaluation.MOBILITY_WEIGHT / 100
				+ currentState.pawnStructureScore * ChessConfig.Evaluation.PAWN_STRUCTURE_WEIGHT / 100
				+ currentState.kingSafetyScore * ChessConfig.Evaluation.KING_SAFETY_WEIGHT / 100
				+ currentState.mobilityScore * ChessConfig.Evaluation.MOBILITY_WEIGHT / 100;
	}
}
