package io.github.seerainer.chess.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Side;

import io.github.seerainer.chess.ChessAI;
import io.github.seerainer.chess.ai.PositionEvaluator;
import io.github.seerainer.chess.config.ChessConfig;

/**
 * Test suite to verify thread safety and performance improvements
 */
public class ChessAITest {

    private ChessAITest() {
	throw new IllegalStateException("Utility class");
    }

    @Test
    @DisplayName("Test basic functionality")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    static void testBasicFunctionality() {
	System.out.println("\n=== Test 1: Basic Functionality ===");

	final var ai = new ChessAI();
	final var board = new Board();

	try {
	    final var move = ai.getBestMove(board);
	    assertNotNull(move, "AI should find a move");
	    System.out.println("AI found move: " + move);
	    System.out.println("✓ Basic functionality works");
	} finally {
	    ai.cleanup();
	}
    }

    @Test
    @DisplayName("Test configuration")
    static void testConfiguration() {
	System.out.println("\n=== Test 3: Configuration ===");

	System.out.println("Max search depth: " + ChessConfig.AI.MAX_SEARCH_DEPTH);
	System.out.println("Parallel search enabled: " + ChessConfig.AI.ENABLE_PARALLEL_SEARCH);
	System.out.println("Parallel search threads: " + ChessConfig.AI.PARALLEL_SEARCH_THREADS);
	System.out.println("Evaluation cache enabled: " + ChessConfig.Performance.ENABLE_EVALUATION_CACHE);
	System.out.println(new StringBuilder().append("Memory limit: ").append(ChessConfig.Memory.MAX_MEMORY_USAGE_MB)
		.append("MB").toString());

	// Basic assertions to ensure configuration is accessible
	assertTrue(ChessConfig.AI.MAX_SEARCH_DEPTH > 0, "Max search depth should be positive");
	assertTrue(ChessConfig.AI.PARALLEL_SEARCH_THREADS > 0, "Parallel search threads should be positive");
	assertTrue(ChessConfig.Memory.MAX_MEMORY_USAGE_MB > 0, "Memory limit should be positive");

	System.out.println("✓ Configuration accessible");
    }

    @Test
    @DisplayName("Test evaluation cache")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    static void testEvaluationCache() {
	System.out.println("\n=== Test 4: Evaluation Cache ===");

	final var board = new Board();

	// Clear cache first
	PositionEvaluator.clearCache();

	// Evaluate position multiple times
	final var startTime = System.currentTimeMillis();
	for (var i = 0; i < 100; i++) {
	    PositionEvaluator.evaluateBoard(board, Side.WHITE);
	}
	final var endTime = System.currentTimeMillis();

	System.out.println(new StringBuilder().append("100 evaluations took: ").append(endTime - startTime).append("ms")
		.toString());
	final var cacheStats = PositionEvaluator.getCacheStatistics();
	System.out.println("Cache statistics: " + cacheStats);

	assertNotNull(cacheStats, "Cache statistics should not be null");
	System.out.println("✓ Evaluation cache works");
    }

    @Test
    @DisplayName("Test thread safety")
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    static void testThreadSafety() {
	System.out.println("\n=== Test 2: Thread Safety ===");

	final var board = new Board();
	final var threadCount = 4;
	final var threads = new Thread[threadCount];
	final var results = new boolean[threadCount];

	// Create multiple threads that use the AI simultaneously
	for (var i = 0; i < threadCount; i++) {
	    final var threadIndex = i;
	    threads[i] = new Thread(() -> {
		try {
		    final var ai = new ChessAI();
		    final var move = ai.getBestMove(board);
		    results[threadIndex] = (move != null);
		    ai.cleanup();
		} catch (final Exception e) {
		    results[threadIndex] = false;
		    System.err.println(new StringBuilder().append("Thread ").append(threadIndex).append(" failed: ")
			    .append(e.getMessage()).toString());
		}
	    });
	}

	// Start all threads
	for (final var thread : threads) {
	    thread.start();
	}

	// Wait for all threads to complete
	for (final var thread : threads) {
	    try {
		thread.join(30000); // 30 second timeout
	    } catch (final InterruptedException e) {
		Thread.currentThread().interrupt();
		fail("Thread was interrupted");
	    }
	}

	// Check results
	for (var i = 0; i < threadCount; i++) {
	    assertTrue(results[i],
		    new StringBuilder().append("Thread ").append(i).append(" should have succeeded").toString());
	}

	System.out.println("✓ Thread safety test passed");
    }
}