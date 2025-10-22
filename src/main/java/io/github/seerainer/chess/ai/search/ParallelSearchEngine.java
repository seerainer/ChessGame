package io.github.seerainer.chess.ai.search;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.move.Move;

import io.github.seerainer.chess.ai.MoveOrdering;
import io.github.seerainer.chess.ai.SearchAlgorithms;
import io.github.seerainer.chess.ai.TranspositionTable;
import io.github.seerainer.chess.ai.ZobristHashing;
import io.github.seerainer.chess.config.ChessConfig;

/**
 * Parallel search implementation using Lazy SMP (Shared Memory Parallel search)
 */
public class ParallelSearchEngine {
    private static final int PARALLEL_THRESHOLD = 4; // Minimum depth for parallel search
    private static final int SPLIT_THRESHOLD = 6; // Minimum moves to split search
    private final ExecutorService parallelExecutor;
    private final SearchAlgorithms searchAlgorithms;
    private final TranspositionTable sharedTT;
    private final AtomicInteger bestScore = new AtomicInteger(Integer.MIN_VALUE);
    private final AtomicReference<Move> bestMove = new AtomicReference<>();

    public ParallelSearchEngine(final ExecutorService parallelExecutor, final SearchAlgorithms searchAlgorithms,
	    final TranspositionTable transpositionTable) {
	this.parallelExecutor = parallelExecutor;
	this.searchAlgorithms = searchAlgorithms;
	this.sharedTT = transpositionTable;
    }

    /**
     * Collect results from parallel searches with timeout handling
     */
    private SearchResult collectParallelResults(final List<CompletableFuture<SearchResult>> futures, final int beta) {
	Move resultMove = null;
	var resultScore = Integer.MIN_VALUE;
	try {
	    // Wait for at least one result with reasonable timeout
	    var hasResult = false;
	    final var timeout = 3000L; // Reduced to 3 second timeout per thread

	    for (final var future : futures) {
		try {
		    final var result = future.get(timeout, TimeUnit.MILLISECONDS);
		    if (result != null && result.score > resultScore) {
			resultScore = result.score;
			resultMove = result.move;
			hasResult = true;

			// Early termination if we found a good enough move
			if (resultScore >= beta) {
			    // Cancel remaining searches
			    futures.forEach(f -> f.cancel(true));
			    break;
			}
		    }
		} catch (final java.util.concurrent.TimeoutException e) {
		    // Timeout - cancel this future and continue
		    future.cancel(true);
		} catch (final java.util.concurrent.CancellationException e) {
		    // Future was cancelled - continue with others
		    continue;
		} catch (final Exception e) {
		    // Continue with other threads - log specific error
		    System.err.println("Thread execution error: " + e.getMessage());
		    future.cancel(true);
		}
	    }

	    // Cancel any remaining futures
	    futures.forEach(f -> f.cancel(true));

	    // If no individual thread found a move, use shared best
	    if (!hasResult || resultMove == null) {
		resultMove = bestMove.get();
		resultScore = bestScore.get();
	    }
	} catch (final Exception e) {
	    System.err.println("Error collecting parallel search results: " + e.getMessage());
	    // Cancel all futures on error
	    futures.forEach(f -> f.cancel(true));
	}

	return new SearchResult(resultMove, resultScore);
    }

    /**
     * Lazy SMP implementation - each thread searches different moves
     */
    private SearchResult lazyMPSearch(final Board board, final List<Move> moves, final int depth, final int alpha,
	    final int beta) {
	// Order moves for better alpha-beta cutoffs
	final var zobristKey = ZobristHashing.calculateZobristHash(board);
	final var ttEntry = sharedTT.get(zobristKey);
	final var orderedMoves = MoveOrdering.orderMovesAdvanced(board, moves, depth, ttEntry);

	// Reset shared variables
	bestScore.set(Integer.MIN_VALUE);
	bestMove.set(null);

	// Calculate optimal thread distribution
	final var threadsToUse = Math.min(ChessConfig.AI.PARALLEL_SEARCH_THREADS, orderedMoves.size());
	final var movesPerThread = orderedMoves.size() / threadsToUse;

	final List<CompletableFuture<SearchResult>> futures = new ArrayList<>();

	// Launch parallel searches
	for (var threadIndex = 0; threadIndex < threadsToUse; threadIndex++) {
	    final var threadId = threadIndex;
	    final var startIndex = threadId * movesPerThread;
	    final var endIndex = (threadId == threadsToUse - 1) ? orderedMoves.size() : (threadId + 1) * movesPerThread;

	    final var threadMoves = orderedMoves.subList(startIndex, endIndex);

	    futures.add(CompletableFuture.supplyAsync(
		    () -> searchMovesRange(board, threadMoves, depth, alpha, beta, threadId), parallelExecutor));
	}

	// Collect results with early termination
	return collectParallelResults(futures, beta);
    }

