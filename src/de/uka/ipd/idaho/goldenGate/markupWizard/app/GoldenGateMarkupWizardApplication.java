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

import java.awt.Color;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.UIManager;

import de.uka.ipd.idaho.easyIO.EasyIO;
import de.uka.ipd.idaho.easyIO.settings.Settings;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.util.validation.Process;
import de.uka.ipd.idaho.goldenGate.GoldenGATE;
import de.uka.ipd.idaho.goldenGate.GoldenGateConfiguration;
import de.uka.ipd.idaho.goldenGate.GoldenGateConfiguration.ConfigurationDescriptor;
import de.uka.ipd.idaho.goldenGate.GoldenGateConstants;
import de.uka.ipd.idaho.goldenGate.GoldenGateConstants.StatusDialog.StatusDialogButton;
import de.uka.ipd.idaho.goldenGate.configuration.ConfigurationUtils;
import de.uka.ipd.idaho.goldenGate.configuration.FileConfiguration;
import de.uka.ipd.idaho.goldenGate.configuration.UrlConfiguration;
import de.uka.ipd.idaho.goldenGate.markupWizard.MarkupWizard;
import de.uka.ipd.idaho.goldenGate.markupWizard.ProcessPartMapper;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentLoader.DocumentData;
import de.uka.ipd.idaho.goldenGate.util.DialogPanel;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * Main class for starting GoldenGATE Markup Wizard as a standalone application
 * 
 * @author sautter
 */
public class GoldenGateMarkupWizardApplication implements GoldenGateConstants {
	
	private static File BASE_PATH = null;
	private static boolean ONLINE = false;
	
	private static Settings PARAMETERS = new Settings();
	private static StringVector CONFIG_HOSTS = new StringVector();
	
	private static final String LOG_TIMESTAMP_DATE_FORMAT = "yyyyMMdd-HHmm";
	private static final DateFormat LOG_TIMESTAMP_FORMATTER = new SimpleDateFormat(LOG_TIMESTAMP_DATE_FORMAT);
	
