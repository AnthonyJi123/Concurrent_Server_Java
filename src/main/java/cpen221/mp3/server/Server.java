package cpen221.mp3.server;

import cpen221.mp3.client.RequestCommand;
import cpen221.mp3.client.RequestType;
import cpen221.mp3.entity.Actuator;
import cpen221.mp3.entity.Entity;
import cpen221.mp3.client.Client;
import cpen221.mp3.event.ActuatorEvent;
import cpen221.mp3.event.Event;
import cpen221.mp3.client.Request;
import cpen221.mp3.event.SensorEvent;
//import cpen221.mp3.handler.MessageHandlerThread;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.Socket;
import java.util.*;
import java.util.Map.Entry;
import java.util.concurrent.*;

public class Server {
    //Abstraction Function:
    //The Server Class is a server that manages events, requests and other actuator/sensor
    //event processing and control. These aspects that Server manages depend on the Client
    //associated with the Server. The server operates maxwaittime update, event processes,
    //actuator controls, filter usage, event logging, and analysis...

    //Representation Invariant:
    //The 'events' list should contain 'Event' objects
    //The 'entityIDs' set should contain valid entity IDs of the events received
    //The 'requests' list should contain 'Request' objects
    //The 'maxWaitTime' should not be negative
    //The 'scheduler' is used for scheduling tasks and needs initialization
    //The 'controlStates' enum should be a valid state('NONE', 'TOGGLE', 'SET')

    private Client client;
    private double maxWaitTime = 2; // in seconds
    private List<Event> events = Collections.synchronizedList(new ArrayList<>());
    private HashSet<Integer> entityIDs = new HashSet<>();
    private ArrayList<Request> requests = new ArrayList<>();
    private ArrayList<Integer> LogList = new ArrayList<>();
    private Actuator actuatorToControl;
    private Filter filterToControl;
    private LinkedBlockingQueue<String> blockingQueue;
    HashMap<Integer, Socket> clientSocketMap;

