/**
 * 
 */
package org.infoglue.deliver.externalsearch;

/**
 * @author Erik Stenbäcka
 *
 */
public class SearchParameters
{
	public enum SortOrder {ASC, DESC};

	//Directory directory, String queryString, Integer startIndex, Integer count, String[] sortFields, Boolean sortAsc
	private String query;
	private Integer startIndex;
	private Integer count;
	private String[] sortFields;
	private SortOrder sortOrder;
}
