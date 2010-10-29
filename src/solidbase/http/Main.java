package solidbase.http;

import java.net.ServerSocket;
import java.net.Socket;

public class Main
{
	/**
	 * @param args
	 */
	public static void main( String[] args )
	{
		try
		{
			ServerSocket server = new ServerSocket( 80 );
			while( true )
			{
				Socket socket = server.accept();
				Handler handler = new Handler( socket );
				handler.start();
			}
		}
		catch( Throwable t )
		{
			t.printStackTrace( System.err );
		}
	}
}
