# Agent Guidelines for ChessGame

This document provides essential information for AI coding agents working on this Java chess engine with AI capabilities.

---

## Project Overview

**Type**: Java 25 Chess Application with AI Engine  
**Build System**: Gradle (see `gradle/wrapper/gradle-wrapper.properties` for exact version)  
**GUI Framework**: Eclipse SWT  
**Chess Library**: ChessLib (see `build.gradle` for exact version)  
**Package**: `io.github.seerainer.chess`

**Architecture**: Component-based evaluation system with multiple search engines (Standard, Advanced, Parallel, Tournament). Uses Strategy, Composite, and Facade patterns. Centralized configuration in `ChessConfig.java`.

---

## Build, Test & Run Commands

### Core Commands
```bash
./gradlew build                    # Build entire project
./gradlew test                     # Run all JUnit tests
./gradlew clean                    # Clean build artifacts
./gradlew run                      # Start chess application
./gradlew jar                      # Create executable JAR
```

### Test Commands
```bash
# Run all tests
./gradlew test

# Run single test class
./gradlew test --tests "ClassName"
./gradlew test --tests "ELOStrengthTest"

# Run specific test method
./gradlew test --tests "ClassName.methodName"
./gradlew test --tests "ELOStrengthTest.testBasicTactics"

# Run tests matching pattern
./gradlew test --tests "*Performance*"
./gradlew test --tests "*Tactical*"
./gradlew test --tests "*Puzzle*"

# Note: Custom test suites can be run by combining --tests filters,
# for example by package, naming convention, or patterns as above.
./gradlew benchmark                # Performance benchmarks
```

### Distribution
```bash
./gradlew distTar                  # Create tar distribution
./gradlew distZip                  # Create zip distribution
./gradlew nativeCompile            # GraalVM native image
```

---

## Code Style Guidelines

### Imports
Organize imports in groups (no blank lines between groups):
1. Java standard library (`java.*`, `javax.*`)
2. Third-party libraries (`com.github.bhlangonijr.chesslib.*`, `org.eclipse.swt.*`)
3. Project internal imports (`io.github.seerainer.chess.*`)

**No wildcard imports** except when necessary for complex cases.

```java
import java.util.HashMap;
import java.util.List;
import com.github.bhlangonijr.chesslib.Board;
import io.github.seerainer.chess.ai.MoveOrdering;
```

### Formatting
- **Tabs for indentation** (not spaces)
- **No semicolons** after class/interface definitions
- **Opening braces on same line** for methods, classes, conditionals
- **Use `final` extensively** for parameters and immutable variables
- **Use `var`** for type inference where type is obvious
- **Switch expressions** preferred over traditional switch statements

```java
public static int calculateValue(final Board board, final int depth) {
	final var result = someOperation();
	var mutableValue = 0;
	return result + mutableValue;
}
```

### Types & Null Safety
- Use `final` for all parameters: `public void method(final Type param)`
- Use `final` for local variables that won't change
- Use `var` only when type is obvious from right-hand side
- **Explicit null checks** before operations
- No null annotations library used

### Naming Conventions
- **Files**: PascalCase (`ChessAI.java`, `PositionEvaluator.java`)
- **Test files**: `ClassName + Test.java` (`ELOStrengthTest.java`)
- **Classes/Interfaces**: PascalCase, no "I" prefix for interfaces
- **Methods**: camelCase (`getBestMove()`, `evaluateBoard()`)
- **Variables**: camelCase (`bestMove`, `zobristKey`, `searchDepth`)
- **Constants**: UPPER_SNAKE_CASE (`MAX_SEARCH_DEPTH`, `KILLER_MOVES`)

### Documentation
- **JavaDoc** for all public APIs with `@param` and `@return` tags
- **Inline comments** for complex algorithms
- Use `// NEW:`, `// ENHANCED:`, `// FIXED:` for marking changes
- Use `// Safety check:` for validation code

```java
/**
 * Evaluates the board position from the given side's perspective.
 * 
 * @param board The chess board position to evaluate
 * @param depth Current search depth
 * @return The evaluation score in centipawns
 */
public int evaluateBoard(final Board board, final int depth) {
	// Safety check: Validate board state
	if (board == null) {
		return 0;
	}
	
	// NEW: Added depth-based evaluation adjustment
	final var baseScore = calculateMaterial(board);
	return baseScore + depthBonus(depth);
}
```

### Error Handling
**Always handle exceptions gracefully** with logging and safe defaults:

