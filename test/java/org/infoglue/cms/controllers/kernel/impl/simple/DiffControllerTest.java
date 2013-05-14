package org.infoglue.cms.controllers.kernel.impl.simple;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.infoglue.cms.applications.databeans.ComponentPropertyDiff;
import org.infoglue.cms.applications.databeans.ComponentPropertyDiff.PropertyDiffState;
import org.infoglue.cms.applications.databeans.DiffTree;
import org.infoglue.cms.exception.SystemException;
import org.infoglue.deliver.applications.actions.InfoGlueComponent;
import org.junit.Before;
import org.junit.Test;

public class DiffControllerTest
{
	DiffController controller;

	@Before
	public void initClassUnderTest()
	{
		controller = new DiffController();
	}
	
	private InfoGlueComponent setupComponent(int id, int contentId, String name)
	{
		InfoGlueComponent baseFirstChildComp = new InfoGlueComponent();
		baseFirstChildComp.setContentId(contentId);
		baseFirstChildComp.setId(id);
		baseFirstChildComp.setName(name);
		return baseFirstChildComp;
	}

	private void addComponentToList(ArrayList<InfoGlueComponent> baseList, int id, int contentId, String name)
	{
		InfoGlueComponent baseFirstChildComp = setupComponent(id, contentId, name);
		baseList.add(baseFirstChildComp);
	}

	@SuppressWarnings("unchecked")
	private Map<String, Object> getProperty(InfoGlueComponent comp, String name, String path, String type)
	{
		Map<String, Object> property = new HashMap<String, Object>();
		property.put("name", name);
		property.put("path", path);
		property.put("type", type);
		comp.getProperties().put(name, property);
		return property;
	}

	@Test
	public void onlyBase()
	{
		ArrayList<InfoGlueComponent> baseList = new ArrayList<InfoGlueComponent>();

		ArrayList<InfoGlueComponent> newList = new ArrayList<InfoGlueComponent>();

		DiffTree diffTree = new DiffTree();
		controller.computeSubTree(diffTree, baseList, newList);

		assertEquals("The DiffTree should contain one child", 0, diffTree.getChildren().size());
	}

	@Test
	public void oneChildNoDiff()
	{
		ArrayList<InfoGlueComponent> baseList = new ArrayList<InfoGlueComponent>();
		addComponentToList(baseList, 1, 2, "apa");

		ArrayList<InfoGlueComponent> newList = new ArrayList<InfoGlueComponent>();
		addComponentToList(newList, 1, 2, "apa");

		DiffTree diffTree = new DiffTree();
		controller.computeSubTree(diffTree, baseList, newList);

		assertEquals("The DiffTree should contain one child", 1, diffTree.getChildren().size());
		DiffTree subTree = diffTree.getChildren().iterator().next();
		assertEquals(DiffTree.DiffState.SAME, subTree.getState());
		assertEquals("apa", subTree.getNodeName());
	}

	@Test
	public void twoChildNoDiff()
	{
		ArrayList<InfoGlueComponent> baseList = new ArrayList<InfoGlueComponent>();
		addComponentToList(baseList, 1, 2, "apa");
		addComponentToList(baseList, 2, 3, "bepa");

		ArrayList<InfoGlueComponent> newList = new ArrayList<InfoGlueComponent>();
		addComponentToList(newList, 1, 2, "apa");
		addComponentToList(newList, 2, 3, "bepa");

		DiffTree diffTree = new DiffTree();
		controller.computeSubTree(diffTree, baseList, newList);

		assertEquals("The DiffTree should contain one child", 2, diffTree.getChildren().size());
		Iterator<DiffTree> it = diffTree.getChildren().iterator();
		DiffTree subTree = it.next();
		assertEquals(DiffTree.DiffState.SAME, subTree.getState());
		assertEquals("apa", subTree.getNodeName());
		subTree = it.next();
		assertEquals(DiffTree.DiffState.SAME, subTree.getState());
		assertEquals("bepa", subTree.getNodeName());
	}

	@Test
	public void oneChildRemove()
	{
		ArrayList<InfoGlueComponent> baseList = new ArrayList<InfoGlueComponent>();
		addComponentToList(baseList, 1, 2, "apa");

		ArrayList<InfoGlueComponent> newList = new ArrayList<InfoGlueComponent>();

		DiffTree diffTree = new DiffTree();
		controller.computeSubTree(diffTree, baseList, newList);

		assertEquals("The DiffTree should contain one child", 1, diffTree.getChildren().size());
		DiffTree subTree = diffTree.getChildren().iterator().next();
		assertEquals(DiffTree.DiffState.CURRENT, subTree.getState());
		assertEquals("apa", subTree.getNodeName());
	}

