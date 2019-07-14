import com.google.api.client.json.Json;
import com.google.gson.Gson;
import com.google.gson.JsonObject;
import jdk.nashorn.internal.parser.JSONParser;
import org.json.JSONObject;

import java.awt.color.ProfileDataException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.ServerSocket;
import java.net.Socket;

public class Server extends Thread {

	ServerSocket serverSocket;
	Socket socket;
	int port;

	public Server(int port)
	{
		this.port = port;
	}

	@Override
	public void run()
	{
		try
		{
			WebServer s = new WebServer(this.port);
			s.setReuseAddr(true);
			s.start();
		}
		catch (IOException e)
		{
			e.printStackTrace();
		}
	}
}
