/*
 * Copyright (c) 2006-, IPD Boehm, Universitaet Karlsruhe (TH) / KIT, by Guido Sautter
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 *     * Redistributions of source code must retain the above copyright
 *       notice, this list of conditions and the following disclaimer.
 *     * Redistributions in binary form must reproduce the above copyright
 *       notice, this list of conditions and the following disclaimer in the
 *       documentation and/or other materials provided with the distribution.
 *     * Neither the name of the Universität Karlsruhe (TH) nor the
 *       names of its contributors may be used to endorse or promote products
 *       derived from this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY UNIVERSITÄT KARLSRUHE (TH) / KIT AND CONTRIBUTORS 
 * "AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
 * THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
 * ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package de.uka.ipd.idaho.goldenGate.markupWizard.app;

import java.awt.Image;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.ArrayList;

import javax.swing.JMenuItem;

import de.uka.ipd.idaho.easyIO.help.HelpChapter;
import de.uka.ipd.idaho.easyIO.settings.Settings;
import de.uka.ipd.idaho.gamta.util.validation.Level;
import de.uka.ipd.idaho.gamta.util.validation.Process;
import de.uka.ipd.idaho.gamta.util.validation.Step;
import de.uka.ipd.idaho.gamta.util.validation.Task;
import de.uka.ipd.idaho.goldenGate.CustomFunction;
import de.uka.ipd.idaho.goldenGate.GoldenGATE;
import de.uka.ipd.idaho.goldenGate.GoldenGateConfiguration;
import de.uka.ipd.idaho.goldenGate.markupWizard.ProcessPartMapper;
import de.uka.ipd.idaho.goldenGate.markupWizard.ProcessPartMapper.ProcessPartMapping;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessor;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessorManager;
import de.uka.ipd.idaho.goldenGate.plugins.GoldenGatePlugin;
import de.uka.ipd.idaho.goldenGate.plugins.GoldenGatePluginDataProvider;
import de.uka.ipd.idaho.goldenGate.plugins.ResourceManager;
import de.uka.ipd.idaho.goldenGate.plugins.SettingsPanel;
import de.uka.ipd.idaho.goldenGate.util.ResourceSelector;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * Specialized GoldenGATE Configuration for Markup Wizards. This configuration
 * wraps another GoldenGATE Configuration, adding a specialized custom function
 * manager that creates custom functions from the steps in a markup process
 * definition.
 * 
 * @author sautter
 */
public class MarkupWizardConfiguration implements GoldenGateConfiguration {
	
	private GoldenGateConfiguration ggConfig;
	private Process process;
	private ProcessPartMapper mapper;
	
	private GoldenGatePlugin[] plugins = null;
	
