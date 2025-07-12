package io.github.seerainer.chess.ai.evaluation;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.File;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Rank;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;

/**
 * Advanced Pawn Structure Analysis - Comprehensive evaluation of pawn
 * formations
 *
 * Features: - Pawn chains and formations - Backward and weak pawns - Pawn
 * storms and attacks - Pawn majorities and minorities - King safety with pawn
 * shields - Advanced passed pawn evaluation - Pawn islands and structure
 * compactness
 */
public class PawnStructureEvaluatorAdvanced implements EvaluationComponent {

	// Basic pawn structure penalties/bonuses
	private static final int DOUBLED_PAWN_PENALTY = -25;
	private static final int ISOLATED_PAWN_PENALTY = -20;
	private static final int PASSED_PAWN_BONUS = 40;
	private static final int CONNECTED_PAWN_BONUS = 15;
	private static final int CENTRAL_PAWN_BONUS = 10;

	// Advanced pawn structure features
	private static final int BACKWARD_PAWN_PENALTY = -18;
	private static final int PAWN_CHAIN_BONUS = 20;
	private static final int PAWN_STORM_BONUS = 25;
	private static final int PAWN_MAJORITY_BONUS = 30;
	private static final int PAWN_ISLAND_PENALTY = -15;
	private static final int KING_SAFETY_PAWN_BONUS = 35;
	private static final int ADVANCED_PASSED_PAWN_BONUS = 60;
	private static final int SUPPORTED_PASSED_PAWN_BONUS = 25;
	private static final int OUTSIDE_PASSED_PAWN_BONUS = 40;
	private static final int ROOK_PAWN_PENALTY = -8;
	private static final int PAWN_LEVER_BONUS = 12;

	// Pawn advancement bonuses by rank
	private static final int[] PAWN_ADVANCEMENT_BONUS = { 0, 5, 10, 15, 25, 40, 60, 90 };

	/**
	 * Count pawn islands (groups of connected pawns)
	 */
	private static int countPawnIslands(final boolean[] pawnFiles) {
		var islands = 0;
		var inIsland = false;

		for (var file = 0; file < 8; file++) {
			if (pawnFiles[file]) {
				if (!inIsland) {
					islands++;
					inIsland = true;
				}
			} else {
				inIsland = false;
			}
		}

		return islands;
	}

	/**
	 * Evaluate advanced pawn features (backward, pawn chains, pawn storms, etc.)
	 */
	private static int evaluateAdvancedPawnFeatures(final Board board, final Square pawnSquare,
			final Piece friendlyPawn, final Piece enemyPawn, final Side side) {
		var score = 0;

		// Backward pawn penalty
		if (isBackwardPawn(board, pawnSquare, side, friendlyPawn, enemyPawn)) {
			score += BACKWARD_PAWN_PENALTY;
		}

		// Pawn chain evaluation
		if (isInPawnChain(board, pawnSquare, friendlyPawn, side)) {
			score += PAWN_CHAIN_BONUS;
		}

		// Pawn storm evaluation
		score += evaluatePawnStorm(board, pawnSquare, side);

		// Pawn lever bonus (pawns attacking enemy pawns)
		if (hasPawnLever(board, pawnSquare, side, enemyPawn)) {
			score += PAWN_LEVER_BONUS;
		}

		return score;
	}

