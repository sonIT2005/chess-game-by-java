package com.chess.engine.ai;

import com.chess.engine.Alliance;
import com.chess.engine.board.Board;
import com.chess.engine.pieces.Piece;
import com.chess.engine.player.Player;

/**
 * Hàm đánh giá thế cờ chuẩn.
 *
 *   score(board) = score(WHITE) - score(BLACK)
 *
 * Mỗi bên được cộng dồn các thành phần:
 *   1. Material           : tổng giá trị quân còn sống
 *   2. Piece-Square       : điểm vị trí (PST)
 *   3. Mobility           : số nước đi hợp lệ * 1
 *   4. Check              : đang chiếu đối thủ → +50
 *   5. CheckMate          : chiếu hết → +10000 (nhân với depth để ưu tiên thắng SỚM)
 *   6. Castled            : đã nhập thành → +60
 *   7. Attack bonus       : ăn được quân nặng hơn → +1
 *
 * Hàm này được gọi RẤT NHIỀU LẦN trong Minimax, nên giữ cho đơn giản và nhanh.
 */
public class StandardBoardEvaluator implements BoardEvaluator {

    private static final int CHECK_BONUS = 50;
    private static final int CHECK_MATE_BONUS = 10000;
    private static final int DEPTH_BONUS = 100;
    private static final int CASTLE_BONUS = 60;
    private static final int MOBILITY_MULTIPLIER = 1;
    private static final int ATTACK_MULTIPLIER = 1;

    @Override
    public int evaluate(final Board board, final int depth) {
        return scorePlayer(board, board.whitePlayer(), depth)
             - scorePlayer(board, board.blackPlayer(), depth);
    }

    private int scorePlayer(final Board board, final Player player, final int depth) {
        return pieceValueAndPosition(board, player)
             + mobility(player)
             + check(player)
             + checkmate(player, depth)
             + castled(player)
             + attacks(player);
    }

    /**
     * Cộng material + PST cho mỗi quân của player.
     * getActivePieces() trong project trả về int[] (mảng tọa độ), nên ta phải tra Board.getPiece(coord).
     */
    private int pieceValueAndPosition(final Board board, final Player player) {
        int score = 0;
        final Alliance alliance = player.getAlliance();
        for (final int coordinate : player.getActivePieces()) {
            final Piece piece = board.getPiece(coordinate);
            if (piece == null) continue;

            score += piece.getPieceValue();
            score += getPositionScore(piece, alliance);
        }
        return score;
    }

    /**
     * Tra PST. Với BLACK ta phải lật bảng theo chiều dọc.
     */
    private int getPositionScore(final Piece piece, final Alliance alliance) {
        final int rawIndex = piece.getPiecePosition();
        // Bảng PST trong project này được viết theo góc nhìn White (index 0 = a8 ở trên)
        // Nhưng index trong code của bạn cũng đã đi từ 0 ở trên xuống 63 ở dưới (xem createStandardBoardImpl).
        // → Với WHITE: dùng nguyên index. Với BLACK: lật dọc.
        final int idx = alliance.isWhite() ? rawIndex : PieceSquareTable.getMirroredIndex(rawIndex);

        switch (piece.getPieceType()) {
            case PAWN:   return PieceSquareTable.PAWN_TABLE[idx];
            case KNIGHT: return PieceSquareTable.KNIGHT_TABLE[idx];
            case BISHOP: return PieceSquareTable.BISHOP_TABLE[idx];
            case ROOK:   return PieceSquareTable.ROOK_TABLE[idx];
            case QUEEN:  return PieceSquareTable.QUEEN_TABLE[idx];
            case KING:   return PieceSquareTable.KING_TABLE[idx];
            default:     return 0;
        }
    }

    private int mobility(final Player player) {
        return MOBILITY_MULTIPLIER * player.getLegalMoves().size();
    }

    private int check(final Player player) {
        return player.getOpponent().isInCheck() ? CHECK_BONUS : 0;
    }

    /**
     * Chiếu hết là mục tiêu cuối → cho điểm cực lớn.
     * Nhân thêm depth để AI ưu tiên chiếu hết ở độ sâu nông (= chiếu hết SỚM hơn).
     */
    private int checkmate(final Player player, final int depth) {
        return player.getOpponent().isInCheckMate()
                ? CHECK_MATE_BONUS * depthBonus(depth)
                : 0;
    }

    private int depthBonus(final int depth) {
        return depth == 0 ? 1 : DEPTH_BONUS * depth;
    }

    private int castled(final Player player) {
        return player.getPlayerKing().isCastled() ? CASTLE_BONUS : 0;
    }

    /**
     * Thưởng nhỏ cho mỗi nước "tấn công lên quân giá trị cao hơn".
     * Encourage trao đổi có lợi.
     */
    private int attacks(final Player player) {
        int attackScore = 0;
        // Note: Move của project bạn có 1 layer subclass (AttackMove). Để giữ code gọn và không
        // phụ thuộc API nội bộ Move, ta tạm bỏ trống. Đây là chỗ để bạn TỰ MỞ RỘNG sau này.
        return ATTACK_MULTIPLIER * attackScore;
    }
}
