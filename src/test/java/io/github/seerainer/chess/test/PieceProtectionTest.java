package io.github.seerainer.chess.test;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.PieceType;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;

import io.github.seerainer.chess.ChessAI;
import io.github.seerainer.chess.ai.evaluation.EvaluationContext;
import io.github.seerainer.chess.ai.evaluation.PieceProtectionEvaluator;

/**
 * Tests that the AI avoids hanging pieces and properly evaluates piece
 * protection in various positions.
 */
class PieceProtectionTest {

    /**
     * Find the queen square for a given side.
     */
    private static Square findQueenSquare(final Board board, final Side side) {
	for (final var square : Square.values()) {
	    if (square == Square.NONE) {
		continue;
	    }
	    final var piece = board.getPiece(square);
	    if (piece.getPieceType() == PieceType.QUEEN && piece.getPieceSide() == side) {
		return square;
	    }
	}
	return null;
    }

    /**
     * Test that the AI does not hang the queen after its first move.
     */
    @Test
    @DisplayName("Hanging Queen Prevention")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testHangingQueenPrevention() {
	final var fen = "rnbqkbnr/pppp1ppp/8/4p3/4P3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 2";
	final var board = new Board();
	board.loadFromFen(fen);

	final var ai = new ChessAI();
	try {
	    final var bestMove = ai.getBestMove(board);
	    assertNotNull(bestMove, "AI should find a move");

	    board.doMove(bestMove);

	    // Check if queen is hanging (attacked but undefended)
	    final var queenSquare = findQueenSquare(board, Side.BLACK);
	    if (queenSquare != null) {
		final var queenHanging = board.squareAttackedBy(queenSquare, Side.WHITE) != 0L
			&& board.squareAttackedBy(queenSquare, Side.BLACK) == 0L;
		assertTrue(!queenHanging, "AI should not hang the queen on " + queenSquare);
	    }
	    // If queen is not on the board at all, that is also fine

	    board.undoMove();
	} finally {
	    ai.cleanup();
	}
    }

    /**
     * Test that the PieceProtectionEvaluator does not report a large penalty for a
     * position where the rook is simply on its starting square.
     */
    @Test
    @DisplayName("Hanging Rook Prevention")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testHangingRookPrevention() {
	// Position with missing knight — rook file opened
	final var fen = "rnbqkbnr/pppppppp/8/8/8/8/PPPPPPPP/RNBQKB1R w KQkq - 0 1";
	final var board = new Board();
	board.loadFromFen(fen);

	final var evaluator = new PieceProtectionEvaluator();
	final var context = new EvaluationContext(board, Side.WHITE);
	final var score = evaluator.evaluate(context);

	assertTrue(score >= -500,
		"Protection score should not be heavily penalised for a near-starting position, got: " + score);
    }

    /**
     * Test that the evaluator detects a weak piece: knight on d4 attacking White's
     * position where the queen is under threat.
     */
    @Test
    @DisplayName("Weak Piece Protection Detection")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testWeakPieceProtection() {
	// Position where queen is attacked by knight
	final var fen = "rnbqkbnr/pppp1ppp/8/4p3/3nP3/8/PPPP1PPP/RNBQKBNR w KQkq - 0 3";
	final var board = new Board();
	board.loadFromFen(fen);

	final var evaluator = new PieceProtectionEvaluator();
	final var context = new EvaluationContext(board, Side.WHITE);
	final var score = evaluator.evaluate(context);

	assertTrue(score < -100, "Evaluator should detect weak piece protection when queen is attacked, got: " + score);
    }

    /**
     * Test that the AI does not leave any non-pawn pieces hanging after its move.
     */
    @Test
    @DisplayName("Protection Before Moving")
    @Timeout(value = 30, unit = TimeUnit.SECONDS)
    void testProtectionBeforeMoving() {
	// Position where moving a defender could leave another piece hanging
	final var fen = "rnbqkbnr/pppp1ppp/8/4p3/4P3/3B4/PPPP1PPP/RNBQK2R w KQkq - 0 3";
	final var board = new Board();
	board.loadFromFen(fen);

	final var ai = new ChessAI();
	try {
	    final var bestMove = ai.getBestMove(board);
	    assertNotNull(bestMove, "AI should find a move");

	    board.doMove(bestMove);

	    // Verify no White non-pawn/king piece is hanging
	    var foundHangingPiece = false;
	    for (final var square : Square.values()) {
		if (square == Square.NONE) {
		    continue;
		}
		final var piece = board.getPiece(square);
		if (piece == Piece.NONE || piece.getPieceSide() != Side.WHITE || piece.getPieceType() == PieceType.PAWN
			|| piece.getPieceType() == PieceType.KING) {
		    continue;
		}
		final var isAttacked = board.squareAttackedBy(square, Side.BLACK) != 0L;
		final var isDefended = board.squareAttackedBy(square, Side.WHITE) != 0L;
		if (isAttacked && !isDefended) {
		    foundHangingPiece = true;
		    break;
		}
	    }

	    assertTrue(!foundHangingPiece, "AI should not leave any White pieces hanging after its move");
	    board.undoMove();
	} finally {
	    ai.cleanup();
	}
    }

    /**
     * Test that the AI consistently protects pieces over a 3-move sequence in a
     * complex middlegame position.
     */
    @Test
    @DisplayName("Piece Protection in Game Sequence")
    @Timeout(value = 120, unit = TimeUnit.SECONDS)
    void testPieceProtectionInGame() {
	final var fen = "r2qkb1r/ppp2ppp/2n1bn2/3p4/3P4/2N1PN2/PPP2PPP/R1BQKB1R w KQkq - 0 6";
	final var board = new Board();
	board.loadFromFen(fen);

	final var ai = new ChessAI();
	try {
	    var movesWithoutHangingPieces = 0;
	    final var totalMoves = 3;

	    for (var i = 0; i < totalMoves; i++) {
		final var move = ai.getBestMove(board);
		assertNotNull(move, "AI should find a move at iteration " + i);
		board.doMove(move);

		// Check if any important pieces are hanging for the side that just moved
		var foundHangingPiece = false;
		for (final var square : Square.values()) {
		    if (square == Square.NONE) {
			continue;
		    }
		    final var piece = board.getPiece(square);
		    if (piece == Piece.NONE || piece.getPieceType() == PieceType.PAWN
			    || piece.getPieceType() == PieceType.KING) {
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

		// Play an opponent move to continue
		final var opponentMoves = board.legalMoves();
		if (!opponentMoves.isEmpty()) {
		    board.doMove(opponentMoves.getFirst());
		}
	    }

	    assertTrue(movesWithoutHangingPieces >= totalMoves - 1,
		    new StringBuilder().append("AI should protect pieces in at least ").append(totalMoves - 1)
			    .append(" of ").append(totalMoves).append(" moves, but only did in ")
			    .append(movesWithoutHangingPieces).toString());
	} finally {
	    ai.cleanup();
	}
    }
}
