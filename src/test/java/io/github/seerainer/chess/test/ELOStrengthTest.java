package io.github.seerainer.chess.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.github.bhlangonijr.chesslib.Board;

import io.github.seerainer.chess.ChessAI;

/**
 * Comprehensive test suite to estimate the ELO strength of the chess engine
 * Based on standardized test positions and tactical puzzles
 */
public class ELOStrengthTest {

	/**
	 * Test Position represents a chess position with expected best moves
	 */
	private static class TestPosition {
		final String fen;
		final String description;
		final List<String> bestMoves;
		final int difficulty; // 1-10 scale
		final int maxTimeSeconds;

		TestPosition(final String fen, final String description, final List<String> bestMoves, final int difficulty,
				final int maxTimeSeconds) {
			this.fen = fen;
			this.description = description;
			this.bestMoves = bestMoves;
			this.difficulty = difficulty;
			this.maxTimeSeconds = maxTimeSeconds;
		}
	}

	/**
	 * Test result for tracking performance
	 */
	private static class TestResult {
		final boolean passed;
		final int difficulty;
		final long timeMs;

		TestResult(final String testName, final boolean passed, final int difficulty, final long timeMs,
				final String actualMove, final String expectedMoves) {
			this.passed = passed;
			this.difficulty = difficulty;
			this.timeMs = timeMs;
		}
	}

	/**
	 * Calculate ELO estimate based on test performance
	 */
	private static int calculateELOEstimate(final double weightedPassRate, final double avgTime,
			final double avgDifficulty) {
		final var baseELO = 1200; // Starting point for basic chess understanding

		// Performance bonus based on weighted pass rate
		final var performanceBonus = (int) (weightedPassRate * 10); // 0-1000 points

		// Difficulty bonus
		final var difficultyBonus = (int) (avgDifficulty * 50); // 0-500 points

		// Time penalty (slower = lower rating)
		var timePenalty = 0;
		if (avgTime > 10000) {
			timePenalty = (int) ((avgTime - 10000) / 1000 * 10); // -10 per extra second
		}

		// Speed bonus for very fast engines
		var speedBonus = 0;
		if (avgTime < 2000) {
			speedBonus = (int) ((2000 - avgTime) / 100 * 5); // +5 per 100ms faster
		}

		final var estimatedELO = baseELO + performanceBonus + difficultyBonus + speedBonus - timePenalty;

		// Clamp to reasonable range
		return Math.max(800, Math.min(2800, estimatedELO));
	}

	/**
	 * Get strength category based on ELO
	 */
	private static String getStrengthCategory(final int elo) {
		if (elo < 1000) {
			return "Beginner";
		}
		if (elo < 1200) {
			return "Novice";
		}
		if (elo < 1400) {
			return "Class E";
		}
		if (elo < 1600) {
			return "Class D";
		}
		if (elo < 1800) {
			return "Class C";
		}
		if (elo < 2000) {
			return "Class B";
		}
		if (elo < 2200) {
			return "Class A";
		}
		if (elo < 2400) {
			return "Expert";
		}
		return elo < 2600 ? "Master" : "Grandmaster";
	}

	private ChessAI ai;

	private List<TestResult> results;

