package io.github.seerainer.chess.config;

/**
 * Centralized configuration for the chess application
 */
public class ChessConfig {

	// AI Configuration
	public static final class AI {
		public static final int MAX_SEARCH_DEPTH = 6; // Optimized for speed: 6 plies for regular positions
		public static final int TACTICAL_SEARCH_DEPTH = 8; // Deeper search only for tactical positions
		public static final int ASPIRATION_WINDOW_SIZE = 25; // Reduced for more precision
		public static final int ASPIRATION_MIN_DEPTH = 4;
		public static final long DEFAULT_THINK_TIME_MS = 3000; // Reduced to 3 seconds for responsive play
		public static final long TACTICAL_THINK_TIME_MS = 5000; // 5 seconds for tactical positions
		public static final int TRANSPOSITION_TABLE_SIZE = 67108864; // Increased to 64MB
		public static final int TRANSPOSITION_TABLE_MAX_SIZE = 134217728; // 128MB
		public static final int OPENING_BOOK_MAX_MOVES = 20; // Longer opening book
		public static final boolean ENABLE_PARALLEL_SEARCH = true;
		public static final int PARALLEL_SEARCH_THREADS = Runtime.getRuntime().availableProcessors();
	}

	// Debug and Logging
	public static final class Debug {
		public static final boolean ENABLE_DEBUG_LOGGING = true; // Enabled for debugging
		public static final boolean ENABLE_STATISTICS = true;
		public static final boolean ENABLE_MOVE_VALIDATION = true;
		public static final boolean ENABLE_PERFORMANCE_MONITORING = true;
		public static final int STATISTICS_REPORT_INTERVAL = 1000; // Every 1000 moves
	}

	// Evaluation Configuration
	public static final class Evaluation {
		public static final int PIECE_VALUES_PAWN = 100;
		public static final int PIECE_VALUES_KNIGHT = 320;
		public static final int PIECE_VALUES_BISHOP = 330;
		public static final int PIECE_VALUES_ROOK = 500;
		public static final int PIECE_VALUES_QUEEN = 900;
		public static final int PIECE_VALUES_KING = 20000;

		public static final int MOBILITY_WEIGHT = 10;
		public static final int CENTER_CONTROL_WEIGHT = 15;
		public static final int KING_SAFETY_WEIGHT = 30;
		public static final int PAWN_STRUCTURE_WEIGHT = 25;
		public static final int ENDGAME_WEIGHT = 50;
	}

	// Memory Management
	public static final class Memory {
		public static final int POSITION_HISTORY_LIMIT = 200;
		public static final int CLEANUP_INTERVAL_MS = 30000; // 30 seconds
		public static final long MAX_MEMORY_USAGE_MB = 512;
		public static final long MEMORY_CHECK_INTERVAL_MS = 10000; // 10 seconds
		public static final float MEMORY_CLEANUP_THRESHOLD = 0.8f; // 80% memory usage
	}

	// Performance tuning
	public static final class Performance {
		public static final boolean ENABLE_EVALUATION_CACHE = true;
		public static final int EVALUATION_CACHE_SIZE = 1048576; // 1MB
		public static final boolean ENABLE_MOVE_ORDERING_CACHE = true;
		public static final int MOVE_ORDERING_CACHE_SIZE = 524288; // 512KB
		public static final boolean ENABLE_PAWN_HASH_TABLE = true;
		public static final int PAWN_HASH_TABLE_SIZE = 262144; // 256KB
	}

	// File paths and resources
//	public static final class Resources {
//		public static final String OPENING_BOOK_FILE = "opening_book.dat";
//		public static final String CONFIG_FILE = "chess_config.properties";
//		public static final String LOG_FILE = "chess_game.log";
//		public static final String STATS_FILE = "chess_stats.json";
//	}

	// Game Rules
	public static final class Rules {
		public static final int THREEFOLD_REPETITION_LIMIT = 3;
		public static final int FIFTY_MOVE_RULE_LIMIT = 50;
		public static final int INSUFFICIENT_MATERIAL_THRESHOLD = 500;
	}

	// Search Configuration
	public static final class Search {
		public static final int KILLER_MOVES_DEPTH = 32;
		public static final int KILLER_MOVES_SLOTS = 3;
		public static final int MAX_HISTORY_VALUE = 32768;
		public static final int HISTORY_AGING_THRESHOLD = 16384;
		public static final int FUTILITY_MARGIN = 200;
		public static final int RAZOR_MARGIN = 300;
		public static final int NULL_MOVE_REDUCTION = 3;
		public static final int LATE_MOVE_REDUCTION = 2;
		public static final int QUIESCENCE_MAX_DEPTH = 16;

		// Advanced search parameters
		public static final int NULL_MOVE_MIN_DEPTH = 3;
		public static final int RAZORING_MAX_DEPTH = 3;
		public static final int RAZORING_MARGIN = 400;
		public static final int MAX_EXTENSION = 2;
		public static final int SINGULAR_EXTENSION_MIN_DEPTH = 6;
		public static final int LMR_SKIP_MOVES = 3;
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
		public static final int MAX_MOVES_WITHOUT_PROGRESS = 100; // 50-move rule
		public static final int MAX_COMPUTER_MOVES = 200; // Safety limit
		public static final int AI_MOVE_DELAY_MS = 500; // Delay between AI moves
		public static final int THREAD_SHUTDOWN_TIMEOUT_MS = 5000;
		public static final String WINDOW_TITLE = "Chess Game";
		public static final int BOARD_SIZE = 568;
	}
}
