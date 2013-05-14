package org.infoglue.cms.applications.databeans;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

public class DiffTree
{
	public enum DiffState {CURRENT, NEW, SAME};

	private String nodeName;
	private List<DiffTree> children;
	private DiffState state;
	private List<ComponentPropertyDiff> properties = new LinkedList<ComponentPropertyDiff>();

	public boolean getIsBranch()
	{
		return children != null && children.size() > 0;
	}
	public String getNodeName()
	{
		return nodeName;
	}
	public void setNodeName(String nodeName)
	{
		this.nodeName = nodeName;
	}
	public List<DiffTree> getChildren()
	{
		if (children == null)
		{
			children = new ArrayList<DiffTree>();
		}
		return children;
	}
	public DiffState getState()
	{
		return state;
	}

	public void setState(DiffState state)
	{
		this.state = state;
	}

	public List<ComponentPropertyDiff> getProperties()
	{
		return properties;
	}

	public void addProperties(ComponentPropertyDiff componentPropertyDiff)
	{
		this.properties.add(componentPropertyDiff);
	}

	@Override
	public int hashCode()
	{
		final int prime = 31;
		int result = 1;
		result = prime * result + ((children == null) ? 0 : children.hashCode());
		result = prime * result + ((nodeName == null) ? 0 : nodeName.hashCode());
		return result;
	}
	@Override
	public boolean equals(Object obj)
	{
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		DiffTree other = (DiffTree) obj;
		if (children == null)
		{
			if (other.children != null)
				return false;
		}
		else if (!children.equals(other.children))
			return false;
		if (nodeName == null)
		{
			if (other.nodeName != null)
				return false;
		}
		else if (!nodeName.equals(other.nodeName))
			return false;
		return true;
	}

	@Override
	public String toString()
	{
		return "DiffTree [nodeName=" + nodeName + ", children=" + children + ", state=" + state + "]";
	}
}
