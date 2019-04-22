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
import updateCoordBuffers.SateliteCoordBuffer;

@WebServlet(name="RetrieveSatelliteData", urlPatterns = {"/retrieveSatellitePosition"})
public class RetrieveSatelliteData extends HttpServlet{
	
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		long currentUnixTime = System.currentTimeMillis() / 1000L;
		
		String requestNoradId = request.getParameter("NORADID");
		System.out.println("requestNoradId: "+requestNoradId);
		if(requestNoradId == null) {
			response.getWriter().println("500");
			return;
		}
		String returnAllFuturePredictions = request.getParameter("allPredictions");
		if(returnAllFuturePredictions == null) {
			returnAllFuturePredictions = "true";
		}
		
		final int predictionsLimit = 300;
		int predictionsCoutner = 0;

		JSONObject container = new JSONObject();
		JSONObject infoObj = null;
		JSONObject positionsObj = new JSONObject();
		

		long unixTimeTest = currentUnixTime;
		while(SateliteCoordBuffer.getSatelliteDataFromAllBuffers(Integer.parseInt(requestNoradId), unixTimeTest) != null) {
			Satellite s = SateliteCoordBuffer.getSatelliteDataFromAllBuffers(Integer.parseInt(requestNoradId), unixTimeTest);
			if(infoObj == null) {
				infoObj = new JSONObject();
				positionsObj = new JSONObject();
				infoObj.put("NORADID", requestNoradId);
			}
			JSONObject position = new JSONObject();
			position.put("lat", s.getLatitude());
			position.put("lon", s.getLongitude());
			position.put("alt", s.getAltitude());
			
			positionsObj.put(Long.toString(unixTimeTest), position);
			
			unixTimeTest++;
			predictionsCoutner++;
			if(predictionsCoutner >= predictionsLimit) {
				break;
			}
			if(returnAllFuturePredictions.equals("false")) {
				break;
			}
		}
		
		container.put("info", infoObj);
		container.put("positions", positionsObj);
		
		response.getWriter().println(container.toString());
		
	}
	
	
}
