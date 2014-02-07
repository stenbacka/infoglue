package org.infoglue.test;

import java.util.Map;

import mockit.Delegate;
import mockit.Mocked;
import mockit.NonStrictExpectations;

import org.exolab.castor.jdo.Database;
import org.infoglue.cms.controllers.kernel.impl.simple.CastorDatabaseService;
import org.infoglue.cms.entities.content.Content;
import org.infoglue.cms.entities.kernel.IBaseEntity;

public class MockDatabase
{
	public static void initMockDatabase(final Map<Object, Content> contents) throws Exception
	{
		new NonStrictExpectations(CastorDatabaseService.class)
		{
			@Mocked Database db;
			{
				CastorDatabaseService.getDatabase(); result = db;
				db.load((Class<?>)any, any, (org.exolab.castor.mapping.AccessMode)any); result = new Delegate<Object>()
				{
					<T extends IBaseEntity> T load(Class<T> clazz, Object id, org.exolab.castor.mapping.AccessMode mode)
					{
						T result = null;
						if (clazz.getName().contains("Content"))
						{
							result = (T) contents.get(id);
						}
						return result;
					}
				};
			}
		};
	}
}
