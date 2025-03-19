import java.io.*;
import java.net.*;
import java.util.*;

public class Server {
    private static final int PORT = 1234;
    private static final int MAX_PLAYERS = 1; // Se requieren al menos 3 jugadores para iniciar la carrera
    private static List<ClientHandler> clients = new ArrayList<>();
    private static List<String> registeredPlayers = new ArrayList<>();
    private static boolean raceStarted = false;
    private static boolean gameEnded = false;
    private static FrmCarrera gui;

    public static void main(String[] args) {
        System.out.println("Servidor iniciado en el puerto " + PORT);
        gui = new FrmCarrera();
        gui.setVisible(true);

        try (ServerSocket serverSocket = new ServerSocket(PORT)) {
            // Acepta clientes hasta alcanzar el máximo requerido
            while (clients.size() < MAX_PLAYERS) {
                Socket clientSocket = serverSocket.accept();
                ClientHandler clientHandler = new ClientHandler(clientSocket);
                clients.add(clientHandler);
                clientHandler.start();
            }

            // Espera a que todos se registren
            while (registeredPlayers.size() < MAX_PLAYERS) {
                Thread.sleep(500);
            }

            startCountdown();
            startRace();

        } catch (IOException | InterruptedException e) {
            e.printStackTrace();
        }
    }

    // Cuenta regresiva antes de iniciar la carrera
    private static void startCountdown() {
        for (int i = 3; i > 0; i--) {
            String msg = "Comienza en: " + i;
            broadcastMessage(msg);
            gui.appendMessage(msg);
            try {
                Thread.sleep(1000);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
        }
    }

    // Envía el mensaje de inicio a todos los clientes
    private static void startRace() {
        raceStarted = true;
        broadcastMessage("START");
        gui.appendMessage("La carrera ha comenzado!");
        System.out.println("La carrera ha comenzado!");
    }

    // Envía un mensaje a todos los clientes conectados
    public static void broadcastMessage(String message) {
        for (ClientHandler client : clients) {
            client.sendMessage(message);
        }
    }

    // Actualiza la barra de progreso en la interfaz gráfica
    public static void updateProgressBar(int playerIndex, int progress) {
        gui.updateProgressBar(playerIndex, progress);
    }

    // Notifica a todos los clientes quién ganó la carrera
    public static void announceWinner(String winnerName) {
        String msg = "WINNER " + winnerName + " ha ganado la carrera!";
        broadcastMessage(msg);
        gui.announceWinner(winnerName);
        gui.appendMessage("Ganador: " + winnerName);
        System.out.println("Ganador: " + winnerName);
    }

    // Imprime y muestra un resumen de la carrera en consola y en la GUI
    private static void printRaceSummary() {
        StringBuilder summary = new StringBuilder();
        summary.append("=== Resumen de la carrera ===\n");
        List<ClientHandler> ranking = new ArrayList<>(clients);
        // Ordenar de mayor a menor progreso; en caso de empate, menor cantidad de clics es mejor.
        ranking.sort((a, b) -> {
            int cmp = Integer.compare(b.progress, a.progress);
            if (cmp == 0) {
                return Integer.compare(a.clicks, b.clicks);
            }
            return cmp;
        });
        int place = 1;
        for (ClientHandler ch : ranking) {
            summary.append(place).append(". ").append(ch.playerName)
                   .append(" - Progreso: ").append(ch.progress).append("%, Clics: ").append(ch.clicks).append("\n");
            place++;
        }
        System.out.println(summary.toString());
        gui.appendMessage(summary.toString());
    }

    // Clase interna que maneja cada conexión de cliente en un hilo independiente
    private static class ClientHandler extends Thread {
        private Socket socket;
        private PrintWriter out;
        private BufferedReader in;
        private int progress = 0;
        private String playerName = "";
        private int clicks = 0;

        ClientHandler(Socket socket) {
            this.socket = socket;
        }

        @Override
        public void run() {
            try {
                in  = new BufferedReader(new InputStreamReader(socket.getInputStream()));
                out = new PrintWriter(socket.getOutputStream(), true);

                // Solicita el nombre del jugador
                out.println("Por favor, ingresa tu nombre:");
                playerName = in.readLine();
                String regMsg = "Jugador registrado: " + playerName;
                System.out.println(regMsg);
                gui.appendMessage(regMsg);

                synchronized (registeredPlayers) {
                    registeredPlayers.add(playerName);
                }

                int playerIndex = clients.indexOf(this);
                gui.setPlayerName(playerIndex, playerName);
                String joinMsg = playerName + " se ha registrado para la carrera.";
                broadcastMessage(joinMsg);
                gui.appendMessage(joinMsg);

                while (!gameEnded) {
                    String message = in.readLine();
                    if (message == null) continue;

                    if (message.startsWith("PROGRESS")) {
                        if (!raceStarted) {
                            out.println("Esperando a que comience la carrera...");
                            continue;
                        }
                        try {
                            String[] parts = message.split(" ");
                            // Se espera el formato: "PROGRESS playerName progress clicks"
                            if (parts.length == 4) {
                                String player = parts[1];
                                int playerProgress = Integer.parseInt(parts[2]);
                                int receivedClicks = Integer.parseInt(parts[3]);

                                // Actualiza solo si el mensaje corresponde a este jugador
                                if (player.equals(this.playerName)) {
                                    progress = playerProgress;
                                    clicks = receivedClicks;
                                    broadcastMessage(message);
                                    updateProgressBar(clients.indexOf(this), progress);

                                    // Si se alcanza o supera el 100%, se finaliza la carrera
                                    if (progress >= 100) {
                                        synchronized (Server.class) {
                                            if (!gameEnded) {
                                                gameEnded = true;
                                                announceWinner(playerName);
                                                printRaceSummary();
                                                break;
                                            }
                                        }
                                    }
                                }
                            }
                        } catch (NumberFormatException e) {
                            System.out.println("Error al convertir el progreso a número: " + message);
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("Jugador desconectado.");
                gui.appendMessage("Jugador desconectado: " + playerName);
            } finally {
                closeConnection();
            }
        }

        void sendMessage(String message) {
            out.println(message);
        }

        private void closeConnection() {
            try {
                if (socket != null) socket.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
