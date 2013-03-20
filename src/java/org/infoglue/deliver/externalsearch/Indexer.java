/**
 * 
 */
package org.infoglue.deliver.externalsearch;


/**
 * @author Erik Stenbäcka
 *
 */
public interface Indexer extends ConfigurableDelegate
{
	void init();
	void index();
	void destroy();
}
