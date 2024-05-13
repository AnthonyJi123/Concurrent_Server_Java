package cpen221.mp3.server;

import cpen221.mp3.CSVEventReader;
import cpen221.mp3.client.Client;
import cpen221.mp3.client.Request;
import cpen221.mp3.client.RequestCommand;
import cpen221.mp3.client.RequestType;
import cpen221.mp3.entity.Actuator;
import cpen221.mp3.entity.Sensor;
import cpen221.mp3.event.ActuatorEvent;
import cpen221.mp3.event.Event;
import cpen221.mp3.event.SensorEvent;
import cpen221.mp3.handler.MessageHandler;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class HanlderTests {
    double maxWaitTime = 2;

    @Test
    public void testRequestSetActuatorStateIf() {
        Client client1 = new Client(2, "test1@test.com", "127.0.0.1", 4579);
        MessageHandler mh = new MessageHandler(client1.getServerPort());
        Actuator actuator1 = new Actuator(93, client1.getClientId(), "Switch", true);
        Sensor thermostat = new Sensor(1, client1.getClientId(), "TempSensor", "127.0.0.1", 4579);
        client1.addEntity(thermostat);
        client1.addEntity(actuator1);
        actuator1.updateState(false);
        Filter sensorValueFilter = new Filter("value", DoubleOperator.GREATER_THAN_OR_EQUALS, 1);
        client1.sendRequest(new Request(RequestType.CONTROL, RequestCommand.CONTROL_SET_ACTUATOR_STATE, new StringBuilder().append(sensorValueFilter.toString()).append("@").append(actuator1.toString()).toString()));
        try{
            Thread.sleep(300);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        thermostat.setEventGenerationFrequency(1);
        thermostat.startEventGeneration(3);
        try{
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        thermostat.stopEventGeneration();
        try{
            Thread.sleep((long) (maxWaitTime+1)*1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        assertEquals(true, actuator1.getState());
    }
    @Test
    public void testRequestTOGGLEActuatorStateIf() {
        MessageHandler mh = new MessageHandler(4578);
        Client client2 = new Client(3, "test2@test.com", "127.0.0.1", 4578);
        Actuator actuator2 = new Actuator(98, client2.getClientId(), "Switch", true);
        Sensor thermostat = new Sensor(0, client2.getClientId(), "TempSensor");
        thermostat.setEndpoint("127.0.0.1", 4578);
        client2.addEntity(thermostat);
        client2.addEntity(actuator2);
        actuator2.updateState(false);

        Filter sensorValueFilter = new Filter("value", DoubleOperator.GREATER_THAN_OR_EQUALS, 1);
        client2.sendRequest(new Request(RequestType.CONTROL, RequestCommand.CONTROL_TOGGLE_ACTUATOR_STATE, new StringBuilder().append(sensorValueFilter.toString()).append("@").append(actuator2.toString()).toString()));
        try{
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        thermostat.setEventGenerationFrequency(1);
        thermostat.startEventGeneration(3);
        try{
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        thermostat.stopEventGeneration();
        try{
            Thread.sleep((long) (maxWaitTime+1)*1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println("test TOGGLE done");
        assertEquals(true, actuator2.getState());
    }

    @Test
    public void testRequestEventsInTimeWindow() {
        Client client3 = new Client(4, "test2@test.com", "127.0.0.1", 4575);
        String csvFilePath = "data/tests/single_client_1000_events_in-order.csv";
        CSVEventReader eventReader = new CSVEventReader(csvFilePath);
        List<Event> eventList = eventReader.readEvents();
        Sensor sensor = new Sensor(2, 4, "x", "127.0.0.1",4575);
        MessageHandler server = new MessageHandler(4575);
        TimeWindow tw = new TimeWindow(0.2, 0.4);
        try{
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        for (int i = 0; i < 10; i++) {
            sensor.sendEvent(eventList.get(i));
        }
        client3.sendRequest(new Request(RequestType.ANALYSIS, RequestCommand.ANALYSIS_GET_EVENTS_IN_WINDOW, tw.toString()));

        try{
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        System.out.println(client3.getResponsseList().get(0));
        assertTrue(client3.getResponsseList().get(0).contains(
                "SensorEvent{TimeStamp=0.22787690162658691,ClientId=0,EntityId=151,EntityType=CO2Sensor,Value=448.9545292667084}")&&client3.getResponsseList().get(0).contains("ActuatorEvent{TimeStamp=0.33080601692199707,ClientId=0,EntityId=97,EntityType=Switch,Value=false}"));
    }

    @Test
    public void testRequestLastNEvents() {
        Client client4 = new Client(5, "test2@test.com", "127.0.0.1", 4573);
        String csvFilePath = "data/tests/single_client_1000_events_in-order.csv";
        CSVEventReader eventReader = new CSVEventReader(csvFilePath);
        List<Event> eventList = eventReader.readEvents();
        Sensor sensor = new Sensor(2, 5, "x", "127.0.0.1",4573);
        MessageHandler server = new MessageHandler(4573);

        try{
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        for (int i = 0; i < 10; i++) {
            sensor.sendEvent(eventList.get(i));
        }

        client4.sendRequest(new Request(RequestType.ANALYSIS, RequestCommand.ANALYSIS_GET_LATEST_EVENTS, "2"));

        try{
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        assertTrue(client4.getResponsseList().get(0).contains(
                "ActuatorEvent{TimeStamp=0.8107128143310547,ClientId=0,EntityId=64,EntityType=Switch,Value=true}")&&client4.getResponsseList().get(0).contains(
                "SensorEvent{TimeStamp=0.8124408721923828,ClientId=0,EntityId=144,EntityType=PressureSensor,Value=1024.642156565536}"));
    }

    @Test
    public void testRequestGetAllEntities() {
        Client client5 = new Client(6, "test2@test.com", "127.0.0.1", 4571);
        String csvFilePath = "data/tests/single_client_1000_events_in-order.csv";
        CSVEventReader eventReader = new CSVEventReader(csvFilePath);
        List<Event> eventList = eventReader.readEvents();
        Sensor sensor = new Sensor(2, 6, "x", "127.0.0.1",4571);

        MessageHandler server = new MessageHandler(4571);
        try{
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        for (int i = 0; i < 5; i++) {
            sensor.sendEvent(eventList.get(i));
        }

        client5.sendRequest(new Request(RequestType.ANALYSIS, RequestCommand.ANALYSIS_GET_ALL_ENTITIES, ""));

        try{
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        assertTrue(client5.getResponsseList().get(0).equals("getAllEntities=0#97#164#151#30#"));
    }


    @Test
    public void testRequestMostActiveEntity() {
        Client client6 = new Client(7, "test2@test.com", "127.0.0.1", 4569);
        MessageHandler server = new MessageHandler(client6.getServerPort());
        Sensor sensor = new Sensor(2, 4, "x", "127.0.0.1",4569);
        Event event1 = new ActuatorEvent(0.00010015, 0, 11,"Switch", true);
        Event event2 = new SensorEvent(0.000111818, 0, 1,"TempSensor", 1.0);
        Event event3 = new ActuatorEvent(0.00015, 0, 5,"Switch", false);
        Event event4 = new SensorEvent(0.00022, 0, 1,"TempSensor", 11.0);
        Event event5 = new ActuatorEvent(0.00027, 0, 11,"Switch", true);
        Event event6 = new ActuatorEvent(0.00047, 0, 11,"Switch", true);
        Event event7 = new SensorEvent(0.00025, 0, 2,"PressureSensor", 8.0);
        Event event8 = new SensorEvent(0.00030, 0, 2,"PressureSensor", 12.0);
        Event event9 = new SensorEvent(0.00035, 0, 2,"PressureSensor", 5.0);
        Event event10 = new SensorEvent(0.00040, 0, 2,"PressureSensor", 2.0);
        List<Event> simulatedEvents = new ArrayList<>();
        simulatedEvents.add(event1);
        simulatedEvents.add(event2);
        simulatedEvents.add(event3);
        simulatedEvents.add(event4);
        simulatedEvents.add(event5);
        simulatedEvents.add(event6);
        simulatedEvents.add(event7);
        simulatedEvents.add(event8);
        simulatedEvents.add(event9);
        simulatedEvents.add(event10);
        try{
            Thread.sleep(100);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        for (int i = 0; i < simulatedEvents.size(); i++) {
            sensor.sendEvent(simulatedEvents.get(i));
        }
        try{
            Thread.sleep(300);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        client6.sendRequest(new Request(RequestType.ANALYSIS, RequestCommand.ANALYSIS_GET_MOST_ACTIVE_ENTITY, ""));

        try{
            Thread.sleep(4000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }

        assertTrue(client6.getResponsseList().get(0).equals("mostActiveEntity=2"));
    }
}