//package org.infoglue.cms.controllers.kernel.impl.simple;
//
//import java.io.File;
//import java.io.FileOutputStream;
//import java.io.OutputStreamWriter;
//import java.util.ArrayList;
//import java.util.Collections;
//import java.util.Date;
//import java.util.HashMap;
//import java.util.Hashtable;
//import java.util.List;
//import java.util.Map;
//import java.util.UUID;
//import java.util.concurrent.atomic.AtomicInteger;
//
//import org.apache.log4j.Logger;
//import org.exolab.castor.jdo.Database;
//import org.exolab.castor.mapping.Mapping;
//import org.exolab.castor.xml.Marshaller;
//import org.infoglue.cms.applications.common.VisualFormatter;
//import org.infoglue.cms.entities.content.Content;
//import org.infoglue.cms.entities.kernel.BaseEntityVO;
//import org.infoglue.cms.entities.management.AccessRight;
//import org.infoglue.cms.entities.management.Category;
//import org.infoglue.cms.entities.management.ContentTypeDefinition;
//import org.infoglue.cms.entities.management.InterceptionPointVO;
//import org.infoglue.cms.entities.management.Repository;
//import org.infoglue.cms.entities.management.impl.simple.InfoGlueExportImpl;
//import org.infoglue.cms.entities.structure.SiteNode;
//import org.infoglue.cms.security.InfoGluePrincipal;
//import org.infoglue.cms.util.CmsPropertyHandler;
//import org.infoglue.cms.util.handlers.DigitalAssetBytesHandler;
//
//import com.opensymphony.module.propertyset.PropertySet;
//import com.opensymphony.module.propertyset.PropertySetManager;
//
//public class ExportRepositoryController extends BaseController
//{
//	private static final ExportRepositoryController controller = new ExportRepositoryController();
//	private static final Logger logger = Logger.getLogger(ExportRepositoryController.class);
//	
//	public static final String PARAM_REPOSITORIES = "REPOSITORIES";
//	public static final String PARAM_SKIP_CATEGORIES = "SKIP_CATEGORIES";
//	public static final String PARAM_ONLY_USED_CATEGORIES = "ONLY_USED_CATEGORIES";
//	public static final Object PARAM_EXPORT_FILE_NAME = "EXPORT_FILE_NAME";
//	public static final Object PARAM_ASSET_MAX_SIZE = "ASSET_MAX_SIZE";
//
//	private Map<UUID, ExportWorker> workers;
//	
//	private ExportRepositoryController()
//	{
//		workers = Collections.synchronizedMap(new HashMap<UUID, ExportWorker>());
//	}
//	
//	/* ############################################################ */
//	/* ####  Public methods  ###################################### */
//	/* ############################################################ */
//	
//	public ExportInfo getExportInfo(UUID exportUUID)
//	{
//		ExportWorker worker = workers.get(exportUUID);
//		if (worker == null)
//		{
//			throw new IllegalStateException("An export with the given id was not found in the current export worker queue. Export id: " + exportUUID);
//		}
//		return worker.getExportInfo();
//	}
//	
//	/* %%%%  Export Methods  %%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%%% */
//	
//	public UUID exportRepository(Map<String, Object> exportParams, InfoGluePrincipal exportOwner)
//	{
//		if (!exportParams.containsKey(ExportRepositoryController.PARAM_REPOSITORIES))
//		{
//			throw new IllegalArgumentException("Export params must contain an entry for the key PARAM_REPOSITORIES");
//		}
//		ExportWorker exportWorker = new ExportWorker(exportOwner, exportParams);
//		
//		workers.put(exportWorker.getId(), exportWorker);
//		new Thread(exportWorker).start();
//		
//		return exportWorker.getId();
//	}
//	
//	/* ############################################################ */
//	/* ####  Auxiliary methods  ################################### */
//	/* ############################################################ */
//	
//	public static ExportRepositoryController getController()
//	{
//		return controller;
//	}
//
//	@Override
//	public BaseEntityVO getNewVO()
//	{
//		return null;
//	}
//	
//	/* ############################################################ */
//	/* ####  Inner classes  ####################################### */
//	/* ############################################################ */
//	
//	private static class ExportWorker implements Runnable
//	{
//		private ExportInfo exportInfo;
//		private UUID id;
//		private InfoGluePrincipal owner;
//		private Integer[] repositoryIds;
//		private String exportFileName;
//		private Integer assetMaxSize = -1;
//		
//		public ExportWorker(InfoGluePrincipal owner, Map<String, Object> exportParams)
//		{
//			this.id = UUID.randomUUID();
//			this.owner = owner;
//			this.repositoryIds = (Integer[])exportParams.get(ExportRepositoryController.PARAM_REPOSITORIES);
//			if (exportParams.containsKey(ExportRepositoryController.PARAM_EXPORT_FILE_NAME))
//			{
//				this.exportFileName = (String)exportParams.get(ExportRepositoryController.PARAM_EXPORT_FILE_NAME);
//			}
//			if (exportParams.containsKey(ExportRepositoryController.PARAM_ASSET_MAX_SIZE))
//			{
//				this.assetMaxSize = (Integer)exportParams.get(ExportRepositoryController.PARAM_ASSET_MAX_SIZE);
//			}
//
//			this.exportInfo = new ExportInfo();
//		}
//		
//		public UUID getId()
//		{
//			return id;
//		}
//		
//		public ExportInfo getExportInfo()
//		{
//			return exportInfo;
//		}
//		
//		public InfoGluePrincipal getOwner()
//		{
//			return owner;
//		}
//
//		@Override
//		public void run()
//		{
//			logger.info("Starting export in worker thread");
//			
//			exportInfo.status = ExportStatus.Running;
//			
//			Database db = CastorDatabaseService.getDatabase();
//			
//			try 
//			{
//				Mapping map = new Mapping();
//				String exportFormat = CmsPropertyHandler.getExportFormat();
//
//				if(exportFormat.equalsIgnoreCase("2"))
//				{
//					logger.info("MappingFile:" + CastorDatabaseService.class.getResource("/xml_mapping_site_2.5.xml").toString());
//					map.loadMapping(CastorDatabaseService.class.getResource("/xml_mapping_site_2.5.xml").toString());
//				}
//				else
//				{
//					logger.info("MappingFile:" + CastorDatabaseService.class.getResource("/xml_mapping_site.xml").toString());
//					map.loadMapping(CastorDatabaseService.class.getResource("/xml_mapping_site.xml").toString());
//				}
//				
//				// All ODMG database access requires a transaction
//				db.begin();
//
//				List<SiteNode> siteNodes = new ArrayList<SiteNode>();
//				List<Content> contents = new ArrayList<Content>();
//				Hashtable<String,String> allRepositoryProperties = new Hashtable<String,String>();
//				Hashtable<String,String> allSiteNodeProperties = new Hashtable<String,String>();
//				Hashtable<String,String> allContentProperties = new Hashtable<String,String>();
//				List<AccessRight> allAccessRights = new ArrayList<AccessRight>();
//				
//				//TEST
//				Map args = new HashMap();
//			    args.put("globalKey", "infoglue");
//			    PropertySet ps = PropertySetManager.getInstance("jdbc", args);
//			    //END TEST
//				
//				String names = "";
//				for(int i=0; i < repositoryIds.length; i++)
//				{
//					Integer repositoryId    = repositoryIds[i];
//					Repository repository 	= RepositoryController.getController().getRepositoryWithId(repositoryId, db);
//					SiteNode siteNode 		= SiteNodeController.getController().getRootSiteNode(repositoryId, db);
//					Content content 		= ContentController.getContentController().getRootContent(repositoryId, db);
//					
//				    InterceptionPointVO interceptionPointVO = InterceptionPointController.getController().getInterceptionPointVOWithName("Repository.Read", db);
//				    if(interceptionPointVO != null)
//				    	allAccessRights.addAll(AccessRightController.getController().getAccessRightListOnlyReadOnly(interceptionPointVO.getId(), repository.getId().toString(), db));
//
//				    interceptionPointVO = InterceptionPointController.getController().getInterceptionPointVOWithName("Repository.ReadForBinding", db);
//				    if(interceptionPointVO != null)
//				    	allAccessRights.addAll(AccessRightController.getController().getAccessRightListOnlyReadOnly(interceptionPointVO.getId(), repository.getId().toString(), db));
//
//					getContentPropertiesAndAccessRights(ps, allContentProperties, allAccessRights, content, db);
//					if(siteNode != null)
//						getSiteNodePropertiesAndAccessRights(ps, allSiteNodeProperties, allAccessRights, siteNode, db);
//					
//					if(siteNode != null)
//						siteNodes.add(siteNode);
//					contents.add(content);
//					names = names + "_" + repository.getName();
//					allRepositoryProperties.putAll(getRepositoryProperties(ps, repositoryId));
//				}
//				
//				List<ContentTypeDefinition> contentTypeDefinitions = ContentTypeDefinitionController.getController().getContentTypeDefinitionList(db);
//				List<Category> categories = CategoryController.getController().findAllActiveCategories();
//				
//				InfoGlueExportImpl infoGlueExportImpl = new InfoGlueExportImpl();
//				
//				VisualFormatter visualFormatter = new VisualFormatter();
//				names = new VisualFormatter().replaceNonAscii(names, '_');
//
//				if(repositoryIds.length > 2 || names.length() > 40)
//					names = "" + repositoryIds.length + "_repositories";
//				
//				String fileName = "Export_" + names + "_" + visualFormatter.formatDate(new Date(), "yyyy-MM-dd_HHmm") + ".xml";
//				if(exportFileName != null && !exportFileName.equals(""))
//					fileName = exportFileName;
//				
//				String filePath = CmsPropertyHandler.getDigitalAssetPath();
//				String fileSystemName =  filePath + File.separator + fileName;
//							
//				exportInfo.fileURL = CmsPropertyHandler.getWebServerAddress() + "/" + CmsPropertyHandler.getDigitalAssetBaseUrl() + "/" + fileName;
//				// TODO what is this used for?
//				//this.fileName = fileName;
//							
//				String encoding = "UTF-8";
//				File file = new File(fileSystemName);
//	            FileOutputStream fos = new FileOutputStream(file);
//	            OutputStreamWriter osw = new OutputStreamWriter(fos, encoding);
//	            Marshaller marshaller = new Marshaller(osw);
//	            marshaller.setMapping(map);
//				marshaller.setEncoding(encoding);
//				DigitalAssetBytesHandler.setMaxSize(assetMaxSize);
//
//				infoGlueExportImpl.getRootContent().addAll(contents);
//				infoGlueExportImpl.getRootSiteNode().addAll(siteNodes);
//				
//				infoGlueExportImpl.setContentTypeDefinitions(contentTypeDefinitions);
//				infoGlueExportImpl.setCategories(categories);
//				
//				infoGlueExportImpl.setRepositoryProperties(allRepositoryProperties);
//				infoGlueExportImpl.setContentProperties(allContentProperties);
//				infoGlueExportImpl.setSiteNodeProperties(allSiteNodeProperties);
//				infoGlueExportImpl.setAccessRights(allAccessRights);
//				
//				marshaller.marshal(infoGlueExportImpl);
//				
//				osw.flush();
//				osw.close();
//				
//				db.rollback();
//				db.close();
//
//			} 
//			catch (Exception e) 
//			{
//				logger.error("An error was found exporting a repository: " + e.getMessage(), e);
//				db.rollback();
//			}
//			
//			logger.info("Finishing export in worker thread");
//		}
//	}
//	
//	public static class ExportInfo
//	{
//		private ExportStatus status = ExportStatus.Queued;
//		private int stepsDone = 0, stepsTotal = -1;
//		private String fileURL;
//		
//		public ExportStatus getStatus()
//		{
//			return status;
//		}
//		public int getStepsDone()
//		{
//			return stepsDone;
//		}
//		public int getStepsTotal()
//		{
//			return stepsTotal;
//		}
//	}
//	
//	enum ExportStatus{ Queued, Running, Finished, Error }
//}
