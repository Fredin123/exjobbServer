package httpRequest;

import java.io.IOException;
import org.apache.commons.math3.util.Pair;

import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

/*import org.json.simple.JSONArray;
import org.json.simple.JSONObject;*/
import org.json.*;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import satellite.Satellite;

public class UpdateSatellitesFuturePosition{
    ArrayList<Satellite> satelliteArrayToUpdate;
    private OnUpdateSatellitesFuturePositionCallback callback;
    private boolean isBussyLoadingData = false;

    private ArrayList<Pair<Long, JSONArray>> returnedArraysFromConnections = new ArrayList<>();
    
    private long downloadStartTime;
    
    public static long timeItTakesToDownload = 6;


    public UpdateSatellitesFuturePosition(OnUpdateSatellitesFuturePositionCallback cb){
        callback = cb;
    }


    public void updateSatelliteArrayIfAvailible(ArrayList<Satellite> satelliteArray) throws NullPointerException{
        if(!isBussyLoadingData){
        	isBussyLoadingData = true;
        	returnedArraysFromConnections.clear();
        	downloadStartTime = System.currentTimeMillis() / 1000L;
            satelliteArrayToUpdate = satelliteArray;
            
            while(isServerReachable("https://www.n2yo.com") == false) {
            	System.out.println("Server is not reachable");
            	try {
					Thread.sleep(10000);
				} catch (InterruptedException e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
				}
            }

            String[] apiRequests = new String[satelliteArrayToUpdate.size()];
            for(int i=0; i<satelliteArrayToUpdate.size(); i++){
            	Satellite s = satelliteArrayToUpdate.get(i);
                String request = "https://www.n2yo.com/rest/v1/satellite/positions/"+Integer.toString(s.getNoradId())
                        +"/"+s.getLatitude()+"/"+s.getLongitude()+"/"+s.getAltitude()+"/300/&apiKey=AC9EDN-UHFJ8N-GDQQZZ-3ZCN";
                apiRequests[i] = request;
            }
            
            sendRequests(apiRequests);
            
        }


    }

    private void sendRequest(String[] urls) throws UnknownHostException{
    	long unixStartDownloadTime = System.currentTimeMillis() / 1000L;
        JSONArray container = new JSONArray();
        int counter = 0;
        for(int i=0; i<urls.length; i++) {
        	System.out.print(" . ");
        }
        System.out.println();
        for(String url : urls){
            Request request = new Request.Builder()
                    .url(url)
                    .build();

            try (Response response = OkHttpClientHandler.getHttpClient().newCall(request).execute()) {
                container.put(counter, new JSONObject(response.body().string()));
                response.body().close();
                counter++;
                System.out.print(" . ");
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }catch(Exception e) {
            	//api server might be down try request again
            	System.out.println("Request failed try again:      "+url);
            	sendRequest(urls);
            	return;
            }
            //Log.i("Project", counter+"/"+urls.length);
        }
        System.out.println();
        
        timeItTakesToDownload = (System.currentTimeMillis() / 1000L) - unixStartDownloadTime;
        
        OnHttpRequestCallback(container);
    }
    
    
    public void sendRequests(final String[] pool){
    	Thread connectionThread = new Thread(){
            public void run(){
                //new HttpRequest(UpdateSatellitesFuturePosition.this, poolOrder).execute(pool);
            	boolean wasAbleToSendRequest = false;
            	while(wasAbleToSendRequest == false) {
            		try {
                		sendRequest(pool);
                		wasAbleToSendRequest = true;
                	}catch(UnknownHostException e) {
                		System.out.print("Could not access api hostm, internet might be down. Trying again.");
                	}
            	}
            	
                
            }
        };
        connectionThread.start();
    }

    
    public void OnHttpRequestCallback(JSONArray result) {
    	long timeToDownloadThisPart = (System.currentTimeMillis()/1000L) - downloadStartTime;
        returnedArraysFromConnections.add(new Pair<>(timeToDownloadThisPart, result));
        
        isBussyLoadingData = false;
        callback.onUpdateSatellitesFuturePositionCallback(returnedArraysFromConnections);
    }
    

    
    private boolean isServerReachable(String url) {
    	/*
    	 * InetAddress.getByName(url).isReachable(20 * 1000);
    	 * should.,d not be used as it depends on the machine having
    	 * full privileges which is not always guaranteed*/
		
    	try {
    		URL serverUrl = new URL(url);
    		HttpsURLConnection connection = (HttpsURLConnection)serverUrl.openConnection();
    	    connection.setRequestMethod("GET");
    	    connection.connect();
    	    int responseCode = connection.getResponseCode();
    		if(responseCode == 200) {
    			return true;
    		}
    	}catch(UnknownHostException e) {
    		return false;
    	} catch (MalformedURLException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
    		return false;
		} catch (IOException e) {
			// TODO Auto-generated catch block
			//e.printStackTrace();
			return false;
		}
    	
    	
    	return false;
    }



}


