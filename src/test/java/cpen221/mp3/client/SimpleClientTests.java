package cpen221.mp3.client;

import cpen221.mp3.entity.Actuator;
import cpen221.mp3.entity.Entity;
import cpen221.mp3.entity.Sensor;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

public class SimpleClientTests{

    @Test
    public void testRegisterEntities() {
        Client client = new Client(0, "test@test.com", "127.0.0.1", 4578);

        Entity thermostat = new Sensor(0, client.getClientId(), "TempSensor");
        Entity valve = new Actuator(0, -1, "Switch", false);

        assertFalse(thermostat.registerForClient(1));   // thermostat is already registered to client 0
        assertTrue(thermostat.registerForClient(0));    // registering thermostat for existing client (client 0) is fine and should return true
        assertTrue(valve.registerForClient(1));         // valve was unregistered, and can be registered to client 1, even if it does not exist
    }

    @Test
    public void testAddEntities() {
        Client client1 = new Client(0, "test1@test.com", "127.0.0.1", 4578);
        Client client2 = new Client(1, "test2@test.com", "127.0.0.1", 4578);

        Entity valve = new Actuator(0, -1, "Switch", false);

        assertTrue(client1.addEntity(valve));
        assertFalse(client2.addEntity(valve));
    }

    //write by us
    @Test
    public void testGetPort() {
        Client client = new Client(0, "test@test.com", "127.0.0.1", 4578);

        assertEquals(4578, client.getServerPort());
    }

    @Test
    public void testGetEntityId() {
        Client clientGEid = new Client(0, "test1@test.com", "127.0.0.1", 4578);

        Entity entity1 = new Actuator(3, -1, "Switch", false);
        Entity entity2 = new Actuator(4,  "Switch", true);
        Entity entity3 = new Actuator(5, "Switch", false,"127.0.0.1", 4578);
        Entity entity4 = new Actuator(6, -1, "Switch", false,"127.0.0.1", 4578);
        Entity entity5 = new Sensor(7, "TempSensor");
        Entity entity6 = new Sensor(8, -1, "PressureSensor");
        Entity entity7 = new Sensor(9,  "PressureSensor", "127.0.0.1", 4578);
        Entity entity8 = new Sensor(10,  -1, "CO2Sensor", "127.0.0.1", 4578);

        clientGEid.addEntity(entity1);
        clientGEid.addEntity(entity2);
        clientGEid.addEntity(entity3);
        clientGEid.addEntity(entity4);
        clientGEid.addEntity(entity5);
        clientGEid.addEntity(entity5);
        clientGEid.addEntity(entity5);

        clientGEid.addEntity(entity7);
        clientGEid.addEntity(entity8);

        Set<Integer> list = clientGEid.getEntityIdSet();
        Set<Integer> expectedElements = new HashSet<>(Arrays.asList(3, 4, 5, 6, 7, 9, 10));

        assertTrue(list.containsAll(expectedElements));
    }
}