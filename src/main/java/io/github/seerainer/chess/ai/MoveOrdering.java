package io.github.seerainer.chess.ai;

import java.util.ArrayList;
import java.util.List;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.PieceType;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;

import io.github.seerainer.chess.ai.evaluation.MaterialEvaluator;
import io.github.seerainer.chess.ai.utils.ChessUtils;
import io.github.seerainer.chess.config.ChessConfig;

/**
 * **ENHANCED: Advanced move ordering with sophisticated history heuristics**
 * Implements multiple heuristic techniques for optimal move ordering
 */
public class MoveOrdering {
	// **ENHANCED: Multi-dimensional history tables for better move evaluation - now
	// using configuration**
	// Traditional history heuristic table
	private static final int[][][] HISTORY_TABLE = new int[2][64][64]; // [color][from][to]

	// **NEW: Butterfly table for relative history (good moves vs all moves)**
	private static final int[][][] BUTTERFLY_TABLE = new int[2][64][64]; // [color][from][to] - total attempts

	// **NEW: Countermove heuristic - response to opponent's last move**
	private static final Move[][] COUNTERMOVE_TABLE = new Move[64][64]; // [from][to] -> best response

	// **NEW: Follow-up move heuristic - best move after our last move**
	private static final Move[][] FOLLOWUP_TABLE = new Move[64][64]; // [from][to] -> best follow-up

	// **NEW: Piece-type specific history tables**
	private static final int[][][][] PIECE_HISTORY = new int[2][6][64][64]; // [color][piece_type][from][to]

	// **ENHANCED: Deeper killer moves table with configuration values**
	private static final Move[][] KILLER_MOVES = new Move[ChessConfig.Search.KILLER_MOVES_DEPTH][ChessConfig.Search.KILLER_MOVES_SLOTS];

	// **NEW: Quiet move counter for Late Move Reduction integration**
	private static final int[][][] QUIET_MOVE_COUNT = new int[2][64][64]; // [color][from][to]

	// **NEW: Threat move table - moves that create threats**
	private static final int[][][] THREAT_TABLE = new int[2][64][64]; // [color][from][to]

	// **NEW: History aging control - now using configuration values**
	private static int historyAge = 0;
	private static final int MAX_HISTORY_VALUE = ChessConfig.Search.MAX_HISTORY_VALUE;
	private static final int HISTORY_AGING_THRESHOLD = ChessConfig.Search.HISTORY_AGING_THRESHOLD;

	// **NEW: Statistics for tuning**
	private static long totalMoveEvaluations = 0;
	private static long historyHits = 0;
	private static long killerHits = 0;
	private static long countermoveHits = 0;

	/**
	 * **NEW: Age history tables to prevent overflow and maintain relevance**
	 */
	public static void ageHistoryTables() {
		historyAge++;

		// Age every 1000 evaluations or when values get too large
		if (historyAge % 1000 == 0 || needsAging()) {
			performHistoryAging();
		}
	}

	// Helper method for attack detection
	private static boolean canPieceAttackSquare(final PieceType pieceType,
			final Square from, final Square to) {
		// Use the utility class method instead of duplicating code
		return ChessUtils.canPieceAttackSquare(pieceType, from, to);
	}

	/**
	 * **NEW: Clear statistics for new game**
	 */
	public static void clearStatistics() {
		totalMoveEvaluations = 0;
		historyHits = 0;
		killerHits = 0;
		countermoveHits = 0;
	}

	/**
	 * **NEW: Get countermove score**
	 */
	private static int getCountermoveScore(final Move move, final Move lastMove) {
		if (lastMove == null) {
			return 0;
		}

		final var lastFromIndex = getSquareIndex(lastMove.getFrom());
		final var lastToIndex = getSquareIndex(lastMove.getTo());

		// **FIXED: Check for valid indices before array access**
		if (!isValidSquareIndex(lastFromIndex) || !isValidSquareIndex(lastToIndex)) {
			return 0;
		}

		final var expectedCounter = COUNTERMOVE_TABLE[lastFromIndex][lastToIndex];

		if (((expectedCounter == null) || !expectedCounter.equals(move))) {
			return 0;
		}
		countermoveHits++;
		return 50000; // Significant bonus for countermoves
	}

