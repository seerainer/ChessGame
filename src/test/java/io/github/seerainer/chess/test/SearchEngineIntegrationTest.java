package io.github.seerainer.chess.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.github.bhlangonijr.chesslib.Board;

import io.github.seerainer.chess.ai.SearchAlgorithms;
import io.github.seerainer.chess.ai.SearchStatistics;
import io.github.seerainer.chess.ai.TimeManager;
import io.github.seerainer.chess.ai.TranspositionTable;
import io.github.seerainer.chess.ai.search.AdvancedSearchEngine;
import io.github.seerainer.chess.ai.search.OptimizedQuiescenceSearch;
import io.github.seerainer.chess.ai.search.ParallelSearchEngine;
import io.github.seerainer.chess.ai.utils.ResourceManager;

/**
 * Integration test for all three search engines
 */
public class SearchEngineIntegrationTest {

	@Test
	@DisplayName("Test AdvancedSearchEngine")
	@Timeout(value = 30, unit = TimeUnit.SECONDS)
	static void testAdvancedSearchEngine() {
		System.out.println("\n=== Testing AdvancedSearchEngine ===");

		final var tt = new TranspositionTable();
		final var stats = new SearchStatistics();
		final var timeManager = new TimeManager();
		final var searchAlgorithms = new SearchAlgorithms(tt, stats, timeManager);

		final var engine = new AdvancedSearchEngine(searchAlgorithms);
		final var board = new Board();

		// Test search functionality
		final var move = engine.getBestMove(board, 4, -1000, 1000);
		assertNotNull(move, "AdvancedSearchEngine should find a move");
		assertNotNull(engine.getStatistics(), "Search statistics should not be null");

		System.out.println("AdvancedSearchEngine found move: " + move);
		System.out.println("Search statistics: " + engine.getStatistics());
		System.out.println("✓ AdvancedSearchEngine works");
	}

	@Test
	@DisplayName("Test OptimizedQuiescenceSearch")
	@Timeout(value = 30, unit = TimeUnit.SECONDS)
	static void testOptimizedQuiescenceSearch() {
		System.out.println("\n=== Testing OptimizedQuiescenceSearch ===");

		final var board = new Board();

		// Reset statistics
		OptimizedQuiescenceSearch.resetStatistics();
		OptimizedQuiescenceSearch.resetNodeCount();

		// Test quiescence search
		final var score = OptimizedQuiescenceSearch.quiescenceSearch(board, -1000, 1000, 0);
		assertNotNull(score, "Quiescence search should return a score");
		assertNotNull(OptimizedQuiescenceSearch.getStatistics(), "Quiescence statistics should not be null");

		System.out.println("Quiescence search score: " + score);
		System.out.println("Quiescence statistics: " + OptimizedQuiescenceSearch.getStatistics());

		// Test with checks
		final var scoreWithChecks = OptimizedQuiescenceSearch.quiescenceSearchWithChecks(board, -1000, 1000, 0);
		assertNotNull(scoreWithChecks, "Quiescence search with checks should return a score");

		System.out.println("Quiescence with checks score: " + scoreWithChecks);
		System.out.println("✓ OptimizedQuiescenceSearch works");
	}

	@Test
	@DisplayName("Test ParallelSearchEngine")
	@Timeout(value = 30, unit = TimeUnit.SECONDS)
	static void testParallelSearchEngine() {
		System.out.println("\n=== Testing ParallelSearchEngine ===");

		try (final var resourceManager = new ResourceManager()) {
			final var tt = new TranspositionTable();
			final var stats = new SearchStatistics();
			final var timeManager = new TimeManager();
			final var searchAlgorithms = new SearchAlgorithms(tt, stats, timeManager);

			final var engine = new ParallelSearchEngine(resourceManager.getExecutorService(), searchAlgorithms, tt);
			final var board = new Board();

			// Test parallel search
			final var result = engine.parallelSearch(board, 4, -1000, 1000);
			assertNotNull(result, "Parallel search should return a result");
			assertNotNull(result.move, "Parallel search result should contain a move");

			System.out.println("Parallel search result: move=" + result.move + ", score=" + result.score);
			System.out.println("✓ ParallelSearchEngine works");
		}
	}
}