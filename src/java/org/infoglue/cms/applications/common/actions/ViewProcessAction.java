package org.infoglue.cms.applications.common.actions;

import java.io.PrintWriter;

import org.apache.log4j.Logger;
import org.infoglue.cms.applications.databeans.ProcessBean;
import org.infoglue.cms.exception.AccessConstraintException;
import org.infoglue.cms.exception.SystemException;
import org.infoglue.cms.services.ProcessBeanService;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonObject;

public class ViewProcessAction extends InfoGlueAbstractAction
{
	private static final long serialVersionUID = -458460311294458003L;
	private final static Logger logger = Logger.getLogger(ViewProcessAction.class.getName());

	private String processId = null;
	private String processName = null;
	private boolean processBeanExists;

	public String doExecute() throws Exception
	{
		try
		{
			ProcessBean processBean = ProcessBeanService.getService().getProcessBean(processName, processId, getInfoGluePrincipal());
			this.processBeanExists = processBean != null;
			return "success";
		}
		catch (AccessConstraintException aex)
		{
			return "accessDenied";
		}
	}

	public String doGetProcessAsJSON() throws Exception
	{
		PrintWriter out = this.getResponse().getWriter();
		out.println(getProcessBeanAsJson());
		out.flush();
		out.close();
		return NONE;
	}

	public String getProcessBeanAsJson() throws SystemException
	{
		try
		{
			ProcessBean processBean = ProcessBeanService.getService().getProcessBean(processName, processId, getInfoGluePrincipal());
			return ProcessBeanService.getService().getProcessBeanAsJSON(processBean);
		}
		catch (Throwable tr)
		{
			if (tr instanceof AccessConstraintException)
			{
				logger.warn("User was not allowed to view process bean. There is no normal use case that should trigger this logic. ProcessName: " + this.processName + ", ProcessId: " + this.processId);
			}
			return getExceptionAsJSON(tr);
		}
	}

	protected String getExceptionAsJSON(Throwable tr)
	{
		Gson gson = new GsonBuilder().setDateFormat("dd MMM HH:mm:ss").create();
		JsonObject wrapper = new JsonObject();
		JsonObject error = new JsonObject(); 
		error.addProperty("message", tr.getMessage());
		error.addProperty("type", tr.getClass().getSimpleName());
		wrapper.add("error", error);
		return gson.toJson(wrapper);
	}

	public String getProcessId()
	{
		return processId;
	}

	public void setProcessId(String processId) 
	{
		this.processId = processId;
	}

	public String getProcessName()
	{
		return processName;
	}

	public void setProcessName(String processName)
	{
		this.processName = processName;
	}

	public boolean getProcessBeanExists()
	{
		return processBeanExists;
	}

}
