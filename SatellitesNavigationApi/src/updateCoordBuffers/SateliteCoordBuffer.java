package updateCoordBuffers;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;
import java.util.TreeMap;

import javax.ejb.Singleton;
import javax.ejb.Startup;

import satellite.Satellite;

@Startup
@Singleton
public class SateliteCoordBuffer {
	private static HashMap<Integer, TreeMap<Long, Satellite>> coordinateBufferOld = new HashMap<>();
	private static HashMap<Integer, TreeMap<Long, Satellite>> coordinateBuffer = new HashMap<>();
	
	
	public static void put(int key, TreeMap<Long, Satellite> map) {
		coordinateBuffer.put(key, map);
	}
	
	
	/*public static void replace(int key, TreeMap<Long, Satellite> map) {
		coordinateBuffer.replace(key, map);
	}*/
	
	public static Set<Integer> keySet(){
		return coordinateBuffer.keySet();
	}
	
	public static Set<Long> treeMapKeySet(int noradId){
		return coordinateBuffer.get(noradId).keySet();
	}
	
	public static void putInTreeMap(int key, long treeMapKey, Satellite s) {
		coordinateBuffer.get(key).put(treeMapKey, s);
	}
	
	public static TreeMap<Long, Satellite> getTreeMap(int noradId){
		return coordinateBuffer.get(noradId);
	}
	
	public static void removeFromTreeMap(int noradId, long treeMapKey) {
		coordinateBuffer.get(noradId).remove(treeMapKey);
	}
	
	public static void removeEntriesOlderThan(int noradId, long targetUnixTime) {
		/*Doesn't delete all elements if container will be empty after deletion*/
		Set<Long> treeMapKeyIter = coordinateBuffer.get(noradId).keySet();
		ArrayList<Long> removeList = new ArrayList<>();
		for(long ut : treeMapKeyIter) {
			if(ut < targetUnixTime) {
				removeList.add(ut);
			}
		}
		
		if(coordinateBuffer.get(noradId).size() > removeList.size()) {
			//Only remove if there will be elements left after deletion
			for(long l : removeList) {
				coordinateBuffer.get(noradId).remove(l);
			}
		}
		
	}
	public static void startNewBuffer() {
		coordinateBufferOld.clear();
		coordinateBufferOld = coordinateBuffer;
		coordinateBuffer.clear();
	}
	
	public static Satellite getSatelliteFromNewBufer(int noradId, long unixTime) {
		return coordinateBuffer.get(noradId).get(unixTime);
	}
	
	public static long getFurthestPredictedSatellite(int noradId) {
		Set<Long> unixTimePredictions = coordinateBuffer.get(noradId).keySet();
		long furthestPrediction = 0;
		for(long l : unixTimePredictions) {
			if(l > furthestPrediction) {
				furthestPrediction = l;
			}
		}
		return furthestPrediction;
	}
	
	public static long getFurthestPredictedSatelliteOldBuffer(int noradId) {
		Set<Long> unixTimePredictions = coordinateBufferOld.get(noradId).keySet();
		long furthestPrediction = 0;
		for(long l : unixTimePredictions) {
			if(l > furthestPrediction) {
				furthestPrediction = l;
			}
		}
		return furthestPrediction;
	}
	
	public static int howManyAvailibleSatellitesAreThere() {
		Set<Integer> noradIds = coordinateBuffer.keySet();
		int availible = 0;
		for(int noradId : noradIds) {
			if(SateliteCoordBuffer.areTherePredictionsForThisSatellite(noradId)) {
				availible++;
			}
		}
		return availible;
	}
	
	public static Satellite getSatelliteDataFromAllBuffers(int noradId, long unixTime) {
		if(coordinateBuffer.get(noradId).get(unixTime) != null) {
			return coordinateBuffer.get(noradId).get(unixTime);
		}else if(coordinateBufferOld.get(noradId).get(unixTime) != null){//our new buffer don't have the target unix time, check previously generated buffer
			return coordinateBufferOld.get(noradId).get(unixTime);
		}
		return null;
	}
	
	
	public static boolean areTherePredictionsForThisSatellite(int noradId) {
		if(coordinateBuffer.get(noradId).lastKey() == null && coordinateBufferOld.get(noradId).lastKey() == null){
			return false;
		}
		return true;
	}
}
