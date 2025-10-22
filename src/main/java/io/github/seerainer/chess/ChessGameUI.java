package io.github.seerainer.chess;

import static org.eclipse.swt.events.SelectionListener.widgetSelectedAdapter;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.events.SelectionEvent;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.layout.GridData;
import org.eclipse.swt.layout.GridLayout;
import org.eclipse.swt.widgets.Composite;
import org.eclipse.swt.widgets.Display;
import org.eclipse.swt.widgets.Label;
import org.eclipse.swt.widgets.Menu;
import org.eclipse.swt.widgets.MenuItem;
import org.eclipse.swt.widgets.Shell;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.PieceType;
import com.github.bhlangonijr.chesslib.Side;
import com.github.bhlangonijr.chesslib.game.GameContext;
import com.github.bhlangonijr.chesslib.game.GameMode;
import com.github.bhlangonijr.chesslib.game.VariationType;

import io.github.seerainer.chess.config.ChessConfig;

public class ChessGameUI {
    private static final int MAX_MOVES_WITHOUT_PROGRESS = ChessConfig.UI.MAX_MOVES_WITHOUT_PROGRESS;
    private static final int MAX_COMPUTER_MOVES = ChessConfig.UI.MAX_COMPUTER_MOVES;

    // **NEW: Memory management constants**
    private static final int POSITION_HISTORY_LIMIT = ChessConfig.Memory.POSITION_HISTORY_LIMIT;
    private static final int CLEANUP_INTERVAL_MS = ChessConfig.Memory.CLEANUP_INTERVAL_MS;
    private static final long MAX_MEMORY_USAGE_MB = ChessConfig.Memory.MAX_MEMORY_USAGE_MB;
    private final Display display;
    private Shell shell;
    private final ChessAI ai;
    private ChessBoard chessBoard;
    private Board board;
    private Label statusLabel;
    private Label turnLabel;
    private Label gameModeLabel;
    private Thread aiThread;
    private volatile boolean aiThinking = false;
    private volatile boolean playingAgainstAI = false;
    private volatile boolean computerVsComputer = false;
    private Side playerSide = Side.WHITE;
    // **ENHANCED: Position tracking with memory management**
    private final List<String> positionHistory = new ArrayList<>();
    private final Map<String, Integer> positionCount = new HashMap<>();
    private int movesWithoutCaptureOrPawn = 0;
    // **NEW: Memory management and cleanup**
    private volatile boolean disposed = false;
    private Thread memoryCleanupThread;
    private final Object cleanupLock = new Object();
    // **NEW: Resource tracking**
    private final List<Font> allocatedFonts = new ArrayList<>();
    private final Runtime runtime = Runtime.getRuntime();

    ChessGameUI(final Display display) {
	this.display = display;
	this.board = new Board();
	this.ai = new ChessAI();
	createShell();
	createContents();

	// **NEW: Initialize memory management**
	initializeMemoryManagement();

	// **NEW: Add shell dispose listener for proper cleanup**
	shell.addDisposeListener(_ -> dispose());
    }

    /**
     * **NEW: Final cleanup operations**
     */
    private static void performFinalCleanup() {
	// Suggest aggressive garbage collection
	System.gc();
    }

    /**
     * **NEW: Clean up allocated fonts that are no longer needed**
     */
    private void cleanupAllocatedFonts() {
	synchronized (allocatedFonts) {
	    allocatedFonts.removeIf(Font::isDisposed);
	}
    }

    /**
     * **NEW: Clean up position history to prevent unbounded growth**
     */
    private void cleanupPositionHistory() {
	synchronized (positionHistory) {
	    if (positionHistory.size() > POSITION_HISTORY_LIMIT) {
		final var removeCount = positionHistory.size() - POSITION_HISTORY_LIMIT;
		for (var i = 0; i < removeCount; i++) {
		    final var removedPosition = positionHistory.remove(0);
		    final var count = positionCount.get(removedPosition);
		    if (count != null) {
			if (count <= 1) {
			    positionCount.remove(removedPosition);
			} else {
			    positionCount.put(removedPosition, count - 1);
			}
		    }
		}
	    }
	}
    }

