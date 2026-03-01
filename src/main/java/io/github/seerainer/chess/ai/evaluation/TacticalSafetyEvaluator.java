package io.github.seerainer.chess.ai.evaluation;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;

import io.github.seerainer.chess.ai.utils.ChessUtils;

/**
 * Tactical safety evaluator to prevent piece blunders Critical for preventing
 * hanging pieces and basic tactical errors
 */
public class TacticalSafetyEvaluator implements EvaluationComponent {

    private static int evaluateDiscoveredAttacks(final Board board, final Side evaluatingSide) {
	var score = 0;

	// Check for pieces that are now under attack after the move
	for (final var square : Square.values()) {
	    if (square == Square.NONE) {
		continue;
	    }

	    final var piece = board.getPiece(square);
	    // Only check enemy pieces
	    if ((piece == Piece.NONE) || (piece.getPieceSide() == evaluatingSide)) {
		continue;
	    }

	    if (board.squareAttackedBy(square, evaluatingSide) != 0L) {
		final var isDefended = board.squareAttackedBy(square, piece.getPieceSide()) != 0L;
		if (!isDefended) {
		    score += ChessUtils.PIECE_VALUES[piece.getPieceType().ordinal()] / 4; // Bonus for attacking
											  // undefended pieces
		}
	    }
	}

	return score;
    }

    private static int evaluateHangingPieces(final Board board, final Side evaluatingSide) {
	var score = 0;

	for (final var square : Square.values()) {
	    if (square == Square.NONE) {
		continue;
	    }

	    final var piece = board.getPiece(square);
	    if (piece == Piece.NONE) {
		continue;
	    }

	    final var isOurPiece = piece.getPieceSide() == evaluatingSide;

	    // Check if piece is attacked
	    final var isAttacked = board.squareAttackedBy(square, piece.getPieceSide().flip()) != 0L;

	    if (isAttacked) {
		// Check if piece is defended
		final var isDefended = board.squareAttackedBy(square, piece.getPieceSide()) != 0L;

		final var pieceValue = ChessUtils.PIECE_VALUES[piece.getPieceType().ordinal()];

		if (!isDefended) {
		    // HANGING PIECE - massive penalty/bonus
		    if (isOurPiece) {
			score -= pieceValue * 2; // HUGE penalty for our hanging pieces
		    } else {
			score += pieceValue * 2; // HUGE bonus for enemy hanging pieces
		    }
		} else {
		    // Piece is attacked but defended - evaluate exchange
		    final var attackerValue = getLowestAttackerValue(board, square, piece.getPieceSide().flip());
		    if (attackerValue > 0 && attackerValue < pieceValue) {
			// Bad exchange for the piece owner
			if (isOurPiece) {
			    score -= (pieceValue - attackerValue) / 2;
			} else {
			    score += (pieceValue - attackerValue) / 2;
			}
		    }
		}
	    }
	}

	return score;
    }

    private static int evaluatePieceExchanges(final Board board, final Side evaluatingSide) {
	var score = 0;

	// Look for favorable captures
	final var legalMoves = board.legalMoves();

	for (final var move : legalMoves) {
	    final var captured = board.getPiece(move.getTo());
	    if (captured != Piece.NONE) {
		final var attacker = board.getPiece(move.getFrom());

		final var captureValue = ChessUtils.PIECE_VALUES[captured.getPieceType().ordinal()];
		final var attackerValue = ChessUtils.PIECE_VALUES[attacker.getPieceType().ordinal()];

		// Simple exchange evaluation
		if ((attacker.getPieceSide() == evaluatingSide) && (captureValue >= attackerValue)) {
		    score += (captureValue - attackerValue) / 4; // Bonus for good exchanges
		}
	    }
	}

	return score;
    }

    private static int evaluateTacticalSafety(final Board board, final Side evaluatingSide) {
	var score = 0;

	// Check for hanging pieces (pieces under attack but not defended)
	score += evaluateHangingPieces(board, evaluatingSide);

	// Check for tactical threats and opportunities
	score += evaluateTacticalThreats(board, evaluatingSide);

	// Evaluate piece exchanges
	score += evaluatePieceExchanges(board, evaluatingSide);

	return score;
    }

    private static int evaluateTacticalThreats(final Board board, final Side evaluatingSide) {
	var score = 0;

	// Generate all legal moves to check for tactical opportunities
	final var legalMoves = board.legalMoves();

	for (final var move : legalMoves) {
	    // Check if this move creates a tactical threat
	    board.doMove(move);

	    // Check if we're giving check
	    if (board.isKingAttacked() && (board.getSideToMove() != evaluatingSide)) {
		score += 20; // REDUCED from 50 - checks are less important than piece safety
	    }

	    // Check for discovered attacks after this move
	    score += evaluateDiscoveredAttacks(board, evaluatingSide);

	    board.undoMove();
	}

	// Limit to avoid excessive computation
	if (legalMoves.size() > 20) {
	    score = score * 20 / legalMoves.size();
	}

	return score;
    }

    private static int getLowestAttackerValue(final Board board, final Square square, final Side attackingSide) {
	var lowestValue = Integer.MAX_VALUE;

	// Check all pieces of the attacking side to find the lowest value attacker
	for (final var attackerSquare : Square.values()) {
	    if (attackerSquare == Square.NONE) {
		continue;
	    }

	    final var piece = board.getPiece(attackerSquare);
	    if (piece == Piece.NONE || piece.getPieceSide() != attackingSide) {
		continue;
	    }

	    // Check if this piece can attack the target square (using board-aware check for
	    // blocking)
	    if (ChessUtils.canPieceAttackSquare(board, attackerSquare, square)) {
		final var pieceValue = ChessUtils.PIECE_VALUES[piece.getPieceType().ordinal()];
		lowestValue = Math.min(lowestValue, pieceValue);
	    }
	}

	return lowestValue == Integer.MAX_VALUE ? 0 : lowestValue;
    }

    @Override
    public int evaluate(final EvaluationContext context) {
	final var board = context.getBoard();
	final var evaluatingSide = context.getEvaluatingSide();

	return evaluateTacticalSafety(board, evaluatingSide);
    }

    @Override
    public String getComponentName() {
	return "Tactical Safety";
    }

    @Override
    public double getWeight(final EvaluationContext context) {
	// Moderate weight — tactical safety matters but should not drown material
	return 2.0;
    }
}