	/**
	 * Evaluate basic pawn features (doubled, isolated, passed, connected, central)
	 */
	private static int evaluateBasicPawnFeatures(final Board board, final Square pawnSquare, final Piece friendlyPawn,
			final Piece enemyPawn, final Side side) {
		final var file = pawnSquare.getFile().ordinal();
		final var rank = pawnSquare.getRank().ordinal();
		var score = 0;

		// Central pawn bonus
		if (file >= 3 && file <= 4 && rank >= 2 && rank <= 5) {
			score += CENTRAL_PAWN_BONUS;
		}

		// Pawn advancement bonus
		final var advancementRank = side == Side.WHITE ? rank : (7 - rank);
		if (advancementRank >= 0 && advancementRank < PAWN_ADVANCEMENT_BONUS.length) {
			score += PAWN_ADVANCEMENT_BONUS[advancementRank];
		}

		// Connected pawn bonus
		if (hasConnectedPawn(board, pawnSquare, friendlyPawn)) {
			score += CONNECTED_PAWN_BONUS;
		}

		// Passed pawn evaluation
		if (isPassedPawn(board, pawnSquare, side, enemyPawn)) {
			score += PASSED_PAWN_BONUS;
			score += evaluatePassedPawnAdvanced(board, pawnSquare, side, friendlyPawn);
		}

		// Rook pawn penalty (a/h file pawns are less valuable)
		if (file == 0 || file == 7) {
			score += ROOK_PAWN_PENALTY;
		}

		return score;
	}

	/**
	 * Evaluate king safety based on pawn shield
	 */
	private static int evaluateKingSafety(final Board board, final Side side, final boolean[] pawnFiles,
			final int[] pawnRanks) {
		final var isWhite = side == Side.WHITE;
		final var friendlyKing = isWhite ? Piece.WHITE_KING : Piece.BLACK_KING;

		// Find king position
		var kingFile = -1;
		var kingRank = -1;

		for (final var square : Square.values()) {
			if (square != Square.NONE && board.getPiece(square) == friendlyKing) {
				kingFile = square.getFile().ordinal();
				kingRank = square.getRank().ordinal();
				break;
			}
		}

		if (kingFile == -1) {
			return 0; // King not found
		}

		var safetyBonus = 0;

		// Check pawn shield in front of king
		for (var fileOffset = -1; fileOffset <= 1; fileOffset++) {
			final var checkFile = kingFile + fileOffset;
			if (checkFile >= 0 && checkFile <= 7 && pawnFiles[checkFile]) {
				final var pawnRank = pawnRanks[checkFile];
				final var distance = Math.abs(pawnRank - kingRank);

				if (distance <= 3) { // Pawn shield
					safetyBonus += KING_SAFETY_PAWN_BONUS / (distance + 1);
				}
			}
		}

		return safetyBonus;
	}

	/**
	 * Advanced passed pawn evaluation
	 */
	private static int evaluatePassedPawnAdvanced(final Board board, final Square pawnSquare, final Side side,
			final Piece friendlyPawn) {
		final var rank = pawnSquare.getRank().ordinal();
		final var isWhite = side == Side.WHITE;
		var bonus = 0;

		// Distance to promotion bonus
		final var distanceToPromotion = isWhite ? (7 - rank) : rank;
		if (distanceToPromotion <= 2) {
			bonus += ADVANCED_PASSED_PAWN_BONUS;
		}

		// Supported passed pawn bonus
		if (isPassedPawnSupported(board, pawnSquare, side, friendlyPawn)) {
			bonus += SUPPORTED_PASSED_PAWN_BONUS;
		}

		// Outside passed pawn bonus (passed pawn on the side away from most pawns)
		if (isOutsidePassedPawn(board, pawnSquare, friendlyPawn)) {
			bonus += OUTSIDE_PASSED_PAWN_BONUS;
		}

		return bonus;
	}

	/**
	 * Evaluate pawn majorities (having more pawns on one side)
	 */
	private static int evaluatePawnMajorities(final boolean[] pawnFiles, final int[] pawnCounts) {
		var leftSidePawns = 0; // Files a-d (0-3)
		var rightSidePawns = 0; // Files e-h (4-7)

		for (var file = 0; file < 4; file++) {
			if (pawnFiles[file]) {
				leftSidePawns += pawnCounts[file];
			}
		}

		for (var file = 4; file < 8; file++) {
			if (pawnFiles[file]) {
				rightSidePawns += pawnCounts[file];
			}
		}

		// Bonus for having pawn majority
		if ((leftSidePawns > rightSidePawns + 1) || (rightSidePawns > leftSidePawns + 1)) {
			return PAWN_MAJORITY_BONUS;
		}

		return 0;
	}

