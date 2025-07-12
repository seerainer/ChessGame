package io.github.seerainer.chess.test;

import com.github.bhlangonijr.chesslib.Board;

import io.github.seerainer.chess.ai.SearchAlgorithms;
import io.github.seerainer.chess.ai.SearchStatistics;
import io.github.seerainer.chess.ai.TimeManager;
import io.github.seerainer.chess.ai.TranspositionTable;
import io.github.seerainer.chess.ai.search.AdvancedSearchEngine;

public class DetailedMoveAnalysis {
	public static void main(final String[] args) {
		System.out.println("Detailed analysis of move scores after search...");

		// Position after 1.Nf3 a5
		final var board = new Board();
		board.doMove("g1f3");
		board.doMove("a7a5");

		System.out.println("Position: " + board.getFen());

		// Create search engine
		final var tt = new TranspositionTable(1024);
		final var stats = new SearchStatistics();
		final var timeManager = new TimeManager();
		final var searchAlgorithms = new SearchAlgorithms(tt, stats, timeManager);
		final var engine = new AdvancedSearchEngine(searchAlgorithms);

		final var moves = board.legalMoves();
		System.out.println("\nAnalyzing " + moves.size() + " moves at depth 3:");

		// Test specific moves we care about
		final String[] testMoves = { "a2a3", "b1c3", "d2d4", "e2e4" };

		for (final var moveStr : testMoves) {
			for (final var move : moves) {
				if (move.toString().equals(moveStr)) {
					board.doMove(move);
					final var score = -engine.search(board, 3, Integer.MIN_VALUE, Integer.MAX_VALUE, true);
					board.undoMove();
					System.out.printf("%s: search score = %d%n", moveStr, score);
					break;
				}
			}
		}

		// Now use getBestMove to see what it actually selects
		System.out.println("\n=== AdvancedSearchEngine.getBestMove ===");
		final var bestMove = engine.getBestMove(board, 3, Integer.MIN_VALUE, Integer.MAX_VALUE);
		System.out.println("Best move selected: " + bestMove);
	}
}
