/* ===============================================================================
 *
 * Part of the InfoGlue Content Management Platform (www.infoglue.org)
 *
 * ===============================================================================
 *
 *  Copyright (C)
 * 
 * This program is free software; you can redistribute it and/or modify it under
 * the terms of the GNU General Public License version 2, as published by the
 * Free Software Foundation. See the file LICENSE.html for more information.
 * 
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY, including the implied warranty of MERCHANTABILITY or FITNESS
 * FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with
 * this program; if not, write to the Free Software Foundation, Inc. / 59 Temple
 * Place, Suite 330 / Boston, MA 02111-1307 / USA.
 *
 * ===============================================================================
 */

package org.infoglue.cms.services;

import java.lang.reflect.Modifier;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.log4j.Logger;
import org.infoglue.cms.applications.databeans.LinkBean;
import org.infoglue.cms.applications.databeans.ProcessBean;
import org.infoglue.cms.controllers.kernel.impl.simple.AccessRightController;
import org.infoglue.cms.exception.AccessConstraintException;
import org.infoglue.cms.exception.SystemException;
import org.infoglue.cms.security.InfoGluePrincipal;
import org.infoglue.cms.util.StringManager;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonSerializationContext;
import com.google.gson.JsonSerializer;
import com.google.gson.reflect.TypeToken;

/**
 * <p>This service provides and administrates {@link ProcessBean}.</p>
 * 
 * <p>Process beans are used by long running operations (processes) in the system that run asynchronously. The operations updates the
 * process bean when certain events occurs in the operation that can be of interest to the user. These updates can then be displayed
 * to the user in the GUI to inform them of what is going on with the operation.</p>
 * 
 * <p>ProcessBeans can be access controlled, which is useful for operations that are only relevant for the initiator of the operation.
 * It should be noted however that the access control can be circumvented and provided mostly for the convenience of the user.</p>
 * 
 * @author Mattias Bogeblad
 * @author Erik Stenbacka <stenbacka@gmail.com>
 */
public class ProcessBeanService
{
	private final static Logger logger = Logger.getLogger(ProcessBean.class.getName());

	private static ProcessBeanService service;
	
	private static synchronized void initializeService()
	{
		if (service == null)
		{
			service = new ProcessBeanService();
		}
	}

	public static ProcessBeanService getService()
	{
		if (service == null)
		{
			initializeService();
		}
		return service;
	}

	private List<ProcessBean> processBeans = new ArrayList<ProcessBean>();
	public List<ProcessBean> getProcessBeans()
	{
		return processBeans;
	}

	/**
	 * <p>Returns a set of all process names currently in the list of processes. Please observer that the list is <b>not</b> a list
	 * of all possible process names. Such a list does not exists as of this writing.</p>

	 * @return A list of process names.
	 */
	public Set<String> getProcessNames()
	{
		Set<String> result = new HashSet<String>();

		for (ProcessBean processBean : processBeans)
		{
			result.add(processBean.getProcessName());
		}

		return result;
	}

	public List<ProcessBean> getProcessBeans(String processName) throws SystemException
	{
		return getProcessBeans(processName, null);
	}

	/**
	 * <p>Returns a list of all processes that has the given <em>processName</em> and the given <em>principal</em>
	 * has access to. If principal is null no access check is performed. I.e. all ProcessBeans for the processName
	 * is returned</p>
	 *
	 * <p>The returned list is a shallow (filtered) copy of the list holding all processes.
	 * As such changes made to the list will not be reflected in the original list however
	 * changes made to the ProcessBeans in the list <b>will</b> affect the original process object.</p>
	 * @param processName The name of the process, usually a class name.
	 * @return
	 * @throws SystemException If a database error occurs
	 */
	public List<ProcessBean> getProcessBeans(String processName, InfoGluePrincipal principal) throws SystemException
	{
		List<ProcessBean> processBeansWithName = new ArrayList<ProcessBean>();
		for (ProcessBean processBean : processBeans)
		{
			if (processBean.getProcessName().equals(processName))
			{
				if (principal == null || processBean.getInitiator().equals(principal.getName()) || AccessRightController.getController().getIsPrincipalAuthorized(principal, "Common.ManageProcessBeans", true))
				{
					processBeansWithName.add(processBean);
				}
			}
		}

		return processBeansWithName;
	}

	/**
	 * Convenience method for {@link #getProcessBean(String, String, InfoGluePrincipal)} with the principal set to null.
	 */
	public ProcessBean getProcessBean(String processName, String processId) throws SystemException, AccessConstraintException
	{
		return getProcessBean(processName, processId, null);
	}

