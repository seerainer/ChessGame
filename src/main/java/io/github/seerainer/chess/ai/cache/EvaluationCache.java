package io.github.seerainer.chess.ai.cache;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

import io.github.seerainer.chess.config.ChessConfig;

/**
 * Thread-safe LRU cache for position evaluations
 */
public class EvaluationCache {
	private static class CacheEntry {
		final int evaluation;
		final long timestamp;

		CacheEntry(final int evaluation) {
			this.evaluation = evaluation;
			this.timestamp = System.currentTimeMillis();
		}
	}

	private final ConcurrentHashMap<Long, CacheEntry> cache;
	private final AtomicInteger hitCount = new AtomicInteger(0);
	private final AtomicInteger missCount = new AtomicInteger(0);

	private final int maxSize;

	public EvaluationCache() {
		this.maxSize = ChessConfig.Performance.EVALUATION_CACHE_SIZE;
		this.cache = new ConcurrentHashMap<>(maxSize);
	}

	/**
	 * Constructor with custom size - now using configuration values
	 */
	public EvaluationCache(final int size) {
		this.maxSize = size;
		this.cache = new ConcurrentHashMap<>(maxSize);
	}

	/**
	 * Remove oldest entries when cache is full
	 */
	private void cleanupOldEntries() {
		final var cutoffTime = System.currentTimeMillis() - 60000; // Remove entries older than 1 minute

		cache.entrySet().removeIf(entry -> entry.getValue().timestamp < cutoffTime);

		// If still too full, remove 25% of entries
		if (cache.size() < maxSize * 0.9) {
			return;
		}
		final var removeCount = maxSize / 4;
		cache.entrySet().stream().sorted((e1, e2) -> Long.compare(e1.getValue().timestamp, e2.getValue().timestamp))
				.limit(removeCount).forEach(entry -> cache.remove(entry.getKey()));
	}

	/**
	 * Clear the cache
	 */
	public void clear() {
		cache.clear();
		hitCount.set(0);
		missCount.set(0);
	}

	/**
	 * Get evaluation from cache
	 */
	public Integer get(final long key) {
		final var entry = cache.get(key);
		if (entry != null) {
			hitCount.incrementAndGet();
			return entry.evaluation;
		}
		missCount.incrementAndGet();
		return null;
	}

	/**
	 * Get cache statistics
	 */
	public String getStatistics() {
		final var total = hitCount.get() + missCount.get();
		final var hitRate = total > 0 ? (double) hitCount.get() / total * 100 : 0;
		return "Evaluation Cache: %.1f%% hit rate (%d/%d), size: %d".formatted(hitRate, hitCount.get(), total,
				cache.size());
	}

	/**
	 * Put evaluation in cache
	 */
	public void put(final long key, final int evaluation) {
		if (cache.size() >= maxSize) {
			cleanupOldEntries();
		}
		cache.put(key, new CacheEntry(evaluation));
	}

	/**
	 * Get cache size
	 */
	public int size() {
		return cache.size();
	}
}