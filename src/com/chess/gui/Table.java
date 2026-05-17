package com.chess.gui;

import com.chess.engine.Alliance;
import com.chess.engine.ai.MiniMax;
import com.chess.engine.ai.MoveStrategy;
import com.chess.engine.board.Board;
import com.chess.engine.board.BoardUtils;
import com.chess.engine.board.Move;
import com.chess.engine.board.Move.MoveFactory;
import com.chess.engine.board.MoveTransition;
import com.chess.engine.pieces.Piece;
import com.chess.pgn.FenUtilities;

import javax.imageio.ImageIO;
import javax.swing.*;
import java.awt.*;
import java.awt.event.*;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.List;

import static javax.swing.JFrame.setDefaultLookAndFeelDecorated;
import static javax.swing.SwingUtilities.*;

public final class Table {

    private final JFrame gameFrame;
    private final GameHistoryPanel gameHistoryPanel;
    private final BoardPanel boardPanel;
    private final MoveLog moveLog;
    private final GameSetup gameSetup;
    private final GameEventManager eventManager;
    private Board chessBoard;

    private Piece sourceTile;
    private Piece humanMovedPiece;
    private BoardDirection boardDirection;
    private String pieceIconPath;
    private Color lightTileColor = Color.decode("#FFFACD");
    private Color darkTileColor = Color.decode("#593E1A");

    private static final Dimension OUTER_FRAME_DIMENSION = new Dimension(600, 600);
    private static final Dimension BOARD_PANEL_DIMENSION = new Dimension(400, 350);
    private static final Dimension TILE_PANEL_DIMENSION = new Dimension(10, 10);

    private static final Table INSTANCE = new Table();

    private Table() {
        this.eventManager = new GameEventManager();
        this.gameFrame = new JFrame("Chess - Simple PvP");
        final JMenuBar tableMenuBar = new JMenuBar();
        populateMenuBar(tableMenuBar);
        this.gameFrame.setJMenuBar(tableMenuBar);
        this.gameFrame.setLayout(new BorderLayout());
        this.chessBoard = Board.createStandardBoard();
        this.boardDirection = BoardDirection.NORMAL;

        this.pieceIconPath = "art/simple/";

        this.gameHistoryPanel = new GameHistoryPanel();
        this.boardPanel = new BoardPanel();
        this.moveLog = new MoveLog();
        this.eventManager.addGameEventListener(this::processAllGameEvents);
        this.gameSetup = new GameSetup(this.gameFrame, true);
        this.gameFrame.add(this.boardPanel, BorderLayout.CENTER);
        this.gameFrame.add(this.gameHistoryPanel, BorderLayout.EAST);
        setDefaultLookAndFeelDecorated(true);
        this.gameFrame.setDefaultCloseOperation(WindowConstants.EXIT_ON_CLOSE);
        this.gameFrame.setSize(OUTER_FRAME_DIMENSION);
        center(this.gameFrame);
        this.gameFrame.setVisible(true);
        this.gameFrame.setResizable(false);
    }

    public static Table get() {
        return INSTANCE;
    }

    public GameSetup getGameSetup() {
        return this.gameSetup;
    }

    private JFrame getGameFrame() { return this.gameFrame; }
    private Board getGameBoard() { return this.chessBoard; }
    private MoveLog getMoveLog() { return this.moveLog; }
    private BoardPanel getBoardPanel() { return this.boardPanel; }
    private GameHistoryPanel getGameHistoryPanel() { return this.gameHistoryPanel; }

    public void show() {
        Table.get().getGameHistoryPanel().redo(this.chessBoard, Table.get().getMoveLog());
        Table.get().getBoardPanel().drawBoard(Table.get().getGameBoard());
    }

    private void processAllGameEvents(final GameEvent event) {
        event.process(this);
    }

    void handleMoveMade(final Move move, final PlayerType playerType) {
        this.boardDirection = this.boardDirection.opposite();
        this.show();

        if (getGameBoard().currentPlayer().isInCheckMate()) {
            JOptionPane.showMessageDialog(getBoardPanel(), "Game Over: Checkmate!", "Game Over", JOptionPane.INFORMATION_MESSAGE);
            return;
        }
        if (getGameBoard().currentPlayer().isInStaleMate()) {
            JOptionPane.showMessageDialog(getBoardPanel(), "Game Over: Stalemate!", "Game Over", JOptionPane.INFORMATION_MESSAGE);
            return;
        }

        // Sau khi vừa có 1 nước đi: nếu LƯỢT TIẾP THEO là AI thì khởi động AI
        if (this.gameSetup.isAIPlayer(getGameBoard().currentPlayer().getAlliance())) {
            new AIThinkTank().execute();
        }
    }

