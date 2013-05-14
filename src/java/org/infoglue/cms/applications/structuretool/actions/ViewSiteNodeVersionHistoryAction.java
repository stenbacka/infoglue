package org.infoglue.cms.applications.structuretool.actions;

import java.util.List;

import org.infoglue.cms.applications.common.actions.InfoGlueAbstractAction;
import org.infoglue.cms.applications.databeans.DiffTree;
import org.infoglue.cms.controllers.kernel.impl.simple.ContentVersionController;
import org.infoglue.cms.controllers.kernel.impl.simple.DiffController;
import org.infoglue.cms.controllers.kernel.impl.simple.LanguageController;
import org.infoglue.cms.controllers.kernel.impl.simple.RepositoryController;
import org.infoglue.cms.controllers.kernel.impl.simple.SiteNodeController;
import org.infoglue.cms.controllers.kernel.impl.simple.SiteNodeVersionController;
import org.infoglue.cms.entities.content.ContentVersionVO;
import org.infoglue.cms.entities.management.LanguageVO;
import org.infoglue.cms.entities.structure.SiteNodeVO;

public class ViewSiteNodeVersionHistoryAction extends InfoGlueAbstractAction
{
	private static final long serialVersionUID = -3684364351134467348L;

	private Integer siteNodeId;
	private SiteNodeVO siteNodeVO;
	private List<ContentVersionVO> metaInfo;
	private DiffTree diffTree;
	private String returnUrl;

	//@Override
	protected String doExecute() throws Exception
	{
		this.siteNodeVO = SiteNodeController.getController().getSiteNodeVOWithId(siteNodeId);
		SiteNodeVersionController.getController().getSiteNodeVersionVOList(siteNodeId);

	    return "success";
	}

	public String doCompare() throws Exception
	{
		SiteNodeVO siteNodeVO = SiteNodeController.getController().getSiteNodeVOWithId(siteNodeId);
		LanguageVO languageVO = LanguageController.getController().getMasterLanguage(siteNodeVO.getRepositoryId());
		ContentVersionVO workingVersion = ContentVersionController.getContentVersionController().getLatestActiveContentVersionVO(siteNodeVO.getMetaInfoContentId(), languageVO.getLanguageId(), ContentVersionVO.WORKING_STATE);
		ContentVersionVO baseVersion = ContentVersionController.getContentVersionController().getLatestActiveContentVersionVO(siteNodeVO.getMetaInfoContentId(), languageVO.getLanguageId(), ContentVersionVO.PUBLISHED_STATE);

		diffTree = DiffController.getController().compareSiteNodeVersions(baseVersion.getContentVersionId(), workingVersion.getContentVersionId());
		return "compare";
	}

	public DiffTree getDiffTree()
	{
		return diffTree;
	}

	public void setSiteNodeId(Integer siteNodeId)
	{
		this.siteNodeId = siteNodeId;
	}

	public String getReturnUrl()
	{
		return returnUrl;
	}

	public void setReturnUrl(String returnUrl)
	{
		this.returnUrl = returnUrl;
	}

}
