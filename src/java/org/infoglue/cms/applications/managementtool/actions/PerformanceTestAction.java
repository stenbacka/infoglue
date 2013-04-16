package org.infoglue.cms.applications.managementtool.actions;

import java.util.ArrayList;
import java.util.List;

import org.infoglue.cms.applications.common.actions.InfoGlueAbstractAction;
import org.infoglue.cms.controllers.kernel.impl.simple.SiteNodeController;
import org.infoglue.deliver.util.Timer;

public class PerformanceTestAction extends InfoGlueAbstractAction
{
	private static final long serialVersionUID = 157567567567L;

	private static final List<PerformanceTest> availableTests;

	static {
		availableTests = new ArrayList<PerformanceTest>();
		availableTests.add(new PerformanceTest("siteNodeList", "List SiteNodes", 1000, new PerformanceTest.PerformanceTestExecution()
		{
			@Override
			public boolean execute(int sampleSize)
			{
				try
				{
//					java.util.List<SiteNodeVO> nodes = 
					SiteNodeController.getController().getSiteNodeVOList(false, 0, sampleSize);
				}
				catch (Exception e)
				{
					e.printStackTrace();
					return false;
				}
				return true;
			}
		}));
	};

	@Override
	protected String doExecute() throws Exception
	{
		
		
		return SUCCESS;
	}

	public List<PerformanceTest> availableTests()
	{
		return PerformanceTestAction.availableTests;
	}

	public static class PerformanceTest
	{
		private String name;
		private String displayName;
		private int sampleSize;
		private PerformanceTestExecution test;

		public PerformanceTest(String name, String displayName, int sampleSize, PerformanceTestExecution test)
		{
			this.name = name;
			this.displayName = displayName;
			this.sampleSize = sampleSize;
			this.test = test;
		}

		public PerformanceTestResult execute()
		{
			Timer t = new Timer();
			boolean testSuccess = test.execute(sampleSize);
			PerformanceTestResult result = new PerformanceTestResult(t.getElapsedTime(), this);
			return result;
		}

		public String getName()
		{
			return name;
		}

		public String getDisplayName()
		{
			return displayName;
		}

		public int getSampleSize()
		{
			return sampleSize;
		}

		static abstract class PerformanceTestExecution
		{
			public abstract boolean execute(int sampleSize);
		}
	}

	static class PerformanceTestResult
	{
		private float duration;
		private PerformanceTest test;

		public PerformanceTestResult(float duration, PerformanceTest test)
		{
			this.duration = duration;
			this.test = test;
		}

		public float getDuration()
		{
			return duration;
		}

		public PerformanceTest getTest()
		{
			return test;
		}
	}
}
