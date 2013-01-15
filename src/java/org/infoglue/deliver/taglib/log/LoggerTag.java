package org.infoglue.deliver.taglib.log;

import javax.servlet.jsp.JspException;

import org.apache.log4j.Logger;
import org.infoglue.deliver.taglib.TemplateControllerTag;

public class LoggerTag extends TemplateControllerTag {
	private static final long serialVersionUID = 1345534534535L;
	private static final Logger logger = Logger.getLogger(LoggerTag.class);

	private String componentName;
	
	@Override
	public int doEndTag() throws JspException {
		Logger componentLogger;
		if (componentName == null)
		{
			logger.info("Getting logger based on current component information");
			componentLogger = LoggerTagsUtil.getTagsUtil().getLogger(getController());
		}
		else
		{
			logger.info("Getting logger based on provided component name");
			componentLogger = LoggerTagsUtil.getTagsUtil().getLogger(this.componentName);
		}

		setResultAttribute(componentLogger);

		this.componentName = null;
		return EVAL_PAGE;
	}

	public void setComponentName(String componentName)
	{
		this.componentName = componentName;
	}
}
