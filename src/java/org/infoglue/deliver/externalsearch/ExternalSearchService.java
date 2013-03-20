/**
 * 
 */
package org.infoglue.deliver.externalsearch;

import java.io.File;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.log4j.Logger;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.infoglue.cms.exception.ConfigurationError;
import org.infoglue.cms.util.CmsPropertyHandler;

/**
 * @author Erik Stenbäcka
 *
 */
public class ExternalSearchService
{
	private static final Logger logger = Logger.getLogger(ExternalSearchService.class);

	private AtomicBoolean running;
	private String name;
	private Date creationDateTime;
	private Integer maxAge;
	private List<String> dependencies;

	private DataRetriever dataRetriever;
	private Parser parser;
	private Indexer indexer;

//	private ExternalSearchServiceConfig config;
	private ExternalSearchServiceConfig newConfig;
	private Directory directory;

	public ExternalSearchService(ExternalSearchServiceConfig config)
	{
		this.name = config.getName();
		this.running = new AtomicBoolean(false);
		setConfig(config);
	}

	public void setConfig(ExternalSearchServiceConfig newConfig) throws ConfigurationError
	{
		logger.info("Queing new config for service: " + name);
		this.newConfig = newConfig;
		if (running.compareAndSet(false, true))
		{
			updateConfig();
			running.set(false);
		}
	}

	public boolean startIndexing()
	{
		if (indexHasExpired() && checkDependencies())
		{
			if (running.compareAndSet(false, true))
			{
				logger.debug("Should start indexing for service: " + name);
				new Thread() {
					@Override
					public void run()
					{
						logger.info("Starting indexing for service: " + name);
						updateIndex();
						updateConfig();
						running.set(false);
					}
				}.start();

				return true;
			}
		}

		return false;
	}

	public SearchResult search(SearchParameters params)
	{
		return new SearchResult(null, 0);
	}

	public void destroyService()
	{
		if (this.dataRetriever != null)
		{
			this.dataRetriever.destroy();
		}
		if (this.parser != null)
		{
			this.parser.destroy();
		}
		if (this.indexer != null)
		{
			this.indexer.destroy();
		}
	}

	public boolean isSearchable()
	{
		return directory != null;
	}

	public boolean indexHasExpired()
	{
		if (this.maxAge == null)
		{
			return false;
		}
		else if (this.creationDateTime == null)
		{
			return true;
		}
		else
		{
			return new Date().getTime() > (creationDateTime.getTime() + this.maxAge);
		}
	}

	/////////////////////////////////////////////////////////////////

	private void updateConfig()
	{
		if (newConfig != null)
		{
			logger.info("Updating config for service: " + name);

			this.maxAge = this.newConfig.getMaxAge();
			this.dependencies = this.newConfig.getDependencis();

			if (this.dataRetriever != null)
			{
				this.dataRetriever.destroy();
			}
			this.dataRetriever = this.newConfig.getDataRetriever();
			this.dataRetriever.init();

			if (this.parser != null)
			{
				this.parser.destroy();
			}
			this.parser = this.newConfig.getParser();
			this.parser.init();

			if (this.indexer != null)
			{
				this.indexer.destroy();
			}
			this.indexer = this.newConfig.getIndexer();
			this.indexer.init();

			this.newConfig = null;
		}
		else
		{
			logger.debug("No new config to update to for service name: " + name);
		}
	}

	private void updateIndex()
	{

		InputStream input = this.dataRetriever.openConnection();
		List<Map<String, Object>> entities = this.parser.parse(input);
		this.dataRetriever.closeConnection();

		String directoryFileName = getLuceneDirectoryPath() + name + "_" + System.currentTimeMillis() + ".directory";
		File directoryFile = new File(directoryFileName);
		logger.info("Creating new directory. The directory will be stored in the file: " + directoryFile.getName());

		if (directoryFile.exists())
		{
			logger.warn("Directory name candidate already exists. Directory name: " + directoryFile.getName());
		}

//		FSDirectory idx = FSDirectory.open(directoryFile);

//		StandardAnalyzer analyzer = new StandardAnalyzer();
//		IndexWriter iw = new IndexWriter(idx, analyzer, true);
	}

	private String getLuceneDirectoryPath()
	{
		StringBuilder sb = new StringBuilder();
		return sb.append(CmsPropertyHandler.getContextRootPath()).append(File.separator).append("lucene").append(File.separator).toString();
	}

	private boolean checkDependencies()
	{
		if (dependencies != null)
		{
			for (String serviceName : dependencies)
			{
				logger.info("Checking dependency for service: " + name + ". Dependency: " + serviceName);
				ExternalSearchService service = ExternalSearchManager.getManager().getService(serviceName);
				if (service == null)
				{
					logger.debug("Dependecy was not found");
					return false;
				}
				else
				{
					if (!service.isSearchable())
					{
						logger.debug("Dependecy was not searchable");
						return false;
					}
				}
			}
		}
		return true;
	}


	@Override
	public String toString()
	{
		return name;
	}
}
