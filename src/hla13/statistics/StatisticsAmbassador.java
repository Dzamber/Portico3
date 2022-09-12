package hla13.statistics;

import hla.rti.*;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.NullFederateAmbassador;
import hla13.clinic.ConstClass;
import hla13.clinic.ExternalEvent;
import org.portico.impl.hla13.types.DoubleTime;

import java.util.ArrayList;

public class StatisticsAmbassador extends NullFederateAmbassador {

	protected boolean running = true;

    protected double federateTime        = 0.0;
    protected double federateLookahead   = 1.0;
    protected boolean isRegulating       = false;
    protected boolean isConstrained      = false;
    protected boolean isAdvancing        = false;

    protected boolean isAnnounced        = false;
    protected boolean isReadyToRun       = false;
    protected int addPatientToQueHandle = 0;
    protected int addDoctorHandle = 2;
    protected int doctorTreatPatientHandle = 3;
    protected int addPatientToReceptionHandle = 4;
    protected ArrayList<ExternalEvent> externalEvents = new ArrayList<>();


    public void timeRegulationEnabled( LogicalTime theFederateTime )
    {
        this.federateTime = convertTime( theFederateTime );
        this.isRegulating = true;
    }

    public void timeConstrainedEnabled( LogicalTime theFederateTime )
    {
        this.federateTime = convertTime( theFederateTime );
        this.isConstrained = true;
    }


    public void synchronizationPointRegistrationFailed( String label )
    {
        log( "Failed to register sync point: " + label );
    }

    public void synchronizationPointRegistrationSucceeded( String label )
    {
        log( "Successfully registered sync point: " + label );
    }

    public void announceSynchronizationPoint( String label, byte[] tag )
    {
        log( "Synchronization point announced: " + label );
        if( label.equals(ConstClass.READY_TO_RUN) )
            this.isAnnounced = true;
    }

    public void federationSynchronized( String label )
    {
        log( "Federation Synchronized: " + label );
        if( label.equals(ConstClass.READY_TO_RUN) )
            this.isReadyToRun = true;
    }


	//public void receiveInteraction( int interactionClass,
	//		                        ReceivedInteraction theInteraction,
    //                               byte[] tag) {
    //
	//	receiveInteraction(interactionClass, theInteraction, tag, null, null);
	//}

    public void receiveInteraction( int interactionClass,
                                    ReceivedInteraction theInteraction,
                                    byte[] tag,
                                    LogicalTime theTime,
                                    EventRetractionHandle eventRetractionHandle )
    {
		StringBuilder builder = new StringBuilder("Interaction Received: ");
		//log("interactionClass: " + interactionClass);
        if(interactionClass == addPatientToReceptionHandle) {
            try {
                int patientNumber = EncodingHelpers.decodeInt(theInteraction.getValue(0));
                double time =  convertTime(theTime);
                externalEvents.add(new ExternalEvent(patientNumber, ExternalEvent.EventType.ADDpatientToReception, time));
                builder.append("ADDpatientToReception , time=" + time + ", patientNumber=" + patientNumber );
                builder.append( "\n" );
            } catch (ArrayIndexOutOfBounds ignored) {

            }
        }
        else if(interactionClass == addDoctorHandle) {
            try {
                int personNumber = EncodingHelpers.decodeInt(theInteraction.getValue(0));
                double time =  convertTime(theTime);
                externalEvents.add(new ExternalEvent(personNumber, ExternalEvent.EventType.ADDdoctor , time));
                builder.append( "ADDdoctor , time=" + time );
                builder.append( "\n" );
            } catch (ArrayIndexOutOfBounds ignored) {

            }
        }
        else if(interactionClass == doctorTreatPatientHandle) {
            try {
                int patientNumber = EncodingHelpers.decodeInt(theInteraction.getValue(0));
                int doctorNumber = EncodingHelpers.decodeInt(theInteraction.getValue(1));
                double time =  convertTime(theTime);
                externalEvents.add(new ExternalEvent(patientNumber, doctorNumber, ExternalEvent.EventType.GETdoctor, time));
                builder.append("GETdoctor , time=" + time + ", patientNumber=" + patientNumber + ", doctorNumber="+doctorNumber);
                builder.append( "\n" );
            } catch (ArrayIndexOutOfBounds ignored) {

            }
        }
        else if(interactionClass == addPatientToReceptionHandle) {
            try {
                int patientNumber = EncodingHelpers.decodeInt(theInteraction.getValue(0));
                double time =  convertTime(theTime);
                externalEvents.add(new ExternalEvent(patientNumber, ExternalEvent.EventType.ADDpatientToReception, time));
                builder.append("ADDpatientToReception , time=" + time + ", patientNumber=" + patientNumber );
                builder.append( "\n" );
            } catch (ArrayIndexOutOfBounds ignored) {

            }
        }
		log(builder.toString());
	}

    public void timeAdvanceGrant( LogicalTime theTime )
    {
        this.federateTime = convertTime( theTime );
        this.isAdvancing = false;
    }

    private double convertTime( LogicalTime logicalTime )
    {
        // PORTICO SPECIFIC!!
        return ((DoubleTime)logicalTime).getTime();
    }

	private void log(String message) {
		System.out.println("StatisticsAmbassador: " + message);
	}

}
