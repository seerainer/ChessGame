package io.github.seerainer.chess.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.github.bhlangonijr.chesslib.Board;

import io.github.seerainer.chess.ChessAI;

/**
 * Tests that the AI makes reasonable opening moves — no early edge-pawn pushes,
 * and can play a full 6-move sequence without returning null.
 */
class SimpleAITest {

    /**
     * Play 6 consecutive moves from the starting position and verify all moves are
     * non-null and that early moves are not dubious edge-pawn pushes (a2a3/a2a4,
     * h2h3/h2h4, a7a6/a7a5, h7h6/h7h5).
     */
    @Test
    @DisplayName("AI plays 6 reasonable opening moves")
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void testOpeningMoveQuality() {
	final var board = new Board();
	final var ai = new ChessAI();

	try {
	    for (var i = 1; i <= 6; i++) {
		final var move = ai.getBestMove(board);
		assertNotNull(move, "AI should find a move at move " + i);

		// For the first 3 half-moves, warn about bad edge-pawn pushes
		if (i <= 3) {
		    final var moveStr = move.toString();
		    final var isBadEdgePush = moveStr.startsWith("a2") || moveStr.startsWith("h2")
			    || moveStr.startsWith("a7") || moveStr.startsWith("h7");
		    assertTrue(!isBadEdgePush, new StringBuilder().append("Move ").append(i)
			    .append(" should not be an early edge-pawn push, but was: ").append(moveStr).toString());
		}

		board.doMove(move);
	    }
	} finally {
	    ai.cleanup();
	}
    }
}
