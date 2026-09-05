package org.cytoscape.cyndex2.internal;

import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.mockito.Mockito.mock;

import java.util.HashMap;
import java.util.Map;

import org.cytoscape.io.read.InputStreamTaskFactory;
import org.cytoscape.io.write.CyNetworkViewWriterFactory;
import org.junit.Before;
import org.junit.Test;

/**
 * Builds its own manager rather than mutating the INSTANCE singleton.
 */
public class CxTaskFactoryManagerTest {

	private CxTaskFactoryManager manager;

	@Before
	public void setUp() {
		manager = new CxTaskFactoryManager();
	}

	private static Map<String, String> props(String id) {
		Map<String, String> p = new HashMap<>();
		p.put("id", id);
		return p;
	}

	@Test
	public void eachFormatResolvesToItsOwnWriter() {
		CyNetworkViewWriterFactory cx1 = mock(CyNetworkViewWriterFactory.class);
		CyNetworkViewWriterFactory cx2 = mock(CyNetworkViewWriterFactory.class);
		manager.addWriterFactory(cx1, props(CxFormat.CX1.getWriterId()));
		manager.addWriterFactory(cx2, props(CxFormat.CX2.getWriterId()));

		assertSame(cx1, manager.getWriterFactory(CxFormat.CX1));
		assertSame(cx2, manager.getWriterFactory(CxFormat.CX2));
	}

	@Test
	public void eachFormatResolvesToItsOwnReader() {
		InputStreamTaskFactory cx1 = mock(InputStreamTaskFactory.class);
		InputStreamTaskFactory cx2 = mock(InputStreamTaskFactory.class);
		manager.addReaderFactory(cx1, props(CxFormat.CX1.getReaderId()));
		manager.addReaderFactory(cx2, props(CxFormat.CX2.getReaderId()));

		assertSame(cx1, manager.getReaderFactory(CxFormat.CX1));
		assertSame(cx2, manager.getReaderFactory(CxFormat.CX2));
	}

	@Test
	@SuppressWarnings("deprecation")
	public void legacyNoArgGettersStillReturnTheCx1Factories() {
		InputStreamTaskFactory reader = mock(InputStreamTaskFactory.class);
		CyNetworkViewWriterFactory writer = mock(CyNetworkViewWriterFactory.class);
		manager.addReaderFactory(reader, props(CxFormat.CX1.getReaderId()));
		manager.addWriterFactory(writer, props(CxFormat.CX1.getWriterId()));

		assertSame(reader, manager.getCxReaderFactory());
		assertSame(writer, manager.getCxWriterFactory());
	}

	@Test
	public void unboundFormatReturnsNullSoCallersCanRaiseTheVersionError() {
		assertNull(manager.getWriterFactory(CxFormat.CX2));
		assertNull(manager.getReaderFactory(CxFormat.CX2));
	}

	@Test
	public void unrelatedServiceIdsAreIgnored() {
		manager.addWriterFactory(mock(CyNetworkViewWriterFactory.class), props("someOtherWriterFactory"));
		manager.addReaderFactory(mock(InputStreamTaskFactory.class), props("someOtherReaderFactory"));
		manager.addReaderFactory(mock(InputStreamTaskFactory.class), new HashMap<String, String>());

		for (CxFormat format : CxFormat.values()) {
			assertNull(manager.getWriterFactory(format));
			assertNull(manager.getReaderFactory(format));
		}
	}

	@Test
	public void removingAFactoryUnbindsOnlyThatFormat() {
		InputStreamTaskFactory cx1 = mock(InputStreamTaskFactory.class);
		InputStreamTaskFactory cx2 = mock(InputStreamTaskFactory.class);
		manager.addReaderFactory(cx1, props(CxFormat.CX1.getReaderId()));
		manager.addReaderFactory(cx2, props(CxFormat.CX2.getReaderId()));

		manager.removeReaderFactory(cx2, props(CxFormat.CX2.getReaderId()));

		assertSame(cx1, manager.getReaderFactory(CxFormat.CX1));
		assertNull(manager.getReaderFactory(CxFormat.CX2));
	}
}