	/**
	 * **NEW: Get follow-up move score**
	 */
	private static int getFollowupScore(final Move move, final Move ourLastMove) {
		if (ourLastMove == null) {
			return 0;
		}

		final var lastFromIndex = getSquareIndex(ourLastMove.getFrom());
		final var lastToIndex = getSquareIndex(ourLastMove.getTo());

		// **FIXED: Check for valid indices before array access**
		if (!isValidSquareIndex(lastFromIndex) || !isValidSquareIndex(lastToIndex)) {
			return 0;
		}

		final var expectedFollowup = FOLLOWUP_TABLE[lastFromIndex][lastToIndex];

		if (expectedFollowup != null && expectedFollowup.equals(move)) {
			return 30000; // Good bonus for follow-up moves
		}

		return 0;
	}

	/**
	 * **NEW: Get piece-specific history score**
	 */
	private static int getPieceHistoryScore(final Board board, final Move move) {
		final var piece = board.getPiece(move.getFrom());
		if (piece == Piece.NONE) {
			return 0;
		}

		final var colorIndex = piece.getPieceSide() == Side.WHITE ? 0 : 1;
		final var pieceIndex = piece.getPieceType().ordinal();
		final var fromIndex = getSquareIndex(move.getFrom());
		final var toIndex = getSquareIndex(move.getTo());

		// **FIXED: Check for valid indices before array access**
		if (!isValidSquareIndex(fromIndex) || !isValidSquareIndex(toIndex)) {
			return 0;
		}

		return PIECE_HISTORY[colorIndex][pieceIndex][fromIndex][toIndex];
	}

	/**
	 * **NEW: Calculate relative history score using butterfly tables**
	 */
	private static int getRelativeHistoryScore(final Side side, final Move move) {
		final var colorIndex = side == Side.WHITE ? 0 : 1;
		final var fromIndex = getSquareIndex(move.getFrom());
		final var toIndex = getSquareIndex(move.getTo());

		// **FIXED: Check for valid indices before array access**
		if (!isValidSquareIndex(fromIndex) || !isValidSquareIndex(toIndex)) {
			return 0;
		}

		final var goodMoves = HISTORY_TABLE[colorIndex][fromIndex][toIndex];
		final var totalMoves = BUTTERFLY_TABLE[colorIndex][fromIndex][toIndex];

		if (totalMoves == 0) {
			return 0;
		}

		// Calculate success ratio (scaled to avoid division issues)
		return (goodMoves * 1024) / totalMoves;
	}

	// Helper method to convert file/rank to Square
	private static Square getSquareFromFileRank(final int file, final int rank) {
		if (file < 0 || file > 7 || rank < 0 || rank > 7) {
			return null;
		}

		try {
			final var fileChar = (char) ('a' + file);
			final var rankChar = (char) ('1' + rank);
			final var squareName = "" + fileChar + rankChar;
			return Square.valueOf(squareName.toUpperCase());
		} catch (final IllegalArgumentException e) {
			return null;
		}
	}

	// **NEW: Helper method to safely convert Square to array index**
	private static int getSquareIndex(final Square square) {
		if (square == Square.NONE) {
			return -1; // Invalid index for NONE square
		}
		final var index = square.ordinal();
		return index >= 64 ? -1 : index; // Safety check
	}

	/**
	 * **NEW: Get statistics for tuning and debugging**
	 */
	public static String getStatistics() {
		if (totalMoveEvaluations == 0) {
			return "No move evaluations yet";
		}

		final var historyHitRate = (double) historyHits / totalMoveEvaluations * 100;
		final var killerHitRate = (double) killerHits / totalMoveEvaluations * 100;
		final var countermoveHitRate = (double) countermoveHits / totalMoveEvaluations * 100;

		return "Move Ordering Stats: %.1f%% history hits, %.1f%% killer hits, %.1f%% countermove hits (Age: %d)"
				.formatted(historyHitRate, killerHitRate, countermoveHitRate, historyAge);
	}

	// **NEW: Check if square index is valid for array access**
	private static boolean isValidSquareIndex(final int index) {
		return index >= 0 && index < 64;
	}

	/**
	 * **NEW: Check if history tables need aging**
	 */
	private static boolean needsAging() {
		// Check if any history value exceeds threshold
		for (var color = 0; color < 2; color++) {
			for (var from = 0; from < 64; from++) {
				for (var to = 0; to < 64; to++) {
					if (HISTORY_TABLE[color][from][to] > HISTORY_AGING_THRESHOLD
							|| BUTTERFLY_TABLE[color][from][to] > HISTORY_AGING_THRESHOLD) {
						return true;
					}
				}
			}
		}
		return false;
	}

