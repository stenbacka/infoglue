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

package org.infoglue.deliver.util;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.Reader;
import java.io.StringReader;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringEscapeUtils;
import org.apache.log4j.Logger;
import org.apache.log4j.MDC;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.app.Velocity;
import org.infoglue.cms.applications.tasktool.actions.ScriptController;
import org.infoglue.cms.exception.SystemException;
import org.infoglue.cms.io.FileHelper;
import org.infoglue.cms.security.InfoGluePrincipal;
import org.infoglue.cms.util.CmsPropertyHandler;
import org.infoglue.cms.util.StringManager;
import org.infoglue.cms.util.StringManagerFactory;
import org.infoglue.deliver.applications.actions.InfoGlueComponent;
import org.infoglue.deliver.applications.databeans.DeliveryContext;
import org.infoglue.deliver.controllers.kernel.impl.simple.TemplateController;
import org.infoglue.deliver.portal.PortalController;
import org.infoglue.deliver.taglib.log.LoggerTagsUtil;

import com.caucho.java.WorkDir;
import com.caucho.quercus.QuercusContext;
import com.caucho.quercus.env.Env;
import com.caucho.quercus.env.Value;
import com.caucho.quercus.page.QuercusPage;
import com.caucho.util.Alarm;
import com.caucho.util.CharBuffer;
import com.caucho.vfs.FilePath;
import com.caucho.vfs.Path;
import com.caucho.vfs.ReadStream;
import com.caucho.vfs.StringWriter;
import com.caucho.vfs.Vfs;
import com.caucho.vfs.VfsStream;
import com.caucho.vfs.WriteStream;

/**
 * @author Mattias Bogeblad
 */

public class VelocityTemplateProcessor
{
	private final static Logger logger = Logger.getLogger(VelocityTemplateProcessor.class.getName());

	public static final String TEMPLATETYPE_COMPONENT_TEMPLATE = "ComponentTemplate";
	public static final String TEMPLATETYPE_PRETEMPLATE_COMPONENT = "PreComponentTemplate";
	public static final String TEMPLATETYPE_FULLPAGE = "FullPage";

	/**
	 * This method takes arguments and renders a template given as a string to the specified outputstream.
	 * Improve later - cache for example the engine.
	 */
	public void renderTemplate(Map<String, Object> params, PrintWriter pw, String templateAsString) throws Exception
	{
		renderTemplate(params, pw, templateAsString, false, null);
	}

	/**
	 * This method takes arguments and renders a template given as a string to the specified outputstream.
	 * Improve later - cache for example the engine.
	 */
	public void renderTemplate(Map<String, Object> params, PrintWriter pw, String templateAsString, boolean forceVelocity) throws Exception
	{
		renderTemplate(params, pw, templateAsString, forceVelocity, null);
	}

	/**
	 * This method takes arguments and renders a template given as a string to the specified outputstream.
	 * Improve later - cache for example the engine.
	 */
	public void renderTemplate(Map<String, Object> params, PrintWriter pw, String templateAsString, boolean forceVelocity, InfoGlueComponent component) throws Exception
	{
		renderTemplate(params, pw, templateAsString, forceVelocity, component, null, null);
	}

