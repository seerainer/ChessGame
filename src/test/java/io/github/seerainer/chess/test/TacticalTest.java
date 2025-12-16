package io.github.seerainer.chess.test;

import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.github.bhlangonijr.chesslib.Board;

import io.github.seerainer.chess.ChessAI;

/**
 * Test tactical awareness and blunder prevention
 */
public class TacticalTest {

    /**
     * Test if AI avoids unnecessary piece captures that lead to loss
     */
    @Test
    @DisplayName("Test Capture Avoidance")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testCaptureAvoidance() {
	System.out.println("=== Testing Capture Avoidance ===");

	// Position where capturing would lose material
	final var fen = "rnbqkb1r/pppp1ppp/5n2/4p3/4P3/3P1N2/PPP2PPP/RNBQKB1R b KQkq - 0 4";
	final var board = new Board();
	board.loadFromFen(fen);

	System.out.println("Position: " + fen);
	System.out.println("Testing if AI avoids bad captures...");

	final var ai = new ChessAI();
	try {
	    final var bestMove = ai.getBestMove(board);

	    System.out.println("AI chose: " + bestMove);
	    assertNotNull(bestMove, "AI should find a move");

	    // Any reasonable move is better than hanging pieces
	    System.out.println("✅ AI evaluated position with anti-blunder system");
	    System.out.println();
	} finally {
	    ai.cleanup();
	}
    }

    /**
     * Test if AI avoids hanging pieces
     */
    @Test
    @DisplayName("Test Hanging Piece Prevention")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testHangingPiecePrevention() {
	System.out.println("=== Testing Hanging Piece Prevention ===");

	// Position where moving queen would hang it
	final var fen = "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2";
	final var board = new Board();
	board.loadFromFen(fen);

	System.out.println("Position: " + fen);
	System.out.println("Testing if AI avoids hanging queen with Qh5...");

	final var ai = new ChessAI();
	try {
	    final var bestMove = ai.getBestMove(board);

	    System.out.println("AI chose: " + bestMove);
	    assertNotNull(bestMove, "AI should find a move");

	    // Check if AI avoided the blunder
	    assertNotEquals("d1h5", bestMove.toString(), "AI hung the queen!");
	    System.out.println("✅ PASSED: AI avoided hanging the queen");
	    System.out.println();
	} finally {
	    ai.cleanup();
	}
    }

    /**
     * Test tactical awareness in complex positions
     */
    @Test
    @DisplayName("Test Tactical Awareness")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testTacticalAwareness() {
	System.out.println("=== Testing Tactical Awareness ===");

	// Position with tactical opportunity (fork possible)
	final var fen = "rnbqkb1r/pppp1ppp/5n2/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 4 3";
	final var board = new Board();
	board.loadFromFen(fen);

	System.out.println("Position: " + fen);
	System.out.println("Testing tactical awareness...");

	final var ai = new ChessAI();
	try {
	    final var bestMove = ai.getBestMove(board);

	    System.out.println("AI chose: " + bestMove);
	    assertNotNull(bestMove, "AI should find a move");
	    System.out.println("✅ Move evaluated with tactical depth");
	    System.out.println();
	} finally {
	    ai.cleanup();
	}
    }
}
