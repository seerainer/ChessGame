package io.github.seerainer.chess.ai;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.PieceType;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;

/**
 * COMPLETE REWRITE: Tournament-strength move ordering system Implements proper
 * opening principles and tactical priorities
 */
public class TournamentMoveOrdering {

	private enum GamePhase {
		OPENING, MIDDLEGAME, ENDGAME
	}

	private record TournamentMoveComparator(Board board, GamePhase gamePhase) implements Comparator<Move> {
		@Override
		public int compare(final Move move1, final Move move2) {
			final var score1 = evaluateMove(move1);
			final var score2 = evaluateMove(move2);
			return Integer.compare(score2, score1); // Higher scores first
		}

		private int evaluateMove(final Move move) {
			var score = 0;

			// 1. Captures (MVV-LVA: Most Valuable Victim - Least Valuable Attacker)
			final var captured = board.getPiece(move.getTo());
			if (captured != Piece.NONE) {
				final var attacker = board.getPiece(move.getFrom());
				score += 10000 + PIECE_VALUES[captured.getPieceType().ordinal()]
						- PIECE_VALUES[attacker.getPieceType().ordinal()];
			}

			// 2. Promotions
			if (move.getPromotion() != Piece.NONE) {
				score += 9000 + PIECE_VALUES[move.getPromotion().getPieceType().ordinal()];
			}

			// 3. Checks
			board.doMove(move);
			if (board.isKingAttacked()) {
				score += 5000;
			}
			board.undoMove();

			// 4. Opening principles (CRUCIAL!)
			if (gamePhase == GamePhase.OPENING) {
				score += evaluateOpeningMove(move);
			}

			// 5. Center control
			for (final var centerSquare : CENTER_SQUARES) {
				if (move.getTo() == centerSquare) {
					score += 300;
					break;
				}
			}

			return score;
		}

		private int evaluateOpeningMove(final Move move) {
			var score = 0;
			final var piece = board.getPiece(move.getFrom());
			final var pieceType = piece.getPieceType();
			final var side = piece.getPieceSide();

			switch (pieceType) {
			case NONE -> {
				// No score adjustment for NONE piece type
			}
			case KNIGHT -> {
				// MASSIVE bonus for knight development to good squares
				if (side == Side.WHITE) {
					for (final var goodSquare : WHITE_KNIGHT_GOOD_SQUARES) {
						if (move.getTo() == goodSquare) {
							score += 3000; // HUGE bonus for Nc3, Nf3, etc.
							break;
						}
					}
					// Heavy penalty for retreating
					if (move.getTo() == Square.B1 || move.getTo() == Square.G1) {
						score -= 2000;
					}
				} else {
					for (final var goodSquare : BLACK_KNIGHT_GOOD_SQUARES) {
						if (move.getTo() == goodSquare) {
							score += 3000; // HUGE bonus for Nc6, Nf6, etc.
							break;
						}
					}
					if (move.getTo() == Square.B8 || move.getTo() == Square.G8) {
						score -= 2000;
					}
				}
			}
			case BISHOP -> {
				// Large bonus for bishop development
				if (side == Side.WHITE) {
					for (final var goodSquare : WHITE_BISHOP_GOOD_SQUARES) {
						if (move.getTo() == goodSquare) {
							score += 2000; // Big bonus for Bc4, Bf4, etc.
							break;
						}
					}
					// Penalty for staying undeveloped
					if (move.getTo() == Square.C1 || move.getTo() == Square.F1) {
						score -= 1500;
					}
				} else {
					for (final var goodSquare : BLACK_BISHOP_GOOD_SQUARES) {
						if (move.getTo() == goodSquare) {
							score += 2000;
							break;
						}
					}
					if (move.getTo() == Square.C8 || move.getTo() == Square.F8) {
						score -= 1500;
					}
				}
			}
			case PAWN -> {
				// HUGE bonus for central pawn advances
				for (final var centerSquare : CENTER_SQUARES) {
					if (move.getTo() == centerSquare) {
						score += 2500; // MASSIVE bonus for d4, e4, d5, e5
						break;
					}
				}
				// MASSIVE penalty for wing pawn moves in opening
				if (side == Side.WHITE) {
					if (move.getFrom() == Square.A2 || move.getFrom() == Square.H2) {
						score -= 3000; // HUGE penalty for a3, h3 in opening
					}
					if (move.getFrom() == Square.B2 || move.getFrom() == Square.G2) {
						score -= 2000; // Large penalty for b3, g3 in opening
					}
				} else {
					if (move.getFrom() == Square.A7 || move.getFrom() == Square.H7) {
						score -= 3000; // HUGE penalty for a6, h6 in opening
					}
					if (move.getFrom() == Square.B7 || move.getFrom() == Square.G7) {
						score -= 2000; // Large penalty for b6, g6 in opening
					}
				}
			}
			case KING -> {
				// Castling is excellent
				if (Math.abs(move.getFrom().getFile().ordinal() - move.getTo().getFile().ordinal()) == 2) {
					score += 2500; // HUGE bonus for castling
				} else {
					score -= 2500; // HUGE penalty for early king moves
				}
			}
			case QUEEN -> {
				// Heavy penalty for early queen development
				if ((side == Side.WHITE && move.getFrom() == Square.D1)
						|| (side == Side.BLACK && move.getFrom() == Square.D8)) {
					score -= 2000; // Big penalty for early queen moves
				}
			}
			case ROOK -> // Penalty for early rook moves (except castling-related)
				score -= 500;
			default -> {
				// No score adjustment for NONE or unrecognized piece types
			}
			}

			return score;
		}
	}

