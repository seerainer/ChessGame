package io.github.seerainer.chess.test;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Side;

import io.github.seerainer.chess.ai.PositionEvaluator;
import io.github.seerainer.chess.ai.SearchAlgorithms;
import io.github.seerainer.chess.ai.SearchStatistics;
import io.github.seerainer.chess.ai.TimeManager;
import io.github.seerainer.chess.ai.TranspositionTable;
import io.github.seerainer.chess.ai.search.AdvancedSearchEngine;

/**
 * Simple debug test to trace exactly what the search algorithm is doing
 */
public class SearchDebugTest {
	public static void main(final String[] args) {
		System.out.println("=== Search Algorithm Debug Test ===");

		final var board = new Board();

		// Test direct evaluation of knight moves vs pawn moves
		System.out.println("Testing direct evaluation:");

		// Test knight move Nf3
		board.doMove("g1f3");
		final var knightMoveScore = PositionEvaluator.evaluateBoard(board, Side.WHITE);
		System.out.println("After Nf3: " + knightMoveScore);
		board.undoMove();

		// Test pawn move a2a3
		board.doMove("a2a3");
		final var pawnMoveScore = PositionEvaluator.evaluateBoard(board, Side.WHITE);
		System.out.println("After a2a3: " + pawnMoveScore);
		board.undoMove();

		System.out.println("Difference (knight - pawn): " + (knightMoveScore - pawnMoveScore));

		// Now test what the AdvancedSearchEngine returns for these moves
		System.out.println("\nTesting AdvancedSearchEngine search scores:");

		final var transpositionTable = new TranspositionTable(1024);
		final var searchStatistics = new SearchStatistics();
		final var timeManager = new TimeManager();
		final var searchAlgorithms = new SearchAlgorithms(transpositionTable, searchStatistics, timeManager);
		final var advancedSearchEngine = new AdvancedSearchEngine(searchAlgorithms);

		// Test knight move with search
		board.doMove("g1f3");
		final var knightSearchScore = advancedSearchEngine.search(board, 1, -50000, 50000, true);
		System.out.println("AdvancedSearch after Nf3 (maximizing=true): " + knightSearchScore);
		board.undoMove();

		// Test pawn move with search
		board.doMove("a2a3");
		final var pawnSearchScore = advancedSearchEngine.search(board, 1, -50000, 50000, true);
		System.out.println("AdvancedSearch after a2a3 (maximizing=true): " + pawnSearchScore);
		board.undoMove();

		System.out.println("Search difference (knight - pawn): " + (knightSearchScore - pawnSearchScore));

		// Now test getBestMove
		System.out.println("\nTesting getBestMove:");
		final var bestMove = advancedSearchEngine.getBestMove(board, 2, -50000, 50000);
		System.out.println("Best move found: " + bestMove);
	}
}
