package org.infoglue.cms.controllers.kernel.impl.simple;

import static org.junit.Assert.*;
import static org.mockito.Mockito.*;
import static org.mockito.Matchers.*;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.infoglue.cms.controllers.kernel.impl.simple.CastorDatabaseService;
import org.infoglue.cms.controllers.kernel.impl.simple.ContentController;
import org.infoglue.cms.entities.content.Content;
import org.infoglue.cms.entities.content.ContentVO;
import org.infoglue.cms.entities.content.impl.simple.SmallContentImpl;
import org.infoglue.deliver.util.CacheController;
import org.infoglue.test.InfoglueControllerTestCase;
import org.infoglue.test.InfoglueMocks;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.powermock.api.mockito.PowerMockito;
import org.powermock.core.classloader.annotations.PrepareForTest;
import org.powermock.modules.junit4.PowerMockRunner;

@RunWith(PowerMockRunner.class)
@PrepareForTest({CastorDatabaseService.class})
public class ContentControllerTest extends InfoglueControllerTestCase
{
	@Test
	public void testGetContentVOWithId_FromCache() throws Throwable
	{
		final String cacheName = "contentCache";
		final Integer contentId = 159;
		final ContentVO cVO = mock(ContentVO.class);
		when(cVO.getContentId()).thenReturn(contentId);

		TestCaseSettings testCaseSettings = new TestCaseSettings();
		testCaseSettings.setEntityContaniner(new EntityContainer(null, null));
		testCaseSettings.addCacheEntity(cacheName, "" + contentId, cVO);

		executeEntityTestcase(testCaseSettings, new TestCase()
		{
			@Override
			public void execute() throws Exception
			{
				ContentVO result = ContentController.getContentController().getContentVOWithId(contentId);

				assertTrue("Got the wrong contentId back from contentVOWithId", cVO == result);
				PowerMockito.verifyStatic();
				CacheController.getCachedObjectFromAdvancedCache(eq(cacheName), eq("" + contentId));
			}
		});
	}

	@Test
	public void testGetContentVOWithId() throws Throwable
	{
		final Integer contentId = 123;
		final Content c = InfoglueMocks.mockContent(SmallContentImpl.class, contentId);

		Map<Integer, Content> contents = new HashMap<Integer, Content>();
		contents.put(contentId, c);
		EntityContainer entities =  new EntityContainer(contents, null);

		TestCaseSettings testCaseSettings = new TestCaseSettings();
		testCaseSettings.setEntityContaniner(entities);
		testCaseSettings.setVerifyReadOnly(true);

		executeEntityTestcase(testCaseSettings, new TestCase()
		{
			@Override
			public void execute() throws Exception
			{
				ContentVO result = ContentController.getContentController().getContentVOWithId(contentId);

				assertTrue("Got the wrong contentId back from contentVOWithId", c.getVO() == result);
				PowerMockito.verifyStatic();
				CacheController.cacheObjectInAdvancedCache(eq("contentCache"), anyString(), eq(c.getVO()), (String[])any(), anyBoolean());
			}
		});
	}
}
