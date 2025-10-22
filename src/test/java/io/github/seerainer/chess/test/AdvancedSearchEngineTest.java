package io.github.seerainer.chess.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.github.bhlangonijr.chesslib.Board;

import io.github.seerainer.chess.ChessAI;

/**
 * Test to verify that advanced search engines are being used in the application
 */
public class AdvancedSearchEngineTest {

    private AdvancedSearchEngineTest() {
	throw new IllegalStateException("Utility class");
    }

    @Test
    @DisplayName("Test complex tactical position")
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    static void testComplexPosition() {
	System.out.println("\n=== Test 3: Complex Tactical Position ===");

	// Tactical position with many captures possible
	final var board = new Board();
	board.loadFromFen("r1bq1rk1/ppp2ppp/2np1n2/2b1p3/2B1P3/3P1N2/PPP1QPPP/RNB2RK1 w - - 0 8");

	final var ai = new ChessAI();

	try {
	    final var move = ai.getBestMove(board);
	    assertNotNull(move, "Tactical search should find a move");
	    System.out.println("Tactical search found move: " + move);
	    System.out.println("✓ Complex position test completed");
	} finally {
	    ai.cleanup();
	}
    }

    @Test
    @DisplayName("Test deep search")
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    static void testDeepSearch() {
	System.out.println("\n=== Test 2: Deep Search (complex position) ===");

	// Set up a more complex position
	final var board = new Board();
	board.loadFromFen("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1");

	final var ai = new ChessAI();

	try {
	    final var move = ai.getBestMove(board);
	    assertNotNull(move, "Deep search should find a move");
	    System.out.println("Deep search found move: " + move);
	    System.out.println("✓ Deep search test completed");
	} finally {
	    ai.cleanup();
	}
    }

    @Test
    @DisplayName("Test parallel search capability")
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    static void testParallelSearch() {
	System.out.println("\n=== Test 4: Parallel Search ===");

	final var board = new Board();
	final var ai = new ChessAI();

	try {
	    // Test that parallel search doesn't crash
	    final var move = ai.getBestMove(board);
	    assertNotNull(move, "Parallel search should find a move");
	    System.out.println("Parallel search found move: " + move);
	    System.out.println("✓ Parallel search test completed");
	} finally {
	    ai.cleanup();
	}
    }

    @Test
    @DisplayName("Test shallow search")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    static void testShallowSearch() {
	System.out.println("\n=== Test 1: Shallow Search ===");

	final var board = new Board();
	final var ai = new ChessAI();

	try {
	    final var move = ai.getBestMove(board);
	    assertNotNull(move, "Shallow search should find a move");
	    System.out.println("Shallow search found move: " + move);
	    System.out.println("✓ Shallow search test completed");
	} finally {
	    ai.cleanup();
	}
    }
}