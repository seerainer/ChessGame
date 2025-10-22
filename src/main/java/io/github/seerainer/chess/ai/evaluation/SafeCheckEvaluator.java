package io.github.seerainer.chess.ai.evaluation;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.File;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.PieceType;
import com.github.bhlangonijr.chesslib.Rank;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;

/**
 * Safe Check Evaluator that ensures the engine only prioritizes checks when
 * they are tactically sound. This prevents the engine from giving checks when
 * the checking piece can be easily captured.
 */
public class SafeCheckEvaluator implements EvaluationComponent {

    // Penalties for unsafe checks
    private static final int UNSAFE_CHECK_PENALTY = -2000;
    private static final int HANGING_CHECKER_PENALTY = -3000;
    private static final int QUEEN_CHECKER_PENALTY = -5000;

    // Bonuses for safe checks
    private static final int SAFE_CHECK_BONUS = 200;
    private static final int PROTECTED_CHECK_BONUS = 100;
    private static final int DISCOVERED_CHECK_BONUS = 150;

    // Piece values for safety calculations
    private static final int[] PIECE_VALUES = { 0, // NONE
	    100, // PAWN
	    320, // KNIGHT
	    330, // BISHOP
	    500, // ROOK
	    900, // QUEEN
	    20000 // KING
    };

    /**
     * Analyze the safety of a specific checking move
     */
    private static int analyzeCheckSafety(final Board board, final Move move, final Side side) {
	var score = 0;

	// Safety checks first
	if (board == null || move == null || side == null) {
	    return 0;
	}

	if (move.getTo() == null || move.getTo() == Square.NONE) {
	    return 0;
	}

	try {
	    final var checkerSquare = move.getTo();
	    final var checker = board.getPiece(checkerSquare);

	    if (checker == null || checker == Piece.NONE) {
		return 0;
	    }

	    final var checkerValue = PIECE_VALUES[checker.getPieceType().ordinal()];

	    // Check if the checking piece is now hanging
	    final var isCheckerAttacked = board.squareAttackedBy(checkerSquare, side.flip()) != 0L;
	    final var isCheckerDefended = board.squareAttackedBy(checkerSquare, side) != 0L;

	    if (isCheckerAttacked) {
		if (!isCheckerDefended) {
		    // Hanging checker - major penalty
		    score += HANGING_CHECKER_PENALTY;

		    // Extra penalty if it's a valuable piece
		    if (checker.getPieceType() == PieceType.QUEEN) {
			score += QUEEN_CHECKER_PENALTY;
		    } else if (checkerValue >= 500) {
			score += UNSAFE_CHECK_PENALTY;
		    }
		} else {
		    // Checker is defended - analyze the exchange
		    final var attackerValue = getLowestAttackerValue(board, checkerSquare, side.flip());

		    if (attackerValue > 0 && attackerValue < checkerValue) {
			// Losing exchange - penalty proportional to loss
			score += UNSAFE_CHECK_PENALTY * ((checkerValue - attackerValue) / 100);
		    } else if (attackerValue > 0 && attackerValue >= checkerValue) {
			// Safe or winning exchange - bonus
			score += SAFE_CHECK_BONUS;
		    }
		}
	    } else {
		// Checker is not attacked - safe check
		score += SAFE_CHECK_BONUS;

		// Bonus if it's also defended
		if (isCheckerDefended) {
		    score += PROTECTED_CHECK_BONUS;
		}
	    }

	    // Check for discovered check bonus
	    if (isDiscoveredCheck(board, move, side)) {
		score += DISCOVERED_CHECK_BONUS;
	    }

	    // Additional analysis for check quality
	    score += evaluateCheckQuality(board, side);

	} catch (final Exception e) {
	    // If there's an error, return 0
	    System.err.println("Error in analyzeCheckSafety: " + e.getMessage());
	    return 0;
	}

	return score;
    }

    /**
     * Check if a piece can attack a specific square
     */
    private static boolean canPieceAttackSquare(final Board board, final Piece piece, final Square from,
	    final Square to) {
	// Safety checks first
	if (board == null || piece == null || from == null || to == null) {
	    return false;
	}

	if (from == Square.NONE || to == Square.NONE) {
	    return false;
	}

	try {
	    // Check if the move is legal (simplified check)
	    final var legalMoves = board.legalMoves();
	    for (final var move : legalMoves) {
		if (move != null && move.getFrom() == from && move.getTo() == to) {
		    return true;
		}
	    }
	} catch (final Exception e) {
	    // If there's an error, assume the piece cannot attack
	    System.err.println("Error checking piece attack: " + e.getMessage());
	}

	return false;
    }

    /**
     * Evaluate the quality of the check (e.g., does it lead to mate threats)
     */
    private static int evaluateCheckQuality(final Board board, final Side side) {
	var score = 0;

	// Check if the opponent has limited escape squares
	final var opponentKing = findKingSquare(board, side.flip());
	if (opponentKing == null) {
	    return 0;
	}

	var escapeSquares = 0;
	for (final var square : getAdjacentSquares(opponentKing)) {
	    // Check if this square is safe for the king
	    if ((square != Square.NONE) && (board.squareAttackedBy(square, side) == 0L)) {
		escapeSquares++;
	    }
	}

	// Bonus for limiting opponent's options
	if (escapeSquares <= 2) {
	    score += 100; // Strong check
	} else if (escapeSquares <= 4) {
	    score += 50; // Decent check
	}

	return score;
    }

