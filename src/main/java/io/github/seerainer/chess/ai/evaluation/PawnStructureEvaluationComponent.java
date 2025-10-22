package io.github.seerainer.chess.ai.evaluation;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.PieceType;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;

/**
 * Pawn structure evaluation component following Single Responsibility Principle
 */
public class PawnStructureEvaluationComponent implements EvaluationComponent {

    private static final int DOUBLED_PAWN_PENALTY = -20;
    private static final int ISOLATED_PAWN_PENALTY = -25;
    private static final int PASSED_PAWN_BONUS = 30;
    private static final int PAWN_ADVANCEMENT_BASE = 10;

    private static void calculatePassedPawns(final int[] whitePawnFiles, final int[] blackPawnFiles,
	    final boolean[] whitePassedPawns, final boolean[] blackPassedPawns) {
	for (var file = 0; file < 8; file++) {
	    // Check white passed pawns
	    if (whitePawnFiles[file] > 0) {
		var isPassed = true;
		for (var checkFile = Math.max(0, file - 1); checkFile <= Math.min(7, file + 1); checkFile++) {
		    if (blackPawnFiles[checkFile] > 0) {
			isPassed = false;
			break;
		    }
		}
		whitePassedPawns[file] = isPassed;
	    }

	    // Check black passed pawns
	    if (blackPawnFiles[file] > 0) {
		var isPassed = true;
		for (var checkFile = Math.max(0, file - 1); checkFile <= Math.min(7, file + 1); checkFile++) {
		    if (whitePawnFiles[checkFile] > 0) {
			isPassed = false;
			break;
		    }
		}
		blackPassedPawns[file] = isPassed;
	    }
	}
    }

    private static int calculatePawnAdvancement(final int rank, final int file, final boolean isWhite) {
	final var distanceToPromotion = isWhite ? (7 - rank) : rank;

	var bonus = switch (distanceToPromotion) {
	case 0 -> 1000;
	case 1 -> 500;
	case 2 -> 200;
	case 3 -> 80;
	case 4 -> 30;
	case 5 -> PAWN_ADVANCEMENT_BASE;
	default -> 0;
	};

	// Center pawn bonus
	if ((file == 3 || file == 4) && distanceToPromotion <= 4) {
	    bonus += 20;
	}

	return bonus;
    }

    private static int evaluatePawnStructure(final Board board) {
	final var whitePawnFiles = new int[8];
	final var blackPawnFiles = new int[8];
	final var whitePassedPawns = new boolean[8];
	final var blackPassedPawns = new boolean[8];

	var pawnScore = 0;
	var advancementScore = 0;

	// Analyze pawn distribution across files
	for (final var square : Square.values()) {
	    if (square != Square.NONE) {
		final var piece = board.getPiece(square);
		if (piece.getPieceType() == PieceType.PAWN) {
		    final var file = square.getFile().ordinal();
		    final var rank = square.getRank().ordinal();
		    final var isWhite = piece.getPieceSide() == Side.WHITE;

		    if (isWhite) {
			whitePawnFiles[file]++;
		    } else {
			blackPawnFiles[file]++;
		    }

		    // Calculate advancement score
		    final var advancement = calculatePawnAdvancement(rank, file, isWhite);
		    if (isWhite) {
			advancementScore += advancement;
		    } else {
			advancementScore -= advancement;
		    }
		}
	    }
	}

	// Calculate passed pawns
	calculatePassedPawns(whitePawnFiles, blackPawnFiles, whitePassedPawns, blackPassedPawns);

	// Calculate pawn structure penalties and bonuses
	for (var file = 0; file < 8; file++) {
	    // White pawn structure
	    if (whitePawnFiles[file] > 1) {
		pawnScore += DOUBLED_PAWN_PENALTY * (whitePawnFiles[file] - 1);
	    }
	    if (whitePawnFiles[file] > 0) {
		// Isolated pawn check
		if ((file == 0 || whitePawnFiles[file - 1] == 0) && (file == 7 || whitePawnFiles[file + 1] == 0)) {
		    pawnScore += ISOLATED_PAWN_PENALTY;
		}
		// Passed pawn bonus
		if (whitePassedPawns[file]) {
		    pawnScore += PASSED_PAWN_BONUS;
		}
	    }

	    // Black pawn structure
	    if (blackPawnFiles[file] > 1) {
		pawnScore -= DOUBLED_PAWN_PENALTY * (blackPawnFiles[file] - 1);
	    }
	    if (blackPawnFiles[file] > 0) {
		if ((file == 0 || blackPawnFiles[file - 1] == 0) && (file == 7 || blackPawnFiles[file + 1] == 0)) {
		    pawnScore -= ISOLATED_PAWN_PENALTY;
		}
		if (blackPassedPawns[file]) {
		    pawnScore -= PASSED_PAWN_BONUS;
		}
	    }
	}

	return pawnScore + advancementScore;
    }

    @Override
    public int evaluate(final EvaluationContext context) {
	return evaluatePawnStructure(context.getBoard());
    }

    @Override
    public String getComponentName() {
	return "PawnStructure";
    }

    @Override
    public double getWeight(final EvaluationContext context) {
	// Pawn structure becomes more important in endgame
	return context.isEndGame() ? 1.2 : 0.8;
    }
}