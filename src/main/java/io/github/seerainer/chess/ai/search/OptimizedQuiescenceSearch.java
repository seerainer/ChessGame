package io.github.seerainer.chess.ai.search;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.move.Move;

import io.github.seerainer.chess.ai.MoveGenerator;
import io.github.seerainer.chess.ai.MoveOrdering;
import io.github.seerainer.chess.ai.PositionEvaluator;
import io.github.seerainer.chess.ai.StaticExchangeEvaluator;
import io.github.seerainer.chess.ai.evaluation.MaterialEvaluator;
import io.github.seerainer.chess.config.ChessConfig;

/**
 * Optimized quiescence search with delta pruning and SEE pruning
 */
public class OptimizedQuiescenceSearch {

    // Delta pruning margins
    private static final int DELTA_PRUNING_MARGIN = 200;
    private static final int BIG_DELTA_MARGIN = 900; // Queen value for big delta
    // SEE pruning threshold
    private static final int SEE_PRUNE_THRESHOLD = -50;
    // Maximum quiescence depth to prevent infinite search
    private static final int MAX_QUIESCENCE_DEPTH = ChessConfig.Search.QUIESCENCE_MAX_DEPTH;
    // Statistics counters (atomic for thread-safety in parallel search)
    private static final AtomicInteger nodeCount = new AtomicInteger(0);
    private static final AtomicInteger deltaPruningCount = new AtomicInteger(0);
    private static final AtomicInteger seePruningCount = new AtomicInteger(0);

    private OptimizedQuiescenceSearch() {
	throw new IllegalStateException("Utility class");
    }

    /**
     * Generate tactical moves with pre-filtering for better performance
     */
    private static List<Move> generateOptimizedTacticalMoves(final Board board) {
	// Use existing tactical move generation but with optimizations
	final var tacticalMoves = MoveGenerator.generateTacticalMoves(board);

	// Pre-sort captures using MoveOrdering.scoreTacticalMove for better ordering
	tacticalMoves.sort((move1, move2) -> {
	    final var score1 = MoveOrdering.scoreTacticalMove(board, move1);
	    final var score2 = MoveOrdering.scoreTacticalMove(board, move2);
	    return Integer.compare(score2, score1); // Descending order
	});

	return tacticalMoves;
    }

    /**
     * Get statistics about quiescence search performance
     */
    public static String getStatistics() {
	return "Quiescence - Nodes: %d, Delta pruning: %d, SEE pruning: %d".formatted(nodeCount.get(),
		deltaPruningCount.get(), seePruningCount.get());
    }

    /**
     * Check if move is a capture
     */
    private static boolean isCaptureMove(final Board board, final Move move) {
	return board.getPiece(move.getTo()) != Piece.NONE;
    }

    /**
     * Optimized quiescence search with multiple pruning techniques
     */
    public static int quiescenceSearch(final Board board, int alpha, final int beta, final int depth) {
	nodeCount.incrementAndGet();

	// Depth limit check
	if (depth <= -MAX_QUIESCENCE_DEPTH) {
	    return PositionEvaluator.evaluateBoard(board, board.getSideToMove());
	}

	// Stand pat (current position evaluation)
	final var standPat = PositionEvaluator.evaluateBoard(board, board.getSideToMove());

	// Beta cutoff
	if (standPat >= beta) {
	    return beta;
	}

	// Delta pruning - if even capturing the best piece can't improve alpha
	if (standPat < alpha - DELTA_PRUNING_MARGIN) {
	    deltaPruningCount.incrementAndGet();
	    // Check if capturing the most valuable piece could still help
	    final var bigDelta = standPat + BIG_DELTA_MARGIN;
	    if (bigDelta < alpha) {
		return alpha; // Futile position
	    }
	}

	// Update alpha
	if (standPat > alpha) {
	    alpha = standPat;
	}

	// Generate and filter tactical moves
	final var tacticalMoves = generateOptimizedTacticalMoves(board);

	// Search tactical moves
	for (final var move : tacticalMoves) {
	    // SEE pruning - skip moves that lose material
	    if (StaticExchangeEvaluator.calculateSEE(board, move) < SEE_PRUNE_THRESHOLD) {
		seePruningCount.incrementAndGet();
		continue;
	    }

	    // Delta pruning for captures
	    if (isCaptureMove(board, move)) {
		final var capturedPiece = board.getPiece(move.getTo());
		final var captureValue = Math.abs(MaterialEvaluator.getPieceValue(capturedPiece));

		// If even this capture can't improve alpha significantly, skip
		if (standPat + captureValue + DELTA_PRUNING_MARGIN < alpha) {
		    deltaPruningCount.incrementAndGet();
		    continue;
		}
	    }

	    // Make move and search
	    board.doMove(move);
	    final var score = -quiescenceSearch(board, -beta, -alpha, depth - 1);
	    board.undoMove();

	    if (score >= beta) {
		return beta; // Beta cutoff
	    }

	    if (score > alpha) {
		alpha = score;
	    }
	}

	return alpha;
    }

    /**
     * Enhanced quiescence search that also considers checks
     */
    public static int quiescenceSearchWithChecks(final Board board, final int alpha, final int beta, final int depth) {
	nodeCount.incrementAndGet();

	// Depth limit
	if (depth <= -MAX_QUIESCENCE_DEPTH) {
	    return PositionEvaluator.evaluateBoard(board, board.getSideToMove());
	}

	final var inCheck = board.isKingAttacked();

	// If in check, we must search all moves to escape check
	if (inCheck) {
	    return searchCheckEvasions(board, alpha, beta, depth);
	}

	// Regular quiescence search for non-check positions
	return quiescenceSearch(board, alpha, beta, depth);
    }

    /**
     * Reset node count
     */
    public static void resetNodeCount() {
	nodeCount.set(0);
    }

    /**
     * Reset all statistics counters
     */
    public static void resetStatistics() {
	nodeCount.set(0);
	deltaPruningCount.set(0);
	seePruningCount.set(0);
    }

    /**
     * Search check evasions when in check
     */
    private static int searchCheckEvasions(final Board board, int alpha, final int beta, final int depth) {
	final var legalMoves = board.legalMoves();

	if (legalMoves.isEmpty()) {
	    return -30000 + Math.abs(depth); // Checkmate, prefer mates in fewer moves
	}

	var bestScore = Integer.MIN_VALUE;

	for (final var move : legalMoves) {
	    board.doMove(move);

	    // If still in check after move, continue quiescence search
	    final int score;
	    if (board.isKingAttacked()) {
		score = -quiescenceSearchWithChecks(board, -beta, -alpha, depth - 1);
	    } else {
		score = -quiescenceSearch(board, -beta, -alpha, depth - 1);
	    }

	    board.undoMove();

	    bestScore = Math.max(bestScore, score);

	    if (score >= beta) {
		return beta;
	    }

	    alpha = Math.max(alpha, score);
	}

	return bestScore;
    }
}