	/**
	 * Print comprehensive strength analysis
	 */
	private void printStrengthAnalysis() {
		System.out.println("\n" + "=".repeat(60));
		System.out.println("CHESS ENGINE STRENGTH ANALYSIS");
		System.out.println("=".repeat(60));

		if (results.isEmpty()) {
			System.out.println("No test results available");
			return;
		}

		// Calculate statistics
		final var totalTests = results.size();
		final var passedTests = results.stream().mapToInt(r -> r.passed ? 1 : 0).sum();
		final var passRate = (double) passedTests / totalTests * 100;

		final var totalTime = results.stream().mapToLong(r -> r.timeMs).sum();
		final var avgTime = (double) totalTime / totalTests;

		final var avgDifficulty = results.stream().mapToInt(r -> r.difficulty).average().orElse(0);

		// Calculate weighted score
		final var weightedScore = results.stream().mapToDouble(r -> r.passed ? r.difficulty * 10 : 0).sum();
		final var maxPossibleScore = results.stream().mapToDouble(r -> r.difficulty * 10).sum();
		final var weightedPassRate = (weightedScore / maxPossibleScore) * 100;

		System.out.println("OVERALL PERFORMANCE:");
		System.out.println("- Total tests: " + totalTests);
		System.out.println("- Passed: " + passedTests + " (" + "%.1f".formatted(passRate) + "%)");
		System.out.println("- Average time per position: " + "%.1f".formatted(avgTime) + "ms");
		System.out.println("- Average difficulty: " + "%.1f".formatted(avgDifficulty) + "/10");
		System.out.println("- Weighted pass rate: " + "%.1f".formatted(weightedPassRate) + "%");

		// ELO Estimation
		final var estimatedELO = calculateELOEstimate(weightedPassRate, avgTime, avgDifficulty);
		System.out.println("\nESTIMATED ELO STRENGTH:");
		System.out.println("- Estimated ELO: " + estimatedELO);
		System.out.println("- Strength category: " + getStrengthCategory(estimatedELO));

		// Detailed breakdown
		System.out.println("\nDETAILED RESULTS:");
		final Map<Integer, Integer> difficultyStats = new HashMap<>();
		results.forEach((final TestResult result) -> difficultyStats.merge(result.difficulty, result.passed ? 1 : 0,
				Integer::sum));

		for (var diff = 1; diff <= 10; diff++) {
			final var difficulty = diff;
			if (difficultyStats.containsKey(difficulty)) {
				final var totalAtDifficulty = results.stream().filter(r -> r.difficulty == difficulty).count();
				final int passedAtDifficulty = difficultyStats.get(difficulty);
				final var successRate = (double) passedAtDifficulty / totalAtDifficulty * 100;

				System.out.println("- Difficulty " + difficulty + ": " + passedAtDifficulty + "/" + totalAtDifficulty
						+ " (" + "%.1f".formatted(successRate) + "%)");
			}
		}

		// Performance analysis
		System.out.println("\nPERFORMANCE ANALYSIS:");
		if (avgTime < 1000) {
			System.out.println("- Speed: Very fast (< 1s average)");
		} else if (avgTime < 5000) {
			System.out.println("- Speed: Fast (1-5s average)");
		} else if (avgTime < 15000) {
			System.out.println("- Speed: Moderate (5-15s average)");
		} else {
			System.out.println("- Speed: Slow (> 15s average)");
		}

		if (weightedPassRate >= 80) {
			System.out.println("- Tactical accuracy: Excellent");
		} else if (weightedPassRate >= 60) {
			System.out.println("- Tactical accuracy: Good");
		} else if (weightedPassRate >= 40) {
			System.out.println("- Tactical accuracy: Fair");
		} else {
			System.out.println("- Tactical accuracy: Needs improvement");
		}

		System.out.println("\nRECOMMENDations:");
		if (weightedPassRate < 50) {
			System.out.println("- Focus on basic tactical recognition");
			System.out.println("- Improve search depth for critical positions");
		} else if (weightedPassRate < 70) {
			System.out.println("- Enhance evaluation function");
			System.out.println("- Add more advanced tactical patterns");
		} else {
			System.out.println("- Engine shows strong tactical ability");
			System.out.println("- Consider optimizing for specific opening/endgame knowledge");
		}

		System.out.println("\n" + "=".repeat(60));
	}

