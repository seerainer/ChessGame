package io.github.seerainer.chess.test;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.move.Move;

import io.github.seerainer.chess.ai.PositionEvaluator;

/**
 * Debug test to verify evaluation scoring
 */
public class EvaluationDebugTest {

	public static void main(final String[] args) {
		System.out.println("Testing evaluation scoring...");

		final var board = new Board();

		// Test initial position evaluation
		System.out.println("=== Initial Position ===");
		var whiteScore = PositionEvaluator.evaluateBoard(board, Side.WHITE);
		var blackScore = PositionEvaluator.evaluateBoard(board, Side.BLACK);
		System.out.println("White perspective: " + whiteScore);
		System.out.println("Black perspective: " + blackScore);

		// Test after knight development
		board.doMove(new Move("e2e4", Side.WHITE));
		board.doMove(new Move("e7e5", Side.BLACK));
		board.doMove(new Move("g1f3", Side.WHITE));

		System.out.println("\n=== After 1.e4 e5 2.Nf3 ===");
		System.out.println(board.toString());

		whiteScore = PositionEvaluator.evaluateBoard(board, Side.WHITE);
		blackScore = PositionEvaluator.evaluateBoard(board, Side.BLACK);
		System.out.println("White perspective: " + whiteScore);
		System.out.println("Black perspective: " + blackScore);

		// Test pawn move vs knight development for Black
		final var testBoard1 = new Board();
		testBoard1.loadFromFen(board.getFen());
		testBoard1.doMove(new Move("a7a6", Side.BLACK)); // Pawn move

		final var testBoard2 = new Board();
		testBoard2.loadFromFen(board.getFen());
		testBoard2.doMove(new Move("b8c6", Side.BLACK)); // Knight development

		System.out.println("\n=== After ...a6 (pawn move) ===");
		final var pawnMoveScore = PositionEvaluator.evaluateBoard(testBoard1, Side.BLACK);
		System.out.println("Black perspective after a6: " + pawnMoveScore);

		System.out.println("\n=== After ...Nc6 (knight development) ===");
		final var knightMoveScore = PositionEvaluator.evaluateBoard(testBoard2, Side.BLACK);
		System.out.println("Black perspective after Nc6: " + knightMoveScore);

		System.out.println("\n=== COMPARISON ===");
		System.out.println("Pawn move a6 score: " + pawnMoveScore);
		System.out.println("Knight development Nc6 score: " + knightMoveScore);
		System.out.println("Difference (should favor knight): " + (knightMoveScore - pawnMoveScore));

		if (knightMoveScore > pawnMoveScore) {
			System.out.println("✓ Evaluation correctly favors knight development!");
		} else {
			System.out.println("✗ Evaluation incorrectly favors pawn moves!");
		}
	}
}
