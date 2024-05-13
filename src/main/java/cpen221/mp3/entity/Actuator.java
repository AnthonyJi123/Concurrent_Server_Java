package cpen221.mp3.entity;

import cpen221.mp3.client.Request;
import cpen221.mp3.client.RequestCommand;
import cpen221.mp3.client.RequestType;
import cpen221.mp3.event.ActuatorEvent;
import cpen221.mp3.event.Event;
import cpen221.mp3.handler.MessageHandler_notForGrade;

import java.net.Socket;
import java.io.*;
import java.util.*;

public class Actuator implements Entity {
    //The Actuator class represents an actuator entity that is used to generate events,
    //receive commands, and interact with a server. It implements the Entity Interface
    //for the methods. The actuator has its ID, clientID, type, event generation frequency
    //serverIP, server port...
    //id, identifies the actuator
    //clientId, the clienId thats with the actuator. -1 if not registered
    //type, type of actuator
    //state, state of actuator
    //eventGenerationFrequency, event generation frequency in Hz
    //serverIP, address of where the events should be sent to
    //host, IP adress of the actuator receiving commands
    //port, port number of the actuator getting command
    //consecutiveFailures, consecutive failure number when sending event
    //outputStream, output stream to send events to server
    //mh, the MessageHandler,
    //MAX_FAILURES, max allowed consectutive fails
    //FAILURE_WAIT_TIME, wait time after reaching MAX_FAILURES

    //Representation Invariant:
    //id, non negative int
    //clientId, -1 or non negative int
    //type, not null
    //state, boolean true or false
    //eventGenerationFrequency, non negative double
    //serverIP, not null
    //host, not null
    //port, positive int
    //consecutiveFailures, non negative int
    //outputStream, initialized when used
    //mh initialized when used
    //MAX_FAILURES, positive int
    //FAILURE_WAIT_TIME, positive long

    private final int id;
    private int clientId = -1;
    private final String type;
    private boolean state;
    private double eventGenerationFrequency = 0.2; // default value in Hz (1/s)
    private Timer eventTimer;
    private int consecutiveFailures = 0;
    // the following specifies the http endpoint that the actuator should send events to
    private String serverIP = null;
    private int serverPort = 0;
    // the following specifies the http endpoint that the actuator should be able to receive commands on from server
    private String host = "127.0.0.1";
    private int port;
    private static int portHistory = 9000;

    private MessageHandler_notForGrade mh;

    private static final int MAX_FAILURES = 5;
    private static final long FAILURE_WAIT_TIME = 10000;


    private PrintWriter outputStream;

    public Actuator(int id, String type, boolean init_state) {
        this.id = id;
        this.clientId = -1;         // remains unregistered
        this.type = type;
        this.state = init_state;
        startReceiveCommand();
        // TODO: need to establish a server socket to listen for commands from server
    }

    public Actuator(int id, int clientId, String type, boolean init_state) {
        this.id = id;
        this.clientId = clientId;   // registered for the client
        this.type = type;
        this.state = init_state;
        startReceiveCommand();
        // TODO: need to establish a server socket to listen for commands from server
    }

    public Actuator(int id, String type, boolean init_state, String serverIP, int serverPort) {
        this.id = id;
        this.clientId = -1;         // remains unregistered
        this.type = type;
        this.state = init_state;
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        startReceiveCommand();
        // TODO: need to establish a server socket to listen for commands from server
    }


    public Actuator(int id, int clientId, String type, boolean init_state, String serverIP, int serverPort) {
        this.id = id;
        this.clientId = clientId;   // registered for the client
        this.type = type;
        this.state = init_state;
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        startReceiveCommand();
        // TODO: need to establish a server socket to listen for commands from server
    }

    public Actuator(int id, int clientId, String type, boolean init_state, String serverIP, int serverPort, int x) {
        this.id = id;
        this.clientId = clientId;   // registered for the client
        this.type = type;
        this.state = init_state;
        this.serverIP = serverIP;
        this.serverPort = serverPort;
        // TODO: need to establish a server socket to listen for commands from server
        startReceiveCommand();
    }

