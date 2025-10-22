package io.github.seerainer.chess.test;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.move.MoveGenerator;

import io.github.seerainer.chess.ai.PositionEvaluator;

/**
 * Debug test to show actual evaluation scores for each possible move
 */
public class MoveEvaluationDebugTest {
    public static void main(final String[] args) {
	System.out.println("=== Move Evaluation Debug Test ===");

	// Create a board in the opening position
	final var board = new Board();

	System.out.println("Starting position evaluation: " + PositionEvaluator.evaluateBoard(board, Side.WHITE));
	System.out.println("Available moves and their resulting evaluations:");

	final var moves = MoveGenerator.generateLegalMoves(board);

	moves.forEach((final var move) -> {
	    board.doMove(move);
	    final var score = PositionEvaluator.evaluateBoard(board, Side.WHITE);
	    board.undoMove();

	    System.out.printf("Move: %-8s -> Score: %6d%n", move, score);
	});

	System.out.println("\n=== Testing a few moves in ===");

	// Play d2-d4 and see what Black should do
	board.doMove("d2d4");
	System.out.println("\nAfter 1.d4, Black's best options:");
	final var blackMoves = MoveGenerator.generateLegalMoves(board);

	for (var i = 0; i < Math.min(10, blackMoves.size()); i++) {
	    final var move = blackMoves.get(i);
	    board.doMove(move);
	    final var score = PositionEvaluator.evaluateBoard(board, Side.BLACK);
	    board.undoMove();

	    System.out.printf("Move: %-8s -> Score: %6d%n", move, score);
	}

	// Now play Nf6 and see what White should do
	board.doMove("g8f6");
	System.out.println("\nAfter 1.d4 Nf6, White's best options:");
	final var whiteMoves = MoveGenerator.generateLegalMoves(board);

	for (var i = 0; i < Math.min(10, whiteMoves.size()); i++) {
	    final var move = whiteMoves.get(i);
	    board.doMove(move);
	    final var score = PositionEvaluator.evaluateBoard(board, Side.WHITE);
	    board.undoMove();

	    System.out.printf("Move: %-8s -> Score: %6d%n", move, score);
	}
    }
}
