package io.github.seerainer.chess.ai;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Centralized statistics tracking for chess AI search operations
 */
public class SearchStatistics {
    // Total node count
    private final AtomicLong nodesSearched = new AtomicLong(0);

    // Futility pruning statistics
    private final AtomicLong futilityPrunes = new AtomicLong(0);
    private final AtomicLong reverseFutilityPrunes = new AtomicLong(0);
    private final AtomicLong razoringPrunes = new AtomicLong(0);
    private final AtomicLong extendedFutilityPrunes = new AtomicLong(0);
    private final AtomicLong probcutPrunes = new AtomicLong(0);
    private final AtomicLong moveCountPrunes = new AtomicLong(0);

    // PVS statistics
    private final AtomicLong pvsNodes = new AtomicLong(0);
    private final AtomicLong pvsResearches = new AtomicLong(0);
    private final AtomicLong pvsFailHigh = new AtomicLong(0);
    private final AtomicLong pvsFailLow = new AtomicLong(0);
    private final AtomicLong nullWindowSearches = new AtomicLong(0);

    // Quiescence search statistics
    private final AtomicLong qNodes = new AtomicLong(0);
    private final AtomicLong deltaPrunes = new AtomicLong(0);
    private final AtomicLong qFutilityPrunes = new AtomicLong(0);
    private final AtomicLong seePrunes = new AtomicLong(0);
    private final AtomicLong qTTHits = new AtomicLong(0);

    public String getFutilityStats() {
	return "Futility Stats: %d futility, %d reverse, %d razoring, %d extended, %d probcut, %d movecount".formatted(
		futilityPrunes.get(), reverseFutilityPrunes.get(), razoringPrunes.get(), extendedFutilityPrunes.get(),
		probcutPrunes.get(), moveCountPrunes.get());
    }

    public String getPVSStats() {
	final var pvsNodesVal = pvsNodes.get();
	final var researchRate = pvsNodesVal > 0 ? (double) pvsResearches.get() / pvsNodesVal * 100 : 0;
	return "PVS Stats: %d nodes, %d researches (%.1f%%), %d fail-high, %d fail-low, %d null-window"
		.formatted(pvsNodesVal, pvsResearches.get(), researchRate, pvsFailHigh.get(), pvsFailLow.get(),
			nullWindowSearches.get());
    }

    public String getQuiescenceStats() {
	return "Quiescence Stats: %d nodes, %d delta prunes, %d futility prunes, %d SEE prunes, %d TT hits"
		.formatted(qNodes.get(), deltaPrunes.get(), qFutilityPrunes.get(), seePrunes.get(), qTTHits.get());
    }

    public long getNodesSearched() {
	return nodesSearched.get();
    }

    public void incrementNodes() {
	nodesSearched.incrementAndGet();
    }

    public void incrementProbcutPrunes() {
	probcutPrunes.incrementAndGet();
    }

    // Quiescence statistics getters and incrementers
    public void incrementQNodes() {
	qNodes.incrementAndGet();
    }

    public void incrementRazoringPrunes() {
	razoringPrunes.incrementAndGet();
    }

    public void incrementReverseFutilityPrunes() {
	reverseFutilityPrunes.incrementAndGet();
    }

    public void reset() {
	// Total nodes
	nodesSearched.set(0);

	// Futility stats
	futilityPrunes.set(0);
	reverseFutilityPrunes.set(0);
	razoringPrunes.set(0);
	extendedFutilityPrunes.set(0);
	probcutPrunes.set(0);
	moveCountPrunes.set(0);

	// PVS stats
	pvsNodes.set(0);
	pvsResearches.set(0);
	pvsFailHigh.set(0);
	pvsFailLow.set(0);
	nullWindowSearches.set(0);

	// Quiescence stats
	qNodes.set(0);
	deltaPrunes.set(0);
	qFutilityPrunes.set(0);
	seePrunes.set(0);
	qTTHits.set(0);
    }
}