	private static final int[] PIECE_VALUES = { 0, // NONE
			100, // PAWN
			320, // KNIGHT
			330, // BISHOP
			500, // ROOK
			900, // QUEEN
			20000 // KING
	};

	// Opening development squares for pieces
	private static final Square[] WHITE_KNIGHT_GOOD_SQUARES = { Square.C3, Square.F3, Square.D4, Square.E4, Square.C4,
			Square.F4 };

	private static final Square[] BLACK_KNIGHT_GOOD_SQUARES = { Square.C6, Square.F6, Square.D5, Square.E5, Square.C5,
			Square.F5 };

	private static final Square[] WHITE_BISHOP_GOOD_SQUARES = { Square.C4, Square.F4, Square.B5, Square.G5, Square.D3,
			Square.E3 };

	private static final Square[] BLACK_BISHOP_GOOD_SQUARES = { Square.C5, Square.F5, Square.B4, Square.G4, Square.D6,
			Square.E6 };

	private static final Square[] CENTER_SQUARES = { Square.D4, Square.D5, Square.E4, Square.E5 };

	private static GamePhase determineGamePhase(final Board board) {
		var pieceCount = 0;
		var queenCount = 0;

		for (final var square : Square.values()) {
			if (square == Square.NONE) {
				continue;
			}

			final var piece = board.getPiece(square);
			if (piece != Piece.NONE) {
				pieceCount++;
				if (piece.getPieceType() == PieceType.QUEEN) {
					queenCount++;
				}
			}
		}

		if (pieceCount > 24) {
			return GamePhase.OPENING;
		}
		if (pieceCount > 12 || queenCount >= 2) {
			return GamePhase.MIDDLEGAME;
		}
		return GamePhase.ENDGAME;
	}

	/**
	 * Tournament-strength move ordering
	 */
	public static List<Move> orderMoves(final Board board, final List<Move> moves) {

		final var orderedMoves = new ArrayList<>(moves);
		final var gamePhase = determineGamePhase(board);

		// Sort moves by priority for tournament strength
		orderedMoves.sort(new TournamentMoveComparator(board, gamePhase));

		return orderedMoves;
	}
}
