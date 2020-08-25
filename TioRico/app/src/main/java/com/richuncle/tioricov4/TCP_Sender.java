package com.richuncle.tioricov4;

import android.os.AsyncTask;

import java.io.DataOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.InetSocketAddress;
import java.net.Socket;

public class TCP_Sender extends AsyncTask<String, Void, Void> {
    Socket server;
    DataOutputStream dos;
    PrintWriter pw;
    @Override
    protected Void doInBackground(String... voids) {
        String message = voids[0];
        try{
            server = new Socket();
            server.connect(new InetSocketAddress("192.168.0.15", 8080), 2000);
            server.setSoTimeout(2000);
            pw = new PrintWriter(server.getOutputStream());
            pw.write(message);
            pw.flush();
            //server.close();
        }catch (IOException e){
            e.printStackTrace();
        }
        return null;
    }
}