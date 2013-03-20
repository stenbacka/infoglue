/**
 * 
 */
package org.infoglue.deliver.externalsearch;

import java.util.Map;

/**
 * @author Erik Stenbäcka
 *
 */
public class DummyIndexer implements Indexer
{
	private Map<String, String> config;

	public Map<String, String> getConfig()
	{
		return this.config;
	}

	@Override
	public void setConfig(Map<String, String> config)
	{
		this.config = config;
	}

	@Override
	public void init()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void index()
	{
		// TODO Auto-generated method stub

	}

	@Override
	public void destroy()
	{
		// TODO Auto-generated method stub

	}

}
