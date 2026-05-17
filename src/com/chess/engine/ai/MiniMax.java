package com.chess.engine.ai;

import com.chess.engine.board.Board;
import com.chess.engine.board.Move;
import com.chess.engine.board.MoveTransition;

import java.util.List;

/**
 * Thuật toán MiniMax + Alpha-Beta Pruning.
 *
 * GIẢI THÍCH:
 * -----------
 * - AI luôn cố tối đa hóa hàm đánh giá theo góc nhìn của chính nó.
 * - Hàm evaluate trả về điểm theo quy ước: dương = lợi cho WHITE, âm = lợi cho BLACK.
 * - Trong root, nếu AI cầm WHITE, ta tìm max của evaluate.
 *                Nếu AI cầm BLACK, ta tìm min của evaluate.
 *   → Cài đặt: searchDepth-1 lần đệ quy, mỗi tầng đảo MAX/MIN bằng cờ maximizingPlayer.
 *
 * - Alpha-Beta:
 *      alpha = giá trị tốt nhất mà MAX đã đảm bảo được (cận dưới)
 *      beta  = giá trị tốt nhất mà MIN đã đảm bảo được (cận trên)
 *      Nếu beta <= alpha → cắt nhánh (cha sẽ không bao giờ chọn nhánh này).
 *
 * - Move ordering: trước khi lặp qua các nước con, ta sort chúng (MoveSorter)
 *   để alpha/beta cập nhật nhanh → cắt nhánh sớm.
 *
 * Độ phức tạp:
 *   - Minimax thuần      : O(b^d)
 *   - Alpha-Beta best    : O(b^(d/2))      (khi move ordering hoàn hảo)
 *   - Alpha-Beta thực tế : ~O(b^(3d/4))
 *
 * Với cờ vua b≈35, depth=4 → từ 1.5M node xuống ~10k-50k node → đủ chạy <1s.
 */
public class MiniMax implements MoveStrategy {

    private final BoardEvaluator evaluator;
    private final int searchDepth;
    private long boardsEvaluated;   // Đếm số node đã xét, để báo cáo / debug

    public MiniMax(final int searchDepth) {
        this.evaluator = new StandardBoardEvaluator();
        this.searchDepth = searchDepth;
        this.boardsEvaluated = 0;
    }

    @Override
    public String toString() {
        return "MiniMax(depth=" + this.searchDepth + ")";
    }

    public long getNumBoardsEvaluated() {
        return this.boardsEvaluated;
    }

    /**
     * Entry point: tìm nước đi tốt nhất cho currentPlayer của board.
     */
    @Override
    public Move execute(final Board board) {
        final long startTime = System.currentTimeMillis();
        this.boardsEvaluated = 0;

        Move bestMove = null;
        final boolean isWhiteToMove = board.currentPlayer().getAlliance().isWhite();

        // Root: nếu AI cầm WHITE → muốn max; nếu cầm BLACK → muốn min
        int highestSeenValue = Integer.MIN_VALUE;
        int lowestSeenValue  = Integer.MAX_VALUE;
        int currentValue;

        System.out.println(board.currentPlayer() + " THINKING with depth = " + this.searchDepth);

        final List<Move> orderedMoves = MoveSorter.sort(board.currentPlayer().getLegalMoves());

        for (final Move move : orderedMoves) {
            final MoveTransition transition = board.currentPlayer().makeMove(move);
            if (!transition.getMoveStatus().isDone()) continue;

            // Sau khi đi xong, lượt thuộc về đối thủ → tầng tiếp theo ngược kiểu
            currentValue = isWhiteToMove
                    ? min(transition.getToBoard(), this.searchDepth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE)
                    : max(transition.getToBoard(), this.searchDepth - 1, Integer.MIN_VALUE, Integer.MAX_VALUE);

            if (isWhiteToMove && currentValue >= highestSeenValue) {
                highestSeenValue = currentValue;
                bestMove = move;
            } else if (!isWhiteToMove && currentValue <= lowestSeenValue) {
                lowestSeenValue = currentValue;
                bestMove = move;
            }
        }

        final long elapsed = System.currentTimeMillis() - startTime;
        System.out.printf("AI chose: %s | nodes=%d | time=%dms%n",
                bestMove, this.boardsEvaluated, elapsed);

        return bestMove;
    }

    /**
     * MAX node: WHITE đi và muốn TỐI ĐA điểm.
     */
    private int max(final Board board, final int depth, int alpha, int beta) {
        if (depth == 0 || isEndGameScenario(board)) {
            this.boardsEvaluated++;
            return this.evaluator.evaluate(board, depth);
        }

        int highestSeenValue = Integer.MIN_VALUE;
        final List<Move> orderedMoves = MoveSorter.sort(board.currentPlayer().getLegalMoves());

        for (final Move move : orderedMoves) {
            final MoveTransition transition = board.currentPlayer().makeMove(move);
            if (!transition.getMoveStatus().isDone()) continue;

            final int currentValue = min(transition.getToBoard(), depth - 1, alpha, beta);
            if (currentValue > highestSeenValue) {
                highestSeenValue = currentValue;
            }
            alpha = Math.max(alpha, currentValue);
            if (beta <= alpha) {
                break;   // Beta cut-off
            }
        }
        return highestSeenValue;
    }

    /**
     * MIN node: BLACK đi và muốn TỐI THIỂU điểm.
     */
    private int min(final Board board, final int depth, int alpha, int beta) {
        if (depth == 0 || isEndGameScenario(board)) {
            this.boardsEvaluated++;
            return this.evaluator.evaluate(board, depth);
        }

        int lowestSeenValue = Integer.MAX_VALUE;
        final List<Move> orderedMoves = MoveSorter.sort(board.currentPlayer().getLegalMoves());

        for (final Move move : orderedMoves) {
            final MoveTransition transition = board.currentPlayer().makeMove(move);
            if (!transition.getMoveStatus().isDone()) continue;

            final int currentValue = max(transition.getToBoard(), depth - 1, alpha, beta);
            if (currentValue < lowestSeenValue) {
                lowestSeenValue = currentValue;
            }
            beta = Math.min(beta, currentValue);
            if (beta <= alpha) {
                break;   // Alpha cut-off
            }
        }
        return lowestSeenValue;
    }

    /**
     * Kiểm tra ván cờ đã kết thúc chưa (chiếu hết / hết nước).
     */
    private static boolean isEndGameScenario(final Board board) {
        return board.currentPlayer().isInCheckMate()
                || board.currentPlayer().isInStaleMate();
    }
}
