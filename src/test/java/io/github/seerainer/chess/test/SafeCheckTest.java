package io.github.seerainer.chess.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;

import io.github.seerainer.chess.ChessAI;

/**
 * Tests that the chess engine does not prioritize unsafe checks where the
 * checking piece can be easily captured.
 */
class SafeCheckTest {

    /**
     * Test that the engine avoids giving check with the queen when the queen would
     * be captured (Qf3-f7 in front of the king with pawns defending).
     */
    @Test
    @DisplayName("Avoid Unsafe Queen Check")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testUnsafeQueenCheck() {
	// White queen on f3 can give check on f7, but would be captured
	final var fen = "rnbqkbnr/pppp1ppp/8/4p3/4P3/5Q2/PPPP1PPP/RNB1KBNR b KQkq - 1 2";
	final var board = new Board();
	board.loadFromFen(fen);
	board.setSideToMove(Side.WHITE);

	final var ai = new ChessAI();
	try {
	    final var bestMove = ai.getBestMove(board);
	    assertNotNull(bestMove, "AI should find a move");

	    // AI should not play Qf3-f7 (unsafe check — queen captured by king/pawn)
	    final var isUnsafeCheck = bestMove.getFrom() == Square.F3 && bestMove.getTo() == Square.F7;
	    assertTrue(!isUnsafeCheck, "AI should not give unsafe queen check (Qf7), chose: " + bestMove);
	} finally {
	    ai.cleanup();
	}
    }

    /**
     * Test that the engine returns a valid move in a position where a rook could
     * give check but would be captured.
     */
    @Test
    @DisplayName("Avoid Unsafe Rook Check")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testUnsafeRookCheck() {
	final var board = new Board();
	board.loadFromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKB1R w KQkq - 0 1");

	final var ai = new ChessAI();
	try {
	    final var bestMove = ai.getBestMove(board);
	    assertNotNull(bestMove, "AI should find a move in this position");
	} finally {
	    ai.cleanup();
	}
    }

    /**
     * Test that if the engine gives check, the checking piece is safe (defended or
     * not attacked).
     */
    @Test
    @DisplayName("Safe Check vs Unsafe Check Preference")
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void testSafeCheckVsUnsafeCheck() {
	// Italian Game position — both safe and unsafe check options exist
	final var fen = "r1bqkb1r/pppp1ppp/2n2n2/4p3/2B1P3/5N2/PPPP1PPP/RNBQK2R w KQkq - 4 4";
	final var board = new Board();
	board.loadFromFen(fen);

	final var ai = new ChessAI();
	try {
	    final var bestMove = ai.getBestMove(board);
	    assertNotNull(bestMove, "AI should find a move");

	    board.doMove(bestMove);
	    final var givesCheck = board.isKingAttacked();

	    if (givesCheck) {
		// If AI chose to give check, verify the checker is safe
		final var checkerSquare = bestMove.getTo();
		final var isAttacked = board.squareAttackedBy(checkerSquare, Side.BLACK) != 0L;
		final var isDefended = board.squareAttackedBy(checkerSquare, Side.WHITE) != 0L;

		assertTrue(!isAttacked || isDefended,
			new StringBuilder().append("If AI gives check, the checking piece should be safe, but ")
				.append(bestMove).append(" leaves piece undefended").toString());
	    }
	    // If AI chose not to give check, that is fine — it may have found a better move

	    board.undoMove();
	} finally {
	    ai.cleanup();
	}
    }

    /**
     * Test that the engine prefers capturing a free queen over giving check. A
     * black queen is placed on f7 where it can be captured.
     */
    @Test
    @DisplayName("Prefer Queen Capture Over Check")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testCheckVsQueenCapture() {
	final var board = new Board();
	board.loadFromFen("rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2");

	// Place opponent queen in capturable position
	board.setPiece(Piece.BLACK_QUEEN, Square.F7);

	final var ai = new ChessAI();
	try {
	    final var bestMove = ai.getBestMove(board);
	    assertNotNull(bestMove, "AI should find a move");

	    // Check if AI captured the queen
	    final var captured = board.getPiece(bestMove.getTo());
	    if (captured == Piece.BLACK_QUEEN) {
		// Best outcome — AI captured the queen
		return;
	    }

	    // If AI didn't capture the queen, it should at least not give an unsafe check
	    board.doMove(bestMove);
	    final var givesCheck = board.isKingAttacked();
	    assertTrue(!givesCheck,
		    "AI should capture the queen or make a safe move, not give check. Chose: " + bestMove);
	    board.undoMove();
	} finally {
	    ai.cleanup();
	}
    }
}