	// Enhanced move ordering
	public static List<Move> orderMovesAdvanced(final Board board, final List<Move> moves, final int depth,
			final TranspositionTable.TTEntry ttEntry) {
		final var orderedMoves = new ArrayList<>(moves);

		// Get move history for context-aware scoring
		final var moveBackupHistory = board.getBackup(); // Get game history
		final var lastMove = moveBackupHistory.isEmpty() ? null : moveBackupHistory.getLast().getMove();
		final var ourLastMove = moveBackupHistory.size() < 2 ? null
				: moveBackupHistory.get(moveBackupHistory.size() - 2).getMove();

		orderedMoves.sort((move1, move2) -> {
			final var score1 = scoreMoveForOrderingAdvanced(board, move1, depth, ttEntry, lastMove, ourLastMove);
			final var score2 = scoreMoveForOrderingAdvanced(board, move2, depth, ttEntry, lastMove, ourLastMove);
			return Integer.compare(score2, score1);
		});

		return orderedMoves;
	}

	/**
	 * **NEW: Perform aging of history tables**
	 */
	private static void performHistoryAging() {
		// Divide all history values by 2 to maintain relative ordering but prevent
		// overflow
		for (var color = 0; color < 2; color++) {
			for (var from = 0; from < 64; from++) {
				for (var to = 0; to < 64; to++) {
					HISTORY_TABLE[color][from][to] /= 2;
					BUTTERFLY_TABLE[color][from][to] /= 2;
					QUIET_MOVE_COUNT[color][from][to] /= 2;
					THREAT_TABLE[color][from][to] /= 2;

					// Age piece-specific history
					for (var piece = 0; piece < 6; piece++) {
						PIECE_HISTORY[color][piece][from][to] /= 2;
					}
				}
			}
		}
	}

