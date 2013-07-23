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
package de.uka.ipd.idaho.goldenGate.markupWizard.manager;

import java.io.BufferedWriter;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.util.Iterator;

import de.uka.ipd.idaho.goldenGate.GoldenGateConstants;
import de.uka.ipd.idaho.goldenGate.configuration.AbstractConfigurationManager;
import de.uka.ipd.idaho.goldenGate.configuration.ConfigurationUtils.Configuration;
import de.uka.ipd.idaho.goldenGate.configuration.ConfigurationUtils.DataItem;
import de.uka.ipd.idaho.goldenGate.configuration.ConfigurationUtils.Plugin;
import de.uka.ipd.idaho.goldenGate.configuration.ConfigurationUtils.Resource;
import de.uka.ipd.idaho.goldenGate.configuration.ConfigurationUtils.SpecialDataHandler;
import de.uka.ipd.idaho.goldenGate.configuration.FileConfiguration;
import de.uka.ipd.idaho.goldenGate.markupWizard.MarkupWizard;
import de.uka.ipd.idaho.goldenGate.markupWizard.ProcessPartMapper.ProcessPartMapping;
import de.uka.ipd.idaho.goldenGate.plugins.GoldenGatePlugin;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * Configuration exporter using markup wizard definitions to conveniently export
 * suitable configurations for GoldenGATE Markup Wizard.
 * 
 * @author sautter
 */
public class MarkupWizardConfigurationManager extends AbstractConfigurationManager {
	
	private MarkupWizardManager mwManager;
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#init()
	 */
	public void init() {
		this.mwManager = ((MarkupWizardManager) this.parent.getPlugin(MarkupWizardManager.class.getName()));
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.configuration.AbstractConfigurationManager#isOperational()
	 */
	public boolean isOperational() {
		return (super.isOperational() && (this.mwManager != null));
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.configuration.AbstractConfigurationManager#getSelectablePluginClassNames()
	 */
	protected StringVector getSelectablePluginClassNames() {
		StringVector selectablePluginClassNames = new StringVector();
		GoldenGatePlugin[] plugins = this.parent.getPlugins();
		for (int p = 0; p < plugins.length; p++)
			selectablePluginClassNames.addElementIgnoreDuplicates(plugins[p].getClass().getName());
		selectablePluginClassNames.removeAll(MarkupWizardManager.class.getName());
		selectablePluginClassNames.removeAll(this.getClass().getName());
		return selectablePluginClassNames;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.configuration.AbstractConfigurationManager#getExportNames()
	 */
	protected String[] getExportNames() {
		return this.mwManager.getResourceNames();
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.configuration.AbstractConfigurationManager#adjustSelection(java.lang.String, de.uka.ipd.idaho.stringUtils.StringVector)
	 */
	protected void adjustSelection(String exportName, StringVector selected) {
		MarkupWizard mw = this.mwManager.getMarkupWizard(exportName);
		
		if (mw != null) {
			ProcessPartMapping[] mappings = mw.mapper.getProcessPartMappings();
			for (int m = 0; m < mappings.length; m++) {
				if (mappings[m].documentProcessorName != null)
					selected.addElementIgnoreDuplicates(mappings[m].documentProcessorName);
			}
		}
		
		for (int s = 0; s < selected.size(); s++) {
			if (selected.get(s).endsWith(MarkupWizardManager.class.getName()))
				selected.remove(s--);
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.configuration.AbstractConfigurationManager#adjustConfiguration(java.lang.String, de.uka.ipd.idaho.goldenGate.configuration.ConfigurationUtils.Configuration)
	 */
	protected void adjustConfiguration(String exportName, Configuration config) {
		
		config.addDataItem(new DataItem("process.xml", config.configTimestamp));
		config.addDataItem(new DataItem("mapping.txt", config.configTimestamp));
		
		Plugin mwManager = ((Plugin) config.pluginsByClassName.get(MarkupWizardManager.class.getName()));
		if (mwManager != null) {
			config.plugins.remove(mwManager);
			config.pluginsByName.remove(mwManager.name);
			config.pluginsByClassName.remove(mwManager.className);
			for (Iterator mwit = mwManager.resources.iterator(); mwit.hasNext();) {
				Resource mw = ((Resource) mwit.next());
				if (mw != null) {
					config.resources.remove(mw);
					config.resourcesByName.remove(mw.name);
				}
			}
		}
		
		Resource mw = ((Resource) config.resourcesByName.get(exportName));
		if (mw != null) {
			config.resources.remove(mw);
			config.resourcesByName.remove(mw.name);
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.configuration.AbstractConfigurationManager#getSpecialDataHandler(java.lang.String, de.uka.ipd.idaho.goldenGate.configuration.ConfigurationUtils.SpecialDataHandler)
	 */
	protected SpecialDataHandler getSpecialDataHandler(String exportName, final SpecialDataHandler sdh) {
		final MarkupWizard markupWizard = this.mwManager.getMarkupWizard(exportName);
		return new SpecialDataHandler() {
			public InputStream getInputStream(String dataName) throws IOException {
				if ("mapping.txt".equals(dataName)) {
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					BufferedWriter mappingWriter = new BufferedWriter(new OutputStreamWriter(baos, "UTF-8"));
					ProcessPartMapping[] mappings = markupWizard.mapper.getProcessPartMappings();
					for (int m = 0; m < mappings.length; m++) {
						mappingWriter.write(mappings[m].toString());
						mappingWriter.newLine();
					}
					mappingWriter.flush();
					mappingWriter.close();
					return new ByteArrayInputStream(baos.toByteArray());
				}
				else if ("process.xml".equals(dataName)) {
					ByteArrayOutputStream baos = new ByteArrayOutputStream();
					BufferedWriter processWriter = new BufferedWriter(new OutputStreamWriter(baos, "UTF-8"));
					markupWizard.process.writeXml(processWriter);
					processWriter.flush();
					processWriter.close();
					return new ByteArrayInputStream(baos.toByteArray());
				}
				else return sdh.getInputStream(dataName);
			}
		};
	}
	
	//	make super class method accessible in local package
	boolean exportMarkupWizardConfiguration(String mwName) {
		return this.doExport(mwName);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.configuration.AbstractConfigurationManager#doExport(java.lang.String, de.uka.ipd.idaho.goldenGate.configuration.ConfigurationUtils.SpecialDataHandler, de.uka.ipd.idaho.goldenGate.configuration.ConfigurationUtils.Configuration, java.io.File, de.uka.ipd.idaho.goldenGate.configuration.AbstractConfigurationManager.ExportStatusDialog)
	 */
	protected boolean doExport(String exportName, SpecialDataHandler specialData, Configuration config, File rootPath, ExportStatusDialog statusDialog) throws IOException {
		File ggRoot = this.getRootPath();
		if (ggRoot == null) return false;
		
		//	determine target folder
		File exportFolder = new File(new File(ggRoot, GoldenGateConstants.CONFIG_FOLDER_NAME), exportName);
		
		//	do export
		return FileConfiguration.createFileConfiguration(exportName, specialData, config, rootPath, exportFolder, statusDialog);
	}
}
