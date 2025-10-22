package io.github.seerainer.chess.ai.evaluation;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;

/**
 * Context object containing shared evaluation data to avoid redundant
 * calculations
 */
public class EvaluationContext {
    private final Board board;
    private final Side evaluatingSide;
    private final boolean isEndGame;
    private final int totalMaterial;
    private final Square whiteKingSquare;
    private final Square blackKingSquare;

    // Cached evaluation results
    private Integer materialScore;

    public EvaluationContext(final Board board, final Side evaluatingSide) {
	this.board = board;
	this.evaluatingSide = evaluatingSide;
	this.totalMaterial = calculateTotalMaterial();
	this.isEndGame = totalMaterial <= 2000;
	this.whiteKingSquare = findKingSquare(Side.WHITE);
	this.blackKingSquare = findKingSquare(Side.BLACK);
    }

    private int calculateTotalMaterial() {
	return MaterialEvaluator.getTotalMaterial(board);
    }

    private Square findKingSquare(final Side side) {
	return KingSafetyEvaluator.findKing(board, side);
    }

    public Board getBoard() {
	return board;
    }

    public Side getEvaluatingSide() {
	return evaluatingSide;
    }

    // Cached score getters with lazy initialization
    public int getMaterialScore() {
	if (materialScore == null) {
	    materialScore = MaterialEvaluator.evaluateMaterial(board);
	}
	return materialScore;
    }

    public int getTotalMaterial() {
	return totalMaterial;
    }

    public boolean isEndGame() {
	return isEndGame;
    }

    public Square getWhiteKingSquare() {
	return whiteKingSquare;
    }

    public Square getBlackKingSquare() {
	return blackKingSquare;
    }
}