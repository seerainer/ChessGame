package io.github.seerainer.chess.ai.evaluation;

/**
 * King safety evaluation component following Single Responsibility Principle
 */
public class KingSafetyEvaluationComponent implements EvaluationComponent {

    @Override
    public int evaluate(final EvaluationContext context) {
	return KingSafetyEvaluator.evaluateKingSafety(context.getBoard(), context.isEndGame());
    }

    @Override
    public String getComponentName() {
	return "KingSafety";
    }

    @Override
    public double getWeight(final EvaluationContext context) {
	// King safety is more important in middlegame
	return context.isEndGame() ? 0.5 : 1.5;
    }
}