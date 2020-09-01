package com.richuncle.proyectoubicacion;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketException;
import java.net.UnknownHostException;

public class UDPSender implements Runnable {

    private final String Message;
    private final int serverPort;

    public UDPSender(String message, int serverPort) {
        Message = message;
        this.serverPort = serverPort;
    }


    @Override
    public void run() {
        try(DatagramSocket clientSocket = new DatagramSocket(10841)){
            DatagramPacket datagramPacket = new DatagramPacket(
                    Message.getBytes(),
                    Message.length(),
                    InetAddress.getByName("186.144.170.107"),
                    serverPort
            );
            clientSocket.send(datagramPacket);
        } catch (SocketException | UnknownHostException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }
}