	@Test
	public void oneChildAdd()
	{
		ArrayList<InfoGlueComponent> baseList = new ArrayList<InfoGlueComponent>();

		ArrayList<InfoGlueComponent> newList = new ArrayList<InfoGlueComponent>();
		addComponentToList(newList, 1, 2, "apa");

		DiffTree diffTree = new DiffTree();
		controller.computeSubTree(diffTree, baseList, newList);

		assertEquals("The DiffTree should contain one child", 1, diffTree.getChildren().size());
		DiffTree subTree = diffTree.getChildren().iterator().next();
		assertEquals(DiffTree.DiffState.NEW, subTree.getState());
		assertEquals("apa", subTree.getNodeName());
	}

	@Test
	public void oneBaseOneChildAdd()
	{
		ArrayList<InfoGlueComponent> baseList = new ArrayList<InfoGlueComponent>();
		addComponentToList(baseList, 1, 2, "apa");

		ArrayList<InfoGlueComponent> newList = new ArrayList<InfoGlueComponent>();
		addComponentToList(newList, 1, 2, "apa");
		addComponentToList(newList, 2, 3, "bepa");

		DiffTree diffTree = new DiffTree();
		controller.computeSubTree(diffTree, baseList, newList);

		assertEquals("The DiffTree should contain one child", 2, diffTree.getChildren().size());
		Iterator<DiffTree> it = diffTree.getChildren().iterator();
		DiffTree subTree = it.next();
		assertEquals(DiffTree.DiffState.SAME, subTree.getState());
		assertEquals("apa", subTree.getNodeName());
		DiffTree subTree2 = it.next();
		assertEquals(DiffTree.DiffState.NEW, subTree2.getState());
		assertEquals("bepa", subTree2.getNodeName());
	}

	@Test
	public void oneBaseOneChildRemove()
	{
		ArrayList<InfoGlueComponent> baseList = new ArrayList<InfoGlueComponent>();
		addComponentToList(baseList, 1, 2, "apa");
		addComponentToList(baseList, 2, 3, "bepa");

		ArrayList<InfoGlueComponent> newList = new ArrayList<InfoGlueComponent>();
		addComponentToList(newList, 1, 2, "apa");

		DiffTree diffTree = new DiffTree();
		controller.computeSubTree(diffTree, baseList, newList);

		assertEquals("The DiffTree should contain one child", 2, diffTree.getChildren().size());
		Iterator<DiffTree> it = diffTree.getChildren().iterator();
		DiffTree subTree = it.next();
		assertEquals(DiffTree.DiffState.SAME, subTree.getState());
		assertEquals("apa", subTree.getNodeName());
		DiffTree subTree2 = it.next();
		assertEquals(DiffTree.DiffState.CURRENT, subTree2.getState());
		assertEquals("bepa", subTree2.getNodeName());
	}

	@Test
	public void threeBaseMiddleRemoval()
	{
		ArrayList<InfoGlueComponent> baseList = new ArrayList<InfoGlueComponent>();
		addComponentToList(baseList, 1, 2, "apa");
		addComponentToList(baseList, 2, 20, "bepa");
		addComponentToList(baseList, 3, 200, "cepa");

		ArrayList<InfoGlueComponent> newList = new ArrayList<InfoGlueComponent>();
		addComponentToList(newList, 1, 2, "apa");
//		addComponentToList(newList, 1, 2, "apa");
		addComponentToList(newList, 3, 200, "cepa");

		DiffTree diffTree = new DiffTree();
		controller.computeSubTree(diffTree, baseList, newList);

		assertEquals("The DiffTree should contain three child. " + diffTree.getChildren(), 3, diffTree.getChildren().size());
		Iterator<DiffTree> it = diffTree.getChildren().iterator();
		DiffTree subTree = it.next();
		assertEquals(DiffTree.DiffState.SAME, subTree.getState());
		assertEquals("apa", subTree.getNodeName());
		subTree = it.next();
		assertEquals(DiffTree.DiffState.CURRENT, subTree.getState());
		assertEquals("bepa", subTree.getNodeName());
		subTree = it.next();
		assertEquals(DiffTree.DiffState.SAME, subTree.getState());
		assertEquals("cepa", subTree.getNodeName());
	}

