package cpen221.mp3.handler;

import java.net.ServerSocket;
import java.net.Socket;
import java.util.*;
import java.util.concurrent.LinkedBlockingQueue;

public class MessageHandler_notForGrade {
    private ServerSocket serverSocket;
    private int port;
    public LinkedBlockingQueue<String> data = new LinkedBlockingQueue<>();

    private ArrayList<Socket> clients;
    private ArrayList<Socket> entities;
    public HashMap<Integer, Socket> clientSocketMap = new HashMap<>();
    public int lastId;
    // you may need to add additional private fields and methods to this class

    public MessageHandler_notForGrade(int port) {
        this.port = port;
        start();
    }

//    public MessageHandler(int port, BlockingDeque<String> in) {
//        this.port = port;
//    }

    public void start() {
        // the following is just to get you started
        // you may need to change it to fit your implementation
        new Thread(() -> {
            try {
                serverSocket = new ServerSocket(port);
                //System.out.println("Server started on port " + port);

                while (true) {
                    Socket incomingSocket = serverSocket.accept();
                    //System.out.println("Client/Entity connected: " + incomingSocket.getInetAddress().getHostAddress());

                    // create a new thread to handle the client request or entity event
                    Thread handlerThread = new Thread(new MessageHandlerThread_notForGrade(incomingSocket, data,  clientSocketMap));
                    handlerThread.start();
                }
            } catch (Exception e) {
                System.err.println("Error: " + e.getMessage() + port);
            }
        }).start();
    }



    public static void main(String[] args) {
        // you would need to initialize the RequestHandler with the port number
        // and then start it here
    }
}