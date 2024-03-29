package hla13.clinic.que;

import hla.rti.*;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.NullFederateAmbassador;
import hla13.clinic.ConstClass;
import hla13.clinic.ExternalEvent;
import org.portico.impl.hla13.types.DoubleTime;

import java.util.ArrayList;


public class QueAmbassador extends NullFederateAmbassador {

    //----------------------------------------------------------
    //                    STATIC VARIABLES
    //----------------------------------------------------------

    //----------------------------------------------------------
    //                   INSTANCE VARIABLES
    //----------------------------------------------------------
    // these variables are accessible in the package
    protected double federateTime        = 0.0;
    protected double grantedTime         = 0.0;
    protected double federateLookahead   = 1.0;

    protected boolean isRegulating       = false;
    protected boolean isConstrained      = false;
    protected boolean isAdvancing        = false;

    protected boolean isAnnounced        = false;
    protected boolean isReadyToRun       = false;

    protected boolean running 			 = true;
    protected int addPatientHandle = 0;
    protected int addDoctorHandle = 2;
    protected int doctorTreatPatientHandle = 3;

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
        this.grantedTime = convertTime( theTime );
        this.isAdvancing = false;
    }


   // public void receiveInteraction( int interactionClass,
   //                                 ReceivedInteraction theInteraction,
   //                                 byte[] tag )
   // {
   //     // just pass it on to the other method for printing purposes
   //     // passing null as the time will let the other method know it
   //     // it from us, not from the RTI
   //     receiveInteraction(interactionClass, theInteraction, tag, null, null);
   // }

    public void receiveInteraction( int interactionClass,
                                    ReceivedInteraction theInteraction,
                                    byte[] tag,
                                    LogicalTime theTime,
                                    EventRetractionHandle eventRetractionHandle )
    {
        StringBuilder builder = new StringBuilder( "Interaction Received:" );
        if(interactionClass == addPatientHandle) {
            try {
                int personNumber = EncodingHelpers.decodeInt(theInteraction.getValue(0));
                double time =  convertTime(theTime);
                externalEvents.add(new ExternalEvent(personNumber, ExternalEvent.EventType.ADDpatientToQue, time));
                builder.append("ADDpatient , time=" + time);
                builder.append(" id=").append(personNumber);
                builder.append( "\n" );

            } catch (ArrayIndexOutOfBounds ignored) {

            }

        }else if (interactionClass == addDoctorHandle) {
            try {
                int personNumber = EncodingHelpers.decodeInt(theInteraction.getValue(0));
                double time =  convertTime(theTime);
                externalEvents.add(new ExternalEvent(personNumber, ExternalEvent.EventType.ADDdoctor , time));
                builder.append( "ADDdoctor , time=" + time );
                builder.append( "\n" );

            } catch (ArrayIndexOutOfBounds ignored) {

            }
        }/* else if (interactionClass == getDoctorHandle) {
            try {
                int qty = EncodingHelpers.decodeInt(theInteraction.getValue(0));
                double time =  convertTime(theTime);
                externalEvents.add(new ExternalEvent(ExternalEvent.EventType.GET , time));
                builder.append( "GetProduct , time=" + time );
                //builder.append(" qty=").append(qty);
                //builder.append( "\n" );

            } catch (ArrayIndexOutOfBounds ignored) {

            }

        }*/

        log( builder.toString() );
    }

}