	/**
	 * Evaluate pawn storm (aggressive pawn advance toward enemy king)
	 */
	private static int evaluatePawnStorm(final Board board, final Square pawnSquare, final Side side) {
		final var file = pawnSquare.getFile().ordinal();
		final var rank = pawnSquare.getRank().ordinal();
		final var isWhite = side == Side.WHITE;

		// Find enemy king
		final var enemyKing = isWhite ? Piece.BLACK_KING : Piece.WHITE_KING;
		var enemyKingFile = -1;

		for (final var square : Square.values()) {
			if (square != Square.NONE && board.getPiece(square) == enemyKing) {
				enemyKingFile = square.getFile().ordinal();
				break;
			}
		}

		if (enemyKingFile == -1) {
			return 0; // King not found
		}

		// Calculate storm bonus based on distance to enemy king
		final var fileDistance = Math.abs(file - enemyKingFile);
		if (fileDistance <= 2) { // Storm on same or adjacent files
			final var advancementRank = isWhite ? rank : (7 - rank);
			if (advancementRank >= 4) { // Advanced pawn
				return PAWN_STORM_BONUS + (fileDistance == 0 ? 10 : 0);
			}
		}

		return 0;
	}

	/**
	 * Comprehensive pawn structure evaluation for one side
	 */
	private static int evaluatePawnStructure(final Board board, final Side side) {
		final var friendlyPawn = side == Side.WHITE ? Piece.WHITE_PAWN : Piece.BLACK_PAWN;
		final var enemyPawn = side == Side.WHITE ? Piece.BLACK_PAWN : Piece.WHITE_PAWN;
		final var isWhite = side == Side.WHITE;

		// Collect pawn data
		final var pawnFiles = new boolean[8];
		final var pawnCounts = new int[8];
		final var pawnRanks = new int[8]; // Store highest rank for each file

		var score = 0;

		// Initialize pawn ranks
		for (var i = 0; i < 8; i++) {
			pawnRanks[i] = isWhite ? -1 : 8;
		}

		// Analyze all pawns
		for (final var square : Square.values()) {
			if (square != Square.NONE) {
				final var piece = board.getPiece(square);
				if (piece == friendlyPawn) {
					final var file = square.getFile().ordinal();
					final var rank = square.getRank().ordinal();

					pawnFiles[file] = true;
					pawnCounts[file]++;

					// Track most advanced pawn on each file
					if (isWhite) {
						pawnRanks[file] = Math.max(pawnRanks[file], rank);
					} else {
						pawnRanks[file] = Math.min(pawnRanks[file], rank);
					}

					// Basic evaluations
					score += evaluateBasicPawnFeatures(board, square, friendlyPawn, enemyPawn, side);

					// Advanced evaluations
					score += evaluateAdvancedPawnFeatures(board, square, friendlyPawn, enemyPawn, side);
				}
			}
		}

		// Evaluate pawn structure patterns
		score += evaluatePawnStructurePatterns(board, side, pawnFiles, pawnCounts, pawnRanks);

		return score;
	}

	/**
	 * Evaluate pawn structure patterns (islands, majorities, king safety)
	 */
	private static int evaluatePawnStructurePatterns(final Board board, final Side side, final boolean[] pawnFiles,
			final int[] pawnCounts, final int[] pawnRanks) {
		var score = 0;

		// Doubled pawn penalty
		for (var file = 0; file < 8; file++) {
			if (pawnCounts[file] > 1) {
				score += DOUBLED_PAWN_PENALTY * (pawnCounts[file] - 1);
			}
		}

		// Isolated pawn penalty
		for (var file = 0; file < 8; file++) {
			if (pawnFiles[file] && isIsolatedPawn(pawnFiles, file)) {
				score += ISOLATED_PAWN_PENALTY;
			}
		}

		// Pawn islands penalty
		final var islands = countPawnIslands(pawnFiles);
		score += PAWN_ISLAND_PENALTY * Math.max(0, islands - 1);

		// Pawn majority evaluation
		score += evaluatePawnMajorities(pawnFiles, pawnCounts);

		// King safety with pawn shield
		score += evaluateKingSafety(board, side, pawnFiles, pawnRanks);

		return score;
	}

