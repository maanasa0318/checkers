package com.mambu.coding;

import java.util.*;

enum PieceColor {
    RED, WHITE
}

enum PieceType {
    MAN, KING
}

class Piece {
    PieceColor color;
    PieceType type;

    Piece(PieceColor color) {
        this.color = color;
        this.type = PieceType.MAN;
    }

    /** Promotes the piece to a KING type. */
    void promote() {
        this.type = PieceType.KING;
    }

    /** Checks if the piece is a KING. */
    boolean isKing() {
        return type == PieceType.KING;
    }
}

class Position {
    int row, col;

    Position(int row, int col) {
        this.row = row;
        this.col = col;
    }

    /** Checks if two Position objects are equal based on row and col values. */
    public boolean equals(Object o) {
        if (this == o)
            return true;
        if (!(o instanceof Position))
            return false;
        Position p = (Position) o;
        return row == p.row && col == p.col;
    }

    /** Generates a hash code for Position for use in hash-based collections. */
    public int hashCode() {
        return Objects.hash(row, col);
    }

    /** Returns board-style notation (e.g., row=5, col=2 -> "c3"). */
    @Override
    public String toString() {
        return "" + (char) ('a' + col) + (8 - row);
    }
}

class Move {
    Position from, to;

    Move(Position f, Position t) {
        this.from = f;
        this.to = t;
    }
}

class Board {
    Piece[][] grid = new Piece[8][8];
    Stack<Piece[][]> history = new Stack<>();

    Board() {
        init();
    }

    /** Initializes the board by placing RED and WHITE pieces in their starting positions. */
    void init() {
        for (int r = 5; r < 8; r++)
            for (int c = 0; c < 8; c++)
                if ((r + c) % 2 == 1)
                    grid[r][c] = new Piece(PieceColor.WHITE);
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 8; c++)
                if ((r + c) % 2 == 1)
                    grid[r][c] = new Piece(PieceColor.RED);
    }

    /** Displays the current state of the board with pieces and empty squares. */
    void display() {
        System.out.println("  a b c d e f g h");
        for (int r = 0; r < 8; r++) {
            System.out.print(8 - r + " ");
            for (int c = 0; c < 8; c++) {
                Piece p = grid[r][c];
                if (p == null)
                    System.out.print(". ");
                else {
                    char ch = p.color == PieceColor.RED ? 'r' : 'w';
                    if (p.isKing())
                        ch = Character.toUpperCase(ch);
                    System.out.print(ch + " ");
                }
            }
            System.out.println(8 - r);
        }
        System.out.println("  a b c d e f g h");
    }

    /** Checks if a position is within the 8x8 board. */
    boolean isValid(Position pos) {
        return pos.row >= 0 && pos.row < 8 && pos.col >= 0 && pos.col < 8;
    }

    /**
     * Attempts to apply a move for a player.
     * Validates source and destination, enforces movement and capture rules,
     * updates board state, and promotes pieces if reaching the opposite side.
     * Saves the current state for undo before making the move.
     */
    boolean applyMove(Move m, PieceColor player) {
        Position from = m.from, to = m.to;
        if (!isValid(from) || !isValid(to))
            return false;
        Piece piece = grid[from.row][from.col];
        if (piece == null || piece.color != player)
            return false;
        if (grid[to.row][to.col] != null)
            return false;

        saveState();

        int dr = to.row - from.row;
        int dc = Math.abs(to.col - from.col);
        if (dc != Math.abs(dr))
            return false;

        if (Math.abs(dr) == 1) { // Simple move
            if (!piece.isKing()) {
                if (piece.color == PieceColor.RED && dr != 1)
                    return false;
                if (piece.color == PieceColor.WHITE && dr != -1)
                    return false;
            }
            grid[to.row][to.col] = piece;
            grid[from.row][from.col] = null;
        } else if (Math.abs(dr) == 2) { // Capture move
            int mr = (from.row + to.row) / 2, mc = (from.col + to.col) / 2;
            Piece mid = grid[mr][mc];
            if (mid == null || mid.color == player)
                return false;
            grid[to.row][to.col] = piece;
            grid[from.row][from.col] = null;
            grid[mr][mc] = null;
        } else
            return false;

        // Promote if reaching the opposite end
        if ((piece.color == PieceColor.RED && to.row == 7) || (piece.color == PieceColor.WHITE && to.row == 0))
            piece.promote();
        return true;
    }

    /** Saves the current board state by deep-copying the grid into the history stack for undo. */
    void saveState() {
        Piece[][] snap = new Piece[8][8];
        for (int i = 0; i < 8; i++)
            for (int j = 0; j < 8; j++) {
                Piece p = grid[i][j];
                if (p != null) {
                    Piece clone = new Piece(p.color);
                    if (p.isKing())
                        clone.promote();
                    snap[i][j] = clone;
                }
            }
        history.push(snap);
    }

    /** Restores the most recent saved state of the board (undo functionality). */
    void undo() {
        if (!history.isEmpty())
            grid = history.pop();
    }

    /**
     * Generates all valid simple moves and capture moves for the specified player.
     * Considers forward movement and jumps based on piece color (non-king pieces).
     */
    /**
     * Generates all valid simple moves and capture moves for the specified player.
     * For MAN pieces: only forward diagonal moves (based on color).
     * For KING pieces: can move diagonally in all four directions.
     */
    List<Move> allValidMoves(PieceColor player) {
        List<Move> moves = new ArrayList<>();
        for (int r = 0; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                Piece p = grid[r][c];
                if (p != null && p.color == player) {
                    // Direction vectors: Kings can move in all four diagonals,
                    // normal men only forward based on their color.
                    int[] drs = p.isKing() ? new int[] { -1, 1 } : 
                                 (p.color == PieceColor.RED ? new int[] { 1 } : new int[] { -1 });
                    int[] dcs = { -1, 1 };

                    for (int dr : drs) {
                        for (int dc : dcs) {
                            // Simple move
                            Position step = new Position(r + dr, c + dc);
                            if (isValid(step) && grid[step.row][step.col] == null)
                                moves.add(new Move(new Position(r, c), step));

                            // Capture move
                            Position jump = new Position(r + 2 * dr, c + 2 * dc);
                            if (isValid(jump)) {
                                int mr = r + dr, mc = c + dc;
                                if (grid[mr][mc] != null && grid[mr][mc].color != player
                                        && grid[jump.row][jump.col] == null)
                                    moves.add(new Move(new Position(r, c), jump));
                            }
                        }
                    }
                }
            }
        }
        return moves;
    }

}