    void handleGameSetupChanged(final GameSetup gameSetup) {
        // Khi user bấm "Start Game" từ dialog setup: nếu lượt đầu là AI → cho AI đi
        if (gameSetup.isAIPlayer(getGameBoard().currentPlayer().getAlliance())
                && !getGameBoard().currentPlayer().isInCheckMate()
                && !getGameBoard().currentPlayer().isInStaleMate()) {
            new AIThinkTank().execute();
        }
    }

    void handleNewGame() {
        undoAllMoves();
        this.chessBoard = Board.createStandardBoard();
        this.boardDirection = BoardDirection.NORMAL;
        this.show();
    }

    private void populateMenuBar(final JMenuBar tableMenuBar) {
        tableMenuBar.add(createFileMenu());
        tableMenuBar.add(createPreferencesMenu());
        tableMenuBar.add(createOptionsMenu());
    }

    private static void center(final JFrame frame) {
        final Dimension dim = Toolkit.getDefaultToolkit().getScreenSize();
        final int w = frame.getSize().width;
        final int h = frame.getSize().height;
        final int x = (dim.width - w) / 2;
        final int y = (dim.height - h) / 2;
        frame.setLocation(x, y);
    }

    private JMenu createFileMenu() {
        final JMenu filesMenu = new JMenu("File");
        filesMenu.setMnemonic(KeyEvent.VK_F);

        final JMenuItem openFEN = new JMenuItem("Load FEN File", KeyEvent.VK_F);
        openFEN.addActionListener(_ -> {
            String fenString = JOptionPane.showInputDialog("Input FEN");
            if(fenString != null) {
                undoAllMoves();
                this.chessBoard = FenUtilities.createGameFromFEN(fenString);
                Table.get().getBoardPanel().drawBoard(this.chessBoard);
            }
        });
        filesMenu.add(openFEN);

        final JMenuItem exitMenuItem = new JMenuItem("Exit", KeyEvent.VK_X);
        exitMenuItem.addActionListener(_ -> {
            Table.get().getGameFrame().dispose();
            System.exit(0);
        });
        filesMenu.add(exitMenuItem);

        return filesMenu;
    }

    private JMenu createOptionsMenu() {
        final JMenu optionsMenu = new JMenu("Options");
        optionsMenu.setMnemonic(KeyEvent.VK_O);

        final JMenuItem resetMenuItem = new JMenuItem("New Game", KeyEvent.VK_P);
        resetMenuItem.addActionListener(_ -> this.eventManager.publishGameEvent(new NewGameEvent()));
        optionsMenu.add(resetMenuItem);

        final JMenuItem undoMoveMenuItem = new JMenuItem("Undo last move", KeyEvent.VK_M);
        undoMoveMenuItem.addActionListener(_ -> undoLastMove());
        optionsMenu.add(undoMoveMenuItem);

        final JMenuItem setupGameMenuItem = new JMenuItem("Setup Game", KeyEvent.VK_S);
        setupGameMenuItem.addActionListener(_ -> Table.get().getGameSetup().promptUser());
        optionsMenu.add(setupGameMenuItem);

        return optionsMenu;
    }

