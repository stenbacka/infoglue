package org.infoglue.cms.controllers.kernel.impl.simple;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Logger;
import org.apache.xerces.parsers.DOMParser;
import org.exolab.castor.jdo.Database;
import org.infoglue.cms.applications.databeans.ComponentPropertyDiff;
import org.infoglue.cms.applications.databeans.ComponentPropertyDiff.PropertyDiffState;
import org.infoglue.cms.applications.databeans.DiffTree;
import org.infoglue.cms.applications.databeans.DiffTree.DiffState;
import org.infoglue.cms.entities.content.ContentVO;
import org.infoglue.cms.exception.SystemException;
import org.infoglue.deliver.applications.actions.InfoGlueComponent;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

public class DiffController
{
	private static final Logger logger = Logger.getLogger(DiffController.class);
	private static DiffController controller = new DiffController();

	public static DiffController getController()
	{
		if (controller == null)
		{
			controller = new DiffController();
		}
		return controller;
	}

	private void computeComponentProperties(InfoGlueComponent component, Node propertiesNode)
	{
		NodeList propertyNodeList = propertiesNode.getChildNodes();
		Element propertyElement;
		for (int i = 0; i < propertyNodeList.getLength(); i++)
		{
			if (propertyNodeList.item(i).getNodeType() == Node.ELEMENT_NODE)
			{
				propertyElement = (Element)propertyNodeList.item(i);
				Map<String, Object> property = new HashMap<String, Object>();
				property.put("name", propertyElement.getAttribute("name"));
				property.put("path", propertyElement.getAttribute("path"));
				property.put("type", propertyElement.getAttribute("type"));
				component.getProperties().put(property.get("name"), property);
			}
		}
	}

	private InfoGlueComponent getInfoGlueComponent(Element element)
	{
		InfoGlueComponent component = new InfoGlueComponent();
		component.setId(new Integer(element.getAttribute("id")));
		component.setContentId(new Integer(element.getAttribute("contentId")));
		try
		{
			ContentVO contentVO = ContentController.getContentController().getContentVOWithId(component.getContentId());
			component.setName(contentVO.getName());
		}
		catch (Exception ex)
		{
			//ex.printStackTrace();
		}
		component.setSlotName(element.getAttribute("name"));

		computeComponentProperties(component, element.getElementsByTagName("properties").item(0));

		Node components = element.getElementsByTagName("components").item(0);
		NodeList childComponents = components.getChildNodes();
		Map<String, InfoGlueComponent> componentMap = new LinkedHashMap<String, InfoGlueComponent>();
		for (int i = 0; i < childComponents.getLength(); i++)
		{
			Node n = childComponents.item(i);
			if (n.getNodeName().equals("component"))
			{
				InfoGlueComponent comp = getInfoGlueComponent((Element)n);
				componentMap.put(comp.getName() == null ? (comp.getId() + "_" + comp.getContentId()) : comp.getName(), comp);
			}
		}
		component.setComponents(componentMap);
		return component;
	}

	private void addPropertyToDiffTree(DiffTree diffTree, String propertyName, String baseValue, String newValue, PropertyDiffState state)
	{
		ComponentPropertyDiff diff = new ComponentPropertyDiff();
		diff.setPropertyName(propertyName);
		diff.setBaseValue(baseValue);
		diff.setNewValue(newValue);
		diff.setState(state);
		diffTree.addProperties(diff);
	}

	private void addPropertyToDiffTree(DiffTree diffTree, String propertyName, Map<String, Object> baseProperty, Map<String, Object> newProperty)
	{
		String baseValue = (String)baseProperty.get("path");
		String newValue = (String)newProperty.get("path");
		PropertyDiffState state;
		if (baseValue.equals(newValue))
		{
			state = PropertyDiffState.SAME;
		}
		else
		{
			state = PropertyDiffState.CHANGED;
		}
		addPropertyToDiffTree(diffTree, propertyName, baseValue, newValue, state);
	}

