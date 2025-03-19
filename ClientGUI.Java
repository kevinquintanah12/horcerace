import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;

public class ClientGUI {
    private static final String SERVER_IP = "localhost";
    private static final int SERVER_PORT = 1234;

    private int progress = 0;
    private int clickCount = 0; // Contador de pulsaciones
    private PrintWriter out;
    private JFrame frame;
    private JButton btnAvanzar;
    private JLabel lblProgreso;
    private JLabel lblEstado;
    private boolean gameEnded = false;
    private boolean gameStarted = false;
    private String playerName;

    public ClientGUI() {
        frame = new JFrame("Carrera de Caballos");
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.setSize(300, 200);
        frame.setLayout(new FlowLayout());

        lblProgreso = new JLabel("Progreso: 0%");
        frame.add(lblProgreso);

        lblEstado = new JLabel("Esperando jugadores...");
        frame.add(lblEstado);

        btnAvanzar = new JButton("Avanzar");
        btnAvanzar.setEnabled(false); // Inicialmente deshabilitado
        btnAvanzar.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent e) {
                if (gameStarted && !gameEnded) {
                    aumentarProgreso();
                }
            }
        });
        frame.add(btnAvanzar);
        frame.setVisible(true);

        solicitarNombre();
        conectarServidor();
    }

    private void solicitarNombre() {
        playerName = JOptionPane.showInputDialog(frame, "Ingresa el nombre de tu caballo:");
        if (playerName == null || playerName.trim().isEmpty()) {
            playerName = "Jugador" + (int) (Math.random() * 1000);
        }
    }

    private void conectarServidor() {
        new Thread(() -> {
            try (Socket socket = new Socket(SERVER_IP, SERVER_PORT);
                 BufferedReader in = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                 PrintWriter writer = new PrintWriter(socket.getOutputStream(), true)) {

                out = writer;
                out.println(playerName); // Enviar el nombre al servidor

                String message;
                while ((message = in.readLine()) != null) {
                    if (message.equals("START")) {
                        iniciarCuentaRegresiva();
                    } else if (message.startsWith("PROGRESS")) {
                        String[] parts = message.split(" ");
                        // Se espera: "PROGRESS playerName progress clickCount"
                        if (parts.length >= 4) {
                            String player = parts[1];
                            int playerProgress = Integer.parseInt(parts[2]);
                            int receivedClickCount = Integer.parseInt(parts[3]);

                            if (player.equals(playerName)) { // Solo actualizar si es nuestro progreso
                                progress = playerProgress;
                                clickCount = receivedClickCount;
                                SwingUtilities.invokeLater(() -> 
                                    lblProgreso.setText("Progreso: " + progress + "% (Clicks: " + clickCount + ")")
                                );
                            }
                        }
                    } else if (message.startsWith("WINNER")) {
                        gameEnded = true;
                        JOptionPane.showMessageDialog(frame, message);
                        btnAvanzar.setEnabled(false);
                        break;
                    } else if (message.startsWith("WAITING")) {
                        lblEstado.setText(message); // Mostrar jugadores conectados
                    }
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void iniciarCuentaRegresiva() {
        new Thread(() -> {
            try {
                for (int i = 3; i > 0; i--) {
                    final int count = i;
                    SwingUtilities.invokeLater(() -> lblEstado.setText("Comienza en: " + count));
                    Thread.sleep(1000);
                }
                SwingUtilities.invokeLater(() -> {
                    lblEstado.setText("¡INICIA!");
                    gameStarted = true;
                    btnAvanzar.setEnabled(true);
                });
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }).start();
    }

    private void aumentarProgreso() {
        if (progress < 100) {
            progress += 2;
            clickCount++; // Contar pulsaciones
            lblProgreso.setText("Progreso: " + progress + "% (Clicks: " + clickCount + ")");

            if (out != null) {
                // Enviamos 4 partes: comando, nombre, progreso y número de clics
                out.println("PROGRESS " + playerName + " " + progress + " " + clickCount);
            }
        }
    }

    public static void main(String[] args) {
        SwingUtilities.invokeLater(ClientGUI::new);
    }
}
