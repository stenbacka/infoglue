/**
 * 
 */
package org.infoglue.deliver.taglib.statkraft;

import java.io.File;
import java.io.FileFilter;
import java.io.FileReader;
import java.io.FileWriter;
import java.text.SimpleDateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.StringUtils;
import org.dom4j.Document;
import org.dom4j.DocumentException;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.dom4j.io.OutputFormat;
import org.dom4j.io.SAXReader;
import org.dom4j.io.XMLWriter;
import org.infoglue.cms.controllers.kernel.impl.simple.BaseController;
import org.infoglue.cms.entities.kernel.BaseEntityVO;

/**
 * @author Erik Stenbäcka
 */
public class StatkraftAssetController extends BaseController
{
	private static Map<String, StatkraftAssetController> instances;

	static {
		instances = new HashMap<String, StatkraftAssetController>();
	};

	// Settings
	private String assetXmlPath;
	private File outputFolder;
	private String assetBasePath;

//	private Element rootElement;
	private Document assetDocument;
	private boolean modified;
	private Thread saver;
	private boolean running;

	private Map<String, Asset> assetMap;
	private Pattern linkCleaningPattern;

	public static StatkraftAssetController getInstance(String key)
	{
		StatkraftAssetController instance = instances.get(key);
		if (instance == null)
		{
			try
			{
				File configFile = new File(key);
				if (!configFile.isFile())
				{
					throw new IllegalArgumentException("Config file does not exist");
				}
				Properties props = new Properties();
				props.load(new FileReader(configFile));
				instance = new StatkraftAssetController(props);
				instances.put(key, instance);
			}
			catch (Exception ex)
			{
				System.err.println("Something went wrong when initializing the asset controller. Message: " + ex.getMessage() + ". Key: " + key);
				ex.printStackTrace();
				instance = null;
			}
		}
		return instance;
	}

	private Document getDocument(String path) throws DocumentException
	{
		SAXReader reader = new SAXReader();
		File file = new File(path);
		if (!file.isFile())
		{
			throw new IllegalArgumentException("The given path was not a valid file. Path: " + path);
		}
		Document document = reader.read(file);
		return document;
	}

	private void setupLinkCleaner(Element rootElement)
	{
		Element rootPage = rootElement.element("page");
		String rootPagePath = rootPage.attributeValue("path");
		if (!rootPagePath.endsWith("/"))
		{
			rootPagePath = rootPagePath + "/";
		}
		linkCleaningPattern = Pattern.compile(rootPagePath);
	}
	
	private File getLatestOutputFile()
	{
		File[] files = outputFolder.listFiles(new FileFilter()
		{
			@Override
			public boolean accept(File pathname)
			{
				return pathname.isFile() && pathname.getName().startsWith("asset_mapping_");
			}
		});
		long lastMod = Long.MIN_VALUE;
		File choice = null;
		for (File file : files)
		{
			if (file.lastModified() > lastMod)
			{
				choice = file;
				lastMod = file.lastModified();
			}
		}
		return choice;
	}
	
	private void initFromOutput(File file)
	{
		
	}

	private void initAssetDomObject() throws DocumentException
	{
		File file = getLatestOutputFile();
		if (file != null)
		{
			initFromOutput(file);
		}
		else
		{
			Document doc = getDocument(assetXmlPath);
			this.assetDocument = DocumentHelper.createDocument();
			Element assetsElement = assetDocument.addElement("assets");
			Element inputRootElement = doc.getRootElement();
			setupLinkCleaner(inputRootElement);
	
			@SuppressWarnings("unchecked")
			List<Element> assets = doc.selectNodes("//img | //href");
			Map<String, Asset> assetCache = new HashMap<String, Asset>();
			for (Element assetElement : assets)
			{
				Element page = assetElement.getParent().getParent().getParent();
				Asset asset = getAsset(assetElement, assetsElement, assetCache);
				addPageToAssetsList(asset, page);
			}
			assetMap = new HashMap<String, Asset>();
			for (Asset asset : assetCache.values())
			{
				assetMap.put(asset.getAssetIdentifier(), asset);
			}
		}
	}

	private Asset getAsset(Element assetElement, Element assetContainer, Map<String, Asset> assetCache)
	{
		String id = null;

		String src = assetElement.attributeValue("src");
		if (src != null)
		{
			id = src;
		}
		else
		{
			String href = assetElement.attributeValue("href");
			if (href != null)
			{
				id = href;
			}
		}
		if (id == null)
		{
			throw new IllegalArgumentException("Given element is missing required attributes (src or href). Element: " + assetElement.asXML());
		}
		Asset asset = assetCache.get(id);
		if (asset == null)
		{
			asset = new Asset(assetElement);
			assetCache.put(id, asset);
			assetContainer.add( assetElement.detach() );
			assetElement.setName("asset");
		}
		else
		{
			asset.complementWithReference(assetElement);
		}
		return asset;
	}

