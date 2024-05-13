package cpen221.mp3.client;

import cpen221.mp3.entity.Entity;

import java.awt.desktop.ScreenSleepEvent;
import java.io.*;
import java.net.Socket;
import java.util.*;

public class Client {

    private final int clientId;
    private String email;

    private String serverIP;
    private int serverPort;
    private Set<Integer> entityIDList = new HashSet<>();
    private PrintWriter outputStream;
    private BufferedReader inputStream;
    private ArrayList<String> responseList = new ArrayList<>();
    private Socket socket;

    // you would need additional fields to enable functionalities required for this class

    public Client(int clientId, String email, String serverIP, int serverPort) {
        this.clientId = clientId;
        this.email = email;
        this.serverIP = serverIP;
        this.serverPort = serverPort;
    }

    public void startConnection(){
        try {
            Thread.sleep(10); //ensure the serverSocket is built
            socket = new Socket(serverIP, serverPort);
            outputStream = new PrintWriter(socket.getOutputStream());
            inputStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
            new Thread(()->{
                try {
                    String message;
                    while ((message = inputStream.readLine()) != null) {
                        processIncomingResponds(message);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }).start();
            outputStream.println("clientRegistration:" + clientId + "," + email + "," + serverIP + "," + serverPort);
            outputStream.flush();
        }catch (IOException e){
            e.printStackTrace();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    public int getClientId() {
        return clientId;
    }

    public int getServerPort(){
        return serverPort;
    }

    /**
     * Registers an entity for the client
     *
     * @return true if the entity is new and gets successfully registered, false if the Entity is already registered
     */
    public boolean addEntity(Entity entity) {
        if(entity.registerForClient(clientId)){
            return entityIDList.add(entity.getId());
        }
        return false;
    }


    public Set<Integer> getEntityIdSet(){
        return entityIDList;
    }

    // sends a request to the server
    public void sendRequest(Request request) {
        // implement this method
        if (socket == null) startConnection();
        outputStream.println(request.toString());
        outputStream.flush();
    }

    public void processIncomingResponds(String response){
        System.out.println("Client" + clientId + " received: " + response);
        responseList.add(response);
    }

    public List<String> getResponsseList(){return responseList;}
}