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
package de.uka.ipd.idaho.goldenGate.markupWizard.app.utilities;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.zip.ZipOutputStream;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

import de.uka.ipd.idaho.goldenGate.utilities.PackerUtils;

/**
 * @author sautter
 *
 */
public class MarkupWizardPacker {
	public static void main(String[] args) {
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {}
		buildVersions();
	}
	
	private static final String[] specialFiles = {
		"Parameters.cnfg",
		"UpdateHosts.cnfg",
		"ConfigHosts.cnfg",
		"GgMarkupWizard.cnfg",
		PackerUtils.README_FILE_NAME
	};
	
	private static void buildVersions() {
		File rootFolder = new File(PackerUtils.normalizePath(new File(".").getAbsolutePath()));
		System.out.println("Root folder is '" + rootFolder.getAbsolutePath() + "'");
		
		String[] configNames = getConfigurationNames(rootFolder);
		
		for (int c = 0; c < configNames.length; c++) try {
			buildVersion(rootFolder, configNames[c]);
		}
		catch (Exception e) {
			System.out.println("An error occurred creating the markup wizard zip file:\n" + e.getMessage());
			e.printStackTrace(System.out);
			JOptionPane.showMessageDialog(null, ("An error occurred creating the markup wizard zip file:\n" + e.getMessage()), "Markup Wizard Creation Error", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private static void buildVersion(File rootFolder, final String configName) throws Exception {
		System.out.println("Building GoldenGATE Markup Wizard with " + ((configName == null) ? "no configuration" : ("configuration '" + configName + "'")) + ".");
		
		String markupWizardZipName = "GgMarkupWizard" + ((configName == null) ? "" : ("-" + configName)) + ".zip";
		System.out.println("Building GoldenGATE Markup Wizard '" + markupWizardZipName + "'");
		
		String[] coreFileNames = getCoreFileNames(rootFolder);
		String[] configFileNames;
		if (configName == null)
			configFileNames = new String[0];
		else {
			configFileNames = PackerUtils.getConfigFileNames(rootFolder, configName);
			for (int f = 0; f < configFileNames.length; f++)
				configFileNames[f] = ("Configurations/" + configName + "/" + configFileNames[f]);
		}
		
		
		File markupWizardZipFile = new File(rootFolder, ("_Zips/" + markupWizardZipName));
		if (markupWizardZipFile.exists()) {
			markupWizardZipFile.renameTo(new File(rootFolder, ("_Zips/" + markupWizardZipName + "." + System.currentTimeMillis() + ".old")));
			markupWizardZipFile = new File(rootFolder, ("_Zips/" + markupWizardZipName));
		}
		System.out.println("Creating markup wizard zip file '" + markupWizardZipFile.getAbsolutePath() + "'");
		
		markupWizardZipFile.getParentFile().mkdirs();
		markupWizardZipFile.createNewFile();
		ZipOutputStream markupWizardZipper = new ZipOutputStream(new FileOutputStream(markupWizardZipFile));
		
		for (int s = 0; s < specialFiles.length; s++) {
			
			//	if config name specified, generate respective cnfg file
			if ("GgMarkupWizard.cnfg".equals(specialFiles[s]) && (configName != null)) {
				PackerUtils.writeZipFileEntry(new InputStream() {
					String data = "WizardName = \"" + configName + "\";";
					int dataPos = 0;
					public int read() throws IOException {
						if (this.dataPos < this.data.length())
							return this.data.charAt(this.dataPos++);
						else return -1;
					}
				}, specialFiles[s], markupWizardZipper);
			}
			
			//	copy forward readme file from configuration
			else if (PackerUtils.README_FILE_NAME.equals(specialFiles[s])) {
				File specialFile = new File(rootFolder, ("Configurations/" + configName + "/" + PackerUtils.README_FILE_NAME));
				PackerUtils.writeZipFileEntry(specialFile, PackerUtils.README_FILE_NAME, markupWizardZipper);
			}
			
			//	copy other files, and use default config name if none specified
			else {
				File specialFile = new File(rootFolder, ("_VersionPacker.markupWizard." + specialFiles[s]));
				PackerUtils.writeZipFileEntry(specialFile, specialFiles[s], markupWizardZipper);
			}
		}
		
		PackerUtils.writeZipFileEntries(rootFolder, markupWizardZipper, coreFileNames);
		PackerUtils.writeZipFileEntries(rootFolder, markupWizardZipper, configFileNames);
		
		markupWizardZipper.flush();
		markupWizardZipper.close();
		
		System.out.println("Markup wizard zip file '" + markupWizardZipFile.getAbsolutePath() + "' created successfully.");
		JOptionPane.showMessageDialog(null, ("Markup wizard '" + markupWizardZipName + "' created successfully."), "Markup Wizard Created Successfully", JOptionPane.INFORMATION_MESSAGE);
	}
	
	private static final String[] getConfigurationNames(File rootFolder) {
		Set configuredConfigNames = new TreeSet();
		try {
			String configuredConfigName = getConfigurationName(rootFolder);
			if (configuredConfigName != null)
				configuredConfigNames.add(configuredConfigName);
		} catch (IOException e) {}
		String[] configNames = PackerUtils.getConfigurationNames(rootFolder);
		
		List configNameList = new LinkedList();
		for (int c = 0; c < configNames.length; c++) {
			File configFolder = PackerUtils.getConfigFolder(rootFolder, configNames[c]);
			if (new File(configFolder, "process.xml").exists() && new File(configFolder, "mapping.txt").exists())
				configNameList.add(configNames[c]);
		}
		
		configNames = ((String[]) configNameList.toArray(new String[configNameList.size()]));
		return PackerUtils.selectStrings(configNames, configuredConfigNames, configuredConfigNames, "Please select the configuration(s) to export into markup wizard zip files.", null);
	}
	
	private static final String getConfigurationName(File rootFolder) throws IOException {
		File cnfgFile = new File(rootFolder, "GgMarkupWizard.cnfg");
		BufferedReader br = new BufferedReader(new FileReader(cnfgFile));
		String configName = null;
		for (String cnfgLine; (cnfgLine = br.readLine()) != null;) {
			cnfgLine = cnfgLine.trim();
			if (cnfgLine.startsWith("WizardName"))
				configName = cnfgLine.substring(cnfgLine.indexOf('"') + 1, cnfgLine.lastIndexOf('"'));
		}
		br.close();
		return configName;
	}
	
	private static String[] getCoreFileNames(File rootFolder) throws IOException {
		Set coreFiles = new TreeSet();
		
		File coreFileList = new File(rootFolder, "_VersionPacker.markupWizard.cnfg");
		BufferedReader br = new BufferedReader(new FileReader(coreFileList));
		for (String coreFile; (coreFile = br.readLine()) != null;) {
			coreFile = coreFile.trim();
			if ((coreFile.length() != 0) && !coreFile.startsWith("//"))
				coreFiles.add(coreFile);
		}
		br.close();
		
		for (int s = 0; s < specialFiles.length; s++)
			coreFiles.remove(specialFiles[s]);
		
		return ((String[]) coreFiles.toArray(new String[coreFiles.size()]));
	}
}