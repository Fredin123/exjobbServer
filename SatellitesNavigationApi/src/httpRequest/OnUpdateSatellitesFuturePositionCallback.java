package httpRequest;

import java.util.ArrayList;

import org.apache.commons.math3.util.Pair;
import org.json.*;

public interface OnUpdateSatellitesFuturePositionCallback {
	void onUpdateSatellitesFuturePositionCallback(ArrayList<Pair<Long, JSONArray>> returnedData);
}