	@Test
	public void threeBaseMiddleAdd()
	{
		ArrayList<InfoGlueComponent> baseList = new ArrayList<InfoGlueComponent>();
		addComponentToList(baseList, 1, 2, "apa");
//		addComponentToList(baseList, 2, 20, "bepa");
		addComponentToList(baseList, 3, 200, "cepa");

		ArrayList<InfoGlueComponent> newList = new ArrayList<InfoGlueComponent>();
		addComponentToList(newList, 1, 2, "apa");
		addComponentToList(newList, 1, 2, "bepa");
		addComponentToList(newList, 3, 200, "cepa");

		DiffTree diffTree = new DiffTree();
		controller.computeSubTree(diffTree, baseList, newList);

		assertEquals("The DiffTree should contain three child. " + diffTree.getChildren(), 3, diffTree.getChildren().size());
		Iterator<DiffTree> it = diffTree.getChildren().iterator();
		DiffTree subTree = it.next();
		assertEquals(DiffTree.DiffState.SAME, subTree.getState());
		assertEquals("apa", subTree.getNodeName());
		subTree = it.next();
		assertEquals(DiffTree.DiffState.NEW, subTree.getState());
		assertEquals("bepa", subTree.getNodeName());
		subTree = it.next();
		assertEquals(DiffTree.DiffState.SAME, subTree.getState());
		assertEquals("cepa", subTree.getNodeName());
	}

	@Test
	public void threeBaseAllRemove()
	{
		ArrayList<InfoGlueComponent> baseList = new ArrayList<InfoGlueComponent>();
		addComponentToList(baseList, 1, 2, "apa");
		addComponentToList(baseList, 2, 20, "bepa");
		addComponentToList(baseList, 3, 200, "cepa");

		ArrayList<InfoGlueComponent> newList = new ArrayList<InfoGlueComponent>();
//		addComponentToList(newList, 1, 2, "apa");
//		addComponentToList(newList, 1, 2, "bepa");
//		addComponentToList(newList, 3, 200, "cepa");

		DiffTree diffTree = new DiffTree();
		controller.computeSubTree(diffTree, baseList, newList);

		assertEquals("The DiffTree should contain three children. " + diffTree.getChildren(), 3, diffTree.getChildren().size());
		Iterator<DiffTree> it = diffTree.getChildren().iterator();
		DiffTree subTree = it.next();
		assertEquals(DiffTree.DiffState.CURRENT, subTree.getState());
		assertEquals("apa", subTree.getNodeName());
		subTree = it.next();
		assertEquals(DiffTree.DiffState.CURRENT, subTree.getState());
		assertEquals("bepa", subTree.getNodeName());
		subTree = it.next();
		assertEquals(DiffTree.DiffState.CURRENT, subTree.getState());
		assertEquals("cepa", subTree.getNodeName());
	}

	@Test
	public void noneBaseThreeAdd()
	{
		ArrayList<InfoGlueComponent> baseList = new ArrayList<InfoGlueComponent>();
//		addComponentToList(baseList, 1, 2, "apa");
//		addComponentToList(baseList, 2, 20, "bepa");
//		addComponentToList(baseList, 3, 200, "cepa");

		ArrayList<InfoGlueComponent> newList = new ArrayList<InfoGlueComponent>();
		addComponentToList(newList, 1, 2, "apa");
		addComponentToList(newList, 1, 2, "bepa");
		addComponentToList(newList, 3, 200, "cepa");

		DiffTree diffTree = new DiffTree();
		controller.computeSubTree(diffTree, baseList, newList);

		assertEquals("The DiffTree should contain three children. " + diffTree.getChildren(), 3, diffTree.getChildren().size());
		Iterator<DiffTree> it = diffTree.getChildren().iterator();
		DiffTree subTree = it.next();
		assertEquals(DiffTree.DiffState.NEW, subTree.getState());
		assertEquals("apa", subTree.getNodeName());
		subTree = it.next();
		assertEquals(DiffTree.DiffState.NEW, subTree.getState());
		assertEquals("bepa", subTree.getNodeName());
		subTree = it.next();
		assertEquals(DiffTree.DiffState.NEW, subTree.getState());
		assertEquals("cepa", subTree.getNodeName());
	}

	@Test
	public void subAdd()
	{
		ArrayList<InfoGlueComponent> baseList = new ArrayList<InfoGlueComponent>();
		InfoGlueComponent comp = setupComponent(1, 2, "apa");
		baseList.add(comp);

		ArrayList<InfoGlueComponent> newList = new ArrayList<InfoGlueComponent>();
		InfoGlueComponent newComp = setupComponent(1, 2, "apa");
		InfoGlueComponent subComp = setupComponent(10, 5, "depa");
		newComp.setComponents(Collections.singletonMap("depa", subComp));
		newList.add(newComp);

		DiffTree diffTree = new DiffTree();
		controller.computeSubTree(diffTree, baseList, newList);

		assertEquals("The DiffTree should contain one child. " + diffTree.getChildren(), 1, diffTree.getChildren().size());
		Iterator<DiffTree> it = diffTree.getChildren().iterator();
		DiffTree subTree = it.next();
		assertEquals(DiffTree.DiffState.SAME, subTree.getState());
		assertEquals("apa", subTree.getNodeName());
		assertTrue(subTree.getIsBranch());
		assertEquals(1, subTree.getChildren().size());

		Iterator<DiffTree> it2 = subTree.getChildren().iterator();
		DiffTree subSubTree = it2.next();
		assertEquals(DiffTree.DiffState.NEW, subSubTree.getState());
		assertEquals("depa", subSubTree.getNodeName());
	}

