package com.mambu.coding;

import java.util.*;

/**
 * SmartCheckersGame implements a checkers game using a HashMap for board state,
 * LinkedList history for undo, and an adjacency graph for possible moves.
 * 
 * Rules enforced:
 * - Man pieces move only forward (no backward simple moves)
 * - King pieces move both forward and backward
 * - Captures (jumps) allowed in all diagonal directions for all pieces
 */
class SmartMove {
    Position from, to;

    SmartMove(Position f, Position t) {
        this.from = f;
        this.to = t;
    }
}

class SmartBoard {
    Map<Position, Piece> pieces = new HashMap<>();
    LinkedList<Map<Position, Piece>> history = new LinkedList<>();
    Map<Position, List<Position>> adjacency = new HashMap<>();

    /**
     * Initialize board and adjacency graph.
     */
    SmartBoard() {
        init();
        buildAdjacency();
    }

    /**
     * Initialize pieces on the board at starting positions for both colors.
     */
    void init() {
        for (int r = 5; r < 8; r++)
            for (int c = 0; c < 8; c++)
                if ((r + c) % 2 == 1)
                    pieces.put(new Position(r, c), new Piece(PieceColor.WHITE));
        for (int r = 0; r < 3; r++)
            for (int c = 0; c < 8; c++)
                if ((r + c) % 2 == 1)
                    pieces.put(new Position(r, c), new Piece(PieceColor.RED));
    }

    /**
     * Builds adjacency list graph for all dark squares (valid move positions).
     * Each position maps to its diagonal neighbors.
     */
    void buildAdjacency() {
        for (int r = 0; r < 8; r++)
            for (int c = 0; c < 8; c++)
                if ((r + c) % 2 == 1) {
                    Position p = new Position(r, c);
                    adjacency.put(p, new ArrayList<>());
                    int[] dr = {-1, 1};
                    int[] dc = {-1, 1};
                    for (int drr : dr)
                        for (int dcc : dc) {
                            Position neigh = new Position(r + drr, c + dcc);
                            if (isValid(neigh))
                                adjacency.get(p).add(neigh);
                        }
                }
    }

    /**
     * Checks if the position is inside the 8x8 board.
     */
    boolean isValid(Position pos) {
        return pos.row >= 0 && pos.row < 8 && pos.col >= 0 && pos.col < 8;
    }

