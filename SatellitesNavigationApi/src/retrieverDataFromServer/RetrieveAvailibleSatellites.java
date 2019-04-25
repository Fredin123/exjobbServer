package retrieverDataFromServer;

import java.io.IOException;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.*;

import updateCoordBuffers.Main;
import updateCoordBuffers.SateliteCoordBuffer;


@WebServlet(name="RetrieveAvailibleSatellites", urlPatterns = {"/retrieveAvailibleSatellites"})
public class RetrieveAvailibleSatellites extends HttpServlet{
	
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		if(Main.startBufferDone == false) {
			response.setStatus(500);
			response.getWriter().println("Not ready");
			return;
		}
		JSONArray container = new JSONArray();
		
		Set<Integer> noradIds = SateliteCoordBuffer.keySet();
		
		for(int noradId : noradIds) {
			if(SateliteCoordBuffer.areTherePredictionsForThisSatellite(noradId)) {
				container.put(noradId);
			}
		}

		response.getWriter().println(container.toString());
		
		
	}
	
	
}