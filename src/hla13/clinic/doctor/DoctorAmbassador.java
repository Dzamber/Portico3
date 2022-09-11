package hla13.clinic.doctor;

import hla.rti.ArrayIndexOutOfBounds;
import hla.rti.EventRetractionHandle;
import hla.rti.LogicalTime;
import hla.rti.ReceivedInteraction;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.NullFederateAmbassador;
import hla13.clinic.ConstClass;
import hla13.clinic.ExternalEvent;
import org.portico.impl.hla13.types.DoubleTime;

import java.util.ArrayList;

/**
 * Created by Michal on 2016-04-27.
 */
public class DoctorAmbassador extends NullFederateAmbassador {

    protected double federateTime        = 0.0;
    protected double federateLookahead   = 1.0;

    protected boolean isRegulating       = false;
    protected boolean isConstrained      = false;
    protected boolean isAdvancing        = false;

    protected boolean isAnnounced        = false;
    protected boolean isReadyToRun       = false;

    protected boolean running 			 = true;

    protected int getDoctorHandle = 3;
    protected ArrayList<ExternalEvent> externalEvents = new ArrayList<>();

    private double convertTime( LogicalTime logicalTime )
    {
        // PORTICO SPECIFIC!!
        return ((DoubleTime)logicalTime).getTime();
    }

    private void log( String message )
    {
        System.out.println( "FederateAmbassador: " + message );
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

    /**
     * The RTI has informed us that time regulation is now enabled.
     */
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

    public void timeAdvanceGrant( LogicalTime theTime )
    {
        this.federateTime = convertTime( theTime );
        this.isAdvancing = false;
    }



    public void receiveInteraction( int interactionClass,
                                    ReceivedInteraction theInteraction,
                                    byte[] tag,
                                    LogicalTime theTime,
                                    EventRetractionHandle eventRetractionHandle )
    {
        StringBuilder builder = new StringBuilder( "Interaction Received:" );
        if(interactionClass == getDoctorHandle) {
            try {
                int qty = EncodingHelpers.decodeInt(theInteraction.getValue(0));
                double time =  convertTime(theTime);
                externalEvents.add(new ExternalEvent(qty, ExternalEvent.EventType.GETdoctor, time));
                builder.append("GETdoctor , time=" + time);
                builder.append(" id=").append(qty);
                builder.append( "\n" );

            } catch (ArrayIndexOutOfBounds ignored) {

            }

        }
        log( builder.toString() );
    }

}