	@Test
	public void addWithSubTree()
	{
		ArrayList<InfoGlueComponent> baseList = new ArrayList<InfoGlueComponent>();

		ArrayList<InfoGlueComponent> newList = new ArrayList<InfoGlueComponent>();
		InfoGlueComponent comp = setupComponent(1, 2, "apa");
		InfoGlueComponent subComp = setupComponent(10, 5, "depa");
		comp.setComponents(Collections.singletonMap("depa", subComp));
		newList.add(comp);

		DiffTree diffTree = new DiffTree();
		controller.computeSubTree(diffTree, baseList, newList);

		assertEquals("The DiffTree should contain one child. " + diffTree.getChildren(), 1, diffTree.getChildren().size());
		Iterator<DiffTree> it = diffTree.getChildren().iterator();
		DiffTree subTree = it.next();
		assertEquals(DiffTree.DiffState.NEW, subTree.getState());
		assertEquals("apa", subTree.getNodeName());
		assertTrue(subTree.getIsBranch());
		assertEquals(1, subTree.getChildren().size());

		Iterator<DiffTree> it2 = subTree.getChildren().iterator();
		DiffTree subSubTree = it2.next();
		assertEquals(DiffTree.DiffState.NEW, subSubTree.getState());
		assertEquals("depa", subSubTree.getNodeName());
	}

	@Test
	public void removeWithSubTree()
	{
		ArrayList<InfoGlueComponent> baseList = new ArrayList<InfoGlueComponent>();
		InfoGlueComponent comp = setupComponent(1, 2, "apa");
		InfoGlueComponent subComp = setupComponent(10, 5, "depa");
		comp.setComponents(Collections.singletonMap("depa", subComp));
		baseList.add(comp);

		ArrayList<InfoGlueComponent> newList = new ArrayList<InfoGlueComponent>();

		DiffTree diffTree = new DiffTree();
		controller.computeSubTree(diffTree, baseList, newList);

		assertEquals("The DiffTree should contain one child. " + diffTree.getChildren(), 1, diffTree.getChildren().size());
		Iterator<DiffTree> it = diffTree.getChildren().iterator();
		DiffTree subTree = it.next();
		assertEquals(DiffTree.DiffState.CURRENT, subTree.getState());
		assertEquals("apa", subTree.getNodeName());
		assertTrue(subTree.getIsBranch());
		assertEquals(1, subTree.getChildren().size());

		Iterator<DiffTree> it2 = subTree.getChildren().iterator();
		DiffTree subSubTree = it2.next();
		assertEquals(DiffTree.DiffState.CURRENT, subSubTree.getState());
		assertEquals("depa", subSubTree.getNodeName());
	}


	@Test
	public void parseSingleComponent() throws SystemException
	{
		String componentStructure = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><components><component contentId=\"123\" id=\"0\" name=\"base\"><properties></properties><bindings></bindings><components></components></component></components>";

		InfoGlueComponent comp = controller.getComponentTree(componentStructure);

		assertEquals("base", comp.getSlotName());
		assertEquals(new Integer(123), comp.getContentId());
		assertEquals(new Integer(0), comp.getId());
		assertEquals(0, comp.getComponents().size());
	}

	@Test
	public void parseSingleComponentWithOneChild() throws SystemException
	{
		String componentStructure = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><components><component contentId=\"123\" id=\"0\" name=\"base\"><properties></properties><bindings></bindings><components><component contentId=\"456\" id=\"1\" name=\"bodyarea\"><properties></properties><components></components></component></components></component></components>";

		InfoGlueComponent comp = controller.getComponentTree(componentStructure);

		assertEquals("base", comp.getSlotName());
		assertEquals(new Integer(123), comp.getContentId());
		assertEquals(new Integer(0), comp.getId());
		assertEquals(1, comp.getComponents().size());
		InfoGlueComponent subComp = (InfoGlueComponent)comp.getComponents().values().iterator().next();
		assertEquals("bodyarea", subComp.getSlotName());
		assertEquals(new Integer(456), subComp.getContentId());
		assertEquals(new Integer(1), subComp.getId());
		assertEquals(0, subComp.getComponents().size());
	}

