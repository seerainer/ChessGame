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

import io.github.seerainer.chess.ChessAI;
import io.github.seerainer.chess.ai.evaluation.EvaluationOrchestrator;

/**
 * Tests the EvaluationOrchestrator's ability to evaluate all legal moves in a
 * position, and verifies that the AI selects a valid move.
 */
class DebugMoveSelection {

    /**
     * After 1.Nf3 a5, evaluate all legal moves with EvaluationOrchestrator and
     * verify the AI selects a non-null move.
     */
    @Test
    @DisplayName("EvaluationOrchestrator evaluates all moves after 1.Nf3 a5")
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void testMoveSelectionAfterNf3A5() {
	final var board = new Board();
	board.doMove("g1f3");
	board.doMove("a7a5");

	final var legalMoves = board.legalMoves();
	assertFalse(legalMoves.isEmpty(), "Position should have legal moves");

	final var evaluator = new EvaluationOrchestrator();

	// Evaluate each move and verify scores are finite
	legalMoves.forEach((final var move) -> {
	    board.doMove(move);
	    final var score = evaluator.evaluatePosition(board, Side.WHITE);
	    board.undoMove();

	    assertTrue(score > Integer.MIN_VALUE && score < Integer.MAX_VALUE, new StringBuilder().append("Score for ")
		    .append(move).append(" should be finite, got: ").append(score).toString());
	});

	// Verify AI selects a valid move
	final var ai = new ChessAI();
	try {
	    final var selectedMove = ai.getBestMove(board);
	    assertNotNull(selectedMove, "AI should select a move");

	    // Verify the selected move is among the legal moves
	    assertTrue(legalMoves.contains(selectedMove), new StringBuilder().append("AI's selected move ")
		    .append(selectedMove).append(" should be a legal move").toString());
	} finally {
	    ai.cleanup();
	}

	// Verify evaluation breakdown does not throw
	final var breakdown = evaluator.getEvaluationBreakdown(board, Side.WHITE);
	assertNotNull(breakdown, "Evaluation breakdown should not be null");
	assertFalse(breakdown.isEmpty(), "Evaluation breakdown should not be empty");
    }
}