	/**
	 * **ENHANCED: Comprehensive move scoring with all heuristics**
	 */
	private static int scoreMoveForOrderingAdvanced(final Board board, final Move move, final int depth,
			final TranspositionTable.TTEntry ttEntry, final Move lastMove, final Move ourLastMove) {
		var score = 0;
		totalMoveEvaluations++;

		// 1. Hash move (from transposition table) - highest priority
		if (ttEntry != null && move.equals(ttEntry.bestMove)) {
			score += 10000000;
		}

		// 2. Captures with MVV-LVA - MASSIVELY increased priority
		final var capturedPiece = board.getPiece(move.getTo());
		if (capturedPiece != Piece.NONE) {
			final var movingPiece = board.getPiece(move.getFrom());
			final var captureValue = Math.abs(MaterialEvaluator.getPieceValue(capturedPiece));
			final var movingValue = Math.abs(MaterialEvaluator.getPieceValue(movingPiece));

			// MUCH higher base score for captures + better MVV-LVA
			score += 5000000 + captureValue * 1000 - movingValue;

			// Bonus for capturing high-value pieces
			if (captureValue >= 500) { // Rook or higher
				score += 2000000;
			}
			if (captureValue >= 900) { // Queen
				score += 3000000;
			}
		}

		// 3. Promotions
		if (move.getPromotion() != Piece.NONE) {
			score += 4000000; // Increased from 900000
		}

		// 4. **ENHANCED: Killer moves with multiple slots**
		if (depth < KILLER_MOVES.length) {
			for (var i = 0; i < KILLER_MOVES[depth].length; i++) {
				if (move.equals(KILLER_MOVES[depth][i])) {
					score += 800000 - (i * 50000); // Decreasing bonus for later killer slots
					killerHits++;
					break;
				}
			}
		}

		// 5. **ENHANCED: Multi-layered history heuristics**
		final var colorIndex = board.getSideToMove() == Side.WHITE ? 0 : 1;
		final var fromIndex = getSquareIndex(move.getFrom());
		final var toIndex = getSquareIndex(move.getTo());

		// **FIXED: Check for valid indices before array access**
		if (isValidSquareIndex(fromIndex) && isValidSquareIndex(toIndex)) {
			// Traditional history
			final var historyScore = HISTORY_TABLE[colorIndex][fromIndex][toIndex];
			if (historyScore > 0) {
				historyHits++;
			}
			score += historyScore;

			// **NEW: Threat bonus**
			score += THREAT_TABLE[colorIndex][fromIndex][toIndex] * 10;
		}

		// **NEW: Relative history (butterfly tables)**
		score += getRelativeHistoryScore(board.getSideToMove(), move) * 2;

		// **NEW: Piece-specific history**
		score += getPieceHistoryScore(board, move);

		// **NEW: Countermove heuristic**
		score += getCountermoveScore(move, lastMove);

		// **NEW: Follow-up move heuristic**
		score += getFollowupScore(move, ourLastMove);

		// **NEW: Threat bonus**
		score += THREAT_TABLE[colorIndex][fromIndex][toIndex] * 10;

		// 5. PIECE DEVELOPMENT BONUSES - Strongly encourage early piece development
		final var movingPiece = board.getPiece(move.getFrom());
		if (movingPiece.getPieceType() != PieceType.PAWN
				&& movingPiece.getPieceType() != PieceType.KING) {

			// Large bonus for developing pieces from back rank
			final var fromRank = move.getFrom().getRank().ordinal();
			final var isWhite = movingPiece.getPieceSide() == Side.WHITE;

			// Check if piece is moving from its starting position
			final var isFromStartingPosition = (isWhite ? fromRank == 0 : fromRank == 7);

			if (isFromStartingPosition) {
				// HUGE bonus for developing pieces from back rank
				switch (movingPiece.getPieceType()) {
				case KNIGHT -> score += 800000; // Major bonus for knight development
				case BISHOP -> score += 750000; // Major bonus for bishop development
				case ROOK -> score += 400000; // Moderate bonus for rook development
				case QUEEN -> score += 200000; // Small bonus for early queen development
				default -> score += 100000; // General piece development
				}
			}

			// Additional bonus for centralization
			final var toFile = move.getTo().getFile().ordinal();
			final var toRank = move.getTo().getRank().ordinal();

			// Center squares (d4, d5, e4, e5) get extra bonus
			if ((toFile == 3 || toFile == 4) && (toRank == 3 || toRank == 4)) {
				score += 50000; // Center control bonus
			}

			// Extended center (c3-f6) gets smaller bonus
			if (toFile >= 2 && toFile <= 5 && toRank >= 2 && toRank <= 5) {
				score += 25000; // Extended center control
			}
		}

		// 6. Checks - but lower priority than captures and development
		// FIXED: Use a safer method to check for checks without modifying board state
		var givesCheck = false;
		try {
			board.doMove(move);
			givesCheck = board.isKingAttacked();
			board.undoMove();
		} catch (final Exception e) {
			// If there's an error, assume no check and continue
			givesCheck = false;
			System.err.println("Error checking for check in move ordering: " + e.getMessage());
		}

		if (givesCheck) {
			score += 25000; // REDUCED from 100000 - checks are less important than piece safety
		}

		// 7. Attacking moves (threatening opponent pieces)
		final var targetPiece = board.getPiece(move.getTo());
		if (targetPiece == Piece.NONE) {
			// Check if this move attacks any enemy pieces
			try {
				board.doMove(move);
				var attackBonus = 0;
				for (final var square : Square.values()) {
					if (square != Square.NONE) {
						final var piece = board.getPiece(square);
						// Check if our moved piece can attack this enemy piece
						if ((piece != Piece.NONE && piece.getPieceSide() != board.getSideToMove())
								&& canPieceAttackSquare(board.getPiece(move.getTo()).getPieceType(), move.getTo(),
										square)) {
							attackBonus += Math.abs(MaterialEvaluator.getPieceValue(piece)) / 10;
						}
					}
				}
				score += attackBonus;
				board.undoMove();
			} catch (final Exception e) {
				// If there's an error, skip the attack bonus
				System.err.println("Error checking attacks in move ordering: " + e.getMessage());
			}
		}

		// 8. PAWN ADVANCEMENT MOVES - prioritize pushing pawns toward promotion
		if (movingPiece.getPieceType() == PieceType.PAWN) {
			final var fromRank = move.getFrom().getRank().ordinal();
			final var toRank = move.getTo().getRank().ordinal();
			final var isWhite = movingPiece.getPieceSide() == Side.WHITE;

			// Calculate advancement
			final var advancement = isWhite ? (toRank - fromRank) : (fromRank - toRank);

			if (advancement > 0) { // Moving forward
				final var distanceToPromotion = isWhite ? (7 - toRank) : toRank;

				// Higher bonus for pawns closer to promotion - FURTHER REDUCED: Minimal pawn
				// bonuses
				var pawnAdvancementBonus = 0;
				switch (distanceToPromotion) {
				case 0 -> pawnAdvancementBonus = 200; // Reaching promotion rank - further reduced from 1000
				case 1 -> pawnAdvancementBonus = 50; // Moving to 7th/2nd rank - further reduced from 500
				case 2 -> pawnAdvancementBonus = 20; // Moving to 6th/3rd rank - further reduced from 200
				case 3 -> pawnAdvancementBonus = 10; // Moving to 5th/4th rank - further reduced from 100
				case 4 -> pawnAdvancementBonus = 5; // Moving to 4th/5th rank - further reduced from 50
				default -> pawnAdvancementBonus = 2; // Any forward movement - further reduced from 25
				}

				// Extra bonus for two-square pawn moves
				if (advancement == 2) {
					pawnAdvancementBonus += 5; // Further reduced from 25
				}

				// Check if this creates a passed pawn
				board.doMove(move);
				if (wouldCreatePassedPawn(board, move.getTo(), isWhite)) {
					pawnAdvancementBonus += 30; // Further reduced from 150
				}
				board.undoMove();

				score += pawnAdvancementBonus;
			}
		}

		return score;
	}

