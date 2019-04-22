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

    public static final int targetMinBufferSize = 280;
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
        int timeItTakesToDownloadPackage = 6;
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
        long currentUnixTime = (System.currentTimeMillis() / 1000L);
        ArrayList<Satellite> startCoordsSattelites = new ArrayList<>();

        Set<Integer> noradIds = SateliteCoordBuffer.keySet();

        long oldestData = 0;
        boolean thereIsOldDataLeft = false;
        for(Integer noradId : noradIds){
            Object[] unixTimes = SateliteCoordBuffer.treeMapKeySet(noradId).toArray();

            Satellite s = SateliteCoordBuffer.getSatelliteFromNewBufer(noradId, (Long)unixTimes[unixTimes.length-1]);

            long diff = (currentUnixTime - (long)unixTimes[unixTimes.length-1]);
            //System.out.println( "base unix time: "+unixTimes[unixTimes.length-1]+"       is "+(diff/60)+" min old");

            if(diff > 0) {
            	thereIsOldDataLeft = true;
            }
            if(diff > oldestData) {
            	oldestData = diff;
            }
            
            startCoordsSattelites.add(s);
        }
        
        if(thereIsOldDataLeft) {
        	System.out.println("There is old data that needs to be updated. Oldest is: "+(oldestData/60)+" min old");
        }

        futureSatellitePositionUpdater.updateSatelliteArrayIfAvailible(startCoordsSattelites);
    }
    
    private void retrieveMorePredictionDataForOldBuffer(){
    	//TODO: implements so that old buffer doesnt tun out of predictions while new buffer is loading
    }

    private boolean isBufferBigEnough(){
        //This also removes outdated predictions
        long unixTime = System.currentTimeMillis() / 1000L;

        Set<Integer> noradIds = SateliteCoordBuffer.keySet();
        int smallesUseableBuffer = 0;
        for(int noradId : noradIds){
            int futureCoordinatesStillUseable = 0;
            //ArrayList<SatelliteCoordsSimple> coordsArr = coordinateBuffer.get(noradId);
            TreeMap<Long, Satellite> coordsHashMap = SateliteCoordBuffer.getTreeMap(noradId);
            Set<Long> unixTimes = coordsHashMap.keySet();

            ArrayList<Long> oldUnixTimes = new ArrayList<>();
            for(Long projectedUnixTime : unixTimes){

                if(projectedUnixTime >= unixTime-numberOfOldPredictionsToKeep){
                    futureCoordinatesStillUseable++;
                }else{
                	futureCoordinatesStillUseable--;
                    oldUnixTimes.add(projectedUnixTime);
                }
            }

            //remove all old projections except the last which will be our next base projection to continue our satellite projection from
            while(oldUnixTimes.size() > 1){
            	SateliteCoordBuffer.removeFromTreeMap(noradId, oldUnixTimes.get(0));
                oldUnixTimes.remove(0);
            }

            if(smallesUseableBuffer == 0 || smallesUseableBuffer > futureCoordinatesStillUseable){
                smallesUseableBuffer = futureCoordinatesStillUseable;
            }
        }

        System.out.println( "Smallest buffer test: "+smallesUseableBuffer+" < "+targetMinBufferSize);
        if(smallesUseableBuffer < targetMinBufferSize){
        	System.out.println( "Projection buffer is too small");
            return false;
        }

        return true;
    }




    public Set<Integer> getCoordinateBufferKeyset(){
        return SateliteCoordBuffer.keySet();
    }

    public void setSatellitePseudorande(int svid, double pseudorange){
        long currentUnixTime = System.currentTimeMillis() / 1000L;
        TreeMap<Long, Satellite> storedPositions;
        //set new pseudorange to each satellite with unix time "currentUnixTime" and after that time
        storedPositions = SateliteCoordBuffer.getTreeMap(SatelliteNORADId.getGalileoNORADId(svid));
        Set<Long> unixTimes = storedPositions.keySet();

        for(Long ut : unixTimes){
            if(ut >= currentUnixTime){
                storedPositions.get(ut).setPseudorange(pseudorange);
            }
        }



    }




    private void insertNewProjectedCoordinates(int noradId, double latitude, double longitude, double altitude, long targetUnixTime){
    	Satellite coords = new Satellite(latitude, longitude, altitude, targetUnixTime, noradId);
    	SateliteCoordBuffer.putInTreeMap(noradId, targetUnixTime, coords);
    }


    @Override
    public void onUpdateSatellitesFuturePositionCallback(JSONArray jsonArray) {
        for(int i=0; i<jsonArray.length(); i++){
            int noradId = 0;
            try {
                noradId = jsonArray.getJSONObject(i).getJSONObject("info").getInt("satid");
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Object[] keyset = SateliteCoordBuffer.treeMapKeySet(noradId).toArray();
            long baseUnixTime = (long)keyset[keyset.length-1];
            try {
                JSONArray arrayOfPositions = jsonArray.getJSONObject(i).getJSONArray("positions");
                for(int ai=0; ai<arrayOfPositions.length(); ai++){
                    insertNewProjectedCoordinates(
                            noradId,
                            arrayOfPositions.getJSONObject(ai).getDouble("satlatitude"),
                            arrayOfPositions.getJSONObject(ai).getDouble("satlongitude"),
                            arrayOfPositions.getJSONObject(ai).getDouble("sataltitude"),
                            baseUnixTime+(ai+1));
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }

        if(isBufferBigEnough()){
        	bufferComplete.onSatellitesFutureCoordinateBufferCallback(true);
        }else{
            System.out.println( "Buffer is not big enough, load more!");
            retrieveMorePredictionData();
        }

    }
    
    
    public void downloadMoreIfBufferIsSmall() {
    	if(!isBufferBigEnough()) {
			retrieveMorePredictionData();
		}
    }




}


