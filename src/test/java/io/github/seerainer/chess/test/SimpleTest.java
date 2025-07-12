package io.github.seerainer.chess.test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.github.bhlangonijr.chesslib.Board;

import io.github.seerainer.chess.ChessAI;
import io.github.seerainer.chess.ai.PositionEvaluator;
import io.github.seerainer.chess.ai.bitboard.BitboardMoveGenerator;
import io.github.seerainer.chess.ai.evaluation.IncrementalEvaluator;

/**
 * Simple test runner to test each component individually
 */
public class SimpleTest {

	@Test
	@DisplayName("Test BitboardMoveGenerator")
	static void testBitboardMoveGenerator() {
		System.out.println("Testing BitboardMoveGenerator...");
		final var board = new Board();
		final var moves = BitboardMoveGenerator.generateMoves(board);

		assertNotNull(moves, "Generated moves should not be null");
		assertFalse(moves.isEmpty(), "Generated moves should not be empty");

		System.out.println("Generated " + moves.size() + " moves using bitboard generator");
		System.out.println("✓ BitboardMoveGenerator works");
	}

	@Test
	@DisplayName("Test ChessAI basic function")
	static void testChessAIBasicFunction() {
		System.out.println("Testing ChessAI basic function...");
		final var ai = new ChessAI();
		final var board = new Board();
		final var move = ai.getBestMove(board);

		assertNotNull(move, "AI should find a move");

		System.out.println("AI found move: " + move);
		ai.cleanup();
		System.out.println("✓ ChessAI basic function works");
	}

	@Test
	@DisplayName("Test IncrementalEvaluator")
	static void testIncrementalEvaluator() {
		System.out.println("Testing IncrementalEvaluator...");
		final var board = new Board();
		final var evaluator = new IncrementalEvaluator();
		evaluator.initializeFromBoard(board);
		final var score = evaluator.getCurrentScore(board.getSideToMove());

		assertNotNull(score, "Evaluation score should not be null");

		System.out.println("Incremental evaluation score: " + score);
		System.out.println("✓ IncrementalEvaluator works");
	}

	@Test
	@DisplayName("Test PositionEvaluator cache")
	static void testPositionEvaluatorCache() {
		System.out.println("Testing PositionEvaluator cache...");
		final var board = new Board();
		PositionEvaluator.clearCache();
		for (var i = 0; i < 10; i++) {
			PositionEvaluator.evaluateBoard(board, board.getSideToMove());
		}

		final var cacheStats = PositionEvaluator.getCacheStatistics();
		assertNotNull(cacheStats, "Cache statistics should not be null");

		System.out.println("Cache stats: " + cacheStats);
		System.out.println("✓ PositionEvaluator cache works");
	}
}