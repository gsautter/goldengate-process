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
package de.uka.ipd.idaho.goldenGate.markupWizard.dmc;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileFilter;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Properties;
import java.util.Map.Entry;

import de.uka.ipd.idaho.easyIO.EasyIO;
import de.uka.ipd.idaho.easyIO.IoProvider;
import de.uka.ipd.idaho.easyIO.SqlQueryResult;
import de.uka.ipd.idaho.easyIO.sql.TableDefinition;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.util.validation.Level;
import de.uka.ipd.idaho.gamta.util.validation.Process;
import de.uka.ipd.idaho.gamta.util.validation.Step;
import de.uka.ipd.idaho.gamta.util.validation.Task;
import de.uka.ipd.idaho.gamta.util.validation.ValidationResult;
import de.uka.ipd.idaho.goldenGateServer.AbstractGoldenGateServerComponent;
import de.uka.ipd.idaho.goldenGateServer.GoldenGateServerComponentRegistry;
import de.uka.ipd.idaho.goldenGateServer.dio.GoldenGateDIO;
import de.uka.ipd.idaho.goldenGateServer.dio.GoldenGateDioConstants.DioDocumentEvent.DioDocumentEventListener;
import de.uka.ipd.idaho.goldenGateServer.dio.data.DocumentList;
import de.uka.ipd.idaho.goldenGateServer.dio.data.DocumentListElement;
import de.uka.ipd.idaho.goldenGateServer.dio.util.AsynchronousDioAction;
import de.uka.ipd.idaho.goldenGateServer.uaa.UserAccessAuthority;
import de.uka.ipd.idaho.goldenGateServer.uaa.UserPermissionAuthority;
import de.uka.ipd.idaho.goldenGateServer.uaa.UserPermissionAuthorityRBAC;
import de.uka.ipd.idaho.htmlXmlUtil.Parser;
import de.uka.ipd.idaho.htmlXmlUtil.TreeNode;
import de.uka.ipd.idaho.stringUtils.StringIndex;
import de.uka.ipd.idaho.stringUtils.csvHandler.StringTupel;

/**
 * GoldenGATE Document Markup Coordinator (DMC) adds markup progress measurement
 * and, based on this, coordination to GoldenGATE DIO. In particular, it extends
 * the document liste returned by DIO with a field indicating how far the markup
 * of each document has advanced. Furthermore, DMC filters DIO's document list
 * so users see only documents whose next markup step they may work on.
 * 
 * @author sautter
 */
public class GoldenGateDMC extends AbstractGoldenGateServerComponent implements GoldenGateDmcConstants {
	
	private UserAccessAuthority uaa = null;
	private UserPermissionAuthorityRBAC upar = null;
	private GoldenGateDIO dio = null;
	private AsynchronousDioAction updateAction;
	
	private static final String MARKUP_USER_ROLE_NAME = "DIO.MarkupUser";
	private static final String ALL_STEPS_PERMISSION_NAME = "DIO.Step.All";
	
	private static final String DOCUMENT_STATUS_TABLE_NAME = "GgDmcDocumentStates";
	
	private static final String NEXT_STEP_COLUMN_NAME = "NextStep";
	private static final int NEXT_STEP_COLUMN_LENGTH = 255;
	private static final String TODO_NEXT_ATTRIBUTE = "TodoNext";
	
	private static final String[] dmcListFieldNames = {TODO_NEXT_ATTRIBUTE};
	
	private IoProvider io;
	private Parser parser = new Parser();
	
	private File documentValidationStatusFolder;
	
	private Process process;
	
	private boolean filterDocumentList = false;
	
	/** Constructor passing 'DMC' as the letter code to super constructor
	 */
	public GoldenGateDMC() {
		super("DMC");
	}
	