	/**
	 * This method takes arguments and renders a template given as a string to the specified outputstream.
	 * Improve later - cache for example the engine.
	 */
	public void renderTemplate(final Map<String, Object> params, PrintWriter pw, String templateAsString, boolean forceVelocity, InfoGlueComponent component, String statisticsSuffix, String templateType) throws Exception
	{
		String componentName = "Unknown name or not a component";
		if(component != null)
		{
			componentName = "" + component.getName() + "(" + component.getContentId() + ")";
		}

		try
		{
			final Timer timer = new Timer();

			if(!forceVelocity && (templateAsString.indexOf("<%") > -1 || templateAsString.indexOf("http://java.sun.com/products/jsp/dtd/jspcore_1_0.dtd") > -1))
			{
				dispatchJSP(params, pw, templateAsString, component);
			}
			else if(!forceVelocity && templateAsString.indexOf("<?php") > -1)
			{
				logger.info("Dispatching php:" + templateAsString.trim());
				dispatchPHP(params, pw, templateAsString, component);
			}
			else
			{
				boolean useFreeMarker = false;
				String useFreeMarkerString = CmsPropertyHandler.getUseFreeMarker();
				if(useFreeMarkerString != null && useFreeMarkerString.equalsIgnoreCase("true"))
				{
					useFreeMarker = true;
				}

				if((useFreeMarker || templateAsString.indexOf("<#-- IG:FreeMarker -->") > -1) && !forceVelocity)
				{
					FreemarkerTemplateProcessor.getProcessor().renderTemplate(params, pw, templateAsString);
				}
				else
				{
					Velocity.init();
					VelocityContext context = new VelocityContext();
					Iterator<String> i = params.keySet().iterator();
					while(i.hasNext())
					{
						String key = i.next();
						context.put(key, params.get(key));
					}

					Reader reader = new StringReader(templateAsString);
					if(logger.isInfoEnabled())
					{
						logger.info("Going to evaluate the string of length:" + templateAsString.length());
					}

					Velocity.evaluate(context, pw, "Generator Error", reader);
				}
			}

			RequestAnalyser.getRequestAnalyser().registerComponentStatistics(componentName + (statisticsSuffix == null ? "" : statisticsSuffix), timer.getElapsedTime());
			if(logger.isInfoEnabled())
			{
				logger.info("Rendering took:" + timer.getElapsedTime());
			}
		}
		catch(Exception ex)
		{
			/* The templateLogic should only be available if this is a component. However for
			 * backward compatibility/safety it will be handled as it was in earlier versions.
			*/
			TemplateController templateController = (TemplateController)params.get("templateLogic");
			boolean exceptionHandled = false;
			if (templateType != null)
			{
				if (templateType.equals(TEMPLATETYPE_COMPONENT_TEMPLATE) || templateType.equals(TEMPLATETYPE_PRETEMPLATE_COMPONENT) || templateType.equals(TEMPLATETYPE_FULLPAGE))
				{
					String componentNameForLogger = component == null ? null : component.getName();
					Logger componentLogger = LoggerTagsUtil.getTagsUtil().getLogger(componentNameForLogger);
					if (templateType.equals(TEMPLATETYPE_FULLPAGE))
					{
						MDC.put("componentId", -1);
					}
					else
					{
						MDC.put("componentId", component.getId());
					}
					MDC.put("templateType", templateType);
					if (templateController != null)
					{
						InfoGluePrincipal principal = templateController.getPrincipal();
						MDC.put("siteNodeId", templateController.getSiteNodeId());
						MDC.put("principalName", principal == null ? "null" : principal.getName());
						MDC.put("languageId", templateController.getLanguageId());
					}
					componentLogger.error("Error when rendering component. Message: " + ex.getMessage());
					componentLogger.warn("Error when rendering component.", ex);
					exceptionHandled = true;
				}
			}

			if (!exceptionHandled)
			{
				logger.error("Error rendering template [" + componentName + "]. Message: " + ex.getMessage());
				logger.warn("Error rendering template [" + componentName + "].", ex);
				if (logger.isDebugEnabled())
				{
					logger.debug("Error rendering template [" + componentName + "]. Message: " + ex.getMessage() + ".\nParams: " + params);
				}
			}

			//If error we don't want the error cached - right?
			if (templateController != null)
			{
				DeliveryContext deliveryContext = templateController.getDeliveryContext();
				deliveryContext.setDisablePageCache(true);
			}

			if (CmsPropertyHandler.getOperatingMode().equalsIgnoreCase("0") && isTemplateDebuggingEnabled())
			{
				String errorMessage = formatComponentErrorMessage(ex, templateController);
				pw.println(errorMessage);
			}
			else
			{
				if (logger.isInfoEnabled())
				{
					logger.info("Rendering template threw an exception and it should not be printed to the page. Message: " + ex.getMessage(), ex);
				}
				throw ex;
			}
		}
	}

	private boolean isTemplateDebuggingEnabled()
	{
		return CmsPropertyHandler.getDisableTemplateDebug() == null || !CmsPropertyHandler.getDisableTemplateDebug().equalsIgnoreCase("true");
	}

