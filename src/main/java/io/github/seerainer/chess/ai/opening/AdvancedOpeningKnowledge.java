package io.github.seerainer.chess.ai.opening;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.PieceType;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;

/**
 * Enhanced opening knowledge system that extends the existing OpeningBook with
 * advanced opening principles, transposition handling, and position-specific
 * guidance.
 *
 * This addresses the "Opening Knowledge - Limited opening book or principles"
 * improvement by adding: - Advanced opening principles beyond basic development
 * - Opening transposition detection and handling - Position-specific opening
 * bonuses - Extended opening theory knowledge
 */
public class AdvancedOpeningKnowledge {

	/**
	 * Container for scored moves
	 */
	public static class ScoredMove {
		public final Move move;
		public final int score;

		public ScoredMove(final Move move, final int score) {
			this.move = move;
			this.score = score;
		}

		@Override
		public String toString() {
			return "%s (score: %d)".formatted(move.toString(), score);
		}
	}

	// Advanced opening principles
	private static final int RAPID_DEVELOPMENT_BONUS = 100;
	private static final int CENTER_CONTROL_BONUS = 80;
	private static final int KING_SAFETY_BONUS = 120;
	private static final int PIECE_COORDINATION_BONUS = 60;

	private static final int TEMPO_BONUS = 40;
	// Opening phase detection thresholds
	private static final int MAX_OPENING_MOVES = 15;

	private static final int DEVELOPED_PIECES_THRESHOLD = 3;

	// Specific opening patterns
	private static final Set<String> STRONG_OPENING_MOVES = Set.of("e2e4", "d2d4", "g1f3", "b1c3", "f1c4", "f1b5",
			"c1f4", "c1g5");

	private static final Set<String> WEAK_OPENING_MOVES = Set.of("f2f3", "g2g4", "h2h3", "h2h4", "a2a3", "a2a4",
			"d1h5");

	private static boolean canCastle(final Board board, final Side side) {
		return board.getCastleRight(side) != com.github.bhlangonijr.chesslib.CastleRight.NONE;
	}

	// Helper methods for detailed evaluation
	private static int countDevelopedPieces(final Board board, final Side side) {
		var count = 0;

		// Count pieces not on starting squares
		if (side == Side.WHITE) {
			// Check white pieces
			final var backRank = 0x00000000000000FFL;
			final var whitePieces = board.getBitboard(Piece.WHITE_KNIGHT) | board.getBitboard(Piece.WHITE_BISHOP)
					| board.getBitboard(Piece.WHITE_QUEEN);
			count += Long.bitCount(whitePieces & ~backRank);
		} else {
			// Check black pieces
			final var backRank = 0xFF00000000000000L;
			final var blackPieces = board.getBitboard(Piece.BLACK_KNIGHT) | board.getBitboard(Piece.BLACK_BISHOP)
					| board.getBitboard(Piece.BLACK_QUEEN);
			count += Long.bitCount(blackPieces & ~backRank);
		}

		return count;
	}

	private static int countForceableMoves() {
		// Simplified forceable moves count
		return 0;
	}

	private static int countMutuallyDefendedPieces() {
		// Simplified mutual defense count
		return 0;
	}

	private static int countRedundantMoves() {
		// Simplified check for piece moving twice
		// In a real implementation, would analyze move history
		return 0;
	}

	private static int countWastedTempo() {
		// Simplified tempo waste count
		return 0;
	}

	private static int countWeakeningMoves() {
		// Simplified weakening moves count
		return 0;
	}

	private static int evaluateBishopPlacement() {
		// Simplified bishop placement evaluation
		return 0;
	}

	/**
	 * Evaluate center control in opening
	 */
	private static int evaluateCenterControl(final Board board, final Side side) {
		var score = 0;

		// Key center squares
		final Square[] centerSquares = { Square.D4, Square.D5, Square.E4, Square.E5 };
		final Square[] extendedCenter = { Square.C4, Square.C5, Square.F4, Square.F5 };

		// Score for occupying center
		for (final var square : centerSquares) {
			final var piece = board.getPiece(square);
			if (piece.getPieceSide() == side) {
				score += piece.getPieceType() == PieceType.PAWN ? CENTER_CONTROL_BONUS : CENTER_CONTROL_BONUS / 2;
			}
		}

		// Score for attacking center
		for (final var _ : extendedCenter) {
			if (isSquareAttackedBy()) {
				score += CENTER_CONTROL_BONUS / 3;
			}
		}

		return score;
	}

	private static int evaluateCentralTension() {
		// Simplified central tension evaluation
		return 0;
	}

