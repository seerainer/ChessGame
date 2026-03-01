# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).

## [0.1.2] - 2026-03-01

### Fixed

- Fixed `AdvancedSearchEngine` always playing as White — added `aiSide` field to replace hardcoded `Side.WHITE`
- Fixed move ordering not applied during search — `searchAllMoves()` and `searchAllMovesWithPVS()` in `SearchAlgorithms` now compute Zobrist hash, look up transposition table entries, and call `MoveOrdering.orderMovesAdvanced()` before iterating
- Fixed node counting never tracked — added `nodesSearched` counter to `SearchStatistics`, wired into `minimax()` and `pvSearch()`, removed `final` from `totalNodesSearched` in `ChessAI`
- Fixed out-of-bounds `THREAT_TABLE` access in `MoveOrdering` — removed duplicate access outside `isValidSquareIndex()` guard
- Fixed `timeUp` flag visibility across threads — changed to `volatile` in `SearchAlgorithms`
- Rebalanced over-weighted evaluation components: BlunderPrevention 10 to 2, TacticalSafety 5 to 2, PieceProtection 8 to 2 (combined weight 23 to 6)
- Removed artificial node limits in `AdvancedSearchEngine` (`nodesSearched > 50000` in `getBestMove()`, `nodesSearched > 10000` in `search()`) that truncated search prematurely — added proper `checkTimeUp()` calls instead
- Fixed aspiration window widening in `ChessAI` — implemented progressive widening loop (3 attempts, delta multiplied by 4) with `lastRootSearchScore` tracking
- Implemented `hasDiscoveredAttack()` in `TacticalPatternEvaluator` — ray-casting in 8 directions from vacated square with `isSlidingPieceForDirection` helper
- Fixed `canPieceAttackSquare` for sliding pieces in `ChessUtils` — added `isPathClear()` helper for blocking-aware path validation; updated `TacticalSafetyEvaluator.getLowestAttackerValue` to use it
- Fixed ProbCut in `SearchAlgorithms` — replaced stub `tryProbCut()` with reduced-depth zero-window search using +200 margin; fixed impossible condition `depth >= 4 && depth <= 3` to `depth >= 5`
- Fixed race condition on position history — wrapped `updatePositionTracking()`, `isDrawByRepetition()`, and `newGame()` in `synchronized(positionHistory)` blocks
- Fixed board/shell size mismatch — updated `ChessConfig.UI.BOARD_SIZE` from 568 to 640; `ChessBoard` now uses `ChessConfig.UI.BOARD_SIZE` instead of hardcoded value
- Fixed pawn promotion always defaulting to first match — `findLegalMove()` now collects all matching moves and explicitly selects queen promotion
- Fixed dead `aiThread` field in `ChessGameUI` — removed never-assigned field and dead code block in `stopAIThread()`
- Removed no-op `SWT.EraseItem` listener that never fires on a Canvas widget
- Added `processingMove` volatile guard in `ChessBoard` to prevent rapid double-click input issues

### Changed

- Removed `System.gc()` calls from `ChessAI.cleanupTablesIfNeeded()` and `ChessGameUI.performFinalCleanup()`
- Replaced `doMove/undoMove` check detection in `AdvancedSearchEngine.isImportantMove()` with static king-attack check using `ChessUtils.canPieceAttackSquare()`
- Replaced `doMove/undoMove` check detection in `MoveOrdering.scoreMoveForOrderingAdvanced()` with static king-square lookup
- Added lazy cached `getLegalMoves()` to `EvaluationContext` — returns `Collections.unmodifiableList` to prevent mutation
- Rewrote `SafeCheckEvaluator` to use `ChessUtils.canPieceAttackSquare()` and cached legal moves from context instead of generating full legal move lists
- Rewrote `TacticalPatternEvaluator` to use static piece checks in `evaluateKnightForks` instead of `board.legalMoves().contains()`; threaded cached legal moves through discovery attack evaluation
- Rewrote `EvaluationTuner` to use `Square.encode()` instead of string-based square lookups and `board.squareAttackedBy()` instead of `board.legalMoves().stream()` for capture detection
- Removed dead `bestMoveTable` field from `ChessAI` — was written to but never read; eliminated per-iteration `board.getFen()` string generation
- Guarded all `System.out.println` calls in `ChessAI`, `TournamentSearchEngine`, `Main`, and `ChessGameUI` behind `ChessConfig.Debug.ENABLE_DEBUG_LOGGING` — `System.err.println` in error handlers left as-is
- Converted static mutable counters to atomic types for thread safety:
  - `MoveOrdering`: `historyAge` to `AtomicInteger`; `totalMoveEvaluations`, `historyHits`, `killerHits`, `countermoveHits` to `AtomicLong`; added Javadoc documenting "benign data race" design pattern for history arrays
  - `OptimizedQuiescenceSearch`: `nodeCount`, `deltaPruningCount`, `seePruningCount` to `AtomicInteger`
  - `PositionEvaluator`: `evaluationCount`, `cacheHits` to `AtomicLong`