	private String formatComponentErrorMessage(Exception ex, TemplateController templateController) throws SystemException
	{
		String errorMessageTemplate = null;
		if (templateController != null)
		{
			Locale locale = templateController.getLocale();
			if (locale == null)
			{
				locale = Locale.ENGLISH;
			}
			StringManager stringManager = StringManagerFactory.getPresentationStringManager("org.infoglue.cms.applications", locale);
			if (stringManager != null)
			{
				errorMessageTemplate = stringManager.getString("tool.structuretool.componentRendering.errorMessage");
			}
		}
		if (errorMessageTemplate == null)
		{
			errorMessageTemplate = "Error rendering template: ${MESSAGE}"; // Fall-back format
		}
		return errorMessageTemplate.replaceAll("\\$\\{MESSAGE\\}", StringEscapeUtils.escapeHtml(ex.getMessage()).replaceAll("\\n+", "<br/>"));
	}

	/**
	 * This methods renders a template which is written in JSP. The string is written to disk and then called.
	 * 
	 * @param params
	 * @param pw
	 * @param templateAsString
	 * @throws ServletException
	 * @throws IOException
	 */
	public void dispatchJSP(final Map<String, Object> params, final PrintWriter pw, final String templateAsString, final InfoGlueComponent component) throws ServletException, IOException, Exception
	{
		final String dir = CmsPropertyHandler.getContextRootPath() + "jsp";
		final String fileName;
		if ( component != null )
		{
			fileName = "Template_" + component.getName().replaceAll( "[^\\w]", "" ) + "_" + templateAsString.hashCode() +  ".jsp";
		}
		else
		{
			fileName = "Template_" + templateAsString.hashCode() + ".jsp";
		}
		final File template = new File(dir , fileName);

		synchronized (fileName.intern()) {
		    if(!template.exists()) {
		        final PrintWriter fpw = new PrintWriter(template);
		        fpw.print(templateAsString);    
		        fpw.flush();
		        fpw.close();
		    }
		}

	    final ScriptController scriptController = (ScriptController)params.get("scriptLogic");
	    final TemplateController templateController = (TemplateController)params.get("templateLogic");
	    final PortalController portletController = (PortalController)params.get("portalLogic");
	    @SuppressWarnings("unchecked")
		final Map<String,Object> model = (Map<String,Object>)params.get("model");
	    if(templateController != null)
	    {
	    	final DeliveryContext deliveryContext = templateController.getDeliveryContext();
			//logger.info("renderJSP: ClassLoader in context for thread:" + Thread.currentThread().getId() + ":" + Thread.currentThread().getContextClassLoader().getClass().getName());

		    templateController.getHttpServletRequest().setAttribute("org.infoglue.cms.deliver.templateLogic", templateController);
		    templateController.getHttpServletRequest().setAttribute("org.infoglue.cms.deliver.portalLogic", portletController);
		    templateController.getHttpServletRequest().setAttribute("org.infoglue.cms.deliver.classLoader", Thread.currentThread().getContextClassLoader());
		    templateController.getHttpServletRequest().setAttribute("model", model);
		    final CharResponseWrapper wrapper = new CharResponseWrapper(deliveryContext.getHttpServletResponse());
		    final RequestDispatcher dispatch = templateController.getHttpServletRequest().getRequestDispatcher("/jsp/" + fileName);
		    dispatch.include(templateController.getHttpServletRequest(), wrapper);

		    pw.println(wrapper.toCharArray());
	    }
	    else if(scriptController != null)
	    {
	    	scriptController.getRequest().setAttribute("org.infoglue.cms.deliver.scriptLogic", scriptController);
	    	scriptController.getRequest().setAttribute("org.infoglue.cms.deliver.portalLogic", portletController);
	    	scriptController.getRequest().setAttribute("org.infoglue.cms.deliver.classLoader", Thread.currentThread().getContextClassLoader());
	    	scriptController.getRequest().setAttribute("model", model);
		    final CharResponseWrapper wrapper = new CharResponseWrapper(scriptController.getResponse());
		    final RequestDispatcher dispatch = scriptController.getRequest().getRequestDispatcher("/jsp/" + fileName);
		    dispatch.include(scriptController.getRequest(), wrapper);

		    pw.println(wrapper.toCharArray());	    	
	    }
	}

	
	/**
	 * This methods renders a template which is written in JSP. The string is written to disk and then called.
	 * 
	 * @param params
	 * @param pw
	 * @param templateAsString
	 * @throws ServletException
	 * @throws IOException
	 * @deprecated
	 */
	@Deprecated
	public void dispatchJSP(Map<String, Object> params, PrintWriter pw, String templateAsString) throws ServletException, IOException, Exception
	{
	    Timer timer = new Timer();
	    timer.setActive(false);

		int hashCode = templateAsString.hashCode();

		String contextRootPath = CmsPropertyHandler.getContextRootPath();
		String fileName = contextRootPath + "jsp" + File.separator + "Template_" + hashCode + ".jsp";
		String tempFileName = contextRootPath + "jsp" + File.separator + Thread.currentThread().getId() + "_tmp_Template_" + hashCode + ".jsp";
		
		File template = new File(fileName);
		File tmpTemplate = new File(tempFileName);

		if(!template.exists())
		{
			logger.info("Going to write template to file: " + template.hashCode());
			//Thread.sleep(50);
			FileHelper.writeToFile(tmpTemplate, templateAsString, false);
		
			synchronized(template) 
			{
				if(tmpTemplate.length() == 0 || template.exists())
				{
					logger.info("written file:" + tmpTemplate.length() + " - removing temp and not renaming it...");	
					tmpTemplate.delete();
				}
				else
				{
					renameTemplate(tmpTemplate, template);
					//tmpTemplate.renameTo(template);
					logger.info("Time for renaming file " + timer.getElapsedTime());
				}					
			}
		}
		
		TemplateController templateController = (TemplateController)params.get("templateLogic");
		PortalController portletController = (PortalController)params.get("portalLogic");
		DeliveryContext deliveryContext = templateController.getDeliveryContext();
    	RequestDispatcher dispatch = templateController.getHttpServletRequest().getRequestDispatcher("/jsp/Template_" + hashCode + ".jsp");
		templateController.getHttpServletRequest().setAttribute("org.infoglue.cms.deliver.templateLogic", templateController);
		templateController.getHttpServletRequest().setAttribute("org.infoglue.cms.deliver.portalLogic", portletController);
	    templateController.getHttpServletRequest().setAttribute("org.infoglue.cms.deliver.classLoader", Thread.currentThread().getContextClassLoader());
    	CharResponseWrapper wrapper = new CharResponseWrapper(deliveryContext.getHttpServletResponse());
		
    	dispatch.include(templateController.getHttpServletRequest(), wrapper);

    	String result = wrapper.toString();

    	pw.println(result);
	}
	
