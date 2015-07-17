package eventstore;
import java.net.Authenticator;

import java.net.PasswordAuthentication;


abstract class Defaults {

	public static final String server = "127.0.0.1";
	public static final String port = "2113";
	public static final String stream = "/streams/account-11";
	public static final String user = "admin";
	public static final String password = "changeit";
	public static final int sleeptime = 5000; // milliseconds
	public static final String embed = "";
	public static final String url = "http://" + server + ":" + port + stream + embed;
	public static final int idleDotsPerLine = 80;
}

class MyAuthenticator extends Authenticator {

    public PasswordAuthentication getPasswordAuthentication () {
        return new PasswordAuthentication (Defaults.user, Defaults.password.toCharArray());
    }
}
		
public class eventStore {
	
	public static void main(String[] args) throws Exception {

	// Start at the head of the stream
	System.out.print("\nStarting point: " + Defaults.url);

	EventStoreStream myEventStream = new EventStoreStream(Defaults.url);
	
	myEventStream.getHeadofStream(); // payload is now set
	if (!myEventStream.getResult()) {
		System.out.print("\nGet Head of Stream failed");
		System.exit(1);
	}
	myEventStream.extractDataFromPayload();
	if (myEventStream.dataExists()) {
		System.out.print(myEventStream.getData());
	}
	
	int idleCount = 0;
	
	do {
		myEventStream.gotoPrevious();
		String newLine = "\n";
		
		do {
			myEventStream.extractDataFromPayload();
			if (myEventStream.dataExists()) {
				System.out.print(myEventStream.getData());
			}
			else {
				
				if ((idleCount % Defaults.idleDotsPerLine) == 0) {newLine = "\n";}
				System.out.print(newLine + ".");
				idleCount++;
				newLine = "";
		
				Thread.sleep(Defaults.sleeptime);}
			
		} while (!myEventStream.dataExists());
		
		
	} while (!myEventStream.getPrevious().equals(""));
	
 }	
	
	


}



