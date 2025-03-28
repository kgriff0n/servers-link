package io.github.kgriff0n.socket;

import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.packet.Packet;
import io.github.kgriff0n.packet.info.NewServerPacket;
import io.github.kgriff0n.packet.info.ServerStatusPacket;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.UUID;

import static io.github.kgriff0n.ServersLink.IS_RUNNING;
import static io.github.kgriff0n.ServersLink.SERVER;

public class SubServer extends Thread {

    private static SubServer connection;

    /** List of player UUIDs that can connect */
    private ArrayList<UUID> waitingPlayers;

    public static SubServer getInstance() {
        return connection;
    }

    private Socket clientSocket;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    public SubServer(String ip, int port) {
        if (connection == null) {
            waitingPlayers = new ArrayList<>();

            try {
                clientSocket = new Socket(ip, port);

                out = new ObjectOutputStream(clientSocket.getOutputStream());
                out.flush();

                in = new ObjectInputStream(clientSocket.getInputStream());
            } catch (IOException e) {
                ServersLink.LOGGER.error("Unable to establish connection");
            }
            connection = this;
        } else {
            ServersLink.LOGGER.error("Connection already established");
        }
    }

    public synchronized void send(Packet packet) {
        try {
            out.writeObject(packet);
            out.flush();
            out.reset();
        } catch (IOException e) {
            ServersLink.LOGGER.error("Unable to send packet");
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
        try {
            send(new NewServerPacket(ServersLink.getServerInfo()));
            send(new ServerStatusPacket(ServersLink.getServerInfo().getName(), 20.0f, false));
            while (IS_RUNNING) {
                try {
                    ((Packet)in.readObject()).onReceive();
                } catch (ClassNotFoundException e) {
                    ServersLink.LOGGER.error("Receive invalid data");
                }
            }
        } catch (IOException e) {
            ServersLink.LOGGER.error("Hub disconnected");
            SERVER.stop(true);
        }
    }

}
