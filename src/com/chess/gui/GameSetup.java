package com.chess.gui;

import com.chess.engine.Alliance;
import com.chess.gui.Table.PlayerType;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

/**
 * Dialog cấu hình game:
 *   - Chọn loại người chơi cho WHITE và BLACK (Human / Computer)
 *   - Chọn độ sâu tìm kiếm của AI (1-6)
 *
 * Sau khi bấm "Start Game", giá trị được lưu vào dialog này và Table sẽ đọc lại
 * thông qua các getter isAIPlayer() và getSearchDepth().
 */
public class GameSetup extends JDialog {

    private PlayerType whitePlayerType;
    private PlayerType blackPlayerType;
    private final JSpinner searchDepthSpinner;

    private static final String HUMAN_TEXT = "Human";
    private static final String COMPUTER_TEXT = "Computer";

    GameSetup(final JFrame frame, final boolean modal) {
        super(frame, modal);

        // Mặc định: White = Human, Black = AI
        this.whitePlayerType = PlayerType.HUMAN;
        this.blackPlayerType = PlayerType.COMPUTER;

        final JPanel myPanel = new JPanel(new GridBagLayout());
        getContentPane().add(myPanel);

        final JLabel title = new JLabel("Game Setup");
        title.setFont(new Font("Arial", Font.BOLD, 16));

        // --- Radio buttons cho White ---
        final JRadioButton whiteHumanButton = new JRadioButton(HUMAN_TEXT);
        final JRadioButton whiteComputerButton = new JRadioButton(COMPUTER_TEXT);
        final ButtonGroup whiteGroup = new ButtonGroup();
        whiteGroup.add(whiteHumanButton);
        whiteGroup.add(whiteComputerButton);
        whiteHumanButton.setSelected(true);

        // --- Radio buttons cho Black ---
        final JRadioButton blackHumanButton = new JRadioButton(HUMAN_TEXT);
        final JRadioButton blackComputerButton = new JRadioButton(COMPUTER_TEXT);
        final ButtonGroup blackGroup = new ButtonGroup();
        blackGroup.add(blackHumanButton);
        blackGroup.add(blackComputerButton);
        blackComputerButton.setSelected(true);

        // --- Spinner độ sâu (1..6, default 3) ---
        this.searchDepthSpinner = new JSpinner(new SpinnerNumberModel(3, 1, 6, 1));

        // Layout
        final GridBagConstraints c = new GridBagConstraints();
        c.insets = new Insets(4, 8, 4, 8);
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0; c.gridy = 0; c.gridwidth = 3;
        c.anchor = GridBagConstraints.CENTER;
        myPanel.add(title, c);

        c.gridwidth = 1;
        c.anchor = GridBagConstraints.WEST;

        c.gridx = 0; c.gridy = 1;
        myPanel.add(new JLabel("White Player:"), c);
        c.gridx = 1; myPanel.add(whiteHumanButton, c);
        c.gridx = 2; myPanel.add(whiteComputerButton, c);

        c.gridx = 0; c.gridy = 2;
        myPanel.add(new JLabel("Black Player:"), c);
        c.gridx = 1; myPanel.add(blackHumanButton, c);
        c.gridx = 2; myPanel.add(blackComputerButton, c);

        c.gridx = 0; c.gridy = 3;
        myPanel.add(new JLabel("Search Depth (AI strength):"), c);
        c.gridx = 1; c.gridwidth = 2;
        myPanel.add(this.searchDepthSpinner, c);

        // --- Nút OK / Cancel ---
        final JButton okButton = new JButton("Start Game");
        final JButton cancelButton = new JButton("Cancel");

        okButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                whitePlayerType = whiteComputerButton.isSelected() ? PlayerType.COMPUTER : PlayerType.HUMAN;
                blackPlayerType = blackComputerButton.isSelected() ? PlayerType.COMPUTER : PlayerType.HUMAN;
                GameSetup.this.setVisible(false);
                // Báo cho Table biết setup vừa thay đổi → kích hoạt AI nếu cần
                Table.get().setupUpdate(Table.get().getGameSetup());
            }
        });

        cancelButton.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                GameSetup.this.setVisible(false);
            }
        });

        final JPanel buttonPanel = new JPanel();
        buttonPanel.add(cancelButton);
        buttonPanel.add(okButton);

        c.gridx = 0; c.gridy = 4; c.gridwidth = 3;
        c.anchor = GridBagConstraints.CENTER;
        myPanel.add(buttonPanel, c);

        setLocationRelativeTo(frame);
        pack();
        setVisible(false);
    }

    /** Có phải AI đang điều khiển bên này không? */
    public boolean isAIPlayer(final Alliance alliance) {
        if (alliance == Alliance.WHITE) {
            return whitePlayerType == PlayerType.COMPUTER;
        }
        return blackPlayerType == PlayerType.COMPUTER;
    }

    public int getSearchDepth() {
        return (Integer) this.searchDepthSpinner.getValue();
    }

    public void promptUser() {
        setVisible(true);
        repaint();
    }
}
