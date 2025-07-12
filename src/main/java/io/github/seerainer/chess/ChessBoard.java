package io.github.seerainer.chess;

import static org.eclipse.swt.events.MouseListener.mouseDownAdapter;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.eclipse.swt.SWT;
import org.eclipse.swt.graphics.Color;
import org.eclipse.swt.graphics.Font;
import org.eclipse.swt.graphics.GC;
import org.eclipse.swt.widgets.Canvas;
import org.eclipse.swt.widgets.Composite;

import com.github.bhlangonijr.chesslib.Board;
import com.github.bhlangonijr.chesslib.Piece;
import com.github.bhlangonijr.chesslib.Square;
import com.github.bhlangonijr.chesslib.move.Move;

public class ChessBoard extends Canvas {
	private static final int BOARD_SIZE = 640;
	private static final int SQUARE_SIZE = BOARD_SIZE / 8;

	// Unicode chess piece symbols
	private static final Map<Piece, String> PIECE_SYMBOLS = Collections.unmodifiableMap(createPieceSymbols());

	private static Map<Piece, String> createPieceSymbols() {
		final Map<Piece, String> symbols = new HashMap<>();
		symbols.put(Piece.WHITE_KING, "♔");
		symbols.put(Piece.WHITE_QUEEN, "♕");
		symbols.put(Piece.WHITE_ROOK, "♖");
		symbols.put(Piece.WHITE_BISHOP, "♗");
		symbols.put(Piece.WHITE_KNIGHT, "♘");
		symbols.put(Piece.WHITE_PAWN, "♙");
		symbols.put(Piece.BLACK_KING, "♚");
		symbols.put(Piece.BLACK_QUEEN, "♛");
		symbols.put(Piece.BLACK_ROOK, "♜");
		symbols.put(Piece.BLACK_BISHOP, "♝");
		symbols.put(Piece.BLACK_KNIGHT, "♞");
		symbols.put(Piece.BLACK_PAWN, "♟");
		return symbols;
	}

	private static int[] getCoordinatesFromSquare(final Square square) {
		// Convert chesslib Square to visual row/col coordinates
		if (square == Square.NONE) {
			return new int[] { -1, -1 }; // Invalid coordinates
		}

		// Direct mapping using square name parsing
		final var squareName = square.toString(); // e.g., "a1", "e4", "h8"
		final var file = squareName.charAt(0) - 'a'; // 0-7 for files a-h
		final var rank = squareName.charAt(1) - '1'; // 0-7 for ranks 1-8
		final var row = 7 - rank; // Flip rank so rank 8 is at top (row 0)
		final var col = file; // File a is leftmost (col 0)
		return new int[] { row, col };
	}

	private static Square getSquareFromCoordinates(final int row, final int col) {
		// Convert visual row/col back to chesslib Square
		if (row < 0 || row >= 8 || col < 0 || col >= 8) {
			return Square.NONE;
		}

		final var rank = 7 - row; // Flip row back to rank (0-7)
		final var file = col; // File (0-7)

		// Create square name string and parse it
		final var fileChar = (char) ('a' + file);
		final var rankChar = (char) ('1' + rank);
		final var squareName = "" + fileChar + rankChar;

		// Use valueOf to get the correct square
		return Square.valueOf(squareName.toUpperCase());
	}

	private Board board;
	private final ChessGameUI gameUI;
	private Square selectedSquare;
	private List<Move> legalMoves;
	private Color lightSquareColor;
	private Color darkSquareColor;
	private Color highlightColor;
	private Color moveHintColor;
	private Font pieceFont;
	private Font smallPawnFont; // Font for smaller black pawns

	ChessBoard(final Composite parent, final Board board, final ChessGameUI gameUI) {
		super(parent, SWT.DOUBLE_BUFFERED); // Enable double buffering to reduce flickering
		this.board = board;
		this.gameUI = gameUI;

		initializeColors();
		initializeFont();
		setupEventHandlers();

		setSize(BOARD_SIZE, BOARD_SIZE);
	}

	@Override
	public void dispose() {
		if (lightSquareColor != null) {
			lightSquareColor.dispose();
		}
		if (darkSquareColor != null) {
			darkSquareColor.dispose();
		}
		if (highlightColor != null) {
			highlightColor.dispose();
		}
		if (moveHintColor != null) {
			moveHintColor.dispose();
		}
		if (pieceFont != null) {
			pieceFont.dispose();
		}
		if (smallPawnFont != null) {
			smallPawnFont.dispose();
		}
		super.dispose();
	}

	private void drawBoard(final GC gc) {
		// Draw squares first
		for (var row = 0; row < 8; row++) {
			for (var col = 0; col < 8; col++) {
				drawSquare(gc, row, col);
			}
		}

		// Draw pieces using direct coordinate mapping
		for (var row = 0; row < 8; row++) {
			for (var col = 0; col < 8; col++) {
				final var square = getSquareFromCoordinates(row, col);
				final var piece = board.getPiece(square);
				if (piece != Piece.NONE) {
					drawPieceAtPosition(gc, row, col, piece);
				}
			}
		}

		// Draw move hints
		if (selectedSquare != null && legalMoves != null) {
			drawMoveHints(gc);
		}
	}