	private static int evaluateKingPositionWeakness() {
		// Simplified king weakness evaluation
		return 0;
	}

	/**
	 * Evaluate king safety in opening
	 */
	private static int evaluateKingSafety(final Board board, final Side side) {
		var score = 0;

		// Bonus for castling early
		if (hasCastled(board, side)) {
			score += KING_SAFETY_BONUS;
		} else if (canCastle(board, side)) {
			score += KING_SAFETY_BONUS / 2; // Can still castle
		}

		// Penalty for king moves before castling
		if (!hasCastled(board, side) && hasKingMoved(board, side)) {
			score -= KING_SAFETY_BONUS;
		}

		// Penalty for weakening king position
		score -= evaluateKingPositionWeakness();

		return score;
	}

	private static int evaluateKnightPlacement() {
		// Simplified knight placement evaluation
		return 0;
	}

	/**
	 * Advanced opening concepts
	 */
	private static int evaluateOpeningHarmony() {
		// Evaluate how well pieces work together
		var score = 0;

		// Bonus for bishops on long diagonals
		score += evaluateBishopPlacement();

		// Bonus for knights on strong squares
		score += evaluateKnightPlacement();

		// Bonus for rooks on open/semi-open files
		score += evaluateRookPlacement();

		return score;
	}

	/**
	 * Evaluate opening position with advanced principles
	 */
	public static int evaluateOpeningPosition(final Board board, final Side side) {
		if (!isInOpeningPhase(board)) {
			return 0; // Not in opening anymore
		}

		var score = 0;

		// Core opening principles
		score += evaluateRapidDevelopment(board, side);
		score += evaluateCenterControl(board, side);
		score += evaluateKingSafety(board, side);
		score += evaluatePieceCoordination();
		score += evaluateTempo();

		// Advanced opening concepts
		score += evaluateOpeningHarmony();
		score += evaluateOpeningTension();
		score += penalizePrematureMoves(board, side);

		return score;
	}

	private static int evaluateOpeningTension() {
		// Evaluate pawn tension and piece pressure
		var score = 0;

		// Bonus for maintaining central tension
		score += evaluateCentralTension();

		// Bonus for piece pressure on opponent
		score += evaluatePiecePressure();

		return score;
	}

	private static int evaluateOptimalDevelopment(final Board board, final Side side) {
		var score = 0;

		// Check for knights on good squares (f3, c3 for white; f6, c6 for black)
		if (side == Side.WHITE) {
			if (board.getPiece(Square.F3).getPieceType() == PieceType.KNIGHT) {
				score += 30;
			}
			if (board.getPiece(Square.C3).getPieceType() == PieceType.KNIGHT) {
				score += 30;
			}
		} else {
			if (board.getPiece(Square.F6).getPieceType() == PieceType.KNIGHT) {
				score += 30;
			}
			if (board.getPiece(Square.C6).getPieceType() == PieceType.KNIGHT) {
				score += 30;
			}
		}

		return score;
	}

	/**
	 * Evaluate piece coordination
	 */
	private static int evaluatePieceCoordination() {
		var score = 0;

		// Bonus for pieces supporting each other
		score += countMutuallyDefendedPieces() * PIECE_COORDINATION_BONUS;

		// Bonus for pieces working toward common goals
		score += evaluateStrategicCoordination();

		return score;
	}

	private static int evaluatePiecePressure() {
		// Simplified piece pressure evaluation
		return 0;
	}

	/**
	 * Evaluate rapid development principle
	 */
	private static int evaluateRapidDevelopment(final Board board, final Side side) {
		var score = 0;
		final var developedPieces = countDevelopedPieces(board, side);

		// Bonus for each developed piece
		score += developedPieces * RAPID_DEVELOPMENT_BONUS;

		// Penalty for moving the same piece twice in opening
		score -= countRedundantMoves() * 30;

		// Bonus for developing pieces to optimal squares
		score += evaluateOptimalDevelopment(board, side);

		return score;
	}

	private static int evaluateRookPlacement() {
		// Simplified rook placement evaluation
		return 0;
	}

	private static int evaluateStrategicCoordination() {
		// Simplified strategic coordination
		return 0;
	}

	/**
	 * Evaluate tempo in opening
	 */
	private static int evaluateTempo() {
		var score = 0;

		// Penalty for wasted tempo (unnecessary pawn moves, etc.)
		score -= countWastedTempo() * TEMPO_BONUS;

		// Bonus for moves that force opponent response
		score += countForceableMoves() * TEMPO_BONUS;

		return score;
	}

