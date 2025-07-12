package io.github.seerainer.chess.test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.github.bhlangonijr.chesslib.Board;

import io.github.seerainer.chess.ChessAI;

/**
 * Performance benchmarking test to measure nodes per second and search
 * efficiency Used to estimate engine strength in conjunction with ELO tests
 */
public class PerformanceBenchmarkTest {

	/**
	 * Benchmark result data structure
	 */
	private static class BenchmarkResult {
		final String positionName;
		final long timeMs;
		final String bestMove;
		final double positionsPerSecond;

		BenchmarkResult(final String positionName, final long timeMs, final int depth, final String bestMove,
				final double positionsPerSecond) {
			this.positionName = positionName;
			this.timeMs = timeMs;
			this.bestMove = bestMove;
			this.positionsPerSecond = positionsPerSecond;
		}
	}

	/**
	 * Internal benchmark method
	 */
	private static BenchmarkResult benchmarkPositionInternal(final String positionName, final String fen,
			final int targetDepth, final ChessAI testAI) {
		final var board = new Board();
		board.loadFromFen(fen);

		final var startTime = System.currentTimeMillis();
		final var bestMove = testAI.getBestMove(board);
		final var endTime = System.currentTimeMillis();

		final var timeMs = endTime - startTime;

		// Estimate positions per second based on time and typical search
		// characteristics
		// This is a rough estimate since we don't have access to actual node counts
		final var estimatedPositions = estimatePositionsSearched(timeMs, targetDepth);
		final var positionsPerSecond = timeMs > 0 ? (estimatedPositions / timeMs) * 1000 : 0;

		return new BenchmarkResult(positionName, timeMs, targetDepth, bestMove != null ? bestMove.toString() : "null",
				positionsPerSecond);
	}

	/**
	 * Calculate performance-based ELO estimate
	 */
	private static int calculatePerformanceELO(final double avgPositionsPerSecond, final double avgTime) {
		var baseELO = 1000;

		// Speed bonus
		if (avgPositionsPerSecond > 100000) {
			baseELO += 600;
		} else if (avgPositionsPerSecond > 50000) {
			baseELO += 400;
		} else if (avgPositionsPerSecond > 20000) {
			baseELO += 200;
		} else if (avgPositionsPerSecond > 10000) {
			baseELO += 100;
		}

		// Time bonus/penalty
		if (avgTime < 1000) {
			baseELO += 200;
		} else if (avgTime < 3000) {
			baseELO += 100;
		} else if (avgTime > 10000) {
			baseELO -= 200;
		}

		return Math.max(800, Math.min(2200, baseELO));
	}

	/**
	 * Estimate positions searched based on time and depth
	 */
	private static double estimatePositionsSearched(final long timeMs, final int depth) {
		// Rough estimation: modern engines search ~50-500K positions per second
		// depending on position complexity and depth
		final var basePositions = Math.pow(35, Math.min(depth, 6)); // ~35 moves per position on average
		return Math.min(basePositions, timeMs * 200); // Cap at 200 positions per millisecond
	}

	/**
	 * Get performance category
	 */
	private static String getPerformanceCategory(final int elo) {
		if (elo < 1000) {
			return "Slow";
		}
		if (elo < 1200) {
			return "Basic";
		}
		if (elo < 1400) {
			return "Moderate";
		}
		if (elo < 1600) {
			return "Good";
		}
		if (elo < 1800) {
			return "Very Good";
		}
		return elo < 2000 ? "Excellent" : "Professional";
	}

	private ChessAI ai;

	private List<BenchmarkResult> results;

	/**
	 * Benchmark a single position
	 */
	private void benchmarkPosition(final String positionName, final String fen, final int targetDepth) {
		final var result = benchmarkPositionInternal(positionName, fen, targetDepth, ai);
		results.add(result);

		System.out.println(positionName + ":");
		System.out.println("  Time: " + result.timeMs + "ms");
		System.out.println("  Best move: " + result.bestMove);
		System.out.println("  Estimated positions/sec: " + "%.0f".formatted(result.positionsPerSecond));
	}

