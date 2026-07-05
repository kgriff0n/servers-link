package io.github.kgriff0n.socket;

import io.github.kgriff0n.ServersLink;
import io.github.kgriff0n.packet.Packet;
import io.github.kgriff0n.packet.info.NewServerPacket;
import io.github.kgriff0n.packet.info.ServerStatusPacket;

import java.io.*;
import java.net.Socket;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static io.github.kgriff0n.ServersLink.IS_RUNNING;
import static io.github.kgriff0n.ServersLink.SERVER;

public class SubServer extends Thread {

    private static SubServer connection;

    private ExecutorService executor;

    public static SubServer getInstance() {
        return connection;
    }

    @SuppressWarnings("FieldCanBeLocal")
    private Socket clientSocket;
    private ObjectInputStream in;
    private ObjectOutputStream out;

    public SubServer(String ip, int port) {
        if (connection == null) {

            try {
                clientSocket = new Socket(ip, port);

                out = new ObjectOutputStream(clientSocket.getOutputStream());
                out.flush();

                in = new ObjectInputStream(clientSocket.getInputStream());
            } catch (IOException e) {
                ServersLink.LOGGER.error("Unable to establish connection");
            }
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
                    out.writeObject(packet);
                    out.flush();
                    out.reset();
                } catch (IOException e) {
                    ServersLink.LOGGER.error("Unable to send {}", packet.getClass().getName());
                }
            });
        }
    }

    @Override
    public void run() {
        try {
            send(new NewServerPacket(ServersLink.getServerInfo()));
            send(new ServerStatusPacket(ServersLink.getServerInfo().getName(), 20.0f, false));
            while (IS_RUNNING) {
                try {
                    Packet pkt = ((Packet)in.readObject());
                    SERVER.execute(pkt::onReceive);
                } catch (ClassNotFoundException e) {
                    ServersLink.LOGGER.error("Receive invalid data");
                }
            }
        } catch (Exception e) {
            ServersLink.LOGGER.error("Gateway disconnected ", e);
            SERVER.halt(true);
        }
    }

    @Override
    public void interrupt() {
        super.interrupt();
        executor.shutdown();
    }
}