	private boolean canWriteToOutput(File testOutputFolder)
	{
		if (!testOutputFolder.isDirectory())
		{
			return false;
		}
		try
		{
			File testFile = new File(testOutputFolder, "writetest.tmp");
			if (testFile.exists())
			{
				testFile.delete();
			}
			return testFile.createNewFile();
		}
		catch (Exception e)
		{
			System.err.println("We do not have write access to the outputfolder. Path: " + testOutputFolder.getAbsolutePath());
			return false;
		}
	}

	private StatkraftAssetController(Properties props) throws Exception
	{
		this.assetXmlPath = props.getProperty("assetXmlPath");
		String outputFolderPath = props.getProperty("outputFolder");
		this.assetBasePath = props.getProperty("assetBasePath");
		if (assetXmlPath == null || outputFolderPath == null || assetBasePath == null)
		{
			throw new IllegalArgumentException("Missing config property. Properties: " + props);
		}
		this.outputFolder = new File(outputFolderPath);
		if (!canWriteToOutput(outputFolder))
		{
			throw new IllegalArgumentException("OutputFolder is not a valid folder or we cannot write. Path: " + outputFolderPath);
		}
		initAssetDomObject();
		this.modified = false;
		this.saver = new Saver();
		this.saver.setName("Saver_" + StringUtils.substringAfterLast(assetXmlPath, "/"));
		this.running = true;
		this.saver.start();
	}

	private class Saver extends Thread
	{
		@Override
		public void run()
		{
			System.out.println("Starting save thread");

			while (running)
			{
				try
				{
					Thread.sleep(30 * 1000); // 2 * 60 * 1000
					if (modified)
					{
						String fileName = "asset_mapping_" + new SimpleDateFormat("yyyy-MM-dd HH_mm").format(new Date()) + ".xml";
						File file = new File(outputFolder, fileName);
						FileWriter writer = new FileWriter(file);
						OutputFormat format = OutputFormat.createPrettyPrint();
						XMLWriter xmlWriter = new XMLWriter(writer, format);
						xmlWriter.write(assetDocument);
						xmlWriter.close();
						modified = false;
					}
				}
				catch (InterruptedException iex)
				{
					System.out.println("Thread was interrupted. Lets stop!");
					running = false;
				}
				catch (Throwable tr)
				{
					System.out.println("Error in save thread. We will keep going though! Message: " + tr.getMessage());
				}
			}

			System.out.println("Ending save thread");
		}
	}

	public Asset updateAsset(String identifier, String description, String path)
	{
		Asset asset = assetMap.get(identifier);
		if (asset == null)
		{
			return null;
		}
		asset.update(description, path);
		this.modified = true;
		return asset;
	}

	public Collection<Asset> getAssets()
	{
		return assetMap.values();
	}

	@Override
	public BaseEntityVO getNewVO()
	{
		return null;
	}

	private void addPageToAssetsList(Asset asset, Element page)
	{
//		Element page = assetElement.getParent().getParent().getParent();
		if (!page.getName().equalsIgnoreCase("page"))
		{
			System.err.println("This is one weird asset! It does not have a page ancestor where it should. Asset: " + asset.getElement().asXML() + ". \n\nPage: " + page.asXML());
			throw new IllegalStateException("Asset did not have a page ancestor");
		}
		String path = page.attributeValue("path");
		path = linkCleaningPattern.matcher(path).replaceFirst("");

		asset.addPageReference(path, page);
	}

	public class Asset
	{
		public static final String SRC = "source";
		public static final String DESCRIPTION = "description";
		public static final String NEWPATH = "new-path";
		public static final String NEWSRC = "new-source";
		public static final String NEWDESCRIPTION = "new-description";
		public static final String MODIFIED = "modified";
		public static final String ID = "uuid";
		public static final String TYPES = "types";

		private Element assetElement;
		private Map<String, Element> pages;
		private boolean supportsDescription;

		public String getPreviewURL()
		{
			if (!getFileExtension().equalsIgnoreCase("pdf"))
			{
				return assetBasePath + getSource();
			}
			return null;
		}

		public String getSource()
		{
			return assetElement.attributeValue(SRC);
		}

		private void setSource(String source)
		{
			assetElement.addAttribute(SRC, source);
		}

		public String getDescription()
		{
			return assetElement.attributeValue(DESCRIPTION);
		}

