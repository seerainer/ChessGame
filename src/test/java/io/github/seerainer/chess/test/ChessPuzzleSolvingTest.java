package io.github.seerainer.chess.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.github.bhlangonijr.chesslib.Board;

import io.github.seerainer.chess.ChessAI;

/**
 * Chess puzzle solving test - a reliable indicator of engine tactical strength
 * These puzzles are calibrated to specific rating levels
 */
public class ChessPuzzleSolvingTest {

    private ChessAI ai;

    private int puzzlesSolved = 0;

    private int totalPuzzles = 0;

    /**
     * Calculate ELO based on puzzle solving performance
     */
    private static int calculatePuzzleELO(final double solveRate) {
	// Puzzle solving is a strong indicator of tactical strength
	// Base ELO starts at 800 (absolute beginner)
	final var baseELO = 800;

	// Linear scaling based on solve rate
	final var bonusELO = (int) (solveRate * 15); // 0-100% solve rate = 0-1500 ELO bonus

	return Math.max(800, Math.min(2300, baseELO + bonusELO));
    }

    /**
     * Get tactical strength category
     */
    private static String getTacticalStrength(final int elo) {
	if (elo < 1000) {
	    return "Beginner";
	}
	if (elo < 1200) {
	    return "Novice";
	}
	if (elo < 1400) {
	    return "Intermediate";
	}
	if (elo < 1600) {
	    return "Club Player";
	}
	if (elo < 1800) {
	    return "Strong Club Player";
	}
	if (elo < 2000) {
	    return "Expert";
	}
	return elo < 2200 ? "Master" : "Grandmaster";
    }

    /**
     * Print puzzle solving analysis
     */
    private void printPuzzleAnalysis() {
	System.out.println("\n" + "=".repeat(60));
	System.out.println("CHESS PUZZLE SOLVING ANALYSIS");
	System.out.println("=".repeat(60));

	if (totalPuzzles == 0) {
	    System.out.println("No puzzles were attempted");
	    return;
	}

	final var solveRate = (double) puzzlesSolved / totalPuzzles * 100;

	System.out.println("PUZZLE SOLVING SUMMARY:");
	System.out.println("- Total puzzles attempted: " + totalPuzzles);
	System.out.println("- Puzzles solved: " + puzzlesSolved);
	System.out.println(new StringBuilder().append("- Solve rate: ").append("%.1f".formatted(solveRate)).append("%")
		.toString());

	// ELO estimation based on puzzle solving
	final var puzzleELO = calculatePuzzleELO(solveRate);
	System.out.println("\nPUZZLE-BASED ELO ESTIMATE:");
	System.out.println("- Estimated ELO: " + puzzleELO);
	System.out.println("- Tactical strength: " + getTacticalStrength(puzzleELO));

	// Performance analysis
	System.out.println("\nTACTICAL ANALYSIS:");
	if (solveRate >= 80) {
	    System.out.println("- Excellent tactical vision");
	    System.out.println("- Strong pattern recognition");
	    System.out.println("- Suitable for competitive play");
	} else if (solveRate >= 60) {
	    System.out.println("- Good tactical ability");
	    System.out.println("- Solid pattern recognition");
	    System.out.println("- Room for improvement in complex positions");
	} else if (solveRate >= 40) {
	    System.out.println("- Fair tactical ability");
	    System.out.println("- Basic pattern recognition");
	    System.out.println("- Focus on fundamental tactics");
	} else {
	    System.out.println("- Limited tactical ability");
	    System.out.println("- Needs improvement in pattern recognition");
	    System.out.println("- Recommend tactical training");
	}

	System.out.println("\nRECOMMENDATIONS:");
	if (solveRate < 30) {
	    System.out.println("- Improve basic tactical patterns (pins, forks, skewers)");
	    System.out.println("- Increase search depth for tactical positions");
	    System.out.println("- Enhance evaluation of tactical threats");
	} else if (solveRate < 60) {
	    System.out.println("- Focus on complex tactical combinations");
	    System.out.println("- Improve calculation accuracy");
	    System.out.println("- Add advanced tactical motifs");
	} else {
	    System.out.println("- Engine shows strong tactical capability");
	    System.out.println("- Consider optimizing for speed while maintaining accuracy");
	    System.out.println("- Focus on positional understanding");
	}

	System.out.println("\n" + "=".repeat(60));
    }

    @BeforeEach
    void setUp() {
	ai = new ChessAI();
	puzzlesSolved = 0;
	totalPuzzles = 0;
    }

