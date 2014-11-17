package org.infoglue.deliver.taglib.log;

import org.apache.log4j.Logger;
import org.infoglue.cms.util.CmsPropertyHandler;
import org.infoglue.deliver.applications.actions.InfoGlueComponent;
import org.infoglue.deliver.controllers.kernel.impl.simple.TemplateController;

public class LoggerTagsUtil {
	private static final Logger logger = Logger.getLogger(LoggerTagsUtil.class);
	
	private static LoggerTagsUtil singleton;
	
	public static LoggerTagsUtil getTagsUtil()
	{
		if (singleton == null)
		{
			singleton = new LoggerTagsUtil();
		}
		return singleton;
	}
	
	private LoggerTagsUtil()
	{
	}
	
	/**
	 * Gets a Log4J {@link Logger} reference based on the given <em>componentName</em>. That is, the Logger's name will be the 
	 * component package name (InfoGlue property: infoglueComponentBasePackage) combined with the componentName (as a Java package).
	 * The name is strips of spaces and then lower cased before it is used to get a logger.
	 * @param componentName The name of the component. Will be appended to the base component package. If null the string "infoGlueComponent"
	 * will be used as the component name.
	 * @return A Log4J logger retrieved with {@link Logger#getLogger(String)}
	 */
	public Logger getLogger(String componentName)
	{
		if (componentName == null)
		{
			componentName = "infoGlueComponent";
		}
		String infoglueComponentBasePackage = CmsPropertyHandler.getInfoglueComponentLoggingBasePackage();
		componentName = componentName.replaceAll("[\\s]", "").toLowerCase();
		return Logger.getLogger(infoglueComponentBasePackage + "." + componentName);
	}

	/**
	 * Gets a Log4J {@link Logger} reference based on the current component in the TemplateController.
	 * If the component has a name it will be used in the Logger name (part of a package/path). If no name is available the ID is used together
	 * with a prefix "component_". The name is strips of spaces and then lower cased before it is used to get a logger.
	 * @param controller
	 * @return A Log4J logger retrieved with {@link Logger#getLogger(String)}
	 */
	public Logger getLogger(TemplateController controller)
	{
		String componentName = null;
		InfoGlueComponent component = controller.getComponentLogic().getInfoGlueComponent();
		componentName = component.getName();
		if (componentName == null || "".equals(componentName.trim()))
		{
			logger.info("Component had no name. Lets use the ID instead. Component-id: " + component.getId());
			componentName = "Component_" + component.getId();
		}
		componentName = componentName.replaceAll("[\\s]", "").toLowerCase();
		return getLogger(componentName);
	}
}