- Centralized hardcoded constants to `ChessConfig`:
  - `SearchAlgorithms`: replaced local `QUIESCENCE_MAX_DEPTH=8`, `NULL_MOVE_REDUCTION=3`, `NULL_MOVE_MIN_DEPTH=3`, `MAX_RAZORING_DEPTH=2` with `ChessConfig.Search.*` references
  - `TournamentSearchEngine.getPieceValue()`: replaced hardcoded switch with delegation to `MaterialEvaluator.getPieceValue()`; replaced hardcoded `-20000` mate score with `ChessConfig.Evaluation.PIECE_VALUES_KING`; replaced hardcoded `4500` time limit with `ChessConfig.AI.DEFAULT_THINK_TIME_MS - 500`
  - `SafeCheckEvaluator.PIECE_VALUES`: replaced all 7 hardcoded values with `ChessConfig.Evaluation.*` references
  - `EvaluationTuner.calculateTotalMaterial()`: replaced hardcoded `100/300/500/900` with `ChessConfig.Evaluation.*` references; split combined `KNIGHT, BISHOP -> 300` into separate `KNIGHT -> 320` and `BISHOP -> 330`
- Converted all 8 `main()`-based test files to JUnit 5 with proper assertions (18 new `@Test` methods):
  - `PieceSafetyTest` (2 tests): AI makes opening move; AI captures free queen
  - `PieceProtectionTest` (5 tests): hanging queen/rook prevention; weak piece detection; no pieces left hanging after move; 3-move game sequence
  - `SafeCheckTest` (4 tests): avoids unsafe queen/rook check; safe check preference; prefers queen capture over check
  - `SimpleAITest` (1 test): 6 consecutive opening moves with no early edge-pawn pushes
  - `MoveEvaluationDebugTest` (2 tests): all opening moves evaluated without exceptions; evaluation spread after 1.d4 Nf6
  - `EvaluationDebugTest` (2 tests): starting position roughly symmetric; knight development preferred over passive pawn move
  - `DetailedMoveAnalysis` (1 test): AdvancedSearchEngine analyzes moves at depth 3; ChessAI returns valid move
  - `DebugMoveSelection` (1 test): EvaluationOrchestrator evaluates all moves; AI selects legal move; breakdown non-empty
- Test count increased from 24 to 42 (zero invisible `main()` tests remaining)

### Removed

- Deleted 6 dead-code duplicate evaluator classes: `MaterialEvaluationComponent`, `PawnStructureEvaluator`, `PawnStructureEvaluationComponent`, `KingSafetyEvaluationComponent`, `PieceActivityEvaluationComponent`, `TacticalEvaluationComponent`
- Removed 4 broken Gradle `JavaExec` tasks (`runChessAITest`, `runSearchEngineTest`, `runSimpleTest`, `runAdvancedSearchTest`) and `runAllCustomTests` aggregate — all targeted JUnit classes with no `main()` method
- Removed unused `assertj-core` dependency (never imported in any test file)
- Removed unused `jmh-core` and `jmh-generator-annprocess` dependencies (never imported anywhere)

## [0.1.1] - 2025-12-02

### Changed
- Upgrade Java language version from 24 to 25
- Introduce performance tracking in ChessAI with total nodes searched and search time
- Implement adaptive search depth calculation based on position complexity
- Enhance iterative deepening with adaptive depth and better time management
- Add new performance statistics methods in ChessAI
- Clean up and optimize various AI components for better performance

## [0.1.0] - 2025-07-12

### Added
- Initial release with basic gameplay and AI
- Advanced search algorithms and evaluation functions
- Cross-platform support with native compilation
- Comprehensive testing suite