    /**
     * **NEW: Clear position tracking data**
     */
    private void clearPositionTracking() {
	synchronized (positionHistory) {
	    positionHistory.clear();
	    positionCount.clear();
	}
    }

    private void createContents() {
	// Create menu bar
	createMenuBar();

	// Create status area
	createStatusArea();

	// Create chess board
	chessBoard = new ChessBoard(shell, board, this);
	chessBoard.setLayoutData(new GridData(SWT.FILL, SWT.FILL, true, true));

	updateUI();
    }

    private void createMenuBar() {
	final var menuBar = new Menu(shell, SWT.BAR);
	shell.setMenuBar(menuBar);

	final var fileMenuItem = new MenuItem(menuBar, SWT.CASCADE);
	fileMenuItem.setText("&File");

	final var fileMenu = new Menu(shell, SWT.DROP_DOWN);
	fileMenuItem.setMenu(fileMenu);

	final var newGameItem = new MenuItem(fileMenu, SWT.PUSH);
	newGameItem.setText("&New Game\tCtrl+N");
	newGameItem.setAccelerator(SWT.CTRL + 'N');
	newGameItem.addSelectionListener(widgetSelectedAdapter(_ -> {
	    shell.getMenuBar().getItem(1).getMenu().getItem(0).setSelection(true);
	    setGameMode(null, false, false, Side.WHITE);
	}));

	new MenuItem(fileMenu, SWT.SEPARATOR);

	final var exitItem = new MenuItem(fileMenu, SWT.PUSH);
	exitItem.setText("E&xit");
	exitItem.setAccelerator(SWT.CTRL + 'Q');
	exitItem.addSelectionListener(widgetSelectedAdapter(_ -> shell.close()));

	// Add Game menu for AI options
	final var gameMenuItem = new MenuItem(menuBar, SWT.CASCADE);
	gameMenuItem.setText("&Game");

	final var gameMenu = new Menu(shell, SWT.DROP_DOWN);
	gameMenuItem.setMenu(gameMenu);

	final var playHumanItem = new MenuItem(gameMenu, SWT.RADIO);
	playHumanItem.setText("Play vs &Human");
	playHumanItem.setSelection(!playingAgainstAI && !computerVsComputer);
	playHumanItem.addSelectionListener(widgetSelectedAdapter(e -> setGameMode(e, false, false, Side.WHITE)));

	final var playAIWhiteItem = new MenuItem(gameMenu, SWT.RADIO);
	playAIWhiteItem.setText("Play as &White vs AI");
	playAIWhiteItem.setSelection(playingAgainstAI && !computerVsComputer && playerSide == Side.WHITE);
	playAIWhiteItem.addSelectionListener(widgetSelectedAdapter(e -> setGameMode(e, true, false, Side.WHITE)));

	final var playAIBlackItem = new MenuItem(gameMenu, SWT.RADIO);
	playAIBlackItem.setText("Play as &Black vs AI");
	playAIBlackItem.setSelection(playingAgainstAI && !computerVsComputer && playerSide == Side.BLACK);
	playAIBlackItem.addSelectionListener(widgetSelectedAdapter(e -> setGameMode(e, true, false, Side.BLACK)));

	new MenuItem(gameMenu, SWT.SEPARATOR);

	final var computerVsComputerItem = new MenuItem(gameMenu, SWT.RADIO);
	computerVsComputerItem.setText("&Computer vs Computer");
	computerVsComputerItem.setSelection(computerVsComputer);
	computerVsComputerItem
		.addSelectionListener(widgetSelectedAdapter(e -> setGameMode(e, false, true, Side.WHITE)));
    }

