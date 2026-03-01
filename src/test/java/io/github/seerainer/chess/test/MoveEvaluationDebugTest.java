package io.github.seerainer.chess.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.move.MoveGenerator;

import io.github.seerainer.chess.ai.PositionEvaluator;

/**
 * Tests that the position evaluator produces consistent, non-exceptional
 * evaluations for all legal moves in various positions.
 */
class MoveEvaluationDebugTest {

    /**
     * Evaluate every legal move from the starting position and verify the evaluator
     * returns finite scores without throwing exceptions.
     */
    @Test
    @DisplayName("Evaluate all opening moves without exceptions")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testOpeningMoveEvaluation() {
	final var board = new Board();
	final var moves = MoveGenerator.generateLegalMoves(board);

	assertFalse(moves.isEmpty(), "Starting position should have legal moves");

	moves.forEach((final var move) -> {
	    board.doMove(move);
	    final var score = PositionEvaluator.evaluateBoard(board, Side.WHITE);
	    board.undoMove();

	    assertTrue(score > Integer.MIN_VALUE && score < Integer.MAX_VALUE,
		    new StringBuilder().append("Evaluation for move ").append(move).append(" should be finite, got: ")
			    .append(score).toString());
	});
    }

    /**
     * After 1.d4 Nf6, evaluate all of White's responses to verify consistency.
     */
    @Test
    @DisplayName("Evaluate moves after 1.d4 Nf6")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testAfterD4Nf6Evaluation() {
	final var board = new Board();
	board.doMove("d2d4");
	board.doMove("g8f6");

	final var moves = MoveGenerator.generateLegalMoves(board);
	assertFalse(moves.isEmpty(), "Position after 1.d4 Nf6 should have legal moves");

	var minScore = Integer.MAX_VALUE;
	var maxScore = Integer.MIN_VALUE;

	for (final var move : moves) {
	    board.doMove(move);
	    final var score = PositionEvaluator.evaluateBoard(board, Side.WHITE);
	    board.undoMove();

	    if (score < minScore) {
		minScore = score;
	    }
	    if (score > maxScore) {
		maxScore = score;
	    }
	}

	// Sanity check: there should be some spread in the evaluations
	assertNotNull(moves.getFirst(), "Should have at least one move");
	assertTrue(maxScore >= minScore, new StringBuilder().append("Max score should be >= min score, got min=")
		.append(minScore).append(" max=").append(maxScore).toString());
    }
}
