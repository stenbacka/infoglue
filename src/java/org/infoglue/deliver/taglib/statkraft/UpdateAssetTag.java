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

package org.infoglue.deliver.taglib.statkraft;

import javax.servlet.jsp.JspException;

import org.infoglue.deliver.taglib.component.ComponentLogicTag;
import org.infoglue.deliver.taglib.statkraft.StatkraftAssetController.Asset;

public class UpdateAssetTag extends ComponentLogicTag
{
	private static final long serialVersionUID = 3546080250652931383L;
	
	private String configPath;
	private String assetId;
	private String assetDescription;
	private String assetPath;

	@SuppressWarnings("unchecked")
	public int doEndTag() throws JspException
	{
		StatkraftAssetController controller = StatkraftAssetController.getInstance(configPath);

		if (controller == null)
		{
			System.err.println("No instance for the config path");
		}
		else
		{
			System.out.println("Assetid: " + assetId);
			Asset asset = controller.updateAsset(assetId, assetDescription, assetPath);
			getController().getDeliveryContext().getHttpHeaders().put("Content-Type", "application/json");
			StringBuilder sb = new StringBuilder();
			sb.append("{");
			sb.append("\"assetName\":").append("\"").append(asset.getNewSource()).append("\",");
			sb.append("\"assetDescription\":").append("\"").append(asset.getNewDescription()).append("\",");
			sb.append("\"assetPath\":").append("\"").append(asset.getNewPath()).append("\",");
			sb.append("\"modified\":").append("\"").append(asset.getIsModified()).append("\"");
			sb.append("}");
			produceResult(sb.toString());
		}
		configPath = null;
		assetId = null;
		assetDescription = null;
		assetPath = null;
		
		return EVAL_PAGE;
	}

	public void setConfigPath(String configPath) throws JspException
	{
		this.configPath = evaluateString("updateAsset", "configPath", configPath);
	}
	public void setAssetId(String assetId) throws JspException
	{
		this.assetId = evaluateString("updateAsset", "assetId", assetId);
	}
	public void setAssetDescription(String assetDescription) throws JspException
	{
		this.assetDescription = evaluateString("updateAsset", "assetDescription", assetDescription);
	}
	public void setAssetPath(String assetPath) throws JspException
	{
		this.assetPath = evaluateString("updateAsset", "assetPath", assetPath);
	}
}