    private void createShell() {
	shell = new Shell(display, SWT.SHELL_TRIM);
	shell.setText(ChessConfig.UI.WINDOW_TITLE); // Now using configuration

	// Set shell size based on board size configuration
	final var windowSize = ChessConfig.UI.BOARD_SIZE; // Add padding for UI elements
	shell.setSize(windowSize, windowSize); // Extra height for menu and status

	// **NEW: Set minimum size based on configuration**
	shell.setMinimumSize(windowSize + 100, windowSize + 180);

	// Center the window on screen
	final var displayBounds = display.getPrimaryMonitor().getBounds();
	final var shellBounds = shell.getBounds();
	final var x = displayBounds.x + (displayBounds.width - shellBounds.width) / 2;
	final var y = displayBounds.y + (displayBounds.height - shellBounds.height) / 2;
	shell.setLocation(x, y);

	final var layout = new GridLayout(1, false);
	shell.setLayout(layout);
    }

    /**
     * **ENHANCED: Create status area with font tracking**
     */
    private void createStatusArea() {
	final var statusComposite = new Composite(shell, SWT.NONE);
	statusComposite.setLayoutData(new GridData(SWT.FILL, SWT.TOP, true, false));
	statusComposite.setLayout(new GridLayout(3, false));

	turnLabel = new Label(statusComposite, SWT.NONE);
	turnLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
	final var fontData = turnLabel.getFont().getFontData()[0];
	fontData.setStyle(SWT.BOLD);
	fontData.setHeight(12);
	final var labelFont = new Font(display, fontData);
	turnLabel.setFont(labelFont);

	// **NEW: Track allocated font for proper disposal**
	synchronized (allocatedFonts) {
	    allocatedFonts.add(labelFont);
	}

	gameModeLabel = new Label(statusComposite, SWT.NONE);
	gameModeLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));

	statusLabel = new Label(statusComposite, SWT.NONE);
	statusLabel.setLayoutData(new GridData(SWT.FILL, SWT.CENTER, true, false));
    }

    /**
     * **NEW: Comprehensive disposal method**
     */
    void dispose() {
	if (disposed) {
	    return;
	}

	disposed = true;

	// Stop AI thread safely
	stopAIThread();

	// Clean up AI resources
	ai.cleanup();

	// Stop memory cleanup thread with configuration timeout
	if (memoryCleanupThread != null && memoryCleanupThread.isAlive()) {
	    memoryCleanupThread.interrupt();
	    try {
		memoryCleanupThread.join(ChessConfig.UI.THREAD_SHUTDOWN_TIMEOUT_MS); // Now using configuration
	    } catch (final InterruptedException e) {
		Thread.currentThread().interrupt();
	    }
	}

	// Clean up all resources
	synchronized (cleanupLock) {
	    disposeAllocatedFonts();
	    clearPositionTracking();
	}

	// Final cleanup
	performFinalCleanup();
    }

    /**
     * **NEW: Dispose all allocated fonts**
     */
    private void disposeAllocatedFonts() {
	synchronized (allocatedFonts) {
	    allocatedFonts.stream().filter((final Font font) -> !font.isDisposed()).forEach(Font::dispose);
	    allocatedFonts.clear();
	}
    }

    /**
     * Get a simplified position key for repetition detection Uses piece positions
     * and castling rights, but not en passant or move counters
     */
    private String getPositionKey() {
	final var fen = board.getFen();
	// Take only the piece positions and castling rights (first 3 parts of FEN)
	final var parts = fen.split(" ");
	if (parts.length >= 3) {
	    return new StringBuilder().append(parts[0]).append(" ").append(parts[1]).append(" ").append(parts[2])
		    .toString();
	}
	return fen;
    }

    /**
     * **NEW: Get current memory usage in MB**
     */
    private long getUsedMemoryMB() {
	return (runtime.totalMemory() - runtime.freeMemory()) / (1024 * 1024);
    }

    /**
     * **NEW: Initialize memory management and cleanup systems**
     */
    private void initializeMemoryManagement() {
	// Start memory monitoring and cleanup thread
	memoryCleanupThread = new Thread(this::memoryCleanupLoop);
	memoryCleanupThread.setDaemon(true);
	memoryCleanupThread.setName("Chess-MemoryCleanup");
	memoryCleanupThread.start();

	// Monitor memory usage
	scheduleMemoryMonitoring();
    }

    /**
     * Check if current position is a draw by threefold repetition - now using
     * configuration
     */
    private boolean isDrawByRepetition() {
	final var currentPosition = getPositionKey();
	final var count = positionCount.getOrDefault(currentPosition, Integer.valueOf(0)).intValue();
	return count >= ChessConfig.Rules.THREEFOLD_REPETITION_LIMIT; // Now using configuration
    }

    boolean isPlayerTurn() {
	if (computerVsComputer) {
	    return false; // No human interaction in computer vs computer mode
	}
	if (!playingAgainstAI) {
	    return true;
	}
	return board.getSideToMove() == playerSide && !aiThinking;
    }

    private void makeAIMove() {
	if (aiThinking) {
	    return; // Prevent multiple AI moves
	}

	// Check for draw conditions before making move
	if (computerVsComputer && isDrawByRepetition()) {
	    statusLabel.setText("Draw by repetition!");
	    statusLabel.setForeground(display.getSystemColor(SWT.COLOR_BLUE));
	    return;
	}

	if (computerVsComputer && movesWithoutCaptureOrPawn >= MAX_MOVES_WITHOUT_PROGRESS) {
	    statusLabel.setText("Draw by 50-move rule!");
	    statusLabel.setForeground(display.getSystemColor(SWT.COLOR_BLUE));
	    return;
	}

	if (computerVsComputer && board.getMoveCounter().intValue() >= MAX_COMPUTER_MOVES) {
	    statusLabel.setText("Game stopped - move limit reached!");
	    statusLabel.setForeground(display.getSystemColor(SWT.COLOR_BLUE));
	    return;
	}

	// **FIXED: Check if there are legal moves before starting AI thinking**
	final var legalMoves = board.legalMoves();
	if (legalMoves.isEmpty()) {
	    if (board.isKingAttacked()) {
		final var winner = board.getSideToMove() == Side.WHITE ? "Black" : "White";
		statusLabel
			.setText(new StringBuilder().append("Checkmate! ").append(winner).append(" wins!").toString());
		statusLabel.setForeground(display.getSystemColor(SWT.COLOR_RED));
	    } else {
		statusLabel.setText("Stalemate!");
		statusLabel.setForeground(display.getSystemColor(SWT.COLOR_BLUE));
	    }
	    return;
	}

	aiThinking = true;
	updateUI();

	// **ENHANCED: Use thread-safe async AI calculation**
	ai.getBestMoveAsync(board).thenAccept(aiMove -> {
	    if (shell.isDisposed()) {
		return;
	    }

	    display.asyncExec(() -> {
		if (shell.isDisposed()) {
		    return;
		}

		if (aiMove != null) {
		    try {
			final var piece = board.getPiece(aiMove.getFrom());
			final var type = piece.getPieceType();

			// Check if move involves capture or pawn move
			final var isCapture = board.getPiece(aiMove.getTo()) != Piece.NONE;
			final var isPawnMove = type == PieceType.PAWN;

			// **FIXED: Better error handling for piece type**
			if (piece == Piece.NONE || type == null) {
			    statusLabel.setText(new StringBuilder().append("Invalid move: ").append(aiMove)
				    .append(" (no piece found)").toString());
			    statusLabel.setForeground(display.getSystemColor(SWT.COLOR_RED));
			    aiThinking = false;
			    updateUI();
			    return;
			}

			// **FIXED: Validate move before executing**
			if (!board.legalMoves().contains(aiMove)) {
			    statusLabel.setText("Illegal move attempted: " + aiMove);
			    statusLabel.setForeground(display.getSystemColor(SWT.COLOR_RED));
			    aiThinking = false;
			    updateUI();
			    return;
			}

			board.doMove(aiMove);

			// Update position tracking
			updatePositionTracking(isCapture || isPawnMove);

			chessBoard.redraw();
			aiThinking = false;
			updateUI();

			// **FIXED: Better game continuation logic with additional safety checks**
			if (computerVsComputer) {
			    // Check all end conditions after the move
			    final var gameEnded = board.isMated() || board.isDraw() || isDrawByRepetition()
				    || movesWithoutCaptureOrPawn >= MAX_MOVES_WITHOUT_PROGRESS
				    || board.getMoveCounter().intValue() >= MAX_COMPUTER_MOVES;

			    if (!gameEnded) {
				// **FIXED: Shorter delay and better error handling**
				display.timerExec(ChessConfig.UI.AI_MOVE_DELAY_MS, () -> {
				    try {
					if (!shell.isDisposed()) {
					    makeAIMove();
					}
				    } catch (final Exception e) {
					System.err.println(
						"Error in computer vs computer continuation: " + e.getMessage());
					statusLabel.setText("Game stopped due to error");
					statusLabel.setForeground(display.getSystemColor(SWT.COLOR_RED));
				    }
				});
			    }
			}
		    } catch (final Exception e) {
			System.err.println("Error applying AI move: " + e.getMessage());
			e.printStackTrace();
			statusLabel.setText("Error applying move: " + e.getMessage());
			statusLabel.setForeground(display.getSystemColor(SWT.COLOR_RED));
			aiThinking = false;
			updateUI();
		    }
		} else {
		    // **FIXED: Better handling when AI returns null move**
		    aiThinking = false;
		    statusLabel.setText("AI could not find a move");
		    statusLabel.setForeground(display.getSystemColor(SWT.COLOR_RED));
		    updateUI();
		}
	    });
	}).exceptionally(throwable -> {
	    System.err.println("Error in AI move calculation: " + throwable.getMessage());
	    throwable.printStackTrace();

	    display.asyncExec(() -> {
		if (!shell.isDisposed()) {
		    aiThinking = false;
		    statusLabel.setText("AI error: " + throwable.getMessage());
		    statusLabel.setForeground(display.getSystemColor(SWT.COLOR_RED));
		    updateUI();
		}
	    });
	    return null;
	});
    }

    /**
     * **NEW: Memory cleanup loop running in background**
     */
    private void memoryCleanupLoop() {
	while (!disposed && !Thread.currentThread().isInterrupted()) {
	    try {
		synchronized (cleanupLock) {
		    performMemoryCleanup();
		}
		Thread.sleep(CLEANUP_INTERVAL_MS);
	    } catch (final InterruptedException e) {
		Thread.currentThread().interrupt();
		break;
	    } catch (final Exception e) {
		// Log error but continue cleanup loop
		System.err.println("Error in memory cleanup: " + e.getMessage());
	    }
	}
    }

    private void newGame(final GameMode mode) {
	// Stop any existing AI thread
	stopAIThread();

	board = new Board(new GameContext(mode, VariationType.NORMAL), false);
	chessBoard.setBoard(board);
	chessBoard.redraw();

	// Reset position tracking
	positionHistory.clear();
	positionCount.clear();
	movesWithoutCaptureOrPawn = 0;

	updateUI();

	// Start AI moves based on game mode
	if (computerVsComputer || (playingAgainstAI && playerSide == Side.BLACK)) {
	    // In computer vs computer mode, start immediately
	    makeAIMove();
	}
    }

    void onPlayerMove() {
	// Called after player makes a move
	updateUI();

	// Check if game is over
	if (board.isMated() || board.isDraw()) {
	    return;
	}

	// If playing against AI and it's AI's turn, make AI move
	if (playingAgainstAI && !computerVsComputer && board.getSideToMove() != playerSide) {
	    makeAIMove();
	}
    }

    Shell open() {
	shell.open();
	return shell;
    }

    /**
     * **NEW: Aggressive memory cleanup when memory is low**
     */
    private void performAggressiveCleanup() {
	performRoutineCleanup();

	// More aggressive position history cleanup
	synchronized (positionHistory) {
	    if (positionHistory.size() > POSITION_HISTORY_LIMIT / 2) {
		final var removeCount = positionHistory.size() - POSITION_HISTORY_LIMIT / 2;
		for (var i = 0; i < removeCount; i++) {
		    final var removedPosition = positionHistory.remove(0);
		    final var count = positionCount.get(removedPosition);
		    if (count != null) {
			if (count <= 1) {
			    positionCount.remove(removedPosition);
			} else {
			    positionCount.put(removedPosition, count - 1);
			}
		    }
		}
	    }
	}

	// Force garbage collection
	performFinalCleanup();
    }

    /**
     * **NEW: Perform comprehensive memory cleanup**
     */
    private void performMemoryCleanup() {
	if (disposed) {
	    return;
	}

	final var usedMemory = getUsedMemoryMB();

	// Aggressive cleanup if memory usage is high
	if (usedMemory > MAX_MEMORY_USAGE_MB) {
	    performAggressiveCleanup();
	} else {
	    performRoutineCleanup();
	}

	// Suggest garbage collection if needed
	if (usedMemory > MAX_MEMORY_USAGE_MB * 0.8) {
	    performFinalCleanup();
	}
    }

    /**
     * **NEW: Routine memory cleanup**
     */
    private void performRoutineCleanup() {
	// Clean up position history if it's getting too large
	cleanupPositionHistory();

	// Clean up any disposed fonts
	cleanupAllocatedFonts();
    }

    /**
     * **NEW: Schedule periodic memory monitoring using configuration intervals**
     */
    private void scheduleMemoryMonitoring() {
	display.timerExec((int) ChessConfig.Memory.MEMORY_CHECK_INTERVAL_MS, () -> { // Now using configuration
	    if (!disposed) {
		final var usedMemory = getUsedMemoryMB();
		final var maxMemory = runtime.maxMemory() / (1024 * 1024);
		final var memoryPercentage = (double) usedMemory / maxMemory * 100;

		// Use configuration threshold for memory cleanup
		final var cleanupThreshold = ChessConfig.Memory.MEMORY_CLEANUP_THRESHOLD * 100;

		// Log memory usage if it's above threshold
		if (memoryPercentage > cleanupThreshold) {
		    System.out.printf("Chess Engine Memory Usage: %d/%d MB (%.1f%%) - Above cleanup threshold%n",
			    usedMemory, maxMemory, memoryPercentage);

		    // Trigger cleanup when above threshold
		    performAggressiveCleanup();
		}

		// Schedule next monitoring
		scheduleMemoryMonitoring();
	    }
	});
    }

    private void setGameMode(final SelectionEvent e, final boolean vsAI, final boolean computerVsComputer,
	    final Side playerSide) {
	// Stop any existing AI thread when changing game mode
	stopAIThread();

	this.playingAgainstAI = vsAI;
	this.computerVsComputer = computerVsComputer;
	this.playerSide = playerSide;

	final GameMode gameMode;
	if (computerVsComputer) {
	    gameMode = GameMode.MACHINE_VS_MACHINE;
	} else if (playingAgainstAI) {
	    gameMode = playerSide == Side.WHITE ? GameMode.HUMAN_VS_MACHINE : GameMode.MACHINE_VS_HUMAN;
	} else {
	    gameMode = GameMode.HUMAN_VS_HUMAN;
	}
	// Ignore event if the menu item is not selected
	if (e != null && !((MenuItem) e.widget).getSelection()) {
	    return;
	}
	newGame(gameMode);
    }

    /**
     * Safely stop the AI thread if it's running
     */
    private void stopAIThread() {
	if (aiThinking) {
	    // Cancel the AI search
	    ai.cancelSearch();
	    aiThinking = false;
	}

	// Clean up the old thread-based approach if still present
	if (aiThread == null || !aiThread.isAlive()) {
	    return;
	}
	aiThread.interrupt();
	try {
	    aiThread.join(3000); // Wait up to 3 seconds
	} catch (final InterruptedException e) {
	    Thread.currentThread().interrupt();
	}
	aiThread = null;
    }

    /**
     * Update position tracking for repetition detection
     */
    private void updatePositionTracking(final boolean resetCounter) {
	final var currentPosition = getPositionKey();

	// Add current position to history
	final var value = Integer
		.valueOf(positionCount.getOrDefault(currentPosition, Integer.valueOf(0)).intValue() + 1);
	positionHistory.add(currentPosition);
	positionCount.put(currentPosition, value);

	// Update 50-move rule counter
	if (resetCounter) {
	    movesWithoutCaptureOrPawn = 0;
	} else {
	    movesWithoutCaptureOrPawn++;
	}

	// Keep position history manageable (last 100 positions)
	if (positionHistory.size() <= 100) {
	    return;
	}
	final var oldPosition = positionHistory.remove(0);
	final var count = positionCount.get(oldPosition).intValue();
	if (count <= 1) {
	    positionCount.remove(oldPosition);
	} else {
	    positionCount.put(oldPosition, Integer.valueOf(count - 1));
	}
    }

    void updateUI() {
	if (board == null) {
	    return;
	}

	final var currentSide = board.getSideToMove() == Side.WHITE ? "White" : "Black";
	turnLabel.setText("Turn: " + currentSide);

	final var menu = shell.getMenuBar().getItem(1).getMenu();
	for (final var item : menu.getItems()) {
	    if (item.getStyle() == SWT.RADIO) {
		item.setSelection(false);
	    }
	}

	// Update game mode label and menu selection
	if (computerVsComputer) {
	    gameModeLabel.setText(new StringBuilder().append("Computer vs Computer (Move: ")
		    .append(board.getMoveCounter()).append(")").toString());
	    menu.getItem(4).setSelection(true); // Computer vs Computer menu item
	} else if (playingAgainstAI) {
	    gameModeLabel.setText("Player: " + (playerSide == Side.WHITE ? "White" : "Black"));
	    menu.getItem(1 + (playerSide == Side.WHITE ? 0 : 1)).setSelection(true);
	} else {
	    gameModeLabel.setText("Human vs Human");
	    menu.getItem(0).setSelection(true);
	}

	if (aiThinking) {
	    if (computerVsComputer) {
		statusLabel.setText(new StringBuilder().append("Computer (").append(currentSide)
			.append(") is thinking...").toString());
	    } else {
		statusLabel.setText("AI is thinking...");
	    }
	    statusLabel.setForeground(display.getSystemColor(SWT.COLOR_BLUE));
	} else if (board.isKingAttacked()) {
	    if (board.isMated()) {
		final var winner = board.getSideToMove() == Side.WHITE ? "Black" : "White";
		statusLabel
			.setText(new StringBuilder().append("Checkmate! ").append(winner).append(" wins!").toString());
		statusLabel.setForeground(display.getSystemColor(SWT.COLOR_RED));
	    } else {
		statusLabel.setText("Check!");
		statusLabel.setForeground(display.getSystemColor(SWT.COLOR_MAGENTA));
	    }
	} else if (board.isDraw()) {
	    statusLabel.setText("Draw!");
	    statusLabel.setForeground(display.getSystemColor(SWT.COLOR_BLUE));
	} else if (computerVsComputer && isDrawByRepetition()) {
	    statusLabel.setText("Draw by repetition!");
	    statusLabel.setForeground(display.getSystemColor(SWT.COLOR_BLUE));
	} else if (computerVsComputer && movesWithoutCaptureOrPawn >= MAX_MOVES_WITHOUT_PROGRESS) {
	    statusLabel.setText("Draw by 50-move rule!");
	    statusLabel.setForeground(display.getSystemColor(SWT.COLOR_BLUE));
	} else {
	    statusLabel.setText("Game in progress");
	    statusLabel.setForeground(display.getSystemColor(SWT.COLOR_BLACK));
	}
    }
}
