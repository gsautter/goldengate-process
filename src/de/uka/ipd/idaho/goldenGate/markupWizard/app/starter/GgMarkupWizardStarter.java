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
package de.uka.ipd.idaho.goldenGate.markupWizard.app.starter;


import java.awt.Toolkit;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;

import javax.swing.JOptionPane;
import javax.swing.UIManager;

import de.uka.ipd.idaho.goldenGate.GoldenGateConstants;
import de.uka.ipd.idaho.goldenGate.util.UpdateUtils;
import de.uka.ipd.idaho.goldenGate.util.UpdateUtils.UpdateStatusDialog;


/**
 * Starter class for GoldenGATE Markup Wizard to enable automated updates
 * 
 * @author sautter
 */
public class GgMarkupWizardStarter implements GoldenGateConstants {
//	private static final boolean DEBUG_BOOTSTRAP_START = true;
	
	private static boolean batchRun = false;
	
	private static boolean logSystemOut = false;
	private static boolean logError = false;
	
	private static final String LOG_TIMESTAMP_DATE_FORMAT = "yyyyMMdd-HHmm";
	private static final DateFormat LOG_TIMESTAMP_FORMATTER = new SimpleDateFormat(LOG_TIMESTAMP_DATE_FORMAT);
	
//	private static final DateFormat UPDATE_TIMESTAMP_FORMATTER = new SimpleDateFormat("yyyyMMdd-HHmm");
//	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		//	set platform L&F
		try {
			UIManager.setLookAndFeel(UIManager.getSystemLookAndFeelClassName());
		} catch (Exception e) {}
		
		//	read data path
		String dataBasePath = "./";
		
		//	get base path (if different from current directory)
		StringBuffer argAssembler = new StringBuffer();
		for (int a = 0; a < args.length; a++) {
			String arg = args[a];
			if (arg != null) {
				if (arg.startsWith(BASE_PATH_PARAMETER + "=")) dataBasePath = arg.substring((BASE_PATH_PARAMETER + "=").length());
				else if (RUN_PARAMETER.equals(arg)) batchRun = true;
				else {
					if ((arg.indexOf(' ') != -1) && !arg.startsWith("\""))
						arg = ("\"" + arg + "\"");
					argAssembler.append(" " + arg);
				}
			}
		}
		File basePath = new File(dataBasePath);
		
		//	load startup parameters
		String startMemory = DEFAULT_START_MEMORY;
		String maxMemory = DEFAULT_MAX_MEMORY;
		String proxyName = null;
		String proxyPort = null;
		String proxyUser = null;
		String proxyPwd = null;
		try {
			BufferedReader parameterReader = new BufferedReader(new FileReader(new File(basePath, PARAMETER_FILE_NAME)));
			String line;
			while ((line = parameterReader.readLine())  != null) {
				if (line.startsWith(START_MEMORY_NAME + "=")) startMemory = line.substring(START_MEMORY_NAME.length() + 1).trim();
				else if (line.startsWith(MAX_MEMORY_NAME + "=")) maxMemory = line.substring(MAX_MEMORY_NAME.length() + 1).trim();
				else if (line.startsWith(PROXY_NAME + "=")) proxyName = line.substring(PROXY_NAME.length() + 1).trim();
				else if (line.startsWith(PROXY_PORT + "=")) proxyPort = line.substring(PROXY_PORT.length() + 1).trim();
				else if (line.startsWith(PROXY_USER + "=")) proxyUser = line.substring(PROXY_USER.length() + 1).trim();
				else if (line.startsWith(PROXY_PWD + "=")) proxyPwd = line.substring(PROXY_PWD.length() + 1).trim();
				else if (line.startsWith(LOG_SYSTEM_OUT + "=")) logSystemOut = true;
				else if (line.startsWith(LOG_ERROR + "=")) logError = true;
			}
		}
		catch (FileNotFoundException fnfe) {
			System.out.println("GgMarkupWizardStarter: " + fnfe.getClass().getName() + " (" + fnfe.getMessage() + ") while reading GoldenGATE Markup Wizard startup parameters.");
		}
		catch (IOException ioe) {
			System.out.println("GgMarkupWizardStarter: " + ioe.getClass().getName() + " (" + ioe.getMessage() + ") while reading GoldenGATE Markup Wizard startup parameters.");
		}
		
		//	configure web access
		if (proxyName != null) {
			System.getProperties().put("proxySet", "true");
			System.getProperties().put("proxyHost", proxyName);
			if (proxyPort != null) System.getProperties().put("proxyPort", proxyPort);
			
			if ((proxyUser != null) && (proxyPwd != null)) {
				//	initialize proxy authentication
			}
		}
		
