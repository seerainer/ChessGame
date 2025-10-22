package io.github.seerainer.chess.ai.evaluation;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.PieceType;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;

/**
 * Piece activity evaluation component following Single Responsibility Principle
 */
public class PieceActivityEvaluationComponent implements EvaluationComponent {

    private static final int BISHOP_PAIR_BONUS = 50;
    private static final int MOBILITY_WEIGHT = 2;
    private static final int SPACE_CONTROL_WEIGHT = 2;
    private static final int DEVELOPMENT_WEIGHT = 4;

    private static int calculateEstimatedMobility(final PieceType pieceType) {
	return switch (pieceType) {
	case QUEEN -> 27;
	case ROOK -> 14;
	case BISHOP -> 13;
	case KNIGHT -> 8;
	case PAWN -> 2;
	case KING -> 3;
	default -> 0;
	};
    }

    private static int evaluatePieceActivity(final Board board, final boolean isEndGame) {
	var whiteMobility = 0;
	var blackMobility = 0;
	var whiteSpace = 0;
	var blackSpace = 0;
	var developmentScore = 0;
	var whiteBishops = 0;
	var blackBishops = 0;

	for (final var square : Square.values()) {
	    if (square == Square.NONE) {
		continue;
	    }

	    final var piece = board.getPiece(square);
	    if (piece == Piece.NONE) {
		continue;
	    }

	    final var rank = square.getRank().ordinal();
	    final var isWhite = piece.getPieceSide() == Side.WHITE;
	    final var pieceType = piece.getPieceType();

	    // Count bishops for bishop pair bonus
	    if (pieceType == PieceType.BISHOP) {
		if (isWhite) {
		    whiteBishops++;
		} else {
		    blackBishops++;
		}
	    }

	    // Estimated mobility
	    final var mobility = calculateEstimatedMobility(pieceType);
	    if (isWhite) {
		whiteMobility += mobility;
	    } else {
		blackMobility += mobility;
	    }

	    // Space evaluation for central squares
	    if (rank >= 2 && rank <= 5) {
		if (isWhite) {
		    whiteSpace += rank >= 4 ? 3 : 2;
		} else {
		    blackSpace += rank <= 3 ? 3 : 2;
		}
	    }

	    // Development checking (opening only)
	    if (!isEndGame && isDevelopmentPiece(piece, square)) {
		final var bonus = pieceType == PieceType.QUEEN ? 10 : 40;
		developmentScore += isWhite ? bonus : -bonus;
	    }
	}

	var activityScore = 0;
	activityScore += (whiteMobility - blackMobility) * MOBILITY_WEIGHT;
	activityScore += (whiteSpace - blackSpace) * SPACE_CONTROL_WEIGHT;

	if (!isEndGame) {
	    activityScore += developmentScore / DEVELOPMENT_WEIGHT;
	}

	// Bishop pair bonus
	if (whiteBishops >= 2) {
	    activityScore += BISHOP_PAIR_BONUS;
	}
	if (blackBishops >= 2) {
	    activityScore -= BISHOP_PAIR_BONUS;
	}

	return activityScore;
    }

    private static boolean isDevelopmentPiece(final Piece piece, final Square square) {
	final var startingSquares = new Square[] { Square.B1, Square.G1, Square.C1, Square.F1, Square.D1, // White
													  // pieces
		Square.B8, Square.G8, Square.C8, Square.F8, Square.D8 // Black pieces
	};

	final var developmentPieces = new Piece[] { Piece.WHITE_KNIGHT, Piece.WHITE_KNIGHT, Piece.WHITE_BISHOP,
		Piece.WHITE_BISHOP, Piece.WHITE_QUEEN, Piece.BLACK_KNIGHT, Piece.BLACK_KNIGHT, Piece.BLACK_BISHOP,
		Piece.BLACK_BISHOP, Piece.BLACK_QUEEN };

	for (var i = 0; i < startingSquares.length; i++) {
	    if (piece == developmentPieces[i] && square != startingSquares[i]) {
		return true;
	    }
	}
	return false;
    }

    @Override
    public int evaluate(final EvaluationContext context) {
	return evaluatePieceActivity(context.getBoard(), context.isEndGame());
    }

    @Override
    public String getComponentName() {
	return "PieceActivity";
    }

    @Override
    public double getWeight(final EvaluationContext context) {
	// Piece activity becomes more important in middlegame and endgame
	return context.getTotalMaterial() > 3000 ? 0.7 : 1.2;
    }
}