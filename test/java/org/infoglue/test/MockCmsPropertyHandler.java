package org.infoglue.test;

import mockit.Mock;
import mockit.MockUp;

import org.infoglue.cms.util.CmsPropertyHandler;

public class MockCmsPropertyHandler extends MockUp<CmsPropertyHandler>
{
	
	@Mock
	void initializeProperties()
	{
		System.out.println("Called1");
	}
	
	@Mock
	String getServerNodeProperty(String prefix, String key, boolean inherit, String defaultValue, boolean skipCaches)
	{
		System.out.println("Called2");
		return "apa";
	}
//	
//	public static void initMockCmsPropertyHandler(final Map<String, String> valueMap)
//	{
//		new NonStrictExpectations(CmsPropertyHandler.class)
//		{
//			{
//				CmsPropertyHandler.initializeProperties();
//				CmsPropertyHandler.getServerNodeProperty(anyString, anyString, anyBoolean, anyString, anyBoolean); result = new mockit.Delegate<String>() {
//					String getServerNodeProperty(String prefix, String key, boolean inherit, String defaultValue, boolean skipCaches)
//					{
//						return valueMap.get((prefix == null ? "" : prefix) + key);
//					}
//				};
//			}
//		};
//	}
}
