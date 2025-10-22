package io.github.seerainer.chess.config;

/**
 * Centralized configuration for the chess application ENHANCED: Optimized
 * values based on performance analysis
 */
public class ChessConfig {

    // AI Configuration
    public static final class AI {
	public static final int MAX_SEARCH_DEPTH = 8; // IMPROVED: Increased from 6 for stronger play
	public static final int TACTICAL_SEARCH_DEPTH = 10; // IMPROVED: Deeper tactical search
	public static final int ASPIRATION_WINDOW_SIZE = 30; // IMPROVED: Optimized window size
	public static final int ASPIRATION_MIN_DEPTH = 4; // IMPROVED: Start aspiration earlier
	public static final long DEFAULT_THINK_TIME_MS = 3000; // IMPROVED: Better balance of speed/strength
	public static final int TRANSPOSITION_TABLE_SIZE = 134217728; // IMPROVED: 128MB default
	public static final int TRANSPOSITION_TABLE_MAX_SIZE = 268435456; // IMPROVED: 256MB max
	public static final int OPENING_BOOK_MAX_MOVES = 25; // IMPROVED: Extended opening coverage
	public static final boolean ENABLE_PARALLEL_SEARCH = true;
	public static final int PARALLEL_SEARCH_THREADS = Math.max(2, Runtime.getRuntime().availableProcessors() - 1); // IMPROVED:
														       // Reserve
														       // one
														       // core
														       // for
														       // UI

	// NEW: Advanced search parameters
	public static final int MIN_SEARCH_DEPTH = 4; // Minimum guaranteed search depth
	public static final int ENDGAME_SEARCH_DEPTH_BONUS = 2; // Extra depth in endgame
	public static final boolean ENABLE_ADAPTIVE_DEPTH = true; // Adjust depth based on position
    }

    // Debug and Logging
    public static final class Debug {
	public static final boolean ENABLE_DEBUG_LOGGING = false; // IMPROVED: Default off for production
	public static final boolean ENABLE_STATISTICS = true;
	public static final boolean ENABLE_PERFORMANCE_MONITORING = true;
	public static final int STATISTICS_REPORT_INTERVAL = 5000; // IMPROVED: Less frequent reporting
    }

    // Evaluation Configuration
    public static final class Evaluation {
	public static final int PIECE_VALUES_PAWN = 100;
	public static final int PIECE_VALUES_KNIGHT = 320;
	public static final int PIECE_VALUES_BISHOP = 330;
	public static final int PIECE_VALUES_ROOK = 500;
	public static final int PIECE_VALUES_QUEEN = 900;
	public static final int PIECE_VALUES_KING = 20000; // IMPROVED: Higher to prevent king trades
    }

    // Memory Management
    public static final class Memory {
	public static final int POSITION_HISTORY_LIMIT = 500; // IMPROVED: Increased from 200
	public static final int CLEANUP_INTERVAL_MS = 60000; // IMPROVED: 60 seconds for less overhead
	public static final long MAX_MEMORY_USAGE_MB = 768; // IMPROVED: Increased from 512MB
	public static final long MEMORY_CHECK_INTERVAL_MS = 15000; // IMPROVED: 15 seconds
	public static final float MEMORY_CLEANUP_THRESHOLD = 0.85f; // IMPROVED: Higher threshold
    }

    // Performance tuning
    public static final class Performance {
	public static final boolean ENABLE_EVALUATION_CACHE = true;
	public static final int EVALUATION_CACHE_SIZE = 2097152; // IMPROVED: 2MB cache
    }

    // File paths and resources
    // public static final class Resources {
    // public static final String OPENING_BOOK_FILE = "opening_book.dat";
    // public static final String CONFIG_FILE = "chess_config.properties";
    // public static final String LOG_FILE = "chess_game.log";
    // public static final String STATS_FILE = "chess_stats.json";
    // }

    // Game Rules
    public static final class Rules {
	public static final int THREEFOLD_REPETITION_LIMIT = 3;
    }

    // Search Configuration
    public static final class Search {
	public static final int KILLER_MOVES_DEPTH = 64; // IMPROVED: Doubled from 32
	public static final int KILLER_MOVES_SLOTS = 4; // IMPROVED: Increased from 3
	public static final int MAX_HISTORY_VALUE = 65536; // IMPROVED: Doubled for better resolution
	public static final int HISTORY_AGING_THRESHOLD = 32768;
	public static final int NULL_MOVE_REDUCTION = 3;
	public static final int QUIESCENCE_MAX_DEPTH = 20; // IMPROVED: Increased from 16

	// Advanced search parameters
	public static final int NULL_MOVE_MIN_DEPTH = 3;
	public static final int RAZORING_MAX_DEPTH = 3;
	public static final int RAZORING_MARGIN = 400;
	public static final int MAX_EXTENSION = 3; // IMPROVED: Increased from 2
	public static final int SINGULAR_EXTENSION_MIN_DEPTH = 6;
	public static final int LMR_SKIP_MOVES = 4; // IMPROVED: Increased from 3
	public static final int LMR_MIN_DEPTH = 3;
	public static final int LMR_AGGRESSIVE_THRESHOLD = 6;
	public static final int LMR_DEPTH_THRESHOLD = 6;
	public static final int LMR_MAX_REDUCTION = 3;
	public static final int LMP_MAX_DEPTH = 3;
	public static final int LMP_MOVE_THRESHOLD = 8;
	public static final int MATE_SCORE_THRESHOLD = 10000;
    }

    // UI Configuration
    public static final class UI {
	public static final int MAX_MOVES_WITHOUT_PROGRESS = 100;
	public static final int MAX_COMPUTER_MOVES = 300; // IMPROVED: Increased from 200
	public static final int AI_MOVE_DELAY_MS = 300; // IMPROVED: Faster response from 500ms
	public static final int THREAD_SHUTDOWN_TIMEOUT_MS = 5000;
	public static final String WINDOW_TITLE = "Chess Game - Advanced AI Engine";
	public static final int BOARD_SIZE = 568;
    }
}