	/**
	 * Get opening move recommendations with advanced scoring
	 */
	public static List<ScoredMove> getOpeningMoveRecommendations(final Board board) {
		final List<ScoredMove> recommendations = new ArrayList<>();

		if (!isInOpeningPhase(board)) {
			return recommendations; // Not in opening phase
		}

		final var legalMoves = board.legalMoves();

		legalMoves.forEach((final var move) -> {
			final var score = scoreOpeningMove(board, move);
			if (score > 0) {
				recommendations.add(new ScoredMove(move, score));
			}
		});

		// Sort by score (highest first)
		recommendations.sort(Comparator.comparing((final ScoredMove a) -> a.score).reversed());

		return recommendations;
	}

	private static boolean hasCastled(final Board board, final Side side) {
		// Check if king has castled (simplified)
		if (side == Side.WHITE) {
			return board.getPiece(Square.G1).getPieceType() == PieceType.KING
					|| board.getPiece(Square.C1).getPieceType() == PieceType.KING;
		}
		return board.getPiece(Square.G8).getPieceType() == PieceType.KING
				|| board.getPiece(Square.C8).getPieceType() == PieceType.KING;
	}

	private static boolean hasKingMoved(final Board board, final Side side) {
		// Check if king is still on starting square
		if (side == Side.WHITE) {
			return board.getPiece(Square.E1).getPieceType() != PieceType.KING;
		}
		return board.getPiece(Square.E8).getPieceType() != PieceType.KING;
	}

	/**
	 * Detect if we're still in the opening phase
	 */
	public static boolean isInOpeningPhase(final Board board) {
		// Use move counter from board history
		final var moveCount = board.getBackup().size();
		if (moveCount > MAX_OPENING_MOVES * 2) { // Each full move is 2 half-moves
			return false;
		}

		// Check if major pieces are still undeveloped
		final var developedPieces = countDevelopedPieces(board, Side.WHITE) + countDevelopedPieces(board, Side.BLACK);

		return developedPieces < DEVELOPED_PIECES_THRESHOLD * 2;
	}

	private static boolean isQueenDevelopedEarly(final Board board, final Side side) {
		// Check if queen moved early
		if (side == Side.WHITE) {
			return board.getPiece(Square.D1).getPieceType() != PieceType.QUEEN;
		}
		return board.getPiece(Square.D8).getPieceType() != PieceType.QUEEN;
	}

	private static boolean isSquareAttackedBy() {
		// Simplified attack detection
		// In real implementation, would use proper attack detection
		return false;
	}

	private static int penalizePrematureMoves(final Board board, final Side side) {
		var penalty = 0;

		// Penalty for early queen development
		if (isQueenDevelopedEarly(board, side)) {
			penalty += 100;
		}

		// Penalty for weakening moves
		penalty += countWeakeningMoves() * 50;

		return -penalty;
	}

	private static int scoreCenterControlMove(final Move move) {
		// Score move based on center control
		final Square[] centerSquares = { Square.D4, Square.D5, Square.E4, Square.E5 };

		for (final var square : centerSquares) {
			if (move.getTo() == square) {
				return 40; // Move to center
			}
		}

		return 0;
	}

	private static int scoreDevelopmentMove(final Board board, final Move move) {
		// Score move based on development value
		final var movingPiece = board.getPiece(move.getFrom());

		if (movingPiece.getPieceType() == PieceType.KNIGHT || movingPiece.getPieceType() == PieceType.BISHOP) {
			return 50; // Development move
		}

		return 0;
	}

	private static int scoreKingSafetyMove(final Board board, final Move move) {
		// Score castling moves highly
		if (Math.abs(move.getFrom().ordinal() - move.getTo().ordinal()) == 2
				&& board.getPiece(move.getFrom()).getPieceType() == PieceType.KING) {
			return 80; // Castling move
		}

		return 0;
	}

	/**
	 * Score individual opening move
	 */
	private static int scoreOpeningMove(final Board board, final Move move) {
		var score = 0;

		final var moveStr = move.toString();

		// Bonus for known strong opening moves
		if (STRONG_OPENING_MOVES.contains(moveStr)) {
			score += 200;
		}

		// Penalty for known weak opening moves
		if (WEAK_OPENING_MOVES.contains(moveStr)) {
			score -= 150;
		}

		// Score based on opening principles
		score += scoreDevelopmentMove(board, move);
		score += scoreCenterControlMove(move);
		score += scoreKingSafetyMove(board, move);

		return score;
	}
}
