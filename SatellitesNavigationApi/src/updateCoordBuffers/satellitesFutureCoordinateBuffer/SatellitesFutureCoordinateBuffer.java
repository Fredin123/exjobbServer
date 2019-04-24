/*
 * THIS NEEDS TO BE UPDATED TO LOOK LIKE THE ANDROID STUDIO VERSION
 * */


package updateCoordBuffers.satellitesFutureCoordinateBuffer;

import java.util.ArrayList;

import java.util.Date;
import java.util.HashMap;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;
import java.util.TreeMap;

import javax.ejb.Singleton;

import org.apache.commons.math3.util.Pair;
import org.json.*;

import httpRequest.OnUpdateSatellitesFuturePositionCallback;
import httpRequest.UpdateSatellitesFuturePosition;
import satellite.Satellite;
import staticFunctions.SatelliteNORADId;
import updateCoordBuffers.SateliteCoordBuffer;

@Singleton
public class SatellitesFutureCoordinateBuffer implements OnUpdateSatellitesFuturePositionCallback {
    private UpdateSatellitesFuturePosition futureSatellitePositionUpdater;
    //             NORAD ID       UNIX TIME            Satellite coords
    private Date dateSinceEphemerisUpdate;

    private OnSatellitesFutureCoordinateBufferCallback callbackBoolean;

    public static final int targetMinBufferSize = 256;
    private final int numberOfOldPredictionsToKeep = 10;

    private Timer scheduleLoadMorePredictions;
    
    
    private OnSatellitesFutureCoordinateBufferCallback bufferComplete;
    
    private Timer timer = new Timer();

    public SatellitesFutureCoordinateBuffer(OnSatellitesFutureCoordinateBufferCallback callback){
    	bufferComplete = callback;
        scheduleLoadMorePredictions = new Timer();
        dateSinceEphemerisUpdate = new Date();

        futureSatellitePositionUpdater = new UpdateSatellitesFuturePosition(this);
    }

    public void startNewBufferFromBaselineSatelliteData(ArrayList<Satellite> newSatts){
        long unixTime = System.currentTimeMillis() / 1000L;
        
        SateliteCoordBuffer.startNewBuffer();
        
        System.out.println("newSatts.size: "+newSatts.size());
        long oldDataOffset = -1;
        for(Satellite s : newSatts){
        	/*Satellite coords = new Satellite(s.getLatitude(),
                    s.getLongitude(), s.getAltitude(), s.getUnixTime(), s.getNoradId());*/
        	SateliteCoordBuffer.put(s.getNoradId(), new TreeMap<>());
        	SateliteCoordBuffer.putInTreeMap(s.getNoradId(), s.getUnixTime(), s);
            if(oldDataOffset == -1 || oldDataOffset < (unixTime - s.getUnixTime())){
                oldDataOffset = (unixTime - s.getUnixTime());
            }
        }

        //Since the ephemerides data is old we first need to load satellites position to catch up with the current unix time
        int timeItTakesToDownloadPackage = 6/*Usually it's roughly 6 seconds*/;
        int numberOfUpdatesNeeded = Math.round(oldDataOffset/(300-timeItTakesToDownloadPackage));
        if(numberOfUpdatesNeeded > 0) {
        	System.out.println( "Need to make roughly "+numberOfUpdatesNeeded+" requests to catch up to current unix time");
        }else {
            System.out.println("Buffer has already caught up to current time, being loading predicted data.");
        }
            


        //futureSatellitePositionUpdater.updateSatelliteArrayIfAvailible(startCoordsSattelites);
        retrieveMorePredictionData();
    }


    private void retrieveMorePredictionData(){
        ArrayList<Satellite> startCoordsSattelites = new ArrayList<>();
        Set<Integer> noradIds = SateliteCoordBuffer.keySet();
        for(Integer noradId : noradIds){
            Satellite s = SateliteCoordBuffer.getSatelliteFromNewBufer(
            		noradId, 
            		SateliteCoordBuffer.getFurthestPredictedSatellite(noradId)
            		);
            if(s == null) {
            	System.out.println("Satellite is null! noradid:"+ noradId+" get furthest predicted satellite position from old buffer");
            	System.out.println(SateliteCoordBuffer.getTreeMap(noradId).toString());
            }
            startCoordsSattelites.add(s);
        }
        

        
        try {
        	futureSatellitePositionUpdater.updateSatelliteArrayIfAvailible(startCoordsSattelites);
        }catch(NullPointerException e) {
        	System.out.println("NullPointerException in SatellitesFutureCoordinateBuffer line 127.");
        	System.out.println(e.getMessage());
        	//End
        	bufferComplete.onSatellitesFutureCoordinateBufferCallback(true);
        }
        
    }
    
