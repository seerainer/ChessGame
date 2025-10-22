package io.github.seerainer.chess.ai.evaluation;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.CastleRight;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.PieceType;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.Square;

/**
 * Evaluation tuning system that provides dynamic weight adjustments based on
 * game phase, position type, and material balance.
 *
 * This addresses the "Evaluation Tuning - Position evaluation could be more
 * accurate" improvement by adding: - Game phase detection (opening, middlegame,
 * endgame) - Dynamic component weight adjustment - Position-specific evaluation
 * bonuses - Material imbalance considerations
 */
public class EvaluationTuner {

    // Game phase thresholds
    private static final int OPENING_PHASE_MATERIAL = 7800; // Most pieces on board
    private static final int ENDGAME_PHASE_MATERIAL = 2000; // Few pieces left
    // Component weight multipliers by game phase
    private static final double OPENING_TACTICAL_WEIGHT = 0.8; // Less tactical in opening
    private static final double OPENING_DEVELOPMENT_WEIGHT = 1.5; // More development focus
    private static final double OPENING_KING_SAFETY_WEIGHT = 1.2; // King safety important
    private static final double MIDDLEGAME_TACTICAL_WEIGHT = 1.2; // Tactical peak
    private static final double MIDDLEGAME_ACTIVITY_WEIGHT = 1.1; // Piece activity key
    private static final double MIDDLEGAME_PAWN_WEIGHT = 1.0; // Standard pawn value
    private static final double ENDGAME_TACTICAL_WEIGHT = 0.9; // Less tactical complexity
    private static final double ENDGAME_KING_ACTIVITY_WEIGHT = 1.4; // King becomes active
    private static final double ENDGAME_PAWN_WEIGHT = 1.3; // Pawns more valuable
    // Position type bonuses
    private static final int CLOSED_POSITION_BONUS = 50;
    private static final int OPEN_POSITION_BONUS = 30;
    private static final int TACTICAL_POSITION_BONUS = 100;

    private EvaluationTuner() {
	throw new IllegalStateException("Utility class");
    }

    /**
     * Analyze position type for specialized evaluation
     */
    private static PositionType analyzePositionType(final Board board) {
	final var openFiles = countOpenFiles(board);
	final var pieceCount = countPieces(board);
	final var pawnMoves = countPawnMoves(board);

	// Tactical position: few pieces, many captures possible
	if (pieceCount < 20 && hasManyCaptureOptions(board)) {
	    return PositionType.TACTICAL;
	}

	// Open position: many open files, developed pieces
	if (openFiles >= 4 && pawnMoves > 8) {
	    return PositionType.OPEN;
	}

	return openFiles <= 2 && pawnMoves < 4 ? PositionType.CLOSED : PositionType.BALANCED;
    }

    /**
     * Calculate endgame-specific bonuses
     */
    private static int calculateEndgameBonus() {
	var bonus = 0;

	// Bonus for king activity
	bonus += evaluateKingActivity();

	// Bonus for passed pawns
	bonus += evaluatePassedPawns();

	// Bonus for piece activity in endgame
	bonus += evaluateEndgamePieceActivity();

	return bonus;
    }

    /**
     * Calculate middlegame-specific bonuses
     */
    private static int calculateMiddlegameBonus() {
	var bonus = 0;

	// Bonus for piece coordination
	bonus += evaluatePieceCoordination();

	// Bonus for king safety
	bonus += evaluateKingSafety();

	// Bonus for pawn structure
	bonus += evaluatePawnStructure();

	return bonus;
    }

    /**
     * Calculate opening-specific bonuses
     */
    private static int calculateOpeningBonus(final Board board, final Side side) {
	var bonus = 0;

	// Bonus for castling
	if (side == Side.WHITE) {
	    if (board.getCastleRight(Side.WHITE) != CastleRight.NONE) {
		bonus += 50; // Can still castle
	    }
	} else if (board.getCastleRight(Side.BLACK) != CastleRight.NONE) {
	    bonus += 50; // Can still castle
	}

	// Bonus for center control
	bonus += evaluateCenterControl(board, side) * 2;

	// Bonus for piece development
	bonus += evaluatePieceDevelopment(board, side);

	return bonus;
    }

    /**
     * Calculate position-specific bonuses
     */
    public static int calculatePositionBonus(final Board board, final Side side) {
	var bonus = 0;

	final var phase = detectGamePhase(board);
	final var positionType = analyzePositionType(board);

	// Game phase bonuses
	switch (phase) {
	case OPENING -> bonus += calculateOpeningBonus(board, side);
	case MIDDLEGAME -> bonus += calculateMiddlegameBonus();
	case ENDGAME -> bonus += calculateEndgameBonus();
	default -> throw new IllegalArgumentException("Unexpected value: " + phase);
	}

	// Position type bonuses
	switch (positionType) {
	case CLOSED -> bonus += CLOSED_POSITION_BONUS;
	case OPEN -> bonus += OPEN_POSITION_BONUS;
	case TACTICAL -> bonus += TACTICAL_POSITION_BONUS;
	case BALANCED -> {
	    // No specific bonus for balanced positions
	    // Could add a small bonus for general balance
	    bonus += 10; // Small bonus for balanced position
	}
	default -> {
	    throw new IllegalArgumentException("Unexpected value: " + positionType);
	}
	}

	return bonus;
    }

