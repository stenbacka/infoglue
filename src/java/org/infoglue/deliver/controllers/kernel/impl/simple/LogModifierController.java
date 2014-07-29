package org.infoglue.deliver.controllers.kernel.impl.simple;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

import org.apache.log4j.Appender;
import org.apache.log4j.ConsoleAppender;
import org.apache.log4j.Layout;
import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.PatternLayout;
import org.infoglue.cms.controllers.kernel.impl.simple.BaseController;
import org.infoglue.cms.entities.kernel.BaseEntityVO;
import org.infoglue.cms.util.CmsPropertyHandler;

public class LogModifierController extends BaseController
{
	private static final Logger classLogger = Logger.getLogger(LogModifierController.class);
	private static LogModifierController controller;

	private Map<Logger, ModificationInformation> currentModifications;
	private Appender debugAppender;
	private Appender componentDebugAppender;
	private Map<Integer, Level> levels;

	private static synchronized void initController()
	{
		if (controller == null)
		{
			controller = new LogModifierController();
		}
	}

	public static LogModifierController getController()
	{
		if (controller == null)
		{
			initController();
		}
		return controller;
	}

	private LogModifierController()
	{
		this.currentModifications = new HashMap<Logger, ModificationInformation>();
		initDebugAppender();
	}

	private void initDebugAppender()
	{
		if (debugAppender == null)
		{
			Logger dummyLogger = Logger.getLogger("org.infoglue.debug-dummy");
			debugAppender = dummyLogger.getAppender("INFOGLUE-DEBUG");
			if (debugAppender == null)
			{
				classLogger.warn("Did not find a debug appender. There should be an appender named INFOGLUE-DEBUG and it should be referenced by a category named org.infoglue.debug-dummy. Will use a Console appender instead.");
				Layout layout = new PatternLayout("%d{dd MMM yyyy HH:mm:ss.SSS} [Debug log] [%-5p] [%t] [%c] - %m%n");
				ConsoleAppender appender = new ConsoleAppender(layout);
				appender.setThreshold(Level.TRACE);
				debugAppender = appender;
			}
			componentDebugAppender = dummyLogger.getAppender("INFOGLUE-COMPONENT-DEBUG");
			if (componentDebugAppender == null)
			{
				classLogger.warn("Did not find a component debug appender. There should be an appender named INFOGLUE-COMPONENT-DEBUG and it should be referenced by a category named org.infoglue.debug-dummy. Will use a Console appender instead.");
				Layout layout = new PatternLayout("%d{ISO8601} - %p%n[%t] [%c] [SiteNodeId: %X{siteNodeId}] [Principal: %X{principalName}] [Language: %X{languageId}] [ComponentId: %X{componentId}]%nMessage: %m%n%X{timerValue}%n%n");
				ConsoleAppender appender = new ConsoleAppender(layout);
				appender.setThreshold(Level.TRACE);
				componentDebugAppender = appender;
			}
		}
	}

	private void populateLevels()
	{
		classLogger.debug("Populating levels");
		levels = new TreeMap<Integer, Level>();
		levels.put(Level.TRACE.toInt(), Level.TRACE);
		levels.put(Level.DEBUG.toInt(), Level.DEBUG);
		levels.put(Level.INFO.toInt(), Level.INFO);
		levels.put(Level.WARN.toInt(), Level.WARN);
		levels.put(Level.ERROR.toInt(), Level.ERROR);
		levels.put(Level.FATAL.toInt(), Level.FATAL);
		levels.put(Level.OFF.toInt(), Level.OFF);
	}

	/**
	 * Provides a Map of the different logger levels available in Log4j. The key is the integer representation for the
	 * level used by log4j and the value is a reference to the corresponding {@link Level} object.
	 * @return
	 */
	public Map<Integer, Level> getLevels()
	{
		if (levels == null)
		{
			populateLevels();
		}
		return levels;
	}

	/**
	 * Gets the logger for the given <em>loggerName</em> and converts the integer representation of the logger level
	 * then calls {@link #changeLogLevel(Logger, Level)}.
	 */
	public void changeLogLevel(String loggerName, Integer loggerLevel)
	{
		Logger logger = Logger.getLogger(loggerName);
		Level level = loggerLevel == null ? null : Level.toLevel(loggerLevel);
		changeLogLevel(logger, level);
	}

	/**
	 * Gets the logger for the given <em>loggerName</em> and calls {@link #changeLogLevel(Logger, Level)}.
	 */
	public void changeLogLevel(String loggerName, Level level)
	{
		Logger logger = Logger.getLogger(loggerName);
		changeLogLevel(logger, level);
	}

