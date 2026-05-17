package com.chess.engine.ai;

/**
 * Piece-Square Tables (PST):
 * Bảng 64 ô cho mỗi loại quân, cho biết vị trí đó "tốt" hay "xấu" với quân đó.
 *
 * Quy ước index: ô 0 = a8 (góc trên-trái theo góc nhìn White), ô 63 = h1.
 * Đây cũng chính là cách bạn đang đánh index trong project (xem Board.createStandardBoardImpl).
 *
 * Các bảng dưới đây được lấy từ tài liệu Chess Programming Wiki (PeSTO/Simplified Evaluation Function),
 * là chuẩn de-facto trong cộng đồng cờ vua máy tính.
 *
 * Với quân ĐEN: dùng method getMirroredIndex để lật bảng theo chiều dọc.
 */
public class PieceSquareTable {

    // -------- PAWN -------- (ưu tiên đẩy tốt lên, kiểm soát trung tâm)
    public static final int[] PAWN_TABLE = {
             0,   0,   0,   0,   0,   0,   0,   0,
            50,  50,  50,  50,  50,  50,  50,  50,
            10,  10,  20,  30,  30,  20,  10,  10,
             5,   5,  10,  25,  25,  10,   5,   5,
             0,   0,   0,  20,  20,   0,   0,   0,
             5,  -5, -10,   0,   0, -10,  -5,   5,
             5,  10,  10, -20, -20,  10,  10,   5,
             0,   0,   0,   0,   0,   0,   0,   0
    };

    // -------- KNIGHT -------- (mạnh ở trung tâm, yếu ở rìa)
    public static final int[] KNIGHT_TABLE = {
           -50, -40, -30, -30, -30, -30, -40, -50,
           -40, -20,   0,   0,   0,   0, -20, -40,
           -30,   0,  10,  15,  15,  10,   0, -30,
           -30,   5,  15,  20,  20,  15,   5, -30,
           -30,   0,  15,  20,  20,  15,   0, -30,
           -30,   5,  10,  15,  15,  10,   5, -30,
           -40, -20,   0,   5,   5,   0, -20, -40,
           -50, -40, -30, -30, -30, -30, -40, -50
    };

    // -------- BISHOP -------- (thích đường chéo dài)
    public static final int[] BISHOP_TABLE = {
           -20, -10, -10, -10, -10, -10, -10, -20,
           -10,   0,   0,   0,   0,   0,   0, -10,
           -10,   0,   5,  10,  10,   5,   0, -10,
           -10,   5,   5,  10,  10,   5,   5, -10,
           -10,   0,  10,  10,  10,  10,   0, -10,
           -10,  10,  10,  10,  10,  10,  10, -10,
           -10,   5,   0,   0,   0,   0,   5, -10,
           -20, -10, -10, -10, -10, -10, -10, -20
    };

    // -------- ROOK -------- (thích cột mở, hàng 7)
    public static final int[] ROOK_TABLE = {
             0,   0,   0,   0,   0,   0,   0,   0,
             5,  10,  10,  10,  10,  10,  10,   5,
            -5,   0,   0,   0,   0,   0,   0,  -5,
            -5,   0,   0,   0,   0,   0,   0,  -5,
            -5,   0,   0,   0,   0,   0,   0,  -5,
            -5,   0,   0,   0,   0,   0,   0,  -5,
            -5,   0,   0,   0,   0,   0,   0,  -5,
             0,   0,   0,   5,   5,   0,   0,   0
    };

    // -------- QUEEN --------
    public static final int[] QUEEN_TABLE = {
           -20, -10, -10,  -5,  -5, -10, -10, -20,
           -10,   0,   0,   0,   0,   0,   0, -10,
           -10,   0,   5,   5,   5,   5,   0, -10,
            -5,   0,   5,   5,   5,   5,   0,  -5,
             0,   0,   5,   5,   5,   5,   0,  -5,
           -10,   5,   5,   5,   5,   5,   0, -10,
           -10,   0,   5,   0,   0,   0,   0, -10,
           -20, -10, -10,  -5,  -5, -10, -10, -20
    };

    // -------- KING (giai đoạn middle game) --------
    // Vua thích nấp ở góc, tránh trung tâm khi còn nhiều quân
    public static final int[] KING_TABLE = {
           -30, -40, -40, -50, -50, -40, -40, -30,
           -30, -40, -40, -50, -50, -40, -40, -30,
           -30, -40, -40, -50, -50, -40, -40, -30,
           -30, -40, -40, -50, -50, -40, -40, -30,
           -20, -30, -30, -40, -40, -30, -30, -20,
           -10, -20, -20, -20, -20, -20, -20, -10,
            20,  20,   0,   0,   0,   0,  20,  20,
            20,  30,  10,   0,   0,  10,  30,  20
    };

    /**
     * Lật chỉ số ô theo chiều dọc dùng cho quân ĐEN.
     * Vì các bảng trên được thiết kế theo góc nhìn của White (hàng 8 ở trên cùng = index nhỏ),
     * còn với Black thì hàng 1 mới là gốc → ta đảo hàng:  newRow = 7 - oldRow.
     *
     * Ví dụ index 0 (a8) ↔ index 56 (a1).
     */
    public static int getMirroredIndex(int index) {
        int row = index / 8;
        int col = index % 8;
        int mirroredRow = 7 - row;
        return mirroredRow * 8 + col;
    }
}
