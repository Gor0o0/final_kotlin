package com.kotlin_ai_chess.engine.game

import com.kotlin_ai_chess.core.model.Board
import com.kotlin_ai_chess.core.model.Move
import com.kotlin_ai_chess.core.model.PieceColor

class GameState(
    val board: Board,
    val currentPlayer: PieceColor
)

class GameEngine {

    fun applyMove(state: GameState, move: Move): GameState {
        val piece = state.board.getPiece(move.from) ?: return state

        state.board.setPiece(move.from, null)
        state.board.setPiece(move.to, piece)

        return GameState(
            board = state.board,
            currentPlayer = switchPlayer(state.currentPlayer)
        )
    }

    private fun switchPlayer(player: PieceColor) =
        if (player == PieceColor.WHITE) PieceColor.BLACK else PieceColor.WHITE
}
