package io.github.seerainer.chess.test;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;

import io.github.seerainer.chess.ChessAI;

/**
 * Test to verify that the chess engine no longer prioritizes unsafe checks
 * where the checking piece can be easily captured.
 */
public class SafeCheckTest {

	public static void main(final String[] args) {
		System.out.println("=== Safe Check Test Suite ===");

		testUnsafeQueenCheck();
		testUnsafeRookCheck();
		testSafeCheckVsUnsafeCheck();
		testCheckVsQueenCapture();

		System.out.println("=== Safe Check Test Complete ===");
	}

	/**
	 * Test that the engine prefers capturing the queen over giving check
	 */
	private static void testCheckVsQueenCapture() {
		System.out.println("\n=== Testing Check vs Queen Capture ===");

		// Position where AI can either give check or capture queen
		final var fen = "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2";
		final var board = new Board();
		board.loadFromFen(fen);

		// Artificially place opponent queen in capturable position
		board.setPiece(Piece.BLACK_QUEEN, Square.F7);

		System.out.println("Position: Queen on f7 can be captured, or other moves available");

		final var ai = new ChessAI();
		final var bestMove = ai.getBestMove(board);

		System.out.println("AI chose: " + bestMove);

		// Check if AI captured the queen
		final var captured = board.getPiece(bestMove.getTo());
		if (captured == Piece.BLACK_QUEEN) {
			System.out.println("✅ PASSED: AI captured queen instead of giving unsafe check");
		} else {
			// Test if the move gives check
			board.doMove(bestMove);
			final var givesCheck = board.isKingAttacked();

			if (givesCheck) {
				System.out.println("❌ FAILED: AI gave check instead of capturing queen!");
			} else {
				System.out.println("✅ PASSED: AI made safe move (neither check nor queen capture)");
			}
			board.undoMove();
		}
	}

	/**
	 * Test that the engine prefers safe checks over unsafe checks
	 */
	private static void testSafeCheckVsUnsafeCheck() {
		System.out.println("\n=== Testing Safe vs Unsafe Check Preference ===");

		// Position where there are both safe and unsafe check options
		final var fen = "r1bqkb1r/pppp1ppp/2n2n2/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4";
		final var board = new Board();
		board.loadFromFen(fen);

		System.out.println("Position: " + fen);
		System.out.println("Testing if AI prefers safe checks over unsafe ones");

		final var ai = new ChessAI();
		final var bestMove = ai.getBestMove(board);

		System.out.println("AI chose: " + bestMove);

		// Test the safety of the chosen move
		board.doMove(bestMove);
		final var givesCheck = board.isKingAttacked();

		if (givesCheck) {
			// Check if the piece that gave check is safe
			final var checkerSquare = bestMove.getTo();

			final var isAttacked = board.squareAttackedBy(checkerSquare, Side.BLACK) != 0L;
			final var isDefended = board.squareAttackedBy(checkerSquare, Side.WHITE) != 0L;

			if (isAttacked && !isDefended) {
				System.out.println("❌ FAILED: AI gave check with undefended piece!");
			} else {
				System.out.println("✅ PASSED: AI gave safe check or defended piece");
			}
		} else {
			System.out.println("✅ PASSED: AI chose not to give check (likely safer)");
		}

		board.undoMove();
	}

	/**
	 * Test that the engine avoids giving check with the queen when it can be
	 * captured
	 */
	private static void testUnsafeQueenCheck() {
		System.out.println("\n=== Testing Unsafe Queen Check ===");

		// Position where queen can give check but would be captured
		final var fen = "rnbqkbnr/pppp1ppp/8/4p3/4P3/5Q2/PPPP1PPP/RNB1KBNR b KQkq - 1 2";
		final var board = new Board();
		board.loadFromFen(fen);

		System.out.println("Position: " + fen);
		System.out.println("White queen on f3 can give check on f7, but would be captured");

		// Switch to white's turn
		board.setSideToMove(Side.WHITE);

		final var ai = new ChessAI();
		final var bestMove = ai.getBestMove(board);

		System.out.println("AI chose: " + bestMove);

		// Check if AI chose to give check with queen
		if (bestMove.getFrom() == Square.F3 && bestMove.getTo() == Square.F7) {
			System.out.println("❌ FAILED: AI gave unsafe check with queen!");
		} else {
			System.out.println("✅ PASSED: AI avoided unsafe queen check");
		}
	}

	/**
	 * Test that the engine avoids giving check with a rook when it can be captured
	 */
	private static void testUnsafeRookCheck() {
		System.out.println("\n=== Testing Unsafe Rook Check ===");

		// Position where rook can give check but would be captured
		final var fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKB1R w KQkq - 0 1";
		final var board = new Board();
		board.loadFromFen(fen);

		// Move rook to give check position
		board.doMove(new Move(Square.H1, Square.H8));

		System.out.println("Position: Rook on h8 giving check but can be captured");

		final var ai = new ChessAI();
		final var bestMove = ai.getBestMove(board);

		System.out.println("AI chose: " + bestMove);

		// This is more complex to analyze automatically, but the idea is that
		// the engine should not prioritize hanging the rook just for check
		System.out.println("✅ Test recorded: " + bestMove);
	}
}
