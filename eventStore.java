

import java.net.HttpURLConnection;
//import java.net.MalformedURLException;
import java.net.URL;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Authenticator;
import java.net.PasswordAuthentication;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

abstract class Defaults {

	public static final String server = "127.0.0.1";
	public static final String port = "2113";
	public static final String stream = "/streams/account-11";
	public static final String user = "admin";
	public static final String password = "changeit";
	public static final int sleeptime = 5000; // milliseconds
	public static final String embed = "";
	public static final String mimetype = "application/json";
	public static final String url = "http://" + server + ":" + port + stream + embed;
}
		
public class eventStore {
	
	public static void main(String[] args) throws Exception {

	// Start at the head of the stream
	System.out.print("\nStarting point: " + Defaults.url);
	
	JSONObject payload = extract(getURL(Defaults.url));

	if (payload.get("headOfStream").equals(true)) {
		String last = getLink(payload,"last");
			if (!last.equals("")) { 
			payload = extract(getURL(last));
		}
	}

	String data = getTheData(payload);
	if (!data.equals("")) {System.out.print(data);}
	
	String previous = "";
	int idleCount = 0;
	do {
		previous = getLink(payload,"previous");

		data = "";
		String newLine = "\n";
		do {
			payload = extract(getURL(previous));
			data = getTheData(payload);
			if (!data.equals( "")) {
				System.out.print(data);
				newLine = "\n";
				idleCount = 0;}
			else {
				if ((idleCount % 80) == 0) {newLine = "\n";}
				System.out.print(newLine + ".");
				idleCount++;
				newLine = "";
				Thread.sleep(Defaults.sleeptime);
			}
		} while (data.equals(""));
	
	} while(!previous.equals("")); 
	
   }	
	
	private static String getTheData(JSONObject payload) {
		// Now we are getting the pages of data
		// The data items are in an array of links within an array of entries 
		
		JSONArray entries = (JSONArray) payload.get("entries");
		//System.out.println("Number of entries: " + entries.size());
		
		String responseData = "";
		String entryResponse = "";
		if (entries.size() == 0) {responseData = "";}
		else {
			for (int i=entries.size()-1;i>-1;i--) {
				//	for (int i=0;i<entries.size();i++) {
				JSONObject entry = (JSONObject) entries.get(i);
				//System.out.println(entry.toString());
				JSONArray entryLinks = (JSONArray) entry.get("links");
				responseData = responseData + "\n" + entry.get("title") + "\t" + entry.get("summary");
	      
				for (int j=0;j<entryLinks.size(); j++) {
					JSONObject entryLink = (JSONObject) entryLinks.get(j);
					if (entryLink.get("relation").equals("alternate")) {
						//System.out.println(entryLink.get("uri"));

						try {
							entryResponse = getURL(entryLink.get("uri").toString());
						} catch (IOException e) {
						  e.printStackTrace();
						}
						JSONObject entryPayload = extract(entryResponse);
						responseData = responseData + "\t" + entryPayload.toString();
					}
				}
			}
		}
		return responseData;
	}
	
	private static String getLink(JSONObject payload, String linkType) {
		
		JSONArray links = (JSONArray) payload.get("links");
		String responseURI = "";
		
		for (int i=0;i<links.size();i++) {
			JSONObject link = (JSONObject) links.get(i);
			if (link.get("relation").toString().equals(linkType)) {
				responseURI = link.get("uri").toString();
			}
		}
		return responseURI;
	}
	
	private static String getURL(String url) throws IOException {	
		
		//System.out.print("\nGoing to get: " + url);
		int responseCode = 0;

		URL urlObj = new URL(url);
		HttpURLConnection con = (HttpURLConnection) urlObj.openConnection();
		con.setRequestMethod("GET");
		//con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
		con.setRequestProperty("Accept" , Defaults.mimetype );
		 
		responseCode = con.getResponseCode();
		//System.out.print("\nGET response code: " + responseCode);

		if (responseCode != 200) {
			if (responseCode == 406) {System.out.print("\nhttp response 406: unacceptable content type specified: " + Defaults.mimetype);}
			if (responseCode == 404) {System.out.print("\nhttp response 404: unable to locate stream: " + Defaults.stream);}
			System.exit(responseCode);  
		}

		BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()));
		String inputLine, response = "";
		
	  	while ((inputLine = in.readLine()) != null) {
	  		response = response + inputLine;
			//System.out.println(inputLine);
		}
	  	
		in.close();
	  	return response;
		}
	
	private static JSONObject extract(String response) {
        JSONParser parser = new JSONParser();
        JSONObject payload = null;

        try {
            payload = (JSONObject) parser.parse(response);
        } catch (ParseException e) {
            System.out.println("Input file has JSON parse error: " + e.getPosition() + " "
                  + e.toString());
            System.exit(4);
        }
        return payload;
	}

	class MyAuthenticator extends Authenticator {

        public PasswordAuthentication getPasswordAuthentication () {
            return new PasswordAuthentication (Defaults.user, Defaults.password.toCharArray());
        }
    }
}

