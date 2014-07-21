package org.infoglue.test;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyInt;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.verify;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.exolab.castor.jdo.Database;
import org.exolab.castor.mapping.AccessMode;
import org.infoglue.cms.controllers.kernel.impl.simple.CastorDatabaseService;
import org.infoglue.cms.entities.content.Content;
import org.infoglue.cms.entities.kernel.IBaseEntity;
import org.infoglue.cms.entities.structure.SiteNode;
import org.infoglue.deliver.invokers.PageInvoker;
import org.infoglue.deliver.util.CacheController;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;

@PrepareForTest({CastorDatabaseService.class, CacheController.class})
public abstract class InfoglueControllerTestCase
{
	protected class EntityContainer
	{
		final Map<Integer, Content> contents;
		final Map<Integer, SiteNode> siteNodes;
		public EntityContainer(Map<Integer, Content> contents, Map<Integer, SiteNode> siteNodes)
		{
			this.contents = contents == null ? new HashMap<Integer, Content>() : contents;
			this.siteNodes = siteNodes == null ? new HashMap<Integer, SiteNode>() : siteNodes;
		}
	}

	protected interface TestCase
	{
		void execute() throws Throwable;
	}

	protected class TestCaseSettings
	{
		EntityContainer databaseEntities;
		boolean verifyReadOnly = false;
		Map<String, Map<String, Object>> caches = new HashMap<String, Map<String,Object>>();

		public TestCaseSettings()
		{
		}

		public void setEntityContaniner(EntityContainer databaseEntities)
		{
			this.databaseEntities = databaseEntities;
		}

		public void addDatabaseEntity(Integer id, Object entity)
		{
			if (this.databaseEntities == null)
			{
				this.databaseEntities = new EntityContainer(null, null);
			}

			if (entity instanceof Content)
			{
				databaseEntities.contents.put(id, (Content)entity);
			}
			else if(entity instanceof SiteNode)
			{
				databaseEntities.siteNodes.put(id, (SiteNode)entity);
			}
			else
			{
				throw new IllegalArgumentException("Cannot add database entity since the given entity is not of a valid class. Class: " + entity.getClass());
			}
		}

		public void setVerifyReadOnly(boolean verifyReadOnly)
		{
			this.verifyReadOnly = verifyReadOnly;
		}

		public void setCaches(Map<String, Map<String, Object>> caches)
		{
			this.caches = caches;
		}

		public void addCache(String cacheName, Map<String, Object> cacheEntities)
		{
			if (this.caches == null)
			{
				this.caches = new HashMap<String, Map<String,Object>>();
			}
			caches.put(cacheName, cacheEntities);
		}

		public void addCacheEntity(String cacheName, String cacheKey, Object entity)
		{
			if (this.caches == null)
			{
				this.caches = new HashMap<String, Map<String,Object>>();
			}
			Map<String, Object> cache = this.caches.get(cacheName);
			if (cache == null)
			{
				cache = new HashMap<String, Object>();
				caches.put(cacheName, cache);
			}
			cache.put(cacheKey, entity);
		}
	}

	private Database mockDatabaseEntities(final EntityContainer entities, final boolean verifyReadOnly) throws Exception
	{
		Answer<Object> databaseLoadAnswer = new Answer<Object>()
		{
			@SuppressWarnings("deprecation")
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable
			{
				Object[] arguments = invocation.getArguments();
				@SuppressWarnings("unchecked")
				Class<IBaseEntity> clazz = (Class<IBaseEntity>)arguments[0];
				Integer id = (Integer)arguments[1];
				if (verifyReadOnly)
				{
					if (arguments.length < 3 || arguments[2] != Database.ReadOnly)
					{
						throw new IllegalArgumentException("Database connection is in read/write mode when it should be in read only mode. Entity type: " + clazz);
					}
				}
				IBaseEntity entity = null;
				if (clazz.getName().contains("Content"))
				{
					entity = entities.contents.get(id);
				}
				else if (clazz.getName().contains("SiteNode"))
				{
					entity = entities.siteNodes.get(id);
				}
				if (entity == null)
				{
					throw new IllegalArgumentException("The given ID does not have a mocked entity. Entity type: " + clazz + ". Id: " + id);
				}
				return entity;
			}
		};

		PowerMockito.mockStatic(CastorDatabaseService.class);
		Database db = mock(Database.class);
		when(CastorDatabaseService.getDatabase()).thenReturn(db);
		when(db.load((Class<?>)any(), any())).thenAnswer(databaseLoadAnswer);
		when(db.load((Class<?>)any(), any(), (AccessMode)any())).thenAnswer(databaseLoadAnswer);
		return db;
	}

	private void mockCacheController(final Map<String, Map<String, Object>> caches) throws Exception
	{
		Answer<Object> cacheAnswer = new Answer<Object>()
		{
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable
			{
				Object[] arguments = invocation.getArguments();
				String cacheName = (String) arguments[0];
				String cacheKey = (String) arguments[1];
				Map<String, Object> cache = caches.get(cacheName);
				return cache == null ? null : cache.get(cacheKey);
			}
		};

		PowerMockito.mockStatic(CacheController.class);
		when(CacheController.getCachedObjectFromAdvancedCache(anyString(), anyString())).thenAnswer(cacheAnswer);
		when(CacheController.getCachedObjectFromAdvancedCache(anyString(), anyString(), anyInt())).thenAnswer(cacheAnswer);
		when(CacheController.getCachedObjectFromAdvancedCache(anyString(), anyString(), anyBoolean(), anyString(), anyBoolean())).thenAnswer(cacheAnswer);
		when(CacheController.getCachedObjectFromAdvancedCache(anyString(), anyString(), anyBoolean(), anyString(), anyBoolean(), any(), (Method)any(), (Object[])any(), (PageInvoker)any())).thenAnswer(cacheAnswer);
	}
	
	private boolean hasDatabaseEntities(TestCaseSettings settings)
	{
		return settings.databaseEntities.contents.size() > 0 || settings.databaseEntities.siteNodes.size() > 0;
	}

	protected void executeEntityTestcase(TestCaseSettings settings, TestCase testCase) throws Throwable
	{
		mockCacheController(settings.caches);
		boolean hasDatabaseEntities = hasDatabaseEntities(settings);
		Database mockedDb = null;
		if (hasDatabaseEntities)
		{
			mockedDb = mockDatabaseEntities(settings.databaseEntities, settings.verifyReadOnly);
		}
		testCase.execute();
		if (hasDatabaseEntities)
		{
			verify(mockedDb).begin();
			try
			{
				verify(mockedDb).commit();
			}
			catch (AssertionError ae)
			{
				try
				{
					verify(mockedDb).rollback();
				}
				catch (AssertionError ae2)
				{
					throw new AssertionError("Neither commit nor rollback was called on the database object");
				}
			}
			verify(mockedDb).close();
		}
	}
}