    /**
     * Calculate total material on the board
     */
    private static int calculateTotalMaterial(final Board board) {
	var total = 0;

	for (final var piece : Piece.values()) {
	    if (piece == Piece.NONE) {
		continue;
	    }

	    final var bitboard = board.getBitboard(piece);
	    final var count = Long.bitCount(bitboard);

	    switch (piece.getPieceType()) {
	    case PAWN -> total += count * 100;
	    case KNIGHT, BISHOP -> total += count * 300;
	    case ROOK -> total += count * 500;
	    case QUEEN -> total += count * 900;
	    case KING -> {
		// King is not counted in material, but we can consider its position
		// for king safety evaluation
		total += 0; // King does not contribute to material score directly
	    }
	    case NONE -> {
		// No contribution for empty squares
	    }
	    default -> {
		throw new IllegalArgumentException("Unexpected piece type: " + piece.getPieceType());
	    }
	    }
	}

	return total;
    }

    // Helper methods for position analysis
    private static int countOpenFiles(final Board board) {
	var openFiles = 0;

	for (var file = 0; file < 8; file++) {
	    var hasWhitePawn = false;
	    var hasBlackPawn = false;

	    for (var rank = 0; rank < 8; rank++) {
		// Convert rank/file to Square using square names
		final var squareName = String.valueOf((char) ('a' + file)) + (rank + 1);
		final Square square;
		try {
		    square = Square.valueOf(squareName.toUpperCase());
		    final var piece = board.getPiece(square);
		    if (piece.getPieceType() == PieceType.PAWN) {
			if (piece.getPieceSide() == Side.WHITE) {
			    hasWhitePawn = true;
			} else {
			    hasBlackPawn = true;
			}
		    }
		} catch (final IllegalArgumentException e) {
		    // Invalid square, skip
		}
	    }

	    if (!hasWhitePawn && !hasBlackPawn) {
		openFiles++;
	    }
	}

	return openFiles;
    }

    private static int countPawnMoves(final Board board) {
	// Estimate pawn moves by counting pawns not on starting squares
	var moves = 0;

	// White pawns not on rank 2
	final var whitePawns = board.getBitboard(Piece.WHITE_PAWN);
	moves += Long.bitCount(whitePawns & ~0x000000000000FF00L);

	// Black pawns not on rank 7
	final var blackPawns = board.getBitboard(Piece.BLACK_PAWN);
	moves += Long.bitCount(blackPawns & ~0x00FF000000000000L);

	return moves;
    }

    private static int countPieces(final Board board) {
	var count = 0;
	for (final var piece : Piece.values()) {
	    if (piece != Piece.NONE) {
		count += Long.bitCount(board.getBitboard(piece));
	    }
	}
	return count;
    }

    /**
     * Detect the current game phase based on material count
     */
    public static GamePhase detectGamePhase(final Board board) {
	final var totalMaterial = calculateTotalMaterial(board);

	if (totalMaterial >= OPENING_PHASE_MATERIAL) {
	    return GamePhase.OPENING;
	}
	if (totalMaterial <= ENDGAME_PHASE_MATERIAL) {
	    return GamePhase.ENDGAME;
	}
	return GamePhase.MIDDLEGAME;
    }

    private static int evaluateCenterControl(final Board board, final Side side) {
	// Simple center control evaluation
	var control = 0;

	// Check center squares d4, d5, e4, e5
	final Square[] centerSquares = { Square.D4, Square.D5, Square.E4, Square.E5 };

	for (final var square : centerSquares) {
	    final var piece = board.getPiece(square);
	    if (piece.getPieceSide() == side) {
		control += piece.getPieceType() == PieceType.PAWN ? 20 : 10;
	    }
	}

	return control;
    }

    private static int evaluateEndgamePieceActivity() {
	// Simplified endgame piece activity evaluation
	return 0; // Placeholder for more complex endgame piece analysis
    }

    private static int evaluateKingActivity() {
	// Simplified king activity evaluation
	return 0; // Placeholder for more complex king activity analysis
    }

    private static int evaluateKingSafety() {
	// Simplified king safety evaluation
	return 0; // Placeholder for more complex king safety analysis
    }

    private static int evaluatePassedPawns() {
	// Simplified passed pawn evaluation
	return 0; // Placeholder for more complex passed pawn analysis
    }