	/**
	 * Run a test suite and record results
	 */
	private void runTestSuite(final String suiteName, final List<TestPosition> positions) {
		System.out.println("\n--- Running " + suiteName + " ---");

		for (var i = 0; i < positions.size(); i++) {
			final var pos = positions.get(i);
			System.out.println("\nTest " + (i + 1) + ": " + pos.description);

			final var board = new Board();
			board.loadFromFen(pos.fen);

			final var startTime = System.currentTimeMillis();
			final var actualMove = ai.getBestMove(board);
			final var endTime = System.currentTimeMillis();

			assertNotNull(actualMove, "Engine should find a move");

			final var passed = pos.bestMoves.contains(actualMove.toString());

			final var result = new TestResult(pos.description, passed, pos.difficulty, endTime - startTime,
					actualMove.toString(), String.join(", ", pos.bestMoves));

			results.add(result);

			System.out.println("Expected: " + String.join(", ", pos.bestMoves));
			System.out.println("Actual: " + actualMove);
			System.out.println("Time: " + (endTime - startTime) + "ms");
			System.out.println("Result: " + (passed ? "✓ PASS" : "✗ FAIL"));

			if ((endTime - startTime) > pos.maxTimeSeconds * 1000) {
				System.out.println("⚠ WARNING: Exceeded time limit");
			}
		}
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
		printStrengthAnalysis();
	}

	@Test
	@DisplayName("Advanced Tactical Test Suite")
	@Timeout(value = 900, unit = TimeUnit.SECONDS)
	void testAdvancedTactics() {
		System.out.println("\n=== ADVANCED TACTICAL TESTS ===");

		final List<TestPosition> advancedTactics = List.of(
				// Complex combination
				new TestPosition("r2qkb1r/ppp2ppp/2np1n2/2b1p3/2B1P3/3P1N2/PPP1QPPP/RNB2RK1 w kq - 0 1",
						"Complex tactical combination", List.of("c4f7", "f3e5", "e2e5"), 7, 30),
				// Sacrificial attack
				new TestPosition("r1bqk2r/pppp1ppp/2n2n2/2b1p3/2B1P3/3P1N2/PPP2PPP/RNBQK2R w KQkq - 0 1",
						"Sacrificial attack", List.of("c4f7", "f3e5"), 8, 30),
				// Zugzwang position
				new TestPosition("8/8/8/8/8/1k6/2K5/8 w - - 0 1", "King and pawn endgame",
						List.of("c2c3", "c2b2", "c2d2"), 6, 25));

		runTestSuite("Advanced Tactics", advancedTactics);
	}

	@Test
	@DisplayName("Basic Tactical Test Suite")
	@Timeout(value = 300, unit = TimeUnit.SECONDS)
	void testBasicTactics() {
		System.out.println("\n=== BASIC TACTICAL TESTS ===");

		final List<TestPosition> basicTactics = List.of(
				// Mate in 1
				new TestPosition("rnbqkb1r/pppp1ppp/5n2/4p2Q/4P3/8/PPPP1PPP/RNB1KBNR w KQkq - 0 1",
						"Mate in 1 - Scholar's Mate", List.of("h5f7"), 2, 5),
				// Simple fork
				new TestPosition("rnbqkb1r/pppp1ppp/5n2/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 0 1",
						"Knight fork opportunity", List.of("f3e5", "f3d4"), 3, 10),
				// Pin tactic
				new TestPosition("rnbqkb1r/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 1", "Pin with bishop",
						List.of("f1c4", "f1b5"), 3, 10),
				// Simple capture
				new TestPosition("rnbqkb1r/pppp1ppp/5n2/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 1",
						"Central pawn capture", List.of("e4e5"), 2, 5));

		runTestSuite("Basic Tactics", basicTactics);
	}

	@Test
	@DisplayName("Blunder Prevention Test")
	@Timeout(value = 300, unit = TimeUnit.SECONDS)
	void testBlunderPrevention() {
		System.out.println("\n=== BLUNDER PREVENTION TESTS ===");

		final List<TestPosition> blunderTests = List.of(
				// Avoid hanging queen
				new TestPosition("rnbqkbnr/pppppppp/8/8/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 1",
						"Don't hang queen with Qh5", List.of("g1f3", "b1c3", "f1c4", "d2d3", "d2d4"), 3, 10),
				// Avoid back-rank mate
				new TestPosition("6k1/5ppp/8/8/8/8/5PPP/6K1 w - - 0 1", "Avoid back-rank weakness",
						List.of("g2g3", "h2h3", "f2f3", "g1h1"), 4, 15),
				// Don't lose material
				new TestPosition("rnbqkb1r/pppp1ppp/5n2/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 1",
						"Don't lose material unnecessarily", List.of("d2d3", "g1f3", "b1c3", "f1c4"), 3, 10));

		runTestSuite("Blunder Prevention", blunderTests);
	}