		//	open monitoring dialog
		UpdateStatusDialog sd = new UpdateStatusDialog(Toolkit.getDefaultToolkit().getImage(new File(new File(basePath, DATA_FOLDER_NAME), ICON_FILE_NAME).toString()));
		sd.popUp();
		
		//	ask if web access allowed
		boolean online = (JOptionPane.showConfirmDialog(sd, "Allow GoldenGATE Markup Wizard and its components to access the web?", "Allow Web Access?", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) == JOptionPane.YES_OPTION);
		
		//	download & install updates
		if (online) {
			sd.setTitle(STATUS_DIALOG_MAIN_TITLE + " - Downloading Updates");
			File ggJar = new File("./GoldenGATE.jar");
			String[] updateHosts = UpdateUtils.readUpdateHosts(basePath);
			UpdateUtils.downloadUpdates(basePath, ggJar.lastModified(), updateHosts, sd);
		}
		sd.setTitle(STATUS_DIALOG_MAIN_TITLE + " - Installing Updates");
		UpdateUtils.installUpdates(basePath, sd);
		
		//	clean up .log and .old files
		sd.setTitle(STATUS_DIALOG_MAIN_TITLE + " - Cleaning Up Old Files");
		cleanUpFiles(basePath, ".old");
		cleanUpFiles(basePath, ".log");
		
		//	start GoldenGATE Markup Wizard itself
		sd.setTitle(STATUS_DIALOG_MAIN_TITLE + " - Starting Main Program");
		String command = "java -jar -Xms" + startMemory + "m -Xmx" + maxMemory + "m GgMarkupWizard.jar " + RUN_PARAMETER + (online ? (" " + ONLINE_PARAMETER) : "") + argAssembler.toString();
		System.out.println("GgMarkupWizardStarter: command is '" + command + "'");
		final Process ggProcess = Runtime.getRuntime().exec(command, null, basePath);
		
		//	redirect output
		String logTimestamp = LOG_TIMESTAMP_FORMATTER.format(new Date());
		
		sd.setTitle(STATUS_DIALOG_MAIN_TITLE + " - Setting Up Logging");
		final BufferedReader ggSystemOutReader = (logSystemOut ? new BufferedReader(new InputStreamReader(ggProcess.getInputStream())) : null);
		final BufferedWriter ggSystemOutLogger = ((batchRun || !logSystemOut) ? null : new BufferedWriter(new FileWriter(new File(basePath, ("GgMwSystemOut." + logTimestamp + ".log")), true)));
		new Thread(new Runnable() {
			public void run() {
				try {
					while (logSystemOut) {
						String s = ggSystemOutReader.readLine();
						if (s != null) {
							if (batchRun) System.out.println(s);
							else {
								ggSystemOutLogger.write(s);
								ggSystemOutLogger.newLine();
							}
						}
					}
				} catch (IOException ioe) {}
			}
		}).start();
		
		final BufferedReader ggErrorReader = (logError ? new BufferedReader(new InputStreamReader(ggProcess.getErrorStream())) : null);
		final BufferedWriter ggErrorLogger = ((batchRun || !logError) ? null : new BufferedWriter(new FileWriter(new File(basePath, ("GgMwError." + logTimestamp + ".log")), true)));
		new Thread(new Runnable() {
			public void run() {
				try {
					while (logError) {
						String s = ggErrorReader.readLine();
						if (s != null) {
							if (batchRun) System.out.println(s);
							else {
								ggErrorLogger.write(s);
								ggErrorLogger.newLine();
							}
						}
					}
				} catch (IOException ioe) {}
			}
		}).start();
		
		//	close startup frame
		sd.dispose();
		
		//	close output when GoldenGATE Markup Wizard closed
		new Thread(new Runnable() {
			public void run() {
				try {
					int ggExit = ggProcess.waitFor();
					System.out.println("GoldenGATE Markup Wizard terminead: " + ggExit);
				} catch (Exception e) {}
				try {
					ggSystemOutReader.close();
					if (!batchRun && logSystemOut) {
						ggSystemOutLogger.flush();
						ggSystemOutLogger.close();
					}
				} catch (Exception e) {}
				try {
					ggErrorReader.close();
					if (!batchRun && logError) {
						ggErrorLogger.flush();
						ggErrorLogger.close();
					}
				} catch (Exception e) {}
				System.exit(0);
			}
		}).start();
	}
	
	private static final String STATUS_DIALOG_MAIN_TITLE = "GoldenGATE Markup Wizard Starting";
	