    private ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());

    private enum ControlStates {
        NONE,
        TOGGLE,
        SET
    }

    private ControlStates controlState = ControlStates.NONE;
    private serverThread serverThread;

    // you may need to add additional private fields


    public Server(Client client) {
        int port = client.getServerPort();
        this.client = client;
    }

    public Server(Client client, LinkedBlockingQueue<String> blockingQueue, HashMap<Integer, Socket> clientSocketMap) {
        int port = client.getServerPort();
        this.client = client;
        this.blockingQueue = blockingQueue;
        this.clientSocketMap = clientSocketMap;
        System.out.println("server built");
        serverThread serverThread1 = new serverThread(blockingQueue, clientSocketMap);
        serverThread1.start();
    }

    private class serverThread extends Thread {
        private LinkedBlockingQueue<String> blockingQueue;
        HashMap<Integer, Socket> clientSocketMap;
        public serverThread(LinkedBlockingQueue<String> blockingQueue, HashMap<Integer, Socket> clientSocketMap) {
            this.blockingQueue = blockingQueue;
            this.clientSocketMap = clientSocketMap;
        }

        @Override
        public void run() {
            try {
                while (true) {
                    String message = blockingQueue.take();
                    new Thread(() -> {
                        System.out.println("Server received: " + message);
                        if (message.charAt(0) == 'R') {
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

                            System.out.println("Request delay stated: " + ((long) maxWaitTime * 1000 - (System.currentTimeMillis() - (long) timeStamp)));
                            Runnable task = () -> processIncomingRequest(request);
                            scheduler.schedule(task, (long) maxWaitTime * 1000 - (System.currentTimeMillis() - (long) timeStamp), TimeUnit.MILLISECONDS);
                        } else {
                            List<Integer> eqSign = new ArrayList<>();
                            List<Integer> commaSign = new ArrayList<>();

                            //process event string
                            for (int i = 0; i < message.length(); i++) {
                                if (message.charAt(i) == '=') {
                                    eqSign.add(i);
                                } else if (message.charAt(i) == ',' || message.charAt(i) == '}') {
                                    commaSign.add(i);
                                }
                            }

                            double timeStamp = Double.valueOf(message.substring(eqSign.get(0) + 1, commaSign.get(0)));
                            int clientId = Integer.valueOf(message.substring(eqSign.get(1) + 1, commaSign.get(1)));
                            int entityId = Integer.valueOf(message.substring(eqSign.get(2) + 1, commaSign.get(2)));
                            String EntityType = message.substring(eqSign.get(3) + 1, commaSign.get(3));
                            if (message.charAt(0) == 'A') {
                                boolean booleanValue = Boolean.valueOf(message.substring(eqSign.get(4) + 1, commaSign.get(4)));
                                ActuatorEvent actuatorEvent = new ActuatorEvent(timeStamp, clientId, entityId, EntityType, booleanValue);
                                processIncomingEvent(actuatorEvent);
//                                System.out.println("Event delay stated: " + ((long) maxWaitTime * 1000 - (System.currentTimeMillis() - (long) timeStamp)));
//                                Runnable task = () -> processIncomingEvent(actuatorEvent);
//                                scheduler.schedule(task, (long) maxWaitTime * 1000 - (System.currentTimeMillis() - (long) timeStamp), TimeUnit.MILLISECONDS);
                            } else {
                                double doubleValue = Double.valueOf(message.substring(eqSign.get(4) + 1, commaSign.get(4)));
                                SensorEvent sensorEvent = new SensorEvent(timeStamp, clientId, entityId, EntityType, doubleValue);
                                processIncomingEvent(sensorEvent);
//                                System.out.println("Event delay stated: " + ((long) maxWaitTime * 1000 - (System.currentTimeMillis() - (long) timeStamp)));
//                                Runnable task = () -> processIncomingEvent(sensorEvent);
//                                scheduler.schedule(task, (long) maxWaitTime * 1000 - (System.currentTimeMillis() - (long) timeStamp), TimeUnit.MILLISECONDS);
                            }
                        }
                    }).start();
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
    }


    /**
     * Update the max wait time for the client.
     * The max wait time is the maximum amount of time
     * that the server can wait for before starting to process each event of the client:
     * It is the difference between the time the message was received on the server
     * (not the event timeStamp from above) and the time it started to be processed.
     *
     * @param maxWaitTime the new max wait time, cannot be negative
     */
    public void updateMaxWaitTime(double maxWaitTime) {
        // implement this method
        if (this.maxWaitTime > maxWaitTime) {
            try {
                serverThread.sleep((long) ((this.maxWaitTime - maxWaitTime) * 1000));
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        this.maxWaitTime = maxWaitTime;
        // Important note: updating maxWaitTime may not be as simple as
        // just updating the field. You may need to do some additional
        // work to ensure that events currently being processed are not
        // dropped or ignored by the change in maxWaitTime.


    }

    /**
     * Set the actuator state if the given filter is satisfied by the latest event.
     * Here the latest event is the event with the latest timestamp not the event
     * that was received by the server the latest.
     * <p>
     * If the actuator is not registered for the client, then this method should do nothing.
     *
     * @param filter   the filter to check, must be non-null and valid
     * @param actuator the actuator to set the state of as true, must be non-null and valid
     */
    public void setActuatorStateIf(Filter filter, Actuator actuator) {
        // implement this method and send the appropriate SeverCommandToActuator as a Request to the actuator
        if (!actuator.registerForClient(client.getClientId())) {
            return;
        }
        if (filter.satisfies(lastNEvents(1).get(0))) {
            setActuator(actuator, RequestCommand.SET_STATE);
        }
    }

    /**
     * Toggle the actuator state if the given filter is satisfied by the latest event.
     * Here the latest event is the event with the latest timestamp not the event
     * that was received by the server the latest.
     * <p>
     * If the actuator has never sent an event to the server, then this method should do nothing.
     * If the actuator is not registered for the client, then this method should do nothing.
     *
     * @param filter   the filter to check, must be non-null and valid
     * @param actuator the actuator to toggle the state of (true -> false, false -> true), must be non-null and valid
     */
    public synchronized void toggleActuatorStateIf(Filter filter, Actuator actuator) {
        // implement this method and send the appropriate SeverCommandToActuator as a Request to the actuator
        if (!actuator.registerForClient(client.getClientId())) {
            return;
        }
        if (filter.satisfies(lastNEvents(1).get(0))) {
            setActuator(actuator, RequestCommand.TOGGLE_STATE);
        }
    }

    public void setActuator(Actuator actuator, RequestCommand requestCommand) {
        try {
            Socket socket = new Socket(actuator.getIP(), actuator.getPort());
            PrintWriter outputStream = new PrintWriter(socket.getOutputStream(), true);
            Request request = new Request(RequestType.SeverCommandToActuator, requestCommand, "none");
            outputStream.println(request.toString());
            System.out.println("Request sent: " + request.toString());
            outputStream.flush();
            outputStream.close();
            socket.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    /**
     * Log the event ID for which a given filter was satisfied.
     * This method is checked for every event received by the server.
     *
     * @param filter the filter to check, must be non-null and valid
     */
    public void logIf(Filter filter) {
        // implement this method
        LogList.clear();
        Collections.sort(events, (e1, e2) -> Double.compare(e1.getTimeStamp(), e2.getTimeStamp()));
        for (Event event : events) {
            if (filter.satisfies(event)) {
                LogList.add(event.getEntityId());
            }
        }
    }

    /**
     * Return all the logs made by the "logIf" method so far.
     * If no logs have been made, then this method should return an empty list.
     * The list should be sorted in the order of event timestamps.
     * After the logs are read, they should be cleared from the server.
     *
     * @return list of event IDs
     */
    public List<Integer> readLogs() {
        // implement this method
        return LogList;
    }

    /**
     * List all the events of the client that occurred in the given time window.
     * Here the timestamp of an event is the time at which the event occurred, not
     * the time at which the event was received by the server.
     * If no events occurred in the given time window, then this method should return an empty list.
     *
     * @param timeWindow the time window of events, inclusive of the start and end times, must be non-negative
     * @return list of the events for the client in the given time window
     */
    public List<Event> eventsInTimeWindow(TimeWindow timeWindow) {
        // implement this method
        List<Event> result = new ArrayList<>();
        for (Event e : events) {
            if (e.getTimeStamp() >= timeWindow.startTime && e.getTimeStamp() <= timeWindow.endTime) {
                result.add(e);
            }
        }
        return result;
    }

    /**
     * Returns a set of IDs for all the entities of the client for which
     * we have received events so far.
     * Returns an empty list if no events have been received for the client.
     *
     * @return list of all the entities of the client for which we have received events so far
     */
    public List<Integer> getAllEntities() {
        // implement this method
        return new ArrayList<>(entityIDs);
    }

    /**
     * List the latest n events of the client.
     * Here the order is based on the original timestamp of the events, not the time at which the events were received by the server.
     * If the client has fewer than n events, then this method should return all the events of the client.
     * If no events exist for the client, then this method should return an empty list.
     * If there are multiple events with the same timestamp in the boundary,
     * the ones with largest EntityId should be included in the list.
     *
     * @param n the max number of events to list, must be non-negative
     * @return list of the latest n events of the client
     */
    public List<Event> lastNEvents(int n) {
        // implement this method
        ArrayList<Event> events = new ArrayList<>(this.events);
        Collections.sort(events, (e1, e2) -> Double.compare(e1.getTimeStamp(), e2.getTimeStamp()));
        List<Event> LastEventList = new ArrayList<>();
        for (int i = events.size() - n; i < events.size(); i++) {
            LastEventList.add(events.get(i));
        }
        return LastEventList;
    }

    /**
     * returns the ID corresponding to the most active entity of the client
     * in terms of the number of events it has generated.
     * <p>
     * If there was a tie, then this method should return the largest ID.
     *
     * @return the most active entity ID of the client
     */
    public int mostActiveEntity() {
        // implement this method
        List<Integer> ActiveList = new ArrayList<>();
        for (Event event : events) {
            ActiveList.add(event.getEntityId());
        }
        Map<Integer, Integer> frequencyMap = new HashMap<>();

        for (int num : ActiveList) {
            frequencyMap.put(num, frequencyMap.getOrDefault(num, 0) + 1);
        }

        int mostFrequentNumber = ActiveList.get(0);
        int maxFrequency = 1;

        for (Entry<Integer, Integer> entry : frequencyMap.entrySet()) {
            if (entry.getValue() > maxFrequency) {
                mostFrequentNumber = entry.getKey();
                maxFrequency = entry.getValue();
            }
        }

        return mostFrequentNumber;
    }

    /**
     * the client can ask the server to predict what will be
     * the next n timestamps for the next n events
     * of the given entity of the client (the entity is identified by its ID).
     * <p>
     * If the server has not received any events for an entity with that ID,
     * or if that Entity is not registered for the client, then this method should return an empty list.
     *
     * @param entityId the ID of the entity, must be non-null and valid
     * @param n        the number of timestamps to predict, must be non-negative
     * @return list of the predicted timestamps
     */
    public List<Double> predictNextNTimeStamps(int entityId, int n) {
        // implement this method
        return null;
    }

    /**
     * the client can ask the server to predict what will be
     * the next n values of the timestamps for the next n events
     * of the given entity of the client (the entity is identified by its ID).
     * The values correspond to Event.getValueDouble() or Event.getValueBoolean()
     * based on the type of the entity. That is why the return type is List<Object>.
     * <p>
     * If the server has not received any events for an entity with that ID,
     * or if that Entity is not registered for the client, then this method should return an empty list.
     *
     * @param entityId the ID of the entity, must be non-null and valid
     * @param n        the number of double value to predict, must be non-negative
     * @return list of the predicted timestamps
     */
    public List<Object> predictNextNValues(int entityId, int n) {
        // implement this method
        return null;
    }

    void processIncomingEvent(Event event) {
        // implement this method
        events.add(event);
        entityIDs.add(event.getEntityId());
        switch (controlState) {
            case SET -> setActuatorStateIf(filterToControl, actuatorToControl);
            case TOGGLE -> toggleActuatorStateIf(filterToControl, actuatorToControl);
        }
    }

    synchronized void processIncomingRequest(Request request) {
        // implement this method
        requests.add(request);
        System.out.println("processing request: " + request.toString());
        if (request.getRequestType() != RequestType.SeverCommandToActuator) {
            switch (request.getRequestCommand()) {
                case CONFIG_UPDATE_MAX_WAIT_TIME:
                    updateMaxWaitTime(Double.parseDouble(request.getRequestData()));
                    break;
                case CONTROL_SET_ACTUATOR_STATE:
                    //requestData format: filter.toString() + "@" + Actuator.toString
                    controlState = ControlStates.SET;
                    System.out.println(controlState);
                    String dataCSAS = request.getRequestData().substring(request.getRequestData().indexOf('=') + 1);
                    filterToControl = filterBuilder(dataCSAS.substring(0, dataCSAS.indexOf('@')));
                    System.out.println("Filter built for set: " + filterToControl.toString());
                    actuatorToControl = actuatorBuilder(dataCSAS.substring(dataCSAS.indexOf('@') + 1));
                    bufferSolution(request.getTimeStamp(), actuatorToControl, RequestCommand.SET_STATE);
                    break;
                case CONTROL_TOGGLE_ACTUATOR_STATE:
                    //requestData format: filter.toString() + "@" + entityId
                    controlState = ControlStates.TOGGLE;
                    System.out.println(controlState);
                    String dataCTAS = request.getRequestData();
                    filterToControl = filterBuilder(dataCTAS.substring(0, dataCTAS.indexOf('@')));
                    System.out.println("filter built: " + filterToControl.toString());
                    actuatorToControl = actuatorBuilder(dataCTAS.substring(dataCTAS.indexOf('@') + 1));
                    System.out.println(actuatorToControl.toString());
//                    if (! (client.entityMap.get(entityIdCTAS) instanceof Actuator)) {
//                        System.out.println("The entity is not an actuator!");
//                        throw new IllegalArgumentException("The entity is not an actuator!");
//                    }
                    bufferSolution(request.getTimeStamp(), actuatorToControl, RequestCommand.TOGGLE_STATE);
                    break;
                case CONTROL_NOTIFY_IF:
                    //requestData format: field + "|" + DoubleOperator + "|" + value + "|" + entityId
                    break;
                case ANALYSIS_GET_EVENTS_IN_WINDOW:
                    //requestData format: startTime + "|" + endTime

                    List<Integer> RequestDataSplitTW = new ArrayList<>(); //TW=timewindow
                    String dataTW = request.getRequestData();

                    for (int i = 0; i < dataTW.length(); i++) {
                        if (dataTW.charAt(i) == '|') {
                            RequestDataSplitTW.add(i);
                        }
                    }

                    Double startTime = Double.valueOf(dataTW.substring(0, RequestDataSplitTW.get(0)));
                    Double endTime = Double.valueOf(dataTW.substring(RequestDataSplitTW.get(0) + 1));

                    TimeWindow timeWindow = new TimeWindow(startTime, endTime);
                    List<Event> TimeWindowList = new ArrayList<>();
                    TimeWindowList = eventsInTimeWindow(timeWindow);
                    String TimeWindowListStr = "eventsInTimeWindow=";
                    for (Event event : TimeWindowList) {
                        TimeWindowListStr = TimeWindowListStr + event.toString() + "#";
                    }
                    sendResponse(TimeWindowListStr);
                    break;
                case ANALYSIS_GET_ALL_ENTITIES:
                    List<Integer> AllEntityList = new ArrayList<>();
                    AllEntityList = getAllEntities();
                    String AllEntityStr = "getAllEntities=";
                    for (Integer entityId : AllEntityList) {
                        AllEntityStr = AllEntityStr + entityId + "#";
                    }
                    sendResponse(AllEntityStr);
                    break;
                case ANALYSIS_GET_LATEST_EVENTS:
                    //requestData format: n
                    int LastN = Integer.valueOf(request.getRequestData());

                    List<Event> LatestEventList = new ArrayList<>();

                    LatestEventList = lastNEvents(LastN);
                    String LatestEventStr = "lastNEvents=";
                    for (Event latestEvent : LatestEventList) {
                        LatestEventStr = LatestEventStr + latestEvent.toString() + "#";
                    }
                    sendResponse(LatestEventStr);
                    break;
                case ANALYSIS_GET_MOST_ACTIVE_ENTITY:
                    String ActiveEntity = "mostActiveEntity=" + mostActiveEntity();
                    sendResponse(ActiveEntity);
                    break;
                case PREDICT_NEXT_N_TIMESTAMPS:

                    break;
                case PREDICT_NEXT_N_VALUES:

                    break;
            }
        }
    }

    private Filter filterBuilder(String data) {
        Filter filter;
        String[] dataSplited = data.split("\\|");
        if (dataSplited[0].equals("booleanOperator")) {
            BooleanOperator operatorCTAS = BooleanOperator.valueOf(dataSplited[1]);
            boolean valueCTAS = Boolean.valueOf(dataSplited[2]);

            filter = new Filter(operatorCTAS, valueCTAS);
        } else if (dataSplited[0].equals("doubleOperator")) {
            String fieldCTAS = dataSplited[1];
            DoubleOperator operatorCTAS = DoubleOperator.valueOf(dataSplited[2]);
            double valueCTAS = Double.valueOf(dataSplited[3]);

            filter = new Filter(fieldCTAS, operatorCTAS, valueCTAS);
        } else {
            String filterData = data.substring(data.indexOf('[') + 1, data.indexOf(']'));
            String[] dataSplitedAgain = filterData.split("/");
            List<Filter> filters = new ArrayList<>();
            for (int i = 0; i < dataSplitedAgain.length; i++) {
                filters.add(filterBuilder(dataSplitedAgain[i]));
            }
            filter = new Filter(filters);
        }
        return filter;
    }

    public static Actuator actuatorBuilder(String data) {
        int id = -1;
        int clientId = -1;
        String entityType = "";
        String ip = "";
        int port = -1;

        String actData = data.substring(data.indexOf("[") + 1, data.lastIndexOf("]"));
        String[] elements = actData.split("\\|");

        for (String element : elements) {
            String[] key = element.split(":");

            switch (key[0]) {
                case "getId":
                    id = Integer.parseInt(key[1]);
                    break;
                case "ClientId":
                    clientId = Integer.parseInt(key[1]);
                    break;
                case "EntityType":
                    entityType = key[1];
                    break;
                case "IP":
                    ip = key[1];
                    break;
                case "Port":
                    port = Integer.parseInt(key[1]);
                    break;
            }
        }
        Actuator actuator = new Actuator(id, clientId, entityType, false, ip, port);
        System.out.println(actuator.toString());
        return actuator;
    }

    private void sendResponse(String response) {
        Socket socket = clientSocketMap.get(client.getClientId());
        try {
            PrintWriter outputStream = new PrintWriter(socket.getOutputStream(), true);
            outputStream.println(response);
            outputStream.flush();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

    synchronized private void bufferSolution(double timeStamp, Actuator actuator, RequestCommand requestCommand) {
        List<Event> events = new ArrayList<>(this.events);
        Collections.sort(events, (e1, e2) -> Double.compare(e1.getTimeStamp(), e2.getTimeStamp()));
        List<Event> LastEventList = new ArrayList<>();
        for (int size = events.size()-1; size >= 0; size--) {
            if (events.get(size).getTimeStamp() >= timeStamp) LastEventList.add(events.get(size));
        }
        System.out.println(events);
        for (Event event : LastEventList) {
            if (filterToControl.satisfies(event)) {
                System.out.println("yes");
                setActuator(actuator, requestCommand);
            }
        }
    }
}

