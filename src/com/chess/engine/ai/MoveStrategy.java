package com.chess.engine.ai;

import com.chess.engine.board.Board;
import com.chess.engine.board.Move;

/**
 * Interface chung cho mọi chiến lược AI sinh nước đi.
 * - Mọi thuật toán AI (MiniMax, RandomAI, sau này có thể là MCTS...) đều implement interface này.
 * - Áp dụng Strategy Pattern: GUI chỉ cần biết "AI có 1 hàm execute(board) trả về nước đi tốt nhất".
 */
public interface MoveStrategy {

    /**
     * Trả về nước đi tốt nhất AI tìm được cho thế cờ hiện tại.
     * @param board thế cờ hiện tại
     * @return nước đi AI chọn
     */
    Move execute(Board board);
}