//	/**
//	 * status dialog for monitoring configuration downloads
//	 * 
//	 * @author sautter
//	 */
//	private static class UpdateStatusDialog extends JFrame implements Runnable {
//		JLabel hostLabel = new JLabel("", JLabel.LEFT);
//		
//		JLabel label = new JLabel("", JLabel.CENTER);
//		ArrayList labelLines = new ArrayList();
//		
//		Thread thread = null;
//		TitledBorder tb;
//		
//		UpdateStatusDialog(Image icon) {
//			super(STATUS_DIALOG_MAIN_TITLE);
//			this.setIconImage(icon);
//			this.setUndecorated(true);
//			
//			JPanel contentPanel = new JPanel(new BorderLayout());
//			contentPanel.add(this.hostLabel, BorderLayout.NORTH);
//			contentPanel.add(this.label, BorderLayout.CENTER);
//			
//			Font labelFont = UIManager.getFont("Label.font");
//			Border outer = BorderFactory.createMatteBorder(2, 3, 2, 3, Color.LIGHT_GRAY);
//			Border inner = BorderFactory.createEtchedBorder(Color.RED, Color.DARK_GRAY);
//			Border compound = BorderFactory.createCompoundBorder(outer, inner);
//			this.tb = BorderFactory.createTitledBorder(compound, STATUS_DIALOG_MAIN_TITLE, TitledBorder.LEFT, TitledBorder.TOP, labelFont.deriveFont(Font.BOLD));
//			contentPanel.setBorder(this.tb);
//			contentPanel.setBackground(Color.WHITE);
//			
//			this.getContentPane().setLayout(new BorderLayout());
//			this.getContentPane().add(contentPanel, BorderLayout.CENTER);
//			
//			this.setSize(400, 120);
//			this.setLocationRelativeTo(null);
//			this.setDefaultCloseOperation(DO_NOTHING_ON_CLOSE);
//		}
//		
//		/* (non-Javadoc)
//		 * @see java.awt.Frame#setTitle(java.lang.String)
//		 */
//		public void setTitle(String title) {
//			super.setTitle(title);
//			this.tb.setTitle(title);
//		}
//
//		void setHost(String host) {
//			this.hostLabel.setText(host);
//			this.hostLabel.validate();
//		}
//		
//		void setLabel(String text) {
//			this.labelLines.add(text);
//			while (this.labelLines.size() > 3)
//				this.labelLines.remove(0);
//			
//			StringBuffer labelText = new StringBuffer("<HTML>" + this.labelLines.get(0));
//			for (int l = 1; l < this.labelLines.size(); l++)
//				labelText.append("<BR>" + this.labelLines.get(l));
//			labelText.append("</HTML>");
//			
//			this.label.setText(labelText.toString());
//			this.label.validate();
//		}
//		
//		void popUp() {
//			if (this.thread == null) {
//				this.thread = new Thread(this);
//				this.thread.start();
//				while (!this.isVisible()) try {
//					Thread.sleep(50);
//				} catch (InterruptedException ie) {}
//			}
//		}
//		
//		public void run() {
//			this.setVisible(true);
//			this.thread = null;
//		}
//	}
//	
//	// matches href="/GgUpdate.-.zip"
//	private static final Pattern updateHrefPattern = Pattern.compile("(a href\\=\\\"((\\/[^\\/]++)*\\/GgUpdate\\.[0-9\\-]++\\.zip))\\\"", Pattern.CASE_INSENSITIVE);
//	
//	// matches GgUpdate.-.zip
//	private static final Pattern updateFileNamePattern = Pattern.compile("GgUpdate\\.[0-9\\-]++\\.zip", Pattern.CASE_INSENSITIVE);
//	
//	// matches GgUpdate.-.zip.done
//	private static final Pattern installedUpdateFileNamePattern = Pattern.compile("GgUpdate\\.[0-9\\-]++\\.zip\\.done", Pattern.CASE_INSENSITIVE);
//	
//	private static void downloadUpdates(File basePath, UpdateStatusDialog sd, String ggTimestamp) {
//		File updateFolder = new File(basePath, UPDATE_FOLDER_NAME);
//		if (!updateFolder.exists())
//			updateFolder.mkdir();
//		
//		//	create index of updates available locally or already installed
//		File[] updateFiles = updateFolder.listFiles(new FileFilter() {
//			public boolean accept(File f) {
//				return (installedUpdateFileNamePattern.matcher(f.getName()).matches() || updateFileNamePattern.matcher(f.getName()).matches());
//			}
//		});
//		
//		Set updateFileNames = new HashSet();
//		for (int u = 0; u < updateFiles.length; u++) {
//			String updateFileName = updateFiles[u].getName().toLowerCase();
//			if (updateFileName.endsWith(".done"))
//				updateFileName = updateFileName.substring(0, (updateFileName.length() - 5));
//			updateFileNames.add(updateFileName);
//		}
//		
//		File updateHostsFile = new File(basePath, UPDATE_HOST_FILE_NAME);
//		if (updateHostsFile.exists()) {
//			try {
//				BufferedReader br = new BufferedReader(new FileReader(updateHostsFile));
//				String updateHost;
//				while ((updateHost = br.readLine()) != null) try {
//					updateHost = updateHost.trim();
//					if ((updateHost.length() != 0) && !updateHost.startsWith("//")) {
//						if (DEBUG_BOOTSTRAP_START) System.out.println("Start downloading update links from '" + updateHost + "' ...");
//						sd.setHost(updateHost);
//						
//						//	download update index from update host
//						URL updateHostUrl = new URL(updateHost + (updateHost.endsWith("/") ? "" : "/"));
//						ArrayList updateUrls = new ArrayList();
//						BufferedReader updateIndexReader = new BufferedReader(new InputStreamReader(updateHostUrl.openStream()));
//						String updateIndexLine;
//						while ((updateIndexLine = updateIndexReader.readLine()) != null) {
//							Matcher updateLinkMatcher = updateHrefPattern.matcher(updateIndexLine);
//							if (updateLinkMatcher.find()) {
//								String updateLink = updateLinkMatcher.group(1);
//								if (DEBUG_BOOTSTRAP_START) System.out.println(" - got update link: '" + updateLink + "'");
//								updateUrls.add(updateLink);
//							}
//						}
//						
//						//	download updates
//						for (int u = 0; u < updateUrls.size(); u++) {
//							String updateName = updateUrls.get(u).toString();
//							updateName = updateName.substring(updateName.lastIndexOf("/") + 1);
//							
//							//	update already installed
//							if (updateFileNames.contains(updateName.toLowerCase()))
//								if (DEBUG_BOOTSTRAP_START) System.out.println(" - update '" + updateName + "' is available locally.");
//							
//							//	check if update is recent, download if so
//							else {
//								
//								//	extract update timestamp & compare to timestamp of GoldenGATE.jar
//								String updateTimestamp = updateName.substring(9, updateName.length() - 4);
//								if ((ggTimestamp == null) || (updateTimestamp.compareTo(ggTimestamp) > 0)) {
//									try {
//										if (DEBUG_BOOTSTRAP_START) System.out.println(" - downloading update '" + updateName + "' ...");
//										sd.setLabel("Downloading " + updateName);
//										
//										File destFile = new File(updateFolder, updateName);
//										if (DEBUG_BOOTSTRAP_START) System.out.println(" - destination file is '" + destFile.getAbsolutePath() + "'");
//										destFile.createNewFile();
//										
//										FileOutputStream dest = new FileOutputStream(destFile);
//										if (DEBUG_BOOTSTRAP_START) System.out.println("   - got destination file writer");
//										
//										BufferedInputStream bis = new BufferedInputStream(new URL(updateHost + (updateHost.endsWith("/") ? "" : "/") + updateName).openStream());
//										if (DEBUG_BOOTSTRAP_START) System.out.println("   - got reader");
//										
//										int count;
//										byte data[] = new byte[1024];
//										while ((count = bis.read(data, 0, 1024)) != -1)
//											dest.write(data, 0, count);
//										dest.flush();
//										dest.close();
//										bis.close();
//										if (DEBUG_BOOTSTRAP_START) System.out.println("   - download completed");
//									}
//									catch (IOException ioe) {
//										System.out.println("GgMarkupWizardStarter: " + ioe.getClass().getName() + " (" + ioe.getMessage() + ") while downloading update '" + updateName + "' from '" + updateHost + "'");
//									}
//								}
//								else if (DEBUG_BOOTSTRAP_START) System.out.println(" - update '" + updateName + "' is out of date.");
//							}
//						}
//					}
//				}
//				catch (IOException ioe) {
//					System.out.println("GgMarkupWizardStarter: " + ioe.getClass().getName() + " (" + ioe.getMessage() + ") while downloading update index from '" + updateHost + "'");
//				}
//			}
//			catch (FileNotFoundException fnfe) {
//				System.out.println("GgMarkupWizardStarter: " + fnfe.getClass().getName() + " (" + fnfe.getMessage() + ") while reading updates.");
//			}
//			catch (IOException ioe) {
//				System.out.println("GgMarkupWizardStarter: " + ioe.getClass().getName() + " (" + ioe.getMessage() + ") while reading updates.");
//			}
//			finally {
//				if (sd != null) sd.dispose();
//			}
//		}
//	}
//	
//	private static void installUpdates(File basePath, UpdateStatusDialog sd) {
//		File updatePath = new File(basePath, UPDATE_FOLDER_NAME);
//		if (!updatePath.exists()) updatePath.mkdir();
//		File[] updates = updatePath.listFiles();
//		for (int u = 0; u < updates.length; u++) {
//			if (updates[u].isFile() && updates[u].getName().endsWith(".zip")) try {
//				System.out.println("Installing update from '" + updates[u] + "' ...");
//				sd.setHost(updates[u].getName());
//				
//				ZipFile zip = new ZipFile(updates[u]);
//				Enumeration entries = zip.entries();
//				while (entries.hasMoreElements()) {
//					ZipEntry ze = ((ZipEntry) entries.nextElement());
//					String name = ze.getName();
//					System.out.println(" - unzipping '" + name + "' ...");
//					
//					//	unzip folder
//					if (ze.isDirectory()) {
//						System.out.println("   - it's a folder");
//						File destFile = new File(basePath, name);
//						System.out.println("   - destination folder is '" + destFile.getAbsolutePath() + "'");
//						System.out.println("   - checking destination folder:");
//						if (!destFile.exists()) {
//							System.out.println("   --> destination folder doesn't exist ...");
//							destFile.mkdirs();
//							System.out.println("     - destination folder created");
//						} else System.out.println("   --> destination folder exists");
//					}
//					
//					//	unzip file
//					else {
//						System.out.println("   - it's a file");
//						File destFile = new File(basePath, name);
//						System.out.println("   - destination file is '" + destFile.getAbsolutePath() + "'");
//						System.out.println("   - checking destination file:");
//						
//						if (!destFile.exists()) {
//							System.out.println("     --> destination file doesn't exist ...");
//							File destFolder = destFile.getParentFile();
//							if (!destFolder.exists()) {
//								destFolder.mkdirs();
//								System.out.println("     - destination folder created");
//							}
//							destFile.createNewFile();
//							System.out.println("       - destination file created");
//						}
//						else if ((ze.getTime() != -1) && (destFile.lastModified() < ze.getTime())) {
//							System.out.println("     --> destination file exists");
//							String destFileName = destFile.toString();
//							File oldDestFile = new File(destFileName + ".old");
//							if (oldDestFile.exists() && oldDestFile.delete())
//								oldDestFile = new File(destFileName + ".old");
//							destFile.renameTo(oldDestFile);
//							System.out.println("       - old destination file renamed");
//							
//							destFile = new File(destFileName);
//							destFile.createNewFile();
//							System.out.println("       - destination file created");
//						}
//						else {
//							System.out.println("     --> destination file exists");
//							System.out.println("       - destination file (" + destFile.lastModified() + ") is newer than update file (" + ze.getTime() + "), skipping update file");
//							destFile = null;
//						}
//						
//						if (destFile != null) {
//							sd.setLabel("Updating " + name);
//							
//							FileOutputStream dest = new FileOutputStream(destFile);
//							System.out.println("   - got destination file writer");
//							
//							BufferedInputStream bis = new BufferedInputStream(zip.getInputStream(ze));
//							System.out.println("   - got reader");
//							
//							int count;
//							byte data[] = new byte[1024];
//							while ((count = bis.read(data, 0, 1024)) != -1)
//								dest.write(data, 0, count);
//							dest.flush();
//							dest.close();
//							
//							if (ze.getTime() != -1) destFile.setLastModified(ze.getTime());
//						}
//					}
//					
//					System.out.println("   - '" + name + "' unzipped");
//				}
//				
//				zip.close();
//				
//				File oldUpdateFile = new File(updates[u].getAbsoluteFile() + ".done");
//				System.out.println(" - renaming update file to '" + oldUpdateFile + "'");
//				updates[u].renameTo(oldUpdateFile);
//				
//				System.out.println(" - update from '" + updates[u] + "' installed");
//			} catch (Exception e) {}
//		}
//	}
//	
	private static void cleanUpFiles(File folder, final String ending) {
		File[] files = folder.listFiles(new FileFilter() {
			public boolean accept(File pathname) {
				return ((pathname != null) && (pathname.isDirectory() || pathname.getName().endsWith(ending)));
			}
		});
		if (files != null)
			for (int f = 0; f < files.length; f++) {
				if (files[f].getName().endsWith(ending))
					files[f].delete();
				else cleanUpFiles(files[f], ending);
			}
	}
}
