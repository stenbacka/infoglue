package org.infoglue.cms.applications.common.actions;

import java.io.PrintWriter;
import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;

import org.apache.log4j.Logger;
import org.infoglue.cms.applications.databeans.ProcessBean;
import org.infoglue.cms.exception.AccessConstraintException;
import org.infoglue.cms.exception.SystemException;
import org.infoglue.cms.services.ProcessBeanService;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.reflect.TypeToken;

public class ViewProcessAction extends InfoGlueAbstractAction
{
	private static final long serialVersionUID = -458460311294458003L;
	private final static Logger logger = Logger.getLogger(ViewProcessAction.class.getName());

	private String processId = null;
	private String processName = null;
	private int processStatus = -1;
	private String returnAddress = null;

	public String doExecute() throws Exception
	{
		try
		{
			ProcessBeanService.getService().getProcessBean(processName, processId, getInfoGluePrincipal());
			return "success";
		}
		catch (AccessConstraintException aex)
		{
			return "accessDenied";
		}
	}

	public String doListProcessNames() throws Exception
	{
		return "successList";
	}

	public String doShowProcessesAsJSON() throws Exception
	{
		PrintWriter out = this.getResponse().getWriter();
		out.println(getStatusAsJSON());
		out.flush();
		out.close();
		return NONE;
	}

	public void setProcessId(String processId) 
	{
		this.processId = processId;
	}

	public void setProcessStatus(String processStatusString)
	{
		if ("running".equals(processStatusString))
		{
			this.processStatus = ProcessBean.RUNNING;
		}
		else if ("finished".equals(processStatusString))
		{
			this.processStatus = ProcessBean.FINISHED;
		}
		else if ("error".equals(processStatusString))
		{
			this.processStatus = ProcessBean.ERROR;
		}
		else if ("notStarted".equals(processStatusString))
		{
			this.processStatus = ProcessBean.NOT_STARTED;
		}
		else
		{
			logger.warn("Got unknown process status parameter. Ignoring value. Parameter: " + processStatusString);
			this.processStatus = -1;
		}
	}

	public List<ProcessBean> getProcessBeans() throws SystemException
	{
		if (processName != null)
		{
			return ProcessBeanService.getService().getProcessBeans(processName, getInfoGluePrincipal());
		}
		else
		{
			return Collections.emptyList();
		}
	}

	public ProcessBean getProcessBean(String processId) throws SystemException
	{
		if (processName != null)
		{
			try
			{
				return ProcessBeanService.getService().getProcessBean(processName, processId, getInfoGluePrincipal());
			}
			catch (AccessConstraintException aex)
			{
				logger.warn("User was not allowed to view process bean. There is no normal use case that should trigger this logic.");
				return null;
			}
		}
		else
		{
			return null;
		}
	}

	public List<ProcessBean> getFilteredProcessBeans() throws SystemException
	{
		List<ProcessBean> processes = getProcessBeans();

		if (logger.isDebugEnabled())
		{
			logger.debug("Number of processes before filtering: " + processes.size());
		}
		if (this.processStatus != -1)
		{
			Iterator<ProcessBean> processIterator = processes.iterator();
			ProcessBean process;
			while (processIterator.hasNext())
			{
				process = processIterator.next();
				if (process.getStatus() != this.processStatus)
				{
					logger.debug("Removing (filtering) process with Id: " + process.getProcessId());
					processIterator.remove();
				}
			}
		}
		if (logger.isDebugEnabled())
		{
			logger.debug("Number of processes after filtering: " + processes.size());
		}

		return processes;
	}

	public String getStatusAsJSON()
	{
		Gson gson = new GsonBuilder()
			.excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.STATIC)
			.setDateFormat("dd MMM HH:mm:ss").create();
		JsonObject object = new JsonObject();

		try
		{
			List<ProcessBean> processes = null;
			if (this.processId != null)
			{
				ProcessBean bean = getProcessBean(this.processId);
				if (bean != null)
				{
					processes = Collections.singletonList(bean);
				}
				else
				{
					object.addProperty("redirect", this.returnAddress);
				}
			}
			else
			{
				processes = getProcessBeans();
			}
			if (processes != null)
			{
				Type processBeanListType = new TypeToken<List<ProcessBean>>() {}.getType();
				JsonElement list = gson.toJsonTree(processes, processBeanListType);
				object.add("processes", list);
			}
			object.addProperty("memoryMessage", getMemoryUsageAsText());
		}
		catch (Throwable t)
		{
			logger.error("An error occured when generating JSON for process bean listing.", t);
			JsonObject error = new JsonObject(); 
			error.addProperty("message", t.getMessage());
			error.addProperty("type", t.getClass().getSimpleName());
			object.add("error", error);
		}

		return gson.toJson(object);
	}

	public String getReturnAddress()
	{
		return returnAddress;
	}

	public void setReturnAddress(String returnAddress)
	{
		this.returnAddress = returnAddress;
	}

	public String getProcessId()
	{
		return processId;
	}

	public String getProcessName()
	{
		return processName;
	}

	public void setProcessName(String processName)
	{
		this.processName = processName;
	}

}
