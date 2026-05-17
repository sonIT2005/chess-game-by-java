package com.chess.engine.ai;

import com.chess.engine.board.Board;

/**
 * Interface đánh giá điểm cho 1 thế cờ.
 * - Quy ước: điểm DƯƠNG có lợi cho WHITE, điểm ÂM có lợi cho BLACK.
 * - depth dùng để: khi gặp chiếu hết thì điểm sẽ phụ thuộc độ sâu —
 *   AI sẽ thích chiếu hết SỚM hơn là muộn.
 */
public interface BoardEvaluator {

    int evaluate(Board board, int depth);
}