    private static int evaluatePawnStructure() {
	// Simplified pawn structure evaluation
	return 0; // Placeholder for more complex pawn analysis
    }

    private static int evaluatePieceCoordination() {
	// Simplified piece coordination evaluation
	return 0; // Placeholder for more complex coordination analysis
    }

    private static int evaluatePieceDevelopment(final Board board, final Side side) {
	// Count developed pieces (not on starting squares)
	var development = 0;

	if (side == Side.WHITE) {
	    // Check if pieces moved from back rank
	    final var backRank = 0x00000000000000FFL;
	    final var whitePieces = board.getBitboard(Piece.WHITE_KNIGHT) | board.getBitboard(Piece.WHITE_BISHOP)
		    | board.getBitboard(Piece.WHITE_QUEEN);

	    development += Long.bitCount(whitePieces & ~backRank) * 10;
	} else {
	    // Check if pieces moved from back rank
	    final var backRank = 0xFF00000000000000L;
	    final var blackPieces = board.getBitboard(Piece.BLACK_KNIGHT) | board.getBitboard(Piece.BLACK_BISHOP)
		    | board.getBitboard(Piece.BLACK_QUEEN);

	    development += Long.bitCount(blackPieces & ~backRank) * 10;
	}

	return development;
    }

    /**
     * Get tuned evaluation weights based on position
     */
    public static EvaluationWeights getTunedWeights(final Board board) {
	final var phase = detectGamePhase(board);
	final var positionType = analyzePositionType(board);

	final var weights = new EvaluationWeights();

	// Base weights
	weights.materialWeight = 1.0;
	weights.tacticalWeight = 1.0;
	weights.kingSafetyWeight = 1.0;
	weights.pawnStructureWeight = 1.0;
	weights.pieceActivityWeight = 1.0;

	// Phase-specific adjustments
	switch (phase) {
	case OPENING -> {
	    weights.tacticalWeight *= OPENING_TACTICAL_WEIGHT;
	    weights.pieceActivityWeight *= OPENING_DEVELOPMENT_WEIGHT;
	    weights.kingSafetyWeight *= OPENING_KING_SAFETY_WEIGHT;
	}
	case MIDDLEGAME -> {
	    weights.tacticalWeight *= MIDDLEGAME_TACTICAL_WEIGHT;
	    weights.pieceActivityWeight *= MIDDLEGAME_ACTIVITY_WEIGHT;
	    weights.pawnStructureWeight *= MIDDLEGAME_PAWN_WEIGHT;
	}
	case ENDGAME -> {
	    weights.tacticalWeight *= ENDGAME_TACTICAL_WEIGHT;
	    weights.kingSafetyWeight *= ENDGAME_KING_ACTIVITY_WEIGHT;
	    weights.pawnStructureWeight *= ENDGAME_PAWN_WEIGHT;
	}
	default -> {
	    throw new IllegalArgumentException("Unexpected game phase: " + phase);
	}
	}

	// Position-type adjustments
	switch (positionType) {
	case CLOSED -> {
	    weights.tacticalWeight *= 0.9;
	    weights.pawnStructureWeight *= 1.2;
	}
	case OPEN -> {
	    weights.pieceActivityWeight *= 1.2;
	    weights.tacticalWeight *= 1.1;
	}
	case TACTICAL -> {
	    weights.tacticalWeight *= 1.3;
	    weights.kingSafetyWeight *= 1.1;
	}
	case BALANCED -> {
	    // Balanced positions do not require specific adjustments
	    // Could add a small bonus for general balance
	    weights.materialWeight *= 1.05; // Small boost for balanced positions
	}
	default -> {
	    throw new IllegalArgumentException("Unexpected position type: " + positionType);
	}
	}

	return weights;
    }

    private static boolean hasManyCaptureOptions(final Board board) {
	return board.legalMoves().stream().mapToInt(move -> board.getPiece(move.getTo()) != Piece.NONE ? 1 : 0)
		.sum() > 3;
    }

    /**
     * Game phase enumeration
     */
    public enum GamePhase {
	OPENING, MIDDLEGAME, ENDGAME
    }

    /**
     * Position type enumeration
     */
    public enum PositionType {
	OPEN, CLOSED, TACTICAL, BALANCED
    }

    /**
     * Evaluation weights container
     */
    public static class EvaluationWeights {
	public double materialWeight = 1.0;
	public double tacticalWeight = 1.0;
	public double kingSafetyWeight = 1.0;
	public double pawnStructureWeight = 1.0;
	public double pieceActivityWeight = 1.0;

	@Override
	public String toString() {
	    return "Weights[Material:%.2f, Tactical:%.2f, KingSafety:%.2f, Pawn:%.2f, Activity:%.2f]".formatted(
		    materialWeight, tacticalWeight, kingSafetyWeight, pawnStructureWeight, pieceActivityWeight);
	}
    }
}
