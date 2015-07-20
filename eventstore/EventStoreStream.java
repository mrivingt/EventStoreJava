package eventstore;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

import org.json.simple.JSONArray;
import org.json.simple.JSONObject;
import org.json.simple.parser.JSONParser;
import org.json.simple.parser.ParseException;

public class EventStoreStream {
	
	
		
		private String stream;
		private JSONObject payload;
		private boolean result;
		private String data;
		private String error;
		private String mimetype;
		
		public EventStoreStream (String stream) throws IOException {
			this.stream = stream;
			this.mimetype = "application/json";
			
		}
		
		public void getHeadofStream () throws IOException {
			// Start at the head of the stream
		
			this.payload = extract(getURL(stream));
			
			if (payload.containsKey("headOfStream")) {
			
				if (payload.get("headOfStream").equals(true)) {
					String last = getLink(payload,"last");
						if (!last.equals("")) { 
							this.payload = extract(getURL(last));
							result = true;
						}
						else {
						result = false;
						error = stream + " - unable to locate last uri";
						}
				}
				else {
					result = false;
					error = stream + " - is not head of stream";
				}
			}
			else {
				result = false;
				error = stream + " - is not head of stream";
		
			}
		}
		
		public String getErrorMessage() {
		 return this.error;
		}
		
		public boolean dataExists() {
			if (!this.data.equals("")) {return true;}
			else {return false;}
		}
		
		public String getData() {
			return this.data;
		}
		
		public void gotoPrevious(String previous) throws IOException {
				this.payload = extract(getURL(previous));
		}
		
		public String getPrevious() {
			return getLink(this.getPayLoad(),"previous");
		}
		
		public void extractDataFromPayload() {
			this.data = getTheData(this.payload);
		}
		
		private String getTheData(JSONObject payload) {
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
		public boolean getResult() {
			return this.result;
		}
		
		private String getLink(JSONObject payload, String linkType) {
			
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
		
		private String getURL(String url) throws IOException {	
			
			//System.out.print("\nGoing to get: " + url);
			int responseCode = 0;

			URL urlObj = new URL(url);
			HttpURLConnection con = (HttpURLConnection) urlObj.openConnection();
			con.setRequestMethod("GET");
			//con.setRequestProperty("Accept-Language", "en-US,en;q=0.5");
			con.setRequestProperty("Accept" , this.mimetype );
			 
			responseCode = con.getResponseCode();
			//System.out.print("\nGET response code: " + responseCode);

			if (responseCode != 200) {
				if (responseCode == 406) {System.out.print("\nhttp response 406: unacceptable content type specified: " + this.mimetype);}
				if (responseCode == 404) {System.out.print("\nhttp response 404: unable to locate stream: " + url);}
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
		
		private JSONObject extract(String response) {
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

		public JSONObject getPayLoad() {
			
			return this.payload;
		}

		
	}
