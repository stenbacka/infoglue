package org.infoglue.deliver.taglib.log;

import javax.servlet.jsp.JspException;

import org.infoglue.deliver.taglib.AbstractTag;
import org.infoglue.deliver.util.Timer;

public class TimerTag extends AbstractTag {
	private static final long serialVersionUID = 8569578487L;

	@Override
	public int doEndTag() throws JspException {
		setResultAttribute(new Timer());
		return EVAL_PAGE;
	}

}