    private JMenu createPreferencesMenu() {
        final JMenu preferencesMenu = new JMenu("Preferences");

        final JMenu colorChooserSubMenu = new JMenu("Choose Colors");
        final JMenuItem chooseDarkMenuItem = new JMenuItem("Choose Dark Tile Color");
        final JMenuItem chooseLightMenuItem = new JMenuItem("Choose Light Tile Color");

        colorChooserSubMenu.add(chooseDarkMenuItem);
        colorChooserSubMenu.add(chooseLightMenuItem);
        preferencesMenu.add(colorChooserSubMenu);

        chooseDarkMenuItem.addActionListener(_ -> {
            final Color colorChoice = JColorChooser.showDialog(Table.get().getGameFrame(), "Choose Dark Tile Color", Table.get().getGameFrame().getBackground());
            if (colorChoice != null) Table.get().getBoardPanel().setTileDarkColor(this.chessBoard, colorChoice);
        });

        chooseLightMenuItem.addActionListener(_ -> {
            final Color colorChoice = JColorChooser.showDialog(Table.get().getGameFrame(), "Choose Light Tile Color", Table.get().getGameFrame().getBackground());
            if (colorChoice != null) Table.get().getBoardPanel().setTileLightColor(this.chessBoard, colorChoice);
        });

        preferencesMenu.addSeparator();
        final JMenuItem flipBoardMenuItem = new JMenuItem("Flip board");
        flipBoardMenuItem.addActionListener(_ -> {
            this.boardDirection = this.boardDirection.opposite();
            this.boardPanel.drawBoard(this.chessBoard);
        });
        preferencesMenu.add(flipBoardMenuItem);

        preferencesMenu.addSeparator();
        return preferencesMenu;
    }

    private void undoAllMoves() {
        for(int i = Table.get().getMoveLog().size() - 1; i >= 0; i--) {
            final Move lastMove = Table.get().getMoveLog().removeMove(Table.get().getMoveLog().size() - 1);
            this.chessBoard = this.chessBoard.currentPlayer().unMakeMove(lastMove).getToBoard();
        }
        Table.get().getMoveLog().clear();
        this.show();
    }

    private void undoLastMove() {
        final MoveLog log = Table.get().getMoveLog();
        if(!log.isEmpty()) {
            final Move lastMove = Table.get().getMoveLog().removeMove(Table.get().getMoveLog().size() - 1);
            this.chessBoard = this.chessBoard.currentPlayer().unMakeMove(lastMove).getToBoard();
            this.boardDirection = this.boardDirection.opposite();
            this.show();
        }
    }

    public void moveMadeUpdate(final PlayerType playerType, final Move move) {
        this.eventManager.publishGameEvent(new MoveMadeEvent(move, playerType));
    }

    public void setupUpdate(final GameSetup gameSetup) {
        this.eventManager.publishGameEvent(new GameSetupChangedEvent(gameSetup));
    }

    public enum PlayerType { HUMAN, COMPUTER }

    private class BoardPanel extends JPanel {
        final List<TilePanel> boardTiles;
        BoardPanel() {
            super(new GridLayout(8,8));
            this.boardTiles = new ArrayList<>();
            for (int i = 0; i < BoardUtils.NUM_TILES; i++) {
                final TilePanel tilePanel = new TilePanel(this, i);
                this.boardTiles.add(tilePanel);
                add(tilePanel);
            }
            setPreferredSize(BOARD_PANEL_DIMENSION);
            setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
            setBackground(Color.decode("#8B4726"));
            validate();
        }

        void drawBoard(final Board board) {
            removeAll();
            for (final TilePanel boardTile : boardDirection.traverse(boardTiles)) {
                boardTile.drawTile(board);
                add(boardTile);
            }
            validate();
            repaint();
        }

        void setTileDarkColor(final Board board, final Color darkColor) {
            for (final TilePanel boardTile : this.boardTiles) boardTile.setDarkTileColor(darkColor);
            drawBoard(board);
        }
        void setTileLightColor(final Board board, final Color lightColor) {
            for (final TilePanel boardTile : this.boardTiles) boardTile.setLightTileColor(lightColor);
            drawBoard(board);
        }
    }

    enum BoardDirection {
        NORMAL {
            @Override List<TilePanel> traverse(final List<TilePanel> boardTiles) { return boardTiles; }
            @Override BoardDirection opposite() { return FLIPPED; }
        },
        FLIPPED {
            @Override List<TilePanel> traverse(final List<TilePanel> boardTiles) {
                final List<TilePanel> reversed = new ArrayList<>(boardTiles);
                Collections.reverse(reversed);
                return reversed;
            }
            @Override BoardDirection opposite() { return NORMAL; }
        };
        abstract List<TilePanel> traverse(final List<TilePanel> boardTiles);
        abstract BoardDirection opposite();
    }