```java
// Pattern 1: Try-catch with logging
try {
	board.doMove(move);
	final var score = evaluate(board);
	board.undoMove();
	return score;
} catch (final Exception e) {
	System.err.println("Error executing move " + move + ": " + e.getMessage());
	return 0; // Safe default
}

// Pattern 2: Validation before operations
if (move == null || move.getFrom() == null || move.getTo() == null) {
	System.err.println("Warning: Invalid move detected: " + move);
	return;
}

// Pattern 3: Cleanup in finally blocks
try {
	// Operation
} finally {
	if (resource != null) {
		try {
			resource.dispose();
		} catch (final Exception e) {
			System.err.println("Error disposing: " + e.getMessage());
		}
	}
}
```

### Testing Patterns
Use JUnit 5 with descriptive names and timeouts:

```java
@DisplayName("AI should find checkmate in 2 moves")
@Test
@Timeout(value = 60, unit = TimeUnit.SECONDS)
void testMateInTwo() {
	// Arrange
	final var ai = new ChessAI();
	final var board = new Board();
	board.loadFromFen("rnb1kbnr/pppp1ppp/8/4p3/5PPq/8/PPPPP2P/RNBQKBNR w KQkq - 1 3");
	
	// Act
	final var bestMove = ai.getBestMove(board, 4);
	
	// Assert
	assertNotNull(bestMove, "AI should find a move");
	assertTrue(isMateMove(board, bestMove), "Should be checkmate");
	
	// Cleanup
	ai.cleanup();
}
```

---

## Architecture Patterns

### Component Organization
- **Single Responsibility**: Each evaluator handles one aspect (material, king safety, pawn structure)
- **Composition**: `ChessAI` composes multiple components, `EvaluationOrchestrator` combines evaluators
- **Facade Pattern**: `PositionEvaluator` provides simple interface to complex evaluation
- **Strategy Pattern**: Different search engines selected based on position characteristics
- **Dependency Injection**: Pass components through constructors, not static access

### Key Classes
- `Main.java` - Application entry point
- `ChessGameUI.java` - SWT-based GUI
- `ChessBoard.java` - Board representation
- `ChessAI.java` - AI coordinator (composes all AI components)
- `ai/SearchAlgorithms.java` - Minimax, PVS, quiescence search
- `ai/PositionEvaluator.java` - Evaluation facade
- `ai/evaluation/EvaluationOrchestrator.java` - Combines all evaluators
- `ai/search/*SearchEngine.java` - Multiple search engines
- `config/ChessConfig.java` - **Centralized configuration** (all magic numbers here)

### Configuration
**Always use `ChessConfig` for constants**, never hardcode magic numbers:

```java
// Good
if (depth > ChessConfig.Search.MAX_DEPTH) { ... }
final var pieceValue = ChessConfig.Evaluation.QUEEN_VALUE;

// Bad
if (depth > 10) { ... }
final var pieceValue = 900;
```

---

## Common Tasks

### Adding a New Evaluation Component
1. Implement `EvaluationComponent` interface
2. Add constants to `ChessConfig.Evaluation`
3. Register in `EvaluationOrchestrator`
4. Add unit tests in `src/test/java/io/github/seerainer/chess/test/`
5. Update integration tests

### Modifying Search Algorithm
1. Changes go in `ai/SearchAlgorithms.java` or `ai/search/` engines
2. Update configuration in `ChessConfig.Search`
3. Add performance tests
4. Run benchmarks: `./gradlew benchmark`

### Adding Configuration
1. Add to appropriate nested class in `ChessConfig.java`
2. Use `public static final` for constants
3. Document with JavaDoc comments
4. Update dependent code to use new config

---

## Important Notes

- **Thread Safety**: Use `ThreadLocal` for evaluators, support concurrent search
- **Memory Management**: Transposition table uses LRU/LFU, monitor with `SearchStatistics`
- **Performance**: Profile changes with JMH benchmarks, check transposition table hit rates
- **Defensive Programming**: Always validate inputs, handle null cases, catch exceptions
- **Modern Java**: Use switch expressions, var, streams, lambdas where appropriate
- **No Utility Class Instantiation**: Utility classes have private constructor throwing error

---

## File Locations

```
src/main/java/io/github/seerainer/chess/
  ├── Main.java, ChessGameUI.java, ChessBoard.java, ChessAI.java
  ├── ai/ - AI engine components
  │   ├── evaluation/ - 15+ evaluation components
  │   ├── search/ - Advanced search engines
  │   ├── cache/, utils/
  ├── config/ChessConfig.java - All configuration constants
  └── benchmark/PerformanceBenchmark.java

src/test/java/io/github/seerainer/chess/test/
  └── *Test.java - Comprehensive test suites
```

---

## Resources

- **ChessLib Documentation**: https://github.com/bhlangonijr/chesslib
- **Eclipse SWT Documentation**: https://www.eclipse.org/swt/
- **JUnit 5 User Guide**: https://junit.org/junit5/docs/current/user-guide/

---

**License**: Unlicense (Public Domain)