	protected void computePropertiesDiff(DiffTree diffTree, InfoGlueComponent baseComponent, InfoGlueComponent newComponent)
	{
		TreeMap<String, Map<String, Object>> baseProperties = new TreeMap<String, Map<String,Object>>();
		TreeMap<String, Map<String, Object>> newProperties = new TreeMap<String, Map<String,Object>>();
		baseProperties.putAll(baseComponent.getProperties());
		newProperties.putAll(newComponent.getProperties());

		Set<String> baseKeySet = baseProperties.keySet();
		String[] baseKeys = baseKeySet.toArray(new String[baseKeySet.size()]);
		Set<String> newKeySet = newProperties.keySet();
		String[] newKeys = newKeySet.toArray(new String[newKeySet.size()]);
		String baseName, newName;
		int baseIndex = 0, newIndex = 0;
		while (baseIndex < baseKeys.length && newIndex < newKeys.length)
		{
			baseName = baseKeys[baseIndex];
			newName = newKeys[newIndex];
			if (baseName.equals(newName))
			{
				addPropertyToDiffTree(diffTree, baseName, baseProperties.get(baseName), newProperties.get(newName));
				baseIndex++;
				newIndex++;
			}
			else
			{
				boolean foundMatch = false;
				base:for (int baseTempIndex = baseIndex; baseTempIndex < baseKeys.length; baseTempIndex++)
				{
					String baseTempName = baseKeys[baseTempIndex];
					for (int newTempIndex = newIndex; newTempIndex < newKeys.length; newTempIndex++)
					{
						String newTempName = newKeys[newTempIndex];
						if (baseTempName.equals(newTempName))
						{
							foundMatch = true;
							addPropertyToDiffTree(diffTree, baseTempName, baseProperties.get(baseTempName), newProperties.get(newTempName));

							for ( ; baseIndex < baseTempIndex; baseIndex++)
							{
								baseName = baseKeys[baseIndex];
								Map<String, Object> property = baseProperties.get(baseName);
								addPropertyToDiffTree(diffTree, baseName, (String)property.get("path"), null, PropertyDiffState.CURRENT);
							}

							for ( ; newIndex < newTempIndex; newIndex++)
							{
								newName = newKeys[newIndex];
								Map<String, Object> property = newProperties.get(newName);
								addPropertyToDiffTree(diffTree, newName, null, (String)property.get("path"), PropertyDiffState.NEW);
							}
							baseIndex++;
							newIndex++;
							break base;
						}
					}
				}
				if (!foundMatch)
				{
					baseName = baseKeys[baseIndex];
					Map<String, Object> currentProperty = baseProperties.get(baseName);
					addPropertyToDiffTree(diffTree, baseName, (String)currentProperty.get("path"), null, PropertyDiffState.CURRENT);

					newName = newKeys[newIndex];
					Map<String, Object> newProperty = newProperties.get(newName);
					addPropertyToDiffTree(diffTree, newName, null, (String)newProperty.get("path"), PropertyDiffState.NEW);

					baseIndex++;
					newIndex++;
				}
			}
		}

		for ( ; baseIndex < baseKeys.length; baseIndex++)
		{
			baseName = baseKeys[baseIndex];
			Map<String, Object> property = baseProperties.get(baseName);
			addPropertyToDiffTree(diffTree, baseName, (String)property.get("path"), null, PropertyDiffState.CURRENT);
		}
		for ( ; newIndex < newKeys.length; newIndex++)
		{
			newName = newKeys[newIndex];
			Map<String, Object> property = newProperties.get(newName);
			addPropertyToDiffTree(diffTree, newName, null, (String)property.get("path"), PropertyDiffState.NEW);
		}
	}

