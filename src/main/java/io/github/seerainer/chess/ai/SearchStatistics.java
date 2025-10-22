package io.github.seerainer.chess.ai;

/**
 * Centralized statistics tracking for chess AI search operations
 */
public class SearchStatistics {
    // Futility pruning statistics
    private long futilityPrunes = 0;
    private long reverseFutilityPrunes = 0;
    private long razoringPrunes = 0;
    private long extendedFutilityPrunes = 0;
    private long probcutPrunes = 0;
    private long moveCountPrunes = 0;

    // PVS statistics
    private long pvsNodes = 0;
    private long pvsResearches = 0;
    private long pvsFailHigh = 0;
    private long pvsFailLow = 0;
    private long nullWindowSearches = 0;

    // Quiescence search statistics
    private long qNodes = 0;
    private long deltaPrunes = 0;
    private long qFutilityPrunes = 0;
    private long seePrunes = 0;
    private long qTTHits = 0;

    public String getFutilityStats() {
	return "Futility Stats: %d futility, %d reverse, %d razoring, %d extended, %d probcut, %d movecount".formatted(
		futilityPrunes, reverseFutilityPrunes, razoringPrunes, extendedFutilityPrunes, probcutPrunes,
		moveCountPrunes);
    }

    public String getPVSStats() {
	final var researchRate = pvsNodes > 0 ? (double) pvsResearches / pvsNodes * 100 : 0;
	return "PVS Stats: %d nodes, %d researches (%.1f%%), %d fail-high, %d fail-low, %d null-window"
		.formatted(pvsNodes, pvsResearches, researchRate, pvsFailHigh, pvsFailLow, nullWindowSearches);
    }

    public String getQuiescenceStats() {
	return "Quiescence Stats: %d nodes, %d delta prunes, %d futility prunes, %d SEE prunes, %d TT hits"
		.formatted(qNodes, deltaPrunes, qFutilityPrunes, seePrunes, qTTHits);
    }

    public void incrementProbcutPrunes() {
	probcutPrunes++;
    }

    // Quiescence statistics getters and incrementers
    public void incrementQNodes() {
	qNodes++;
    }

    public void incrementRazoringPrunes() {
	razoringPrunes++;
    }

    public void incrementReverseFutilityPrunes() {
	reverseFutilityPrunes++;
    }

    public void reset() {
	// Futility stats
	futilityPrunes = 0;
	reverseFutilityPrunes = 0;
	razoringPrunes = 0;
	extendedFutilityPrunes = 0;
	probcutPrunes = 0;
	moveCountPrunes = 0;

	// PVS stats
	pvsNodes = 0;
	pvsResearches = 0;
	pvsFailHigh = 0;
	pvsFailLow = 0;
	nullWindowSearches = 0;

	// Quiescence stats
	qNodes = 0;
	deltaPrunes = 0;
	qFutilityPrunes = 0;
	seePrunes = 0;
	qTTHits = 0;
    }
}