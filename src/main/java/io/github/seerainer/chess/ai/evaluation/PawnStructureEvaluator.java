package io.github.seerainer.chess.ai.evaluation;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.File;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Rank;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;

/**
 * Evaluates pawn structure - critical for strong positional play
 */
public class PawnStructureEvaluator implements EvaluationComponent {

	// Pawn structure values
	private static final int DOUBLED_PAWN_PENALTY = -25;
	private static final int ISOLATED_PAWN_PENALTY = -20;
	private static final int PASSED_PAWN_BONUS = 40;
	private static final int CONNECTED_PAWN_BONUS = 15;
	private static final int CENTRAL_PAWN_BONUS = 10;

	private static int evaluatePawnStructure(final com.github.bhlangonijr.chesslib.Board board, final Side side) {
		var score = 0;
		final var friendlyPawn = (side == Side.WHITE) ? Piece.WHITE_PAWN : Piece.BLACK_PAWN;
		final var enemyPawn = (side == Side.WHITE) ? Piece.BLACK_PAWN : Piece.WHITE_PAWN;

		// Get pawn positions
		final var pawnFiles = new boolean[8];
		final var pawnCounts = new int[8];

		for (final var square : Square.values()) {
			if (square != Square.NONE && board.getPiece(square) == friendlyPawn) {
				final var file = square.getFile().ordinal();
				pawnFiles[file] = true;
				pawnCounts[file]++;

				// Central pawn bonus
				if (file >= 2 && file <= 5) {
					score += CENTRAL_PAWN_BONUS;
				}

				// Check for passed pawn
				if (isPassedPawn(board, square, side, enemyPawn)) {
					score += PASSED_PAWN_BONUS;
				}

				// Check for connected pawns
				if (hasConnectedPawn(board, square, friendlyPawn)) {
					score += CONNECTED_PAWN_BONUS;
				}
			}
		}

		// Evaluate file-based penalties
		for (var file = 0; file < 8; file++) {
			if (pawnFiles[file]) {
				// Doubled pawns
				if (pawnCounts[file] > 1) {
					score += DOUBLED_PAWN_PENALTY * (pawnCounts[file] - 1);
				}

				// Isolated pawns
				if (isIsolatedPawn(pawnFiles, file)) {
					score += ISOLATED_PAWN_PENALTY;
				}
			}
		}

		return score;
	}

	private static boolean hasConnectedPawn(final Board board, final Square pawnSquare, final Piece friendlyPawn) {
		final var file = pawnSquare.getFile().ordinal();
		final var rank = pawnSquare.getRank().ordinal();

		// Check adjacent files on same rank or adjacent ranks
		for (var fileOffset = -1; fileOffset <= 1; fileOffset += 2) { // -1 and +1
			for (var rankOffset = -1; rankOffset <= 1; rankOffset++) {
				final var checkFile = file + fileOffset;
				final var checkRank = rank + rankOffset;

				if (checkFile >= 0 && checkFile <= 7 && checkRank >= 0 && checkRank <= 7) {
					final var checkSquare = Square.encode(com.github.bhlangonijr.chesslib.Rank.allRanks[checkRank],
							com.github.bhlangonijr.chesslib.File.allFiles[checkFile]);
					if (board.getPiece(checkSquare) == friendlyPawn) {
						return true;
					}
				}
			}
		}

		return false;
	}

	private static boolean isIsolatedPawn(final boolean[] pawnFiles, final int file) {
		final var leftFile = file - 1;
		final var rightFile = file + 1;

		final var hasLeftPawn = (leftFile >= 0) && pawnFiles[leftFile];
		final var hasRightPawn = (rightFile <= 7) && pawnFiles[rightFile];

		return !hasLeftPawn && !hasRightPawn;
	}

	private static boolean isPassedPawn(final Board board, final Square pawnSquare, final Side side,
			final Piece enemyPawn) {
		final var file = pawnSquare.getFile().ordinal();
		final var rank = pawnSquare.getRank().ordinal();
		final var isWhite = (side == Side.WHITE);

		// Check if any enemy pawns can stop this pawn
		for (var checkFile = Math.max(0, file - 1); checkFile <= Math.min(7, file + 1); checkFile++) {
			final var startRank = isWhite ? rank + 1 : 0;
			final var endRank = isWhite ? 8 : rank;

			for (var checkRank = startRank; checkRank < endRank; checkRank++) {
				final var checkSquare = Square.encode(Rank.allRanks[checkRank], File.allFiles[checkFile]);
				if (board.getPiece(checkSquare) == enemyPawn) {
					return false;
				}
			}
		}

		return true;
	}

	@Override
	public int evaluate(final EvaluationContext context) {
		final var board = context.getBoard();
		var pawnScore = 0;

		// Evaluate White pawns
		pawnScore += evaluatePawnStructure(board, Side.WHITE);

		// Evaluate Black pawns (subtract since we're from White's perspective)
		pawnScore -= evaluatePawnStructure(board, Side.BLACK);

		return pawnScore;
	}

	@Override
	public String getComponentName() {
		return "Pawn Structure";
	}

	@Override
	public double getWeight(final EvaluationContext context) {
		// Pawn structure more important in endgame
		return context.isEndGame() ? 1.5 : 1.0;
	}
}