    private void startReceiveCommand(){
        port = portHistory + id;
        mh = new MessageHandler_notForGrade(port);
//        try {
//            ServerSocket serverSocket = new ServerSocket(serverPort);
//            new Thread(()->{
//                while (true){
//                    try {
//                        Socket incomingSocket = serverSocket.accept();
//                        BufferedReader input = new BufferedReader(new InputStreamReader(incomingSocket.getInputStream()));
//                        String message;
//                        while ((message = input.readLine()) != null) {
//                            processServerMessage(decodeRequest(message));
//                            System.out.println(System.identityHashCode(this));
//                        }
//                    } catch (IOException e) {
//                        throw new RuntimeException(e);
//                    }
//                }
//            });
//
//
//        } catch (IOException e) {
//            throw new RuntimeException(e);
//        }

        new Thread(() -> {
            try {
                while (true) {
                    String message = mh.data.take();
                    System.out.println(type + id + "Received: " + message);
                    Request request = decodeRequest(message);
                    processServerMessage(request);
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }).start();
    }

    private Request decodeRequest(String message){
        List<Integer> RequestEqSign = new ArrayList<>();
        List<Integer> RequestCommaSign = new ArrayList<>();

        for (int i = 0; i < message.length(); i++) {
            if (message.charAt(i) == '=') {
                RequestEqSign.add(i);
            } else if (message.charAt(i) == ',' || message.charAt(i) == '}') {
                RequestCommaSign.add(i);
            }
        }

        double timeStamp = Double.valueOf(message.substring(RequestEqSign.get(0) + 1, RequestCommaSign.get(0)));
        RequestType requestType = RequestType.valueOf(message.substring(RequestEqSign.get(1) + 1, RequestCommaSign.get(1)));
        RequestCommand requestCommand = RequestCommand.valueOf(message.substring(RequestEqSign.get(2) + 1, RequestCommaSign.get(2)));
        String requestData = message.substring(RequestEqSign.get(3) + 1, RequestCommaSign.get(3));

        Request request = new Request(requestType, requestCommand, requestData);
        return request;
    }


    public int getId() {
        return id;
    }

    public int getClientId() {
        return clientId;
    }

    public String getType() {
        return type;
    }

    public boolean isActuator() {
        return true;
    }

    public synchronized boolean getState() {
        return state;
    }

    public String getIP() {
        return host;
    }

    public int getPort() {
        return port;
    }

    public synchronized void updateState(boolean new_state) {
        state = new_state;
        System.out.println(type + id + " changed state to " + state);
        System.out.println(System.identityHashCode(this));
    }

    /**
     * Registers the actuator for the given client
     *
     * @return true if the actuator is new (clientID is -1 already) and gets successfully registered or if it is already registered for clientId, else false
     */
    public boolean registerForClient(int clientId) {
        if (this.clientId == -1 || clientId == this.clientId) {
            this.clientId = clientId;
            return true;
        } else {
            return false;
        }
    }

    /**
     * Sets or updates the http endpoint that
     * the actuator should send events to
     *
     * @param serverIP   the IP address of the endpoint
     * @param serverPort the port number of the endpoint
     */
    public void setEndpoint(String serverIP, int serverPort) {
        this.serverIP = serverIP;
        this.serverPort = serverPort;
    }

    /**
     * Sets the frequency of event generation
     *
     * @param frequency the frequency of event generation in Hz (1/s)
     */
    public void setEventGenerationFrequency(double frequency) {
        // implement this method
        this.eventGenerationFrequency = frequency;
    }

    public void startEventGeneration() {
        eventTimer = new Timer();
        long period = (long) (1000 / eventGenerationFrequency);
        eventTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (consecutiveFailures >= MAX_FAILURES) {
                    try {
                        System.out.println(type + "sleeping");
                        Thread.sleep(FAILURE_WAIT_TIME);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    consecutiveFailures = 0;
                }

                if (serverIP != null && serverPort != 0) {
                    Event event = generateEvent();
                    //System.out.println(type + " started sending");
                    sendEvent(event);
                }
            }
        }, 0, period);
    }

    private Event generateEvent() {
        double timestamp = System.currentTimeMillis();
        return new ActuatorEvent(timestamp, clientId, id, type, state);
    }

    public void sendEvent(Event event) {
        // implement this method
        try {
            Socket socket = new Socket(serverIP, serverPort);
            outputStream = new PrintWriter(socket.getOutputStream(), true);
            outputStream.println(event.toString());
            outputStream.flush();
            outputStream.close();
            socket.close();
            consecutiveFailures = 0;
        } catch (Exception e) {
            e.printStackTrace();
            consecutiveFailures++;
            if (consecutiveFailures >= MAX_FAILURES) {
                try {
                    Thread.sleep(FAILURE_WAIT_TIME);
                } catch (InterruptedException ie) {
                    ie.printStackTrace();
                }
                consecutiveFailures = 0;
            }
        }
        // note that Event is a complex object that you need to serialize before sending
    }

    public void processServerMessage(Request command) {
        // implement this method
        if (command.getRequestType() != RequestType.SeverCommandToActuator){
            throw new IllegalArgumentException("The command is not SeverCommandToActuator!");
        }
        else {
            switch (command.getRequestCommand()) {
                case SET_STATE:
                    updateState(true);
                    break;
                case TOGGLE_STATE:
                    boolean stateNow = state;
                    updateState(!stateNow);
                    break;
            }
        }
    }

    @Override
    public String toString() {
        return "Actuator[" +
                "getId:" + getId() +
                "|ClientId:" + getClientId() +
                "|EntityType:" + getType() +
                "|IP:" + getIP() +
                "|Port:" + getPort() +
                ']';
    }




    // you will most likely need additional helper methods for this class
}