	/**
	 * Gets a specific process bean.
	 * 
	 * @param processName The name of the desired process type.
	 * @param processId The ID of the desired process.
	 * @param principal The principal who owns the process bean.
	 * @return A process bean matching the given <em>processName</em> and <em>processId</em>, otherwise null.
	 * @throws SystemException Thrown if an exception in the authorization check occurs.
	 * @throws AccessConstraintException Thrown if the process bean is found but the provided user is not allowed to view it.
	 */
	public ProcessBean getProcessBean(String processName, String processId, InfoGluePrincipal principal) throws SystemException, AccessConstraintException
	{
		for (ProcessBean processBean : processBeans)
		{
			if (processBean.getProcessName().equals(processName) && processBean.getProcessId().equals(processId))
			{
				if (principal == null || processBean.getInitiator().equals(principal.getName()) || AccessRightController.getController().getIsPrincipalAuthorized(principal, "Common.ManageProcessBeans", true))
				{
					return processBean;
				}
				else
				{
					logger.info("Found process bean but the principal was not authorized to view it. Principal: " + principal.getName() + ". ProcessBean.id: " + processId);
					throw new AccessConstraintException("Process", "");
				}
			}
		}
		return null;
	}

	/**
	 * Convenience method for {@link #createProcessBean(String, String, InfoGluePrincipal, StringManager)} with the stringManager set to null.
	 */
	public ProcessBean createProcessBean(String processName, String processId, InfoGluePrincipal principal)
	{
		return createProcessBean(processName, processId, principal, null);
	}

	/**
	 * <p>Creates a new process bean with the given parameters. When the process bean is created the <em>ProcessBeanService</em> will keep track of the process bean
	 * and provide to operations that want to list current process beans. In other words, after this method has executed the process bean is active and viewable
	 * to the user without the caller of this method having to do anything.</p>
	 * 
	 * <p>The name is used as a grouping for the same type of processes  (e.g. <em>RepositoryExport</em>). The process named is used to provide list of processes of
	 * the same type. Any string value is acceptable as a process name. It is common for a process name to be local to one calling class and therefore it is that
	 * class's responsibility to make sure the name is used correctly.</p>
	 * 
	 * @param processName The desired name of the process bean.
	 * @param processId The ID of the process bean. This value needs to be unique among all processes that share a <em>processName</em>.
	 * @param principal The initiator of the process. The process will only be visible to this user in GUIs.
	 * @param stringManager
	 * @throws NullPointerException Thrown if the given <em>principal</em> is null.
	 */
	public ProcessBean createProcessBean(String processName, String processId, InfoGluePrincipal principal, StringManager stringManager) throws NullPointerException
	{
		ProcessBean processBean = new ProcessBean(processName, processId, principal.getName(), stringManager);
		getProcessBeans().add(processBean);
		return processBean;
	}
	
	public String getProcessBeanAsJSON(ProcessBean processBean)
	{
		Gson gson = createGsonInstance();
		return gson.toJson(processBean);
	}
	
	public String getProcessBeansAsJSON(List<ProcessBean> processBeans) throws SystemException
	{
		Gson gson = createGsonInstance();
		JsonElement list = null;
		try
		{
			if (processBeans != null)
			{
				Type processBeanListType = new TypeToken<List<ProcessBean>>() {}.getType();
				list = gson.toJsonTree(processBeans, processBeanListType);
			}
		}
		catch (Throwable tr)
		{
			logger.error("An error occured when generating JSON for process bean listing. Message: " + tr.getMessage());
			logger.warn("An error occured when generating JSON for process bean listing.", tr);
			throw new SystemException("Error when generating JSON", tr);
		}
		return gson.toJson(list);
	}
	
	private Gson createGsonInstance()
	{
		GsonBuilder builder = new GsonBuilder()
			.excludeFieldsWithModifiers(Modifier.TRANSIENT, Modifier.STATIC)
			.setDateFormat("dd MMM HH:mm:ss");
		builder.registerTypeAdapter(new TypeToken<LinkBean>() {}.getType(), new JsonSerializer<LinkBean>()
		{

			@Override
			public JsonElement serialize(LinkBean linkBean, Type type, JsonSerializationContext context)
			{
				JsonObject result = new JsonObject();
				result.addProperty("id", linkBean.getId());
				result.addProperty("text", linkBean.getText());
				result.addProperty("title", linkBean.getTitle());
				result.addProperty("description", linkBean.getDescription());
				result.addProperty("actionURL", linkBean.getActionURL());
				return result;
			}
		});
		return builder.create();
	}
}