	/**
	 * Converts the integer representation of the logger level and calls {@link #changeLogLevel(Logger, Level)}.
	 * The integer value should correspond to one of the values Log4j uses to represent its logger levels.
	 */
	public void changeLogLevel(Logger logger, Integer loggerLevel)
	{
		Level level = loggerLevel == null ? null : Level.toLevel(loggerLevel);
		changeLogLevel(logger, level);
	}

	public synchronized void changeLogLevel(Logger logger, Level level)
	{
		if (logger == null)
		{
			classLogger.warn("Tried to change logging level without the required information. Logger: " + logger + ". Level " + level);
		}
		else
		{
			if (level == null)
			{
				clearModifications(logger);
			}
			else
			{
				ModificationInformation currentState = currentModifications.get(logger);
				if (currentState == null)
				{
					classLogger.info("Logger has not been modified before this. Logger: " + logger.getName() + ". New level: " + level);
					currentState = new ModificationInformation();
					currentState.originalLevel = logger.getLevel();
					currentState.currentLevel = level;
					logger.setLevel(level);
					currentModifications.put(logger, currentState);

					String componentLoggerBasePackage = CmsPropertyHandler.getInfoglueComponentLoggingBasePackage();
					if (logger.getName().startsWith(componentLoggerBasePackage))
					{
						classLogger.info("This logger is a component logger.  Attaching the logger to the component debug appender. Logger name: " + logger.getName());
						logger.addAppender(componentDebugAppender);
					}
					else
					{
						classLogger.info("This logger is a core logger. Attaching the logger to the debug appender. Logger name: " + logger.getName());
						logger.addAppender(debugAppender);
					}
				}
				else
				{
					classLogger.info("Logger has been modified before this. Logger: " + logger.getName() + ". New level: " + level);
					currentState.currentLevel = level;
					logger.setLevel(level);
				}
			}
		}
	}

	/**
	 * Gets the Log4j logger with the name <em>loggerName</em> and calls {@link #clearModifications(Logger)}.
	 * @param loggerName The name of the logger to clear
	 */
	public void clearModifications(String loggerName)
	{
		clearModifications(Logger.getLogger(loggerName));
	}

	/**
	 * <p>If a logger has been modified using {@link #changeLogLevel(Logger, Level)} calling this method will restore the logger to its
	 * initial logging level and remove it from the debug appender. If the logger has not been modified by <em>changeLogLevel</em> this method does nothing.</p>
	 * 
	 * <p>This method cannot revert changes made by called {@link Logger#setLevel(Level)} directly. It is limited to changes this class has been able to
	 * record it self.</p>
	 * @param logger The logger which level should be reset
	 */
	public synchronized void clearModifications(Logger logger)
	{
		ModificationInformation currentState = currentModifications.get(logger);
		if (currentState == null)
		{
			classLogger.info("No modifications to clear in logger. Logger: " + logger.getName());
		}
		else
		{
			classLogger.debug("Clearing modifications for Logger. Logger: " + logger.getName() + ". Modificatons: " + currentState);
			logger.setLevel(currentState.originalLevel);
			
			String componentLoggerBasePackage = CmsPropertyHandler.getInfoglueComponentLoggingBasePackage();
			if (logger.getName().startsWith(componentLoggerBasePackage))
			{
				classLogger.info("This logger is a component logger.  Detaching the logger from the component debug appender. Logger name: " + logger.getName());
				logger.removeAppender(componentDebugAppender);
			}
			else
			{
				classLogger.info("This logger is a core logger. Detaching the logger from the debug appender. Logger name: " + logger.getName());
				logger.removeAppender(debugAppender);
			}
			currentModifications.remove(logger);
		}
	}

	/**
	 * Returns a set of logger whose logging level has been changed using {@link #changeLogLevel(Logger, Level)}.
	 */
	public Set<Logger> getCurrentModifications()
	{
		return currentModifications.keySet();
	}

	/**
	 * Does nothing.
	 */
	@Override
	public BaseEntityVO getNewVO()
	{
		return null;
	}

	/**
	 * A model to keep track of logger modifications. Used as the value in a <em>Map</em>.
	 */
	private static class ModificationInformation
	{
		Level currentLevel;
		Level originalLevel;

		@Override
		public String toString()
		{
			return "[currentLevel: " + currentLevel + ", originalLevel: " + originalLevel + "]";
		}
	}

}
