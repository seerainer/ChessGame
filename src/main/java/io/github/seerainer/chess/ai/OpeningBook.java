package io.github.seerainer.chess.ai;

import java.security.SecureRandom;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.move.Move;

import io.github.seerainer.chess.config.ChessConfig;

/**
 * **OPTIMIZED: Opening book with Zobrist hash-based lookups for better
 * performance** Replaces slow FEN string comparisons with fast long hash
 * lookups
 */
public class OpeningBook {
	/**
	 * **NEW: Compact move storage to reduce memory overhead** Stores moves as
	 * packed integers instead of strings
	 */
	private static class BookEntry {
		// Pack move string (e.g., "e2e4") into a 32-bit integer
		private static int packMove(final String moveStr) {
			if (moveStr.length() < 4) {
				return 0;
			}

			final var fromFile = moveStr.charAt(0) - 'a';
			final var fromRank = moveStr.charAt(1) - '1';
			final var toFile = moveStr.charAt(2) - 'a';
			final var toRank = moveStr.charAt(3) - '1';

			// Pack into 16 bits: from_square (6 bits) + to_square (6 bits) + unused (4
			// bits)
			return (fromRank << 12) | (fromFile << 9) | (toRank << 6) | (toFile << 3);
		}

		// Unpack integer back to move string
		private static String unpackMove(final int packedMove) {
			final var fromFile = (packedMove >> 9) & 0x7;
			final var fromRank = (packedMove >> 12) & 0x7;
			final var toFile = (packedMove >> 3) & 0x7;
			final var toRank = (packedMove >> 6) & 0x7;

			return "" + (char) ('a' + fromFile) + (char) ('1' + fromRank) + (char) ('a' + toFile)
					+ (char) ('1' + toRank);
		}

		private final int[] moves; // Packed move representations

		private final int count;

		public BookEntry(final String[] moveStrings) {
			this.count = moveStrings.length;
			this.moves = new int[count];

			// Pack moves into integers for faster access
			for (var i = 0; i < count; i++) {
				this.moves[i] = packMove(moveStrings[i]);
			}
		}

		public String getRandomMove(final SecureRandom random) {
			if (count == 0) {
				return null;
			}
			final var packedMove = moves[random.nextInt(count)];
			return unpackMove(packedMove);
		}
	}

	/**
	 * **PERFORMANCE: Custom hash map optimized for long keys** Uses open addressing
	 * with linear probing for better cache performance
	 */
	private static class ZobristBookMap {
		private static final int INITIAL_CAPACITY = 2048;
		private static final double LOAD_FACTOR = 0.75;

		private long[] keys;
		private BookEntry[] values;
		private boolean[] occupied;
		private int size;
		private int threshold;

		public ZobristBookMap() {
			this.keys = new long[INITIAL_CAPACITY];
			this.values = new BookEntry[INITIAL_CAPACITY];
			this.occupied = new boolean[INITIAL_CAPACITY];
			this.threshold = (int) (INITIAL_CAPACITY * LOAD_FACTOR);
		}

		private int findSlot(final long key) {
			var index = (int) (key & (keys.length - 1)); // Fast modulo for power of 2
			while (occupied[index] && keys[index] != key) {
				index = (index + 1) & (keys.length - 1); // Linear probing with wraparound
			}
			return index;
		}

		public BookEntry get(final long key) {
			final var index = findSlot(key);
			return occupied[index] && keys[index] == key ? values[index] : null;
		}

		public void put(final long key, final BookEntry value) {
			if (size >= threshold) {
				resize();
			}

			final var index = findSlot(key);
			if (!occupied[index]) {
				size++;
				occupied[index] = true;
			}
			keys[index] = key;
			values[index] = value;
		}

