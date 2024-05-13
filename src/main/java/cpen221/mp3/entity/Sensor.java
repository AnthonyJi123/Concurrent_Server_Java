package cpen221.mp3.entity;
import java.io.*;
import java.net.Socket;
import java.util.*;

import cpen221.mp3.event.Event;
import cpen221.mp3.event.SensorEvent;

public class Sensor implements Entity {
    //Abstraction Function:
    //The Sensor class represents a sensor entity that generate and send events to a server.
    //It implements Entity Interface and provides methods for the sensor. The class can generate
    //sensor events, send to server, or stop generation.

    //Representation Invariant:
    //'id' should be non-negative integer
    //'clientId' should be a non-negative, valid integer
    //'type' should be non null string
    //'serverIP' should be valid and only null if it's not set
    //'serverPort' should be valid port number, or 0 if not appplicable
    //'eventGenerationFrequency' should be a positive double showing its frequecny in generation
    //'consecutiveFailures' should be non-negative
    //'random' should be a generated random value

    private final int id;
    private int clientId;
    private final String type;
    private String serverIP = null;
    private int serverPort = 0;
    private double eventGenerationFrequency = 0.2; // default value in Hz (1/s)
    private Timer eventTimer;
    private int consecutiveFailures = 0;
    private static final int MAX_FAILURES = 5;
    private static final long FAILURE_WAIT_TIME = 10000;
    private Random random = new Random();

    private PrintWriter outputStream;

    public Sensor(int id, String type) {
        this.id = id;
        this.clientId = -1;         // remains unregistered
        this.type = type;
    }

    public Sensor(int id, int clientId, String type) {
        this.id = id;
        this.clientId = clientId;   // registered for the client
        this.type = type;
    }

    public Sensor(int id, String type, String serverIP, int serverPort) {
        this.id = id;
        this.clientId = -1;   // remains unregistered
        this.type = type;
        this.serverIP = serverIP;
        this.serverPort = serverPort;
    }

    public Sensor(int id, int clientId, String type, String serverIP, int serverPort) {
        this.id = id;
        this.clientId = clientId;   // registered for the client
        this.type = type;
        this.serverIP = serverIP;
        this.serverPort = serverPort;
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
        return false;
    }

    /**
     * Registers the sensor for the given client
     *
     * @return true if the sensor is new (clientID is -1 already) and gets successfully registered or if it is already registered for clientId, else false
     */
    public boolean registerForClient(int clientId) {
        if (this.clientId == -1 || this.clientId == clientId){
            this.clientId = clientId;
            return true;
        }
        else {
            return false;
        }
        //return this.clientId == -1 || this.clientId == clientId;
    }

    /**
     * Sets or updates the http endpoint that 
     * the sensor should send events to
     *
     * @param serverIP the IP address of the endpoint, must be valid if it exists, null if not
     * @param serverPort the port number of the endpoint, must be valid, null if not
     */
    public void setEndpoint(String serverIP, int serverPort){
        this.serverIP = serverIP;
        this.serverPort = serverPort;
    }

    /**
     * Sets the frequency of event generation
     *
     * @param frequency the frequency of event generation in Hz (1/s), must be non-negative double
     */
    public void setEventGenerationFrequency(double frequency){
        this.eventGenerationFrequency = frequency;
    }
    int i;
    public void startEventGeneration(int maxTime) {
        i = 0;
        eventTimer = new Timer();
        long period = (long) (1000 / eventGenerationFrequency);
        eventTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                if (consecutiveFailures >= MAX_FAILURES) {
                    try {
                        Thread.sleep(FAILURE_WAIT_TIME);
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                    consecutiveFailures = 0;
                }

                if (serverIP != null && serverPort != 0 && i<maxTime) {
                    Event event = generateEvent();
                    sendEvent(event);
                }
            }
        }, 0, period);
    }

    public void stopEventGeneration() {
        eventTimer.cancel();
    }

    private Event generateEvent() {
        double value;
        switch (this.type) {
            case "TempSensor":
                value = 20 + (24 - 20) * random.nextDouble();
                break;
            case "PressureSensor":
                value = 1020 + (1024 - 1020) * random.nextDouble();
                break;
            case "CO2Sensor":
                value = 400 + (450 - 400) * random.nextDouble();
                break;
//            case "Switch":
//                value = random.nextBoolean() ? 1 : 0;
//                break;
            default:
                throw new IllegalArgumentException("Unknown sensor type: " + this.type);
        }

        double timestamp = System.currentTimeMillis();
        return new SensorEvent(timestamp, clientId, id, type, value);
    }


    public void sendEvent(Event event) {
        i++;
        try {
            Socket socket = new Socket(serverIP, serverPort);
            outputStream = new PrintWriter(socket.getOutputStream(), true);
            outputStream.println(event.toString());
            outputStream.flush();
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
//        // note that Event is a complex object that you need to serialize before sending
    }
}