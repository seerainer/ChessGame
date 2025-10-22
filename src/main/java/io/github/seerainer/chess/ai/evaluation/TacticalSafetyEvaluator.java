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

    private static boolean canBishopAttack(final Square from, final Square to) {
	final var fromFile = from.getFile().ordinal();
	final var fromRank = from.getRank().ordinal();
	final var toFile = to.getFile().ordinal();
	final var toRank = to.getRank().ordinal();

	final var fileDiff = Math.abs(fromFile - toFile);
	final var rankDiff = Math.abs(fromRank - toRank);

	return (fileDiff == rankDiff);
    }

    private static boolean canKingAttack(final Square from, final Square to) {
	final var fromFile = from.getFile().ordinal();
	final var fromRank = from.getRank().ordinal();
	final var toFile = to.getFile().ordinal();
	final var toRank = to.getRank().ordinal();

	final var fileDiff = Math.abs(fromFile - toFile);
	final var rankDiff = Math.abs(fromRank - toRank);

	return fileDiff <= 1 && rankDiff <= 1 && (fileDiff != 0 || rankDiff != 0);
    }

    private static boolean canKnightAttack(final Square from, final Square to) {
	final var fromFile = from.getFile().ordinal();
	final var fromRank = from.getRank().ordinal();
	final var toFile = to.getFile().ordinal();
	final var toRank = to.getRank().ordinal();

	final var fileDiff = Math.abs(fromFile - toFile);
	final var rankDiff = Math.abs(fromRank - toRank);

	return (fileDiff == 2 && rankDiff == 1) || (fileDiff == 1 && rankDiff == 2);
    }

    private static boolean canPawnAttack(final Square from, final Square to, final Side side) {
	final var fromFile = from.getFile().ordinal();
	final var fromRank = from.getRank().ordinal();
	final var toFile = to.getFile().ordinal();
	final var toRank = to.getRank().ordinal();

	final var fileDiff = Math.abs(fromFile - toFile);
	final var rankDiff = toRank - fromRank;

	return side == Side.WHITE ? fileDiff == 1 && rankDiff == 1 : fileDiff == 1 && rankDiff == -1;
    }

    private static boolean canPieceAttackSquare(final Piece piece, final Square from, final Square to) {
	// Simplified piece attack detection
	// In a full implementation, this would check actual piece movement rules
	final var pieceType = piece.getPieceType();

	return switch (pieceType) {
	case PAWN -> canPawnAttack(from, to, piece.getPieceSide());
	case KNIGHT -> canKnightAttack(from, to);
	case BISHOP -> canBishopAttack(from, to);
	case ROOK -> canRookAttack(from, to);
	case QUEEN -> canQueenAttack(from, to);
	case KING -> canKingAttack(from, to);
	default -> false;
	};
    }

    private static boolean canQueenAttack(final Square from, final Square to) {
	return canBishopAttack(from, to) || canRookAttack(from, to);
    }

    private static boolean canRookAttack(final Square from, final Square to) {
	final var fromFile = from.getFile().ordinal();
	final var fromRank = from.getRank().ordinal();
	final var toFile = to.getFile().ordinal();
	final var toRank = to.getRank().ordinal();

	// Must be on same rank or file
	if (fromFile != toFile && fromRank != toRank) {
	    return false;
	}

	// Check for pieces blocking the path (simplified)
	return true; // For now, assume path is clear
    }

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

	    // Check if this piece can attack the target square
	    // This is a simplified check - a full implementation would verify legal moves
	    if (canPieceAttackSquare(piece, attackerSquare, square)) {
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
	// Tactical safety is ALWAYS critical
	return 5.0; // Very high weight to prevent blunders
    }
}