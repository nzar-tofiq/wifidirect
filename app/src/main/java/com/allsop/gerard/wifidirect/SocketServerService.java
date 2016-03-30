package com.allsop.gerard.wifidirect;

import android.app.IntentService;
import android.content.Context;
import android.content.Intent;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.UnknownHostException;

/**
 * Created by gja10 on 17/03/2016.
 */
public class SocketServerService extends IntentService {
    private static String TAG = "SocketServerService";
    public static final String ACTION_SEND_MESSAGE = "SEND_MESSAGE";
    public static final String EXTRAS_MESSAGE = "MESSAGE";
    public static final String ACTION_START_SERVER = "ACTION_START_SERVER";

    int port = 10051;
    static Boolean myTurn=false;
    Boolean listen=true;
    static String message;

    public SocketServerService(String name) {
        super(name);
    }
    public SocketServerService() {super("SocketServerService");}


    public static void nextTurn(){
        myTurn = !myTurn;
        Log.d(TAG,"myTurn value: "+myTurn);
    }

    public static void setMessage(String msg){
        message = msg;
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Context context = getApplicationContext();
        if (intent.getAction().equals(ACTION_START_SERVER)) {
            new Thread(new Runnable() {
                public void run() {
                    while(true){
                        try(  ServerSocket dataSocket = new ServerSocket(port);
                              Socket clientSocket = dataSocket.accept();
                              PrintWriter outputStreamWriter =
                                      new PrintWriter(clientSocket.getOutputStream(), true);
                              InputStreamReader inputStreamReader =
                                      new InputStreamReader(clientSocket.getInputStream());
                              BufferedReader bufferedReader =
                                      new BufferedReader(inputStreamReader);
                        ){
                            if(listen) {
                                message = bufferedReader.readLine();
                                if (message != null) {
                                    listen=false;
                                    log(message);
                                }
                            }
                            if(!listen) {
                                while(!myTurn){
                                    ;
                                }
                                log("Server sending message");
                                outputStreamWriter.print(message+'\n');
                                outputStreamWriter.flush();
                                listen=false;
                                myTurn=false;
                                }
                        }catch (IOException e){log(e.getMessage());}
                    }
                }
            }).start();
        }
    }
    private void log(String msg) {
        Log.d(TAG, msg);
    }
}