	@Test
	public void parseSingleComponentWithTwoChildren() throws SystemException
	{
		String componentStructure = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><components><component contentId=\"123\" id=\"0\" name=\"base\"><properties></properties><bindings></bindings><components><component contentId=\"456\" id=\"1\" name=\"bodyarea\"><properties></properties><components></components></component><component contentId=\"789\" id=\"2\" name=\"bodyarea\"><properties></properties><components></components></component></components></component></components>";

		InfoGlueComponent comp = controller.getComponentTree(componentStructure);

		assertEquals("base", comp.getSlotName());
		assertEquals(new Integer(123), comp.getContentId());
		assertEquals(new Integer(0), comp.getId());
		assertEquals(2, comp.getComponents().size());
		@SuppressWarnings("unchecked")
		Iterator<InfoGlueComponent> it = comp.getComponents().values().iterator();
		InfoGlueComponent subComp = (InfoGlueComponent)it.next();
		assertEquals("bodyarea", subComp.getSlotName());
		assertEquals(new Integer(456), subComp.getContentId());
		assertEquals(new Integer(1), subComp.getId());
		assertEquals(0, subComp.getComponents().size());
		InfoGlueComponent subComp2 = (InfoGlueComponent)it.next();
		assertEquals("bodyarea", subComp2.getSlotName());
		assertEquals(new Integer(789), subComp2.getContentId());
		assertEquals(new Integer(2), subComp2.getId());
		assertEquals(0, subComp2.getComponents().size());
	}

	@SuppressWarnings("unchecked")
	@Test
	public void parseSingleComponentWithOneProperty() throws SystemException
	{
		String componentStructure = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><components><component contentId=\"123\" id=\"0\" name=\"base\"><properties><property name=\"oneProp\" path=\"foo\" type=\"textfield\"></property></properties><bindings></bindings><components></components></component></components>";

		InfoGlueComponent comp = controller.getComponentTree(componentStructure);

		assertEquals("base", comp.getSlotName());
		assertEquals(new Integer(123), comp.getContentId());
		assertEquals(new Integer(0), comp.getId());
		assertEquals(0, comp.getComponents().size());
		assertTrue(comp.getProperties().containsKey("oneProp"));
		assertProperty(comp.getProperties(), "oneProp", "foo", "textfield");
	}

	@SuppressWarnings("unchecked")
	@Test
	public void parseSingleComponentWithTwoProperties() throws SystemException
	{
		String componentStructure = "<?xml version=\"1.0\" encoding=\"UTF-8\"?><components><component contentId=\"123\" id=\"0\" name=\"base\"><properties><property name=\"oneProp\" path=\"foo\" type=\"textfield\"></property><property name=\"twoProp\" path=\"bar\" type=\"textfield\"></property></properties><bindings></bindings><components></components></component></components>";

		InfoGlueComponent comp = controller.getComponentTree(componentStructure);

		assertEquals("base", comp.getSlotName());
		assertEquals(new Integer(123), comp.getContentId());
		assertEquals(new Integer(0), comp.getId());
		assertEquals(0, comp.getComponents().size());
		assertTrue(comp.getProperties().containsKey("oneProp"));
		assertProperty(comp.getProperties(), "oneProp", "foo", "textfield");
		assertProperty(comp.getProperties(), "twoProp", "bar", "textfield");
	}

	@Test
	public void computePropertiesDiffNoProperties() throws SystemException
	{
		DiffTree diffTree = new DiffTree();
		InfoGlueComponent baseComponent = new InfoGlueComponent();
		InfoGlueComponent newComponent = new InfoGlueComponent();

		controller.computePropertiesDiff(diffTree, baseComponent, newComponent);

		assertEquals(0, diffTree.getProperties().size());
	}

	@Test
	public void computePropertiesDiffAddOneProperty() throws SystemException
	{
		DiffTree diffTree = new DiffTree();
		InfoGlueComponent baseComponent = new InfoGlueComponent();
		InfoGlueComponent newComponent = new InfoGlueComponent();

		getProperty(newComponent, "foo", "bar", "textfield");

		controller.computePropertiesDiff(diffTree, baseComponent, newComponent);

		assertEquals(1, diffTree.getProperties().size());
		assertComponentPropertyDiff(getComponentPropertyDiffNamed(diffTree.getProperties(), "foo"), null, "bar", "foo", PropertyDiffState.NEW);
	}

	@Test
	public void computePropertiesDiffRemoveOneProperty() throws SystemException
	{
		DiffTree diffTree = new DiffTree();
		InfoGlueComponent baseComponent = new InfoGlueComponent();
		InfoGlueComponent newComponent = new InfoGlueComponent();

		getProperty(baseComponent, "foo", "bar", "textfield");

		controller.computePropertiesDiff(diffTree, baseComponent, newComponent);

		assertEquals(1, diffTree.getProperties().size());
		assertComponentPropertyDiff(getComponentPropertyDiffNamed(diffTree.getProperties(), "foo"), "bar", null, "foo", PropertyDiffState.CURRENT);
	}