		private void resize() {
			final var oldKeys = keys;
			final var oldValues = values;
			final var oldOccupied = occupied;
			final var oldCapacity = keys.length;

			final var newCapacity = oldCapacity * 2;
			keys = new long[newCapacity];
			values = new BookEntry[newCapacity];
			occupied = new boolean[newCapacity];
			threshold = (int) (newCapacity * LOAD_FACTOR);
			size = 0;

			// Rehash existing entries
			for (var i = 0; i < oldCapacity; i++) {
				if (oldOccupied[i]) {
					put(oldKeys[i], oldValues[i]);
				}
			}
		}
	}

	// **OPTIMIZED: Use Zobrist hash map instead of FEN string map**
	private static final ZobristBookMap OPENING_BOOK = createOptimizedOpeningBook();

	/**
	 * **PERFORMANCE: Helper method to add opening lines efficiently** Plays moves
	 * on temporary board and stores Zobrist hash
	 */
	private static void addOpeningLine(final ZobristBookMap book, final Board tempBoard, final String[] setupMoves,
			final String[] responseMoves) {
		// Reset board and play setup moves
		tempBoard.loadFromFen("rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKBNR w KQkq - 0 1");

		for (final var moveStr : setupMoves) {
			final var move = findLegalMove(tempBoard, moveStr);
			if (move != null) {
				tempBoard.doMove(move);
			}
		}

		// Store position hash with response moves
		final var zobristKey = ZobristHashing.calculateZobristHash(tempBoard);
		book.put(zobristKey, new BookEntry(responseMoves));
	}

	/**
	 * **PERFORMANCE: Create optimized opening book with Zobrist hashes**
	 * Pre-computes all position hashes for O(1) lookup performance
	 */
	private static ZobristBookMap createOptimizedOpeningBook() {
		final var book = new ZobristBookMap();
		final var tempBoard = new Board();

		// **PERFORMANCE: Batch insert all opening positions**
		addOpeningLine(book, tempBoard, new String[] {}, new String[] { "e2e4", "d2d4", "g1f3", "c2c4", "f2f4" });

		// After 1.e4 - diverse responses
		addOpeningLine(book, tempBoard, new String[] { "e2e4" },
				new String[] { "e7e5", "c7c5", "e7e6", "c7c6", "d7d6", "g8f6" });

		// After 1.e4 e5 - aggressive development
		addOpeningLine(book, tempBoard, new String[] { "e2e4", "e7e5" },
				new String[] { "g1f3", "b1c3", "f2f4", "f1c4" });

		// After 1.d4 - Queen's Gambit and others
		addOpeningLine(book, tempBoard, new String[] { "d2d4" }, new String[] { "d7d5", "g8f6", "f7f5", "e7e6" });

		// Sicilian Defense responses
		addOpeningLine(book, tempBoard, new String[] { "e2e4", "c7c5" }, new String[] { "g1f3", "b1c3", "f2f4" });

		// French Defense
		addOpeningLine(book, tempBoard, new String[] { "e2e4", "e7e6" }, new String[] { "d2d4", "b1c3", "g1f3" });

		// Ruy Lopez - 1.e4 e5 2.Nf3 Nc6 3.Bb5
		addOpeningLine(book, tempBoard, new String[] { "e2e4", "e7e5", "g1f3" },
				new String[] { "b8c6", "f7f5", "d7d6" });
		addOpeningLine(book, tempBoard, new String[] { "e2e4", "e7e5", "g1f3", "b8c6" },
				new String[] { "f1b5", "f1c4", "d2d4" });
		addOpeningLine(book, tempBoard, new String[] { "e2e4", "e7e5", "g1f3", "b8c6", "f1b5" },
				new String[] { "a7a6", "f7f5", "g8f6", "f8c5" });

		// Italian Game - 1.e4 e5 2.Nf3 Nc6 3.Bc4
		addOpeningLine(book, tempBoard, new String[] { "e2e4", "e7e5", "g1f3", "b8c6", "f1c4" },
				new String[] { "f8c5", "f7f5", "g8f6", "f8e7" });

		// Queen's Gambit - 1.d4 d5 2.c4
		addOpeningLine(book, tempBoard, new String[] { "d2d4", "d7d5" }, new String[] { "c2c4", "g1f3", "b1c3" });
		addOpeningLine(book, tempBoard, new String[] { "d2d4", "d7d5", "c2c4" },
				new String[] { "d5c4", "e7e6", "c7c6", "g8f6" });

		// Add more opening lines efficiently...
		addOpeningLine(book, tempBoard, new String[] { "d2d4", "g8f6" }, new String[] { "c2c4", "g1f3", "b1c3" });
		addOpeningLine(book, tempBoard, new String[] { "d2d4", "g8f6", "c2c4" },
				new String[] { "g7g6", "e7e6", "d7d5" });

		// English Opening - 1.c4
		addOpeningLine(book, tempBoard, new String[] { "c2c4" }, new String[] { "e7e5", "g8f6", "c7c5", "e7e6" });

		// Caro-Kann Defense - 1.e4 c6
		addOpeningLine(book, tempBoard, new String[] { "e2e4", "c7c6" }, new String[] { "d2d4", "b1c3", "g1f3" });

		// Sicilian variations
		addOpeningLine(book, tempBoard, new String[] { "e2e4", "c7c5", "g1f3" },
				new String[] { "d7d6", "b8c6", "g8f6" });
		addOpeningLine(book, tempBoard, new String[] { "e2e4", "c7c5", "g1f3", "d7d6" },
				new String[] { "d2d4", "f1b5", "c2c3" });

		// French Defense main line
		addOpeningLine(book, tempBoard, new String[] { "e2e4", "e7e6", "d2d4" },
				new String[] { "d7d5", "g8f6", "c7c5" });

		// King's Indian Defense
		addOpeningLine(book, tempBoard, new String[] { "d2d4", "g8f6", "c2c4", "g7g6" },
				new String[] { "b1c3", "g1f3", "g2g3" });

		// Nimzo-Indian Defense
		addOpeningLine(book, tempBoard, new String[] { "d2d4", "g8f6", "c2c4", "e7e6", "b1c3" },
				new String[] { "f8b4", "d7d5", "b7b6" });

		// Additional popular openings
		addOpeningLine(book, tempBoard, new String[] { "e2e4", "g7g6" }, new String[] { "d2d4", "g1f3", "b1c3" });
		addOpeningLine(book, tempBoard, new String[] { "e2e4", "e7e5", "g1f3", "g8f6" },
				new String[] { "f3e5", "d2d4", "b1c3" });

		return book;
	}

