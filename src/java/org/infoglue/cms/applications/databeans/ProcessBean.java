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
package org.infoglue.cms.applications.databeans;

import java.io.File;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;
import java.util.Set;

import org.apache.log4j.Logger;
import org.infoglue.cms.controllers.kernel.impl.simple.AccessRightController;
import org.infoglue.cms.exception.AccessConstraintException;
import org.infoglue.cms.exception.SystemException;
import org.infoglue.cms.security.InfoGluePrincipal;
import org.infoglue.cms.util.ChangeNotificationController;
import org.infoglue.cms.util.StringManager;

/**
 * This bean allows for processes to give information about the process itself and what the status is.
 * The bean has a listener option in which an external class can ask it to report process when it happens (push).
 * 
 * @author Mattias Bogeblad
 * @author Erik Stenbäcka <stenbacka@gmail.com>
 */

public class ProcessBean
{
	private final static Logger logger = Logger.getLogger(ProcessBean.class.getName());

	//The class has its own factory and list of all active processBeans
	private static List<ProcessBean> processBeans = new ArrayList<ProcessBean>();
	private static List<ProcessBean> getProcessBeans()
	{
		return processBeans;
	}
	/**
	 * <p>Returns a set of all process names currently in the list of processes. Please observer that the list is <b>not</b> a list
	 * of all possible process names. Such a list does not exists as of this writing.</p>

	 * @return A list of process names.
	 */
	public static Set<String> getProcessNames()
	{
		Set<String> result = new HashSet<String>();

		for (ProcessBean processBean : processBeans)
		{
			result.add(processBean.getProcessName());
		}

		return result;
	}

