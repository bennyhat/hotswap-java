package me.benbrewer.tools;

import java.io.DataInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.sun.jdi.*;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;

/*
	HotSwapHelper

	@author David A. Kavanagh <dak@dotech.com>
	@contributor Ben Brewer <benbrewer@dswinc.com>

	@description
		Adapted from code provided by https://code.google.com/p/hotswap
		Provides basic connection tools to connect to a Java VM that is listening on a debug port

	@notes
		All methods are set to throw, as these are intended to get caught as an exit code by a running application
		Limited try catching is used to improve performance
 */

public class HotSwapHelper {
	private VirtualMachine virtualMachine;

	public HotSwapHelper() {
	}

	public void connect( String host, String port ) throws Exception {
		// find the connector to use
		AttachingConnector connector = this.getAttachingConnector();

		// configure it using the passed in host and port
		Map connectorArguments = this.setupConnectorArguments( host, port, connector );

		// connect to the virtual machine
		this.virtualMachine = connector.attach( connectorArguments );

		// validate if we can even use it or not
		this.checkCapabilities();
	}

	public void disconnect() throws Exception {
		// no throwing here, as this is expected to be a last resort
		try {
			if( this.virtualMachine != null ) {
				this.virtualMachine.dispose();
			}
		} catch( RuntimeException exception ) {
			System.err.println( "unable to disconnect from VM: " + exception.getMessage() );
		}
	}

	private void checkCapabilities() throws Exception {
		if( !this.virtualMachine.canRedefineClasses() ) {
			throw new IllegalStateException( "doesn't support class replacement" );
		}
	}

	private Map setupConnectorArguments( String host, String port, AttachingConnector connector ) {
		Map connectorArguments = connector.defaultArguments();
		Connector.Argument connectorHost;
		Connector.Argument connectorPort;

		// use port if using dt_socket
		connectorPort = (Connector.Argument)connectorArguments.get( "hostname" );
		connectorPort.setValue( host );
		connectorHost = (Connector.Argument)connectorArguments.get( "port" );
		connectorHost.setValue( port );
		return connectorArguments;
	}

	private AttachingConnector getAttachingConnector() {
		VirtualMachineManager manager = Bootstrap.virtualMachineManager();
		List connectors = manager.attachingConnectors();
		AttachingConnector connector = null;

		for( Object baseConnector : connectors ) {
			AttachingConnector pendingConnector = (AttachingConnector)baseConnector;
			if( pendingConnector.transport().name().equals( "dt_socket" ) ) {
				connector = pendingConnector;
				break;
			}
		}
		if( connector == null ) {
			throw new IllegalStateException( "cannot find a socket connector" );
		}
		return connector;
	}

	public void replace( File classFile, String className ) throws Exception {
		// load class(es)
		byte[] fileBytes = this.loadClassFile( classFile );
		// redefine in JVM
		List classes = this.virtualMachine.classesByName( className );

		// if the class isn't loaded on the VM, can't do the replace.
		if( classes == null || classes.size() == 0 ) {
			throw new IllegalStateException( "no loaded class to swap for " + className );
		}

		// go through the matching classes and swap out their contents
		for( Object baseType : classes ) {
			ReferenceType referenceType = (ReferenceType)baseType;
			HashMap map = new HashMap();
			map.put( referenceType, fileBytes );
			this.virtualMachine.redefineClasses( map );
		}
	}

	private byte[] loadClassFile( File classFile ) throws IOException {
		DataInputStream fileInput = new DataInputStream( new FileInputStream( classFile ) );

		byte[] fileBytes = new byte[(int)classFile.length()];
		fileInput.readFully( fileBytes );
		fileInput.close();

		return fileBytes;
	}
}
