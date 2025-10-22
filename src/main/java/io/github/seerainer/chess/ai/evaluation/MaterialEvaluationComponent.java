package io.github.seerainer.chess.ai.evaluation;

/**
 * Material evaluation component following Single Responsibility Principle
 */
public class MaterialEvaluationComponent implements EvaluationComponent {

    @Override
    public int evaluate(final EvaluationContext context) {
	return MaterialEvaluator.evaluateMaterial(context.getBoard());
    }

    @Override
    public String getComponentName() {
	return "Material";
    }

    @Override
    public double getWeight(final EvaluationContext context) {
	// Material is always the most important factor
	return 1.0;
    }
}