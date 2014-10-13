package me.benbrewer.tools;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;

/*
	HotSwap

	@author Ben Brewer <benbrewer@dswinc.com>

	@description
		A very simple, fast and static hot swap on a running JVM

	@notes
		All methods are set to throw, as these are intended to get caught as an exit code by a running application
		Limited catching is done, as there is not much we want to hide (this should fail hard and fast)
 */

public class HotSwap {
	private static String            host;
	private static String            port;
	private static String            path;
	private static ArrayList<String> fileNames;

	public static void main( String[] args ) throws Exception {
		HotSwapHelper hotSwapHelper = new HotSwapHelper();
		try {
			// check properties
			HotSwap.checkProperties();

			// check arguments
			HotSwap.checkArguments( args );

			// connect to the VM of interest
			hotSwapHelper.connect( HotSwap.host, HotSwap.port );

			// process the files
			for( String fileName : HotSwap.fileNames ) {
				File classFile = HotSwap.getClassFile( fileName );
				String className = HotSwap.getClassName( fileName );
				hotSwapHelper.replace( classFile, className );
			}
		} finally {
			hotSwapHelper.disconnect();
		}
	}

	private static void checkProperties() {
		// get the actual application settings
		HotSwap.host = System.getProperty( "host" );
		HotSwap.port = System.getProperty( "port" );
		HotSwap.path = System.getProperty( "path" );

		// broken out for clarity
		if( !( HotSwap.host instanceof String ) ) { throw new RuntimeException( "missing host property. please set as -Dhost" ); }
		if( !( HotSwap.port instanceof String ) ) { throw new RuntimeException( "missing port property. please set as -Dport" ); }
		if( !( HotSwap.path instanceof String ) ) { throw new RuntimeException( "missing path property. please set as -Dpath" ); }
	}

	private static void checkArguments( String[] arguments ) {
		// check the files string to make sure it's okay
		if( arguments.length < 1 ) { throw new IllegalArgumentException( "missing file path arguments. must be relative to -Dpath" ); }

		// get the arguments as files
		HotSwap.fileNames = new ArrayList<String>( Arrays.asList( arguments ) );
	}

	private static File getClassFile( String fileName ) {
		//  just a simple init that will throw if the file doesn't actually exist
		return new File( HotSwap.path + File.separator + fileName );
	}

	// TODO - make this smarter - perhaps it could do a quick search for a corresponding java file and parse out the package
	private static String getClassName( String fileName ) {
		// take the .class off
		String className = fileName.substring( 0, fileName.length() - 6 );

		// turn any path separators into the dot
		return className.replace( File.separator, "." );
	}
}
