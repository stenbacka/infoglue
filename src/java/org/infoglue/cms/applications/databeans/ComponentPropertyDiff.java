package org.infoglue.cms.applications.databeans;

import org.infoglue.cms.applications.databeans.DiffTree.DiffState;


public class ComponentPropertyDiff
{
	public enum PropertyDiffState {CURRENT, NEW, SAME, CHANGED};

	private String propertyName;
	private PropertyDiffState state;
	private String baseValue;
	private String newValue;

	public String getPropertyName()
	{
		return propertyName;
	}
	public void setPropertyName(String propertyName)
	{
		this.propertyName = propertyName;
	}

	public PropertyDiffState getState()
	{
		return state;
	}

	public void setState(PropertyDiffState state)
	{
		this.state = state;
	}

	public String getBaseValue()
	{
		return baseValue;
	}

	public void setBaseValue(String baseValue)
	{
		this.baseValue = baseValue;
	}

	public String getNewValue()
	{
		return newValue;
	}

	public void setNewValue(String newValue)
	{
		this.newValue = newValue;
	}

	@Override
	public String toString()
	{
		return "ComponentPropertiesDiff [propertyName=" + propertyName + ", state=" + state + ", baseValue=" + baseValue + ", newValue=" + newValue + "]";
	}

}
