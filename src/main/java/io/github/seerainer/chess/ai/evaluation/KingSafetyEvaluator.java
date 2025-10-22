package io.github.seerainer.chess.ai.evaluation;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.File;
import com.github.bhlangonijr.chesslib.PieceType;
import com.github.bhlangonijr.chesslib.Rank;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;

/**
 * Evaluates king safety including pawn shields, piece attacks, and king
 * position - Updated to implement EvaluationComponent interface
 */
public class KingSafetyEvaluator implements EvaluationComponent {

    private static final int KING_SAFETY_BONUS = 30;
    private static final int KING_DANGER_PENALTY = -50;
    private static final int PAWN_SHIELD_BONUS = 20;

    /**
     * Evaluate king activity in endgame
     */
    private static int evaluateKingActivity(final Board board) {
	var whiteKingActivity = 0;
	var blackKingActivity = 0;

	final var whiteKing = findKing(board, Side.WHITE);
	final var blackKing = findKing(board, Side.BLACK);

	if (whiteKing != Square.NONE) {
	    // King centralization bonus in endgame
	    final var file = whiteKing.getFile().ordinal();
	    final var rank = whiteKing.getRank().ordinal();
	    final var centerDistance = Math.abs(file - 3.5) + Math.abs(rank - 3.5);
	    whiteKingActivity += (int) (20 - centerDistance * 2);
	}

	if (blackKing != Square.NONE) {
	    final var file = blackKing.getFile().ordinal();
	    final var rank = blackKing.getRank().ordinal();
	    final var centerDistance = Math.abs(file - 3.5) + Math.abs(rank - 3.5);
	    blackKingActivity += (int) (20 - centerDistance * 2);
	}

	return whiteKingActivity - blackKingActivity;
    }

    /**
     * Evaluate king safety for both sides
     */
    public static int evaluateKingSafety(final Board board, final boolean isEndGame) {
	if (isEndGame) {
	    // In endgame, king activity is more important than safety
	    return evaluateKingActivity(board);
	}

	final var whiteKingSafety = evaluateKingSafetyForSide(board, Side.WHITE);
	final var blackKingSafety = evaluateKingSafetyForSide(board, Side.BLACK);

	return whiteKingSafety - blackKingSafety;
    }

    /**
     * Evaluate king safety for one side
     */
    private static int evaluateKingSafetyForSide(final Board board, final Side side) {
	final var kingSquare = findKing(board, side);
	if (kingSquare == Square.NONE) {
	    return 0;
	}

	var safety = 0;

	safety += board.isKingAttacked() ? KING_DANGER_PENALTY : KING_SAFETY_BONUS;

	// Pawn shield evaluation
	safety += evaluatePawnShield(board, kingSquare, side);

	return safety;
    }

    /**
     * Evaluate pawn shield around king
     */
    private static int evaluatePawnShield(final Board board, final Square kingSquare, final Side side) {
	var shieldBonus = 0;
	final var kingFile = kingSquare.getFile().ordinal();
	final var kingRank = kingSquare.getRank().ordinal();

	// Check pawns in front of king
	final var direction = side == Side.WHITE ? 1 : -1;
	for (var fileOffset = -1; fileOffset <= 1; fileOffset++) {
	    final var checkFile = kingFile + fileOffset;
	    if (checkFile >= 0 && checkFile <= 7) {
		final var checkRank = kingRank + direction;
		if (checkRank >= 0 && checkRank <= 7) {
		    final var checkSquare = Square.encode(Rank.allRanks[checkRank], File.allFiles[checkFile]);
		    final var piece = board.getPiece(checkSquare);
		    if (piece.getPieceType() == PieceType.PAWN && piece.getPieceSide() == side) {
			shieldBonus += PAWN_SHIELD_BONUS;
		    }
		}
	    }
	}

	return shieldBonus;
    }

    /**
     * Find the king square for the given side
     */
    public static Square findKing(final Board board, final Side side) {
	for (final var square : Square.values()) {
	    if (square != Square.NONE) {
		final var piece = board.getPiece(square);
		if (piece.getPieceType() == PieceType.KING && piece.getPieceSide() == side) {
		    return square;
		}
	    }
	}
	return Square.NONE;
    }

    @Override
    public int evaluate(final EvaluationContext context) {
	return evaluateKingSafety(context.getBoard(), context.isEndGame());
    }

    @Override
    public String getComponentName() {
	return "King Safety";
    }

    @Override
    public double getWeight(final EvaluationContext context) {
	// King safety more important in middlegame, less in endgame
	return context.isEndGame() ? 0.8 : 1.2;
    }
}
