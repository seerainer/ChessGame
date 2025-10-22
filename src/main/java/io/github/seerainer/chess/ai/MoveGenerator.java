package io.github.seerainer.chess.ai;

import java.util.ArrayList;
import java.util.List;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;

import io.github.seerainer.chess.ai.utils.ChessUtils;

/**
 * Utility class for generating and filtering chess moves
 */
public class MoveGenerator {

    private MoveGenerator() {
	throw new IllegalStateException("Utility class");
    }

    /**
     * Generate tactical moves (captures, promotions, checks)
     */
    public static List<Move> generateTacticalMoves(final Board board) {
	final var tacticalMoves = new ArrayList<Move>();

	board.legalMoves().stream().filter(move -> isTacticalMove(board, move)).forEach(tacticalMoves::add);

	return tacticalMoves;
    }

    /**
     * Check if a square is attacked by the specified side
     */
    public static boolean isSquareAttacked(final Board board, final Square square, final Side attackingSide) {
	// Use the utility class method instead of duplicating code
	return ChessUtils.isSquareAttacked(board, square, attackingSide);
    }

    /**
     * Check if move is tactical
     */
    public static boolean isTacticalMove(final Board board, final Move move) {
	// Captures or promotions
	if (board.getPiece(move.getTo()) != Piece.NONE || move.getPromotion() != Piece.NONE) {
	    return true;
	}

	// Checks
	board.doMove(move);
	final var givesCheck = board.isKingAttacked();
	board.undoMove();

	return givesCheck;
    }
}