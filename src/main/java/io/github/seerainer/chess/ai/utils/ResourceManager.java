package io.github.seerainer.chess.ai.utils;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import io.github.seerainer.chess.config.ChessConfig;

/**
 * Resource manager for proper cleanup of AI components
 */
public class ResourceManager implements AutoCloseable {
    private final ExecutorService executorService;
    private final ExecutorService parallelSearchExecutor;
    private volatile boolean closed = false;

    public ResourceManager() {
	this.executorService = Executors.newSingleThreadExecutor(r -> {
	    final var t = new Thread(r, "Chess-AI-Worker");
	    t.setDaemon(true);
	    return t;
	});

	// Create parallel search executor if enabled
	if (ChessConfig.AI.ENABLE_PARALLEL_SEARCH) {
	    this.parallelSearchExecutor = Executors.newFixedThreadPool(ChessConfig.AI.PARALLEL_SEARCH_THREADS, r -> {
		final var t = new Thread(r, "Chess-AI-Parallel-" + System.currentTimeMillis());
		t.setDaemon(true);
		return t;
	    });
	} else {
	    this.parallelSearchExecutor = null;
	}
    }

    @Override
    public void close() {
	if (closed) {
	    return;
	}

	closed = true;

	// Shutdown main executor
	executorService.shutdown();

	// Shutdown parallel search executor if present
	if (parallelSearchExecutor != null) {
	    parallelSearchExecutor.shutdown();
	}

	try {
	    if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
		System.err.println("AI threads did not terminate gracefully, forcing shutdown");
		executorService.shutdownNow();
		if (!executorService.awaitTermination(2, TimeUnit.SECONDS)) {
		    System.err.println("AI threads did not terminate after forced shutdown");
		}
	    }

	    if (parallelSearchExecutor != null && !parallelSearchExecutor.awaitTermination(5, TimeUnit.SECONDS)) {
		System.err.println("Parallel search threads did not terminate gracefully, forcing shutdown");
		parallelSearchExecutor.shutdownNow();
		if (!parallelSearchExecutor.awaitTermination(2, TimeUnit.SECONDS)) {
		    System.err.println("Parallel search threads did not terminate after forced shutdown");
		}
	    }
	} catch (final InterruptedException e) {
	    executorService.shutdownNow();
	    if (parallelSearchExecutor != null) {
		parallelSearchExecutor.shutdownNow();
	    }
	    Thread.currentThread().interrupt();
	}
    }

    public ExecutorService getExecutorService() {
	if (closed) {
	    throw new IllegalStateException("ResourceManager has been closed");
	}
	return executorService;
    }

    public boolean isClosed() {
	return closed;
    }
}
