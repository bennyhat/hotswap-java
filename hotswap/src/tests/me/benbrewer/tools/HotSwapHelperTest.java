package me.benbrewer.tools;

import com.sun.jdi.ReferenceType;
import com.sun.jdi.VirtualMachine;
import com.sun.jdi.VirtualMachineManager;
import com.sun.jdi.connect.AttachingConnector;
import com.sun.jdi.connect.Connector;
import com.sun.tools.jdi.SharedMemoryAttachingConnector;
import com.sun.tools.jdi.SocketAttachingConnector;
import junit.framework.Assert;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.mockito.ArgumentMatcher;
import org.powermock.modules.junit4.PowerMockRunner;

import static org.mockito.Mockito.*;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Map;

/*
	HotSwapHelperTest

	@contributor Ben Brewer <benbrewer@dswinc.com>

	@description
		The purpose of these tests is to verify that the helper is doing what it was intended to do
		which is: perform as a helper for interacting with a virtualmachine object and other JDI components
		necessary to get this virtualmachine.

		Some effort has been put forward to reduce coupling. However, as this class is intended to work with JDI
		components, these are mocked freely.
 */

@RunWith(PowerMockRunner.class)
public class HotSwapHelperTest {

	@Rule
	TemporaryFolder classFolder;

	HotSwapHelper         subject;
	VirtualMachine        virtualMachine;
	VirtualMachineManager virtualMachineManager;
	ReferenceType         foundClass;

	byte[]             classFileBytes;
	AttachingConnector connector;

	@Before
	public void setup() throws Exception {
		// mocks
		connector = mock( AttachingConnector.class );
		virtualMachine = mock( VirtualMachine.class );
		virtualMachineManager = mock( VirtualMachineManager.class );
		foundClass = mock( ReferenceType.class );
		classFileBytes = new byte[1];

		// subject and real objects
		subject = new HotSwapHelper();
		classFolder = new TemporaryFolder();
		classFolder.create();

		// private sets
		TestUtilities.setPrivateField( subject, "virtualMachine", virtualMachine );
		TestUtilities.setPrivateField( subject, "virtualMachineManager", virtualMachineManager );
	}

	@Test
	public void connectAttachesToValidVirtualMachine() throws Exception {
		// variables and mocks
		HotSwapHelper mockSubject = spy( subject );
		AttachingConnector connector = new SocketAttachingConnector();
		AttachingConnector mockConnector = spy( connector );
		String host = "whinko";
		String port = "whanko";

		doReturn( true ).when( virtualMachine ).canRedefineClasses();
		doReturn( mockConnector ).when( mockSubject ).getAttachingConnector();
		doReturn( virtualMachine ).when( mockConnector ).attach( anyMap() );

		// calls
		mockSubject.connect( host, port );

		// verifies/asserts
		VirtualMachine abstractedVirtualMachine = TestUtilities.getPrivateField( subject, "virtualMachine", VirtualMachine.class );
		Assert.assertEquals( virtualMachine, abstractedVirtualMachine );
	}

	@Test(expected = IllegalStateException.class)
	public void connectThrowsIllegalStateOnInvalidVirtualMachine() throws Exception {
		// variables and mocks
		HotSwapHelper mockSubject = spy( subject );
		AttachingConnector connector = new SocketAttachingConnector();
		AttachingConnector mockConnector = spy( connector );
		String host = "whinko";
		String port = "whanko";

		doReturn( false ).when( virtualMachine ).canRedefineClasses();
		doReturn( mockConnector ).when( mockSubject ).getAttachingConnector();
		doReturn( virtualMachine ).when( mockConnector ).attach( anyMap() );

		// calls
		mockSubject.connect( host, port );
	}

	@Test
	public void setupConnectorArgumentsAttachesValidOptions() throws Exception {
		// variables and mocks
		String host = "whinko";
		String port = "whanko";
		AttachingConnector connector = new SocketAttachingConnector();

		// calls
		Map<String, Connector.Argument> arguments = subject.setupConnectorArguments( host, port, connector.defaultArguments() );

		// verifies/asserts
		Assert.assertEquals( host, arguments.get( "hostname" ).value() );
		Assert.assertEquals( port, arguments.get( "port" ).value() );
	}

