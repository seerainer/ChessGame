package io.github.seerainer.chess.ai;

import com.github.bhlangonijr.chesslib.Board;

import io.github.seerainer.chess.config.ChessConfig;

/**
 * Enhanced time management class with memory monitoring for chess AI search
 */
public class TimeManager {
	// Time management constants - now using configuration values
	private static final long BASE_TIME_MS = ChessConfig.AI.DEFAULT_THINK_TIME_MS;
	private static final long CRITICAL_TIME_MS = ChessConfig.AI.DEFAULT_THINK_TIME_MS * 3; // 3x for critical positions
	private static final long OPENING_TIME_MS = ChessConfig.AI.DEFAULT_THINK_TIME_MS / 2; // Half time in opening
	private static final long ENDGAME_TIME_MS = ChessConfig.AI.DEFAULT_THINK_TIME_MS + 3000; // Extra time in endgame
	private static final int NODES_CHECK_INTERVAL = 2000;
	private static final double TIME_SAFETY_MARGIN = 0.95;

	/**
	 * Calculate appropriate time limit based on board position
	 */
	public static long calculateTimeLimit(final Board board) {
		if (board.isKingAttacked() || PositionEvaluator.isCriticalPosition(board)) {
			return CRITICAL_TIME_MS;
		}
		if (PositionEvaluator.isOpeningPhase(board)) {
			return OPENING_TIME_MS;
		}
		if (PositionEvaluator.isEndGame(board)) {
			return ENDGAME_TIME_MS;
		}
		return BASE_TIME_MS;
	}

	private long searchStartTime;
	private long timeLimit;

	private int nodesChecked;
	// Memory tracking for search termination
	private final Runtime runtime = Runtime.getRuntime();

	private long initialMemory;

	/**
	 * Check if memory pressure is too high during search
	 */
	private boolean isMemoryPressureHigh() {
		final var currentMemory = runtime.totalMemory() - runtime.freeMemory();
		final var memoryIncrease = currentMemory - initialMemory;
		final var maxMemory = runtime.maxMemory();

		// Stop search if memory usage increased significantly or total usage is very
		// high
		return memoryIncrease > 100 * 1024 * 1024 || // 100MB increase
				currentMemory > maxMemory * 0.9; // 90% of max memory
	}

	public boolean isTimeUp() {
		final var elapsed = System.currentTimeMillis() - searchStartTime;
		return elapsed >= timeLimit;
	}

	public boolean shouldStop() {
		if (++nodesChecked % NODES_CHECK_INTERVAL != 0) {
			return false;
		}

		final var elapsed = System.currentTimeMillis() - searchStartTime;

		// Check memory pressure
		if (isMemoryPressureHigh()) {
			return true;
		}

		return elapsed >= timeLimit * TIME_SAFETY_MARGIN;
	}

	public void startSearch(final long newTimeLimit) {
		this.searchStartTime = System.currentTimeMillis();
		this.timeLimit = newTimeLimit;
		this.nodesChecked = 0;
		this.initialMemory = runtime.totalMemory() - runtime.freeMemory();
	}
}