	@Test
	public void computePropertiesDiffChangeOneProperty() throws SystemException
	{
		DiffTree diffTree = new DiffTree();
		InfoGlueComponent baseComponent = new InfoGlueComponent();
		InfoGlueComponent newComponent = new InfoGlueComponent();

		getProperty(baseComponent, "foo", "bar", "textfield");
		getProperty(newComponent, "foo", "barium", "textfield");

		controller.computePropertiesDiff(diffTree, baseComponent, newComponent);

		assertEquals(1, diffTree.getProperties().size());
		assertComponentPropertyDiff(getComponentPropertyDiffNamed(diffTree.getProperties(), "foo"), "bar", "barium", "foo", PropertyDiffState.CHANGED);
	}

	@Test
	public void computePropertiesDiffOneProperty() throws SystemException
	{
		DiffTree diffTree = new DiffTree();
		InfoGlueComponent baseComponent = new InfoGlueComponent();
		InfoGlueComponent newComponent = new InfoGlueComponent();

		getProperty(baseComponent, "foo", "bar", "textfield");
		getProperty(newComponent, "foo", "bar", "textfield");

		controller.computePropertiesDiff(diffTree, baseComponent, newComponent);

		assertEquals(1, diffTree.getProperties().size());
		assertComponentPropertyDiff(getComponentPropertyDiffNamed(diffTree.getProperties(), "foo"), "bar", "bar", "foo", PropertyDiffState.SAME);
	}

	@Test
	public void computePropertiesDiffOnePropertyOneAdd() throws SystemException
	{
		DiffTree diffTree = new DiffTree();
		InfoGlueComponent baseComponent = new InfoGlueComponent();
		InfoGlueComponent newComponent = new InfoGlueComponent();

		getProperty(baseComponent, "foo", "bar", "textfield");
		getProperty(newComponent, "foo", "bar", "textfield");
		getProperty(newComponent, "foorium", "bar2", "textfield");

		controller.computePropertiesDiff(diffTree, baseComponent, newComponent);

		assertEquals(2, diffTree.getProperties().size());
		assertComponentPropertyDiff(getComponentPropertyDiffNamed(diffTree.getProperties(), "foo"), "bar", "bar", "foo", PropertyDiffState.SAME);
		assertComponentPropertyDiff(getComponentPropertyDiffNamed(diffTree.getProperties(), "foorium"), null, "bar2", "foorium", PropertyDiffState.NEW);
	}

	@Test
	public void computePropertiesDiffOnePropertyOneRemove() throws SystemException
	{
		DiffTree diffTree = new DiffTree();
		InfoGlueComponent baseComponent = new InfoGlueComponent();
		InfoGlueComponent newComponent = new InfoGlueComponent();

		getProperty(baseComponent, "foo", "bar", "textfield");
		getProperty(baseComponent, "foorium", "bar2", "textfield");
		getProperty(newComponent, "foo", "bar", "textfield");

		controller.computePropertiesDiff(diffTree, baseComponent, newComponent);

		assertEquals(2, diffTree.getProperties().size());
		assertComponentPropertyDiff(getComponentPropertyDiffNamed(diffTree.getProperties(), "foo"), "bar", "bar", "foo", PropertyDiffState.SAME);
		assertComponentPropertyDiff(getComponentPropertyDiffNamed(diffTree.getProperties(), "foorium"), "bar2", null, "foorium", PropertyDiffState.CURRENT);
	}

	@Test
	public void computePropertiesDiffOnePropertyOneChanged() throws SystemException
	{
		DiffTree diffTree = new DiffTree();
		InfoGlueComponent baseComponent = new InfoGlueComponent();
		InfoGlueComponent newComponent = new InfoGlueComponent();

		getProperty(baseComponent, "foo", "bar", "textfield");
		getProperty(baseComponent, "foorium", "bar2", "textfield");
		getProperty(newComponent, "foo", "bar", "textfield");
		getProperty(newComponent, "foorium", "barium2", "textfield");

		controller.computePropertiesDiff(diffTree, baseComponent, newComponent);

		assertEquals(2, diffTree.getProperties().size());
		assertComponentPropertyDiff(getComponentPropertyDiffNamed(diffTree.getProperties(), "foo"), "bar", "bar", "foo", PropertyDiffState.SAME);
		assertComponentPropertyDiff(getComponentPropertyDiffNamed(diffTree.getProperties(), "foorium"), "bar2", "barium2", "foorium", PropertyDiffState.CHANGED);
	}

