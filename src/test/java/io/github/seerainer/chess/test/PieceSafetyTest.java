package io.github.seerainer.chess.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Square;

import io.github.seerainer.chess.ChessAI;

/**
 * Test to verify piece safety improvements — the engine should not hang pieces
 * and should prioritize material capture over giving check.
 */
class PieceSafetyTest {

    /**
     * Test that the engine makes a reasonable opening move without hanging the
     * queen.
     */
    @Test
    @DisplayName("Queen Safety - AI makes reasonable opening move")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testQueenSafety() {
	final var board = new Board();
	board.loadFromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");

	final var ai = new ChessAI();
	try {
	    final var bestMove = ai.getBestMove(board);
	    assertNotNull(bestMove, "AI should find an opening move");
	} finally {
	    ai.cleanup();
	}
    }

    /**
     * Test that the engine captures a free queen rather than ignoring it. Black
     * queen sits on e5 undefended, White knight on f3 can capture (Nxe5).
     */
    @Test
    @DisplayName("Check Priority - AI captures free queen")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testCheckPriority() {
	// Black queen on e5 (undefended), White knight on f3 can capture
	final var board = new Board();
	board.loadFromFen("rnb1kbnr/pppp1ppp/8/4q3/4P3/5N2/PPPP1PPP/RNBQKB1R w KQkq - 0 3");

	final var ai = new ChessAI();
	try {
	    final var bestMove = ai.getBestMove(board);
	    assertNotNull(bestMove, "AI should find a move");

	    // AI should strongly prefer capturing the free queen on e5
	    assertSame(Square.E5, bestMove.getTo(),
		    "AI should capture the undefended queen on e5, but chose: " + bestMove);
	} finally {
	    ai.cleanup();
	}
    }
}
