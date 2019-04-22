package satellite;

import staticFunctions.CalculateSatellitePosition;
import staticFunctions.SatelliteNORADId;

public class Satellite {
    private SatelliteEphemerides eph;
    private SatellitePositionData myPositionData;
    private int noradId;
    private int svId;

    private double pseudorange = -1;

    private long unixTime;
    private double longitude;
    private double latitude;
    private double altitude;

    public Satellite(String data){
        eph = new SatelliteEphemerides(data);
        myPositionData = CalculateSatellitePosition.getGalileoSatellitePosition(eph);

        noradId = SatelliteNORADId.getGalileoNORADId(eph.getSvid());
        
        svId = eph.getSvid();
        		
        unixTime = eph.getUnixTime();
        latitude = myPositionData.getEllipsoidalCoords()[0] * (180/Math.PI);
        longitude = myPositionData.getEllipsoidalCoords()[1] * (180/Math.PI);
        altitude = myPositionData.getEllipsoidalCoords()[2];
    }
    
    public Satellite(double latitude, double longitude, double altitude, long unixTime, int noradId){
        this.longitude = longitude;
        this.latitude = latitude;
        this.altitude = altitude;
        this.unixTime = unixTime;
        this.noradId = noradId;
    }

    public int getNoradId(){
        return noradId;
    }

    public SatellitePositionData getSatellitePositionData(){
        return myPositionData;
    }

    public SatelliteEphemerides getEphemerides(){
        return eph;
    }

    
    public void updatteSatelliteCoordinates(){
        myPositionData = CalculateSatellitePosition.getGalileoSatellitePosition(eph);
    }

    public void updateSatelliteEphemerides(SatelliteEphemerides newEph){
        eph = newEph;
    }

    public int getSvid(){
        return svId;
    }

    public double getLatitude() {
        //Angle is stored in rade but this returns it in degrees
        return latitude;
    }
    
    public double getLongitude() {
        //Angle is stored in rade but this returns it in degrees
        return longitude;
    }

    public double getAltitude(){
        return altitude;
    }

    public long getUnixTime(){
        return unixTime;
    }
    
    public void setUnixTime(long newUnixTime){
    	unixTime = newUnixTime;
    }


    public void setPseudorange(double pseudorange) {
        this.pseudorange = pseudorange;
    }
}
