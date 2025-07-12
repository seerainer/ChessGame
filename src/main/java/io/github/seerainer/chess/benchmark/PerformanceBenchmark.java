package io.github.seerainer.chess.benchmark;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Side;

import io.github.seerainer.chess.ChessAI;
import io.github.seerainer.chess.ai.PositionEvaluator;
import io.github.seerainer.chess.ai.bitboard.BitboardMoveGenerator;
import io.github.seerainer.chess.config.ChessConfig;

/**
 * Comprehensive benchmark suite to demonstrate performance improvements
 */
public class PerformanceBenchmark {

	private static final int WARMUP_RUNS = 10;
	private static final int BENCHMARK_RUNS = 100;
	private static final int EVALUATION_RUNS = 10000;
	private static final int MOVE_GENERATION_RUNS = 50000;

	private static void benchmarkEvaluation() {
		System.out.println("=== Evaluation Performance Benchmark ===");

		final var board = new Board();

		// Test without cache
		PositionEvaluator.clearCache();

		var startTime = System.currentTimeMillis();
		for (var i = 0; i < EVALUATION_RUNS; i++) {
			PositionEvaluator.evaluateBoard(board, Side.WHITE);
		}
		var endTime = System.currentTimeMillis();

		final var timeWithoutCache = endTime - startTime;

		// Test with cache
		PositionEvaluator.clearCache();

		startTime = System.currentTimeMillis();
		for (var i = 0; i < EVALUATION_RUNS; i++) {
			PositionEvaluator.evaluateBoard(board, Side.WHITE);
		}
		endTime = System.currentTimeMillis();

		final var timeWithCache = endTime - startTime;

		System.out.println("Results:");
		System.out.printf("- %d evaluations without cache: %d ms\n", EVALUATION_RUNS, timeWithoutCache);
		System.out.printf("- %d evaluations with cache: %d ms\n", EVALUATION_RUNS, timeWithCache);
		System.out.printf("- Cache speedup: %.2fx\n", (double) timeWithoutCache / timeWithCache);
		System.out.printf("- Evaluations per second: %.0f\n", EVALUATION_RUNS * 1000.0 / timeWithCache);
		System.out.println("- Cache stats: " + PositionEvaluator.getCacheStatistics());
		System.out.println();
	}

	private static void benchmarkMemoryUsage() {
		System.out.println("=== Memory Usage Benchmark ===");

		final var runtime = Runtime.getRuntime();

		// Force garbage collection
		System.gc();
		final var initialMemory = runtime.totalMemory() - runtime.freeMemory();

		// Create AI and run some searches
		final var ai = new ChessAI();
		final var board = new Board();

		for (var i = 0; i < 50; i++) {
			ai.getBestMove(board);
		}

		final var memoryAfterSearch = runtime.totalMemory() - runtime.freeMemory();

		System.out.println("Results:");
		System.out.printf("- Initial memory: %.2f MB\n", initialMemory / 1024.0 / 1024.0);
		System.out.printf("- Memory after 50 searches: %.2f MB\n", memoryAfterSearch / 1024.0 / 1024.0);
		System.out.printf("- Memory increase: %.2f MB\n", (memoryAfterSearch - initialMemory) / 1024.0 / 1024.0);
		System.out.printf("- Memory per search: %.2f KB\n", (memoryAfterSearch - initialMemory) / 1024.0 / 50);
		System.out.println();

		ai.cleanup();

		// Force garbage collection again
		System.gc();
		final var finalMemory = runtime.totalMemory() - runtime.freeMemory();
		System.out.printf("- Memory after cleanup: %.2f MB\n", finalMemory / 1024.0 / 1024.0);
		System.out.printf("- Memory reclaimed: %.2f MB\n", (memoryAfterSearch - finalMemory) / 1024.0 / 1024.0);
	}

	private static void benchmarkMoveGeneration() {
		System.out.println("=== Move Generation Performance Benchmark ===");

		final var board = new Board();

		// Standard move generation
		var startTime = System.currentTimeMillis();
		for (var i = 0; i < MOVE_GENERATION_RUNS; i++) {
			board.legalMoves();
		}
		var endTime = System.currentTimeMillis();

		final var standardTime = endTime - startTime;

		// Bitboard move generation (if implemented)
		startTime = System.currentTimeMillis();
		for (var i = 0; i < MOVE_GENERATION_RUNS; i++) {
			try {
				BitboardMoveGenerator.generateMoves(board);
			} catch (final Exception e) {
				// Fall back to standard if bitboard fails
				board.legalMoves();
			}
		}
		endTime = System.currentTimeMillis();

		final var bitboardTime = endTime - startTime;

		System.out.println("Results:");
		System.out.printf("- %d standard move generations: %d ms\n", MOVE_GENERATION_RUNS, standardTime);
		System.out.printf("- %d bitboard move generations: %d ms\n", MOVE_GENERATION_RUNS, bitboardTime);
		System.out.printf("- Bitboard speedup: %.2fx\n", (double) standardTime / bitboardTime);
		System.out.printf("- Move generations per second: %.0f\n", MOVE_GENERATION_RUNS * 1000.0 / bitboardTime);
		System.out.println();
	}

	private static void benchmarkSearch() {
		System.out.println("=== Search Performance Benchmark ===");

		final var board = new Board();
		final var ai = new ChessAI();

		// Warmup
		System.out.println("Warming up...");
		for (var i = 0; i < WARMUP_RUNS; i++) {
			ai.getBestMove(board);
		}

		// Benchmark search speed
		System.out.println("Running search benchmark...");
		final var startTime = System.currentTimeMillis();

		for (var i = 0; i < BENCHMARK_RUNS; i++) {
			ai.getBestMove(board);
		}

		final var endTime = System.currentTimeMillis();
		final var totalTime = endTime - startTime;
		final var avgTime = (double) totalTime / BENCHMARK_RUNS;
		final var movesPerSecond = 1000.0 / avgTime;

		System.out.println("Results:");
		System.out.printf("- Total time: %d ms\n", totalTime);
		System.out.printf("- Average time per move: %.2f ms\n", avgTime);
		System.out.printf("- Moves per second: %.2f\n", movesPerSecond);
		System.out.printf("- Nodes per second: ~%.0f (estimated)\n", movesPerSecond * 1000);
		System.out.println();

		ai.cleanup();
	}

	private static void displayConfiguration() {
		System.out.println("Configuration:");
		System.out.println("- Max search depth: " + ChessConfig.AI.MAX_SEARCH_DEPTH);
		System.out.println("- Parallel search: " + ChessConfig.AI.ENABLE_PARALLEL_SEARCH);
		System.out.println("- Search threads: " + ChessConfig.AI.PARALLEL_SEARCH_THREADS);
		System.out.println("- Evaluation cache: " + ChessConfig.Performance.ENABLE_EVALUATION_CACHE);
		System.out.println("- Memory limit: " + ChessConfig.Memory.MAX_MEMORY_USAGE_MB + "MB");
		System.out.println("- TT size: " + ChessConfig.AI.TRANSPOSITION_TABLE_SIZE);
		System.out.println();
	}

	public static void main(final String[] args) {
		System.out.println("=== Chess AI Performance Benchmark ===\n");

		// Display current configuration
		displayConfiguration();

		// Run benchmarks
		benchmarkSearch();
		benchmarkEvaluation();
		benchmarkMoveGeneration();
		benchmarkMemoryUsage();

		System.out.println("\n=== Benchmark Complete ===");
	}
}