	@Test
	public void computePropertiesDiffTwoPropertyOneRemoved() throws SystemException
	{
		DiffTree diffTree = new DiffTree();
		InfoGlueComponent baseComponent = new InfoGlueComponent();
		InfoGlueComponent newComponent = new InfoGlueComponent();

		/* Properties are sorted before compare. In order to make a remove in the middle happen
		 * we need to make sure we know the order in which the properties will be compared
		 */
		getProperty(baseComponent, "A", "bar", "textfield");
		getProperty(baseComponent, "B", "bar2", "textfield");
		getProperty(baseComponent, "C", "bar3", "textfield");

		getProperty(newComponent, "A", "bar", "textfield");
		getProperty(newComponent, "C", "bar3", "textfield");

		controller.computePropertiesDiff(diffTree, baseComponent, newComponent);

		assertEquals(3, diffTree.getProperties().size());
		assertComponentPropertyDiff(getComponentPropertyDiffNamed(diffTree.getProperties(), "A"), "bar", "bar", "A", PropertyDiffState.SAME);
		assertComponentPropertyDiff(getComponentPropertyDiffNamed(diffTree.getProperties(), "B"), "bar2", null, "B", PropertyDiffState.CURRENT);
		assertComponentPropertyDiff(getComponentPropertyDiffNamed(diffTree.getProperties(), "C"), "bar3", "bar3", "C", PropertyDiffState.SAME);
	}

	@Test
	public void computePropertiesDiffTwoPropertyOneAdd() throws SystemException
	{
		DiffTree diffTree = new DiffTree();
		InfoGlueComponent baseComponent = new InfoGlueComponent();
		InfoGlueComponent newComponent = new InfoGlueComponent();

		/* Properties are sorted before compare. In order to make a remove in the middle happen
		 * we need to make sure we know the order in which the properties will be compared
		 */
		getProperty(baseComponent, "A", "bar", "textfield");
		getProperty(baseComponent, "C", "bar3", "textfield");

		getProperty(newComponent, "A", "bar", "textfield");
		getProperty(newComponent, "B", "bar2", "textfield");
		getProperty(newComponent, "C", "bar3", "textfield");

		controller.computePropertiesDiff(diffTree, baseComponent, newComponent);

		assertEquals(3, diffTree.getProperties().size());
		assertComponentPropertyDiff(getComponentPropertyDiffNamed(diffTree.getProperties(), "A"), "bar", "bar", "A", PropertyDiffState.SAME);
		assertComponentPropertyDiff(getComponentPropertyDiffNamed(diffTree.getProperties(), "B"), null, "bar2", "B", PropertyDiffState.NEW);
		assertComponentPropertyDiff(getComponentPropertyDiffNamed(diffTree.getProperties(), "C"), "bar3", "bar3", "C", PropertyDiffState.SAME);
	}

	@Test
	public void computePropertiesDiffTwoPropertyOneChanged() throws SystemException
	{
		DiffTree diffTree = new DiffTree();
		InfoGlueComponent baseComponent = new InfoGlueComponent();
		InfoGlueComponent newComponent = new InfoGlueComponent();

		/* Properties are sorted before compare. In order to make a remove in the middle happen
		 * we need to make sure we know the order in which the properties will be compared
		 */
		getProperty(baseComponent, "A", "bar", "textfield");
		getProperty(baseComponent, "B", "bar2", "textfield");
		getProperty(baseComponent, "C", "bar3", "textfield");

		getProperty(newComponent, "A", "bar", "textfield");
		getProperty(newComponent, "B", "barium2", "textfield");
		getProperty(newComponent, "C", "bar3", "textfield");

		controller.computePropertiesDiff(diffTree, baseComponent, newComponent);

		assertEquals(3, diffTree.getProperties().size());
		assertComponentPropertyDiff(getComponentPropertyDiffNamed(diffTree.getProperties(), "A"), "bar", "bar", "A", PropertyDiffState.SAME);
		assertComponentPropertyDiff(getComponentPropertyDiffNamed(diffTree.getProperties(), "B"), "bar2", "barium2", "B", PropertyDiffState.CHANGED);
		assertComponentPropertyDiff(getComponentPropertyDiffNamed(diffTree.getProperties(), "C"), "bar3", "bar3", "C", PropertyDiffState.SAME);
	}

	@Test
	public void computePropertiesDiffMiddledReplaced() throws SystemException
	{
		DiffTree diffTree = new DiffTree();
		InfoGlueComponent baseComponent = new InfoGlueComponent();
		InfoGlueComponent newComponent = new InfoGlueComponent();

		/* Properties are sorted before compare. In order to make a remove in the middle happen
		 * we need to make sure we know the order in which the properties will be compared
		 */
		getProperty(baseComponent, "A", "bar", "textfield");
		getProperty(baseComponent, "B", "bar2", "textfield");
		getProperty(baseComponent, "D", "bar4", "textfield");

		getProperty(newComponent, "A", "bar", "textfield");
		getProperty(newComponent, "C", "bar3", "textfield");
		getProperty(newComponent, "D", "bar4", "textfield");

		controller.computePropertiesDiff(diffTree, baseComponent, newComponent);

		assertEquals(4, diffTree.getProperties().size());
		assertComponentPropertyDiff(getComponentPropertyDiffNamed(diffTree.getProperties(), "A"), "bar", "bar", "A", PropertyDiffState.SAME);
		assertComponentPropertyDiff(getComponentPropertyDiffNamed(diffTree.getProperties(), "B"), "bar2", null, "B", PropertyDiffState.CURRENT);
		assertComponentPropertyDiff(getComponentPropertyDiffNamed(diffTree.getProperties(), "C"), null, "bar3", "C", PropertyDiffState.NEW);
		assertComponentPropertyDiff(getComponentPropertyDiffNamed(diffTree.getProperties(), "D"), "bar4", "bar4", "D", PropertyDiffState.SAME);
	}

