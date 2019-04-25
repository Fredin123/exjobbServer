package updateCoordBuffers;

import satellite.Satellite;

import java.util.ArrayList;
import java.util.Timer;
import java.util.TimerTask;

import updateCoordBuffers.downloadEphemerides.OnDownloadEphemeridesCallback;

import javax.annotation.PostConstruct;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.ejb.Stateless;
import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

import updateCoordBuffers.downloadEphemerides.DownloadEphemerides;
import updateCoordBuffers.satellitesFutureCoordinateBuffer.OnSatellitesFutureCoordinateBufferCallback;
import updateCoordBuffers.satellitesFutureCoordinateBuffer.SatellitesFutureCoordinateBuffer;

@WebListener
@Singleton
public class Main implements OnDownloadEphemeridesCallback, ServletContextListener, OnSatellitesFutureCoordinateBufferCallback{
	private Timer scheduleDownloadNewEphemerides = new Timer();
	private long timerCounter = 0;
	public static boolean startBufferDone = false;
	
	
	
	private SatellitesFutureCoordinateBuffer satellitesFutureCoordinateBuffer;
	ArrayList<Satellite> storedSatellites;
	private long unixTimeOnDownloadStart;

	
	@Override
    public void contextInitialized(ServletContextEvent sce) {
        System.out.println("On start web app");
        startup();
    }

    @Override
    public void contextDestroyed(ServletContextEvent sce) {
        System.out.println("On shutdown web app");
    }
	
	public void startup() {
		satellitesFutureCoordinateBuffer = new SatellitesFutureCoordinateBuffer(this);
		startEphemeridesDownload();
	}
	
	
	private void startEphemeridesDownload() {
		System.out.println("startEphemeridesDownload\n\n");
		unixTimeOnDownloadStart = System.currentTimeMillis() / 1000L;
		//try {
			DownloadEphemerides.ftpDownload(this, false);
		/*}catch(Exception e) {
			System.out.println("Error while downloading ephemerides from ftp. Trying again");
			System.out.println(e.getMessage());
			startEphemeridesDownload();
		}*/
	}

	@Override
	public void OnDownloadEphemeridesCallback(ArrayList<Satellite> satellites) {
		long timeItTookToDownloadEphemerisData = (System.currentTimeMillis() / 1000L) - unixTimeOnDownloadStart;
		//subtract time it took to download from satellites unix time. Since otherwise they are gonna be as old as it took to download them.
		/*for(Satellite s : satellites) {
			//Need to offset the time it took to download the data to get correct time
			 //s.getUnixTime()-timeItTookToDownloadEphemerisData
			s.setUnixTime(unixTimeOnDownloadStart-timeItTookToDownloadEphemerisData);
		}*/
		
		
		System.out.println("retrieved satts: "+satellites.size());
		// TODO Auto-generated method stub
		storedSatellites = satellites;
        satellitesFutureCoordinateBuffer.startNewBufferFromBaselineSatelliteData(storedSatellites);
	}

	@Override
	public void onSatellitesFutureCoordinateBufferCallback(boolean b) {
		cleanupOldPredictions();
		updateLoop();
		if(!startBufferDone) {
			startBufferDone = true;
		}
	}
	
	
	private void updateLoop() {
		scheduleDownloadNewEphemerides.schedule(new TimerTask() {
    		@Override
    		public void run() {
    			if(timerCounter == 1800*10) {
    				//every ? hours, update data from ephemerides data
    				timerCounter = 0;
    				//startEphemeridesDownload();
    			}else {
    				//Update existing data
    				satellitesFutureCoordinateBuffer.downloadMoreIfBufferIsSmall();
    			}
    			
    			timerCounter++;
    		}
    	}, 2000);//After 2 seconds
	}
	
	
	private void cleanupOldPredictions() {
		satellitesFutureCoordinateBuffer.cleanup();
	}
	
	
}