    /**
     * Displays the current board state, with:
     * - 'r' for red man, 'R' for red king
     * - 'w' for white man, 'W' for white king
     * - '.' for empty squares
     */
    void display() {
        System.out.println("  a b c d e f g h");
        for (int r = 0; r < 8; r++) {
            System.out.print(8 - r + " ");
            for (int c = 0; c < 8; c++) {
                Position pos = new Position(r, c);
                Piece p = pieces.get(pos);
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

    /**
     * Saves a deep copy of the current pieces map to history for undo.
     */
    void saveState() {
        Map<Position, Piece> snapshot = new HashMap<>();
        for (Map.Entry<Position, Piece> e : pieces.entrySet()) {
            Piece orig = e.getValue();
            Piece clone = new Piece(orig.color);
            if (orig.isKing())
                clone.promote();
            snapshot.put(new Position(e.getKey().row, e.getKey().col), clone);
        }
        history.addFirst(snapshot);
    }

    /**
     * Restores the most recent saved board state from history (undo).
     */
    void undo() {
        if (!history.isEmpty())
            pieces = history.removeFirst();
    }

    /**
     * DFS to find all multi-jump capture chains starting from current position.
     * Captures are allowed in all diagonal directions.
     */
    void dfsJumps(Position start, Position current, PieceColor player, List<Position> path,
                  List<List<Position>> results) {
        boolean extended = false;
        for (Position neighbor : adjacency.getOrDefault(current, List.of())) {
            int dr = neighbor.row - current.row;
            int dc = neighbor.col - current.col;
            Position jump = new Position(current.row + 2 * dr, current.col + 2 * dc);
            Position mid = new Position(current.row + dr, current.col + dc);
            if (isValid(jump) && !pieces.containsKey(jump) && pieces.containsKey(mid)
                    && pieces.get(mid).color != player) {
                extended = true;
                Piece captured = pieces.get(mid);
                pieces.remove(mid);
                path.add(jump);
                dfsJumps(start, jump, player, path, results);
                path.remove(path.size() - 1);
                pieces.put(mid, captured);
            }
        }
        if (!extended && path.size() > 1) {
            results.add(new ArrayList<>(path));
        }
    }

    /**
     * Generates all valid moves (simple and jump chains) for a player.
     * Man pieces move only forward; kings move both directions.
     * Captures allowed in all diagonal directions.
     */
    List<SmartMove> allValidMoves(PieceColor player) {
        List<SmartMove> moves = new ArrayList<>();
        for (Position pos : new ArrayList<>(pieces.keySet())) {
            Piece p = pieces.get(pos);
            if (p == null || p.color != player)
                continue;

            for (Position neighbor : adjacency.getOrDefault(pos, List.of())) {
                // Simple moves (non-captures)
                if (!pieces.containsKey(neighbor)) {
                    if (p.isKing()) {
                        // Kings can move forward and backward
                        moves.add(new SmartMove(pos, neighbor));
                    } else {
                        // Men can move only forward
                        int dir = (p.color == PieceColor.RED) ? 1 : -1;
                        if ((neighbor.row - pos.row) == dir)
                            moves.add(new SmartMove(pos, neighbor));
                    }
                }

                // Capture jumps allowed in any diagonal direction for all pieces
                int dr = neighbor.row - pos.row;
                int dc = neighbor.col - pos.col;
                Position jump = new Position(pos.row + 2 * dr, pos.col + 2 * dc);
                Position mid = new Position(pos.row + dr, pos.col + dc);
                if (isValid(jump) && !pieces.containsKey(jump) && pieces.containsKey(mid)
                        && pieces.get(mid).color != player) {
                    moves.add(new SmartMove(pos, jump));
                }
            }

            // Multi-jump capture chains
            List<List<Position>> chains = new ArrayList<>();
            dfsJumps(pos, pos, player, new ArrayList<>(List.of(pos)), chains);
            for (List<Position> chain : chains) {
                moves.add(new SmartMove(chain.get(0), chain.get(chain.size() - 1)));
            }
        }
        return moves;
    }
}

public class SmartCheckersGame {
    public static void main(String[] args) {
        Scanner sc = new Scanner(System.in);
        SmartBoard board = new SmartBoard();
        PieceColor human = PieceColor.RED;
        PieceColor ai = PieceColor.WHITE;
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
                    current = (current == human) ? ai : human;
                    continue;
                }
                String[] parts = input.split("-");
                if (parts.length != 2) {
                    System.out.println("Invalid input");
                    continue;
                }
                Position from = parsePosition(parts[0]);
                Position to = parsePosition(parts[1]);
                // Validate and make move only if valid
                List<SmartMove> validMoves = board.allValidMoves(current);
                boolean valid = false;
                for (SmartMove m : validMoves) {
                    if (m.from.equals(from) && m.to.equals(to)) {
                        valid = true;
                        break;
                    }
                }
                if (!valid) {
                    System.out.println("Invalid move.");
                    continue;
                }
                board.saveState();
                board.pieces.put(to, board.pieces.remove(from));

                // Promote man to king if reaches opposite side
                Piece movedPiece = board.pieces.get(to);
                if (!movedPiece.isKing()) {
                    if ((movedPiece.color == PieceColor.RED && to.row == 7) ||
                            (movedPiece.color == PieceColor.WHITE && to.row == 0)) {
                        movedPiece.promote();
                    }
                }
                current = ai;
            } else {
                List<SmartMove> moves = board.allValidMoves(ai);
                if (moves.isEmpty()) {
                    System.out.println("Computer has no moves. You win!");
                    break;
                }
                SmartMove move = moves.get(rand.nextInt(moves.size()));
                board.saveState();
                board.pieces.put(move.to, board.pieces.remove(move.from));

                // Promote man to king if reaches opposite side
                Piece movedPiece = board.pieces.get(move.to);
                if (!movedPiece.isKing()) {
                    if ((movedPiece.color == PieceColor.RED && move.to.row == 7) ||
                            (movedPiece.color == PieceColor.WHITE && move.to.row == 0)) {
                        movedPiece.promote();
                    }
                }

                System.out.println("Computer moves white(w) from " + move.from.toString() + " to " + move.to.toString());
                current = human;
            }
        }
    }

    /**
     * Parses board notation string (e.g., "b6") to Position object.
     */
    static Position parsePosition(String s) {
        int col = s.charAt(0) - 'a';
        int row = 8 - Character.getNumericValue(s.charAt(1));
        return new Position(row, col);
    }
}