	/* (non-Javadoc)
	 * @see de.goldenGateScf.AbstractServerComponent#initComponent()
	 */
	protected void initComponent() {
		
		//	get and check database connection
		this.io = this.host.getIoProvider();
		if (!this.io.isJdbcAvailable())
			throw new RuntimeException("GoldenGateDMC: Cannot work without database access.");
		
		//	produce document status table
		TableDefinition td = new TableDefinition(DOCUMENT_STATUS_TABLE_NAME);
		td.addColumn(DOCUMENT_ID_ATTRIBUTE, TableDefinition.VARCHAR_DATATYPE, 32);
		td.addColumn(NEXT_STEP_COLUMN_NAME, TableDefinition.VARCHAR_DATATYPE, NEXT_STEP_COLUMN_LENGTH);
		if (!this.io.ensureTable(td, true))
			throw new RuntimeException("GoldenGateDMC: Cannot work without database access.");
		
		//	index document status table
		this.io.indexColumn(DOCUMENT_STATUS_TABLE_NAME, DOCUMENT_ID_ATTRIBUTE);
		
		//	check whether or not to filter document lists
		this.filterDocumentList = "yes".equalsIgnoreCase(this.configuration.getSetting("filterDocumentList"));
		
		
		//	create meta data folder
		this.documentValidationStatusFolder = new File(this.dataPath, "DocumentMetaData");
		if (!this.documentValidationStatusFolder.exists())
			this.documentValidationStatusFolder.mkdir();
		
		
		//	load markup process
		File processFile = new File(this.dataPath, "MarkupProcess.xml");
		InputStream is = null;
		try {
			is = new FileInputStream(processFile);
			TreeNode pRoot = this.parser.parse(is);
			if (TreeNode.ROOT_NODE_TYPE.equals(pRoot.getNodeType()))
				pRoot = pRoot.getChildNode(Process.PROCESS, 0);
			this.process = new Process(pRoot);
		}
		catch (IOException ioe) {
			System.out.println("GoldenGateDMC: Could not load markup process - " + ioe.getMessage());
			ioe.printStackTrace(System.out);
		}
		finally {
			if (is != null) try {
				is.close();
			} catch (IOException ioe) {}
		}
		
		
		//	cache document states
		SqlQueryResult sqr = null;
		String cacheQuery = "SELECT " + DOCUMENT_ID_ATTRIBUTE + ", " + NEXT_STEP_COLUMN_NAME +
				" FROM " + DOCUMENT_STATUS_TABLE_NAME + 
				";";
		try {
			sqr = this.io.executeSelectQuery(cacheQuery);
			while (sqr.next()) {
				String docId = sqr.getString(0);
				String nextStepName = sqr.getString(1);
				this.cacheNextStepName(docId, nextStepName);
			}
		}
		catch (SQLException sqle) {
			System.out.println("GoldenGateDMC: " + sqle.getClass().getName() + " (" + sqle.getMessage() + ") while caching document states.");
			System.out.println("  query was " + cacheQuery);
		}
		finally {
			if (sqr != null)
				sqr.close();
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGateServer.AbstractGoldenGateServerComponent#exitComponent()
	 */
	protected void exitComponent() {
		
		//	disconnect from database
		this.io.close();
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGateServer.AbstractGoldenGateServerComponent#link()
	 */
	public void link() {
		
		//	get access authority
		this.uaa = ((UserAccessAuthority) GoldenGateServerComponentRegistry.getServerComponent(UserAccessAuthority.class.getName()));
		
		//	check success
		if (this.uaa == null) throw new RuntimeException(UserAccessAuthority.class.getName());
		
		//	get document IO server
		this.dio = ((GoldenGateDIO) GoldenGateServerComponentRegistry.getServerComponent(GoldenGateDIO.class.getName()));
		
		//	check success
		if (this.dio == null) throw new RuntimeException(GoldenGateDIO.class.getName());
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGateServer.GoldenGateServerComponent#linkInit()
	 */
	public void linkInit() {
		
		//	listen for new documents directly uploaded to DIO
		this.dio.addDocumentEventListener(new DioDocumentEventListener() {
			
			/* (non-Javadoc)
			 * @see de.uka.ipd.idaho.goldenGateServer.dio.DioDocumentStorageListener#documentCheckedOut(de.uka.ipd.idaho.goldenGateServer.dst.DocumentStorageEvent)
			 */
			public void documentCheckedOut(DioDocumentEvent dse) {}
			
			/* (non-Javadoc)
			 * @see de.goldenGateScf.dss.notification.DocumentStorageListener#documentDeleted(de.goldenGateScf.dss.notification.DocumentStorageEvent)
			 */
			public void documentDeleted(DioDocumentEvent dse) {
				deleteDocument(dse.documentId);
			}
			
			/* (non-Javadoc)
			 * @see de.goldenGateScf.dss.notification.DocumentStorageListener#documentUpdated(de.goldenGateScf.dss.notification.DocumentStorageEvent)
			 */
			public void documentUpdated(DioDocumentEvent dse) {
				
				//	process missing, report error
				if (process == null) {
					dse.writeLog("===== DMC Document Validation Result =====");
					dse.writeLog("Could not validate document, no process given.");
				}
				
				//	got process, validate document
				else {
					
					//	do validation
					ValidationResult vr = process.getValidator().validate(dse.document);
					
					//	compute next step
					ValidationResult[] levels = vr.getPartialResults();
					String nextStepName = null;
					for (int l = 0; l < levels.length; l++) {
						ValidationResult[] tasks = levels[l].getPartialResults();
						for (int t = 0; t < tasks.length; t++) {
							ValidationResult[] steps = tasks[t].getPartialResults();
							for (int s = 0; s < steps.length; s++)
								if (!steps[s].isPassed()) {
									nextStepName = steps[s].getValidator().getFullName();
									s = steps.length;
									t = tasks.length;
									l = levels.length;
								}
						}
					}
					
					//	store next step
					storeDocumentStatus(dse.documentId, nextStepName);
					
					//	write detailed validation result
					dse.writeLog("===== DMC Document Validation Result =====");
					String[] vrDetails = vr.getDetailDescriptions();
					for (int d = 0; d < vrDetails.length; d++)
						dse.writeLog(vrDetails[d]);
					
					//	store validation result
					storeValidationStatus(dse.documentId, vr);
				}
			}
			
			/* (non-Javadoc)
			 * @see de.uka.ipd.idaho.goldenGateServer.dio.DioDocumentStorageListener#documentReleased(de.uka.ipd.idaho.goldenGateServer.dst.DocumentStorageEvent)
			 */
			public void documentReleased(DioDocumentEvent dse) {}
		});
		
		this.dio.addDocumentIoExtension(new DocumentIoExtension(50) {
			public int getSelectivity(int selectivity, Properties filter, String user) {
				return (filter.containsKey(NEXT_STEP_COLUMN_NAME) ? getNextStepSelectivity(filter.getProperty(NEXT_STEP_COLUMN_NAME)) : selectivity);
			}
			public DocumentList extendList(final DocumentList dl, final String user, boolean headOnly, Properties filter) {
				if (process == null)
					return dl;
				
				if (headOnly) {
					return new DocumentList(dl, dmcListFieldNames) {
						public boolean hasNextDocument() {
							return dl.hasNextDocument();
						}
						public DocumentListElement getNextDocument() {
							return dl.getNextDocument();
						}
						public DocumentAttributeSummary getListFieldValues(String listFieldName) {
							if (TODO_NEXT_ATTRIBUTE.equals(listFieldName)) {
								DocumentAttributeSummary das = new DocumentAttributeSummary();
								for (Iterator idit = stepTodos.values().iterator(); idit.hasNext();)
									das.add((String) idit.next());
								return das;
							}
							else return dl.getListFieldValues(listFieldName);
						}
					};
				}
				
				final boolean allStepsPermission = (!filterDocumentList || uaa.hasPermission(user, ALL_STEPS_PERMISSION_NAME, true));
				final HashSet nextStepFilter;
				if ((filter != null) && filter.containsKey(TODO_NEXT_ATTRIBUTE)) {
					String nextStepString = filter.getProperty(TODO_NEXT_ATTRIBUTE, "").trim();
					if (nextStepString.length() == 0)
						nextStepFilter = null;
					else nextStepFilter = new HashSet(Arrays.asList(nextStepString.split("[\\n\\r\\s]++")));
				}
				else nextStepFilter = null;
				
				return new DocumentList(dl, dmcListFieldNames) {
					private HashSet knownPermissions = new HashSet(); 
					private DocumentListElement next; 
					public boolean hasNextDocument() {
						while ((this.next == null) && dl.hasNextDocument()) {
							DocumentListElement dle = dl.getNextDocument();
							String docId = ((String) dle.getAttribute(DOCUMENT_ID_ATTRIBUTE));
							String nextStepName = getNextStepName(docId);
							if ((nextStepFilter != null) && !nextStepFilter.contains(nextStepName))
								continue;
							if (nextStepName == null)
								this.next = dle;
							else if (allStepsPermission || this.knownPermissions.contains(nextStepName) || uaa.hasPermission(user, nextStepName, true)) {
								this.knownPermissions.add(nextStepName);
								this.next = new DocumentListElement();
								this.next.copyAttributes(dle);
								this.next.setAttribute(TODO_NEXT_ATTRIBUTE, stepTodos.getProperty(nextStepName, ""));
							}
						}
						return (this.next != null);
					}
					public DocumentListElement getNextDocument() {
						if (!this.hasNextDocument()) return null;
						DocumentListElement next = this.next;
						this.next = null;
						return next;
					}
					public DocumentAttributeSummary getListFieldValues(String listFieldName) {
						if (TODO_NEXT_ATTRIBUTE.equals(listFieldName)) {
							DocumentAttributeSummary das = new DocumentAttributeSummary();
							for (Iterator idit = stepTodos.values().iterator(); idit.hasNext();)
								das.add((String) idit.next());
							return das;
						}
						else return dl.getListFieldValues(listFieldName);
					}
				};
			}
		});
		
		UserPermissionAuthority upa = this.uaa.getUserPermissionAuthority();
		
		//	check if role management available, and create default role if it is
		if ((upa != null) && (upa instanceof UserPermissionAuthorityRBAC)) {
			this.upar = ((UserPermissionAuthorityRBAC) upa);
			this.upar.createRole(MARKUP_USER_ROLE_NAME);
			this.upar.registerPermission(ALL_STEPS_PERMISSION_NAME);
		}
		if (this.process != null)
			this.registerProcessData(this.process);
		
		//	create update action
		this.updateAction = new AsynchronousDioAction(
				null,
				this.dio,
				"Re-compute the markup states of the documents in a DIO. This is mainly for administrative purposes, namely for adding a DMC to an existing DIO that already has a document collection, or for updating the status information after major changes to the markup process definition.",
				"markup state",
				this.dataPath,
				"DmcMarkupStatusUpdateLog"
			) {
			
			/* (non-Javadoc)
			 * @see de.uka.ipd.idaho.goldenGateServer.dio.util.DioBasedUpdateAction#checkRunnable()
			 */
			protected void checkRunnable() {
				
				//	catch missing process
				if (process == null)
					throw new RuntimeException("Cannnot compute document markup states, no markup process given.");
			}
			
			protected void update(StringTupel docData) throws IOException {
				
				//	get document from DIO
				String docId = docData.getValue(DOCUMENT_ID_ATTRIBUTE);
				QueriableAnnotation doc = dio.getDocument(docId);
				this.log("    - document loaded");
				
				//	validate document
				ValidationResult vr = process.getValidator().validate(doc);
				this.log("   - document validated");
				
				//	compute access roles
				ValidationResult[] levels = vr.getPartialResults();
				String nextStepName = null;
				for (int l = 0; l < levels.length; l++) {
					ValidationResult[] tasks = levels[l].getPartialResults();
					for (int t = 0; t < tasks.length; t++) {
						ValidationResult[] steps = tasks[t].getPartialResults();
						for (int s = 0; s < steps.length; s++)
							if (!steps[s].isPassed()) {
								nextStepName = steps[s].getValidator().getFullName();
								s = steps.length;
								t = tasks.length;
								l = levels.length;
							}
					}
				}
				this.log((nextStepName == null) ? "     ==> document done" : ("     ==> next step is " + nextStepName));
				storeDocumentStatus(docId, nextStepName);
				
				//	store validation result
				storeValidationStatus(docId, vr);
				this.log("   - validation status stored");
			}
		};
	}
	
	private Properties stepTodos = new Properties();
	
	private void registerProcessData(Process process) {
		
		//	generate step permissions, and roles along the way, if possible
		Level[] levels = process.getLevels();
		for (int l = 0; l < levels.length; l++) {
			String levelRoleName = "DIO.Level." + levels[l].getName();
			
			//	if possible, create and imply level role
			if (this.upar != null) {
				this.upar.createRole(levelRoleName);
				this.upar.implyRole(MARKUP_USER_ROLE_NAME, levelRoleName);
			}
			
			Task[] tasks = levels[l].getTasks();
			for (int t = 0; t < tasks.length; t++) {
				String taskRoleName = "DIO.Task." + levels[l].getName() + "." + tasks[t].getName();
				
				//	if possible, create and imply task role
				if (this.upar != null) {
					this.upar.createRole(taskRoleName);
					this.upar.implyRole(levelRoleName, taskRoleName);
				}
				
				Step[] steps = tasks[t].getSteps();
				for (int s = 0; s < steps.length; s++) {
					String stepPermissionName = "DIO.Step." + levels[l].getName() + "." + tasks[t].getName() + "." + steps[s].getName();
					
					//	add permission
					this.uaa.registerPermission(stepPermissionName);
					
					//	grant permission to task role if possible
					if (this.upar != null)
						this.upar.grantPermission(taskRoleName, stepPermissionName);
					
					//	store todo label
					this.stepTodos.setProperty(steps[s].getFullName(), ("Level " + l + ": " + steps[s].getLabel()));
				}
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see de.goldenGateScf.ServerComponent#getActions()
	 */
	public ComponentAction[] getActions() {
		ArrayList cal = new ArrayList();
		ComponentAction ca;
		
		//	get validator for client side validation
		ca = new ComponentActionNetwork() {
			public String getActionCommand() {
				return GET_PROCESS;
			}
			public void performActionNetwork(BufferedReader input, BufferedWriter output) throws IOException {
				
				//	check authentication
				String sessionId = input.readLine();
				if (!uaa.isValidSession(sessionId)) {
					output.write("Invalid session (" + sessionId + ")");
					output.newLine();
					return;
				}
				
				//	process missing, report error
				if (process == null) {
					output.write("No markup process given.");
					output.newLine();
				}
				
				//	process given, send it
				else {
					output.write(GET_PROCESS);
					output.newLine();
					
					//	send data
					process.writeXml(output);
					output.newLine();
				}
			}
		};
		cal.add(ca);
		
		//	get validation details
		ca = new ComponentActionNetwork() {
			public String getActionCommand() {
				return GET_VALIDATION_RESULT;
			}
			public void performActionNetwork(BufferedReader input, BufferedWriter output) throws IOException {
				
				//	check authentication
				String sessionId = input.readLine();
				if (!uaa.isValidSession(sessionId)) {
					output.write("Invalid session (" + sessionId + ")");
					output.newLine();
					return;
				}
				
				//	get document ID
				String docId = input.readLine();
				
				//	get data
				ValidationResult vr = getValidationStatus(docId);
				
				//	data not available
				if (vr == null) {
					output.write("Validation details not available.");
					output.newLine();
				}
				
				//	send details
				else {
					output.write(GET_VALIDATION_RESULT);
					output.newLine();
					
					//	send validation result
					vr.writeXml(output);
					output.newLine();
				}
			}
		};
		cal.add(ca);
		
		//	update markup process
		ca = new ComponentActionNetwork() {
			public String getActionCommand() {
				return UPDATE_PROCESS;
			}
			public void performActionNetwork(final BufferedReader input, BufferedWriter output) throws IOException {
				
				//	check authentication
				String sessionId = input.readLine();
				if (!uaa.isValidSession(sessionId)) {
					output.write("Invalid session (" + sessionId + ")");
					output.newLine();
					return;
				}
				else if (!uaa.isAdminSession(sessionId)) {
					output.write("Administrative priviledges required");
					output.newLine();
					return;
				}
				
				//	read process definition
				Process process;
				try {
					TreeNode pRoot = parser.parse(new Reader() {
						String data = "";
						int offset = 0;
						public void close() throws IOException {}
						public int read(char[] cbuf, int off, int len) throws IOException {
							if (this.data == null) return -1;
							
							for (int o = 0; o < len; o++) {
								if (this.offset == this.data.length()) {
									String line = input.readLine();
									if ((line != null) && (line.length() != 0)) {
										this.data = (line + "\n");
										this.offset = 0;
									}
								}
								if (this.offset < this.data.length())
									cbuf[off + o] = this.data.charAt(this.offset++);
								else {
									this.data = null;
									return ((o == 0) ? -1 : o);
								}
							}
							return (len - off);
						}
					});
					if (TreeNode.ROOT_NODE_TYPE.equals(pRoot.getNodeType()))
						pRoot = pRoot.getChildNode(Process.PROCESS, 0);
					process = new Process(pRoot);
				}
				catch (IOException ioe) {
					output.write("Receiving process definition failed: " + ioe.getMessage());
					output.newLine();
					return;
				}
				
				try {
					File processFile = new File(dataPath, "MarkupProcess.xml");
					invalidate(processFile);
					processFile.createNewFile();
					
					OutputStreamWriter osw = new OutputStreamWriter(new FileOutputStream(processFile));
					process.writeXml(osw);
					osw.flush();
					osw.close();
				}
				catch (IOException ioe) {
					output.write("Storing process definition failed: " + ioe.getMessage());
					output.newLine();
					return;
				}
				
				//	store new process definition
				GoldenGateDMC.this.process = process;
				
				//	make new permissions available
				registerProcessData(GoldenGateDMC.this.process);
				
				//	inidicate success
				output.write(UPDATE_PROCESS);
				output.newLine();
			}
		};
		cal.add(ca);
		
		//	re-compute document states
		ca = this.updateAction;
		cal.add(ca);
		
		return ((ComponentAction[]) cal.toArray(new ComponentAction[cal.size()]));
	}
	
	private static final int ValidationResultCacheSize = 256;
	private LinkedHashMap validationResultCache = new LinkedHashMap(ValidationResultCacheSize, .9f, true) {
		protected boolean removeEldestEntry(Entry eldest) {
			return this.size() > ValidationResultCacheSize;
		}
	};
	
	private ValidationResult getValidationStatus(String docId) {
		
		//	do cache lookup
		ValidationResult vr = ((ValidationResult) this.validationResultCache.get(docId));
		
		//	cache hit
		if (vr != null) return vr;
		
		//	cannot reproduce validation result without process
		if (this.process == null) return null;
		
		//	cache miss, prepare loading data
		File vrFile = this.getValidationStatusFile(docId);
		
		//	file exists
		if (vrFile.exists()) {
			
			//	load data
			FileInputStream fis = null;
			try {
				fis = new FileInputStream(vrFile);
				
				Parser parser = new Parser();
				TreeNode vrRoot = parser.parse(fis);
				fis.close();
				
				if (TreeNode.ROOT_NODE_TYPE.equals(vrRoot.getNodeType()))
					vrRoot = vrRoot.getChildNode(Process.PROCESS, 0);
				
				if (vrRoot != null) {
					vr = this.process.loadValidationResult(vrRoot);
					this.validationResultCache.put(docId, vr);
				}
			}
			catch (IOException ioe) {
				System.out.println(ioe.getClass().getName() + " (" + ioe.getMessage() + ") while loading validation status of document " + docId);
				ioe.printStackTrace(System.out);
			}
			finally {
				if (fis != null) try {
					fis.close();
				} catch (IOException ioe) {}
			}
		}
		
		//	return result (will be null if not available)
		return vr;
	}
	
	private Properties nextStepCache = new Properties();
	private StringIndex nextStepSelectivities = new StringIndex(false);
	private void cacheNextStepName (String docId, String nextStepName) {
		String oldStepName = this.nextStepCache.getProperty(docId);
		if (oldStepName != null)
			this.nextStepSelectivities.remove(oldStepName);
		if (nextStepName == null)
			this.nextStepCache.remove(docId);
		else {
			this.nextStepCache.setProperty(docId, nextStepName);
			this.nextStepSelectivities.add(nextStepName);
		}
	}
	private String getNextStepName(String docId) {
		return this.nextStepCache.getProperty(docId);
	}
	private int getNextStepSelectivity(String nextStepName) {
		return this.nextStepSelectivities.getCount(nextStepName);
	}
	
	private void storeDocumentStatus(String docId, String nextStepName) {
		if (nextStepName == null) {
			String deleteQuery = "DELETE FROM " + DOCUMENT_STATUS_TABLE_NAME + 
					" WHERE " + DOCUMENT_ID_ATTRIBUTE + " LIKE '" + EasyIO.sqlEscape(docId) + "'" +
					";";
			try {
				this.io.executeUpdateQuery(deleteQuery);
				this.cacheNextStepName(docId, null);
			}
			catch (SQLException sqle) {
				System.out.println("GoldenGateDMC: " + sqle.getClass().getName() + " (" + sqle.getMessage() + ") while removing document status.");
				System.out.println("  Query was " + deleteQuery);
			}
		}
		else {
			String updateQuery = "UPDATE " + DOCUMENT_STATUS_TABLE_NAME + 
					" SET " + NEXT_STEP_COLUMN_NAME + " = '" + EasyIO.sqlEscape(nextStepName) + "'" +
					" WHERE " + DOCUMENT_ID_ATTRIBUTE + " LIKE '" + EasyIO.sqlEscape(docId) + "'" +
					";";
			try {
				if (this.io.executeUpdateQuery(updateQuery) == 0) {
					String insertQuery = "INSERT INTO " + DOCUMENT_STATUS_TABLE_NAME + 
							" (" + DOCUMENT_ID_ATTRIBUTE + ", " + NEXT_STEP_COLUMN_NAME + ")" +
							" VALUES" +
							" ('" + EasyIO.sqlEscape(docId) + "', '" + EasyIO.sqlEscape(nextStepName) + "')" +
							";";
					try {
						this.io.executeUpdateQuery(insertQuery);
					}
					catch (SQLException sqle) {
						System.out.println("GoldenGateDMC: " + sqle.getClass().getName() + " (" + sqle.getMessage() + ") while storing document status.");
						System.out.println("  Query was " + insertQuery);
					}
				}
				this.cacheNextStepName(docId, nextStepName);
			}
			catch (SQLException sqle) {
				System.out.println("GoldenGateDMC: " + sqle.getClass().getName() + " (" + sqle.getMessage() + ") while updating document status.");
				System.out.println("  Query was " + updateQuery);
			}
		}
	}
	
	private void storeValidationStatus(String docId, ValidationResult vr) {
		this.validationResultCache.remove(docId);
		
		File vrFile = this.getValidationStatusFile(docId);
		if (vrFile.exists()) {
			vrFile.renameTo(new File(vrFile.getPath() + "." + System.currentTimeMillis() + ".old"));
			vrFile = this.getValidationStatusFile(docId);
		}
		
		FileOutputStream fos = null;
		try {
			vrFile.getParentFile().mkdirs();
			vrFile.createNewFile();
			
			fos = new FileOutputStream(vrFile);
			Writer w = new OutputStreamWriter(fos);
			vr.writeXml(w);
			w.flush();
			w.close();
		}
		catch (IOException ioe) {
			System.out.println(ioe.getClass().getName() + " (" + ioe.getMessage() + ") while saving validation status of document " + docId);
			ioe.printStackTrace(System.out);
		}
		finally {
			if (fos != null) try {
				fos.close();
			} catch (IOException ioe) {}
		}
	}
	
	private File getValidationStatusFile(String docId) {
		String primaryFolderName = docId.substring(0, 2);
		String secondaryFolderName = docId.substring(2, 4);
		return new File(this.documentValidationStatusFolder + "/" + primaryFolderName + "/" + secondaryFolderName + "/" + docId + ".validation");
	}
	
	private void deleteDocument(final String docId) {
		String deleteQuery = "DELETE FROM " + DOCUMENT_STATUS_TABLE_NAME + " WHERE " + DOCUMENT_ID_ATTRIBUTE + " LIKE '" + EasyIO.sqlEscape(docId) + "';";
		try {
			this.io.executeUpdateQuery(deleteQuery);
			this.cacheNextStepName(docId, null);
		}
		catch (SQLException sqle) {
			System.out.println("GoldenGateDMC: " + sqle.getClass().getName() + " (" + sqle.getMessage() + ") while deleting document.");
			System.out.println("  Query was " + deleteQuery);
		}
		File vsFile = this.getValidationStatusFile(docId);
		if (vsFile.exists()) {
			vsFile = vsFile.getParentFile();
			File[] vsFiles = vsFile.listFiles(new FileFilter() {
				public boolean accept(File file) {
					return (file.isFile() && file.getName().startsWith(docId));
				}
			});
			if (vsFiles != null)
				for (int f = 0; f < vsFiles.length; f++)
					vsFiles[f].delete();
		}
	}
	
	private void invalidate(File file) {
		if (file.exists()) {
			String fileName = file.getAbsolutePath();
			file.renameTo(new File(fileName + "." + System.currentTimeMillis() + ".old"));
		}
	}
}
