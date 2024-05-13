package cpen221.mp3.server;

import cpen221.mp3.entity.Sensor;
import cpen221.mp3.event.ActuatorEvent;
import cpen221.mp3.event.Event;
import cpen221.mp3.event.SensorEvent;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

enum DoubleOperator {
    EQUALS,
    GREATER_THAN,
    LESS_THAN,
    GREATER_THAN_OR_EQUALS,
    LESS_THAN_OR_EQUALS
}

enum BooleanOperator {
    EQUALS,
    NOT_EQUALS
}

public class Filter {
    //Abstraction Function:
    //The Filter class represents a filter that evaluates whether the events stifies certain
    //criteria or not. There are double, boolean, and composite filters. The booleanOperator
    //represents the comparison operator. BooleanValue is the boolean value compared. Field
    //determines the what the comparison is based on value or timestamp. doubleOperator is
    //comparison operator for double filter, and doubleValue is the double value compared.
    //For composite filter, filters is the filter list that the composite filter should
    //satisfy.

    //Representation Invariant:
    //boolean filter:
    //booleanOperator is not null
    //field is null
    //doubleOperator null
    //filters is not null
    //double filter:
    //doubleOperator is not null
    //field is value or timestamp
    //booleanOperator is null
    //filters is null
    //composite filter:
    //filters is not null
    //booleanOperator is null
    //doubleOperator is null
    //field is null

    // you can add private fields and methods to this class
    private BooleanOperator booleanOperator;
    private boolean booleanValue;
    private String field;
    private DoubleOperator doubleOperator;
    private double doubleValue;
    private List<Filter> filters;

    /**
     * Constructs a filter that compares the boolean (actuator) event value
     * to the given boolean value using the given BooleanOperator.
     * (X (BooleanOperator) value), where X is the event's value passed by satisfies or sift methods.
     * A BooleanOperator can be one of the following:
     * 
     * BooleanOperator.EQUALS
     * BooleanOperator.NOT_EQUALS
     *
     * @param operator the BooleanOperator to use to compare the event value with the given value, not null
     * @param value the boolean value to match, true or false
     */
    public Filter(BooleanOperator operator, boolean value) {
        // TODO: implement this method
        this.booleanOperator = operator;
        this.booleanValue = value;
    }

    /**
     * Constructs a filter that compares a double field in events
     * with the given double value using the given DoubleOperator.
     * (X (DoubleOperator) value), where X is the event's value passed by satisfies or sift methods.
     * A DoubleOperator can be one of the following:
     * 
     * DoubleOperator.EQUALS
     * DoubleOperator.GREATER_THAN
     * DoubleOperator.LESS_THAN
     * DoubleOperator.GREATER_THAN_OR_EQUALS
     * DoubleOperator.LESS_THAN_OR_EQUALS
     * 
     * For non-double (boolean) value events, the satisfies method should return false.
     *
     * @param field the field to match (event "value" or event "timestamp"), a valid string
     * @param operator the DoubleOperator to use to compare the event value with the given value, not null
     * @param value the double value to match, valid double
     *
     * @throws IllegalArgumentException if the given field is not "value" or "timestamp"
     */
    public Filter(String field, DoubleOperator operator, double value) {
        // TODO: implement this method
        this.field = field;
        doubleOperator = operator;
        doubleValue = value;
    }
    
    /**
     * A filter can be composed of other filters.
     * in this case, the filter should satisfy all the filters in the list.
     * Constructs a complex filter composed of other filters.
     *
     * @param filters the list of filters to use in the composition, not null list of filters
     */
    public Filter(List<Filter> filters) {
        // TODO: implement this method
        this.filters = filters;
    }

