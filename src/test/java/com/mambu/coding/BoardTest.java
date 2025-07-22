package com.mambu.coding;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.List;

public class BoardTest {

    Board board;

    @BeforeEach
    void setup() {
        board = new Board();
    }

    @Test
    void testInitialSetup() {
        // Check that red pieces are in rows 0-2 on black squares
        for (int r = 0; r < 3; r++) {
            for (int c = 0; c < 8; c++) {
                if ((r + c) % 2 == 1) {
                    assertNotNull(board.grid[r][c]);
                    assertEquals(PieceColor.RED, board.grid[r][c].color);
                } else {
                    assertNull(board.grid[r][c]);
                }
            }
        }

        // Check that white pieces are in rows 5-7 on black squares
        for (int r = 5; r < 8; r++) {
            for (int c = 0; c < 8; c++) {
                if ((r + c) % 2 == 1) {
                    assertNotNull(board.grid[r][c]);
                    assertEquals(PieceColor.WHITE, board.grid[r][c].color);
                } else {
                    assertNull(board.grid[r][c]);
                }
            }
        }

        // Middle rows should be empty
        for (int r = 3; r < 5; r++) {
            for (int c = 0; c < 8; c++) {
                assertNull(board.grid[r][c]);
            }
        }
    }

    @Test
    void testValidMoveSimple() {
        // Move a RED piece from initial position (2,1) to (3,0) - one step diagonally forward
        Position from = new Position(2, 1);
        Position to = new Position(3, 0);
        Move move = new Move(from, to);

        assertTrue(board.applyMove(move, PieceColor.RED));
        assertNull(board.grid[2][1]);
        assertNotNull(board.grid[3][0]);
        assertEquals(PieceColor.RED, board.grid[3][0].color);
    }

    @Test
    void testInvalidMoveWrongDirectionForMan() {
        // RED man tries to move backward - should fail
        Position from = new Position(2, 1);
        Position to = new Position(1, 0);
        Move move = new Move(from, to);

        assertFalse(board.applyMove(move, PieceColor.RED));
    }

    @Test
    void testCaptureMove() {
        // Setup a capture scenario:
        // Place RED at (2,1)
        // Place WHITE at (3,2)
        // Try to jump from (2,1) to (4,3)

        board.grid[2][1] = new Piece(PieceColor.RED);
        board.grid[3][2] = new Piece(PieceColor.WHITE);
        board.grid[4][3] = null;

        Move jump = new Move(new Position(2, 1), new Position(4, 3));
        assertTrue(board.applyMove(jump, PieceColor.RED));

        // WHITE piece at (3,2) should be captured (removed)
        assertNull(board.grid[3][2]);
        // RED piece moved to (4,3)
        assertNotNull(board.grid[4][3]);
        assertEquals(PieceColor.RED, board.grid[4][3].color);
        // Original position should be empty
        assertNull(board.grid[2][1]);
    }

    @Test
    void testPromotionToKing() {
        // Move RED piece to last row to promote it to King

        Position from = new Position(6, 1);
        Position to = new Position(7, 0);

        board.grid[6][1] = new Piece(PieceColor.RED);
        board.grid[7][0] = null;

        Move move = new Move(from, to);
        assertTrue(board.applyMove(move, PieceColor.RED));

        assertTrue(board.grid[7][0].isKing());
    }

	@Test
	void testUndoMove() {
		Position from = new Position(2, 1);
		Position to = new Position(3, 0);
		Move move = new Move(from, to);

		Piece beforeMovePiece = board.grid[from.row][from.col];
		board.applyMove(move, PieceColor.RED);
		board.undo();

		// The piece should be back at original position, check its properties
		Piece afterUndoPiece = board.grid[from.row][from.col];
		assertNotNull(afterUndoPiece, "Piece should be present after undo");
		assertEquals(beforeMovePiece.color, afterUndoPiece.color, "Piece color should match");
		assertEquals(beforeMovePiece.type, afterUndoPiece.type, "Piece type should match");

		// The destination should be empty
		assertNull(board.grid[to.row][to.col]);
	}

    @Test
    void testAllValidMoves() {
        List<Move> redMoves = board.allValidMoves(PieceColor.RED);
        assertFalse(redMoves.isEmpty());

        // On initial board, a RED piece at (2,1) can move to (3,0) or (3,2) if free
        boolean foundMove = redMoves.stream().anyMatch(m ->
            m.from.equals(new Position(2, 1)) &&
            (m.to.equals(new Position(3, 0)) || m.to.equals(new Position(3, 2)))
        );
        assertTrue(foundMove);
    }

    @Test
    void testPositionEqualsAndHashcode() {
        Position p1 = new Position(3, 4);
        Position p2 = new Position(3, 4);
        Position p3 = new Position(4, 3);

        assertEquals(p1, p2);
        assertNotEquals(p1, p3);
        assertEquals(p1.hashCode(), p2.hashCode());
    }
}
