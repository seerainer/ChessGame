package io.github.seerainer.chess.test;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Side;

import io.github.seerainer.chess.ChessAI;
import io.github.seerainer.chess.ai.evaluation.EvaluationOrchestrator;

public class DebugMoveSelection {
    public static void main(final String[] args) {
	System.out.println("Debugging move selection after 1.Nf3 a5...");

	// Position after 1.Nf3 a5
	final var board = new Board();
	board.doMove("g1f3");
	board.doMove("a7a5");

	System.out.println("Current position:");
	System.out.println(board.toString());
	System.out.println("Side to move: " + board.getSideToMove());

	// Get all legal moves
	final var legalMoves = board.legalMoves();
	System.out.println(
		new StringBuilder().append("\nLegal moves (").append(legalMoves.size()).append("):").toString());

	final var evaluator = new EvaluationOrchestrator();

	// Evaluate each move
	legalMoves.forEach((final var move) -> {
	    board.doMove(move);
	    final var score = evaluator.evaluatePosition(board, Side.WHITE);
	    board.undoMove();

	    System.out.printf("%s: %d%n", move.toString(), score);
	});

	// Now test AI selection
	System.out.println("\n=== AI Move Selection ===");
	final var ai = new ChessAI();
	final var selectedMove = ai.getBestMove(board); // AI selection
	System.out.println("AI selected: " + selectedMove);

	// Show evaluation breakdown for this position
	System.out.println("\n=== Position Evaluation Breakdown ===");
	System.out.println(evaluator.getEvaluationBreakdown(board, Side.WHITE));
    }
}
