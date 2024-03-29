package hla13.clinic;


import java.util.Comparator;

public class ExternalEvent {

    public enum EventType {ADDpatientToQue, GET, ADDdoctor, GETdoctor, ADDpatientToReception}

    private int personNumber;
    private int doctorNumber;
    private EventType eventType;
    private Double time;

    public ExternalEvent(int personNumber, EventType eventType, Double time) {
        this.personNumber = personNumber;
        this.eventType = eventType;
        this.time = time;
    }

    public ExternalEvent(int personNumber, int doctorNumber, EventType eventType, Double time) {
        this.personNumber = personNumber;
        this.doctorNumber = doctorNumber;
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
    public int getDoctorNumber() {return doctorNumber;}

    public double getTime() {
        return time;
    }

    public static class ExternalEventComparator implements Comparator<ExternalEvent> { //This was protected. Maybe there should be single external event for every federate? But it is easier to have one in that case

        @Override
        public int compare(ExternalEvent o1, ExternalEvent o2) {
            return o1.time.compareTo(o2.time);
        }
    }

}