		private void setDescription(String description)
		{
			assetElement.addAttribute(DESCRIPTION, description);
		}

		public void setNewPath(String path, String fileName)
		{
			String extension = getFileExtension();
			String newSource = path + "/" + fileName + "." + extension;
			assetElement.addAttribute(NEWPATH, path);
			setNewSource(newSource);
		}

		private String getFileExtension()
		{
			return getFileExtension(getSource());
		}

		private String getFileExtension(String source)
		{
			return StringUtils.substringAfterLast(source, ".");
		}

		public String getNewPath()
		{
			return assetElement.attributeValue(NEWPATH);
		}

		public String getNewSource()
		{
			return assetElement.attributeValue(NEWSRC);
		}

		public void setNewSource(String source)
		{
			assetElement.addAttribute(NEWSRC, source);
		}

		public String getNewDescription()
		{
			return assetElement.attributeValue(NEWDESCRIPTION);
		}

		public void setNewDescription(String description)
		{
			assetElement.addAttribute(NEWDESCRIPTION, description);
		}

		public String[] getTypes()
		{
			String typesString = assetElement.attributeValue(TYPES);
			return StringUtils.split(typesString, ",");
		}

		public void addType(String type)
		{
			String[] types = getTypes();
			if (!ArrayUtils.contains(types, type))
			{
				types = (String[]) ArrayUtils.add(types, type);
				assetElement.addAttribute(TYPES, StringUtils.join(types, ","));
			}
		}

		public String getAssetName()
		{
			String src = getSource();
			return StringUtils.substringAfterLast(src, "/");
		}

		public boolean getSupportsDescription()
		{
			return supportsDescription;
		}

		public Asset(Element assetElement)
		{
			// It is very important to set the assetElement property first since its value will be used in construction
			this.assetElement = assetElement;
			this.pages = new HashMap<String, Element>();
			
			normalizeElement();
			setNewSource(getSource());
			setNewDescription(getDescription());
			
			String path = assetElement.attributeValue(NEWPATH);
			if (path == null || path.equals(""))
			{
				assetElement.addAttribute(NEWPATH, getCurrentPath());
			}
		}
		
		private void normalizeElement()
		{
			String src = this.assetElement.attributeValue("src");
			if (src != null)
			{
				this.supportsDescription = true;
				setSource(src);
				String alt = this.assetElement.attributeValue("alt");
				if (alt != null)
				{
					setDescription(alt);
				}
				addType("image");
			}
			else
			{
				this.supportsDescription = false;
				String href = this.assetElement.attributeValue("href");
				if (href != null)
				{
					setSource(href);
					addType("link");
				}
				else
				{
					System.out.println("An asset element without source data :O  XML: " + assetElement.asXML());
				}
			}
		}
		
		private void complementWithReference(Element newElement)
		{
			String src = newElement.attributeValue("src");
			if (src != null)
			{
				this.supportsDescription = true;
				String currentDescription = getDescription();
				if (currentDescription == null || currentDescription.equals(""))
				{
					String alt = newElement.attributeValue("alt");
					setDescription(alt);
				}
				addType("image");
			}
			else
			{
				String href = newElement.attributeValue("href");
				if (href != null)
				{
					addType("link");
				}
				else
				{
					System.out.println("An asset element without source data :O  XML: " + newElement.asXML());
				}
			}
		}

		private String getCurrentPath()
		{
			return StringUtils.substringBeforeLast(getSource(), "/");
		}

		public void addPageReference(String path, Element page)
		{
			if (!path.startsWith("http"))
			{
				path = "http://" + path;
			}
			pages.put(path, page);
		}

		public Set<String> getPageNames()
		{
			return pages.keySet();
		}

		public boolean getIsModified()
		{
			String modifiedAttribute = assetElement.attributeValue(MODIFIED);
			return modifiedAttribute != null && modifiedAttribute.equalsIgnoreCase("true");
		}

		protected void setIsModified()
		{
			assetElement.addAttribute(MODIFIED, "" + true);
		}

		public String getAssetIdentifier()
		{
			if (assetElement == null)
			{
				return null;
			}
			String id = assetElement.attributeValue(ID);
			if (id == null)
			{
				UUID uuid = UUID.randomUUID();
				id = uuid.toString();
				assetElement.addAttribute(ID, id);
			}
			return id;
		}

		public Element getElement()
		{
			return assetElement;
		}

		public void update(String description, String path)
		{
			String name = description.replaceAll("\\W+", "-").toLowerCase();
			description = description.replaceAll("\\W+", " ");
			
			setNewDescription(description);
			setNewPath(path, name);
			setIsModified();
		}
	}
}
