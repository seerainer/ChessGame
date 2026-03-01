package io.github.seerainer.chess.test;

import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.move.Move;

import io.github.seerainer.chess.ai.PositionEvaluator;

/**
 * Tests that the evaluation function produces reasonable scores — symmetric in
 * the starting position and favoring piece development over passive pawn moves.
 */
class EvaluationDebugTest {

    /**
     * Verify that the starting position evaluation is roughly symmetric: the score
     * from White's perspective and Black's perspective should be close to each
     * other (both near zero).
     */
    @Test
    @DisplayName("Initial position evaluation is roughly symmetric")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testInitialPositionSymmetry() {
	final var board = new Board();

	final var whiteScore = PositionEvaluator.evaluateBoard(board, Side.WHITE);
	final var blackScore = PositionEvaluator.evaluateBoard(board, Side.BLACK);

	// The scores should be roughly symmetric (sum near zero, or at least within
	// reasonable range)
	final var asymmetry = Math.abs(whiteScore + blackScore);
	assertTrue(asymmetry < 200,
		new StringBuilder().append("Initial position evaluation should be roughly symmetric. White=")
			.append(whiteScore).append(" Black=").append(blackScore).append(" asymmetry=").append(asymmetry)
			.toString());
    }

    /**
     * After 1.e4 e5 2.Nf3, verify that Black's knight development (Nc6) is
     * evaluated better than a passive pawn move (a6).
     */
    @Test
    @DisplayName("Knight development preferred over passive pawn move")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testKnightDevelopmentPreferred() {
	final var board = new Board();
	board.doMove(new Move("e2e4", Side.WHITE));
	board.doMove(new Move("e7e5", Side.BLACK));
	board.doMove(new Move("g1f3", Side.WHITE));

	// Evaluate after ...a6 (passive pawn move)
	final var boardAfterA6 = new Board();
	boardAfterA6.loadFromFen(board.getFen());
	boardAfterA6.doMove(new Move("a7a6", Side.BLACK));
	final var pawnMoveScore = PositionEvaluator.evaluateBoard(boardAfterA6, Side.BLACK);

	// Evaluate after ...Nc6 (knight development)
	final var boardAfterNc6 = new Board();
	boardAfterNc6.loadFromFen(board.getFen());
	boardAfterNc6.doMove(new Move("b8c6", Side.BLACK));
	final var knightMoveScore = PositionEvaluator.evaluateBoard(boardAfterNc6, Side.BLACK);

	assertTrue(knightMoveScore > pawnMoveScore,
		new StringBuilder().append("Evaluation should favor Nc6 (knight development) over a6 (passive pawn). ")
			.append("Nc6=").append(knightMoveScore).append(" a6=").append(pawnMoveScore).toString());
    }
}