	protected InfoGlueComponent getComponentTree(String componentStructure) throws SystemException
	{
		InfoGlueComponent componentTree = new InfoGlueComponent();

		try
		{
			InputSource xmlSource = new InputSource(new StringReader(componentStructure));
			DOMParser parser = new DOMParser();
			parser.parse(xmlSource);
			Document document = parser.getDocument();
			Element components = document.getDocumentElement();
			Element component = (Element) components.getElementsByTagName("component").item(0);

			componentTree = getInfoGlueComponent(component);
		}
		catch (Exception ex)
		{
			logger.warn("Error when parsing component tree.", ex);
			throw new SystemException("Error when parsing component tree. Message: " + ex.getMessage());
		}

		return componentTree;
	}

	private boolean isComponentsEqual(InfoGlueComponent x, InfoGlueComponent y)
	{
		return x.getContentId().equals(y.getContentId()) && x.getId().equals(y.getId());
	}

	protected void computeSubTree(DiffTree diffTree, ArrayList<InfoGlueComponent> baseList, ArrayList<InfoGlueComponent> newList)
	{
		int f = 0, s = 0;

		while (f < baseList.size() || s < newList.size())
		{
			InfoGlueComponent baseComponent;
			InfoGlueComponent newComponent;
			if (s < baseList.size())
			{
				baseComponent = baseList.get(s);
			}
			else
			{
				while (s < newList.size())
				{
					newComponent = newList.get(s++);
					DiffTree subTree = new DiffTree();
					subTree.setNodeName(newComponent.getName());
					subTree.setState(DiffState.NEW);
					diffTree.getChildren().add(subTree);
					ArrayList<InfoGlueComponent> nl = new ArrayList<InfoGlueComponent>(newComponent.getComponents().values());
					computeSubTree(subTree, new ArrayList<InfoGlueComponent>(), nl);
				}
				continue;
			}
			if (s < newList.size())
			{
				newComponent = newList.get(s);
			}
			else
			{
				while (f < baseList.size())
				{
					baseComponent = baseList.get(f++);
					DiffTree subTree = new DiffTree();
					subTree.setNodeName(baseComponent.getName());
					subTree.setState(DiffState.CURRENT);
					diffTree.getChildren().add(subTree);
					ArrayList<InfoGlueComponent> bl = new ArrayList<InfoGlueComponent>(baseComponent.getComponents().values());
					computeSubTree(subTree, bl, new ArrayList<InfoGlueComponent>());
				}
				continue;
			}


			if (isComponentsEqual(baseComponent, newComponent))
			{
				DiffTree subTree = new DiffTree();
				subTree.setNodeName(baseComponent.getName());
				subTree.setState(DiffState.SAME);
				diffTree.getChildren().add(subTree);
				computePropertiesDiff(subTree, baseComponent, newComponent);
				ArrayList<InfoGlueComponent> fl = new ArrayList<InfoGlueComponent>(baseComponent.getComponents().values());
				ArrayList<InfoGlueComponent> sl = new ArrayList<InfoGlueComponent>(newComponent.getComponents().values());
				computeSubTree(subTree, fl, sl);
				f++;
				s++;
			}
			else
			{
				boolean foundMatch = false;
				base:for (int tf = f; tf < baseList.size(); tf++)
				{
					System.out.println("tf: " + tf);
					InfoGlueComponent bc = baseList.get(tf);
					for (int ts = s; ts < newList.size(); ts++)
					{
						System.out.println("ts: " + ts);
						InfoGlueComponent nc = newList.get(ts);
						if (isComponentsEqual(bc, nc))
						{
							// Found match
							foundMatch = true;
							while (f < tf)
							{
								System.out.println(">> tf: " + tf + ", f: " + f);
								InfoGlueComponent x = baseList.get(f++);
								DiffTree subTree = new DiffTree();
								subTree.setNodeName(x.getName());
								subTree.setState(DiffState.CURRENT);
								diffTree.getChildren().add(subTree);
								ArrayList<InfoGlueComponent> fl = new ArrayList<InfoGlueComponent>(x.getComponents().values());
								computeSubTree(subTree, fl, new ArrayList<InfoGlueComponent>());
							}
							while (s < ts)
							{
								System.out.println(">> ts: " + ts + ", s: " + s);
								InfoGlueComponent x = newList.get(s++);
								DiffTree subTree = new DiffTree();
								subTree.setNodeName(x.getName());
								subTree.setState(DiffState.NEW);
								diffTree.getChildren().add(subTree);
								ArrayList<InfoGlueComponent> sl = new ArrayList<InfoGlueComponent>(x.getComponents().values());
								computeSubTree(subTree, new ArrayList<InfoGlueComponent>(), sl);
							}

							f = tf + 1;
							s = ts + 1;
							DiffTree subTree = new DiffTree();
							subTree.setNodeName(bc.getName());
							subTree.setState(DiffState.SAME);
							computePropertiesDiff(subTree, bc, nc);
							diffTree.getChildren().add(subTree);
							ArrayList<InfoGlueComponent> fl = new ArrayList<InfoGlueComponent>(bc.getComponents().values());
							ArrayList<InfoGlueComponent> sl = new ArrayList<InfoGlueComponent>(nc.getComponents().values());
							computeSubTree(subTree, fl, sl);
							break base;
						}
					}
				}
				if (!foundMatch)
				{
					DiffTree subTree = new DiffTree();
					subTree.setNodeName(baseComponent.getName());
					subTree.setState(DiffState.CURRENT);
					diffTree.getChildren().add(subTree);
					ArrayList<InfoGlueComponent> fl = new ArrayList<InfoGlueComponent>(baseComponent.getComponents().values());
					computeSubTree(subTree, fl, new ArrayList<InfoGlueComponent>());

					DiffTree subTree2 = new DiffTree();
					subTree2.setNodeName(newComponent.getName());
					subTree2.setState(DiffState.NEW);
					diffTree.getChildren().add(subTree2);
					ArrayList<InfoGlueComponent> sl = new ArrayList<InfoGlueComponent>(newComponent.getComponents().values());
					computeSubTree(subTree2, new ArrayList<InfoGlueComponent>(), sl);

					f++;
					s++;
				}
			}
		}
	}