	@Test
	@DisplayName("Endgame Test Suite")
	@Timeout(value = 600, unit = TimeUnit.SECONDS)
	void testEndgames() {
		System.out.println("\n=== ENDGAME TESTS ===");

		final List<TestPosition> endgames = List.of(
				// Basic king and pawn
				new TestPosition("8/8/8/8/4P3/8/8/3K1k2 w - - 0 1", "King and pawn vs king",
						List.of("d1d2", "d1e2", "e4e5"), 4, 20),
				// Rook endgame
				new TestPosition("8/8/8/8/8/8/2R5/3K1k2 w - - 0 1", "Rook endgame", List.of("c2c8", "c2f2", "d1d2"), 6,
						25),
				// Queen vs pawn
				new TestPosition("8/8/8/8/8/8/1Q6/3K1k2 w - - 0 1", "Queen vs king", List.of("b2b8", "b2f2", "b2f6"), 5,
						20));

		runTestSuite("Endgames", endgames);
	}

	@Test
	@DisplayName("Famous Test Positions")
	@Timeout(value = 600, unit = TimeUnit.SECONDS)
	void testFamousPositions() {
		System.out.println("\n=== FAMOUS TEST POSITIONS ===");

		final List<TestPosition> famousPositions = List.of(
				// Capablanca vs Marshall
				new TestPosition("r1bqkb1r/pppp1ppp/2n2n2/4p3/4P3/3P1N2/PPP2PPP/RNBQKB1R w KQkq - 0 1",
						"Capablanca's position", List.of("f3e5", "c1g5", "f1c4"), 6, 25),
				// Morphy's opera game position
				new TestPosition("rnbqkb1r/ppp2ppp/3p1n2/4p3/4P3/3P1N2/PPP2PPP/RNBQKB1R w KQkq - 0 1",
						"Morphy-style position", List.of("f3e5", "c1g5", "f1c4"), 7, 30));

		runTestSuite("Famous Positions", famousPositions);
	}

	@Test
	@DisplayName("Intermediate Tactical Test Suite")
	@Timeout(value = 600, unit = TimeUnit.SECONDS)
	void testIntermediateTactics() {
		System.out.println("\n=== INTERMEDIATE TACTICAL TESTS ===");

		final List<TestPosition> intermediateTactics = List.of(
				// Discovered attack
				new TestPosition("r1bqkb1r/pppp1ppp/2n2n2/4p3/2B1P3/3P1N2/PPP2PPP/RNBQK2R w KQkq - 0 1",
						"Discovered attack", List.of("c4f7", "f3e5"), 5, 15),
				// Double attack
				new TestPosition("r1bqkb1r/pppp1ppp/2n2n2/4p3/4P3/2N2N2/PPPP1PPP/R1BQKB1R w KQkq - 0 1",
						"Double attack with knight", List.of("c3d5", "f3e5"), 5, 15),
				// Deflection
				new TestPosition("r1bq1rk1/ppp2ppp/2np1n2/2b1p3/2B1P3/3P1N2/PPP1QPPP/RNB2RK1 w - - 0 8",
						"Deflection tactic", List.of("c4f7", "f3e5"), 6, 20),
				// Skewer
				new TestPosition("r1bqk2r/pppp1ppp/2n2n2/2b1p3/2B1P3/3P1N2/PPP2PPP/RNBQK2R w KQkq - 0 1",
						"Skewer with bishop", List.of("c4f7"), 5, 15));

		runTestSuite("Intermediate Tactics", intermediateTactics);
	}
}