    /**
     * Parallel search using Lazy SMP approach with better error handling
     */
    public SearchResult parallelSearch(final Board board, final int depth, final int alpha, final int beta) {
	if (depth < PARALLEL_THRESHOLD || parallelExecutor == null || parallelExecutor.isShutdown()) {
	    // Fall back to sequential search for shallow depths or if executor is
	    // unavailable
	    final var score = searchAlgorithms.minimax(board, depth, alpha, beta, true, false);
	    return new SearchResult(null, score);
	}

	final var legalMoves = board.legalMoves();
	if (legalMoves.isEmpty()) {
	    return new SearchResult(null, board.isKingAttacked() ? -30000 : 0);
	}

	if (legalMoves.size() < SPLIT_THRESHOLD) {
	    // Not enough moves to justify parallel search
	    return sequentialSearch(board, legalMoves, depth, alpha, beta);
	}

	try {
	    return lazyMPSearch(board, legalMoves, depth, alpha, beta);
	} catch (final Exception e) {
	    System.err.println("Error in parallel search, falling back to sequential: " + e.getMessage());
	    return sequentialSearch(board, legalMoves, depth, alpha, beta);
	}
    }

    /**
     * Search a range of moves in a single thread
     */
    private SearchResult searchMovesRange(final Board board, final List<Move> moves, final int depth, final int alpha,
	    final int beta, final int threadId) {
	Move localBestMove = null;
	var localBestScore = Integer.MIN_VALUE;
	var localAlpha = alpha;

	for (final var move : moves) {
	    // Check if search was cancelled or we found a better move in another thread
	    if (Thread.currentThread().isInterrupted() || bestScore.get() >= beta) {
		break;
	    }

	    try {
		// Create board copy for this thread
		final var boardCopy = new Board();
		boardCopy.loadFromFen(board.getFen());
		boardCopy.doMove(move);

		// Search with slight depth variations for different threads (Lazy SMP)
		var searchDepth = depth - 1;
		if (threadId > 0) {
		    // Vary depth slightly for helper threads
		    searchDepth += (threadId % 2 == 0) ? -1 : 0;
		    searchDepth = Math.max(1, searchDepth);
		}

		final var score = -searchAlgorithms.minimax(boardCopy, searchDepth, -beta, -localAlpha, false, true);

		// Update local best
		if (score > localBestScore) {
		    localBestScore = score;
		    localBestMove = move;

		    // Update shared best if better
		    updateSharedBest(move, score);

		    localAlpha = Math.max(localAlpha, score);
		    if (localAlpha >= beta) {
			break; // Beta cutoff
		    }
		}

	    } catch (final Exception e) {
		// Continue with next move if this one fails
		System.err.println(new StringBuilder().append("Error in parallel search thread ").append(threadId)
			.append(": ").append(e.getMessage()).toString());
	    }
	}

	return new SearchResult(localBestMove, localBestScore);
    }

    /**
     * Sequential search fallback
     */
    private SearchResult sequentialSearch(final Board board, final List<Move> moves, final int depth, int alpha,
	    final int beta) {
	Move newBestMove = null;
	var newBestScore = Integer.MIN_VALUE;

	for (final var move : moves) {
	    board.doMove(move);
	    final var score = -searchAlgorithms.minimax(board, depth - 1, -beta, -alpha, false, true);
	    board.undoMove();

	    if (score > newBestScore) {
		newBestScore = score;
		newBestMove = move;
		alpha = Math.max(alpha, score);

		if (alpha >= beta) {
		    break;
		}
	    }
	}

	return new SearchResult(newBestMove, newBestScore);
    }

    /**
     * Update shared best move and score atomically
     */
    private void updateSharedBest(final Move move, final int score) {
	var currentBest = bestScore.get();
	while (score > currentBest && !bestScore.compareAndSet(currentBest, score)) {
	    currentBest = bestScore.get();
	}

	if (score == bestScore.get()) {
	    bestMove.set(move);
	}
    }

    /**
     * Result container for search operations
     */
    public static class SearchResult {
	public final Move move;
	public final int score;

	public SearchResult(final Move move, final int score) {
	    this.move = move;
	    this.score = score;
	}
    }
}
