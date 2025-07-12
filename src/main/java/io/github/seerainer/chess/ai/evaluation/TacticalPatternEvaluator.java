package io.github.seerainer.chess.ai.evaluation;

import java.util.ArrayList;
import java.util.List;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.File;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.PieceType;
import com.github.bhlangonijr.chesslib.Rank;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;

/**
 * Enhanced tactical pattern recognition for basic 1-2 move tactics Addresses
 * the 0% tactical puzzle solve rate by implementing specific pattern detection
 */
public class TacticalPatternEvaluator implements EvaluationComponent {

	private static final int FORK_BONUS = 800;
	private static final int PIN_BONUS = 600;
	private static final int SKEWER_BONUS = 700;
	private static final int DISCOVERED_ATTACK_BONUS = 650;
	private static final int DOUBLE_ATTACK_BONUS = 500;
	private static final int BACK_RANK_MATE_BONUS = 900;
	private static final int KNIGHT_FORK_BONUS = 850;
	private static final int ROYAL_FORK_BONUS = 1200;

	/**
	 * Check if a square attacks the opponent's king
	 */
	private static boolean attacksKing(final Board board, final Square fromSquare, final Side opponentSide) {
		final var kingSquare = findKingSquare(board, opponentSide);
		if (kingSquare == null) {
			return false;
		}

		// Check if this square attacks the king square
		return isSquareAttacked(board, fromSquare, kingSquare);
	}

	/**
	 * Check if a square attacks the opponent's queen
	 */
	private static boolean attacksQueen(final Board board, final Square fromSquare, final Side opponentSide) {
		final var queenSquare = findQueenSquare(board, opponentSide);
		if (queenSquare == null) {
			return false;
		}

		// Check if this square attacks the queen square
		return isSquareAttacked(board, fromSquare, queenSquare);
	}

	/**
	 * Check for diagonal pins from a specific square
	 */
	private static int checkDiagonalPins(final Board board, final Square fromSquare, final Side side) {
		var score = 0;

		// Four diagonal directions
		final int[][] directions = { { 1, 1 }, { 1, -1 }, { -1, 1 }, { -1, -1 } };

		for (final int[] dir : directions) {
			final List<Square> line = new ArrayList<>();
			final var rank = fromSquare.getRank().ordinal();
			final var file = fromSquare.getFile().ordinal();

			// Travel along the diagonal
			for (var i = 1; i < 8; i++) {
				final var newRank = rank + dir[0] * i;
				final var newFile = file + dir[1] * i;

				if (newRank < 0 || newRank > 7 || newFile < 0 || newFile > 7) {
					break;
				}

				final var square = Square.encode(Rank.allRanks[newRank], File.allFiles[newFile]);
				if (square == null || square == Square.NONE) {
					break;
				}

				final var piece = board.getPiece(square);
				if (piece != Piece.NONE) {
					line.add(square);
					if (piece.getPieceSide() == side) {
						break; // Our piece blocks further
					}
				}
			}

			// Check if we have a pin: opponent piece, then our valuable piece
			if (line.size() >= 2) {
				final var firstPiece = board.getPiece(line.get(0));
				final var secondPiece = board.getPiece(line.get(1));

				if (firstPiece.getPieceSide() == side.flip() && secondPiece.getPieceSide() == side
						&& getPieceValue(secondPiece) > getPieceValue(firstPiece)) {
					score += PIN_BONUS;
				}
			}
		}

		return score;
	}