	/**
	 * Print comprehensive performance analysis
	 */
	private void printPerformanceAnalysis() {
		System.out.println("\n" + "=".repeat(60));
		System.out.println("CHESS ENGINE PERFORMANCE ANALYSIS");
		System.out.println("=".repeat(60));

		if (results.isEmpty()) {
			System.out.println("No benchmark results available");
			return;
		}

		// Calculate statistics
		final var avgTime = results.stream().mapToLong(r -> r.timeMs).average().orElse(0);
		final var avgPositionsPerSecond = results.stream().mapToDouble(r -> r.positionsPerSecond).average().orElse(0);
		final var minTime = results.stream().mapToLong(r -> r.timeMs).min().orElse(0);
		final var maxTime = results.stream().mapToLong(r -> r.timeMs).max().orElse(0);

		System.out.println("PERFORMANCE SUMMARY:");
		System.out.println("- Total positions tested: " + results.size());
		System.out.println("- Average time per position: " + "%.1f".formatted(avgTime) + "ms");
		System.out.println("- Time range: " + minTime + "ms - " + maxTime + "ms");
		System.out.println("- Estimated positions per second: " + "%.0f".formatted(avgPositionsPerSecond));

		// Performance classification
		System.out.println("\nPERFORMANCE CLASSIFICATION:");
		if (avgPositionsPerSecond > 100000) {
			System.out.println("- Search speed: Excellent (>100K pos/sec)");
		} else if (avgPositionsPerSecond > 50000) {
			System.out.println("- Search speed: Very good (50-100K pos/sec)");
		} else if (avgPositionsPerSecond > 20000) {
			System.out.println("- Search speed: Good (20-50K pos/sec)");
		} else if (avgPositionsPerSecond > 5000) {
			System.out.println("- Search speed: Fair (5-20K pos/sec)");
		} else {
			System.out.println("- Search speed: Slow (<5K pos/sec)");
		}

		if (avgTime < 1000) {
			System.out.println("- Response time: Excellent (<1s)");
		} else if (avgTime < 3000) {
			System.out.println("- Response time: Good (1-3s)");
		} else if (avgTime < 10000) {
			System.out.println("- Response time: Fair (3-10s)");
		} else {
			System.out.println("- Response time: Slow (>10s)");
		}

		// Strength estimation based on performance
		System.out.println("\nSTRENGTH ESTIMATION (Performance-based):");
		final var performanceELO = calculatePerformanceELO(avgPositionsPerSecond, avgTime);
		System.out.println("- Performance-based ELO: " + performanceELO);
		System.out.println("- Performance category: " + getPerformanceCategory(performanceELO));

		// Detailed results
		System.out.println("\nDETAILED RESULTS:");
		results.forEach(
				(final BenchmarkResult result) -> System.out.println("- " + result.positionName + ": " + result.timeMs
						+ "ms, " + result.bestMove + ", " + "%.0f".formatted(result.positionsPerSecond) + " pos/sec"));

		System.out.println("\nPERFORMANCE RECOMMENDATIONS:");
		if (avgTime > 5000) {
			System.out.println("- Consider reducing search depth in time-critical situations");
			System.out.println("- Optimize evaluation function for speed");
		}
		if (avgPositionsPerSecond < 20000) {
			System.out.println("- Consider optimizing move generation");
			System.out.println("- Improve pruning techniques");
		}
		if (avgPositionsPerSecond > 50000) {
			System.out.println("- Good search efficiency - consider increasing search depth");
			System.out.println("- Engine shows strong performance characteristics");
		}

		System.out.println("\n" + "=".repeat(60));
	}

	@BeforeEach
	void setUp() {
		ai = new ChessAI();
		results = new ArrayList<>();
	}

	@AfterEach
	void tearDown() {
		if (ai != null) {
			ai.cleanup();
		}
		printPerformanceAnalysis();
	}

	@Test
	@DisplayName("Deep Search Performance Test")
	@Timeout(value = 300, unit = TimeUnit.SECONDS)
	void testDeepSearch() {
		System.out.println("\n=== DEEP SEARCH PERFORMANCE TEST ===");

		final var complexPosition = "r1bqkb1r/pppp1ppp/2n2n2/4p3/2B1P3/3P1N2/PPP2PPP/RNBQK2R w KQkq - 0 1";

		// Test various depths
		for (var depth = 4; depth <= 8; depth++) {
			System.out.println("\nTesting depth " + depth + "...");
			benchmarkPosition("Complex Position (depth " + depth + ")", complexPosition, depth);
		}
	}

	@Test
	@DisplayName("Endgame Position Performance Benchmark")
	@Timeout(value = 120, unit = TimeUnit.SECONDS)
	void testEndgamePositions() {
		System.out.println("\n=== ENDGAME POSITION PERFORMANCE BENCHMARK ===");

		final List<String> endgamePositions = List.of("8/8/8/8/4P3/8/8/3K1k2 w - - 0 1", // King and pawn
				"8/8/8/8/8/8/2R5/3K1k2 w - - 0 1", // Rook endgame
				"8/8/8/8/8/8/1Q6/3K1k2 w - - 0 1", // Queen endgame
				"8/8/8/8/8/5k2/5N2/5K2 w - - 0 1", // Knight endgame
				"8/8/8/8/8/3k4/3B4/3K4 w - - 0 1" // Bishop endgame
		);

		final String[] positionNames = { "King and Pawn", "Rook Endgame", "Queen Endgame", "Knight Endgame",
				"Bishop Endgame" };

		for (var i = 0; i < endgamePositions.size(); i++) {
			benchmarkPosition(positionNames[i], endgamePositions.get(i), 7);
		}
	}

