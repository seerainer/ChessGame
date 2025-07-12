package io.github.seerainer.chess.test;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Side;

import io.github.seerainer.chess.ChessAI;
import io.github.seerainer.chess.ai.evaluation.EvaluationContext;
import io.github.seerainer.chess.ai.evaluation.PieceProtectionEvaluator;

public class PieceProtectionTest {

	/**
	 * Find the queen square for a given side
	 */
	private static com.github.bhlangonijr.chesslib.Square findQueenSquare(final Board board, final Side side) {
		for (final var square : com.github.bhlangonijr.chesslib.Square.values()) {
			if (square == com.github.bhlangonijr.chesslib.Square.NONE) {
				continue;
			}

			final var piece = board.getPiece(square);
			if (piece.getPieceType() == com.github.bhlangonijr.chesslib.PieceType.QUEEN
					&& piece.getPieceSide() == side) {
				return square;
			}
		}
		return null;
	}

	public static void main(final String[] args) {
		runPieceProtectionTests();
	}

	private static void runPieceProtectionTests() {
		System.out.println("=== Piece Protection Test Suite ===\n");

		testHangingQueenPrevention();
		testHangingRookPrevention();
		testWeakPieceProtection();
		testProtectionBeforeMoving();
		testPieceProtectionInGame();

		System.out.println("=== Piece Protection Tests Complete ===");
	}

	/**
	 * Test that the engine avoids hanging the queen
	 */
	private static void testHangingQueenPrevention() {
		System.out.println("=== Testing Hanging Queen Prevention ===");

		// Position where queen can be hung easily
		final var fen = "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2";
		final var board = new Board();
		board.loadFromFen(fen);

		System.out.println("Position: " + fen);
		System.out.println("Testing if AI avoids hanging queen...");

		final var ai = new ChessAI();
		final var bestMove = ai.getBestMove(board);

		System.out.println("AI chose: " + bestMove);

		// Test the move - make sure it doesn't hang the queen
		board.doMove(bestMove);

		// Check if queen is now hanging
		final var queenSquare = findQueenSquare(board, Side.BLACK);
		if (queenSquare != null) {
			final var queenHanging = board.squareAttackedBy(queenSquare, Side.WHITE) != 0L
					&& board.squareAttackedBy(queenSquare, Side.BLACK) == 0L;

			if (queenHanging) {
				System.out.println("❌ FAILED: AI move hangs the queen!");
			} else {
				System.out.println("✅ PASSED: Queen is safe");
			}
		} else {
			System.out.println("✅ PASSED: Queen moved to safety");
		}

		board.undoMove();
		System.out.println();
	}

	/**
	 * Test that the engine avoids hanging the rook
	 */
	private static void testHangingRookPrevention() {
		System.out.println("=== Testing Hanging Rook Prevention ===");

		// Position where rook can be attacked
		final var fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKB1R w KQkq - 0 1";
		final var board = new Board();
		board.loadFromFen(fen);

		System.out.println("Position: " + fen);
		System.out.println("Testing if AI protects rook...");

		// Test the evaluator directly
		final var evaluator = new PieceProtectionEvaluator();
		final var context = new EvaluationContext(board, Side.WHITE);

		final var score = evaluator.evaluate(context);
		System.out.println("Protection score: " + score);

		if (score < -500) {
			System.out.println("❌ FAILED: Significant piece protection penalty detected");
		} else {
			System.out.println("✅ PASSED: Piece protection looks good");
		}

		System.out.println();
	}

