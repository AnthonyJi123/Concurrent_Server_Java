package cpen221.mp3.handler;

import cpen221.mp3.server.Server;

import java.io.*;
import java.net.Socket;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.LinkedBlockingQueue;

class MessageHandlerThread_notForGrade implements Runnable {
    //Abstraction Function:
    //The MessageHandlerThread represents a thread that handles the messages from clients or entities.
    //It gets the incoming messgae from a socket, process them, and adds them to the LinkedBlockingQueue<String> data
    //for other processing in the server.

    //Representation Invariant:
    //The 'incomingSocket' has to be a open and valid socket for communication.
    //The 'data' queue has to be initialized
    //The 'clients' and 'entities' lists show the connection between clients and entities, and are
    //upadated based on the new message.
    //The 'servers' map refer to the Server objects and it's indexed by clientID. it should sync with
    //the clients.
    //The 'TimeStamp, ClientId, EntityId,  String EntityType, doubleValue, booleanValue' help
    //the information store and should be updated based on the message.

    private Socket incomingSocket;

    private ArrayList<Socket> clients;
    private ArrayList<Socket> entities;
    private Map<Integer, Server> servers;  //Integer = ClientId

    Server server;
    private double TimeStamp;
    private int ClientId;
    private int EntityId;
    private String EntityType;
    private double doubleValue;
    private boolean booleanValue;


    private LinkedBlockingQueue<String> data;
    private PrintWriter output;
    private HashMap<Integer, Socket> clientSocketMap;

    public MessageHandlerThread_notForGrade(Socket incomingSocket) {
        this.incomingSocket = incomingSocket;
    }

    public MessageHandlerThread_notForGrade(Socket incomingSocket, LinkedBlockingQueue<String> data, HashMap<Integer, Socket> clientSocketMap) {
        this.incomingSocket = incomingSocket;
        this.data = data;
        this.clientSocketMap = clientSocketMap;
    }

    @Override
    public void run() {
        // handle the client request or entity event here
        // and deal with exceptions if needed
        try {
            BufferedReader input = new BufferedReader(new InputStreamReader(incomingSocket.getInputStream()));
            String message = input.readLine();
            data.add(message);
            input.close();
            incomingSocket.close();
        } catch (IOException e) {
            System.err.println("Error in handling client connection: " + e.getMessage());
            e.printStackTrace();
        }
    }
}