	/**
	 * Check for diagonal skewers from a specific square
	 */
	private static int checkDiagonalSkewers(final Board board, final Square fromSquare, final Side side) {
		var score = 0;

		// Four diagonal directions
		final int[][] directions = { { 1, 1 }, { 1, -1 }, { -1, 1 }, { -1, -1 } };

		for (final int[] dir : directions) {
			final List<Square> line = new ArrayList<>();
			final var rank = fromSquare.getRank().ordinal();
			final var file = fromSquare.getFile().ordinal();

			// Travel along the diagonal
			for (var i = 1; i < 8; i++) {
				final var newRank = rank + dir[0] * i;
				final var newFile = file + dir[1] * i;

				if (newRank < 0 || newRank > 7 || newFile < 0 || newFile > 7) {
					break;
				}

				final var square = Square.encode(Rank.allRanks[newRank], File.allFiles[newFile]);
				if (square == null || square == Square.NONE) {
					break;
				}

				final var piece = board.getPiece(square);
				if (piece != Piece.NONE) {
					line.add(square);
					if (piece.getPieceSide() == side) {
						break; // Our piece blocks further
					}
				}
			}

			// Check if we have a skewer: valuable opponent piece, then less valuable
			// opponent piece
			if (line.size() >= 2) {
				final var firstPiece = board.getPiece(line.get(0));
				final var secondPiece = board.getPiece(line.get(1));

				if (firstPiece.getPieceSide() == side.flip() && secondPiece.getPieceSide() == side.flip()
						&& getPieceValue(firstPiece) > getPieceValue(secondPiece)) {
					score += SKEWER_BONUS;
				}
			}
		}

		return score;
	}

	/**
	 * Check for discovered attack moves from a specific square
	 */
	private static int checkDiscoveredAttackMoves(final Board board, final Square fromSquare) {
		var score = 0;

		// For each legal move from this square
		for (final var move : board.legalMoves()) {
			if (move.getFrom() != fromSquare) {
				continue;
			}

			// Make the move temporarily
			board.doMove(move);

			// Check if this creates a discovered attack
			if (hasDiscoveredAttack()) {
				score += DISCOVERED_ATTACK_BONUS;
			}

			// Undo the move
			board.undoMove();
		}

		return score;
	}

	/**
	 * Check for linear pins from a specific square
	 */
	private static int checkLinearPins(final Board board, final Square fromSquare, final Side side) {
		var score = 0;

		// Four linear directions (rank and file)
		final int[][] directions = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };

		for (final int[] dir : directions) {
			final List<Square> line = new ArrayList<>();
			final var rank = fromSquare.getRank().ordinal();
			final var file = fromSquare.getFile().ordinal();

			// Travel along the line
			for (var i = 1; i < 8; i++) {
				final var newRank = rank + dir[0] * i;
				final var newFile = file + dir[1] * i;

				if (newRank < 0 || newRank > 7 || newFile < 0 || newFile > 7) {
					break;
				}

				final var square = Square.encode(Rank.allRanks[newRank], File.allFiles[newFile]);
				if (square == null || square == Square.NONE) {
					break;
				}

				final var piece = board.getPiece(square);
				if (piece != Piece.NONE) {
					line.add(square);
					if (piece.getPieceSide() == side) {
						break; // Our piece blocks further
					}
				}
			}

			// Check if we have a pin: opponent piece, then our valuable piece
			if (line.size() >= 2) {
				final var firstPiece = board.getPiece(line.get(0));
				final var secondPiece = board.getPiece(line.get(1));

				if (firstPiece.getPieceSide() == side.flip() && secondPiece.getPieceSide() == side
						&& getPieceValue(secondPiece) > getPieceValue(firstPiece)) {
					score += PIN_BONUS;
				}
			}
		}

