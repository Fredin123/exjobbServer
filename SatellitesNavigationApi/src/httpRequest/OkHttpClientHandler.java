package httpRequest;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import okhttp3.Interceptor;
import okhttp3.Interceptor.Chain;
import okhttp3.OkHttpClient;
import okhttp3.Response;

public class OkHttpClientHandler {
	private static OkHttpClient httpClient;
	
	static public OkHttpClient getHttpClient() {
		if(httpClient == null) {
			OkHttpClient.Builder builder = new OkHttpClient.Builder();
			builder.connectTimeout(45, TimeUnit.SECONDS);
			builder.readTimeout(45, TimeUnit.SECONDS);
			builder.writeTimeout(45, TimeUnit.SECONDS);
			

			httpClient = builder.build();
		}
		return httpClient;
	}
	

}