    /**
     * Evaluate the safety of potential checking moves
     */
    private static int evaluateCheckSafety(final Board board, final Side side) {
	var score = 0;

	// Safety checks first
	// Skip if not our turn
	if (board == null || side == null || (board.getSideToMove() != side)) {
	    return 0;
	}

	try {
	    final var legalMoves = board.legalMoves();

	    // Safety check for move list
	    if (legalMoves == null || legalMoves.isEmpty()) {
		return 0;
	    }

	    // Limit evaluation for performance
	    var checkedMoves = 0;
	    for (final var move : legalMoves) {
		if (move == null) {
		    continue;
		}
		if (checkedMoves++ > 25) {
		    break; // Reasonable limit
		}

		try {
		    // Check if this move gives check
		    board.doMove(move);
		    final var givesCheck = board.isKingAttacked();

		    if (givesCheck) {
			// Analyze the safety of this check
			score += analyzeCheckSafety(board, move, side);
		    }

		    board.undoMove();
		} catch (final Exception e) {
		    // If there's an error with this move, skip it
		    System.err.println(new StringBuilder().append("Error evaluating check safety for move ")
			    .append(move).append(": ").append(e.getMessage()).toString());
		    try {
			board.undoMove();
		    } catch (final Exception undoError) {
			// Board might be in inconsistent state, return current score
			return score;
		    }
		}
	    }
	} catch (final Exception e) {
	    System.err.println("Error in evaluateCheckSafety: " + e.getMessage());
	    return 0;
	}

	return score;
    }

    /**
     * Find the king square for a given side
     */
    private static Square findKingSquare(final Board board, final Side side) {
	for (final var square : Square.values()) {
	    if (square == Square.NONE) {
		continue;
	    }

	    final var piece = board.getPiece(square);
	    if (piece.getPieceType() == PieceType.KING && piece.getPieceSide() == side) {
		return square;
	    }
	}
	return null;
    }

    /**
     * Get adjacent squares around a king
     */
    private static Square[] getAdjacentSquares(final Square kingSquare) {
	final var file = kingSquare.getFile().ordinal();
	final var rank = kingSquare.getRank().ordinal();

	final var adjacent = new Square[8];
	var index = 0;

	for (var fileOffset = -1; fileOffset <= 1; fileOffset++) {
	    for (var rankOffset = -1; rankOffset <= 1; rankOffset++) {
		if (fileOffset == 0 && rankOffset == 0) {
		    continue;
		}

		final var newFile = file + fileOffset;
		final var newRank = rank + rankOffset;

		if (newFile >= 0 && newFile <= 7 && newRank >= 0 && newRank <= 7) {
		    try {
			adjacent[index++] = Square.encode(Rank.allRanks[newRank], File.allFiles[newFile]);
		    } catch (final Exception e) {
			adjacent[index++] = Square.NONE;
		    }
		} else {
		    adjacent[index++] = Square.NONE;
		}
	    }
	}

	return adjacent;
    }

    /**
     * Get the value of the lowest attacker
     */
    private static int getLowestAttackerValue(final Board board, final Square square, final Side attackingSide) {
	var lowestValue = Integer.MAX_VALUE;

	for (final var attackerSquare : Square.values()) {
	    if (attackerSquare == Square.NONE) {
		continue;
	    }

	    final var piece = board.getPiece(attackerSquare);
	    if (piece == Piece.NONE || piece.getPieceSide() != attackingSide) {
		continue;
	    }

	    // Check if this piece can attack the target square
	    if (canPieceAttackSquare(board, piece, attackerSquare, square)) {
		final var pieceValue = PIECE_VALUES[piece.getPieceType().ordinal()];
		lowestValue = Math.min(lowestValue, pieceValue);
	    }
	}

	return lowestValue == Integer.MAX_VALUE ? 0 : lowestValue;
    }

    /**
     * Check if this is a discovered check
     */
    private static boolean isDiscoveredCheck(final Board board, final Move move, final Side side) {
	// Simple heuristic: if the moving piece is not giving direct check,
	// it might be a discovered check
	final var from = move.getFrom();
	final var to = move.getTo();

	// Find opponent king
	final var opponentKing = findKingSquare(board, side.flip());
	if (opponentKing == null) {
	    return false;
	}

	// Check if the piece that moved is on the same line as the king
	// and there might be a piece behind it that's now giving check
	return isOnSameLine(from, opponentKing) && !isOnSameLine(to, opponentKing);
    }

    /**
     * Check if two squares are on the same line (rank, file, or diagonal)
     */
    private static boolean isOnSameLine(final Square square1, final Square square2) {
	if (square1 == null || square2 == null) {
	    return false;
	}

	final var file1 = square1.getFile().ordinal();
	final var rank1 = square1.getRank().ordinal();
	final var file2 = square2.getFile().ordinal();
	final var rank2 = square2.getRank().ordinal();

	// Same file, rank, or diagonal
	return file1 == file2 || rank1 == rank2 || Math.abs(file1 - file2) == Math.abs(rank1 - rank2);
    }

    @Override
    public int evaluate(final EvaluationContext context) {
	final var board = context.getBoard();
	final var evaluatingSide = context.getEvaluatingSide();

	var score = 0;

	// Evaluate check safety for our potential moves
	score += evaluateCheckSafety(board, evaluatingSide);

	// Penalize opponent's unsafe checks against us
	score -= evaluateCheckSafety(board, evaluatingSide.flip());

	return score;
    }

    @Override
    public String getComponentName() {
	return "SafeCheckEvaluator";
    }
}
