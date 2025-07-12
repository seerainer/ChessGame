package io.github.seerainer.chess.ai.search;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.move.Move;

import io.github.seerainer.chess.ai.TournamentMoveOrdering;
import io.github.seerainer.chess.ai.evaluation.EvaluationOrchestrator;

/**
 * COMPLETE REWRITE: Tournament-strength search engine Implements proper minimax
 * with correct evaluation and move ordering
 */
public class TournamentSearchEngine {

	/**
	 * Filter out moves that hang pieces or make obvious blunders
	 */
	private static List<Move> filterOutBlunders(final Board board, final List<Move> moves) {
		return new ArrayList<>(
				moves.stream().filter((final Move move) -> !isBlunder(board, move)).collect(Collectors.toList()));
	}

	/**
	 * Get piece value for tactical calculations
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
	 * Check if a move is a blunder (hangs a piece)
	 */
	private static boolean isBlunder(final Board board, final Move move) {
		// Store the original piece that's moving
		final var originalPiece = board.getPiece(move.getFrom());
		if (originalPiece == Piece.NONE) {
			return false; // No piece to move
		}

		// Store what we're capturing (if anything)
		final var capturedPiece = board.getPiece(move.getTo());
		final var captureValue = capturedPiece != Piece.NONE ? getPieceValue(capturedPiece) : 0;

		// Make the move
		board.doMove(move);

		var hangsAPiece = false;

		// Check if the piece that moved is now hanging
		final var movedPiece = board.getPiece(move.getTo());
		if (movedPiece != Piece.NONE) {
			final var isAttacked = board.squareAttackedBy(move.getTo(), movedPiece.getPieceSide().flip()) != 0L;
			final var isDefended = board.squareAttackedBy(move.getTo(), movedPiece.getPieceSide()) != 0L;

			if (isAttacked && !isDefended) {
				// Piece is hanging - this might be a blunder
				final var movedPieceValue = getPieceValue(movedPiece);

				// It's a blunder if we lose more than we gain
				if (movedPieceValue > captureValue + 100) { // Allow some tactical compensation
					hangsAPiece = true;
				}
			}
		}

		board.undoMove();
		return hangsAPiece;
	}

	/**
	 * Check if position is tactical (optimized for speed)
	 */
	private static boolean isPositionTactical(final Board board) {
		// Quick checks first
		if (board.isKingAttacked()) {
			return true; // Check = tactical
		}

		final var moves = board.legalMoves();

		// Fast heuristic: many moves usually means tactical complexity
		if (moves.size() > 35) {
			return true;
		}

		// Count captures quickly (no need to check all)
		var captureCount = 0;
		for (var i = 0; i < Math.min(moves.size(), 10); i++) { // Check only first 10 moves
			if (board.getPiece(moves.get(i).getTo()) != Piece.NONE) {
				captureCount++;
				if (captureCount > 2) {
					return true; // Multiple captures = tactical
				}
			}
		}

		return false; // Quiet position
	}

	/**
	 * Quick evaluation for simple positions with few legal moves
	 */
	private static Move quickEvaluateSimplePosition(final Board board, final List<Move> moves) {
		var bestMove = moves.get(0);
		var bestScore = Integer.MIN_VALUE;

		for (final var move : moves) {
			// Quick heuristic evaluation
			var score = 0;

			// Favor captures
			if (board.getPiece(move.getTo()) != Piece.NONE) {
				score += getPieceValue(board.getPiece(move.getTo()));
			}

			// Avoid hanging pieces (quick check)
			board.doMove(move);
			final var movedPiece = board.getPiece(move.getTo());
			if (movedPiece != Piece.NONE) {
				final var isAttacked = board.squareAttackedBy(move.getTo(), movedPiece.getPieceSide().flip()) != 0L;
				final var isDefended = board.squareAttackedBy(move.getTo(), movedPiece.getPieceSide()) != 0L;

				if (isAttacked && !isDefended) {
					score -= getPieceValue(movedPiece); // Heavy penalty for hanging
				}
			}
			board.undoMove();

			if (score > bestScore) {
				bestScore = score;
				bestMove = move;
			}
		}

		System.out.printf("Quick evaluation: chosen %s (score: %d)%n", bestMove, bestScore);
		return bestMove;
	}

