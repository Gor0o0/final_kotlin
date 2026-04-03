package com.kotlin_ai_chess.core.model

enum class PieceType {
    KING, QUEEN, ROOK, BISHOP, KNIGHT, PAWN
}

enum class PieceColor {
    WHITE, BLACK
}

data class Piece(
    val type: PieceType,
    val color: PieceColor
)

data class Position(
    val x: Int,
    val y: Int,
    val z: Int = 0 // для 3D шахмат
)

class Board(
    val sizeX: Int = 8,
    val sizeY: Int = 8,
    val sizeZ: Int = 1
) {
    private val grid = mutableMapOf<Position, Piece>()

    fun getPiece(pos: Position): Piece? = grid[pos]

    fun setPiece(pos: Position, piece: Piece?) {
        if (piece == null) grid.remove(pos)
        else grid[pos] = piece
    }
}

data class Move(
    val from: Position,
    val to: Position,
    val promotion: PieceType? = null
)
