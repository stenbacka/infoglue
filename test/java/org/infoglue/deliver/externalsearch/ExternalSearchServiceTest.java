/**
 * 
 */
package org.infoglue.deliver.externalsearch;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.mockito.MockitoAnnotations;
import org.mockito.Spy;

/**
 * @author Erik Stenbäcka
 *
 */
public class ExternalSearchServiceTest
{
	ExternalSearchService service;

	@Spy DataRetriever dataRetriever = new DummyRetriever();
	@Spy Parser parser = new DummyParser();
	@Spy Indexer indexer = new DummyIndexer();

	@Before
	public void setUp() throws Exception
	{
		MockitoAnnotations.initMocks(this);
	}
	
	@Test
	public void testDestroyService()
	{
		ExternalSearchServiceConfig config = new ExternalSearchServiceConfig();

		config.setMaxAge(null);
		config.setDataRetriever(dataRetriever);
		config.setParser(parser);
		config.setIndexer(indexer);

		// Execution
		service = new ExternalSearchService(config);
		service.destroyService();

		verify(dataRetriever).destroy();
		verify(parser).destroy();
		verify(indexer).destroy();
	}

	@Test
	public void testDelegateLifeCycleInit()
	{
		ExternalSearchServiceConfig config = new ExternalSearchServiceConfig();

		config.setMaxAge(null);
		config.setDataRetriever(dataRetriever);
		config.setParser(parser);
		config.setIndexer(indexer);

		// Execution
		service = new ExternalSearchService(config);

		verify(dataRetriever).init();
		verify(parser).init();
		verify(indexer).init();
	}

	@Test
	public void testDelegateLifeCycleDestroy()
	{
		ExternalSearchServiceConfig config = new ExternalSearchServiceConfig();

		config.setMaxAge(null);
		config.setDataRetriever(dataRetriever);
		config.setParser(parser);
		config.setIndexer(indexer);

		service = new ExternalSearchService(config);

		ExternalSearchServiceConfig config2 = new ExternalSearchServiceConfig();
		config2.setMaxAge(null);
		config2.setDataRetriever(new DummyRetriever());
		config2.setParser(new DummyParser());
		config2.setIndexer(new DummyIndexer());

		// Execution
		service.setConfig(config2);

		verify(dataRetriever).destroy();
		verify(parser).destroy();
		verify(indexer).destroy();
	}

	@Test
	public void testIndexHasExpiredNoMaxAge()
	{
		ExternalSearchServiceConfig config = new ExternalSearchServiceConfig();

		config.setMaxAge(null);
		config.setDataRetriever(new DummyRetriever());
		config.setParser(new DummyParser());
		config.setIndexer(new DummyIndexer());

		// Execution
		service = new ExternalSearchService(config);

		assertFalse("", service.indexHasExpired());
	}

	@Test
	public void testIndexHasExpiredNoCreated()
	{
		ExternalSearchServiceConfig config = new ExternalSearchServiceConfig();

		config.setMaxAge(3600);
		config.setDataRetriever(new DummyRetriever());
		config.setParser(new DummyParser());
		config.setIndexer(new DummyIndexer());

		// Execution
		service = new ExternalSearchService(config);

		assertTrue("", service.indexHasExpired());
	}

	@Test
	public void testDontIndexWhenNoMaxAge()
	{
		ExternalSearchServiceConfig config = new ExternalSearchServiceConfig();

		config.setMaxAge(null);
		config.setDataRetriever(new DummyRetriever());
		config.setParser(new DummyParser());
		config.setIndexer(new DummyIndexer());

		// Execution
		service = new ExternalSearchService(config);
		boolean doIndex = service.startIndexing();

		assertFalse("Should not start index if no max age", doIndex);
	}

	@Test
	public void testIndexWhenNoDependencies()
	{
		ExternalSearchServiceConfig config = new ExternalSearchServiceConfig();

		config.setMaxAge(3600);
		config.setDataRetriever(new DummyRetriever());
		config.setParser(new DummyParser());
		config.setIndexer(new DummyIndexer());
		config.setDependencis(null);

		// Execution
		service = new ExternalSearchService(config);
		boolean doIndex = service.startIndexing();

		assertTrue("Should start index if no dependencies", doIndex);
	}

	@Test
	public void testDontIndexWhenNonExistingDependency()
	{
		ExternalSearchServiceConfig config = new ExternalSearchServiceConfig();

		config.setMaxAge(3600);
		config.setDataRetriever(new DummyRetriever());
		config.setParser(new DummyParser());
		config.setIndexer(new DummyIndexer());
		config.setDependencis(Collections.singletonList("apa"));

		ExternalSearchManager mockManager = mock(ExternalSearchManager.class);
		when(mockManager.getService("apa")).thenReturn(null);

		// Execution
		service = new ExternalSearchService(config);
		boolean doIndex = service.startIndexing();

		assertFalse("Should not start index missing dependency", doIndex);
	}

	@Test
	public void testDontIndexWhenExistingDependencyNotSearchable()
	{
		ExternalSearchServiceConfig config = new ExternalSearchServiceConfig();

		config.setMaxAge(3600);
		config.setDataRetriever(new DummyRetriever());
		config.setParser(new DummyParser());
		config.setIndexer(new DummyIndexer());
		config.setDependencis(Collections.singletonList("apa"));

		ExternalSearchManager mockManager = mock(ExternalSearchManager.class);
		ExternalSearchService mockService = mock(ExternalSearchService.class);
		when(mockService.isSearchable()).thenReturn(false);
		when(mockManager.getService("apa")).thenReturn(mockService);

		// Execution
		service = new ExternalSearchService(config);
		boolean doIndex = service.startIndexing();

		assertFalse("Should not start index when dependency not searchable", doIndex);
	}

	@Test
	public void testIndexWhenExistingDependencySearchable()
	{
		ExternalSearchServiceConfig config = new ExternalSearchServiceConfig();

		config.setMaxAge(3600);
		config.setDataRetriever(new DummyRetriever());
		config.setParser(new DummyParser());
		config.setIndexer(new DummyIndexer());
		config.setDependencis(Collections.singletonList("apa"));

		ExternalSearchManager mockManager = mock(ExternalSearchManager.class);
		ExternalSearchService mockService = mock(ExternalSearchService.class);
		when(mockService.isSearchable()).thenReturn(true);
		when(mockManager.getService("apa")).thenReturn(mockService);
		ExternalSearchManager.injectManager(mockManager);

		// Execution
		service = new ExternalSearchService(config);
		boolean doIndex = service.startIndexing();

		assertTrue("Should start index when dependency is searchable", doIndex);
	}

	@Test
	public void testDontIndexMultipleDependencies()
	{
		ExternalSearchServiceConfig config = new ExternalSearchServiceConfig();

		config.setMaxAge(3600);
		config.setDataRetriever(new DummyRetriever());
		config.setParser(new DummyParser());
		config.setIndexer(new DummyIndexer());
		List<String> dependencies = new ArrayList<String>();
		dependencies.add("apa");
		dependencies.add("bepa");
		config.setDependencis(dependencies);

		ExternalSearchManager mockManager = mock(ExternalSearchManager.class);
		ExternalSearchService mockServiceA = mock(ExternalSearchService.class);
		when(mockServiceA.isSearchable()).thenReturn(true);
		when(mockManager.getService("apa")).thenReturn(mockServiceA);
		when(mockManager.getService("bepa")).thenReturn(null);

		// Execution
		service = new ExternalSearchService(config);
		boolean doIndex = service.startIndexing();

		assertFalse("Should not start index if no dependencies", doIndex);
	}

}
