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
import io.github.seerainer.chess.ai.bitboard.BitboardMoveGenerator;
import io.github.seerainer.chess.ai.evaluation.IncrementalEvaluator;
import io.github.seerainer.chess.config.ChessConfig;

/**
 * Test suite to verify thread safety and performance improvements
 */
public class ChessAITest {

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
		System.out.println("Memory limit: " + ChessConfig.Memory.MAX_MEMORY_USAGE_MB + "MB");

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

		System.out.println("100 evaluations took: " + (endTime - startTime) + "ms");
		final var cacheStats = PositionEvaluator.getCacheStatistics();
		System.out.println("Cache statistics: " + cacheStats);

		assertNotNull(cacheStats, "Cache statistics should not be null");
		System.out.println("✓ Evaluation cache works");
	}

	@Test
	@DisplayName("Test incremental evaluation performance")
	@Timeout(value = 30, unit = TimeUnit.SECONDS)
	static void testIncrementalEvaluationPerformance() {
		System.out.println("\n=== Test 8: Incremental Evaluation Performance ===");

		final var board = new Board();

		// Test full evaluation
		var startTime = System.currentTimeMillis();
		for (var i = 0; i < 1000; i++) {
			PositionEvaluator.evaluateBoard(board, board.getSideToMove());
		}
		var endTime = System.currentTimeMillis();
		System.out.println("1,000 full evaluations took: " + (endTime - startTime) + "ms");

		// Test incremental evaluation
		final var incrementalEvaluator = new IncrementalEvaluator();
		incrementalEvaluator.initializeFromBoard(board);

		startTime = System.currentTimeMillis();
		for (var i = 0; i < 1000; i++) {
			incrementalEvaluator.getCurrentScore(board.getSideToMove());
		}
		endTime = System.currentTimeMillis();
		System.out.println("1,000 incremental evaluations took: " + (endTime - startTime) + "ms");

		System.out.println("✓ Incremental evaluation performance test completed");
	}

	@Test
	@DisplayName("Test move generation performance")
	@Timeout(value = 30, unit = TimeUnit.SECONDS)
	static void testMoveGenerationPerformance() {
		System.out.println("\n=== Test 7: Move Generation Performance ===");

		final var board = new Board();

		// Test standard move generation
		var startTime = System.currentTimeMillis();
		for (var i = 0; i < 1000; i++) {
			board.legalMoves();
		}
		var endTime = System.currentTimeMillis();
		System.out.println("1,000 standard move generations took: " + (endTime - startTime) + "ms");

		// Test bitboard move generation
		startTime = System.currentTimeMillis();
		for (var i = 0; i < 1000; i++) {
			BitboardMoveGenerator.generateMoves(board);
		}
		endTime = System.currentTimeMillis();
		System.out.println("1,000 bitboard move generations took: " + (endTime - startTime) + "ms");

		System.out.println("✓ Move generation performance test completed");
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
					System.err.println("Thread " + threadIndex + " failed: " + e.getMessage());
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
			assertTrue(results[i], "Thread " + i + " should have succeeded");
		}

		System.out.println("✓ Thread safety test passed");
	}
}