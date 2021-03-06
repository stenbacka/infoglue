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

package org.infoglue.cms.applications.managementtool.actions;

import org.infoglue.cms.controllers.kernel.impl.simple.ServerNodeController;
import org.infoglue.cms.entities.management.ServerNodeVO;
import org.infoglue.cms.util.ConstraintExceptionBuffer;


/**
  * This is the action-class for UpdateServerNode
  * 
  * @author Mattias Bogeblad
  */
public class UpdateServerNodeAction extends ViewServerNodeAction //WebworkAbstractAction
{
	
	private ServerNodeVO serverNodeVO;
	private Integer serverNodeId;
	private String userAction = "";
	private ConstraintExceptionBuffer ceb;
	
	public UpdateServerNodeAction()
	{
		this(new ServerNodeVO());
	}
	
	public UpdateServerNodeAction(ServerNodeVO serverNodeVO)
	{
		this.serverNodeVO = serverNodeVO;
		this.ceb = new ConstraintExceptionBuffer();	
	}

       	
	public String doExecute() throws Exception
    {
		super.initialize(getServerNodeId());

    	ceb.add(this.serverNodeVO.validate());
    	ceb.throwIfNotEmpty();		
    	
		ServerNodeController.getController().update(this.serverNodeVO);
				
		return "success";
	}

	public String doSaveAndExit() throws Exception
    {
		doExecute();
						
		return "saveAndExit";
	}

	public void setServerNodeId(Integer serverNodeId) throws Exception
	{
		this.serverNodeVO.setServerNodeId(serverNodeId);	
	}

    public java.lang.Integer getServerNodeId()
    {
        return this.serverNodeVO.getServerNodeId();
    }
        
    public java.lang.String getName()
    {
    	return this.serverNodeVO.getName();
    }
        
    public void setName(java.lang.String name)
    {
       	this.serverNodeVO.setName(name);
    }

    public String getDescription()
    {
        return this.serverNodeVO.getDescription();
    }
        
    public void setDescription(String description)
    {
       	this.serverNodeVO.setDescription(description);
    }
    
    public String getDnsName()
    {
        return this.serverNodeVO.getDnsName();
    }
        
    public void setDnsName(String dnsName)
    {
       	this.serverNodeVO.setDnsName(dnsName);
    }
    
}