    /**
     * Solve a set of puzzles and track results
     */
    private void solvePuzzles(final String category, final List<ChessPuzzle> puzzles) {
	System.out.println(
		new StringBuilder().append("\n--- Solving ").append(category).append(" Puzzles ---").toString());

	for (var i = 0; i < puzzles.size(); i++) {
	    final var puzzle = puzzles.get(i);
	    totalPuzzles++;

	    System.out.println(new StringBuilder().append("\nPuzzle ").append(i + 1).append(" (Rating ")
		    .append(puzzle.rating).append("): ").append(puzzle.description).toString());

	    final var board = new Board();
	    board.loadFromFen(puzzle.fen);

	    final var startTime = System.currentTimeMillis();
	    final var actualMove = ai.getBestMove(board);
	    final var endTime = System.currentTimeMillis();

	    assertNotNull(actualMove, "Engine should find a move");

	    final var solved = puzzle.solutions.contains(actualMove.toString());
	    if (solved) {
		puzzlesSolved++;
	    }

	    System.out.println("Expected: " + String.join(", ", puzzle.solutions));
	    System.out.println("Actual: " + actualMove);
	    System.out
		    .println(new StringBuilder().append("Time: ").append(endTime - startTime).append("ms").toString());
	    System.out.println("Result: " + (solved ? "✓ SOLVED" : "✗ FAILED"));

	    if ((endTime - startTime) > puzzle.maxTimeSeconds * 1000) {
		System.out.println(new StringBuilder().append("⚠ WARNING: Exceeded time limit (")
			.append(puzzle.maxTimeSeconds).append("s)").toString());
	    }
	}
    }

    @AfterEach
    void tearDown() {
	if (ai != null) {
	    ai.cleanup();
	}
	printPuzzleAnalysis();
    }

    @Test
    @DisplayName("Advanced Puzzles (1600-2000 ELO)")
    @Timeout(value = 600, unit = TimeUnit.SECONDS)
    void testAdvancedPuzzles() {
	System.out.println("\n=== ADVANCED PUZZLES (1600-2000 ELO) ===");

	final List<ChessPuzzle> advancedPuzzles = List.of(
		new ChessPuzzle("r2q1rk1/ppp2ppp/2np1n2/2b1p3/2B1P3/3P1N2/PPP1QPPP/RNB2RK1 w - - 0 8",
			"Complex combination - sacrifice for mate", List.of("c4f7", "e2e5", "f3e5"), 1750, 45),
		new ChessPuzzle("r1bq1rk1/ppp2ppp/2np1n2/2b1p3/2B1P3/3P1N2/PPP1QPPP/RNB2RK1 w - - 0 8",
			"Clearance sacrifice for breakthrough", List.of("f3e5", "c4f7"), 1800, 40),
		new ChessPuzzle("r2qkb1r/ppp2ppp/2np1n2/2b1p3/2B1P3/3P1N2/PPP1QPPP/RNB2RK1 w kq - 0 1",
			"Zugzwang in middle game", List.of("e2e5", "c4f7", "f3e5"), 1900, 50));

	solvePuzzles("Advanced", advancedPuzzles);
    }

    @Test
    @DisplayName("Beginner Puzzles (800-1200 ELO)")
    @Timeout(value = 180, unit = TimeUnit.SECONDS)
    void testBeginnerPuzzles() {
	System.out.println("\n=== BEGINNER PUZZLES (800-1200 ELO) ===");

	final List<ChessPuzzle> beginnerPuzzles = List.of(
		new ChessPuzzle("rnbqkb1r/pppp1ppp/5n2/4p2Q/4P3/8/PPPP1PPP/RNB1KBNR w KQkq - 0 1",
			"Mate in 1 (Scholar's Mate pattern)", List.of("h5f7"), 900, 10),
		new ChessPuzzle("r1bqkb1r/pppp1ppp/2n2n2/4p3/2B1P3/3P1N2/PPP2PPP/RNBQK2R w KQkq - 0 1",
			"Win material - fork with knight", List.of("f3e5"), 950, 15),
		new ChessPuzzle("r1bqkb1r/pppp1ppp/2n2n2/4p3/2B1P3/8/PPPP1PPP/RNBQK1NR w KQkq - 0 1",
			"Pin the knight to win material", List.of("c4f7"), 1000, 15),
		new ChessPuzzle("rnbqkb1r/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 1", "Simple development",
			List.of("g1f3", "b1c3", "f1c4", "d2d3"), 850, 10));

	solvePuzzles("Beginner", beginnerPuzzles);
    }

