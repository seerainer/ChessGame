package io.github.seerainer.chess.test;

import com.github.bhlangonijr.chesslib.Board;

import io.github.seerainer.chess.ChessAI;

/**
 * Simple test to verify the AI makes good moves with the new evaluation system
 */
public class EvaluationTest {

	public static void main(final String[] args) {
		System.out.println("Testing AI with new evaluation system...");

		// Create a new board
		final var board = new Board();
		final var ai = new ChessAI();

		System.out.println("Starting position:");
		System.out.println(board.toString());

		// Play a few moves to see if AI develops pieces
		for (var i = 0; i < 10; i++) {
			if (board.isDraw() || board.isMated()) {
				break;
			}

			final var move = ai.getBestMove(board);
			if (move == null) {
				break;
			}

			System.out.println("Move " + (i + 1) + ": " + move.toString());
			System.out.println("From: " + move.getFrom() + " To: " + move.getTo());

			board.doMove(move);
			System.out.println("Position after move:");
			System.out.println(board.toString());
			System.out.println();
		}

		System.out.println("Test completed!");
	}
}
