package httpRequest;

import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.InetAddress;
import java.net.MalformedURLException;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import javax.net.ssl.HttpsURLConnection;

/*import org.json.simple.JSONArray;
import org.json.simple.JSONObject;*/
import org.json.*;

import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import satellite.Satellite;

public class UpdateSatellitesFuturePosition implements OnHttpRequestCallback {
    ArrayList<Satellite> satelliteArrayToUpdate;
    private OnUpdateSatellitesFuturePositionCallback callback;
    private boolean isBussyLoadingData = false;

    private int connectionsOpened = 0;
    private JSONArray[] returnedArraysFromConnections;
    private int numberOfRetrievedResults = 0;
    
    


    public UpdateSatellitesFuturePosition(OnUpdateSatellitesFuturePositionCallback cb){
        callback = cb;
    }


    public void updateSatelliteArrayIfAvailible(ArrayList<Satellite> satelliteArray){
        if(!isBussyLoadingData){
            numberOfRetrievedResults = 0;
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

            //Send requests
            if(apiRequests.length > 3){
                //Divide the requests into four different connections to make it faster
                int poolSize = (int)Math.floor((double)apiRequests.length/4.0);
                int leftover = apiRequests.length - poolSize*4;
                if(leftover < 0){
                    leftover = 0;
                }

                String[] pool1 = new String[poolSize];
                String[] pool2 = new String[poolSize];
                String[] pool3 = new String[poolSize];
                String[] pool4 = new String[poolSize+leftover];
                for(int i=0; i<poolSize; i++){
                    pool1[i] = apiRequests[i];
                    pool2[i] = apiRequests[poolSize+i];
                    pool3[i] = apiRequests[(poolSize*2)+i];
                    pool4[i] = apiRequests[(poolSize*3)+i];
                }

                for(int i=0; i<leftover; i++){
                    pool4[poolSize+i] = apiRequests[(poolSize*4)+i];
                }

                connectionsOpened = 4;
                returnedArraysFromConnections = new JSONArray[connectionsOpened];
                startNewParallelConnection(0, pool1);
                startNewParallelConnection(1, pool2);
                startNewParallelConnection(2, pool3);
                startNewParallelConnection(3, pool4);

            }else if(apiRequests.length == 3){
                String[] pool1 = new String[]{apiRequests[0]};
                String[] pool2 = new String[]{apiRequests[1]};
                String[] pool3 = new String[]{apiRequests[2]};

                connectionsOpened = 3;
                returnedArraysFromConnections = new JSONArray[connectionsOpened];
                startNewParallelConnection(0, pool1);
                startNewParallelConnection(1, pool2);
                startNewParallelConnection(2, pool3);

            }else if(apiRequests.length == 2){
                String[] pool1 = new String[]{apiRequests[0]};
                String[] pool2 = new String[]{apiRequests[1]};

                connectionsOpened = 2;
                returnedArraysFromConnections = new JSONArray[connectionsOpened];
                startNewParallelConnection(0, pool1);
                startNewParallelConnection(1, pool2);

            }else if(apiRequests.length == 1){
                connectionsOpened = 1;
                returnedArraysFromConnections = new JSONArray[connectionsOpened];
                startNewParallelConnection(0, apiRequests);
            }

            isBussyLoadingData = true;
        }







    }

    private void sendRequest(int poolOrder, String[] urls) throws UnknownHostException{
        JSONArray container = new JSONArray();
        int counter = 0;
        for(String url : urls){
            Request request = new Request.Builder()
                    .url(url)
                    .build();

            
            try (Response response = OkHttpClientHandler.getHttpClient().newCall(request).execute()) {
                container.put(counter, new JSONObject(response.body().string()));
                response.body().close();
            } catch (IOException e) {
                e.printStackTrace();
            } catch (JSONException e) {
                e.printStackTrace();
            }catch(Exception e) {
            	//api server might be down try request again
            	sendRequest(poolOrder, urls);
            }
            counter++;
            //Log.i("Project", counter+"/"+urls.length);
        }

        OnHttpRequestCallback(poolOrder, container);
    }
    public void startNewParallelConnection(final int poolOrder, final String[] pool){
        Thread connectionThread = new Thread(){
            public void run(){
                //new HttpRequest(UpdateSatellitesFuturePosition.this, poolOrder).execute(pool);
            	boolean wasAbleToSendRequest = false;
            	while(wasAbleToSendRequest == false) {
            		try {
                		sendRequest(poolOrder, pool);
                		wasAbleToSendRequest = true;
                	}catch(UnknownHostException e) {
                		System.out.print("Could not access api hostm, internet might be down. Trying again.");
                	}
            	}
            	
                
            }
        };
        connectionThread.start();
    }

    @Override
    public void OnHttpRequestCallback(int poolOrder, JSONArray result) {
        returnedArraysFromConnections[poolOrder] = result;
        numberOfRetrievedResults++;

        if(numberOfRetrievedResults == connectionsOpened){//ALl opened connections are done
            JSONArray compiledArrays = new JSONArray();

            int mainIndex = 0;
            for(int i=0; i<returnedArraysFromConnections.length; i++){
                JSONArray subArr = returnedArraysFromConnections[i];
                for(int x=0; x<subArr.length(); x++){
                    try {
                        compiledArrays.put(mainIndex, subArr.get(x));
                        mainIndex++;
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                }
            }

            isBussyLoadingData = false;
            callback.onUpdateSatellitesFuturePositionCallback(compiledArrays);
        }
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


