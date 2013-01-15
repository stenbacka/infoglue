package org.infoglue.deliver.taglib.log;

import javax.servlet.jsp.JspException;

import org.infoglue.deliver.taglib.AbstractTag;
import org.infoglue.deliver.util.RequestAnalyser;
import org.infoglue.deliver.util.Timer;

/**
 * Tag handler for the tag <em>infoglue-log#registerComponentStatistics</em>. The tag adds
 * (registers) the provided value for the provided component in the RequestAnalyser.
 * 
 * @author Erik Stenbacka <stenbacka@gmail.com>
 */
public class RegisterComponentStatisticsTag extends AbstractTag {
	private static final long serialVersionUID = 123483457474L;

	private String componentName;
	private Long value;

	@Override
	public int doEndTag() throws JspException
	{
		if (value == null)
		{
			throw new JspException("Must specify either 'value' or 'timer'");
		}
		
		RequestAnalyser.getRequestAnalyser().registerComponentStatistics(componentName, value);

		this.value = null;
		this.componentName = null;
		return EVAL_PAGE;
	}

	public void setComponentName(String componentName) throws JspException
	{
		this.componentName = evaluateString("registerComponentStatisticsTag", "componentName", componentName);
	}

	public void setValue(String value) throws JspException
	{
		this.value = (Long)evaluate("registerComponentStatisticsTag", "value", value, Long.class);
	}

	public void setTimer(String timer) throws JspException
	{
		Timer t = (Timer)evaluate("registerComponentStatisticsTag", "timer", timer, Timer.class);
		/* The timer's value is retrieved here to minimize the amount of time 
		 * added to the measurement from overhead in this class.
		 */
		this.value = t.getElapsedTime();
	}
}