    public static class MoveLog {
        private final List<Move> moves;
        MoveLog() { this.moves = new ArrayList<>(); }
        public boolean isEmpty() { return this.moves.isEmpty(); }
        public List<Move> getMoves() { return this.moves; }
        void addMove(final Move move) { this.moves.add(move); }
        public int size() { return this.moves.size(); }
        void clear() { this.moves.clear(); }
        Move removeMove(final int index) { return this.moves.remove(index); }
    }

    private class TilePanel extends JPanel {
        private final int tileId;
        TilePanel(final BoardPanel boardPanel, final int tileId) {
            super(new GridBagLayout());
            this.tileId = tileId;
            setPreferredSize(TILE_PANEL_DIMENSION);
            assignTileColor();
            assignTilePieceIcon(chessBoard);
            highlightTileBorder(chessBoard);
            addMouseListener(new MouseListener() {
                @Override
                public void mouseClicked(final MouseEvent event) {
                    if(BoardUtils.isEndGame(Table.get().getGameBoard())) return;
                    if (isRightMouseButton(event)) {
                        sourceTile = null;
                        humanMovedPiece = null;
                    } else if (isLeftMouseButton(event)) {
                        if (sourceTile == null) {
                            sourceTile = chessBoard.getPiece(tileId);
                            humanMovedPiece = sourceTile;
                            if (humanMovedPiece == null || humanMovedPiece.getPieceAllegiance() != chessBoard.currentPlayer().getAlliance()) {
                                sourceTile = null;
                                humanMovedPiece = null;
                            }
                        } else {
                            final Move move = MoveFactory.createMove(chessBoard, sourceTile.getPiecePosition(), tileId);
                            final MoveTransition transition = chessBoard.currentPlayer().makeMove(move);
                            if (transition.getMoveStatus().isDone()) {
                                chessBoard = transition.getToBoard();
                                moveLog.addMove(move);
                                invokeLater(() -> {
                                    Table.get().moveMadeUpdate(PlayerType.HUMAN, moveLog.getMoves().get(moveLog.size() - 1));
                                });
                            }
                            sourceTile = null;
                            humanMovedPiece = null;
                        }
                    }
                    invokeLater(() -> boardPanel.drawBoard(chessBoard));
                }
                @Override public void mouseExited(final MouseEvent e) {}
                @Override public void mouseEntered(final MouseEvent e) {}
                @Override public void mouseReleased(final MouseEvent e) {}
                @Override public void mousePressed(final MouseEvent e) {}
            });
            validate();
        }

        void drawTile(final Board board) {
            assignTileColor();
            assignTilePieceIcon(board);
            highlightTileBorder(board);
            validate();
            repaint();
        }
        void setLightTileColor(final Color color) { lightTileColor = color; }
        void setDarkTileColor(final Color color) { darkTileColor = color; }

        private void highlightTileBorder(final Board board) {
            if(humanMovedPiece != null && humanMovedPiece.getPieceAllegiance() == board.currentPlayer().getAlliance() && humanMovedPiece.getPiecePosition() == this.tileId) {
                setBorder(BorderFactory.createLineBorder(Color.cyan));
            } else {
                setBorder(BorderFactory.createLineBorder(Color.GRAY));
            }
        }

        private Collection<Move> pieceLegalMoves(final Board board) {
            if(humanMovedPiece != null && humanMovedPiece.getPieceAllegiance() == board.currentPlayer().getAlliance()) {
                return humanMovedPiece.calculateLegalMoves(board);
            }
            return Collections.emptyList();
        }

        private void assignTilePieceIcon(final Board board) {
            this.removeAll();
            if (board.getPiece(this.tileId) != null) {
                String imageFilename = pieceIconPath +
                        board.getPiece(this.tileId).getPieceAllegiance().toString().charAt(0) +
                        board.getPiece(this.tileId).toString() + ".gif";
                File imageFile = new File(imageFilename);
                System.out.println("Đang tìm file tại: " + imageFile.getAbsolutePath());
                try {
                    if (imageFile.exists()) {
                        add(new JLabel(new ImageIcon(ImageIO.read(imageFile))));
                    } else {
                        JLabel label = new JLabel(board.getPiece(this.tileId).toString());
                        label.setFont(new Font("Arial", Font.BOLD, 20));
                        label.setForeground(board.getPiece(this.tileId).getPieceAllegiance().toString().equals("WHITE") ? Color.WHITE : Color.BLACK);
                        add(label);
                    }
                } catch (final IOException e) { e.printStackTrace(); }
            }
        }

