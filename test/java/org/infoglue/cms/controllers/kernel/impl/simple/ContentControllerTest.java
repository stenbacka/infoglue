package org.infoglue.cms.controllers.kernel.impl.simple;

import static org.junit.Assert.assertEquals;

import java.util.HashMap;
import java.util.Map;

import mockit.Mocked;
import mockit.NonStrictExpectations;
import mockit.Verifications;

import org.infoglue.cms.entities.content.Content;
import org.infoglue.cms.entities.content.ContentVO;
import org.infoglue.cms.entities.content.impl.simple.ContentImpl;
import org.infoglue.cms.util.CmsPropertyHandler;
import org.infoglue.deliver.util.CacheController;
import org.infoglue.test.MockDatabase;
import org.junit.Test;

public class ContentControllerTest
{
	@Mocked final CmsPropertyHandler cph = null;
	
	@Test
	public void testGetContentVOWithId(@Mocked final CacheController cc) throws Exception
	{
		final Integer contentId = 123;
		final Integer contentId2 = 456;
		Content c1 = new ContentImpl();
		final ContentVO c1VO = new ContentVO();
		final ContentVO c2VO = new ContentVO();
		c1VO.setContentId(contentId);
		c1.setValueObject(c1VO);
		c2VO.setContentId(contentId2);
		Map<Object, Content> contents = new HashMap<Object, Content>();
		contents.put(c1.getContentId(), c1);
		final Map<String, String> valueMap = new HashMap<String, String>();
		valueMap.put("defaultNumberOfYearsBeforeExpire", "50");
		MockDatabase.initMockDatabase(contents);

		new NonStrictExpectations()
		{{
			CmsPropertyHandler.initializeProperties();
			CmsPropertyHandler.getServerNodeProperty(anyString, anyString, anyBoolean, anyString, anyBoolean); result = new mockit.Delegate<String>() {
				String getServerNodeProperty(String prefix, String key, boolean inherit, String defaultValue, boolean skipCaches)
				{
					System.out.println("Gets here");
					return valueMap.get((prefix == null ? "" : prefix) + key);
				}
			};

			CacheController.getCachedObjectFromAdvancedCache("contentCache", "" + contentId2); result = c2VO;
		}};
		
		ContentController.getContentController().getContentVOWithId(contentId);
		ContentVO result2 = ContentController.getContentController().getContentVOWithId(contentId2);
		
		new Verifications()
		{
			{
				CacheController.cacheObjectInAdvancedCache("contentCache", "123", (ContentVO)any, (String[])any, true);// times = 1;
				CacheController.cacheObjectInAdvancedCache("contentCache", "456", (ContentVO)any, (String[])any, true); times = 0;
			}
		};

		assertEquals("", result2.getContentId(), contentId2);
	}
}
