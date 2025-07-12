package io.github.seerainer.chess.ai.evaluation;

/**
 * Interface for evaluation components following Single Responsibility Principle
 */
public interface EvaluationComponent {
	/**
	 * Evaluate this component and return the score
	 *
	 * @param context The evaluation context containing board state and cached data
	 * @return The evaluation score for this component
	 */
	int evaluate(EvaluationContext context);

	/**
	 * Get the name of this evaluation component for debugging
	 */
	String getComponentName();

	/**
	 * Get the weight/importance of this evaluation component
	 *
	 * @param context The evaluation context for dynamic weighting
	 * @return The weight multiplier for this component
	 */
	default double getWeight(final EvaluationContext context) {
		return 1.0;
	}
}