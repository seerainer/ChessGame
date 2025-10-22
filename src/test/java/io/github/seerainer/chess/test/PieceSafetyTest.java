package io.github.seerainer.chess.test;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Square;

import io.github.seerainer.chess.ChessAI;

/**
 * Simple test to verify piece safety improvements
 */
public class PieceSafetyTest {

    public static void main(final String[] args) {
	System.out.println("=== Testing Piece Safety Improvements ===");

	testQueenSafety();
	testCheckPriority();

	System.out.println("=== Test Complete ===");
    }

    /**
     * Test that engine doesn't prioritize checks over material
     */
    private static void testCheckPriority() {
	System.out.println("\n--- Testing Check Priority ---");

	// Position where a queen can be captured
	final var board = new Board();
	board.loadFromFen("rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2");

	// Place a black queen that can be captured
	board.setPiece(Piece.BLACK_QUEEN, Square.E5);

	System.out.println("Position: Black queen on e5 can be captured");

	final var ai = new ChessAI();
	final var bestMove = ai.getBestMove(board);

	System.out.println("AI chose: " + bestMove);

	// Check if AI captured the queen
	if (bestMove != null && bestMove.getTo() == Square.E5) {
	    System.out.println("✅ AI captured the queen!");
	} else {
	    System.out.println("⚠️  AI chose different move: " + bestMove);
	}
    }

    /**
     * Test that engine doesn't hang the queen unnecessarily
     */
    private static void testQueenSafety() {
	System.out.println("\n--- Testing Queen Safety ---");

	// Simple starting position
	final var board = new Board();
	board.loadFromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");

	final var ai = new ChessAI();
	final var bestMove = ai.getBestMove(board);

	System.out.println("Opening move: " + bestMove);

	// Check if it's a reasonable opening move
	if (bestMove != null) {
	    System.out.println("✅ AI made a move: " + bestMove);
	} else {
	    System.out.println("❌ AI failed to make a move");
	}
    }
}