	@Test
	public void computePropertiesDiffMiddledReplacedLastChanged() throws SystemException
	{
		DiffTree diffTree = new DiffTree();
		InfoGlueComponent baseComponent = new InfoGlueComponent();
		InfoGlueComponent newComponent = new InfoGlueComponent();

		/* Properties are sorted before compare. In order to make a remove in the middle happen
		 * we need to make sure we know the order in which the properties will be compared
		 */
		getProperty(baseComponent, "A", "bar", "textfield");
		getProperty(baseComponent, "B", "bar2", "textfield");
		getProperty(baseComponent, "D", "bar4", "textfield");

		getProperty(newComponent, "A", "bar", "textfield");
		getProperty(newComponent, "C", "bar3", "textfield");
		getProperty(newComponent, "D", "barium4", "textfield");

		controller.computePropertiesDiff(diffTree, baseComponent, newComponent);

		assertEquals(4, diffTree.getProperties().size());
		assertComponentPropertyDiff(getComponentPropertyDiffNamed(diffTree.getProperties(), "A"), "bar", "bar", "A", PropertyDiffState.SAME);
		assertComponentPropertyDiff(getComponentPropertyDiffNamed(diffTree.getProperties(), "B"), "bar2", null, "B", PropertyDiffState.CURRENT);
		assertComponentPropertyDiff(getComponentPropertyDiffNamed(diffTree.getProperties(), "C"), null, "bar3", "C", PropertyDiffState.NEW);
		assertComponentPropertyDiff(getComponentPropertyDiffNamed(diffTree.getProperties(), "D"), "bar4", "barium4", "D", PropertyDiffState.CHANGED);
	}

	@Test
	public void computePropertiesDiffLastReplaced() throws SystemException
	{
		DiffTree diffTree = new DiffTree();
		InfoGlueComponent baseComponent = new InfoGlueComponent();
		InfoGlueComponent newComponent = new InfoGlueComponent();

		/* Properties are sorted before compare. In order to make a remove in the middle happen
		 * we need to make sure we know the order in which the properties will be compared
		 */
		getProperty(baseComponent, "A", "bar", "textfield");
		getProperty(baseComponent, "B", "bar2", "textfield");

		getProperty(newComponent, "A", "bar", "textfield");
		getProperty(newComponent, "C", "bar3", "textfield");

		controller.computePropertiesDiff(diffTree, baseComponent, newComponent);

		assertEquals(3, diffTree.getProperties().size());
		assertComponentPropertyDiff(getComponentPropertyDiffNamed(diffTree.getProperties(), "A"), "bar", "bar", "A", PropertyDiffState.SAME);
		assertComponentPropertyDiff(getComponentPropertyDiffNamed(diffTree.getProperties(), "B"), "bar2", null, "B", PropertyDiffState.CURRENT);
		assertComponentPropertyDiff(getComponentPropertyDiffNamed(diffTree.getProperties(), "C"), null, "bar3", "C", PropertyDiffState.NEW);
	}






	private void assertProperty(Map<String, Object> properties, String name, String path, String type)
	{
		@SuppressWarnings("unchecked")
		Map<String, Object> prop = (Map<String, Object>) properties.get(name);
		assertEquals(name, prop.get("name"));
		assertEquals(path, prop.get("path"));
		assertEquals(type, prop.get("type"));
	}

	private void assertComponentPropertyDiff(ComponentPropertyDiff propDiff, String baseValue, String newValue, String name, PropertyDiffState state)
	{
		assertEquals(state, propDiff.getState());
		assertEquals(baseValue, propDiff.getBaseValue());
		assertEquals(newValue, propDiff.getNewValue());
		assertEquals(name, propDiff.getPropertyName());
	}

	private ComponentPropertyDiff getComponentPropertyDiffNamed(List<ComponentPropertyDiff> list, String name)
	{
		for (ComponentPropertyDiff cpd : list)
		{
			if (cpd.getPropertyName().equals(name))
			{
				return cpd;
			}
		}
		return null;
	}
}



