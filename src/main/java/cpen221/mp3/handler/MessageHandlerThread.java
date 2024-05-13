package cpen221.mp3.handler;

import cpen221.mp3.client.Client;
import cpen221.mp3.server.Server;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

class MessageHandlerThread implements Runnable {
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

    public MessageHandlerThread(Socket incomingSocket) {
        this.incomingSocket = incomingSocket;
    }

    public MessageHandlerThread(Socket incomingSocket, LinkedBlockingQueue<String> data, HashMap<Integer, Socket> clientSocketMap) {
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
            //output = new PrintWriter(incomingSocket.getOutputStream(), true);
            String message = input.readLine();
            if(message.charAt(0) == 'c'){
                String clinetInfor = message.substring(message.indexOf(":")+1);
                System.out.println("client: "+clinetInfor);
                String[] parts = clinetInfor.split(",");
                int clientId = Integer.valueOf(parts[0].trim());
                String email = parts[1].trim();
                String serverIP = parts[2].trim();
                int serverPort = Integer.parseInt(parts[3].trim());
                Client client = new Client(clientId, email, serverIP, serverPort);
                System.out.println("Client"+ clientId + " register");
                clientSocketMap.put(clientId, incomingSocket);
                Server server = new Server(client, data, clientSocketMap);
                while ((message = input.readLine()) != null) {
                    data.add(message);
                }
            }
            else {
                data.add(message);
                input.close();
                incomingSocket.close();
            }
        } catch (IOException e) {
            System.err.println("Error in handling client connection: " + e.getMessage());
            e.printStackTrace();
        }
    }
}