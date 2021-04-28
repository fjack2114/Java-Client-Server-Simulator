import java.io.*;
import java.net.Socket;
import java.util.ArrayList;

public class Client {
	private static Socket socket = null;
	private final DataOutputStream outStream;
	private final BufferedReader inStream;
	private static final String HELO = "HELO\n";
	private static final String AUTH = "AUTH " + System.getProperty("user.name") + "\n";
	private static final String REDY = "REDY\n";
	private static final String NONE = "NONE\n";
	private static final String QUIT = "QUIT\n";
	private static final String OK = "OK\n";
	private static final String JOBN = "JOBN";
	private static final String JCPL = "JCPL";
	private static final String DATA = "DATA";
//	private String serverData = "";
	private String jobID = "";
	private String incomingMessage = inMessage();

// GETS : serverType  serverID  state  curStartTime  core  mem  disk  #wJobs  #rJobs
//		  [0]		  [1]		[2]	   [3]		     [4]   [5]  [6]   [7]     [8]

// DATA : DATA  nRecs  recLen
// 		  [0]	[1]	   [2]
//
//				String[] splitStr = incomingMessage.split("\\s+");
//				private static List<String> serverInfo = new ArrayList<>();
//				String[] splitStr =  serverData.split;
//
//				serverInfo.add()

	public static void main(String[] args) throws IOException {
		Client client = new Client("127.0.0.1", 50000);
		client.eventLoop();
	}

	private static void connect(String address, int port) {
		try {
			System.out.println("Connecting to server...");
			socket = new Socket(address, port);
			System.out.println("Connection Established");
		} catch (IOException e) {
			e.printStackTrace();
		}
	}
	
	public Client(String address, int port) throws IOException {
		connect(address, port);
		outStream = new DataOutputStream(socket.getOutputStream());
		inStream = new BufferedReader(new InputStreamReader(socket.getInputStream()));
	}

	private void eventLoop() throws FileNotFoundException {
		// "HELO" to initiate connection
		outMessage(HELO);
		inMessage();

		// Authentication information
		outMessage(AUTH);
		inMessage();

//		// Read ds-system information for serverList
		ArrayList<Server> serverList;
		serverList = XML.parse("ds-system.xml");

		// Confirm to Server that Client is ready
		outMessage(REDY);

		int largestServer = mostCores(serverList);

		// If there are no jobs, then quit
		if (incomingMessage.contains(NONE)) {
			outMessage(QUIT);
		}

		// Event loop to handle incoming jobs
		while (!incomingMessage.contains(NONE)) {
			if (incomingMessage.contains(JOBN)) {
				String[] splitStr = incomingMessage.split("\\s+");
				jobID = splitStr[2];
				outMessage(getsCapable(incomingMessage));
			}
			if (incomingMessage.contains(DATA)) {
				outMessage(OK);
			}
			if (incomingMessage.contains(OK)) {
				outMessage(REDY);
			}
			if (incomingMessage.contains(JCPL)) {
				outMessage(REDY);
			}
			if (incomingMessage.contains(".")) {
				outMessage(availableServer(serverList.get(largestServer)));
			}
			if(incomingMessage.contains("joon")) {
				outMessage(OK);
			}
			incomingMessage = inMessage();
		}

		// If all jobs are completed sent quit to Server
		outMessage(QUIT);

		try {
			if (inMessage().contains(QUIT)) {
				outStream.close();
				socket.close();
			}
		} catch (IOException ignored) {
		}
		System.exit(1);
	}

	private void outMessage(String str) {
		// Client to Server messages
		byte[] byteMsg = str.getBytes();
		try {
			outStream.write(byteMsg);
		} catch (IOException e) {
			e.printStackTrace();
		}

		// Print Client output to screen
		System.out.print("Client: " + str);
	}

	private String inMessage() {
		// Server to Client messages
		String str;
		char[] charbuf = new char[65535];
		try {
			if (inStream != null) {
				inStream.read(charbuf);
			}
		} catch (IOException e) {
			e.printStackTrace();
		}
		str = new String(charbuf, 0, charbuf.length);

		// Print Server input to screen
		System.out.print("Server: " + str);

		return str;
	}

	private String availableServer(Server s) {
		return "SCHD " + jobID + " " + s.getType() + " " + "0" + "\n";
	}

	private String getsCapable(String job) {
		String[] splitStr = job.split("\\s+");
		return "GETS Capable " + splitStr[4] + " " + splitStr[5] + " " + splitStr[6] + "\n";
	}

	// Gets first server with the highest number of cores
	private int mostCores(ArrayList<Server> s) {
		int highestID = 0;
		if (s.size() > 0) {
			for (int i = 0; i < s.size(); i++) {
				if (s.get(i).getCores() > s.get(highestID).getCores())
					highestID = i;
			}
				return highestID;
			}
		return 0;
	}
}