	/**
	 * <p>Returns a list of all processes that has the given <em>processName</em> and where one of the following is true:
	 * (1) the given <em>principal</em> is the initiator of the process or (2) the given principal has <i>Common.ManageProcessBeans</i>
	 * privileges.</p>
	 *
	 * <p>The returned list is a shallow (filtered) copy of the list holding all processes. As such changes made to the list will not
	 * be reflected in the original list however changes made to the ProcessBeans in the list <b>will</b> affect the original
	 * process object.</p>
	 *
	 * @param processName The name of the process, usually a class name.
	 * @return
	 * @throws SystemException If a database error occurs
	 */
	public static List<ProcessBean> getProcessBeans(String processName)
	{
		return null;
	}
	public static List<ProcessBean> getProcessBeans(String processName, InfoGluePrincipal principal) throws SystemException
	{
		List<ProcessBean> processBeansWithName = new ArrayList<ProcessBean>();
		for(ProcessBean processBean : processBeans)
		{
			if(processBean.getProcessName().equals(processName))
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
	 * <p>Returns a process with the given <em>processName</em> and given <em>processId</em> and where one of the following is true:
	 * (1) the given <em>principal</em> is the initiator of the process or (2) the given principal has <i>Common.ManageProcessBeans</i>
	 * privileges. The processId of a process bean is assumed to be unique. As such this method will return the first process that
	 * matches the given <em>processId</em>. A process may be without a processId. Such processes will never be returned by this method.</p>
	 *
	 * @param processName The processName of the desired process
	 * @param principal The principal that is trying to access the process.
	 * @param processId The processId of the desired process. Handles null value but no process will be matched for that value.
	 *
	 * @throws SystemException If a database error occurs
	 * @throws AccessConstraintException If a process is found for the given <em>processName</em> and given <em>processId</em> but the given
	 * <em>principal</em> is not allowed to access it.
	 * @throws NullPointerException If <em>processName</em> or <em>principal</em> is null.
	 */
	public static ProcessBean getProcessBean(String processName, String processId)
	{
		return null;
	}
	public static ProcessBean getProcessBean(String processName, InfoGluePrincipal principal, String processId) throws SystemException, AccessConstraintException, NullPointerException
	{
		for(ProcessBean processBean : processBeans)
		{
			if(processName.equals(processBean.getProcessName()) && processBean.getProcessId() != null && processBean.getProcessId().equals(processId))
			{
				if (processBean.getInitiator().equals(principal.getName()) || AccessRightController.getController().getIsPrincipalAuthorized(principal, "Common.ManageProcessBeans", true))
				{
					return processBean;
				}
				else
				{
					logger.info("Found process bean but the principal was not authorized to view it. Principal: " + principal.getName() + ". ProcessBean.id: " + processId);
					throw new AccessConstraintException("Process", "1000");
				}
			}
		}

		return null;
	}

	/**
	 * Creates a process in the process category <em>processName</em> attached to the given <em>principal</em>.
	 * @param processName
	 * @param principal
	 * @return
	 */
	public static ProcessBean createProcessBean(String processName, String principal)
	{
		return null;
	}
	public static ProcessBean createProcessBean(String processName, InfoGluePrincipal principal)
	{
		return createProcessBean(processName, principal, null);
	}

	public static ProcessBean createProcessBean(String processName, String processId, InfoGluePrincipal principal)
	{
		return null;
	}
	public static ProcessBean createProcessBean(String processName, InfoGluePrincipal principal, String processId)
	{
		return createProcessBean(processName, principal, null, processId);
	}

	public static ProcessBean createProcessBean(String processName, InfoGluePrincipal principal, StringManager stringManager, String processId)
	{
		ProcessBean processBean = new ProcessBean(processName, processId, principal.getName(), stringManager);
		getProcessBeans().add(processBean);

		return processBean;
	}
	//-End factory stuff

	public static final int NOT_STARTED = 0;
	public static final int RUNNING = 1;
	public static final int FINISHED = 2;
	/** Indicates that the process has entered an unrecoverable error state. */
	public static final int ERROR = 3;

	//ID can be any string the process decides while processName is a general name for all instances of a certain process.
	private String processName;
	private String processId;
	private String initiator;
	private transient StringManager stringManager;
	private int status = NOT_STARTED;
	// TODO should the dates really be initiated here? getFinished() will say that the process has finished even though it has not
	private Date started = new Date();
	private Date finished = new Date();
	private transient boolean autoRemoveOnSuccess = false;

	private String errorMessage;
	// TODO Write custom serializer so we don't get circular dependency
	private transient Throwable exception;

	private transient List<ProcessBeanListener> listeners = new ArrayList<ProcessBeanListener>();
	private List<String> processEvents = new ArrayList<String>();
	private Map<String,Map<String,Object>> artifacts = new HashMap<String,Map<String,Object>>();
	private List<File> files = new ArrayList<File>();

	private ProcessBean(String processName, String processId)
	{
		this(processName, processId, null, null);
	}

	private ProcessBean(String processName, String processId, String initiator, StringManager stringManager)
	{
		this.processName = processName;
		this.processId = processId;
		this.initiator = initiator;
		this.stringManager = stringManager;
	}
	
	private String processDescription(String eventDescription, Object[] args)
	{
		if (stringManager != null)
		{
			try
			{
				return stringManager.getString(eventDescription, args);
			}
			catch (MissingResourceException mrex)
			{
				logger.debug("The given event description was not a valid resource key. Event description: " + eventDescription);
			}
		}
		return eventDescription;
	}

	/**
	 * This method sends the event description to all listeners.
	 * 
	 * @param eventDescription
	 */
	public void updateProcess(String eventDescription, Object... args)
	{
		eventDescription = processDescription(eventDescription, args);
		processEvents.add(eventDescription);
		// TODO Does this need to be synchronized with adding listeners?
		for(ProcessBeanListener processBeanListener : listeners)
		{
			try
			{
				processBeanListener.processUpdated(eventDescription);
			}
			catch (Exception e) 
			{
				logger.error("Error updating ProcessBeanListener: " + e.getMessage());
			}
		}
	}

	/**
	 * This method sends the event description to all listeners.
	 * 
	 * @param eventDescription
	 */
	public void updateLastDescription(String eventDescription)
	{
		updateCurrentEvent(eventDescription);
	}

	public void updateCurrentEvent(String eventDescription, Object... args) throws IndexOutOfBoundsException
	{
		eventDescription = processDescription(eventDescription, args);
		processEvents.set(processEvents.size() - 1, eventDescription);
		// TODO Does this need to be synchronized with adding listeners?
		for(ProcessBeanListener processBeanListener : listeners)
		{
			try
			{
				processBeanListener.processUpdatedLastDescription(eventDescription);
			}
			catch (Exception e) 
			{
				logger.error("Error updating ProcessBeanListener: " + e.getMessage());
			}
		}
	}

	/**
	 * This method sends the new artifact to all listeners.
	 * 
	 */
	public void updateProcessArtifacts(String artifactId, String url, File file)
	{
		Map<String,Object> artifactDescMap = new HashMap<String,Object>();
		artifactDescMap.put("url", url);
		artifactDescMap.put("file", file);
		artifactDescMap.put("fileSize", file.length());
		
		artifacts.put(artifactId, artifactDescMap);
		
		files.add(file);
		for(ProcessBeanListener processBeanListener : listeners)
		{
			try
			{
	    		processBeanListener.processArtifactsUpdated(artifactId, url, file);
			}
			catch (Exception e) 
			{
				logger.error("Error updating ProcessBeanListener: " + e.getMessage());
			}
		}
	}

	/**
	 * This method removes the bean from list of active processes and clears all references.
	 */
	public void removeProcess()
	{
		updateProcess("Process removed");
		this.listeners.clear();
		if(files != null)
		{
			for(File file : files)
			{
				file.delete();
			}
		}
		getProcessBeans().remove(this);
	}

	/**
	 * Same as calling {@link #setError(String, Throwable)} with null passed as the
	 * second parameter.
	 * @param errorMessage
	 */
	public void setError(String errorMessage, Object... args)
	{
		setError(errorMessage, null, args);
	}

	/**
	 * Marks the process as an erroneous processes. Calling this method means that the process has
	 * terminated but was not successful. This method also sets the state of the process to {@link #ERROR}.
	 * @param errorMessage
	 * @param exception The exception that caused the process to fail. May be null if the error was not related to an exception.
	 */
	public void setError(String errorMessage, Throwable exception, Object... args)
	{
		errorMessage = processDescription(errorMessage, args);
		this.errorMessage = errorMessage;
		this.exception = exception;
		setStatus(ERROR);
	}
	
	public void setAutoRemoveOnSuccess(boolean autoRemoveOnSuccess)
	{
		this.autoRemoveOnSuccess = autoRemoveOnSuccess;
	}

	public String getErrorMessage()
	{
		return this.errorMessage;
	}
	
	public Throwable getException()
	{
		return this.exception;
	}

	public String getProcessName()
	{
		return processName;
	}

	public String getProcessId()
	{
		return processId;
	}

	public String getInitiator()
	{
		return initiator;
	}

    public int getStatus()
    {
    	return this.status;
    }

	public Date getStarted() 
	{
		return started;
	}

	public Date getFinished() 
	{
		return finished;
	}

	/**
	 * Sets the status of the process.
	 * 
	 * Possible values are:
	 * <ul>
	 * 	<li>{@linkplain #NOT_STARTED}</li>
	 * 	<li>{@linkplain #RUNNING}</li>
	 * 	<li>{@linkplain #FINISHED}</li>
	 * 	<li>{@linkplain #ERROR}</li>
	 * </ul>
	 * 
	 * While it is allowed to set the state to {@link #ERROR} through this method
	 * it is encouraged to do it using the {@link #setError(String, Throwable)} method.
	 * 
	 * @param status The new status. See available values in text above.
	 */
	public void setStatus(int status)
	{
		this.status = status;
		switch (this.status)
		{
			case RUNNING:
				this.started = new Date();
				break;
			case FINISHED: // Fall-through case (!)
				if (this.autoRemoveOnSuccess)
				{
					removeProcess();
				}
			case ERROR:
				this.finished = new Date();
				ChangeNotificationController.getInstance().notifyListeners();
				break;
		}
	}

	public List<String> getProcessEvents()
	{
		return this.processEvents;
	}
	
	public Map<String,Map<String,Object>> getProcessArtifacts()
	{
		return this.artifacts;
	}

	@Override
	public String toString()
	{
		return "ProcessBean [processName=" + processName + ", processId=" + processId + ", initiator=" + initiator + ", status=" + status + ", started=" + started + ", finished=" + finished + ", errorMessage=" + errorMessage + "]";
	}
}