	/**
	 * Original helper methods from the base implementation
	 */
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

	/**
	 * Check if pawn has a pawn lever (attacking enemy pawn)
	 */
	private static boolean hasPawnLever(final Board board, final Square pawnSquare, final Side side,
			final Piece enemyPawn) {
		final var file = pawnSquare.getFile().ordinal();
		final var rank = pawnSquare.getRank().ordinal();
		final var isWhite = side == Side.WHITE;

		// Check diagonal attacks
		final var attackRank = isWhite ? rank + 1 : rank - 1;
		if (attackRank >= 0 && attackRank <= 7) {
			for (final var fileOffset : new int[] { -1, 1 }) {
				final var attackFile = file + fileOffset;
				if (attackFile >= 0 && attackFile <= 7) {
					final var attackSquare = Square.encode(Rank.allRanks[attackRank], File.allFiles[attackFile]);
					if (board.getPiece(attackSquare) == enemyPawn) {
						return true;
					}
				}
			}
		}

		return false;
	}

	/**
	 * Check if pawn is backward (can't advance safely and isn't defended by pawns)
	 */
	private static boolean isBackwardPawn(final Board board, final Square pawnSquare, final Side side,
			final Piece friendlyPawn, final Piece enemyPawn) {
		final var file = pawnSquare.getFile().ordinal();
		final var rank = pawnSquare.getRank().ordinal();
		final var isWhite = side == Side.WHITE;

		// Check if pawn can advance
		final var nextRank = isWhite ? rank + 1 : rank - 1;
		if (nextRank >= 0 && nextRank <= 7) {
			final var nextSquare = Square.encode(Rank.allRanks[nextRank], File.allFiles[file]);
			if (board.getPiece(nextSquare) != Piece.NONE) {
				return false; // Can't advance due to piece blocking
			}

			// Check if advancing square is attacked by enemy pawns
			// Check if this pawn is defended by friendly pawns
			if (isSquareAttackedByEnemyPawns(board, nextSquare, enemyPawn)
					&& !isPawnDefended(board, pawnSquare, friendlyPawn)) {
				return true; // Backward pawn
			}
		}

		return false;
	}