    /**
     * Returns true if the given event satisfies the filter criteria.
     *
     * @param event the event to check, valid event
     * @return true if the event satisfies the filter criteria, false otherwise
     */
    public boolean satisfies(Event event) {
        // TODO: implement this method
        if (booleanOperator != null) {
            //if event is not ActuatorEvent, it's not satisfied
            if (event instanceof SensorEvent) return false;
            switch (booleanOperator) {
                case EQUALS:
                    return event.getValueBoolean() == booleanValue;
                case NOT_EQUALS:
                    return event.getValueBoolean() != booleanValue;
            }
        }

        if (doubleOperator != null) {
            //if event is not SensorEvent, it's not satisfied
            if (event instanceof ActuatorEvent && "value".equals(field)) return false;
            double eventValue = "value".equals(field) ? event.getValueDouble() : event.getTimeStamp();
            switch (doubleOperator) {
                case EQUALS:
                    return eventValue == doubleValue;
                case GREATER_THAN:
                    return eventValue > doubleValue;
                case LESS_THAN:
                    return eventValue < doubleValue;
                case GREATER_THAN_OR_EQUALS:
                    return eventValue >= doubleValue;
                case LESS_THAN_OR_EQUALS:
                    return eventValue <= doubleValue;
            }
        }

        if (filters != null) {
            for (Filter filter : filters) {
                if (!filter.satisfies(event)) {
                    return false;
                }
            }
            return true;
        }

        throw new IllegalStateException("Filter not properly initialized");
    }

    /**
     * Returns true if the given list of events satisfies the filter criteria.
     *
     * @param events the list of events to check, valid events
     * @return true if every event in the list satisfies the filter criteria, false otherwise
     */
    public boolean satisfies(List<Event> events) {
        // TODO: implement this method
        for(Event e : events){
            if (!satisfies(e)) return false;
        }
        return true;
    }

    /**
     * Returns a new event if it satisfies the filter criteria.
     * If the given event does not satisfy the filter criteria, then this method should return null.
     *
     * @param event the event to sift, valid event
     * @return a new event if it satisfies the filter criteria, null otherwise
     */
    public Event sift(Event event) {
        // TODO: implement this method
//        if (satisfies(event)) {
//            if (booleanOperator != null) {
//                return new ActuatorEvent(event.getTimeStamp(), event.getClientId(), event.getEntityId(), event.getEntityType(), event.getValueBoolean());
//            }
//            if (doubleOperator != null) {
//                return new SensorEvent(event.getTimeStamp(), event.getClientId(), event.getEntityId(), event.getEntityType(), event.getValueDouble());
//            }
//            if (filters != null) {
//                return new SensorEvent(event.getTimeStamp(), event.getClientId(), event.getEntityId(), event.getEntityType(), event.getValueDouble());
//            }
//        }
//        return null;
        return satisfies(event) ? event : null;
    }

    /**
     * Returns a list of events that contains only the events in the given list that satisfy the filter criteria.
     * If no events in the given list satisfy the filter criteria, then this method should return an empty list.
     *
     * @param events the list of events to sift, valid event
     * @return a list of events that contains only the events in the given list that satisfy the filter criteria
     *        or an empty list if no events in the given list satisfy the filter criteria
     */
    public List<Event> sift(List<Event> events) {
        // TODO: implement this method
        List<Event> filteredEvents = new ArrayList<>();
        for (Event event : events) {
            Event filteredEvent = sift(event);
            if (filteredEvent != null) {
                filteredEvents.add(filteredEvent);
            }
        }
        return filteredEvents;
    }

    @Override
    public String toString() {
        // TODO: implement this method
        StringBuilder sb = new StringBuilder("");

        if (booleanOperator != null) {
            sb.append("booleanOperator|").append(booleanOperator)
                    .append("|").append(booleanValue);
        }

        if (doubleOperator != null) {
            sb.append("doubleOperator|").append(field)
                    .append("|").append(doubleOperator)
                    .append("|").append(doubleValue);
        }

        if (filters != null && !filters.isEmpty()) {
            sb.append("compositeFilter[");
            for (Filter filter : filters) {
                sb.append(filter.toString()).append("/");
            }
            sb.delete(sb.length() - 2, sb.length()); // Remove the last comma and space
            sb.append(']');
        }
        return sb.toString();
    }
}