	/**
	 * This methods renders a template which is written in PHP. The string is written to disk and then called.
	 * 
	 * @param params
	 * @param pw
	 * @param templateAsString
	 * @throws ServletException
	 * @throws IOException
	 */
	@SuppressWarnings("unused") // TODO: evaluate if we can safely remove the unused variables. Are there any chance of there being a problem?
	public void dispatchPHP(final Map<String, Object> params, final PrintWriter pw, final String templateAsString, final InfoGlueComponent component) throws ServletException, IOException, Exception
	{
	    final String dir = CmsPropertyHandler.getContextRootPath() + "jsp";
	    final String fileName;
	    if ( component != null ) {
	        fileName = "Template_" + component.getName().replaceAll( "[^\\w]", "" ) + "_" + templateAsString.hashCode() +  ".php";
	    } else {
	        fileName = "Template_" + templateAsString.hashCode() + ".php";
	    }
	    final File template = new File(dir, fileName);
	    
	    synchronized (fileName.intern()) {
		    if(!template.exists()) {
		        final PrintWriter fpw = new PrintWriter(template);
		        fpw.print(templateAsString);    
		        fpw.flush();
		        fpw.close();
		    }
		}

	    final ScriptController scriptController = (ScriptController)params.get("scriptLogic");
	    final TemplateController templateController = (TemplateController)params.get("templateLogic");
	    final PortalController portletController = (PortalController)params.get("portalLogic");
	    @SuppressWarnings("unchecked")
		final Map<String,Object> model = (Map<String,Object>)params.get("model");

	    byte[] result = null;
	    if(templateController != null)
	    {
	    	final DeliveryContext deliveryContext = templateController.getDeliveryContext();
	    
		    templateController.getHttpServletRequest().setAttribute("org.infoglue.cms.deliver.templateLogic", templateController);
		    templateController.getHttpServletRequest().setAttribute("org.infoglue.cms.deliver.portalLogic", portletController);
		    templateController.getHttpServletRequest().setAttribute("model", model);
		    try
		    {
			    QuercusContext quercus = new QuercusContext();
			    quercus.setServletContext(deliveryContext.getHttpServletRequest().getSession().getServletContext());
			    Path pwd = new FilePath(CmsPropertyHandler.getContextRootPath());
			    quercus.setPwd(pwd);
			    if (! Alarm.isTest() && ! quercus.isResin()) {
			    	Vfs.setPwd(pwd);
			    	WorkDir.setLocalWorkDir(pwd.lookup("WEB-INF/work"));
			    }

			    quercus.init();
			    quercus.start();
			    
	            StringWriter writer = new StringWriter(new CharBuffer(1024));
	            writer.openWrite();
	            
	            ByteArrayInputStream bais = new ByteArrayInputStream(templateAsString.getBytes());
	            VfsStream stream = new VfsStream(bais, null);        
	            QuercusPage page = quercus.parse(new ReadStream(stream));
	            
	            WriteStream ws = new WriteStream(writer);
	            
	            Env env = quercus.createEnv(page, ws, deliveryContext.getHttpServletRequest(), deliveryContext.getHttpServletResponse());
	            env.start();
	                        
	            Value value = page.executeTop(env);
	            ws.flush();

	            String output = ((StringWriter)ws.getSource()).getString();            
	            
	            Object returnObject = value.toJavaObject();
	
	            //logger.info("output:" + output);
			    pw.println(output);
		    }
		    catch (Throwable e) 
		    {
		    	e.printStackTrace();
			}
		}
	    else if(scriptController != null)
	    {
	    	scriptController.getRequest().setAttribute("org.infoglue.cms.deliver.scriptLogic", scriptController);
	    	scriptController.getRequest().setAttribute("org.infoglue.cms.deliver.portalLogic", portletController);
	    	scriptController.getRequest().setAttribute("model", model);
		    try
		    {
			    QuercusContext quercus = new QuercusContext();
			    quercus.setServletContext(scriptController.getRequest().getSession().getServletContext());
			    Path pwd = new FilePath(CmsPropertyHandler.getContextRootPath());
			    quercus.setPwd(pwd);
			    if (! Alarm.isTest() && ! quercus.isResin()) {
			    	Vfs.setPwd(pwd);
			    	WorkDir.setLocalWorkDir(pwd.lookup("WEB-INF/work"));
			    }

			    quercus.init();
			    quercus.start();
			    
	            StringWriter writer = new StringWriter(new CharBuffer(1024));
	            writer.openWrite();
	            
	            ByteArrayInputStream bais = new ByteArrayInputStream(templateAsString.getBytes());
	            VfsStream stream = new VfsStream(bais, null);        
	            QuercusPage page = quercus.parse(new ReadStream(stream));
	            
	            WriteStream ws = new WriteStream(writer);
	            
	            Env env = quercus.createEnv(page, ws, scriptController.getRequest(), scriptController.getResponse());
	            env.start();
	                        
	            Value value = page.executeTop(env);
	            ws.flush();

	            String output = ((StringWriter)ws.getSource()).getString();            
	            
	            Object returnObject = value.toJavaObject();
	
	            //logger.info("output:" + output);
			    pw.println(output);
		    }
		    catch (Throwable e) 
		    {
		    	e.printStackTrace();
			}
	    }
	    
	}

	protected WriteStream openWrite(HttpServletResponse response) throws IOException
    {
		WriteStream ws;
    
		OutputStream out = response.getOutputStream();

		ws = Vfs.openWrite(out);

		return ws;
    }
	
	private synchronized void renameTemplate(File tempFile, File newFileName)
	{
		if(tempFile.length() == 0 || newFileName.exists())
		{
			logger.info("written file:" + newFileName.length() + " - removing temp and not renaming it...");	
			tempFile.delete();
		}
		else
		{
			logger.info("written file:" + tempFile.length() + " - renaming it to " + newFileName.getAbsolutePath());	
			tempFile.renameTo(newFileName);
		}	
	}
}
