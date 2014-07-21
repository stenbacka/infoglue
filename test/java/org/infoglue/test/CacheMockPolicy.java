package org.infoglue.test;

import java.lang.reflect.Method;

import org.infoglue.deliver.util.CacheController;
import org.powermock.core.spi.PowerMockPolicy;
import org.powermock.mockpolicies.MockPolicyClassLoadingSettings;
import org.powermock.mockpolicies.MockPolicyInterceptionSettings;
import org.powermock.reflect.Whitebox;

public class CacheMockPolicy implements PowerMockPolicy
{

	@Override
	public void applyClassLoadingPolicy(MockPolicyClassLoadingSettings settings)
	{
		System.out.println("LOOL");
		settings.addFullyQualifiedNamesOfClassesToLoadByMockClassloader(CacheController.class.getName());
	}

	@Override
	public void applyInterceptionPolicy(MockPolicyInterceptionSettings settings)
	{
//		when(CacheController.getCachedObjectFromAdvancedCache(anyString(), anyString())).thenAnswer(new Answer<Object>()
//		{
//			@Override
//			public Object answer(InvocationOnMock invocation) throws Throwable
//			{
//				Object[] arguments = invocation.getArguments();
//				String cacheName = (String) arguments[0];
//				String cacheKey = (String) arguments[1];
//				Map<String, Object> cache = caches.get(cacheName);
//				return cache == null ? null : cache.get(cacheKey);
//			}
//		});
		Method m = Whitebox.getMethod(CacheController.class, "getCachedObjectFromAdvancedCache");
		System.out.println("M: " + m);
	}

}
