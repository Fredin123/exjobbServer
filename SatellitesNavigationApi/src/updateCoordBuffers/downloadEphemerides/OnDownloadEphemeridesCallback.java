package updateCoordBuffers.downloadEphemerides;

import java.util.ArrayList;

import satellite.Satellite;

public interface OnDownloadEphemeridesCallback {
	void OnDownloadEphemeridesCallback(ArrayList<Satellite> satellites);
}
