package io.github.seerainer.chess.ai.evaluation;

import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.PieceType;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;

import io.github.seerainer.chess.ai.utils.ChessUtils;

/**
 * Tactical evaluation component for immediate tactical considerations
 */
public class TacticalEvaluationComponent implements EvaluationComponent {

    private static final int HANGING_PIECE_PENALTY = -200;
    private static final int ATTACKED_PIECE_PENALTY = -50;
    private static final int DEFENDED_PIECE_BONUS = 10;

    private static boolean isPieceDefended(final EvaluationContext context, final Square square, final Piece piece) {
	return isSquareDefended(context, square, piece.getPieceSide());
    }

    private static boolean isPieceHanging(final EvaluationContext context, final Square square, final Piece piece) {
	// Simplified hanging piece detection - piece is attacked but not defended
	return isSquareAttacked(context, square, piece.getPieceSide().flip())
		&& !isSquareDefended(context, square, piece.getPieceSide());
    }

    private static boolean isPieceUnderAttack(final EvaluationContext context, final Square square, final Piece piece) {
	return isSquareAttacked(context, square, piece.getPieceSide().flip());
    }

    private static boolean isSquareAttacked(final EvaluationContext context, final Square square,
	    final Side attackingSide) {
	// Use the utility class method instead of duplicating code
	return ChessUtils.isSquareAttacked(context.getBoard(), square, attackingSide);
    }

    private static boolean isSquareDefended(final EvaluationContext context, final Square square,
	    final Side defendingSide) {
	return isSquareAttacked(context, square, defendingSide);
    }

    @Override
    public int evaluate(final EvaluationContext context) {
	var tacticalScore = 0;

	// Quick tactical assessment - hanging pieces and basic safety
	for (final var square : Square.values()) {
	    if (square == Square.NONE) {
		continue;
	    }

	    final var piece = context.getBoard().getPiece(square);
	    if (piece == Piece.NONE || piece.getPieceType() == PieceType.KING) {
		continue;
	    }

	    final var pieceValue = Math.abs(MaterialEvaluator.getPieceValue(piece));
	    if (pieceValue < 300) {
		continue; // Skip pawns for performance
	    }

	    final var isWhite = piece.getPieceSide() == Side.WHITE;
	    final var multiplier = isWhite ? 1 : -1;

	    // Simple hanging piece detection
	    if (isPieceHanging(context, square, piece)) {
		tacticalScore += HANGING_PIECE_PENALTY * multiplier;
	    } else if (isPieceUnderAttack(context, square, piece)) {
		tacticalScore += ATTACKED_PIECE_PENALTY * multiplier;
	    } else if (isPieceDefended(context, square, piece)) {
		tacticalScore += DEFENDED_PIECE_BONUS * multiplier;
	    }
	}

	return context.getEvaluatingSide() == Side.WHITE ? tacticalScore : -tacticalScore;
    }

    @Override
    public String getComponentName() {
	return "Tactical";
    }

    @Override
    public double getWeight(final EvaluationContext context) {
	// Tactical considerations are always important but less so in quiet positions
	return context.getBoard().isKingAttacked() ? 2.0 : 1.0;
    }
}