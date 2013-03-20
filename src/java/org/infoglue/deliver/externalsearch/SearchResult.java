/**
 * 
 */
package org.infoglue.deliver.externalsearch;

import java.util.List;
import java.util.Map;

/**
 * @author Erik Stenbäcka
 *
 */
public class SearchResult
{
	public final List<Map<String,Object>> result;
	public final Integer totalSize;

	public SearchResult(List<Map<String,Object>> result, Integer totalSize)
	{
		this.result = result;
		this.totalSize = totalSize;
	}
}
