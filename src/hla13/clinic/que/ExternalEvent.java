package hla13.clinic.que;


import java.util.Comparator;

public class ExternalEvent {

    public enum EventType {ADDpatient, GET, ADDdoctor}

    private int personNumber;
    private EventType eventType;
    private Double time;

    public ExternalEvent(int personNumber, EventType eventType, Double time) {
        this.personNumber = personNumber;
        this.eventType = eventType;
        this.time = time;
    }

    public ExternalEvent(EventType eventType, Double time) {
        this.eventType = eventType;
        this.time = time;
    }

    public EventType getEventType() {
        return eventType;
    }

    public int getPersonNumber() {
        return personNumber;
    }

    public double getTime() {
        return time;
    }

    static class ExternalEventComparator implements Comparator<ExternalEvent> {

        @Override
        public int compare(ExternalEvent o1, ExternalEvent o2) {
            return o1.time.compareTo(o2.time);
        }
    }

}
