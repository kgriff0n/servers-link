package io.github.kgriff0n.socket;

import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.packet.Packet;
import io.github.kgriff0n.packet.info.NewServerPacket;
import io.github.kgriff0n.packet.info.ServerStatusPacket;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static io.github.kgriff0n.ServersLink.IS_RUNNING;
import static io.github.kgriff0n.ServersLink.SERVER;

public class SubServer extends Thread {

    private static SubServer connection;

    private ExecutorService executor;

    /** List of player UUIDs that can connect */
    private ArrayList<UUID> waitingPlayers;

    private String ip;
    private int port;

    public static SubServer getInstance() {
        return connection;
    }

    private Socket clientSocket;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    public SubServer(String ip, int port) {
        this.ip = ip;
        this.port = port;
        if (connection == null) {
            waitingPlayers = new ArrayList<>();

            connection = this;
            executor  = Executors.newSingleThreadExecutor();
        } else {
            ServersLink.LOGGER.error("Connection already established");
        }
    }

    public synchronized void send(Packet packet) {
        if (executor.isShutdown()) {
            ServersLink.LOGGER.warn("Can't send {}", packet.getClass().getName());
        } else {
            executor.submit(() -> {
                try {
                    if (out != null) {
                        out.writeObject(packet);
                        out.flush();
                        out.reset();
                    }
                } catch (IOException e) {
                    ServersLink.LOGGER.error("Unable to send {}", packet.getClass().getName());
                }
            });
        }
    }

    public ArrayList<UUID> getWaitingPlayers() {
        return this.waitingPlayers;
    }

    public void addWaitingPlayer(UUID uuid) {
        this.waitingPlayers.add(uuid);
    }

    public void removeWaitingPlayer(UUID uuid) {
        this.waitingPlayers.remove(uuid);
    }

    @Override
    public void run() {
        while (IS_RUNNING) {
            try {
                if (clientSocket == null || clientSocket.isClosed() || !clientSocket.isConnected()) {
                    try {
                        ServersLink.LOGGER.info("Connecting to gateway...");
                        clientSocket = new Socket(ip, port);
                        out = new ObjectOutputStream(clientSocket.getOutputStream());
                        out.flush();
                        in = new ObjectInputStream(clientSocket.getInputStream());

                        // Send handshake packets
                        send(new NewServerPacket(ServersLink.getServerInfo()));
                        send(new ServerStatusPacket(ServersLink.getServerInfo().getName(), 20.0f, false));

                        ServersLink.LOGGER.info("Connected to gateway!");
                    } catch (IOException e) {
                        ServersLink.LOGGER.error("Unable to connect to gateway: {}", e.getMessage());
                        try {
                            if (clientSocket != null) clientSocket.close();
                        } catch (IOException ex) {
                            // ignore
                        }
                        clientSocket = null;
                        out = null;
                        in = null;

                        try {
                            Thread.sleep(60000); // Wait 1 min
                        } catch (InterruptedException ex) {
                            break;
                        }
                        continue;
                    }
                }

                try {
                    Packet pkt = ((Packet)in.readObject());
                    SERVER.execute(pkt::onReceive);
                } catch (IOException e) {
                    ServersLink.LOGGER.error("Gateway disconnected");
                    try {
                        if (clientSocket != null) clientSocket.close();
                    } catch (IOException ex) {
                        // ignore
                    }
                    clientSocket = null;
                    out = null;
                    in = null;

                    try {
                        Thread.sleep(60000); // Wait 1 min
                    } catch (InterruptedException ex) {
                        break;
                    }
                } catch (ClassNotFoundException e) {
                    ServersLink.LOGGER.error("Receive invalid data");
                }
            } catch (Exception e) {
                ServersLink.LOGGER.error("Unexpected error in SubServer loop", e);
                try {
                    Thread.sleep(60000);
                } catch (InterruptedException ex) {
                    break;
                }
            }
        }
    }

    @Override
    public void interrupt() {
        super.interrupt();
        executor.shutdown();
    }
}
