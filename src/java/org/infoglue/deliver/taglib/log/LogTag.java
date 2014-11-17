package org.infoglue.deliver.taglib.log;

import javax.servlet.jsp.JspException;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.infoglue.deliver.applications.actions.InfoGlueComponent;
import org.infoglue.deliver.controllers.kernel.impl.simple.TemplateController;
import org.infoglue.deliver.taglib.TemplateControllerTag;
import org.infoglue.deliver.util.Timer;

public class LogTag extends TemplateControllerTag
{
	private static final long serialVersionUID = 1675678575675L;
	private static final Logger tagLogger = Logger.getLogger(LogTag.class);

	private String message;
	private Throwable throwable;
	private Logger logger;
	private Level level;
	private String componentName;
	private Long timerValueNanos;
	private Long timerValue;

	@Override
	public int doEndTag() throws JspException
	{
		TemplateController controller = getController();
		InfoGlueComponent component = controller.getComponentLogic().getInfoGlueComponent();
		if (componentName != null)
		{
			this.logger = LoggerTagsUtil.getTagsUtil().getLogger(componentName);
		}
		if (this.logger == null)
		{
			this.logger = LoggerTagsUtil.getTagsUtil().getLogger(controller);
		}

		if (this.logger.isEnabledFor(this.level))
		{
			tagLogger.debug("Logger is enabled for level");

			if (timerValue != null)
			{
				MDC.put("timerValue", "Timer: " + timerValue + " ms");
			}
			if (timerValueNanos != null)
			{
				MDC.put("timerValue", "Timer: " + timerValueNanos + " ns");
			}

			MDC.put("siteNodeId", controller.getSiteNodeId());
			MDC.put("principalName", controller.getPrincipal().getName());
			MDC.put("languageId", controller.getLanguageId());
			MDC.put("componentId", component.getId());

			if (throwable == null)
			{
				this.logger.log(level, message);
			}
			else
			{
				this.logger.log(level, message, throwable);
			}

			// Remove values to prevent memory leaks
			MDC.remove("siteNodeId");
			MDC.remove("timerValue");
			MDC.remove("principalName");
			MDC.remove("languageId");
			MDC.remove("componentId");
		}

		this.message = null;
		this.throwable = null;
		this.logger = null;
		this.level = null;
		this.componentName = null;
		this.timerValue = null;
		this.timerValueNanos = null;
		return EVAL_PAGE;
	}

	public void setMessage(String message) throws JspException
	{
		this.message = evaluateString("LogTag", "message", message);
	}

	public void setThrowable(String throwable) throws JspException
	{
		this.throwable = (Throwable)evaluate("LogTag", "throwable", throwable, Throwable.class);
	}

	public void setLogger(String logger) throws JspException
	{
		this.logger = (Logger)evaluate("LogTag", "logger", logger, Logger.class);
	}

	public void setLevel(String level) throws JspException
	{
		String levelString = evaluateString("LogTag", "level", level);
		this.level = Level.toLevel(levelString);
	}

	public void setComponentName(String componentName) throws JspException
	{
		this.componentName = evaluateString("LogTag", "componentName", componentName);
	}

	public void setTimerValue(String timerValue) throws JspException
	{
		Timer t = (Timer)evaluate("LogTag", "timerValue", timerValue, Timer.class);
		this.timerValue = t.getElapsedTime();
	}

	public void setTimerValueNanos(String timerValueNanos) throws JspException
	{
		Timer t = (Timer)evaluate("LogTag", "timerValueNanos", timerValueNanos, Timer.class);
		this.timerValueNanos = t.getElapsedTimeNanos();
	}

}
