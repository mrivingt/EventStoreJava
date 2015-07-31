package eventstore;
import java.io.IOException;
import java.net.Authenticator;

import java.net.PasswordAuthentication;
import java.util.concurrent.CountDownLatch;

import org.json.simple.JSONObject;

abstract class Defaults {

	public static final String server = "127.0.0.1";
	public static final String port = "2113";
	//public static final String stream = "/streams/account-32";
	public static final String stream = "/streams/markStream3";
	public static final int writes = 1000000;
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
		

//Event Pump; pump out lots of events
class EventStoreWriter implements Runnable {
	Thread t;
	int myId;
	CountDownLatch myLatch;
	int myRepeats;
	int myMinimumResponse;
	int myCount = 0;
	int myUniqueKey;
	//boolean myUnique;
	boolean myStop = false;
	boolean myDummy = false;

	EventStoreWriter(CountDownLatch latch) {
		// Create a new event store writer
		t = new Thread(this, "Event Store Writer");
		myLatch = latch;

	}

	// This is the entry point for the thread
	public void run() {
		
		JSONObject testPayload = new JSONObject();
		long then = System.currentTimeMillis();
		EventStoreStream myEventStream = null;
		try {
			myEventStream = new EventStoreStream(Defaults.url);
		} catch (IOException e1) {
			// TODO Auto-generated catch block
			e1.printStackTrace();
		}
		
		for (int i = Defaults.writes; i > 0 ; i--) {
			// Let's write to the stream before we read it
			
			//System.out.print("\nI am here");
			
			long now = System.currentTimeMillis();
			testPayload.put( "time", String.valueOf(now));
			testPayload.put( "value", now-then);
			testPayload.put("source", "DellXPS");
			try {
				myEventStream.writeToStream(Defaults.url, "mark", testPayload);
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
				System.exit(1);
			}
			then = now;
	
		}

	    try {
	    	  myLatch.countDown();
	    	 } catch (Exception e) {
	    		 e.printStackTrace();
	    	 }
	   }
      
}


public class eventStore {
	
	public static void main(String[] args) throws Exception {

		
	// Start at the head of the stream
	System.out.print("\nStarting point: " + Defaults.url);

	EventStoreStream myEventStream = new EventStoreStream(Defaults.url);
	
	CountDownLatch latch = new CountDownLatch(1);
	EventStoreWriter writerThread = new EventStoreWriter(latch);
	writerThread.t.start();
	
	Thread.sleep(5000); // Let the writer get started
	
	myEventStream.getHeadofStream(); // payload is now set
	if (!myEventStream.getResult()) {
		System.out.print("\nGet Head of Stream failed");
		System.out.print("\n" + myEventStream.getErrorMessage());
		System.exit(1);
	}
	myEventStream.extractDataFromPayload();
	if (myEventStream.dataExists()) {
		System.out.print(myEventStream.getData());
	}
	
	int idleCount = 0;
	
	String previous = "";
	do {
	
		previous = myEventStream.getPrevious();
		
		String newLine = "\n";
		
		do {
			myEventStream.gotoPrevious(previous);
			myEventStream.extractDataFromPayload();
			if (myEventStream.dataExists()) {
				System.out.print(myEventStream.getData());
			}
			else {
				
				if ((idleCount % Defaults.idleDotsPerLine) == 0) {newLine = "\n";}
				System.out.print(newLine + ".");
				idleCount++;
				newLine = "";
				Thread.sleep(Defaults.sleeptime);
				}
			
		} while (!myEventStream.dataExists());
		
		
	} while (!previous.equals(""));
	
 }	

}



