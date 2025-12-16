package io.github.seerainer.chess.ai.evaluation;

import java.util.ArrayList;
import java.util.List;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Side;

/**
 * Orchestrates evaluation components following the Composite pattern and
 * Open/Closed principle. This class is open for extension (adding new
 * components) but closed for modification.
 */
public class EvaluationOrchestrator {
    private final List<EvaluationComponent> components;

    public EvaluationOrchestrator() {
	this.components = new ArrayList<>();
	initializeDefaultComponents();
    }

    /**
     * Get tuned weight multiplier for specific component
     */
    private static double getTunedWeightForComponent(final EvaluationComponent component,
	    final EvaluationTuner.EvaluationWeights weights) {
	final var componentName = component.getComponentName();

	return switch (componentName) {
	case "Material Evaluator" -> weights.materialWeight;
	case "Simple Tactical Evaluator", "Tactical Safety Evaluator", "Tactical Pattern Evaluator" ->
	    weights.tacticalWeight;
	case "King Safety Evaluator" -> weights.kingSafetyWeight;
	case "Pawn Structure Evaluator", "Advanced Pawn Structure" -> weights.pawnStructureWeight;
	case "Piece Activity Evaluator" -> weights.pieceActivityWeight;
	default -> 1.0; // No tuning for unknown components
	};
    }

    /**
     * Add a new evaluation component (Open/Closed principle)
     */
    public void addComponent(final EvaluationComponent component) {
	components.add(component);
    }

    /**
     * Main evaluation method that delegates to all registered components Enhanced
     * with dynamic tuning based on position characteristics
     */
    public int evaluatePosition(final Board board, final Side evaluatingSide) {
	// Fast terminal position checks
	if (board.isMated()) {
	    return evaluatingSide == board.getSideToMove() ? -100000 : 100000;
	}
	if (board.isDraw()) {
	    return 0;
	}

	// Get tuned weights based on current position
	final var tunedWeights = EvaluationTuner.getTunedWeights(board);

	final var context = new EvaluationContext(board, evaluatingSide);

	var totalScore = 0;
	for (final var component : components) {
	    try {
		final var componentScore = component.evaluate(context);
		final var weight = component.getWeight(context);

		// Apply both component weight and tuned weight
		final var tunedWeight = getTunedWeightForComponent(component, tunedWeights);
		totalScore += (int) (componentScore * weight * tunedWeight);
	    } catch (final Exception e) {
		// Log error but continue evaluation with other components
		System.err.println(new StringBuilder().append("Error in component ")
			.append(component.getComponentName()).append(": ").append(e.getMessage()).toString());
	    }
	}

	// Add position-specific bonus from tuner
	totalScore += EvaluationTuner.calculatePositionBonus(board, evaluatingSide);

	return totalScore;
    }

    /**
     * Get evaluation breakdown for debugging
     */
    public String getEvaluationBreakdown(final Board board, final Side evaluatingSide) {
	final var context = new EvaluationContext(board, evaluatingSide);
	final var breakdown = new StringBuilder();

	var totalScore = 0;
	for (final var component : components) {
	    try {
		final var componentScore = component.evaluate(context);
		final var weight = component.getWeight(context);
		final var weightedScore = (int) (componentScore * weight);

		breakdown.append("%s: %d (weight: %.2f) = %d%n".formatted(component.getComponentName(), componentScore,
			weight, weightedScore));

		totalScore += weightedScore;
	    } catch (final Exception e) {
		breakdown.append("%s: ERROR - %s%n".formatted(component.getComponentName(), e.getMessage()));
	    }
	}

	breakdown.append("Total: %d%n".formatted(totalScore));
	return breakdown.toString();
    }

    /**
     * Initialize default evaluation components - now with tactical safety and
     * patterns
     */
    private void initializeDefaultComponents() {
	// Add BLUNDER PREVENTION FIRST - absolutely critical
	addComponent(new BlunderPreventionEvaluator());

	// Add SAFE CHECK EVALUATOR - prevents unsafe checks
	addComponent(new SafeCheckEvaluator());

	// Add TACTICAL SAFETY - most critical for preventing blunders
	addComponent(new TacticalSafetyEvaluator());

	// Add PIECE PROTECTION - critical for preventing hanging pieces
	addComponent(new PieceProtectionEvaluator());

	// Add ADVANCED TACTICAL PATTERNS - critical for spotting tactics
	addComponent(new TacticalPatternEvaluator());

	// Add ENHANCED POSITIONAL EVALUATION - piece-square tables with game phase
	addComponent(new PositionalEvaluationComponent());

	// Add core evaluation components
	addComponent(new MaterialEvaluator());
	addComponent(new PieceActivityEvaluator());
	addComponent(new KingSafetyEvaluator());
	addComponent(new PawnStructureEvaluatorAdvanced());

	// Add Endgame Evaluator
	addComponent(new EndgameEvaluator());
    }
}