		return score;
	}

	/**
	 * Check for linear skewers from a specific square
	 */
	private static int checkLinearSkewers(final Board board, final Square fromSquare, final Side side) {
		var score = 0;

		// Four linear directions
		final int[][] directions = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };

		for (final int[] dir : directions) {
			final List<Square> line = new ArrayList<>();
			final var rank = fromSquare.getRank().ordinal();
			final var file = fromSquare.getFile().ordinal();

			// Travel along the line
			for (var i = 1; i < 8; i++) {
				final var newRank = rank + dir[0] * i;
				final var newFile = file + dir[1] * i;

				if (newRank < 0 || newRank > 7 || newFile < 0 || newFile > 7) {
					break;
				}

				final var square = Square.encode(Rank.allRanks[newRank], File.allFiles[newFile]);
				if (square == null || square == Square.NONE) {
					break;
				}

				final var piece = board.getPiece(square);
				if (piece != Piece.NONE) {
					line.add(square);
					if (piece.getPieceSide() == side) {
						break; // Our piece blocks further
					}
				}
			}

			// Check if we have a skewer: valuable opponent piece, then less valuable
			// opponent piece
			if (line.size() >= 2) {
				final var firstPiece = board.getPiece(line.get(0));
				final var secondPiece = board.getPiece(line.get(1));

				if (firstPiece.getPieceSide() == side.flip() && secondPiece.getPieceSide() == side.flip()
						&& getPieceValue(firstPiece) > getPieceValue(secondPiece)) {
					score += SKEWER_BONUS;
				}
			}
		}

		return score;
	}

	/**
	 * Count how many opponent pieces are attacked from a specific square
	 */
	private static int countAttackedPieces(final Board board, final Square fromSquare, final Side opponentSide) {
		var count = 0;

		// This is a simplified version - in a full implementation, we'd calculate all
		// attacked squares
		// For now, we'll check adjacent squares and piece-specific attack patterns

		final var piece = board.getPiece(fromSquare);
		if (piece == Piece.NONE) {
			return 0;
		}

		// Check piece-specific attack patterns
		switch (piece.getPieceType()) {
		case KNIGHT -> count += countKnightAttacks(board, fromSquare, opponentSide);
		case BISHOP -> count += countDiagonalAttacks(board, fromSquare, opponentSide);
		case ROOK -> count += countLinearAttacks(board, fromSquare, opponentSide);
		case QUEEN -> {
			count += countDiagonalAttacks(board, fromSquare, opponentSide);
			count += countLinearAttacks(board, fromSquare, opponentSide);
		}
		case PAWN -> count += countPawnAttacks(board, fromSquare, opponentSide);
		default -> {
			// King does not attack in this context, as it is not a tactical pattern
			// but we could count adjacent squares if needed
			final var rank = fromSquare.getRank().ordinal();
			final var file = fromSquare.getFile().ordinal();

			// Check adjacent squares for king attacks
			for (var r = -1; r <= 1; r++) {
				for (var f = -1; f <= 1; f++) {
					if (r == 0 && f == 0) {
						continue; // Skip the square itself
					}

					final var newRank = rank + r;
					final var newFile = file + f;

					if (newRank >= 0 && newRank <= 7 && newFile >= 0 && newFile <= 7) {
						final var square = Square.encode(Rank.allRanks[newRank], File.allFiles[newFile]);
						if (square != null) {
							final var targetPiece = board.getPiece(square);
							if (targetPiece.getPieceSide() == opponentSide) {
								count++;
							}
						}
					}
				}
			}
			break;
		}
		}

		return count;
	}

	/**
	 * Count diagonal attacks on opponent pieces
	 */
	private static int countDiagonalAttacks(final Board board, final Square fromSquare, final Side opponentSide) {
		var count = 0;

		// Four diagonal directions
		final int[][] directions = { { 1, 1 }, { 1, -1 }, { -1, 1 }, { -1, -1 } };

		for (final int[] dir : directions) {
			final var rank = fromSquare.getRank().ordinal();
			final var file = fromSquare.getFile().ordinal();

			for (var i = 1; i < 8; i++) {
				final var newRank = rank + dir[0] * i;
				final var newFile = file + dir[1] * i;

				if (newRank < 0 || newRank > 7 || newFile < 0 || newFile > 7) {
					break;
				}

				final var square = Square.encode(Rank.allRanks[newRank], File.allFiles[newFile]);
				if (square == null || square == Square.NONE) {
					break;
				}

				final var piece = board.getPiece(square);
				if (piece != Piece.NONE) {
					if (piece.getPieceSide() == opponentSide) {
						count++;
					}
					break; // Piece blocks further movement
				}
			}
		}

		return count;
	}

	/**
	 * Count knight attacks on opponent pieces
	 */
	private static int countKnightAttacks(final Board board, final Square knightSquare, final Side opponentSide) {
		var count = 0;

		// Knight moves: 8 possible L-shaped moves
		final int[][] knightMoves = { { 2, 1 }, { 2, -1 }, { -2, 1 }, { -2, -1 }, { 1, 2 }, { 1, -2 }, { -1, 2 },
				{ -1, -2 } };

		final var rank = knightSquare.getRank().ordinal();
		final var file = knightSquare.getFile().ordinal();

		for (final int[] move : knightMoves) {
			final var newRank = rank + move[0];
			final var newFile = file + move[1];

			if (newRank >= 0 && newRank <= 7 && newFile >= 0 && newFile <= 7) {
				final var square = Square.encode(Rank.allRanks[newRank], File.allFiles[newFile]);
				if (square != null) {
					final var piece = board.getPiece(square);
					if (piece.getPieceSide() == opponentSide) {
						count++;
					}
				}
			}
		}

		return count;
	}

	/**
	 * Count linear attacks on opponent pieces
	 */
	private static int countLinearAttacks(final Board board, final Square fromSquare, final Side opponentSide) {
		var count = 0;

		// Four linear directions
		final int[][] directions = { { 1, 0 }, { -1, 0 }, { 0, 1 }, { 0, -1 } };

		for (final int[] dir : directions) {
			final var rank = fromSquare.getRank().ordinal();
			final var file = fromSquare.getFile().ordinal();

			for (var i = 1; i < 8; i++) {
				final var newRank = rank + dir[0] * i;
				final var newFile = file + dir[1] * i;

				if (newRank < 0 || newRank > 7 || newFile < 0 || newFile > 7) {
					break;
				}

				final var square = Square.encode(Rank.allRanks[newRank], File.allFiles[newFile]);
				if (square == null || square == Square.NONE) {
					break;
				}

				final var piece = board.getPiece(square);
				if (piece != Piece.NONE) {
					if (piece.getPieceSide() == opponentSide) {
						count++;
					}
					break; // Piece blocks further movement
				}
			}
		}

		return count;
	}

	/**
	 * Count pawn attacks on opponent pieces
	 */
	private static int countPawnAttacks(final Board board, final Square pawnSquare, final Side opponentSide) {
		var count = 0;

		final var pawn = board.getPiece(pawnSquare);
		final var pawnSide = pawn.getPieceSide();

		final var direction = (pawnSide == Side.WHITE) ? 1 : -1;
		final var newRank = pawnSquare.getRank().ordinal() + direction;

		if (newRank >= 0 && newRank <= 7) {
			// Check diagonal captures
			final int[] files = { pawnSquare.getFile().ordinal() - 1, pawnSquare.getFile().ordinal() + 1 };

			for (final int file : files) {
				if (file >= 0 && file <= 7) {
					final var square = Square.encode(Rank.allRanks[newRank], File.allFiles[file]);
					if (square != null) {
						final var piece = board.getPiece(square);
						if (piece.getPieceSide() == opponentSide) {
							count++;
						}
					}
				}
			}
		}

		return count;
	}

	/**
	 * Evaluate back rank mate patterns
	 */
	private static int evaluateBackRankMates(final Board board, final Side side) {
		var score = 0;

		// Check if opponent king is vulnerable to back rank mate
		final var opponentSide = side.flip();
		final var opponentKingSquare = findKingSquare(board, opponentSide);

		if ((opponentKingSquare != null) && isBackRankMateVulnerable(board, opponentKingSquare, opponentSide)) {
			score += BACK_RANK_MATE_BONUS;
		}

		return score;
	}

	/**
	 * Evaluate diagonal pins (bishop/queen)
	 */
	private static int evaluateDiagonalPins(final Board board, final Side side) {
		var score = 0;

		for (final var square : Square.values()) {
			if (square == Square.NONE) {
				continue;
			}

			final var piece = board.getPiece(square);
			if (piece.getPieceSide() != side) {
				continue;
			}

			if (piece.getPieceType() == PieceType.BISHOP || piece.getPieceType() == PieceType.QUEEN) {
				score += checkDiagonalPins(board, square, side);
			}
		}

		return score;
	}

	/**
	 * Evaluate diagonal skewers
	 */
	private static int evaluateDiagonalSkewers(final Board board, final Side side) {
		var score = 0;

		for (final var square : Square.values()) {
			if (square == Square.NONE) {
				continue;
			}

			final var piece = board.getPiece(square);
			if (piece.getPieceSide() != side) {
				continue;
			}

			if (piece.getPieceType() == PieceType.BISHOP || piece.getPieceType() == PieceType.QUEEN) {
				score += checkDiagonalSkewers(board, square, side);
			}
		}

		return score;
	}

	/**
	 * Evaluate discovered attack patterns
	 */
	private static int evaluateDiscoveredAttacks(final Board board, final Side side) {
		var score = 0;

		// Check for pieces that can move to create discovered attacks
		for (final var square : Square.values()) {
			if (square == Square.NONE) {
				continue;
			}

			final var piece = board.getPiece(square);
			if (piece.getPieceSide() != side) {
				continue;
			}

			// Check if moving this piece would create a discovered attack
			score += checkDiscoveredAttackMoves(board, square);
		}

		return score;
	}

	/**
	 * Evaluate double attack patterns (attacking two pieces simultaneously)
	 */
	private static int evaluateDoubleAttacks(final Board board, final Side side) {
		var score = 0;

		for (final var square : Square.values()) {
			if (square == Square.NONE) {
				continue;
			}

			final var piece = board.getPiece(square);
			if (piece.getPieceSide() != side) {
				continue;
			}

			// Count how many opponent pieces this piece attacks
			final var attackCount = countAttackedPieces(board, square, side.flip());
			if (attackCount >= 2) {
				score += DOUBLE_ATTACK_BONUS;
			}
		}

		return score;
	}

	/**
	 * Evaluate knight fork patterns (attacking two or more pieces)
	 */
	private static int evaluateForkPatterns(final Board board, final Side side) {
		var score = 0;

		for (final var square : Square.values()) {
			if (square == Square.NONE) {
				continue;
			}

			final var piece = board.getPiece(square);
			if (piece.getPieceSide() != side) {
				continue;
			}

			if (piece.getPieceType() == PieceType.KNIGHT) {
				score += evaluateKnightForks(board, square, side);
			} else if (piece.getPieceType() == PieceType.PAWN) {
				score += evaluatePawnForks(board, square, side);
			}
		}

		return score;
	}

	/**
	 * Evaluate knight fork opportunities from a specific square
	 */
	private static int evaluateKnightForks(final Board board, final Square knightSquare, final Side side) {
		var score = 0;

		// Knight moves: 8 possible L-shaped moves
		final int[][] knightMoves = { { 2, 1 }, { 2, -1 }, { -2, 1 }, { -2, -1 }, { 1, 2 }, { 1, -2 }, { -1, 2 },
				{ -1, -2 } };

		final var knightRank = knightSquare.getRank().ordinal();
		final var knightFile = knightSquare.getFile().ordinal();

		for (final int[] moveOffset : knightMoves) {
			final var newRank = knightRank + moveOffset[0];
			final var newFile = knightFile + moveOffset[1];

			final var moveSquare = getSquareFromCoordinates(newRank, newFile);
			if (moveSquare == Square.NONE) {
				continue;
			}

			// Check if this knight move creates a fork
			final var knightMove = new Move(knightSquare, moveSquare);
			if (board.legalMoves().contains(knightMove)) {
				final var targets = countAttackedPieces(board, moveSquare, side.flip());
				if (targets >= 2) {
					score += KNIGHT_FORK_BONUS;

					// Special bonus for royal fork (attacking king and queen)
					if (attacksKing(board, moveSquare, side.flip()) && attacksQueen(board, moveSquare, side.flip())) {
						score += ROYAL_FORK_BONUS;
					}
				}
			}
		}

		return score;
	}

	/**
	 * Evaluate linear pins (rook/queen along ranks and files)
	 */
	private static int evaluateLinearPins(final Board board, final Side side) {
		var score = 0;

		for (final var square : Square.values()) {
			if (square == Square.NONE) {
				continue;
			}

			final var piece = board.getPiece(square);
			if (piece.getPieceSide() != side) {
				continue;
			}

			if (piece.getPieceType() == PieceType.ROOK || piece.getPieceType() == PieceType.QUEEN) {
				score += checkLinearPins(board, square, side);
			}
		}

		return score;
	}

	/**
	 * Evaluate linear skewers
	 */
	private static int evaluateLinearSkewers(final Board board, final Side side) {
		var score = 0;

		for (final var square : Square.values()) {
			if (square == Square.NONE) {
				continue;
			}

			final var piece = board.getPiece(square);
			if (piece.getPieceSide() != side) {
				continue;
			}

			if (piece.getPieceType() == PieceType.ROOK || piece.getPieceType() == PieceType.QUEEN) {
				score += checkLinearSkewers(board, square, side);
			}
		}

		return score;
	}

	/**
	 * Evaluate pawn fork opportunities
	 */
	private static int evaluatePawnForks(final Board board, final Square pawnSquare, final Side side) {
		var score = 0;

		final var direction = (side == Side.WHITE) ? 1 : -1;
		final var newRank = pawnSquare.getRank().ordinal() + direction;

		if (newRank >= 0 && newRank <= 7) {
			// Pawn captures diagonally
			final var leftFile = pawnSquare.getFile().ordinal() - 1;
			final var rightFile = pawnSquare.getFile().ordinal() + 1;

			if (leftFile >= 0) {
				final var leftCapture = Square.encode(Rank.allRanks[newRank], File.allFiles[leftFile]);
				if (leftCapture != null && board.getPiece(leftCapture).getPieceSide() == side.flip()) {
					score += FORK_BONUS / 2; // Pawn forks are less valuable than knight forks
				}
			}

			if (rightFile <= 7) {
				final var rightCapture = Square.encode(Rank.allRanks[newRank], File.allFiles[rightFile]);
				if (rightCapture != null && board.getPiece(rightCapture).getPieceSide() == side.flip()) {
					score += FORK_BONUS / 2;
				}
			}
		}

		return score;
	}

	/**
	 * Evaluate pin patterns (piece cannot move without exposing a more valuable
	 * piece)
	 */
	private static int evaluatePinPatterns(final Board board, final Side side) {
		var score = 0;

		// Check for bishop/queen pins along diagonals
		score += evaluateDiagonalPins(board, side);

		// Check for rook/queen pins along ranks and files
		score += evaluateLinearPins(board, side);

		return score;
	}

	/**
	 * Evaluate skewer patterns (forcing a valuable piece to move and attacking a
	 * less valuable piece behind)
	 */
	private static int evaluateSkewers(final Board board, final Side side) {
		// Similar to pins but reversed - valuable piece first, then less valuable
		var score = 0;

		// Check for diagonal skewers
		score += evaluateDiagonalSkewers(board, side);

		// Check for linear skewers
		score += evaluateLinearSkewers(board, side);

		return score;
	}

	/**
	 * Find the king square for a specific side
	 */
	private static Square findKingSquare(final Board board, final Side side) {
		for (final var square : Square.values()) {
			if (square == Square.NONE) {
				continue;
			}

			final var piece = board.getPiece(square);
			if (piece.getPieceSide() == side && piece.getPieceType() == PieceType.KING) {
				return square;
			}
		}
		return null;
	}

	/**
	 * Find the queen square for a specific side
	 */
	private static Square findQueenSquare(final Board board, final Side side) {
		for (final var square : Square.values()) {
			if (square == Square.NONE) {
				continue;
			}

			final var piece = board.getPiece(square);
			if (piece.getPieceSide() == side && piece.getPieceType() == PieceType.QUEEN) {
				return square;
			}
		}
		return null;
	}

	/**
	 * Get the value of a piece for tactical calculations
	 */
	private static int getPieceValue(final Piece piece) {
		return switch (piece.getPieceType()) {
		case PAWN -> 100;
		case KNIGHT -> 320;
		case BISHOP -> 330;
		case ROOK -> 500;
		case QUEEN -> 900;
		case KING -> 20000;
		default -> 0;
		};
	}

	/**
	 * Get Square from rank and file coordinates
	 */
	private static Square getSquareFromCoordinates(final int rank, final int file) {
		if (rank < 0 || rank > 7 || file < 0 || file > 7) {
			return Square.NONE;
		}

		// Convert to square name
		final var fileChar = (char) ('a' + file);
		final var rankChar = (char) ('1' + rank);
		final var squareName = "" + fileChar + rankChar;

		try {
			return Square.valueOf(squareName.toUpperCase());
		} catch (final IllegalArgumentException e) {
			return Square.NONE;
		}
	}

	/**
	 * Check if a move creates a discovered attack
	 */
	private static boolean hasDiscoveredAttack() {
		// Check if removing the piece from originalSquare creates an attack line
		// This is a simplified check - in a full implementation, we'd check all
		// possible attack lines
		return false; // Simplified for this example
	}

	/**
	 * Check if a king is vulnerable to back rank mate
	 */
	private static boolean isBackRankMateVulnerable(final Board board, final Square kingSquare, final Side kingSide) {
		final var kingRank = kingSquare.getRank().ordinal();

		// Check if king is on back rank
		if ((kingSide == Side.WHITE && kingRank != 0) || (kingSide == Side.BLACK && kingRank != 7)) {
			return false;
		}

		// Check if king is trapped by its own pawns
		final var pawnRank = (kingSide == Side.WHITE) ? 1 : 6;
		final var kingFile = kingSquare.getFile().ordinal();

		// Check pawns in front of king
		for (var file = Math.max(0, kingFile - 1); file <= Math.min(7, kingFile + 1); file++) {
			final var pawnSquare = Square.encode(Rank.allRanks[pawnRank], File.allFiles[file]);
			if (pawnSquare != null) {
				final var piece = board.getPiece(pawnSquare);
				if (piece.getPieceSide() == kingSide && piece.getPieceType() == PieceType.PAWN) {
					return true; // King is trapped
				}
			}
		}

		return false;
	}

	/**
	 * Check if one square attacks another
	 */
	private static boolean isSquareAttacked(final Board board, final Square fromSquare, final Square toSquare) {
		// This is a simplified check - in a full implementation, we'd check all attack
		// patterns
		// For now, we'll use a basic distance check

		final var rankDiff = Math.abs(fromSquare.getRank().ordinal() - toSquare.getRank().ordinal());
		final var fileDiff = Math.abs(fromSquare.getFile().ordinal() - toSquare.getFile().ordinal());

		final var piece = board.getPiece(fromSquare);

		return switch (piece.getPieceType()) {
		case KNIGHT -> (rankDiff == 2 && fileDiff == 1) || (rankDiff == 1 && fileDiff == 2);
		case BISHOP -> rankDiff == fileDiff;
		case ROOK -> rankDiff == 0 || fileDiff == 0;
		case QUEEN -> rankDiff == fileDiff || rankDiff == 0 || fileDiff == 0;
		case PAWN -> {
			final var direction = (piece.getPieceSide() == Side.WHITE) ? 1 : -1;
			yield rankDiff == 1 && fileDiff == 1
					&& (toSquare.getRank().ordinal() - fromSquare.getRank().ordinal()) == direction;
		}
		default -> false;
		};
	}

	@Override
	public int evaluate(final EvaluationContext context) {
		final var board = context.getBoard();
		final var side = context.getEvaluatingSide();

		var score = 0;

		// Check for tactical patterns available to the side to move
		score += evaluateForkPatterns(board, side);
		score += evaluatePinPatterns(board, side);
		score += evaluateSkewers(board, side);
		score += evaluateDiscoveredAttacks(board, side);
		score += evaluateDoubleAttacks(board, side);
		score += evaluateBackRankMates(board, side);

		// Subtract opponent's tactical threats
		score -= evaluateForkPatterns(board, side.flip());
		score -= evaluatePinPatterns(board, side.flip());
		score -= evaluateSkewers(board, side.flip());
		score -= evaluateDiscoveredAttacks(board, side.flip());
		score -= evaluateDoubleAttacks(board, side.flip());
		score -= evaluateBackRankMates(board, side.flip());

		return score;
	}

	@Override
	public String getComponentName() {
		return "TacticalPatterns";
	}
}
