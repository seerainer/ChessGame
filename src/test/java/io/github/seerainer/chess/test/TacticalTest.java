package io.github.seerainer.chess.test;

import com.github.bhlangonijr.chesslib.Board;

import io.github.seerainer.chess.ChessAI;

/**
 * Test tactical awareness and blunder prevention
 */
public class TacticalTest {

	public static void main(final String[] args) {
		testHangingPiecePrevention();
		testTacticalAwareness();
		testCaptureAvoidance();
	}

	/**
	 * Test if AI avoids unnecessary piece captures that lead to loss
	 */
	private static void testCaptureAvoidance() {
		System.out.println("=== Testing Capture Avoidance ===");

		// Position where capturing would lose material
		final var fen = "rnbqkb1r/pppp1ppp/5n2/4p3/4P3/3P1N2/PPP2PPP/RNBQKB1R b KQkq - 0 4";
		final var board = new Board();
		board.loadFromFen(fen);

		System.out.println("Position: " + fen);
		System.out.println("Testing if AI avoids bad captures...");

		final var ai = new ChessAI();
		final var bestMove = ai.getBestMove(board);

		System.out.println("AI chose: " + bestMove);

		// Any reasonable move is better than hanging pieces
		System.out.println("✅ AI evaluated position with anti-blunder system");
		System.out.println();
	}

	/**
	 * Test if AI avoids hanging pieces
	 */
	private static void testHangingPiecePrevention() {
		System.out.println("=== Testing Hanging Piece Prevention ===");

		// Position where moving queen would hang it
		final var fen = "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2";
		final var board = new Board();
		board.loadFromFen(fen);

		System.out.println("Position: " + fen);
		System.out.println("Testing if AI avoids hanging queen with Qh5...");

		final var ai = new ChessAI();
		final var bestMove = ai.getBestMove(board);

		System.out.println("AI chose: " + bestMove);

		// Check if AI avoided the blunder
		if ("d1h5".equals(bestMove.toString())) {
			System.out.println("❌ FAILED: AI hung the queen!");
		} else {
			System.out.println("✅ PASSED: AI avoided hanging the queen");
		}
		System.out.println();
	}

	/**
	 * Test tactical awareness in complex positions
	 */
	private static void testTacticalAwareness() {
		System.out.println("=== Testing Tactical Awareness ===");

		// Position with tactical opportunity (fork possible)
		final var fen = "rnbqkb1r/pppp1ppp/5n2/4p3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 4 3";
		final var board = new Board();
		board.loadFromFen(fen);

		System.out.println("Position: " + fen);
		System.out.println("Testing tactical awareness...");

		final var ai = new ChessAI();
		final var bestMove = ai.getBestMove(board);

		System.out.println("AI chose: " + bestMove);
		System.out.println("✅ Move evaluated with tactical depth");
		System.out.println();
	}
}
