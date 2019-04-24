package retrieverDataFromServer;

import java.io.IOException;
import java.util.TreeMap;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.json.*;

import satellite.Satellite;
import updateCoordBuffers.Main;
import updateCoordBuffers.SateliteCoordBuffer;

@WebServlet(name="RetrieveSatelliteData", urlPatterns = {"/retrieveSatellitePosition"})
public class RetrieveSatelliteData extends HttpServlet{
	
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if(Main.startBufferDone == false) {
			response.getWriter().println("Not ready");
			return;
		}
		long currentUnixTime = System.currentTimeMillis() / 1000L;
		
		String requestNoradId = request.getParameter("NORADID");
		//System.out.println("requestNoradId: "+requestNoradId);
		if(requestNoradId == null) {
			response.getWriter().println("500");
			return;
		}
		String returnAllFuturePredictions = request.getParameter("allPredictions");
		if(returnAllFuturePredictions == null) {
			returnAllFuturePredictions = "true";
		}
		
		final int predictionsLimit = 100;
		int predictionsCounterTest = 0;

		JSONObject container = new JSONObject();
		JSONObject infoObj = null;
		JSONObject positionsObj = new JSONObject();
		

		while(predictionsCounterTest < predictionsLimit) {
			if(SateliteCoordBuffer.getSatelliteDataFromAllBuffers(Integer.parseInt(requestNoradId), currentUnixTime+predictionsCounterTest) != null) {
				Satellite s = SateliteCoordBuffer.getSatelliteDataFromAllBuffers(Integer.parseInt(requestNoradId), currentUnixTime+predictionsCounterTest);
				if(infoObj == null) {
					infoObj = new JSONObject();
					positionsObj = new JSONObject();
					infoObj.put("NORADID", Integer.parseInt(requestNoradId));
				}
				JSONObject position = new JSONObject();
				position.put("lat", s.getLatitude());
				position.put("lon", s.getLongitude());
				position.put("alt", s.getAltitude());
				
				positionsObj.put(Long.toString(currentUnixTime+predictionsCounterTest), position);
				if(returnAllFuturePredictions.equals("false")) {
					break;
				}
			}else if(predictionsCounterTest == 0){
				//If the first position was not found. then we we don't want to send back only future predictions as the server is not done loading so we quit instead
				break;
			}
			
			predictionsCounterTest++;
		}
		
		
		container.put("info", infoObj);
		container.put("positions", positionsObj);
		
		response.getWriter().println(container.toString());
		
	}
	
	
}
