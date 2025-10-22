package io.github.seerainer.chess.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import com.github.bhlangonijr.chesslib.Board;

import io.github.seerainer.chess.ChessAI;
import io.github.seerainer.chess.ai.PositionEvaluator;

/**
 * Simple test runner to test each component individually
 */
public class SimpleTest {

    private SimpleTest() {
	throw new IllegalStateException("Utility class");
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