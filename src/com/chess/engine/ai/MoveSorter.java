package com.chess.engine.ai;

import com.chess.engine.board.Move;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Sắp xếp danh sách nước đi để Alpha-Beta pruning hiệu quả hơn.
 *
 * Nguyên lý: nếu ta xét NƯỚC TỐT trước, alpha sẽ tăng nhanh → cắt nhánh nhiều hơn.
 *
 * Tiêu chí ưu tiên (cao xuống thấp):
 *   1. Castling          : ưu tiên nhập thành (king safety)
 *   2. Nước ăn quân (MVV-LVA):
 *        score = 10 * giá_trị_quân_bị_ăn - giá_trị_quân_đi
 *        (Pawn ăn Queen được ưu tiên hơn Queen ăn Pawn)
 *   3. Nước thường       : score = 0
 */
public class MoveSorter {

    public static List<Move> sort(final Collection<Move> moves) {
        final List<Move> sorted = new ArrayList<>(moves);
        sorted.sort(MOVE_COMPARATOR);
        return sorted;
    }

    private static final Comparator<Move> MOVE_COMPARATOR = (m1, m2) ->
            Integer.compare(score(m2), score(m1));   // giảm dần

    private static int score(final Move move) {
        if (move.isCastlingMove()) {
            return 10000;
        }
        if (move.isAttack()) {
            // MVV-LVA: Most Valuable Victim - Least Valuable Attacker
            return 10 * move.getAttackedPiece().getPieceValue()
                    - move.getMovedPiece().getPieceValue();
        }
        return 0;
    }
}
