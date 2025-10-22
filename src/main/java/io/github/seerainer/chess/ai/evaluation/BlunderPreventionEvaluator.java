package io.github.seerainer.chess.ai.evaluation;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.PieceType;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;

/**
 * Blunder prevention system that specifically looks for moves that would leave
 * important pieces hanging or create tactical weaknesses. This is critical for
 * preventing the engine from making obvious blunders.
 */
public class BlunderPreventionEvaluator implements EvaluationComponent {

    private static final int CRITICAL_BLUNDER_PENALTY = -5000;
    private static final int MAJOR_BLUNDER_PENALTY = -2000;
    private static final int MINOR_BLUNDER_PENALTY = -500;

    /**
     * Check for pieces that are hanging in the current position
     */
    private static int checkForHangingPieces(final Board board, final Side side) {
	var score = 0;

	for (final var square : Square.values()) {
	    if (square == Square.NONE) {
		continue;
	    }

	    final var piece = board.getPiece(square);
	    if (piece == Piece.NONE || piece.getPieceSide() != side) {
		continue;
	    }

	    // Skip pawns unless they're advanced
	    if (piece.getPieceType() == PieceType.PAWN) {
		final var rank = square.getRank().ordinal();
		if ((side == Side.WHITE && rank < 5) || (side == Side.BLACK && rank > 2)) {
		    continue;
		}
	    }

	    final var pieceValue = MaterialEvaluator.getPieceValue(piece.getPieceType());

	    // Check if piece is hanging
	    if (isPieceHanging(board, square, side)) {
		// Critical penalty for hanging pieces
		if (piece.getPieceType() == PieceType.QUEEN) {
		    score += CRITICAL_BLUNDER_PENALTY;
		} else if (piece.getPieceType() == PieceType.ROOK) {
		    score += MAJOR_BLUNDER_PENALTY;
		} else if (pieceValue >= 300) {
		    score += MINOR_BLUNDER_PENALTY * (pieceValue / 100);
		}
	    }
	}

	return score;
    }

    /**
     * Check for blunders in our possible moves
     */
    private static int checkForMoveBlunders(final Board board, final Side side) {
	var score = 0;

	// Check if we have any moves that would create blunders
	final var ourMoves = board.legalMoves();

	var checkedMoves = 0;
	for (final var move : ourMoves) {
	    // Skip if not our turn
	    if (board.getSideToMove() != side) {
		continue;
	    }

	    // Limit evaluation for performance
	    if (checkedMoves++ > 20) {
		break;
	    }

	    // Make the move temporarily
	    board.doMove(move);

	    // Check if this move leaves any pieces hanging
	    final var hangingPenalty = checkForHangingPieces(board, side);
	    if (hangingPenalty < -1000) {
		// This move creates a major blunder
		score += MAJOR_BLUNDER_PENALTY / 4;
	    }

	    board.undoMove();
	}

	return score;
    }

    /**
     * Check for new threats created by opponent's move
     */
    private static int checkForNewThreats(final Board board, final Side ourSide) {
	var score = 0;

	// Check all our pieces for new threats
	for (final var square : Square.values()) {
	    if (square == Square.NONE) {
		continue;
	    }

	    final var piece = board.getPiece(square);
	    // Skip pawns and king
	    if (piece == Piece.NONE || piece.getPieceSide() != ourSide || piece.getPieceType() == PieceType.PAWN
		    || piece.getPieceType() == PieceType.KING) {
		continue;
	    }

	    // Check if this piece is now threatened
	    final var isAttacked = board.squareAttackedBy(square, ourSide.flip()) != 0L;
	    final var isDefended = board.squareAttackedBy(square, ourSide) != 0L;

	    if (isAttacked && !isDefended) {
		final var pieceValue = MaterialEvaluator.getPieceValue(piece.getPieceType());
		score += MINOR_BLUNDER_PENALTY * (pieceValue / 200);
	    }
	}

	return score;
    }

    /**
     * Check for tactical blunders (pieces that can be won tactically)
     */
    private static int checkForTacticalBlunders(final Board board, final Side side) {
	var score = 0;

	// Check if opponent has any immediate tactical threats
	final var opponentMoves = board.legalMoves();

	for (final var move : opponentMoves) {
	    // Skip if not opponent's turn
	    if (board.getSideToMove() == side) {
		continue;
	    }

	    // Make the move temporarily
	    board.doMove(move);

	    // Check what this move captures or threatens
	    final var capturedPiece = board.getPiece(move.getTo());
	    if (capturedPiece != Piece.NONE && capturedPiece.getPieceSide() == side) {
		final var captureValue = MaterialEvaluator.getPieceValue(capturedPiece.getPieceType());

		// Check if this is a good capture for opponent
		final var attacker = board.getPiece(move.getTo());
		final var attackerValue = MaterialEvaluator.getPieceValue(attacker.getPieceType());

		if (captureValue >= attackerValue) {
		    // Bad for us - opponent gets good trade
		    score += MINOR_BLUNDER_PENALTY * (captureValue / 100);
		}
	    }

	    // Check if this move creates new threats
	    score += checkForNewThreats(board, side);

	    board.undoMove();
	}

	return score;
    }

    /**
     * Check if a piece is hanging (attacked but not defended)
     */
    private static boolean isPieceHanging(final Board board, final Square square, final Side side) {
	final var isAttacked = board.squareAttackedBy(square, side.flip()) != 0L;
	final var isDefended = board.squareAttackedBy(square, side) != 0L;

	return isAttacked && !isDefended;
    }

    @Override
    public int evaluate(final EvaluationContext context) {
	final var board = context.getBoard();
	final var evaluatingSide = context.getEvaluatingSide();

	var score = 0;

	// Check for immediate blunders after this position
	score += checkForHangingPieces(board, evaluatingSide);
	score += checkForTacticalBlunders(board, evaluatingSide);
	score += checkForMoveBlunders(board, evaluatingSide);

	return score;
    }

    @Override
    public String getComponentName() {
	return "Blunder Prevention";
    }

    @Override
    public double getWeight(final EvaluationContext context) {
	// Maximum weight to prevent blunders
	return 10.0;
    }
}