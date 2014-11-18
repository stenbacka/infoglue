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
import java.util.List;
import java.util.Map;
import java.util.MissingResourceException;

import org.apache.log4j.Logger;
import org.infoglue.cms.services.ProcessBeanService;
import org.infoglue.cms.util.StringManager;

/**
 * A bean class used by the {@link org.infoglue.cms.services.ProcessBeanService} to keep track of processes.
 * 
 * @author Mattias Bogeblad
 * @author Erik Stenbacka <stenbacka@gmail.com>
 */
public class ProcessBean
{
	private final static Logger logger = Logger.getLogger(ProcessBean.class.getName());

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
	private List<LinkBean> successActions = new ArrayList<LinkBean>();

	private String errorMessage;
	// TODO Write custom serializer so we don't get circular dependency
	private transient Throwable exception;

	private transient List<ProcessBeanListener> listeners = new ArrayList<ProcessBeanListener>();
	private List<String> processEvents = new ArrayList<String>();
	private Map<String,Map<String,Object>> artifacts = new HashMap<String,Map<String,Object>>();
	private List<File> files = new ArrayList<File>();

	public ProcessBean(String processName, String processId)
	{
		this(processName, processId, null, null);
	}

	public ProcessBean(String processName, String processId, String initiator, StringManager stringManager)
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
	
	public void updateLastDescription(String eventDescription, Object... args)
	{
		eventDescription = processDescription(eventDescription, args);
		processEvents.set(processEvents.size() - 1, eventDescription);
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
		ProcessBeanService.getService().getProcessBeans().remove(this);
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
			case FINISHED:
			case ERROR:
				this.finished = new Date();
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
	
	

	public List<LinkBean> getSuccessActions()
	{
		return successActions;
	}

	public void addSuccessActions(LinkBean successAction)
	{
		this.successActions.add(successAction);
	}

	@Override
	public String toString()
	{
		return "ProcessBean [processName=" + processName + ", processId=" + processId + ", initiator=" + initiator + ", status=" + status + ", started=" + started + ", finished=" + finished + ", errorMessage=" + errorMessage + "]";
	}
}