	/**	the main method to run GoldenGATE as a standalone application
	 * @param args the arguments, which have the following meaning:<ul>
	 * <li>args[0]: the RUN parameter (if not specified, the GoldenGATE.bat startup script will be created)</li>
	 * <li>args[1]: the ONLINE parameter (if not specified, GoldenGATE will run purely offline and will not allow its plugin components to access the network or WWW)</li>
	 * <li>args[2]: the root path of the GoldenGATE installation (if not specified, GoldenGATE will use the current path instead, i.e. './')</li>
	 * <li>args[3]: the file to open (this parameter is used to handle a file drag&dropped on the GoldenGATE.bat startup script)</li>
	 * </ul>
	 */
	public static void main(String[] args) {
		
		//	set platform L&F
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {}
		
		//	if no args, jar has been started directly
		if ((args.length == 0) || !RUN_PARAMETER.equals(args[0])) {
			File file = new File("./GgMarkupWizard.bat");
			
			//	create batch file if not exists
			if (!file.exists()) {
				try {
					EasyIO.writeFile(file, ("java -jar -Xms" + DEFAULT_START_MEMORY + "m -Xmx" + DEFAULT_MAX_MEMORY + "m GgMarkupWizard.jar " + RUN_PARAMETER + " %1"));
				} catch (IOException ioe) {}
			}
			
			//	batch file exists, show error
			else JOptionPane.showMessageDialog(null, "Please use GgMarkupWizardStarter.jar to start the GoldenGATE Markup Wizard.", "Please Use GoldenGateStarter.jar", JOptionPane.INFORMATION_MESSAGE);
			
			//	we're done here
			System.exit(0);
			return;
		}
		String basePath = "./";
		String fileToOpen = null;
		boolean log = false;
		String logFileName = ("GgMarkupWizard." + LOG_TIMESTAMP_FORMATTER.format(new Date()) + ".log");
		
		//	parse remaining args
		for (int a = 1; a < args.length; a++) {
			String arg = args[a];
			if (arg != null) {
				if (arg.startsWith(BASE_PATH_PARAMETER + "="))
					basePath = arg.substring((BASE_PATH_PARAMETER + "=").length());
				else if (ONLINE_PARAMETER.equals(arg))
					ONLINE = true;
				else if (arg.equals(LOG_PARAMETER + "=IDE") || arg.equals(LOG_PARAMETER + "=NO"))
					logFileName = null;
				else if (arg.startsWith(LOG_PARAMETER + "="))
					logFileName = arg.substring((LOG_PARAMETER + "=").length());
				else if (fileToOpen == null) fileToOpen = arg;
			}
		}
		
		//	remember program base path
		BASE_PATH = new File(basePath);
		
		//	keep user posted
		StatusDialog sd = new StatusDialog(Toolkit.getDefaultToolkit().getImage(new File(new File(BASE_PATH, DATA_FOLDER_NAME), ICON_FILE_NAME).toString()), "GoldenGATE Editor Initializing");
		sd.popUp();
		
		//	load parameters
		sd.setStatusLabel("Loading parameters");
		try {
			StringVector parameters = StringVector.loadList(new File(BASE_PATH, PARAMETER_FILE_NAME));
			for (int p = 0; p < parameters.size(); p++) try {
				String param = parameters.get(p);
				int split = param.indexOf('=');
				if (split != -1) {
					String key = param.substring(0, split).trim();
					String value = param.substring(split + 1).trim();
					if ((key.length() != 0) && (value.length() != 0))
						PARAMETERS.setSetting(key, value);
				}
			} catch (Exception e) {}
		} catch (Exception e) {}
		
		//	configure web access
		if (PARAMETERS.containsKey(PROXY_NAME)) {
			System.getProperties().put("proxySet", "true");
			System.getProperties().put("proxyHost", PARAMETERS.getSetting(PROXY_NAME));
			if (PARAMETERS.containsKey(PROXY_PORT))
				System.getProperties().put("proxyPort", PARAMETERS.getSetting(PROXY_PORT));
			
			if (PARAMETERS.containsKey(PROXY_USER) && PARAMETERS.containsKey(PROXY_PWD)) {
				//	initialize proxy authentication
			}
		}
		
		//	create log files if required
		File logFolder = null;
		File logFileOut = null;
		File logFileErr = null;
		if (logFileName != null) try {
			
			//	truncate log file extension
			if (logFileName.endsWith(".log"))
				logFileName = logFileName.substring(0, (logFileName.length() - ".log".length()));
			
			//	create absolute log files
			if (logFileName.startsWith("/") || (logFileName.indexOf(':') != -1)) {
				logFileOut = new File(logFileName + ".out.log");
				logFileErr = new File(logFileName + ".err.log");
				logFolder = logFileOut.getAbsoluteFile().getParentFile();
			}
			
			//	create relative log files (the usual case)
			else {
				
				//	get log path
				String logFolderName = PARAMETERS.getSetting(LOG_PATH, LOG_FOLDER_NAME);
				if (logFolderName.startsWith("/") || (logFolderName.indexOf(':') != -1))
					logFolder = new File(logFolderName);
				else logFolder = new File(BASE_PATH, logFolderName);
				logFolder = logFolder.getAbsoluteFile();
				logFolder.mkdirs();
				
				//	create log files
				logFileOut = new File(logFolder, (logFileName + ".out.log"));
				logFileErr = new File(logFolder, (logFileName + ".err.log"));
			}
			
			//	redirect System.out
			logFileOut.getParentFile().mkdirs();
			logFileOut.createNewFile();
			System.setOut(new PrintStream(new BufferedOutputStream(new FileOutputStream(logFileOut)), true, "UTF-8"));
			
			//	redirect System.err
			logFileErr.getParentFile().mkdirs();
			logFileErr.createNewFile();
			System.setErr(new PrintStream(new BufferedOutputStream(new FileOutputStream(logFileErr)), true, "UTF-8"));
		}
		catch (Exception e) {
			JOptionPane.showMessageDialog(null, "Could not create log files in folder '" + logFolder.getAbsolutePath() + "'." +
					"\nCommon reasons are a full hard drive, lack of write permissions to the folder, or system protection software." +
					"\nUse the 'Configure' button in the configuration selector dialog to select a different log location." +
					"\nThen exit and re-start GoldenGATE Editor to apply the change." +
					"\n\nNote that you can work normally without the log files, it's just that in case of an error, there are" +
					"\nno log files to to help investigate what exactly went wrong and help developers fix the problem.", "Error Creating Log Files", JOptionPane.ERROR_MESSAGE);
		}
		
		//	load configuration hosts
		sd.setStatusLabel("Loading config hosts");
		try {
			StringVector configHosts = StringVector.loadList(new File(BASE_PATH, CONFIG_HOST_FILE_NAME));
			CONFIG_HOSTS.addContentIgnoreDuplicates(configHosts);
		} catch (Exception e) {}
		
		//	read wizard configuration 
		sd.setStatusLabel("Loading markup wizard data");
		Settings mwSet = Settings.loadSettings(new File(BASE_PATH, "GgMarkupWizard.cnfg"));
		
		//	get wizard name
		String mwName = mwSet.getSetting("WizardName");
		if (mwName == null) {
			JOptionPane.showMessageDialog(DialogPanel.getTopWindow(), "Unable to run GoldenGATE Markup Wizard without name.", "Wizard Name Missing", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		
		//	get available configurations
		sd.setStepLabel("Listing Configurations");
		ConfigurationDescriptor[] configurations = getConfigurations(BASE_PATH, sd);
		
		//	find required configuration
		ConfigurationDescriptor configuration = ConfigurationUtils.getConfiguration(configurations, mwName, BASE_PATH, true);
		
		//	check if configuration found
		if (configuration == null) {
			JOptionPane.showMessageDialog(DialogPanel.getTopWindow(), ("The configuration '" + mwName + "' is not available."), "Configuration Missing", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		//	get mappings, process, and configuration
		sd.setStepLabel("Initializing Markup Wizard");
		sd.setStatusLabel("Creating basic components");
		GoldenGateConfiguration ggConfiguration;
		ProcessPartMapper mwMapper;
		Process mwProcess;
		try {
			
			//	local configuration selected
			if (configuration.host == null) {
				File configRoot = new File(new File(BASE_PATH, CONFIG_FOLDER_NAME), configuration.name);
//				ggConfiguration = new FileConfiguration(configuration.name, configRoot, false, ONLINE, (log ? new File(BASE_PATH, ("GgMarkupWizard." + System.currentTimeMillis() + ".log")) : null));
				ggConfiguration = new FileConfiguration(configuration.name, configRoot, false, ONLINE, null);
			}
			
			//	remote configuration selected
			else {
				String configRoot = (configuration.host + (configuration.host.endsWith("/") ? "" : "/") + configuration.name);
				ggConfiguration = new UrlConfiguration(configRoot, configuration.name);
			}
			
			//	load mappings
			InputStream mis = ggConfiguration.getInputStream("mapping.txt");
			StringVector mappings = StringVector.loadList(mis);
			mwMapper = new ProcessPartMapper(mappings.toStringArray());
			mis.close();
			
			//	load process definition
			InputStream pis = ggConfiguration.getInputStream("process.xml");
			mwProcess = Process.loadMarkupProcess(pis);
			pis.close();
			
			//	wrap configuration
			ggConfiguration = new MarkupWizardConfiguration(ggConfiguration, mwProcess, mwMapper);
		}
		catch (IOException ioe) {
			System.out.println("Exception loading GoldenGATE Markup Wizard '" + configuration.name + "':\n   " + ioe.getClass().getName() + " (" + ioe.getMessage() + ")");
			ioe.printStackTrace(System.out);
			JOptionPane.showMessageDialog(DialogPanel.getTopWindow(), ("Exception loading GoldenGATE Markup Wizard:" + ioe.getMessage()), "Exception Loading GoldenGATE Markup Wizard", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		
		//	make sure icons are OK
		JFrame iconFrame = new JFrame();
		iconFrame.setIconImage(ggConfiguration.getIconImage());
		try {
			
			//	load GoldenGATE core
			sd.setStatusLabel("Loading GoldenGATE instance");
			final GoldenGATE goldenGate = GoldenGATE.openGoldenGATE(ggConfiguration, false, true);
			
			//	create wizard
			sd.setStatusLabel("Initializing markup wizard");
			final GoldenGateMarkupWizard ggmw = new GoldenGateMarkupWizard(ggConfiguration, goldenGate, new MarkupWizard(mwName, mwProcess, mwMapper));
			
			//	open file dropped on bat/sh
			if (fileToOpen != null) try {
				String fileName = fileToOpen;
				String dataType;
				if ((fileName.indexOf('.') == -1) || fileName.endsWith(".")) dataType = "xml";
				else dataType = fileName.substring(fileName.lastIndexOf('.') + 1);
				
				DocumentFormat format = goldenGate.getDocumentFormatForFileExtension(dataType);
				if (format == null) {
					String formatName = goldenGate.selectLoadFormat();
					if (formatName != null)
						format = goldenGate.getDocumentFormatForName(formatName);
				}
				
				if (format == null) JOptionPane.showMessageDialog(iconFrame, ("GoldenGATE Markup Wizzard cannot open the dropped file, sorry,\nthe data format in '" + fileToOpen + "' is unknown."), "Unknown Document Format", JOptionPane.INFORMATION_MESSAGE);
				else {
					InputStream source = new FileInputStream(new File(fileToOpen));
					MutableAnnotation doc = format.loadDocument(source);
					source.close();
					if (doc != null)
						ggmw.openDocument(new DocumentData(doc, fileName, format));
				}
			}
			catch (IOException ioe) {
				System.out.println("Error opening document '" + fileToOpen + "':\n   " + ioe.getClass().getName() + " (" + ioe.getMessage() + ")");
				ioe.printStackTrace(System.out);
			}
			
			//	listen for exit
			ggmw.addWindowListener(new WindowAdapter() {
				boolean closed = false;
				public void windowClosed(WindowEvent we) {
					if (this.closed) return;
					this.closed = true;
					ggmw.closeDocument();
					goldenGate.exitShutdown();
					System.exit(0);
				}
			});
			
			//	dispose startup splash screen
			sd.setVisible(false);
			
			//	open wizard
			ggmw.setVisible(true);
		}
		catch (Exception e) {
			System.out.println("Exception starting GoldenGATE Markup Wizard from configuration '" + ggConfiguration.getName() + "':\n   " + e.getClass().getName() + " (" + e.getMessage() + ")");
			e.printStackTrace(System.out);
			JOptionPane.showMessageDialog(iconFrame, ("Error starting GoldenGATE Markup Wizard from configuration '" + ggConfiguration.getName() + "':\n   " + e.getClass().getName() + " (" + e.getMessage() + ")"), "Error Starting GoldenGATE Markup Wizard", JOptionPane.ERROR_MESSAGE);
			System.exit(0);
		}
	}
	
	private static final String MASTER_CONFIG_NAME = "Local Master Configuration";
	
	private static ConfigurationDescriptor[] getConfigurations(File dataBasePath, StatusDialog sd) {
		
		//	collect configurations
		final ArrayList configList = new ArrayList();
		
		//	add local default configuration
		ConfigurationDescriptor defaultConfig = new ConfigurationDescriptor(null, MASTER_CONFIG_NAME, System.currentTimeMillis());
		configList.add(defaultConfig);
		
		//	load local non-default configurations
		ConfigurationDescriptor[] configs = ConfigurationUtils.getLocalConfigurations(dataBasePath);
		for (int c = 0; c < configs.length; c++)
			configList.add(configs[c]);
		
		//	get downloaded zip files
		configs = ConfigurationUtils.getZipConfigurations(dataBasePath);
		for (int c = 0; c < configs.length; c++)
			configList.add(configs[c]);
		
		//	no permission to download configurations, we're done here
		if (!ONLINE)
			return ((ConfigurationDescriptor[]) configList.toArray(new ConfigurationDescriptor[configList.size()]));
		
		//	get available configurations from configuration hosts
		for (int h = 0; h < CONFIG_HOSTS.size(); h++) {
			final String configHost = CONFIG_HOSTS.get(h).trim();
			if ((configHost.length() == 0) || configHost.startsWith("//"))
				continue;
			
			//	show activity
			sd.setStatusLabel("- from " + configHost);
			
			//	create control objects
			final Object configFetcherLock = new Object();
			final boolean[] addHostConfigs = {true};
			
			//	build buttons
			JButton skipButton = new StatusDialogButton("Skip", 5, 2);
			skipButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					synchronized (configFetcherLock) {
						addHostConfigs[0] = false;
						configFetcherLock.notify();
					}
				}
			});
			
			//	line up button panel
			JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
			buttonPanel.setBackground(Color.WHITE);
			buttonPanel.add(skipButton);
			
			//	show buttons
			sd.setCustomComponent(buttonPanel);
			
			//	load configs in extra thread
			Thread configFetcher = new Thread() {
				public void run() {
					ConfigurationDescriptor[] hostConfigs = ConfigurationUtils.getRemoteConfigurations(configHost);
					synchronized (configFetcherLock) {
						if (!addHostConfigs[0])
							return;
						for (int c = 0; c < hostConfigs.length; c++)
							configList.add(hostConfigs[c]);
						configFetcherLock.notify();
					}
					System.out.println("Config fetcher terminared for " + configHost);
				}
			};
			
			//	start download and block main thread
			synchronized (configFetcherLock) {
				configFetcher.start();
				System.out.println("Config fetcher started for " + configHost);
				try {
					configFetcherLock.wait();
				}
				catch (InterruptedException ie) {
					System.out.println("Interrupted waiting for config fetcher from " + configHost);
					addHostConfigs[0] = false;
				}
			}
		}
		
		//	finally ...
		return ((ConfigurationDescriptor[]) configList.toArray(new ConfigurationDescriptor[configList.size()]));
	}
}