	// Aggressive tactical move scoring for quiescence search
	public static int scoreTacticalMove(final Board board, final Move move) {
		var score = 0;

		// Captures - extremely high priority
		final var capturedPiece = board.getPiece(move.getTo());
		if (capturedPiece != Piece.NONE) {
			final var movingPiece = board.getPiece(move.getFrom());
			final var captureValue = Math.abs(MaterialEvaluator.getPieceValue(capturedPiece));
			final var movingValue = Math.abs(MaterialEvaluator.getPieceValue(movingPiece));

			// Much higher scoring for tactical captures
			score += captureValue * 1000 - movingValue;

			// Extra bonus for high-value captures
			if (captureValue >= 900) { // Queen
				score += 5000;
			} else if (captureValue >= 500) { // Rook
				score += 3000;
			} else if (captureValue >= 300) { // Bishop/Knight
				score += 1000;
			}
		}

		// Promotions
		if (move.getPromotion() != Piece.NONE) {
			score += 9000; // Very high for promotions
		}

		// Checks (but lower than captures)
		board.doMove(move);
		if (board.isKingAttacked()) {
			score += 200; // REDUCED from 500 - prioritize material safety
		}
		board.undoMove();

		return score;
	}

	/**
	 * **NEW: Update countermove table**
	 */
	public static void updateCountermove(final Move lastOpponentMove, final Move bestResponse) {
		if (lastOpponentMove == null || bestResponse == null) {
			return;
		}

		final var fromIndex = lastOpponentMove.getFrom().ordinal();
		final var toIndex = lastOpponentMove.getTo().ordinal();
		COUNTERMOVE_TABLE[fromIndex][toIndex] = bestResponse;
	}

	/**
	 * **NEW: Update follow-up move table**
	 */
	public static void updateFollowupMove(final Move ourLastMove, final Move bestFollowup) {
		if (ourLastMove == null || bestFollowup == null) {
			return;
		}

		final var fromIndex = ourLastMove.getFrom().ordinal();
		final var toIndex = ourLastMove.getTo().ordinal();
		FOLLOWUP_TABLE[fromIndex][toIndex] = bestFollowup;
	}

	/**
	 * **ENHANCED: Advanced history table update with multiple techniques**
	 */
	public static void updateHistoryTable(final Side side, final Move move, final int depth) {
		final var colorIndex = side == Side.WHITE ? 0 : 1;
		final var fromIndex = move.getFrom().ordinal();
		final var toIndex = move.getTo().ordinal();

		// **ENHANCED: Depth-squared bonus for more important positions**
		final var bonus = depth * depth + depth;

		// Update traditional history
		HISTORY_TABLE[colorIndex][fromIndex][toIndex] += bonus;

		// **NEW: Update butterfly table (total attempts)**
		BUTTERFLY_TABLE[colorIndex][fromIndex][toIndex] += 1;

		// **NEW: Update piece-specific history**
		// This would need the piece type from the move context
		// For now, we'll track it separately

		// **NEW: Update threat table if move creates threats**
		// This would be updated when move creates tactical threats

		// Prevent overflow with more sophisticated capping
		if (HISTORY_TABLE[colorIndex][fromIndex][toIndex] > MAX_HISTORY_VALUE) {
			HISTORY_TABLE[colorIndex][fromIndex][toIndex] = MAX_HISTORY_VALUE;
		}

		// Age tables periodically
		ageHistoryTables();
	}