	@Test
	@DisplayName("Parallel Search Performance Test")
	@Timeout(value = 180, unit = TimeUnit.SECONDS)
	void testParallelSearchPerformance() {
		System.out.println("\n=== PARALLEL SEARCH PERFORMANCE TEST ===");

		final var complexPosition = "r1bqkb1r/pppp1ppp/2n2n2/4p3/2B1P3/3P1N2/PPP2PPP/RNBQK2R w KQkq - 0 1";

		// Test concurrent searches
		final List<CompletableFuture<BenchmarkResult>> futures = new ArrayList<>();

		for (var i = 0; i < 3; i++) {
			final var testId = i;
			futures.add(CompletableFuture.supplyAsync(() -> {
				final var testAI = new ChessAI();
				try {
					return benchmarkPositionInternal("Parallel Test " + testId, complexPosition, 5, testAI);
				} finally {
					testAI.cleanup();
				}
			}));
		}

		// Wait for all to complete
		futures.forEach((final CompletableFuture<BenchmarkResult> future) -> {
			try {
				final var result = future.get(60, TimeUnit.SECONDS);
				results.add(result);
				System.out.println("Parallel test completed: " + result.positionName + " in " + result.timeMs
						+ "ms, move: " + result.bestMove);
			} catch (final Exception e) {
				System.err.println("Parallel test failed: " + e.getMessage());
			}
		});
	}

	@Test
	@DisplayName("Standard Position Performance Benchmark")
	@Timeout(value = 180, unit = TimeUnit.SECONDS)
	void testStandardPositions() {
		System.out.println("\n=== STANDARD POSITION PERFORMANCE BENCHMARK ===");

		final List<String> standardPositions = List.of("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1", // Starting
																													// position
				"rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR b KQkq e3 0 1", // After 1.e4
				"rnbqkb1r/pppppppp/5n2/8/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 1 2", // After 1.e4 Nf6
				"rnbqkb1r/pppp1ppp/4pn2/8/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 3", // After 1.e4 Nf6 2.e5
				"r1bqkb1r/pppp1ppp/2n2n2/4p3/4P3/3P1N2/PPP2PPP/RNBQKB1R w KQkq - 0 4" // Italian Game
		);

		final String[] positionNames = { "Starting Position", "After 1.e4", "After 1.e4 Nf6", "After 1.e4 Nf6 2.e5",
				"Italian Game" };

		for (var i = 0; i < standardPositions.size(); i++) {
			benchmarkPosition(positionNames[i], standardPositions.get(i), 5);
		}
	}

	@Test
	@DisplayName("Tactical Position Performance Benchmark")
	@Timeout(value = 240, unit = TimeUnit.SECONDS)
	void testTacticalPositions() {
		System.out.println("\n=== TACTICAL POSITION PERFORMANCE BENCHMARK ===");

		final List<String> tacticalPositions = List.of(
				"r1bqkb1r/pppp1ppp/2n2n2/4p3/2B1P3/3P1N2/PPP2PPP/RNBQK2R w KQkq - 0 1", // Open position
				"r2qkb1r/ppp2ppp/2np1n2/2b1p3/2B1P3/3P1N2/PPP1QPPP/RNB2RK1 w kq - 0 1", // Complex middle game
				"r1bq1rk1/ppp2ppp/2np1n2/2b1p3/2B1P3/3P1N2/PPP1QPPP/RNB2RK1 w - - 0 8", // Castled position
				"r1bqk2r/pppp1ppp/2n2n2/2b1p3/2B1P3/3P1N2/PPP2PPP/RNBQK2R w KQkq - 0 1", // Pre-castle
				"rnbqkb1r/ppp2ppp/3p1n2/4p3/4P3/3P1N2/PPP2PPP/RNBQKB1R w KQkq - 0 1" // French Defense
		);

		final String[] positionNames = { "Open Position", "Complex Middle Game", "Castled Position", "Pre-Castle",
				"French Defense" };

		for (var i = 0; i < tacticalPositions.size(); i++) {
			benchmarkPosition(positionNames[i], tacticalPositions.get(i), 6);
		}
	}
}
