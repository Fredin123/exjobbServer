package retrieverDataFromServer;

import java.io.IOException;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.annotation.WebServlet;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import org.json.*;
import updateCoordBuffers.SateliteCoordBuffer;


@WebServlet(name="RetrieveAvailibleSatellites", urlPatterns = {"/retrieveAvailibleSatellites"})
public class RetrieveAvailibleSatellites extends HttpServlet{
	
	
	@Override
	protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		JSONArray container = new JSONArray();
		
		Set<Integer> noradIds = SateliteCoordBuffer.keySet();
		
		for(int noradId : noradIds) {
			container.put(Integer.toString(noradId));
		}

		response.getWriter().println(container.toString());
		
		
	}
	
	
}