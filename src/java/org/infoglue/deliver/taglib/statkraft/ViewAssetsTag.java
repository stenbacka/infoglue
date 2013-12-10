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

import java.util.Collection;

import javax.servlet.jsp.JspException;

import org.infoglue.deliver.taglib.component.ComponentLogicTag;
import org.infoglue.deliver.taglib.statkraft.StatkraftAssetController.Asset;

public class ViewAssetsTag extends ComponentLogicTag
{
	private static final long serialVersionUID = -7557457576052970148L;

	private String configPath;
	private int startIndex;
	private int endIndex;
	private String assetCount;

	public int doEndTag() throws JspException
	{
		if (configPath != null && !configPath.equals(""))
		{
			Integer[] assetCountVar = new Integer[1];
			Collection<Asset> assets = StatkraftAssetController.getInstance(configPath).getAssets(startIndex, endIndex, assetCountVar);
			pageContext.setAttribute(assetCount, assetCountVar[0]);
			setResultAttribute(assets);
		}
		return EVAL_PAGE;
	}

	public void setConfigPath(String configPath) throws JspException
	{
		this.configPath = evaluateString("viewAssets", "configPath", configPath);
	}

	public void setAssetCount(String assetCount) throws JspException
	{
		this.assetCount = assetCount;
	}

	public void setStartIndex(String startIndex) throws JspException
	{
		this.startIndex = evaluateInteger("viewAssets", "startIndex", startIndex);
	}

	public void setEndIndex(String endIndex) throws JspException
	{
		this.endIndex = evaluateInteger("viewAssets", "endIndex", endIndex);
	}
}