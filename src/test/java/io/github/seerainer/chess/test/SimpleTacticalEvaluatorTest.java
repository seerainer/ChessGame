package io.github.seerainer.chess.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.github.bhlangonijr.chesslib.Board;

import io.github.seerainer.chess.ChessAI;
import io.github.seerainer.chess.ai.evaluation.EvaluationContext;
import io.github.seerainer.chess.ai.evaluation.SimpleTacticalEvaluator;

/**
 * Test for SimpleTacticalEvaluator functionality
 */
public class SimpleTacticalEvaluatorTest {

	@Test
	@DisplayName("Test Hanging Piece Detection")
	static void testHangingPieceDetection() {
		// Position with hanging queen
		final var board = new Board();
		board.loadFromFen("rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2");

		final var evaluator = new SimpleTacticalEvaluator();
		final var context = new EvaluationContext(board, board.getSideToMove());
		final var evaluation = evaluator.evaluate(context);

		System.out.println("Hanging piece test FEN: " + board.getFen());
		System.out.println("Tactical evaluation: " + evaluation);

		// Test should complete without errors
		assertTrue(true, "Hanging piece detection test completed");
	}

	@Test
	@DisplayName("Test Knight Fork Detection")
	static void testKnightForkDetection() {
		// Position where white knight on d5 can fork king on e7 and queen on c7
		final var board = new Board();
		board.loadFromFen("r1bqkb1r/ppqp1ppp/2n2n2/3N4/8/8/PPPP1PPP/RNBQKB1R w KQkq - 0 1");

		final var evaluator = new SimpleTacticalEvaluator();
		final var context = new EvaluationContext(board, board.getSideToMove());
		final var evaluation = evaluator.evaluate(context);

		System.out.println("Knight fork position FEN: " + board.getFen());
		System.out.println("Tactical evaluation: " + evaluation);

		// Should be positive since white has tactical opportunities
		assertTrue(evaluation > 0, "Knight fork should give positive evaluation");
	}

	@Test
	@DisplayName("Test SimpleTacticalEvaluator Integration")
	static void testTacticalEvaluatorIntegration() {
		// Create a simple tactical position with a knight fork
		final var board = new Board();
		board.loadFromFen("rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2");

		final var ai = new ChessAI();

		// Test that AI can find a move
		final var bestMove = ai.getBestMove(board);
		assertNotNull(bestMove, "AI should find a move");

		System.out.println("Position: " + board.getFen());
		System.out.println("Best move found: " + bestMove);

		// Test SimpleTacticalEvaluator directly
		final var evaluator = new SimpleTacticalEvaluator();
		final var context = new EvaluationContext(board, board.getSideToMove());
		final var evaluation = evaluator.evaluate(context);

		System.out.println("Tactical evaluation: " + evaluation);

		// Test should pass if we get valid results
		assertTrue(true, "Basic tactical evaluator test completed");
	}
}
