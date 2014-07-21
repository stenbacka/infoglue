package org.infoglue.test;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyBoolean;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.powermock.api.mockito.PowerMockito.mock;
import static org.powermock.api.mockito.PowerMockito.when;
import static org.mockito.Mockito.*;

import java.util.Map;

import org.exolab.castor.jdo.Database;
import org.exolab.castor.mapping.AccessMode;
import org.infoglue.cms.controllers.kernel.impl.simple.CastorDatabaseService;
import org.infoglue.cms.entities.content.Content;
import org.infoglue.cms.entities.content.ContentVO;
import org.infoglue.cms.util.CmsPropertyHandler;
import org.infoglue.deliver.util.CacheController;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.powermock.api.mockito.PowerMockito;

public final class InfoglueMocks
{
	public static void mockCmsPropertyHandler(final Map<String, Object> propertyMap)
	{
		PowerMockito.mockStatic(CmsPropertyHandler.class, new Answer<Object>()
		{
			@Override
			public Object answer(InvocationOnMock invocation) throws Throwable
			{
				String methodName = invocation.getMethod().getName();
				if (propertyMap.containsKey(methodName))
				{
					return propertyMap.get(methodName);
				}
				else
				{
					methodName = methodName.replaceFirst("^get(.)", "$1");
					if (propertyMap.containsKey(methodName))
					{
						return propertyMap.get(methodName);
					}
					else
					{
						throw new IllegalArgumentException("The current method does not have a mock value in the mock class. Method name: " + invocation.getMethod().getName());
					}
				}
			}
		});
	}

	public static void mockDatabase(final Map<Object, Content> contents)
	{
		try
		{
			PowerMockito.mockStatic(CastorDatabaseService.class);
			Database db = mock(Database.class);
			when(db.load((Class<?>)any(), any(), (AccessMode)any())).thenAnswer(new Answer<Object>()
			{
				@Override
				public Object answer(InvocationOnMock invocation) throws Throwable
				{
					System.out.println("Asking for content: " + invocation.getArguments()[1]);
					return contents.get(invocation.getArguments()[1]);
				}
				
			});
			when(CastorDatabaseService.getDatabase()).thenReturn(db);
		}
		catch (Throwable tr)
		{
			System.err.println("Exception in mockDatabase: " + tr.getMessage());
		}
	}

	public static <T extends Content> T mockContent(Class<T> clazz, Integer contentId)
	{
		return mockContent(clazz, contentId, null);
	}

	/**
	 * <p>Instantiates a content for mock purposes of the specific class provided in <em>clazz</em>. The
	 * actual content is a real object instantiated with the no-parameter constructor. The <em>getVO</em> method is mocked
	 * to return a mocked instance of <em>ContentVO</em>.</p>
	 *
	 * <p>The contentId i always mocked with the rest of the method set to returning the default value as defined by
	 * the mocking library used. If more methods need to have explicit values set the <em>callback</em> can be used to
	 * set them. This method is intended as a general purpose convenience method. There may be cases where a mock of the
	 * Content class is required. In those cases you should mock them for yourself.</p>
	 *
	 * @param clazz The class to create a mock for.
	 * @param contentId The contentId of the mock object
	 * @param callback An optional callback for setting expectations on the mock object (ContentVO). Provide null if no 
	 * 		extra configuration is required
	 * @return A real object that is backed by a mocked ContentVO.
	 */
	public static <T extends Content> T mockContent(Class<T> clazz, Integer contentId, ContentMockCallback callback)
	{
		try
		{
			T c = clazz.newInstance();
			final ContentVO cVO = mock(ContentVO.class);
			c.setVO(cVO);
			when(cVO.getContentId()).thenReturn(contentId);
			if (callback != null)
			{
				callback.complementVO(cVO);
			}
			return c;
		}
		catch (Exception ex)
		{
			throw new RuntimeException("Exception converted to runtime exception. Original type: " + ex.getClass(), ex);
		}
	}

	interface ContentMockCallback
	{
		void complementVO(ContentVO mock);
	}
}