public class CheckersGame {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        Board board = new Board();
        PieceColor human = PieceColor.RED;
        PieceColor computer = PieceColor.WHITE;
        PieceColor current = human;
        Random rand = new Random();

        while (true) {
            board.display();
            if (current == human) {
                System.out.println("Your move ( Red(r) e.g., b6-c5) or 'undo' or 'exit':");
                String input = sc.nextLine();
                if (input.equalsIgnoreCase("exit"))
                    break;
                if (input.equalsIgnoreCase("undo")) {
                    board.undo();
                    current = (current == human) ? computer : human;
                    continue;
                }
                String[] parts = input.split("-");
                if (parts.length != 2) {
                    System.out.println("Invalid input.");
                    continue;
                }
                Position from = parsePosition(parts[0]);
                Position to = parsePosition(parts[1]);
                if (!board.applyMove(new Move(from, to), human))
                    System.out.println("Invalid move.");
                else
                    current = computer;
            } else {
                List<Move> moves = board.allValidMoves(computer);
                if (moves.isEmpty()) {
                    System.out.println("Computer has no moves. You win!");
                    break;
                }
                Move choice = moves.get(rand.nextInt(moves.size()));
                board.applyMove(choice, computer);
                System.out.println("Computer moves white(w) from " + toNotation(choice.from) + " to " + toNotation(choice.to));
                current = human;
            }
        }
    }

    /** Converts a string like "b6" to a Position object (zero-based row and column). */
    static Position parsePosition(String s) {
        int col = s.charAt(0) - 'a';
        int row = 8 - Character.getNumericValue(s.charAt(1));
        return new Position(row, col);
    }

    /** Converts a Position object to standard board notation (e.g., row=5,col=2 -> "c3"). */
    static String toNotation(Position p) {
        return "" + (char) ('a' + p.col) + (8 - p.row);
    }
}