	public DiffTree compareSiteNodeVersions(Integer baseMetaContentVersionId, Integer workingMetaContentVersionId)
	{
		DiffTree diffTree = null;
		Database db = null;
		try
		{
			db = CastorDatabaseService.getDatabase();
			db.begin();

			String firstComponentStructure = ContentVersionController.getContentVersionController().getAttributeValue(baseMetaContentVersionId, "ComponentStructure", false);
			String secondComponentStructure = ContentVersionController.getContentVersionController().getAttributeValue(workingMetaContentVersionId, "ComponentStructure", false);

			InfoGlueComponent baseComponentTree = getComponentTree(firstComponentStructure);
			InfoGlueComponent workingComponentTree = getComponentTree(secondComponentStructure);

			diffTree = new DiffTree();
			if (isComponentsEqual(baseComponentTree, workingComponentTree))
			{
				diffTree.setState(DiffState.SAME);
				diffTree.setNodeName(baseComponentTree.getName());
				ArrayList<InfoGlueComponent> baseList = new ArrayList<InfoGlueComponent>();
				baseList.addAll(baseComponentTree.getComponents().values());
				ArrayList<InfoGlueComponent> newList = new ArrayList<InfoGlueComponent>();
				newList.addAll(workingComponentTree.getComponents().values());
				computeSubTree(diffTree, baseList, newList);
			}
			else
			{
				diffTree.setState(DiffState.NEW);
				diffTree.setNodeName(workingComponentTree.getName());
			}
		}
		catch (Exception ex)
		{
			if (db != null)
			{
				try
				{
					db.rollback();
					db.close();
				}
				catch (Exception e)
				{
					e.printStackTrace();
				}
			}
		}
		return diffTree;
	}

}
