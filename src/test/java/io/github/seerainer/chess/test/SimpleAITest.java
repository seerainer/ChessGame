package io.github.seerainer.chess.test;

import com.github.bhlangonijr.chesslib.Board;

import io.github.seerainer.chess.ChessAI;

public class SimpleAITest {
    public static void main(final String[] args) {
	System.out.println("Testing if AI makes proper opening moves...");

	// Starting position
	final var board = new Board();
	final var ai = new ChessAI();

	// Play first 6 moves
	for (var i = 1; i <= 6; i++) {
	    System.out.println(new StringBuilder().append("\n=== Move ").append(i).append(" ===").toString());
	    System.out.println("Position: " + board.getFen());
	    System.out.println("To move: " + board.getSideToMove());

	    final var move = ai.getBestMove(board);
	    System.out.println("AI plays: " + move);

	    if (move == null) {
		System.out.println("No move found!");
		break;
	    }

	    board.doMove(move);
	    System.out.println("After move:");
	    System.out.println(board.toString());

	    // Stop if we see bad moves like early pawn advances
	    if (move.toString().matches("[a-h][2-7][a-h][3-6]") && i <= 3) {
		final var moveStr = move.toString();
		if (moveStr.startsWith("a2") || moveStr.startsWith("h2") || moveStr.startsWith("a7")
			|| moveStr.startsWith("h7")) {
		    System.out.println("*** WARNING: Bad opening move detected! ***");
		}
	    }
	}
    }
}