	private final EvaluationOrchestrator evaluator;

	// Search statistics
	private int nodesSearched;

	private long startTime;

	public TournamentSearchEngine() {
		this.evaluator = new EvaluationOrchestrator();
	}

	/**
	 * Find the best move using tournament-strength search with tactical awareness
	 */
	public Move getBestMove(final Board board, final int depth) {
		startTime = System.currentTimeMillis();
		nodesSearched = 0;

		final var moves = board.legalMoves();
		if (moves.isEmpty()) {
			return null;
		}

		// Quick decision for simple positions
		if (moves.size() <= 5) {
			System.out.println("=== Simple position detected, quick evaluation ===");
			return quickEvaluateSimplePosition(board, moves);
		}

		// Use tournament-strength move ordering
		final var orderedMoves = TournamentMoveOrdering.orderMoves(board, moves);

		Move bestMove = null;
		var bestScore = Integer.MIN_VALUE;
		var alpha = Integer.MIN_VALUE;
		final var beta = Integer.MAX_VALUE;

		System.out.println("=== Tournament Search with Tactical Analysis ===");

		// TACTICAL SAFETY CHECK: First eliminate moves that hang pieces
		final var safeMoves = filterOutBlunders(board, orderedMoves);
		final var movesToEvaluate = safeMoves.isEmpty() ? orderedMoves : safeMoves;

		System.out.printf("Filtered %d moves to %d safe moves%n", orderedMoves.size(), movesToEvaluate.size());

		// Determine time limit based on position complexity
		final var isTactical = isPositionTactical(board);
		final long timeLimit = isTactical ? 4500 : 2500; // 4.5s for tactical, 2.5s for normal

		// Evaluate each safe move with optimized tournament search
		for (final var move : movesToEvaluate) {
			board.doMove(move);

			// Use adaptive search depth: deeper only for tactical positions
			final var searchDepth = isTactical ? Math.min(depth + 1, 8) : // Tactical: up to 8 plies
					Math.min(depth, 6); // Normal: up to 6 plies

			// Use negamax pattern for correct evaluation
			final var score = -search(board, searchDepth - 1, -beta, -alpha);

			board.undoMove();

			System.out.printf("Move %s: score = %d (depth %d)%n", move.toString(), score, searchDepth);

			if (score > bestScore) {
				bestScore = score;
				bestMove = move;
				alpha = Math.max(alpha, score);
			}

			// Optimized time check - exit early if time is up
			if (System.currentTimeMillis() - startTime > timeLimit) {
				System.out.printf("Time limit (%dms) reached, returning best move found%n", timeLimit);
				break;
			}
		}

		System.out.printf("=== Best move: %s (score: %d, nodes: %d) ===%n", bestMove, bestScore, nodesSearched);

		return bestMove;
	}

	/**
	 * Get search statistics
	 */
	public String getStatistics() {
		return "Tournament Search: %d nodes, %d ms".formatted(nodesSearched, System.currentTimeMillis() - startTime);
	}

	/**
	 * Recursive search with proper tournament-strength evaluation
	 */
	private int search(final Board board, final int depth, final int alpha, final int beta) {
		nodesSearched++;

		// Terminal node check
		if (depth <= 0) {
			return evaluator.evaluatePosition(board, board.getSideToMove());
		}

		// Checkmate and stalemate
		final var moves = board.legalMoves();
		if (moves.isEmpty()) {
			return board.isKingAttacked() ? -20000 + (6 - depth) : 0;
		}

		// Order moves for better pruning
		final var orderedMoves = TournamentMoveOrdering.orderMoves(board, moves);

		var bestScore = Integer.MIN_VALUE;
		var currentAlpha = alpha;

		for (final var move : orderedMoves) {
			board.doMove(move);

			final var score = -search(board, depth - 1, -beta, -currentAlpha);

			board.undoMove();

			if (score > bestScore) {
				bestScore = score;
				currentAlpha = Math.max(currentAlpha, score);

				// Alpha-beta pruning
				if (currentAlpha >= beta) {
					break; // Beta cutoff
				}
			}

			// Time check
			if (System.currentTimeMillis() - startTime > 4500) { // Leave time for move selection
				break;
			}
		}

		return bestScore;
	}
}
