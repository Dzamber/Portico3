package hla13.clinic.patient;


import hla.rti.*;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.RtiFactoryFactory;
import org.portico.impl.hla13.types.DoubleTime;
import org.portico.impl.hla13.types.DoubleTimeInterval;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.Random;

public class PatientFederate {

    public static final String READY_TO_RUN = "ReadyToRun";

    private RTIambassador rtiamb;
    private PatientAmbassador fedamb;
    private final double timeStep           = 10.0;
    private int patientHlaHandle;
    static int patientAmountCurrent = 0;

    public void runFederate() throws RTIexception{
        rtiamb = RtiFactoryFactory.getRtiFactory().createRtiAmbassador();

        try
        {
            File fom = new File( "producer-consumer.fed" );
            rtiamb.createFederationExecution( "ExampleFederation",
                    fom.toURI().toURL() );
            log( "Created Federation" );
        }
        catch( FederationExecutionAlreadyExists exists )
        {
            log( "Didn't create federation, it already existed" );
        }
        catch( MalformedURLException urle )
        {
            log( "Exception processing fom: " + urle.getMessage() );
            urle.printStackTrace();
            return;
        }

        fedamb = new PatientAmbassador();
        rtiamb.joinFederationExecution( "PatientFederate", "ExampleFederation", fedamb );
        log( "Joined Federation as PatientFederate");

        rtiamb.registerFederationSynchronizationPoint( READY_TO_RUN, null );

        while( fedamb.isAnnounced == false )
        {
            rtiamb.tick();
        }

        waitForUser();

        rtiamb.synchronizationPointAchieved( READY_TO_RUN );
        log( "Achieved sync point: " +READY_TO_RUN+ ", waiting for federation..." );
        while( fedamb.isReadyToRun == false )
        {
            rtiamb.tick();
        }

        enableTimePolicy();

        publishAndSubscribe();
        registerPatientObject();
        while (fedamb.running) {

            advanceTime(randomTime());
            log("Current time :" + fedamb.federateTime);
            sendInteraction(fedamb.federateTime + fedamb.federateLookahead);
            rtiamb.tick();
        }

    }

    private double randomTime() {
        Random r = new Random();
        return timeStep +(4 * r.nextDouble());
    }


    private void registerPatientObject() throws RTIexception {
        int classHandle = rtiamb.getObjectClassHandle("ObjectRoot.Patient");
        int patientHandle    = rtiamb.getAttributeHandle( "patientNumber", classHandle );
        AttributeHandleSet attributes =
                RtiFactoryFactory.getRtiFactory().createAttributeHandleSet();
        attributes.add( patientHandle );
        rtiamb.publishObjectClass(classHandle, attributes);
        this.patientHlaHandle = rtiamb.registerObjectInstance(classHandle);
    }

    private void waitForUser()
    {
        log( " >>>>>>>>>> Press Enter to Continue <<<<<<<<<<" );
        BufferedReader reader = new BufferedReader( new InputStreamReader(System.in) );
        try
        {
            reader.readLine();
        }
        catch( Exception e )
        {
            log( "Error while waiting for user input: " + e.getMessage() );
            e.printStackTrace();
        }
    }

    private void enableTimePolicy() throws RTIexception
    {
        LogicalTime currentTime = convertTime( fedamb.federateTime );
        LogicalTimeInterval lookahead = convertInterval( fedamb.federateLookahead );

        this.rtiamb.enableTimeRegulation( currentTime, lookahead );

        while( fedamb.isRegulating == false )
        {
            rtiamb.tick();
        }

        this.rtiamb.enableTimeConstrained();

        while( fedamb.isConstrained == false )
        {
            rtiamb.tick();
        }
    }

    private void sendInteraction(double timeStep) throws RTIexception {
        SuppliedParameters parameters =
                RtiFactoryFactory.getRtiFactory().createSuppliedParameters();
        byte[] quantity = EncodingHelpers.encodeInt(patientAmountCurrent);
        patientAmountCurrent++;
        int interactionHandle = rtiamb.getInteractionClassHandle("InteractionRoot.AddPatientQue");

        int quantityHandle = rtiamb.getParameterHandle( "patientNumber", interactionHandle );
        LogicalTime time = convertTime( timeStep );

        parameters.add(quantityHandle, quantity);

        rtiamb.sendInteraction( interactionHandle, parameters, "tag".getBytes(), time );
    }

    private void publishAndSubscribe() throws RTIexception {
        int addProductHandle = rtiamb.getInteractionClassHandle( "InteractionRoot.AddPatientQue" );
        int classHandle = rtiamb.getObjectClassHandle("ObjectRoot.Patient");
        int patientNumber    = rtiamb.getAttributeHandle( "patientNumber", classHandle );

        AttributeHandleSet attributes =
                RtiFactoryFactory.getRtiFactory().createAttributeHandleSet();
        attributes.add( patientNumber );

        rtiamb.publishObjectClass(classHandle, attributes);

        classHandle = rtiamb.getObjectClassHandle("ObjectRoot.Patient");
        this.patientHlaHandle = rtiamb.registerObjectInstance(classHandle);
        rtiamb.publishInteractionClass(addProductHandle);
    }

    private void advanceTime( double timestep ) throws RTIexception
    {
        log("requesting time advance for: " + timestep + ", in total: " + fedamb.federateTime + timestep);
        // request the advance
        fedamb.isAdvancing = true;
        LogicalTime newTime = convertTime( fedamb.federateTime + timestep );
        rtiamb.timeAdvanceRequest( newTime );
        while( fedamb.isAdvancing )
        {
            rtiamb.tick();
        }
    }


    private void updateHLAObject(double time) throws RTIexception{
        SuppliedAttributes attributes =
                RtiFactoryFactory.getRtiFactory().createSuppliedAttributes();

        int classHandle = rtiamb.getObjectClass(patientHlaHandle);
        int stockHandle = rtiamb.getAttributeHandle( "Patient", classHandle );
        byte[] stockValue = EncodingHelpers.encodeInt(patientAmountCurrent);

        attributes.add(stockHandle, stockValue);
        LogicalTime logicalTime = convertTime( time );
        rtiamb.updateAttributeValues( patientHlaHandle, attributes, "actualize patient".getBytes(), logicalTime );
    }

    private LogicalTime convertTime( double time )
    {
        // PORTICO SPECIFIC!!
        return new DoubleTime( time );
    }

    /**
     * Same as for {@link #convertTime(double)}
     */
    private LogicalTimeInterval convertInterval( double time )
    {
        // PORTICO SPECIFIC!!
        return new DoubleTimeInterval( time );
    }

    private void log( String message )
    {
        System.out.println( "PatientFederate   : " + message );
    }

    public static void main(String[] args) {
        try {
            new PatientFederate().runFederate();
        } catch (RTIexception rtIexception) {
            rtIexception.printStackTrace();
        }
    }

}