	/**
	 * Check if pawn is part of a pawn chain (diagonal support)
	 */
	private static boolean isInPawnChain(final Board board, final Square pawnSquare, final Piece friendlyPawn,
			final Side side) {
		final var file = pawnSquare.getFile().ordinal();
		final var rank = pawnSquare.getRank().ordinal();
		final var isWhite = side == Side.WHITE;

		// Check if this pawn is defended by a friendly pawn
		final var defendingRank = isWhite ? rank - 1 : rank + 1;
		if (defendingRank >= 0 && defendingRank <= 7) {
			// Check both diagonals
			for (final var fileOffset : new int[] { -1, 1 }) {
				final var defendingFile = file + fileOffset;
				if (defendingFile >= 0 && defendingFile <= 7) {
					final var defendingSquare = Square.encode(Rank.allRanks[defendingRank],
							File.allFiles[defendingFile]);
					if (board.getPiece(defendingSquare) == friendlyPawn) {
						return true;
					}
				}
			}
		}

		// Check if this pawn is defending another pawn
		final var defendedRank = isWhite ? rank + 1 : rank - 1;
		if (defendedRank >= 0 && defendedRank <= 7) {
			for (final var fileOffset : new int[] { -1, 1 }) {
				final var defendedFile = file + fileOffset;
				if (defendedFile >= 0 && defendedFile <= 7) {
					final var defendedSquare = Square.encode(Rank.allRanks[defendedRank], File.allFiles[defendedFile]);
					if (board.getPiece(defendedSquare) == friendlyPawn) {
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

	/**
	 * Check if passed pawn is an outside passed pawn
	 */
	private static boolean isOutsidePassedPawn(final Board board, final Square pawnSquare, final Piece friendlyPawn) {
		final var file = pawnSquare.getFile().ordinal();

		// Count pawns on each side
		var leftPawns = 0;
		var rightPawns = 0;

		for (final var square : Square.values()) {
			if (square != Square.NONE && board.getPiece(square) == friendlyPawn) {
				final var pawnFile = square.getFile().ordinal();
				if (pawnFile < file) {
					leftPawns++;
				} else if (pawnFile > file) {
					rightPawns++;
				}
			}
		}

		// Outside passed pawn if most pawns are on the opposite side
		return (file <= 3 && rightPawns > leftPawns + 2) || (file >= 4 && leftPawns > rightPawns + 2);
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

	/**
	 * Check if passed pawn is supported by other pawns
	 */
	private static boolean isPassedPawnSupported(final Board board, final Square pawnSquare, final Side side,
			final Piece friendlyPawn) {
		final var file = pawnSquare.getFile().ordinal();
		final var rank = pawnSquare.getRank().ordinal();
		final var isWhite = side == Side.WHITE;

		// Check if pawn is defended by friendly pawns
		final var defendingRank = isWhite ? rank - 1 : rank + 1;
		if (defendingRank >= 0 && defendingRank <= 7) {
			for (final var fileOffset : new int[] { -1, 1 }) {
				final var defendingFile = file + fileOffset;
				if (defendingFile >= 0 && defendingFile <= 7) {
					final var defendingSquare = Square.encode(Rank.allRanks[defendingRank],
							File.allFiles[defendingFile]);
					if (board.getPiece(defendingSquare) == friendlyPawn) {
						return true;
					}
				}
			}
		}

		return false;
	}

	/**
	 * Check if pawn is defended by friendly pawns
	 */
	private static boolean isPawnDefended(final Board board, final Square pawnSquare, final Piece friendlyPawn) {
		final var file = pawnSquare.getFile().ordinal();
		final var rank = pawnSquare.getRank().ordinal();

		// Check diagonals where friendly pawns could defend from
		final var defendingRank = friendlyPawn == Piece.WHITE_PAWN ? rank - 1 : rank + 1;

		if (defendingRank >= 0 && defendingRank <= 7) {
			for (final var fileOffset : new int[] { -1, 1 }) {
				final var defendingFile = file + fileOffset;
				if (defendingFile >= 0 && defendingFile <= 7) {
					final var defendingSquare = Square.encode(Rank.allRanks[defendingRank],
							File.allFiles[defendingFile]);
					if (board.getPiece(defendingSquare) == friendlyPawn) {
						return true;
					}
				}
			}
		}

		return false;
	}

	/**
	 * Check if square is attacked by enemy pawns
	 */
	private static boolean isSquareAttackedByEnemyPawns(final Board board, final Square square, final Piece enemyPawn) {
		final var file = square.getFile().ordinal();
		final var rank = square.getRank().ordinal();

		// Check both diagonals where enemy pawns could attack from
		final var isWhiteTarget = enemyPawn == Piece.BLACK_PAWN;
		final var enemyPawnRank = isWhiteTarget ? rank - 1 : rank + 1;

		if (enemyPawnRank >= 0 && enemyPawnRank <= 7) {
			for (final var fileOffset : new int[] { -1, 1 }) {
				final var enemyFile = file + fileOffset;
				if (enemyFile >= 0 && enemyFile <= 7) {
					final var enemySquare = Square.encode(Rank.allRanks[enemyPawnRank], File.allFiles[enemyFile]);
					if (board.getPiece(enemySquare) == enemyPawn) {
						return true;
					}
				}
			}
		}

		return false;
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
		return "Advanced Pawn Structure";
	}

	@Override
	public double getWeight(final EvaluationContext context) {
		// Pawn structure becomes increasingly important in the endgame
		return context.isEndGame() ? 1.8 : 1.2;
	}
}
