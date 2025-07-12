package io.github.seerainer.chess.ai.evaluation;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.PieceType;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;

/**
 * Evaluator for piece activity, development, and positional factors
 */
public class PieceActivityEvaluator implements EvaluationComponent {

	private static int calculatePieceActivity(final Board board, final Side evaluatingSide) {
		var score = 0;

		// Evaluate piece development and activity
		score += evaluatePieceDevelopment(board, evaluatingSide);
		score += evaluateCenterControl(board, evaluatingSide);
		score += evaluateKnightOutposts(board, evaluatingSide);
		score += evaluateBishopPairs(board, evaluatingSide);

		return score;
	}

	private static int evaluateBishopPairs(final Board board, final Side evaluatingSide) {
		var whiteBishops = 0;
		var blackBishops = 0;

		for (final var square : Square.values()) {
			if (square == Square.NONE) {
				continue;
			}

			final var piece = board.getPiece(square);
			if (piece.getPieceType() == PieceType.BISHOP) {
				if (piece.getPieceSide() == Side.WHITE) {
					whiteBishops++;
				} else {
					blackBishops++;
				}
			}
		}

		// Bishop pair bonus
		var score = 0;
		if (evaluatingSide == Side.WHITE) {
			if (whiteBishops >= 2) {
				score += 30;
			}
			if (blackBishops >= 2) {
				score -= 30;
			}
		} else {
			if (blackBishops >= 2) {
				score += 30;
			}
			if (whiteBishops >= 2) {
				score -= 30;
			}
		}

		return score;
	}

	private static int evaluateCenterControl(final Board board, final Side evaluatingSide) {
		var score = 0;

		// Center squares: d4, d5, e4, e5
		final var centerSquares = new Square[] { Square.D4, Square.D5, Square.E4, Square.E5 };

		for (final var square : centerSquares) {
			final var piece = board.getPiece(square);
			if (piece != Piece.NONE) {
				final var pieceType = piece.getPieceType();
				final var pieceSide = piece.getPieceSide();
				final var isOurPiece = pieceSide == evaluatingSide;

				switch (pieceType) {
				case PAWN:
					score += isOurPiece ? 20 : -20;
					break;
				case KNIGHT:
					score += isOurPiece ? 30 : -30;
					break;
				case BISHOP:
					score += isOurPiece ? 25 : -25;
					break;
				case null:
				default:
					break;
				}
			}
		}

		return score;
	}

	private static int evaluateKnightOutposts(final Board board, final Side evaluatingSide) {
		var score = 0;

		// Knights on strong squares (6th rank for white, 3rd rank for black)
		for (final var square : Square.values()) {
			if (square == Square.NONE) {
				continue;
			}

			final var piece = board.getPiece(square);
			if (piece.getPieceType() == PieceType.KNIGHT) {
				final var pieceSide = piece.getPieceSide();
				final var isOurPiece = pieceSide == evaluatingSide;

				if (isOurPiece) {
					final var rank = square.getRank().ordinal();
					if ((pieceSide == Side.WHITE && rank == 5) || (pieceSide == Side.BLACK && rank == 2)) { // 6th rank
						score += 20;
					}
				}
			}
		}

		return score;
	}

	private static int evaluatePieceDevelopment(final Board board, final Side evaluatingSide) {
		var score = 0;

		// Penalize pieces on starting squares (except pawns and king)
		for (final var square : Square.values()) {
			if (square == Square.NONE) {
				continue;
			}

			final var piece = board.getPiece(square);
			if (piece == Piece.NONE) {
				continue;
			}

			final var pieceType = piece.getPieceType();
			final var pieceSide = piece.getPieceSide();
			final var isOurPiece = pieceSide == evaluatingSide;

			if (pieceType == PieceType.KNIGHT) {
				// Knights should be developed
				if (pieceSide == Side.WHITE) {
					// White knights on b1, g1 are undeveloped
					if (square == Square.B1 || square == Square.G1) {
						score += isOurPiece ? -150 : 150; // Penalty for us, bonus against opponent
					} else if (square == Square.C3 || square == Square.F3 || square == Square.D4 || square == Square.E4
							|| square == Square.C4 || square == Square.F4) {
						// Good development squares
						score += isOurPiece ? 100 : -100; // Bonus for us, penalty against opponent
					}
				} else // Black knights on b8, g8 are undeveloped
				if (square == Square.B8 || square == Square.G8) {
					score += isOurPiece ? -150 : 150; // Penalty for us, bonus against opponent
				} else if (square == Square.C6 || square == Square.F6 || square == Square.D5 || square == Square.E5
						|| square == Square.C5 || square == Square.F5) {
					// Good development squares
					score += isOurPiece ? 100 : -100; // Bonus for us, penalty against opponent
				}
			} else if (pieceType == PieceType.BISHOP) {
				// Bishops should be developed
				if (pieceSide == Side.WHITE) {
					// White bishops on c1, f1 are undeveloped
					if (square == Square.C1 || square == Square.F1) {
						score += isOurPiece ? -120 : 120; // Penalty for us, bonus against opponent
					} else {
						// Developed bishops get bonus
						score += isOurPiece ? 80 : -80; // Bonus for us, penalty against opponent
					}
				} else // Black bishops on c8, f8 are undeveloped
				if (square == Square.C8 || square == Square.F8) {
					score += isOurPiece ? -120 : 120; // Penalty for us, bonus against opponent
				} else {
					// Developed bishops get bonus
					score += isOurPiece ? 80 : -80; // Bonus for us, penalty against opponent
				}
			}
		}

		return score;
	}

	@Override
	public int evaluate(final EvaluationContext context) {
		final var board = context.getBoard();
		final var evaluatingSide = context.getEvaluatingSide();

		return calculatePieceActivity(board, evaluatingSide);
	}

	@Override
	public String getComponentName() {
		return "Piece Activity";
	}

	@Override
	public double getWeight(final EvaluationContext context) {
		// Much more important in opening and middlegame
		return context.isEndGame() ? 0.8 : 2.0; // Doubled the middlegame weight
	}
}
