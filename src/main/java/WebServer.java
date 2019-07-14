import com.google.common.collect.Lists;
import com.google.gson.Gson;
import org.java_websocket.WebSocket;
import org.java_websocket.handshake.ClientHandshake;
import org.java_websocket.server.WebSocketServer;
import java.net.InetSocketAddress;
import java.net.UnknownHostException;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.ByteBuffer;

public class WebServer extends WebSocketServer {

	public WebServer( int port ) throws UnknownHostException
	{
		super( new InetSocketAddress( port ) );
	}

	public WebServer( InetSocketAddress address ) {
		super( address );
	}

	@Override
	public void onOpen(WebSocket conn, ClientHandshake handshake ) {
		conn.send("Welcome to the server!"); //This method sends a message to the new client
		broadcast( "new connection: " + handshake.getResourceDescriptor() ); //This method sends a message to all clients connected
	}

	@Override
	public void onClose( WebSocket conn, int code, String reason, boolean remote ) {
		broadcast( conn + " has left the room!" );
	}

	@Override
	public void onMessage( WebSocket conn, String message ) {
		broadcast( message );
		Gson g = new Gson();

		SourceState[] p = g.fromJson(message, SourceState[].class);

		for (SourceState d: p)
		{

			DataSourceWrapper dataSourceWrapper = Producer.dataSourceHashMap.get(d.getDataSourceName());

			if(dataSourceWrapper.isRunning() && !d.isRunning())
			{
				// setting flag as specified in the json
				dataSourceWrapper.setRunning(d.isRunning());

			}
			else if(!dataSourceWrapper.isRunning() && d.isRunning())
			{
				if(d.getDataSourceName().equals("twitter"))
				{
					System.out.println(d.getTag());
					TwitterMessages.RESET = true;
					TwitterMessages.keywords = Lists.newArrayList(d.getTag());
				}

				synchronized (dataSourceWrapper)
				{
					System.out.println("Restarting " + dataSourceWrapper.getDataSource());
					dataSourceWrapper.setRunning(d.isRunning());
					dataSourceWrapper.notify();
				}
			}
			else if(
					d.isRunning()
					&& dataSourceWrapper.isRunning()
					&& d.getDataSourceName().equals("twitter")
					&& !TwitterMessages.keywords.get(0).equals(d.getTag())
			)
			{

				TwitterMessages.RESET = true;
				TwitterMessages.keywords = Lists.newArrayList(d.getTag());
			}
		}
	}
	@Override
	public void onMessage( WebSocket conn, ByteBuffer message ) {
		broadcast( message.array() );
	}


	@Override
	public void onError( WebSocket conn, Exception ex ) {
		ex.printStackTrace();
		if( conn != null ) {
			// some errors like port binding failed may not be assignable to a specific websocket
		}
	}

	@Override
	public void onStart() {
		System.out.println("Server started!");
		setConnectionLostTimeout(0);
		setConnectionLostTimeout(100);
	}

}
