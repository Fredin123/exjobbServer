package httpRequest;

import org.json.*;

public interface OnHttpRequestCallback {
	void OnHttpRequestCallback(int poolOrder, JSONArray jsonArray);
}