    private void retrieveMorePredictionDataForOldBuffer(){
    	//TODO: implements so that old buffer doesnt tun out of predictions while new buffer is loading
    }

    private boolean isBufferBigEnough(){
        //This also removes outdated predictions
        long currentUnixTime = System.currentTimeMillis() / 1000L;

        Set<Integer> noradIds = SateliteCoordBuffer.keySet();
        for(int noradId : noradIds){
        	TreeMap<Long, Satellite> satellitePredictions = SateliteCoordBuffer.getTreeMap(noradId);
        	long satelliteFurthestPrediction = SateliteCoordBuffer.getFurthestPredictedSatellite(noradId);
        	if(satelliteFurthestPrediction > currentUnixTime) {
        		/*Satellites furthest prediction is in the future. Check how long in the future*/
        		long howLongInTheFuture = satelliteFurthestPrediction - currentUnixTime;
        		//Check if this satellite have enough buffered predictions
        		System.out.println("howLongInTheFuture: "+howLongInTheFuture);
        		if(howLongInTheFuture < targetMinBufferSize) {
        			System.out.println("Satellite "+noradId+" buffer needs "+(targetMinBufferSize-howLongInTheFuture)+" more predictions to reach prediction target");
        			return false;
        		}
        	}else {
        		System.out.println("Satellite "+noradId+" buffer needs "+(currentUnixTime-satelliteFurthestPrediction)+" more predictions to reach current time");
        		double estimatedTime = ((((currentUnixTime-satelliteFurthestPrediction)/200)*UpdateSatellitesFuturePosition.timeItTakesToDownload)/60) 
        				* SateliteCoordBuffer.howManyAvailibleSatellitesAreThere();
        		System.out.println("Estimated time: "+estimatedTime+" minutes");
        		return false;
        	}
        }
        
        
        return true;
    }

    
    
    public void cleanup() {
        long unixTime = System.currentTimeMillis() / 1000L;
        
    	Set<Integer> noradIds = SateliteCoordBuffer.keySet();
        for(int noradId : noradIds){
        	//remove all old projections except the last which will be our next base projection to continue our satellite projection from
            SateliteCoordBuffer.removeEntriesOlderThan(noradId, unixTime-numberOfOldPredictionsToKeep);
        }
    	
    }



    public Set<Integer> getCoordinateBufferKeyset(){
        return SateliteCoordBuffer.keySet();
    }





    private void insertNewProjectedCoordinates(int noradId, double latitude, double longitude, double altitude, long targetUnixTime){
    	Satellite coords = new Satellite(latitude, longitude, altitude, targetUnixTime, noradId);
    	SateliteCoordBuffer.putInTreeMap(noradId, targetUnixTime, coords);
    }


    @Override
    public void onUpdateSatellitesFuturePositionCallback(ArrayList<Pair<Long, JSONArray>> returnedData) {
    	for(Pair<Long, JSONArray> d : returnedData) {
    		long howLongItTookToDownload = d.getKey();
    		//System.out.println("howLongItTookToDownload: "+howLongItTookToDownload);
    		JSONArray dataArr = d.getValue();
    		
    		for(int i=0; i<dataArr.length(); i++){
    			int noradId = dataArr.getJSONObject(i).getJSONObject("info").getInt("satid");
    			
    			long furthestPrediction = SateliteCoordBuffer.getFurthestPredictedSatellite(noradId);

                
    			JSONArray positions = dataArr.getJSONObject(i).getJSONArray("positions");
    			for(int p=0; p<positions.length(); p++) {
    				insertNewProjectedCoordinates(
                            noradId,
                            positions.getJSONObject(p).getDouble("satlatitude"),
                            positions.getJSONObject(p).getDouble("satlongitude"),
                            positions.getJSONObject(p).getDouble("sataltitude"),
                            furthestPrediction+p);
    			}
    			
    		}
    		
    	}


        if(isBufferBigEnough()){
        	bufferComplete.onSatellitesFutureCoordinateBufferCallback(true);
        }else{
            retrieveMorePredictionData();
        }

    }
    
    
    public void downloadMoreIfBufferIsSmall() {
    	if(!isBufferBigEnough()) {
			retrieveMorePredictionData();
		}else {
			bufferComplete.onSatellitesFutureCoordinateBufferCallback(true);
		}
    }




}


