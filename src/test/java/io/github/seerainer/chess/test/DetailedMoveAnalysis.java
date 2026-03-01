package io.github.seerainer.chess.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.github.bhlangonijr.chesslib.Board;

import io.github.seerainer.chess.ChessAI;
import io.github.seerainer.chess.ai.SearchAlgorithms;
import io.github.seerainer.chess.ai.SearchStatistics;
import io.github.seerainer.chess.ai.TimeManager;
import io.github.seerainer.chess.ai.TranspositionTable;
import io.github.seerainer.chess.ai.search.AdvancedSearchEngine;

/**
 * Tests that the AdvancedSearchEngine can analyze specific moves at depth 3 and
 * returns a valid best move.
 */
class DetailedMoveAnalysis {

    /**
     * After 1.Nf3 a5, verify that the search engine can evaluate specific moves
     * (a2a3, b1c3, d2d4, e2e4) at depth 3 without exceptions, and that it returns a
     * non-null best move.
     */
    @Test
    @DisplayName("AdvancedSearchEngine analyzes moves after 1.Nf3 a5")
    @Timeout(value = 60, unit = TimeUnit.SECONDS)
    void testDetailedMoveAnalysis() {
	final var board = new Board();
	board.doMove("g1f3");
	board.doMove("a7a5");

	final var tt = new TranspositionTable(1024);
	final var stats = new SearchStatistics();
	final var timeManager = new TimeManager();
	final var searchAlgorithms = new SearchAlgorithms(tt, stats, timeManager);
	final var engine = new AdvancedSearchEngine(searchAlgorithms);

	final var legalMoves = board.legalMoves();
	assertTrue(legalMoves.size() > 4, "Position should have many legal moves");

	// Verify specific moves can be analysed without exceptions
	final String[] testMoveStrs = { "a2a3", "b1c3", "d2d4", "e2e4" };
	var analysedCount = 0;

	for (final var moveStr : testMoveStrs) {
	    for (final var move : legalMoves) {
		if (move.toString().equals(moveStr)) {
		    board.doMove(move);
		    final var score = -engine.search(board, 3, Integer.MIN_VALUE, Integer.MAX_VALUE, true);
		    board.undoMove();

		    assertTrue(score > Integer.MIN_VALUE && score < Integer.MAX_VALUE,
			    new StringBuilder().append("Score for ").append(moveStr).append(" should be finite, got: ")
				    .append(score).toString());
		    analysedCount++;
		    break;
		}
	    }
	}

	assertTrue(analysedCount >= 3,
		"Should be able to analyse at least 3 of the 4 target moves, got: " + analysedCount);

	// Verify ChessAI returns a valid move for this position
	final var ai = new ChessAI();
	try {
	    final var bestMove = ai.getBestMove(board);
	    assertNotNull(bestMove, "ChessAI should return a best move");
	} finally {
	    ai.cleanup();
	}
    }
}
