package io.github.seerainer.chess.ai;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

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
 * Implements multiple heuristic techniques for optimal move ordering.
 *
 * <p>
 * <b>Thread-safety note:</b> The static history arrays ({@code HISTORY_TABLE},
 * {@code BUTTERFLY_TABLE}, {@code COUNTERMOVE_TABLE}, {@code FOLLOWUP_TABLE},
 * {@code PIECE_HISTORY}, {@code KILLER_MOVES}, {@code THREAT_TABLE}) are
 * intentionally unsynchronized and shared across parallel search threads. This
 * follows the "benign data race" pattern used by production chess engines (e.g.
 * Stockfish): torn reads/writes only affect heuristic move ordering quality,
 * never search correctness. Adding synchronization would severely degrade
 * parallel search throughput for negligible accuracy gains.
 * </p>
 *
 * <p>
 * Mutable counters ({@code historyAge}, {@code totalMoveEvaluations}, etc.) use
 * {@code AtomicInteger}/{@code AtomicLong} to avoid undefined behavior on
 * concurrent increments; exact accuracy is not required but atomic operations
 * are cheap enough to be worthwhile.
 * </p>
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

    // **NEW: Threat move table - moves that create threats**
    private static final int[][][] THREAT_TABLE = new int[2][64][64]; // [color][from][to]

    // **NEW: History aging control - now using configuration values**
    private static final AtomicInteger historyAge = new AtomicInteger(0);

    private static final int HISTORY_AGING_THRESHOLD = ChessConfig.Search.HISTORY_AGING_THRESHOLD;

    // **NEW: Statistics for tuning (atomic for thread-safety in parallel search)**
    private static final AtomicLong totalMoveEvaluations = new AtomicLong(0);

    private static final AtomicLong historyHits = new AtomicLong(0);

    private static final AtomicLong killerHits = new AtomicLong(0);

    private static final AtomicLong countermoveHits = new AtomicLong(0);

    private MoveOrdering() {
	throw new IllegalStateException("Utility class");
    }

    /**
     * **NEW: Age history tables to prevent overflow and maintain relevance**
     */
    public static void ageHistoryTables() {
	historyAge.incrementAndGet();

	// Age every 1000 evaluations or when values get too large
	if (historyAge.get() % 1000 == 0 || needsAging()) {
	    performHistoryAging();
	}
    }

    // Helper method for attack detection
    private static boolean canPieceAttackSquare(final PieceType pieceType, final Square from, final Square to) {
	// Use the utility class method instead of duplicating code
	return ChessUtils.canPieceAttackSquare(pieceType, from, to);
    }

    /**
     * **NEW: Clear statistics for new game**
     */
    public static void clearStatistics() {
	totalMoveEvaluations.set(0);
	historyHits.set(0);
	killerHits.set(0);
	countermoveHits.set(0);
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
	countermoveHits.incrementAndGet();
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
	    final var squareName = new StringBuilder().append("").append(fileChar).append(rankChar).toString();
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
	final var total = totalMoveEvaluations.get();
	if (total == 0) {
	    return "No move evaluations yet";
	}

	final var historyHitRate = (double) historyHits.get() / total * 100;
	final var killerHitRate = (double) killerHits.get() / total * 100;
	final var countermoveHitRate = (double) countermoveHits.get() / total * 100;

	return "Move Ordering Stats: %.1f%% history hits, %.1f%% killer hits, %.1f%% countermove hits (Age: %d)"
		.formatted(historyHitRate, killerHitRate, countermoveHitRate, historyAge.get());
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
	totalMoveEvaluations.incrementAndGet();

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
		    killerHits.incrementAndGet();
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
		historyHits.incrementAndGet();
	    }
	    score += historyScore;

	    // Threat bonus
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

	// 5. PIECE DEVELOPMENT BONUSES - Strongly encourage early piece development
	final var movingPiece = board.getPiece(move.getFrom());
	if (movingPiece.getPieceType() != PieceType.PAWN && movingPiece.getPieceType() != PieceType.KING) {

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

	// 6. Checks - use static detection (piece attacks enemy king square from
	// destination)
	final var enemySide = movingPiece.getPieceSide().flip();
	final var enemyKingPiece = enemySide == Side.WHITE ? Piece.WHITE_KING : Piece.BLACK_KING;
	var enemyKingSquare = Square.NONE;
	for (final var sq : Square.values()) {
	    if (sq != Square.NONE && board.getPiece(sq) == enemyKingPiece) {
		enemyKingSquare = sq;
		break;
	    }
	}
	if (enemyKingSquare != Square.NONE
		&& canPieceAttackSquare(movingPiece.getPieceType(), move.getTo(), enemyKingSquare)) {
	    score += 25000; // Check bonus
	}

	// 7. Attacking moves — static detection from destination square
	final var targetPiece = board.getPiece(move.getTo());
	if (targetPiece == Piece.NONE) {
	    // Check if our piece type can attack any enemy piece from the destination
	    // square
	    var attackBonus = 0;
	    for (final var square : Square.values()) {
		if (square != Square.NONE) {
		    final var piece = board.getPiece(square);
		    if (piece != Piece.NONE && piece.getPieceSide() != movingPiece.getPieceSide()
			    && canPieceAttackSquare(movingPiece.getPieceType(), move.getTo(), square)) {
			attackBonus += Math.abs(MaterialEvaluator.getPieceValue(piece)) / 10;
		    }
		}
	    }
	    score += attackBonus;
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

    // Check if a pawn move would create a passed pawn
    private static boolean wouldCreatePassedPawn(final Board board, final Square pawnSquare, final boolean isWhite) {
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