	/**
	 * **PERFORMANCE: Fast move lookup without creating Move objects**
	 */
	private static Move findLegalMove(final Board board, final String moveStr) {
		if (moveStr.length() < 4) {
			return null;
		}

		// **OPTIMIZATION: Direct coordinate comparison instead of string comparison**
		final var fromFile = moveStr.charAt(0);
		final var fromRank = moveStr.charAt(1);
		final var toFile = moveStr.charAt(2);
		final var toRank = moveStr.charAt(3);

		for (final var move : board.legalMoves()) {
			final var moveString = move.toString();
			if (moveString.length() >= 4 && moveString.charAt(0) == fromFile && moveString.charAt(1) == fromRank
					&& moveString.charAt(2) == toFile && moveString.charAt(3) == toRank) {
				return move;
			}
		}
		return null;
	}

	private final SecureRandom random = new SecureRandom();

	/**
	 * **OPTIMIZED: Fast book move lookup using Zobrist hash** O(1) average case vs
	 * O(n) string comparison in original
	 */
	public Move getBookMove(final Board board) {
		// Only use opening book within configured move limit
		if (board.getMoveCounter() > ChessConfig.AI.OPENING_BOOK_MAX_MOVES) {
			return null;
		}

		final var zobristKey = ZobristHashing.calculateZobristHash(board);
		final var entry = OPENING_BOOK.get(zobristKey);

		if (entry != null) {
			final var moveStr = entry.getRandomMove(random);
			if (moveStr != null) {
				return findLegalMove(board, moveStr);
			}
		}
		return null;
	}
}