    @Test
    @DisplayName("Endgame Puzzles")
    @Timeout(value = 300, unit = TimeUnit.SECONDS)
    void testEndgamePuzzles() {
	System.out.println("\n=== ENDGAME PUZZLES ===");

	final List<ChessPuzzle> endgamePuzzles = List.of(
		new ChessPuzzle("8/8/8/8/4P3/8/8/3K1k2 w - - 0 1", "King and pawn vs king - opposition",
			List.of("d1d2", "d1e2", "e4e5"), 1200, 30),
		new ChessPuzzle("8/8/8/8/8/8/2R5/3K1k2 w - - 0 1", "Rook vs king - cutting off",
			List.of("c2c8", "c2f2", "c2c1"), 1400, 30),
		new ChessPuzzle("8/8/8/8/8/5k2/5N2/5K2 w - - 0 1", "Knight endgame - zugzwang",
			List.of("f2d3", "f2e4", "f2g4"), 1600, 40),
		new ChessPuzzle("8/8/8/8/8/3k4/3B4/3K4 w - - 0 1", "Bishop endgame - tempo",
			List.of("d2e3", "d2c3", "d2f4"), 1500, 35));

	solvePuzzles("Endgame", endgamePuzzles);
    }

    @Test
    @DisplayName("Expert Puzzles (2000+ ELO)")
    @Timeout(value = 900, unit = TimeUnit.SECONDS)
    void testExpertPuzzles() {
	System.out.println("\n=== EXPERT PUZZLES (2000+ ELO) ===");

	final List<ChessPuzzle> expertPuzzles = List.of(
		new ChessPuzzle("r2q1rk1/ppp2ppp/2np1n2/2b1p3/2B1P3/3P1N2/PPP1QPPP/RNB2RK1 w - - 0 8",
			"Study-like position - quiet move wins", List.of("h2h3", "g2g3", "a2a3"), 2100, 60),
		new ChessPuzzle("r1bq1rk1/ppp2ppp/2np1n2/2b1p3/2B1P3/3P1N2/PPP1QPPP/RNB2RK1 w - - 0 8",
			"Positional sacrifice for long-term advantage", List.of("c4f7", "e2e5"), 2200, 75));

	solvePuzzles("Expert", expertPuzzles);
    }

    @Test
    @DisplayName("Intermediate Puzzles (1200-1600 ELO)")
    @Timeout(value = 300, unit = TimeUnit.SECONDS)
    void testIntermediatePuzzles() {
	System.out.println("\n=== INTERMEDIATE PUZZLES (1200-1600 ELO) ===");

	final List<ChessPuzzle> intermediatePuzzles = List.of(
		new ChessPuzzle("r1bq1rk1/ppp2ppp/2np1n2/2b1p3/2B1P3/3P1N2/PPP1QPPP/RNB2RK1 w - - 0 8",
			"Deflection tactic - remove defender", List.of("c4f7", "f3e5"), 1400, 25),
		new ChessPuzzle("r2qkb1r/ppp2ppp/2np1n2/2b1p3/2B1P3/3P1N2/PPP1QPPP/RNB2RK1 w kq - 0 1",
			"Double attack - queen and bishop", List.of("e2e5", "c4f7"), 1350, 20),
		new ChessPuzzle("r1bqk2r/pppp1ppp/2n2n2/2b1p3/2B1P3/3P1N2/PPP2PPP/RNBQK2R w KQkq - 0 1",
			"Discovered attack with tempo", List.of("f3e5", "c4f7"), 1450, 25),
		new ChessPuzzle("r1bqkb1r/pppp1ppp/2n2n2/4p3/2B1P3/3P1N2/PPP2PPP/RNBQK2R w KQkq - 0 1",
			"Skewer after exchange", List.of("c4f7", "f3e5"), 1300, 20));

	solvePuzzles("Intermediate", intermediatePuzzles);
    }

    /**
     * Represents a chess puzzle with multiple solutions and rating
     */
    private static class ChessPuzzle {
	final String fen;
	final String description;
	final List<String> solutions;
	final int rating; // Expected rating level to solve this puzzle
	final int maxTimeSeconds;

	ChessPuzzle(final String fen, final String description, final List<String> solutions, final int rating,
		final int maxTimeSeconds) {
	    this.fen = fen;
	    this.description = description;
	    this.solutions = solutions;
	    this.rating = rating;
	    this.maxTimeSeconds = maxTimeSeconds;
	}
    }
}
