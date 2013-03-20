/**
 * 
 */
package org.infoglue.deliver.externalsearch;

import java.io.InputStream;

/**
 * @author Erik Stenbäcka
 *
 */
public interface DataRetriever extends ConfigurableDelegate
{
	void init();
	InputStream openConnection();
	void closeConnection();
	void destroy();
}