        private void assignTileColor() {
            if (BoardUtils.FIRST_ROW.get(this.tileId) || BoardUtils.THIRD_ROW.get(this.tileId) || BoardUtils.FIFTH_ROW.get(this.tileId) || BoardUtils.SEVENTH_ROW.get(this.tileId)) {
                setBackground(this.tileId % 2 == 0 ? lightTileColor : darkTileColor);
            } else if(BoardUtils.SECOND_ROW.get(this.tileId) || BoardUtils.FOURTH_ROW.get(this.tileId) || BoardUtils.SIXTH_ROW.get(this.tileId)  || BoardUtils.EIGHTH_ROW.get(this.tileId)) {
                setBackground(this.tileId % 2 != 0 ? lightTileColor : darkTileColor);
            }
        }
    }

    private static class GameEventManager {
        @FunctionalInterface interface GameEventListener { void onGameEvent(GameEvent event); }
        private final List<GameEventListener> gameListeners = new ArrayList<>();
        public void addGameEventListener(final GameEventListener listener) { this.gameListeners.add(listener); }
        public void publishGameEvent(final GameEvent event) { this.gameListeners.forEach(listener -> listener.onGameEvent(event)); }
    }

    public abstract static class GameEvent {
        public enum Type { MOVE_MADE, GAME_SETUP_CHANGED, NEW_GAME }
        private final Type type;
        protected GameEvent(Type type) { this.type = type; }
        public abstract void process(Table table);
    }

    private static class MoveMadeEvent extends GameEvent {
        private final Move move;
        private final PlayerType playerType;
        MoveMadeEvent(Move move, PlayerType playerType) { super(Type.MOVE_MADE); this.move = move; this.playerType = playerType; }
        @Override public void process(Table table) { table.handleMoveMade(move, playerType); }
    }

    private static class GameSetupChangedEvent extends GameEvent {
        private final GameSetup gameSetup;
        GameSetupChangedEvent(GameSetup gameSetup) { super(Type.GAME_SETUP_CHANGED); this.gameSetup = gameSetup; }
        @Override public void process(Table table) { table.handleGameSetupChanged(gameSetup); }
    }

    private static class NewGameEvent extends GameEvent {
        NewGameEvent() { super(Type.NEW_GAME); }
        @Override public void process(Table table) { table.handleNewGame(); }
    }

    /**
     * SwingWorker chạy thuật toán AI ở thread nền, tránh đơ GUI.
     *
     * - doInBackground() chạy ở worker thread → tự do tính toán nặng (Minimax).
     * - done() chạy lại trên Event Dispatch Thread → an toàn để update GUI.
     *
     * Type params: SwingWorker<T, V> với T = kiểu kết quả cuối cùng (Move),
     *              V = kiểu publish intermediate (không dùng).
     */
    private static class AIThinkTank extends SwingWorker<Move, String> {

        @Override
        protected Move doInBackground() {
            final int depth = Table.get().getGameSetup().getSearchDepth();
            final MoveStrategy strategy = new MiniMax(depth);
            return strategy.execute(Table.get().getGameBoard());
        }

        @Override
        protected void done() {
            try {
                final Move bestMove = get();
                if (bestMove == null) return;

                // Áp dụng nước đi của AI vào board
                final MoveTransition transition = Table.get().getGameBoard()
                        .currentPlayer().makeMove(bestMove);

                if (transition.getMoveStatus().isDone()) {
                    Table.get().chessBoard = transition.getToBoard();
                    Table.get().getMoveLog().addMove(bestMove);
                    Table.get().getGameHistoryPanel().redo(
                            Table.get().getGameBoard(), Table.get().getMoveLog());
                    Table.get().getBoardPanel().drawBoard(Table.get().getGameBoard());

                    // Báo cho hệ thống event biết AI vừa đi xong
                    // → handleMoveMade sẽ check xem bên còn lại có phải AI không (cho mode AI vs AI)
                    Table.get().moveMadeUpdate(PlayerType.COMPUTER, bestMove);
                }
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
    }
}