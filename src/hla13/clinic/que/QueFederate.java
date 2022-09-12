package hla13.clinic.que;


import hla.rti.*;
import hla.rti.jlc.EncodingHelpers;
import hla.rti.jlc.RtiFactoryFactory;
import hla13.clinic.ExternalEvent;
import org.portico.impl.hla13.types.DoubleTime;
import org.portico.impl.hla13.types.DoubleTimeInterval;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.util.ArrayList;
import java.util.Random;

public class QueFederate {

    public static final String READY_TO_RUN = "ReadyToRun";

    private RTIambassador rtiamb;
    private QueAmbassador fedamb;
    private final double timeStep           = 10.0;
    private ArrayList<Integer> queArrayListPatients = new ArrayList<>();
    private ArrayList<Integer> queArrayListDoctors = new ArrayList<>();
    private int queHlaHandle;


    public void runFederate() throws Exception {

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

        fedamb = new QueAmbassador();
        rtiamb.joinFederationExecution( "QueFederate", "ExampleFederation", fedamb );
        log( "Joined Federation as QueFederate");

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

        publishAndSubscribe(); // merged with registerStorageObject(); beacuse there is literally no reason why not

        while (fedamb.running) {
            double timeToAdvance = fedamb.federateTime + timeStep;
            advanceTime(timeToAdvance);

            if(fedamb.externalEvents.size() > 0) {
                fedamb.externalEvents.sort(new ExternalEvent.ExternalEventComparator());
                for(ExternalEvent externalEvent : fedamb.externalEvents) {
                    fedamb.federateTime = externalEvent.getTime();
                    switch (externalEvent.getEventType()) {
                        case ADDpatient:
                            log(" ADDpatient");
                            this.addPatientQue(externalEvent.getPersonNumber());
                            break;
                        //case GET:
                        //    externalEvent.getPersonNumber();
                        //    this.getPatientQue();
                        //    break;
                        case ADDdoctor:
                            log(" ADDdoctor");
                            this.addDoctorQue(externalEvent.getPersonNumber());
                            break;
                    }
                }
                fedamb.externalEvents.clear();
            }

            if(fedamb.grantedTime == timeToAdvance) {
                timeToAdvance += fedamb.federateLookahead;
                fedamb.federateTime = timeToAdvance;
                log("Current time :" + fedamb.federateTime);
                while(this.queArrayListDoctors.size() > 0 && this.queArrayListPatients.size() > 0){
                    log("Patient: " + this.queArrayListPatients.get(0) + " is being treated by doctor: " + this.queArrayListDoctors.get(0));
                    //log("Removing doctor: " + );
                    sendInteractionGetDoctor(fedamb.federateTime + fedamb.federateLookahead);
                    updateHLAObject(timeToAdvance);
                }
            }
            rtiamb.tick();
        }

    }



    public void addPatientQue(int patientNumber) {
        this.queArrayListPatients.add(patientNumber);
        log("Added "+ patientNumber + " at time: "+ fedamb.federateTime +", current patient list size: " + this.queArrayListPatients.size());
    }

    public void addDoctorQue(int doctorNumber) {
        this.queArrayListDoctors.add(doctorNumber);
        log("Added "+ doctorNumber + " at time: "+ fedamb.federateTime +", current doctor list size: " + this.queArrayListDoctors.size());
    }

    /*public void getPatientQue() {
        int patientNumberToReturn = -1;
        if(this.queArrayListPatients.size() < 0) {
            log("Que empty");
        }
        else {
            patientNumberToReturn = this.queArrayListPatients.get(0);
            this.queArrayListPatients.remove(0);
            log("Removed "+ patientNumberToReturn + " at time: "+ fedamb.federateTime +", current patient list size:  " + this.queArrayListPatients.size());

        }
    }*/

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

    private void updateHLAObject(double time) throws RTIexception{
        SuppliedAttributes attributes =
                RtiFactoryFactory.getRtiFactory().createSuppliedAttributes();
        //int classHandle = rtiamb.getObjectClass(queHlaHandle);
        //int doctorNumberHandle = rtiamb.getAttributeHandle( "doctorNumber", classHandle );
        //int doctorNumber = queArrayListDoctors.size();
        //attributes.add();
    }

    private void advanceTime( double timeToAdvance ) throws RTIexception {
        log("requesting time advance to: " + timeToAdvance);
        fedamb.isAdvancing = true;
        LogicalTime newTime = convertTime( timeToAdvance );
        rtiamb.timeAdvanceRequest( newTime );

        while( fedamb.isAdvancing )
        {
            rtiamb.tick();
        }
    }

    private void publishAndSubscribe() throws RTIexception {

        int addPatientHandle = rtiamb.getInteractionClassHandle( "InteractionRoot.AddPatientQue" );
        fedamb.addPatientHandle = addPatientHandle;
        rtiamb.subscribeInteractionClass( addPatientHandle );


        int addDoctorHandle = rtiamb.getInteractionClassHandle( "InteractionRoot.AddDoctorQue" );
        fedamb.addDoctorHandle = addDoctorHandle;
        rtiamb.subscribeInteractionClass( addDoctorHandle );

        int doctorTreatPatientHandle = rtiamb.getInteractionClassHandle( "InteractionRoot.DoctorTreatPatient");
        fedamb.doctorTreatPatientHandle = doctorTreatPatientHandle;
        rtiamb.publishInteractionClass(doctorTreatPatientHandle);


        //int getPatientHandle = rtiamb.getInteractionClassHandle( "InteractionRoot.GetPatientQue" );
        //fedamb.getProductHandle = getPatientHandle;
        //rtiamb.subscribeInteractionClass( getPatientHandle );

    }

    private void sendInteractionGetDoctor(double timeToSendMessage) throws RTIexception {
        SuppliedParameters parameters =
                RtiFactoryFactory.getRtiFactory().createSuppliedParameters();
        byte[] doctorNumber = EncodingHelpers.encodeInt(queArrayListDoctors.get(0));
        byte[] patientNumber = EncodingHelpers.encodeInt(queArrayListPatients.get(0));
        this.queArrayListPatients.remove(0);
        this.queArrayListDoctors.remove(0);
        int interactionHandle = rtiamb.getInteractionClassHandle("InteractionRoot.DoctorTreatPatient");
        int doctorNumberHandle = rtiamb.getParameterHandle( "doctorNumber", interactionHandle );
        int patientNumberHandle = rtiamb.getParameterHandle( "patientNumber", interactionHandle );
        LogicalTime time = convertTime( timeToSendMessage );

        parameters.add(doctorNumberHandle, doctorNumber);
        parameters.add(patientNumberHandle, patientNumber);
        rtiamb.sendInteraction( interactionHandle, parameters, "tag".getBytes(), time );
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
        System.out.println( "QueFederate   : " + message );
    }

    public static void main(String[] args) {
        try {
            new QueFederate().runFederate();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