	/**
	 * Constructor
	 * @param ggConfig the GoldenGATE Configuration to wrap
	 * @param process the markup process definition to generate custom functions
	 *            from
	 * @param mapper the mapper providing the document processors for the steps
	 *            of the markup process
	 */
	public MarkupWizardConfiguration(GoldenGateConfiguration ggConfig, Process process, ProcessPartMapper mapper) {
		this.ggConfig = ggConfig;
		this.process = process;
		this.mapper = mapper;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.GoldenGateConfiguration#allowWebAccess()
	 */
	public boolean allowWebAccess() {
		return this.ggConfig.allowWebAccess();
	}

	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.GoldenGateConfiguration#getHelpBaseURL()
	 */
	public String getHelpBaseURL() {
		return this.ggConfig.getHelpBaseURL();
	}

	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.GoldenGateConfiguration#getIconImage()
	 */
	public Image getIconImage() {
		return this.ggConfig.getIconImage();
	}

	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.GoldenGateConfiguration#getName()
	 */
	public String getName() {
		return this.ggConfig.getName();
	}

	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.GoldenGateConfiguration#getPath()
	 */
	public String getPath() {
		return this.ggConfig.getPath();
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.GoldenGateConfiguration#getAbsolutePath()
	 */
	public String getAbsolutePath() {
		return this.ggConfig.getAbsolutePath() + "$MW";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.GoldenGateConfiguration#isMasterConfiguration()
	 */
	public boolean isMasterConfiguration() {
		return false;
	}

	/* (non-Javadoc)
	 * @see java.lang.Object#finalize()
	 */
	public void finalize() throws Throwable {
		this.ggConfig.finalize();
	}

	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.GoldenGateConfiguration#getPlugins()
	 */
	public GoldenGatePlugin[] getPlugins() {
		if (this.plugins == null) {
			GoldenGatePlugin[] plugins = this.ggConfig.getPlugins();
			ArrayList pluginList = new ArrayList();
			boolean gotCfm = false;
			for (int p = 0; p < plugins.length; p++) {
				if (plugins[p] instanceof CustomFunction.Manager) {
					if (!gotCfm) {
						if (Runtime.getRuntime().maxMemory() < (128 * 1024 * 1024))
							pluginList.add(plugins[p]);
						else pluginList.add(new MarkupWizardCustomFunctionManager(((CustomFunction.Manager) plugins[p]), this.process, this.mapper));
					}
					gotCfm = true;
				}
				else pluginList.add(plugins[p]);
			}
			if (!gotCfm)
				pluginList.add(new MarkupWizardCustomFunctionManager(null, this.process, this.mapper));
			this.plugins = ((GoldenGatePlugin[]) pluginList.toArray(new GoldenGatePlugin[pluginList.size()]));
		}
		
		GoldenGatePlugin[] plugins = new GoldenGatePlugin[this.plugins.length];
		System.arraycopy(this.plugins, 0, plugins, 0, plugins.length);
		return plugins;
	}

	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.GoldenGateConfiguration#getFileMenuItems()
	 */
	public JMenuItem[] getFileMenuItems() {
		return this.ggConfig.getFileMenuItems();
	}

	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.GoldenGateConfiguration#getWindowMenuItems()
	 */
	public JMenuItem[] getWindowMenuItems() {
		return this.ggConfig.getWindowMenuItems();
	}

	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.GoldenGateConfiguration#getSettings()
	 */
	public Settings getSettings() {
		return this.ggConfig.getSettings();
	}

	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.GoldenGateConfiguration#storeSettings(de.uka.ipd.idaho.easyIO.settings.Settings)
	 */
	public void storeSettings(Settings settings) throws IOException {
		this.ggConfig.storeSettings(settings);
	}

	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.GoldenGateConfiguration#writeLog(java.lang.String)
	 */
	public void writeLog(String entry) {
		this.ggConfig.writeLog(entry);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.GoldenGateConfiguration#getDataNames()
	 */
	public String[] getDataNames() {
		return this.ggConfig.getDataNames();
	}

	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.GoldenGateConfiguration#isDataAvailable(java.lang.String)
	 */
	public boolean isDataAvailable(String dataName) {
		return this.ggConfig.isDataAvailable(dataName);
	}

	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.GoldenGateConfiguration#getInputStream(java.lang.String)
	 */
	public InputStream getInputStream(String dataName) throws IOException {
		return this.ggConfig.getInputStream(dataName);
	}

	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.GoldenGateConfiguration#isDataEditable()
	 */
	public boolean isDataEditable() {
		return this.ggConfig.isDataEditable();
	}

	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.GoldenGateConfiguration#isDataEditable(java.lang.String)
	 */
	public boolean isDataEditable(String dataName) {
		return this.ggConfig.isDataEditable(dataName);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.GoldenGateConfiguration#getOutputStream(java.lang.String)
	 */
	public OutputStream getOutputStream(String dataName) throws IOException {
		return this.ggConfig.getOutputStream(dataName);
	}

	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.GoldenGateConfiguration#deleteData(java.lang.String)
	 */
	public boolean deleteData(String dataName) {
		return this.ggConfig.deleteData(dataName);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.GoldenGateConfiguration#getURL(java.lang.String)
	 */
	public URL getURL(String dataName) throws IOException {
		return this.ggConfig.getURL(dataName);
	}

	/**
	 * Special custom function manager for markup wizards.
	 * 
	 * @author sautter
	 */
	private static class MarkupWizardCustomFunctionManager implements CustomFunction.Manager {
		
		private GoldenGATE parent;
		private CustomFunction.Manager dcfm;
		
		private Process process;
		private ProcessPartMapper mapper;
		private MarkupWizardCustomFunction[] mwCfs;
		
		MarkupWizardCustomFunctionManager(CustomFunction.Manager dcfm, Process process, ProcessPartMapper mapper) {
			this.dcfm = dcfm;
			this.process = process;
			this.mapper = mapper;
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#init()
		 */
		public void init() {
			if (this.dcfm != null)
				this.dcfm.init();
			
			ArrayList mwCfList = new ArrayList();
			Level[] levels = this.process.getLevels();
			for (int l = 0; l < levels.length; l++) {
				Task[] tasks = levels[l].getTasks();
				for (int t = 0; t < tasks.length; t++) {
					Step[] steps = tasks[t].getSteps();
					for (int s = 0; s < steps.length; s++) {
						String cfLabel = steps[s].getLabel();
						if (cfLabel.length() == 0)
							cfLabel = steps[s].getName();
						cfLabel = ((l + 1) + "." + (t + 1) + "." + (s + 1) + " " + cfLabel);
						String prefix = ("" + mwCfList.size());
						while (prefix.length() < 3)
							prefix = ("0" + prefix);
						MarkupWizardCustomFunction mwcf = this.buildCustomFunction(prefix, cfLabel, steps[s]);
						if (mwcf != null) mwCfList.add(mwcf);
					}
				}
			}
			this.mwCfs = ((MarkupWizardCustomFunction[]) mwCfList.toArray(new MarkupWizardCustomFunction[mwCfList.size()]));
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#exit()
		 */
		public void exit() {
			if (this.dcfm != null)
				this.dcfm.exit();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.GoldenGatePlugin#getAboutBoxExtension()
		 */
		public String getAboutBoxExtension() {
			return null;
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#getHelpMenuItem()
		 */
		public JMenuItem getHelpMenuItem() {
			return ((this.dcfm == null) ? null : this.dcfm.getHelpMenuItem());
		}

		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#getHelp()
		 */
		public HelpChapter getHelp() {
			return ((this.dcfm == null) ? null : this.dcfm.getHelp());
		}

		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.GoldenGatePlugin#getMainMenuItems()
		 */
		public JMenuItem[] getMainMenuItems() {
			return ((this.dcfm == null) ? new JMenuItem[0] : this.dcfm.getMainMenuItems());
		}

		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.GoldenGatePlugin#getMainMenuTitle()
		 */
		public String getMainMenuTitle() {
			return ((this.dcfm == null) ? null : this.dcfm.getMainMenuTitle());
		}

		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.GoldenGatePlugin#getPluginName()
		 */
		public String getPluginName() {
			return ((this.dcfm == null) ? "Custom Functions" : this.dcfm.getPluginName());
		}

		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.GoldenGatePlugin#getToolsMenuFunctionItems(de.uka.ipd.idaho.goldenGate.GoldenGateConstants.InvokationTargetProvider)
		 */
		public JMenuItem[] getToolsMenuFunctionItems(InvokationTargetProvider targetProvider) {
			return ((this.dcfm == null) ? new JMenuItem[0] : this.dcfm.getToolsMenuFunctionItems(targetProvider));
		}

		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.GoldenGatePlugin#getSettingsPanel()
		 */
		public SettingsPanel getSettingsPanel() {
			return ((this.dcfm == null) ? null : this.dcfm.getSettingsPanel());
		}

		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.ResourceManager#getToolsMenuLabel()
		 */
		public String getToolsMenuLabel() {
			return ((this.dcfm == null) ? null : this.dcfm.getToolsMenuLabel());
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.ResourceManager#getResourceTypeLabel()
		 */
		public String getResourceTypeLabel() {
			return ((this.dcfm == null) ? "Custom Function" : this.dcfm.getResourceTypeLabel());
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.GoldenGatePlugin#isOperational()
		 */
		public boolean isOperational() {
			return ((this.parent != null) && ((this.dcfm == null) || this.dcfm.isOperational()));
		}

		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.GoldenGatePlugin#setDataProvider(de.uka.ipd.idaho.goldenGate.plugins.GoldenGatePluginDataProvider)
		 */
		public void setDataProvider(GoldenGatePluginDataProvider dataProvider) {
			if (this.dcfm != null)
				this.dcfm.setDataProvider(dataProvider);
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.GoldenGatePlugin#setParent(de.uka.ipd.idaho.goldenGate.GoldenGATE)
		 */
		public void setParent(GoldenGATE parent) {
			this.parent = parent;
			if (this.dcfm != null)
				this.dcfm.setParent(parent);
		}

		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getResourceNames()
		 */
		public String[] getResourceNames() {
			StringVector nameCollector = new StringVector();
			for (int c = 0; c < this.mwCfs.length; c++)
				nameCollector.addElement(this.mwCfs[c].name);
			if (this.dcfm != null)
				nameCollector.addContent(this.dcfm.getResourceNames());
			return nameCollector.toStringArray();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.ResourceManager#getSelector(java.lang.String)
		 */
		public ResourceSelector getSelector(String label) {
			return this.getSelector(label, null);
		}

		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.ResourceManager#getSelector(java.lang.String, java.lang.String)
		 */
		public ResourceSelector getSelector(String label, String initialSelection) {
			return new ResourceSelector(this, initialSelection, label);
		}

		/** @see de.uka.ipd.idaho.goldenGate.plugins.ResourceManager#getDataNamesForResource(java.lang.String)
		 */
		public String[] getDataNamesForResource(String name) {
			if (!name.endsWith(MarkupWizardCustomFunction.FILE_EXTENSION))
				return ((this.dcfm == null) ? new String[0] : this.dcfm.getDataNamesForResource(name));
			
			StringVector nameCollector = new StringVector();
			nameCollector.addElementIgnoreDuplicates(name + "@" + this.getClass().getName());
			
			MarkupWizardCustomFunction mwcf = this.getMwCustomFunction(name);
			if (mwcf == null)
				nameCollector.toStringArray();
			
			ResourceManager rm = this.parent.getResourceProvider(mwcf.dpProviderClassName);
			if (rm != null)
				nameCollector.addContentIgnoreDuplicates(rm.getDataNamesForResource(mwcf.dpName));
//			for (int p = 0; p < mwcf.dpNames.length; p++) {
//				ResourceManager rm = this.parent.getResourceProvider(mwcf.dpProviderClassNames[p]);
//				if (rm != null)
//					nameCollector.addContentIgnoreDuplicates(rm.getDataNamesForResource(mwcf.dpNames[p]));
//			}
			
			return nameCollector.toStringArray();
		}
		
		/** @see de.uka.ipd.idaho.goldenGate.plugins.ResourceManager#getRequiredResourceNames(java.lang.String, boolean)
		 */
		public String[] getRequiredResourceNames(String name, boolean recourse) {
			if (!name.endsWith(MarkupWizardCustomFunction.FILE_EXTENSION))
				return ((this.dcfm == null) ? new String[0] : this.dcfm.getRequiredResourceNames(name, recourse));
			
			StringVector nameCollector = new StringVector();
			
			MarkupWizardCustomFunction mwcf = this.getMwCustomFunction(name);
			if (mwcf == null) nameCollector.toStringArray();
			
			nameCollector.addElement(mwcf.dpFullName);
//			nameCollector.addContent(mwcf.dpFullNames);
			
			int nameIndex = 0;
			while (recourse && (nameIndex < nameCollector.size())) {
				String resName = nameCollector.get(nameIndex);
				int split = resName.indexOf('@');
				if (split != -1) {
					String plainResName = resName.substring(0, split);
					String resProviderClassName = resName.substring(split + 1);
					
					ResourceManager rm = this.parent.getResourceProvider(resProviderClassName);
					if (rm != null)
						nameCollector.addContentIgnoreDuplicates(rm.getRequiredResourceNames(plainResName, recourse));
				}
				nameIndex++;
			}
			
			return nameCollector.toStringArray();
		}
		
		/**
		 * retrieve a custom function by its name
		 * @param name the name of the reqired customFunction
		 * @return the custom function with the required name, or null, if there is
		 *         no such custom function
		 */
		public CustomFunction getCustomFunction(String name) {
			if (name == null)
				return null;
			else if (!name.endsWith(MarkupWizardCustomFunction.FILE_EXTENSION))
				return ((this.dcfm == null) ? null : this.dcfm.getCustomFunction(name));
			else return this.wrapMwCustomFunction(this.getMwCustomFunction(name));
		}
		
		private MarkupWizardCustomFunction getMwCustomFunction(String name) {
			for (int c = 0; c < this.mwCfs.length; c++) {
				if (this.mwCfs[c].name.equals(name))
					return this.mwCfs[c];
			}
			return null;
		}
		
		private CustomFunction wrapMwCustomFunction(final MarkupWizardCustomFunction mwcf) {
			if (mwcf == null)
				return null;
			DocumentProcessorManager dpm = this.parent.getDocumentProcessorProvider(mwcf.dpProviderClassName);
			if (dpm == null) {
				DocumentProcessor dp = parent.getDocumentProcessorForName(mwcf.dpFullName);
				if (dp != null)
					dpm = this.parent.getDocumentProcessorProvider(dp.getProviderClassName());
			}
			if (dpm == null)
				return null;
			return new CustomFunction(mwcf.label, mwcf.toolTip, dpm, mwcf.dpName, true, false);
		}
		
		private MarkupWizardCustomFunction buildCustomFunction(String prefix, String label, Step step) {
			ProcessPartMapping mapping = this.mapper.getProcessPartMapping(step.getFullName());
			if ((mapping == null) || (mapping.documentProcessorName == null))
				return null;
			
			String cfName = (prefix + step.getFullName() + MarkupWizardCustomFunction.FILE_EXTENSION);
			String cfLabel = ((label == null) ? step.getLabel() : label);
			if (cfLabel.length() == 0)
				cfLabel = step.getName();
			String cfToolTip = step.getDescription();
			
			String dpName;
			String dpPcName;
			int split = mapping.documentProcessorName.indexOf('@');
			if (split == -1) {
				dpName = mapping.documentProcessorName;
				dpPcName = "";
			}
			else {
				dpName = mapping.documentProcessorName.substring(0, split);
				dpPcName = mapping.documentProcessorName.substring(split + 1);
			}
			
			return new MarkupWizardCustomFunction(cfName, cfLabel, cfToolTip, mapping.documentProcessorName, dpName, dpPcName);
		}
		
		static class MarkupWizardCustomFunction {
			
			static final String FILE_EXTENSION = ".mwCustomFunction";
			
			final String name;
			final String label;
			final String toolTip;
			final String dpFullName;
			final String dpName;
			final String dpProviderClassName;
			
			MarkupWizardCustomFunction(String name, String label, String toolTip, String dpFullName, String dpName, String dpProviderClassName) {
				this.name = name;
				this.label = label;
				this.toolTip = toolTip;
				this.dpFullName = dpFullName;
				this.dpName = dpName;
				this.dpProviderClassName = dpProviderClassName;
			}
		}
		
//		IN THE SIMPLIFIED DESIGN, DOCUMENT PROCESSORS ARE ASSIGNED EXCLUSIVELY TO STEPS !!!
//		
//		private CustomFunction wrapMwCustomFunction(final MarkupWizardCustomFunction mwcf) {
//			if (mwcf == null)
//				return null;
//			
//			DocumentProcessor dp;
//			if (mwcf.dpFullNames.length == 1)
//				dp = this.parent.getDocumentProcessorForName(mwcf.dpFullNames[0]);
//			
//			else dp = new DocumentProcessor() {
//				public void process(MutableAnnotation data) {
//					for (int p = 0; p < mwcf.dpFullNames.length; p++) {
//						DocumentProcessor dp = parent.getDocumentProcessorForName(mwcf.dpFullNames[p]);
//						if (dp != null)
//							dp.process(data);
//					}
//				}
//				public void process(MutableAnnotation data, Properties parameters) {
//					for (int p = 0; p < mwcf.dpFullNames.length; p++) {
//						DocumentProcessor dp = parent.getDocumentProcessorForName(mwcf.dpFullNames[p]);
//						if (dp != null)
//							dp.process(data, parameters);
//					}
//				}
//				public String getName() {
//					return mwcf.name;
//				}
//				public String getProviderClassName() {
//					return MarkupWizardCustomFunctionManager.class.getName();
//				}
//				public String getTypeLabel() {
//					return "Custom Function";
//				}
//			};
//			
//			return new CustomFunction(mwcf.label, mwcf.toolTip, dp, true, false);
//		}
//		
//		private MarkupWizardCustomFunction buildCustomFunction(String prefix, String label, Step step) {
//			StringVector processorCollector = new StringVector();
//			
//			ProcessPartMapping mapping = this.mapper.getProcessPartMapping(step.getFullName());
//			if ((mapping == null) || (mapping.documentProcessorName == null)) {
//				Criterion[] criterions = step.getCriterions();
//				for (int c = 0; c < criterions.length; c++) {
//					mapping = this.mapper.getProcessPartMapping(criterions[c].getFullName());
//					if ((mapping != null) && (mapping.documentProcessorName != null))
//						processorCollector.addElement(mapping.documentProcessorName);
//				}
//			}
//			else processorCollector.addElement(mapping.documentProcessorName);
//			
//			if (processorCollector.isEmpty()) return null;
//			
//			String cfName = (prefix + step.getFullName() + MarkupWizardCustomFunction.FILE_EXTENSION);
//			String cfLabel = ((label == null) ? step.getLabel() : label);
//			if (cfLabel.length() == 0)
//				cfLabel = step.getName();
//			String cfToolTip = step.getDescription();
//			
//			String[] dpNames = new String[processorCollector.size()];
//			String[] dpPcNames = new String[processorCollector.size()];
//			for (int p = 0; p < processorCollector.size(); p++) {
//				String dpFullName = processorCollector.get(p);
//				int split = dpFullName.indexOf('@');
//				if (split == -1) {
//					dpNames[p] = dpFullName;
//					dpPcNames[p] = "";
//				}
//				else {
//					dpNames[p] = dpFullName.substring(0, split);
//					dpPcNames[p] = dpFullName.substring(split + 1);
//				}
//			}
//			
//			return new MarkupWizardCustomFunction(cfName, cfLabel, cfToolTip, processorCollector.toStringArray(), dpNames, dpPcNames);
//		}
//		
//		static class MarkupWizardCustomFunction {
//
//			static final String FILE_EXTENSION = ".mwCustomFunction";
//			
//			final String name;
//			final String label;
//			final String toolTip;
//			final String[] dpFullNames;
//			final String[] dpNames;
//			final String[] dpProviderClassNames;
//			
//			MarkupWizardCustomFunction(String name, String label, String toolTip, String[] dpFullNames, String[] dpNames, String[] dpProviderClassNames) {
//				this.name = name;
//				this.label = label;
//				this.toolTip = toolTip;
//				this.dpFullNames = dpFullNames;
//				this.dpNames = dpNames;
//				this.dpProviderClassNames = dpProviderClassNames;
//			}
//		}
	}
}