	private void drawMoveHints(final GC gc) {
		if (legalMoves == null) {
			return;
		}

		gc.setBackground(moveHintColor);
		gc.setAlpha(128);

		legalMoves.stream().filter(move -> move.getFrom() == selectedSquare)
				.map(move -> getCoordinatesFromSquare(move.getTo())).forEach(coords -> {
					final var row = coords[0];
					final var col = coords[1];
					final var centerX = col * SQUARE_SIZE + SQUARE_SIZE / 2;
					final var centerY = row * SQUARE_SIZE + SQUARE_SIZE / 2;
					final var radius = SQUARE_SIZE / 6;
					gc.fillOval(centerX - radius, centerY - radius, radius * 2, radius * 2);
				});

		gc.setAlpha(255);
	}

	private void drawPieceAtPosition(final GC gc, final int row, final int col, final Piece piece) {
		final var symbol = PIECE_SYMBOLS.get(piece);
		if (symbol == null) {
			return;
		}

		// Use smaller font for black pawns
		if (piece == Piece.BLACK_PAWN) {
			gc.setFont(smallPawnFont);
		} else {
			gc.setFont(pieceFont);
		}

		gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_BLACK));

		// Calculate text position for centering
		final var textBounds = gc.textExtent(symbol);
		final var x = col * SQUARE_SIZE + (SQUARE_SIZE - textBounds.x) / 2;
		final var y = row * SQUARE_SIZE + (SQUARE_SIZE - textBounds.y) / 2;
		gc.drawString(symbol, x, y, true);
	}

	private void drawSquare(final GC gc, final int row, final int col) {
		final var square = getSquareFromCoordinates(row, col);
		final var isLight = (row + col) % 2 == 0;

		// Choose color
		final Color squareColor;
		if (square == selectedSquare) {
			squareColor = highlightColor;
		} else {
			squareColor = isLight ? lightSquareColor : darkSquareColor;
		}

		gc.setBackground(squareColor);
		gc.fillRectangle(col * SQUARE_SIZE, row * SQUARE_SIZE, SQUARE_SIZE, SQUARE_SIZE);

		// Draw border
		gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_BLACK));
		gc.drawRectangle(col * SQUARE_SIZE, row * SQUARE_SIZE, SQUARE_SIZE, SQUARE_SIZE);

		// Draw coordinates on edge squares
		if (row == 7) {
			gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_BLACK));
			gc.drawString(String.valueOf((char) ('a' + col)), col * SQUARE_SIZE + 5,
					row * SQUARE_SIZE + SQUARE_SIZE - 30); // Moved higher up from -20 to -30
		}
		if (col != 0) {
			return;
		}
		gc.setForeground(getDisplay().getSystemColor(SWT.COLOR_BLACK));
		gc.drawString(String.valueOf(8 - row), col * SQUARE_SIZE + 5, row * SQUARE_SIZE + 5);
	}

	private Move findLegalMove(final Square from, final Square to) {
		if (legalMoves == null) {
			return null;
		}

		// Find the legal move that matches the from and to squares
		// This will automatically handle pawn promotion since the chesslib
		// generates all legal moves including promotion moves
		// For pawn promotion, chesslib automatically generates moves that promote to
		// queen
		// If there are multiple promotion options, we'll pick the queen promotion
		return legalMoves.stream().filter(move -> move.getFrom() == from && move.getTo() == to).findFirst()
				.orElse(null);
	}

	private void handleMouseClick(final int x, final int y) {
		final var col = x / SQUARE_SIZE;
		final var row = y / SQUARE_SIZE;

		if ((col < 0) || (col >= 8) || (row < 0) || (row >= 8)) {
			return;
		}

		// Check if it's the player's turn (important for AI games)
		if (!gameUI.isPlayerTurn()) {
			return;
		}

		final var clickedSquare = getSquareFromCoordinates(row, col);
		var needsRedraw = false;

		if (selectedSquare == null) {
			// Select piece
			if (board.getPiece(clickedSquare) != Piece.NONE) {
				selectedSquare = clickedSquare;
				legalMoves = board.legalMoves();
				needsRedraw = true;
			}
		} else // Try to make a move
		if (clickedSquare == selectedSquare) {
			// Deselect
			selectedSquare = null;
			legalMoves = null;
			needsRedraw = true;
		} else {
			// Attempt move
			final var move = findLegalMove(selectedSquare, clickedSquare);
			if (move != null) {
				board.doMove(move);
				selectedSquare = null;
				legalMoves = null;
				redraw();
				gameUI.onPlayerMove(); // Notify the UI that player made a move
				return; // Early return to avoid double redraw
			}
			if (board.getPiece(clickedSquare) != Piece.NONE) {
				selectedSquare = clickedSquare;
				legalMoves = board.legalMoves();
				needsRedraw = true;
			}
		}

		// Only redraw if something actually changed
		if (needsRedraw) {
			redraw();
		}
	}

	private void initializeColors() {
		lightSquareColor = new Color(getDisplay(), 240, 217, 181);
		darkSquareColor = new Color(getDisplay(), 181, 136, 99);
		highlightColor = new Color(getDisplay(), 255, 255, 0);
		moveHintColor = new Color(getDisplay(), 0, 255, 0);
	}

	private void initializeFont() {
		pieceFont = new Font(getDisplay(), "Arial", 36, SWT.NORMAL);
		smallPawnFont = new Font(getDisplay(), "Arial", 30, SWT.NORMAL); // Smaller font for black pawns
	}

	void setBoard(final Board board) {
		this.board = board;
		this.selectedSquare = null;
		this.legalMoves = null;
	}

	private void setupEventHandlers() {
		addPaintListener(e -> drawBoard(e.gc));
		addMouseListener(mouseDownAdapter(e -> handleMouseClick(e.x, e.y)));

		// Prevent background clearing to reduce flickering
		addListener(SWT.EraseItem, event -> event.detail &= ~SWT.BACKGROUND);
	}
}