	@Test
	public void setupConnectorArgumentsDoesNotAttachInvalidOptions() throws Exception {
		// variables and mocks
		String host = null;
		String port = null;
		AttachingConnector connector = new SocketAttachingConnector();

		// calls
		Map<String, Connector.Argument> arguments = subject.setupConnectorArguments( host, port, connector.defaultArguments() );

		// verifies/asserts
		Assert.assertEquals( arguments, connector.defaultArguments() );
	}

	@Test
	public void getAttachingConnectorReturnsValidConnector() throws Exception {
		// variables and mocks
		AttachingConnector connector = new SocketAttachingConnector();
		doReturn( Arrays.asList( connector ) ).when( virtualMachineManager ).attachingConnectors();

		// calls
		AttachingConnector returned = subject.getAttachingConnector();

		// verifies/asserts
		Assert.assertEquals( connector, returned );
	}

	@Test(expected = IllegalStateException.class)
	public void getAttachingConnectorThrowsIllegalStateOnInvalidConnectors() throws Exception {
		// variables and mocks
		AttachingConnector connector = new SharedMemoryAttachingConnector();
		doReturn( Arrays.asList( connector ) ).when( virtualMachineManager ).attachingConnectors();

		// calls
		subject.getAttachingConnector();
	}

	@Test(expected = IllegalStateException.class)
	public void getAttachingConnectorThrowsIllegalStateOnNoConnectors() throws Exception {
		// variables and mocks
		doReturn( Arrays.asList() ).when( virtualMachineManager ).attachingConnectors();

		// calls
		subject.getAttachingConnector();
	}

	@Test
	public void replaceSwapsValidClass() throws Exception {
		// variables and mocks
		doReturn( Arrays.asList( foundClass ) ).when( virtualMachine ).classesByName( "whonko" );
		File classFile = createRealFile();

		// calls
		subject.replace( classFile, "whonko" );

		// verifies/asserts
		verify( virtualMachine ).redefineClasses( argThat( new IsMapWithClassAndFile() ) );
	}

	@Test()
	public void replaceDoesNotSwapInvalidClass() throws Exception {
		// variables and mocks
		doReturn( null ).when( virtualMachine ).classesByName( "whonko" );
		File classFile = createRealFile();

		// calls - I know try catches are frowned upon in tests, but this builds upon the logical requirements only
		try {
			subject.replace( classFile, "whonko" );
		} catch( Exception e ) {
			// disregard
		}

		// verifies/asserts
		verify( virtualMachine, never() ).redefineClasses( anyMap() );
	}

	@Test()
	public void replaceDoesNotSwapInvalidFile() throws Exception {
		// variables and mocks
		File classFile = createFakeFile();

		// calls - I know try catches are frowned upon in tests, but this builds upon the logical requirements only
		try {
			subject.replace( classFile, "whonko" );
		} catch( Exception e ) {
			// disregard
		}

		// verifies/asserts
		verify( virtualMachine, never() ).redefineClasses( anyMap() );
	}

	@Test(expected = IllegalStateException.class)
	public void replaceThrowsIllegalStateOnInvalidClass() throws Exception {
		// variables and mocks
		doReturn( null ).when( virtualMachine ).classesByName( "whonko" );
		File classFile = createRealFile();

		// calls
		subject.replace( classFile, "whonko" );
	}

	@Test(expected = IOException.class)
	public void loadClassFileThrowsIOExceptionOnInvalidFile() throws Exception {
		// variables and mocks
		File classFile = createFakeFile();

		// calls
		subject.loadClassFile( classFile );
	}

	@Test(expected = NullPointerException.class)
	public void loadClassFileThrowsNullExceptionOnNullFile() throws Exception {
		// variables and mocks

		// calls
		subject.loadClassFile( null );
	}

	private File createRealFile() throws IOException {
		File classFile = classFolder.newFile( "whonko" );
		FileOutputStream mockOut = new FileOutputStream( classFile );
		mockOut.write( classFileBytes );
		mockOut.close();
		return classFile;
	}

	private File createFakeFile() throws IOException {
		File classFile = mock( File.class );
		doReturn( "whinko" ).when( classFile ).getPath();
		return classFile;
	}

	class IsMapWithClassAndFile extends ArgumentMatcher<Map<ReferenceType, byte[]>> {
		public boolean matches( Object map ) {
			Map tested = (Map)map;
			return Arrays.equals( (byte[])tested.get( foundClass ), classFileBytes );
		}
	}

}