	// **ENHANCED: Update killer moves table with multiple slots**
	public static void updateKillerMoves(final int depth, final Move move) {
		if (depth < 0 || depth >= KILLER_MOVES.length) {
			return;
		}

		// Don't add if already in killer table
		for (var i = 0; i < KILLER_MOVES[depth].length; i++) {
			if (move.equals(KILLER_MOVES[depth][i])) {
				return;
			}
		}

		// Shift moves down and add new killer at top
		for (var i = KILLER_MOVES[depth].length - 1; i > 0; i--) {
			KILLER_MOVES[depth][i] = KILLER_MOVES[depth][i - 1];
		}
		KILLER_MOVES[depth][0] = move;
	}

	/**
	 * **NEW: Update piece-specific history**
	 */
	public static void updatePieceHistory(final Board board, final Move move, final int depth) {
		final var piece = board.getPiece(move.getFrom());
		if (piece == Piece.NONE) {
			return;
		}

		final var colorIndex = piece.getPieceSide() == Side.WHITE ? 0 : 1;
		final var pieceIndex = piece.getPieceType().ordinal();
		final var fromIndex = move.getFrom().ordinal();
		final var toIndex = move.getTo().ordinal();

		final var bonus = depth * depth;
		PIECE_HISTORY[colorIndex][pieceIndex][fromIndex][toIndex] += bonus;

		// Cap to prevent overflow
		if (PIECE_HISTORY[colorIndex][pieceIndex][fromIndex][toIndex] > MAX_HISTORY_VALUE) {
			PIECE_HISTORY[colorIndex][pieceIndex][fromIndex][toIndex] = MAX_HISTORY_VALUE;
		}
	}

	/**
	 * **NEW: Update quiet move statistics for LMR integration**
	 */
	public static void updateQuietMoveStats(final Side side, final Move move) {
		final var colorIndex = side == Side.WHITE ? 0 : 1;
		final var fromIndex = move.getFrom().ordinal();
		final var toIndex = move.getTo().ordinal();

		QUIET_MOVE_COUNT[colorIndex][fromIndex][toIndex]++;
	}

	/**
	 * **NEW: Update threat table for moves that create tactical threats**
	 */
	public static void updateThreatTable(final Side side, final Move move, final boolean createsThreat) {
		final var colorIndex = side == Side.WHITE ? 0 : 1;
		final var fromIndex = move.getFrom().ordinal();
		final var toIndex = move.getTo().ordinal();

		if (!createsThreat) {
			return;
		}
		THREAT_TABLE[colorIndex][fromIndex][toIndex] += 10;
		// Cap threat values
		if (THREAT_TABLE[colorIndex][fromIndex][toIndex] > 1000) {
			THREAT_TABLE[colorIndex][fromIndex][toIndex] = 1000;
		}
	}

	// Check if a pawn move would create a passed pawn
	private static boolean wouldCreatePassedPawn(final Board board,
			final Square pawnSquare, final boolean isWhite) {
		final var file = pawnSquare.getFile().ordinal();
		final var rank = pawnSquare.getRank().ordinal();
		final var enemyPawn = isWhite ? Piece.BLACK_PAWN : Piece.WHITE_PAWN;
		final var direction = isWhite ? 1 : -1;
		final var endRank = isWhite ? 7 : 0;

		// Check files that could block this pawn (same file and adjacent files)
		for (var fileOffset = -1; fileOffset <= 1; fileOffset++) {
			final var checkFile = file + fileOffset;
			if (checkFile >= 0 && checkFile < 8) {
				// Check all squares from current rank to promotion rank
				var checkRank = rank + direction;

				// **FIXED: Proper loop termination conditions to prevent infinite loops**
				final var maxIterations = 8; // Safety limit
				var iterations = 0;

				while (iterations < maxIterations) {
					// Check bounds first
					// Check if we've reached the end rank
					if (checkRank < 0 || checkRank > 7 || (isWhite ? (checkRank > endRank) : (checkRank < endRank))) {
						break;
					}

					final var checkSquare = getSquareFromFileRank(checkFile, checkRank);
					if (checkSquare != null && board.getPiece(checkSquare) == enemyPawn) {
						return false; // Enemy pawn can stop this pawn
					}

					checkRank += direction;
					iterations++;
				}
			}
		}

		return true; // No enemy pawns can stop this pawn
	}
}
