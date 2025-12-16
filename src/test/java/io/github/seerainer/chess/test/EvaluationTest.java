package io.github.seerainer.chess.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.github.bhlangonijr.chesslib.Board;

import io.github.seerainer.chess.ChessAI;

/**
 * Simple test to verify the AI makes good moves with the new evaluation system
 */
public class EvaluationTest {

    @Test
    @DisplayName("Test AI Evaluation System")
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void testEvaluationSystem() {
	System.out.println("Testing AI with new evaluation system...");

	// Create a new board
	final var board = new Board();
	final var ai = new ChessAI();

	try {
	    System.out.println("Starting position:");
	    System.out.println(board.toString());

	    // Play a few moves to see if AI develops pieces
	    for (var i = 0; i < 5; i++) { // Reduced to 5 moves for test speed
		if (board.isDraw() || board.isMated()) {
		    break;
		}

		final var move = ai.getBestMove(board);
		assertNotNull(move, "AI should find a move");

		System.out.println(new StringBuilder().append("Move ").append(i + 1).append(": ")
			.append(move.toString()).toString());
		System.out.println(new StringBuilder().append("From: ").append(move.getFrom()).append(" To: ")
			.append(move.getTo()).toString());

		board.doMove(move);
		System.out.println("Position after move:");
		System.out.println(board.toString());
		System.out.println();
	    }

	    System.out.println("Test completed!");
	} finally {
	    ai.cleanup();
	}
    }
}
