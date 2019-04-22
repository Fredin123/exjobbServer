package updateCoordBuffers.downloadEphemerides;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.zip.GZIPInputStream;

import org.apache.commons.net.PrintCommandListener;
import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPReply;

import satellite.Satellite;
import staticFunctions.ParseRinex;

public class DownloadEphemerides {
	/*
	* Retrieves the RINEX Data of satellites
	* */
	//Url templete of where the satellite ephemerides should be downloaded
	private final static String host = "igs.bkg.bund.de";//"ftp://igs.bkg.bund.de";
	/*
	* RINEX File name description:
	* file:///home/elias/H%C3%A4mtningar/rinex303%20(6).pdf
	* A 1 RINEX File name description
	* */
	private final static String IGS_GALILEO_RINEX = "/IGS/BRDC/${yyyy}/${ddd}/BRDC00WRD_R_${yyyy}${ddd}0000_01D_EN.rnx.gz";
	
	
	public static final void ftpDownload(OnDownloadEphemeridesCallback callback, boolean oneDayBefore) {
		
	
		String substitutedStirng = IGS_GALILEO_RINEX;
	
	    int yearNumber = Calendar.getInstance().get(Calendar.YEAR);
	    int dayNumber = Calendar.getInstance().get(Calendar.DAY_OF_YEAR);
	    
	    if(oneDayBefore) {
	    	/*
			 * oneDayBefore is a check that is on if a previous ftp download failed. A possible reason for the 
			 * previous failed download could be that the uploaded file is in a diferent timezone, so while this
			 * program whats to download from day number 4, the ftp server might still be on day 3, so A simple solution 
			 * is to just go beck and check "a day" past.
			 * */
	    	dayNumber--;
	    	if(dayNumber < 0) {
	    		dayNumber = 365;
	    		yearNumber--;
	    	}
	    }
	    
	    String dayNumberString = Integer.toString(dayNumber);
	    if(dayNumber < 100){
	        dayNumberString = "0"+dayNumberString;
	    }
	
	    substitutedStirng = substitutedStirng.replaceAll("\\$\\{yyyy\\}", Integer.toString(yearNumber));
	    substitutedStirng = substitutedStirng.replaceAll("\\$\\{ddd\\}", dayNumberString);

	
	
		
		try {

            FTPClient ftpClient = new FTPClient();
            ftpClient.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out)));
            ftpClient.connect(host, 21);
            int responseCode = ftpClient.getReplyCode();
            if(!FTPReply.isPositiveCompletion(responseCode)){
                //Log.i("Project", "Negative completion"+Integer.toString(responseCode));
                ftpClient.disconnect();
            }
            ftpClient.login("Anonymous", "");
            ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
            ftpClient.enterLocalPassiveMode();

            String lastModifiedTime = ftpClient.getModificationTime(substitutedStirng);


            boolean loginResult = ftpClient.login("", "");
            System.out.println("Login result"+Boolean.toString(loginResult));


            ByteArrayOutputStream byteStream = new ByteArrayOutputStream();
            ftpClient.retrieveFile(substitutedStirng, byteStream);


            BufferedReader br = new BufferedReader(new InputStreamReader(new GZIPInputStream(new ByteArrayInputStream(byteStream.toByteArray()))));
            String response = "";
            String line;

            while((line = br.readLine()) != null){
            	//System.out.println(line);
                response += line+"\n";
            }

            ftpClient.logout();
            ftpClient.disconnect();



            ArrayList<Satellite> satellites = ParseRinex.parse(response);
            callback.OnDownloadEphemeridesCallback(satellites);

        } catch (IOException e) {
        	if(oneDayBefore == false) {
        		ftpDownload(callback, true);
        	}else {
        		/*our previous fix didn't work before so just cancel the function and hope the 
        		next time this function is called it'll be able to download the file*/
        		return;
        	}
        	
            //e.printStackTrace();
        }
	}
	
	
	

	
	
}
