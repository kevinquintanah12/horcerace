import javax.swing.*;

public class FrmCarrera extends JFrame {
    private JProgressBar[] progressBars;
    private JLabel lblGanador;
    private JLabel[] playerNames;
    private JButton btnAvanzar;
    private JTextArea txtLog; // Área para mostrar mensajes de la carrera

    public FrmCarrera() {
        setTitle("Carrera de Caballos");
        setSize(600, 450);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLayout(null);

        // Se manejarán 3 jugadores.
        progressBars = new JProgressBar[3];
        playerNames = new JLabel[3];

        for (int i = 0; i < 3; i++) {
            playerNames[i] = new JLabel("Jugador " + (i + 1));
            playerNames[i].setBounds(20, (i * 40) + 20, 100, 30);
            add(playerNames[i]);

            progressBars[i] = new JProgressBar(0, 100);
            progressBars[i].setBounds(120, (i * 40) + 20, 400, 30);
            progressBars[i].setStringPainted(true);
            add(progressBars[i]);
        }

        lblGanador = new JLabel("Esperando ganador...");
        lblGanador.setBounds(250, 220, 300, 30);
        add(lblGanador);

        btnAvanzar = new JButton("Avanzar");
        btnAvanzar.setBounds(250, 260, 100, 30);
        btnAvanzar.setEnabled(false); // Se deshabilita hasta que la carrera inicie.
        add(btnAvanzar);

        txtLog = new JTextArea();
        txtLog.setEditable(false);
        JScrollPane scrollPane = new JScrollPane(txtLog);
        scrollPane.setBounds(20, 300, 560, 100);
        add(scrollPane);
    }

    // Actualiza la barra de progreso del jugador en la posición playerIndex.
    public void updateProgressBar(int playerIndex, int progress) {
        if (playerIndex >= 0 && playerIndex < progressBars.length) {
            progressBars[playerIndex].setValue(progress);
        }
    }

    // Asigna o actualiza el nombre del jugador en la posición playerIndex.
    public void setPlayerName(int playerIndex, String name) {
        if (playerIndex >= 0 && playerIndex < playerNames.length) {
            playerNames[playerIndex].setText(name);
        }
    }

    // Muestra en la interfaz el ganador de la carrera.
    public void announceWinner(String winnerName) {
        lblGanador.setText("¡Ganador: " + winnerName + "!");
    }

    // Habilita el botón "Avanzar".
    public void enableButton() {
        btnAvanzar.setEnabled(true);
    }

    // Agrega un mensaje al área de log de la GUI.
    public void appendMessage(String message) {
        txtLog.append(message + "\n");
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(() -> new FrmCarrera().setVisible(true));
    }
}