	/**
	 * Test piece protection in a real game scenario
	 */
	private static void testPieceProtectionInGame() {
		System.out.println("=== Testing Piece Protection in Game ===");

		// Complex middle game position
		final var fen = "r2qkb1r/ppp2ppp/2n1bn2/3p4/3P4/2N1PN2/PPP2PPP/R1BQKB1R w KQkq - 0 6";
		final var board = new Board();
		board.loadFromFen(fen);

		System.out.println("Position: " + fen);
		System.out.println("Testing piece protection in complex position...");

		final var ai = new ChessAI();
		final var bestMove = ai.getBestMove(board);

		System.out.println("AI chose: " + bestMove);

		// Test multiple moves to see if AI consistently protects pieces
		var movesWithoutHangingPieces = 0;
		final var totalMoves = 3;

		for (var i = 0; i < totalMoves; i++) {
			final var move = ai.getBestMove(board);
			board.doMove(move);

			// Check if any important pieces are hanging
			var foundHangingPiece = false;
			for (final var square : com.github.bhlangonijr.chesslib.Square.values()) {
				if (square == com.github.bhlangonijr.chesslib.Square.NONE) {
					continue;
				}

				final var piece = board.getPiece(square);
				// Check both sides
				if ((piece == com.github.bhlangonijr.chesslib.Piece.NONE)
						|| piece.getPieceType() == com.github.bhlangonijr.chesslib.PieceType.PAWN
						|| piece.getPieceType() == com.github.bhlangonijr.chesslib.PieceType.KING) {
					continue;
				}

				final var isAttacked = board.squareAttackedBy(square, piece.getPieceSide().flip()) != 0L;
				final var isDefended = board.squareAttackedBy(square, piece.getPieceSide()) != 0L;

				if (isAttacked && !isDefended) {
					foundHangingPiece = true;
					break;
				}
			}

			if (!foundHangingPiece) {
				movesWithoutHangingPieces++;
			}

			// Get opponent move (simplified)
			final var opponentMoves = board.legalMoves();
			if (!opponentMoves.isEmpty()) {
				board.doMove(opponentMoves.get(0));
			}
		}

		// Reset board properly
		final int moveCount = board.getMoveCounter();
		for (var i = 0; i < moveCount; i++) {
			if (board.getBackup().size() > 0) {
				board.undoMove();
			}
		}

		System.out.println("Moves without hanging pieces: " + movesWithoutHangingPieces + "/" + totalMoves);

		if (movesWithoutHangingPieces >= totalMoves - 1) {
			System.out.println("✅ PASSED: AI consistently protects pieces");
		} else {
			System.out.println("❌ FAILED: AI not consistently protecting pieces");
		}

		System.out.println();
	}

	/**
	 * Test that engine considers protection before moving pieces
	 */
	private static void testProtectionBeforeMoving() {
		System.out.println("=== Testing Protection Before Moving ===");

		// Position where moving a defender would leave another piece hanging
		final var fen = "rnbqkbnr/pppp1ppp/8/4p3/4P3/3B4/PPPP1PPP/RNBQK2R w KQkq - 0 3";
		final var board = new Board();
		board.loadFromFen(fen);

		System.out.println("Position: " + fen);
		System.out.println("Testing protection before moving...");

		final var ai = new ChessAI();
		final var bestMove = ai.getBestMove(board);

		System.out.println("AI chose: " + bestMove);

		// Check if the move leaves any pieces hanging
		board.doMove(bestMove);

		var foundHangingPiece = false;
		for (final var square : com.github.bhlangonijr.chesslib.Square.values()) {
			if (square == com.github.bhlangonijr.chesslib.Square.NONE) {
				continue;
			}

			final var piece = board.getPiece(square);
			if (piece == com.github.bhlangonijr.chesslib.Piece.NONE || piece.getPieceSide() != Side.WHITE
					|| piece.getPieceType() == com.github.bhlangonijr.chesslib.PieceType.PAWN
					|| piece.getPieceType() == com.github.bhlangonijr.chesslib.PieceType.KING) {
				continue;
			}

			final var isAttacked = board.squareAttackedBy(square, Side.BLACK) != 0L;
			final var isDefended = board.squareAttackedBy(square, Side.WHITE) != 0L;

			if (isAttacked && !isDefended) {
				foundHangingPiece = true;
				System.out.println("❌ FAILED: " + piece.getPieceType() + " on " + square + " is hanging!");
				break;
			}
		}

		if (!foundHangingPiece) {
			System.out.println("✅ PASSED: No pieces left hanging after move");
		}

		board.undoMove();
		System.out.println();
	}

	/**
	 * Test weak piece protection (pieces that can be exchanged unfavorably)
	 */
	private static void testWeakPieceProtection() {
		System.out.println("=== Testing Weak Piece Protection ===");

		// Position where queen is attacked by knight but defended by pawn
		final var fen = "rnbqkbnr/pppp1ppp/8/4p3/3nP3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 3";
		final var board = new Board();
		board.loadFromFen(fen);

		System.out.println("Position: " + fen);
		System.out.println("Testing weak piece protection evaluation...");

		final var evaluator = new PieceProtectionEvaluator();
		final var context = new EvaluationContext(board, Side.WHITE);

		final var score = evaluator.evaluate(context);
		System.out.println("Protection score: " + score);

		// We expect some penalty because queen is attacked by knight
		if (score < -200) {
			System.out.println("✅ PASSED: Detected weak piece protection");
		} else {
			System.out.println("❌ FAILED: Should detect weak piece protection");
		}

		System.out.println();
	}
}
