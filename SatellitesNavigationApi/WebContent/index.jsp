<%@ page language="java" contentType="text/html; charset=UTF-8"
    pageEncoding="UTF-8"%>
<!DOCTYPE html>
<html>
<head>
<meta charset="UTF-8">
<title>Satellites navigation API manual</title>
	<style>
	.boldText{
		display:block;
		font-weight: bold;
	}
	.boldTextInline{
		color: #3e3e3e;
		display: inline-block;
		font-weight: bold;
	}
	body{
		max-width: 640px;
		margin: 8px auto; 
		margin-bottom: 500px;
	}
	html *{
	   font-family: HELVETICA !important;
	}
	#satelliteImage{
		display: block;
		margin: 32px auto;
	}
	h1{
		text-align:center;
		margin: 16px auto;
		margin-bottom: 128px;
	}
	.jsonCode{
		border:solid 1px #e8e8e8;
		background-color: #f5f5f5;
		font-family: Courier, Monaco, monospace;
		font-size: 14px;
		margin:8px;
		padding: 6px;
		word-break: break-all;
	}
	#servStatus{
		font-size: 32px;
		display: inline;
	}
	#errorMessage{
	font-size: 12px;
	color:red;
	display:inline;
	}
	</style>
</head>
<body>

	<jsp:useBean id="servStatusBean" 
	class="updateCoordBuffers.ServerStatusBean"
	scope="session">
	</jsp:useBean>

<h1>Satellites navigation API manual</h1>

<img src="satellite.png" id="satelliteImage" alt="Satellite image">

<br>

	<div>Status: <div id="servStatus"></div></div>

<h2>About</h2>
<p>
This API gives the current position of GALILEO satellites only. It uses the satellites ephemerides data to calculate their initial position, 
this data is downloaded from "igs.bkg.bund.de" through FTP. After the initial satellite position has been calculated it predicts it's future position up to the current UNIX time with "www.n2yo.com/api".
</p>


<h2>Functions</h2>

<p><div class="boldText">API Part 1: Retrieve available satellites:</div>
http://178.62.193.218:8080/SatellitesNavigationApi/retrieveAvailibleSatellites<br>
Retrieves a list of GALILEO satellites that are available from the API. The list is made up of their NORAD ID. A catalogue of satellites NORAD ID and svId can be found here: "https://celestrak.com/satcat/search.php".</p>

<div class="boldText">Example:</div>
<a href="http://178.62.193.218:8080/SatellitesNavigationApi/retrieveAvailibleSatellites">http://178.62.193.218:8080/SatellitesNavigationApi/retrieveAvailibleSatellites</a>

<br>
<p>Returns a JSON array of availible satellites:<br>
<div class="jsonCode">[40544,40128,40545,41859,41862,43564,41549,43565,41550,43055,43567,43057,41174,41175,40889]</div>

<p><div class="boldText">API part 2: Retrieve a satellites position:</div>
http://178.62.193.218:8080/SatellitesNavigationApi/retrieveSatellitePosition<br>
Retrieves a GALILEO satellite's positions based on its NORAD ID.<br>
<br>

<div class="boldText">Parameters:</div><br>
<div class="boldTextInline">NORADID</div>(Integer): Required. NORAD ID of target GALILEO satellite.<br>
<div class="boldTextInline">allPredictions</div>(true/false): Optional, default is true. Flag to specify if all future predictions should be sent or only the current time prediction.<br><br>


<div class="boldText">Example:</div>
<a href="http://178.62.193.218:8080/SatellitesNavigationApi/retrieveSatellitePosition?NORADID=40128">http://178.62.193.218:8080/SatellitesNavigationApi/retrieveSatellitePosition?NORADID=40128</a>

<br>
Returns a JSON object of available satellites. <br>
Root property "positions" is a hashed map and iterable array that contains the satellites latitude(lat), longitude(lon) and altitude(alt), in ellipsoidal coordinates. The key is the locations unix time.<br>
The second root property is "info" which is a JSON object which contains the property "NORADID" which is the returned satellites NORAD ID:<br>

<div class="jsonCode">
	{"positions":<br>
	"1556107754":{"alt":23236.33,"lon":-109.77517175,"lat":-49.83837784},<br>
	"1556107759":{"alt":23236.06,"lon":-110.87064839,"lat":-49.06048347},<br>
	"1556107760":{"alt":23236.06,"lon":-110.86562791,"lat":-49.06423165},<br>
	"1556107761":{"alt":23236.07,"lon":-110.86060644,"lat":-49.0679788}},<br>
	"info":{"NORADID":"40545"}}
</div>


<script>
	var serverStatus = <jsp:getProperty name="servStatusBean" property="serverStatus" />;
	var statusElement = document.getElementById("servStatus");
	statusElement.innerHTML = "●";
	if(serverStatus){
		statusElement.style.color = "green";
	}else{
		statusElement.style.color = "red";
		statusElement.innerHTML = statusElement.innerHTML+" <div id=\"errorMessage\">try again in a few minutes.</div>";
	}
	

</script>
</body>
</html>