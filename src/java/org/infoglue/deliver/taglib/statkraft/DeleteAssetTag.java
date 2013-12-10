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

public class DeleteAssetTag extends ComponentLogicTag
{
	private static final long serialVersionUID = 3546080250652931383L;

	private String configPath;
	private String assetId;

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
			boolean success = controller.deleteAsset(assetId);
			getController().getDeliveryContext().getHttpHeaders().put("Content-Type", "application/json");
			StringBuilder sb = new StringBuilder();
			sb.append("{");
			sb.append("\"success\":").append("\"").append("" + success).append("\"");
			sb.append("}");
			produceResult(sb.toString());
		}
		configPath = null;
		assetId = null;

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
}