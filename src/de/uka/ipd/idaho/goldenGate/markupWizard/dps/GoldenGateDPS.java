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
package de.uka.ipd.idaho.goldenGate.markupWizard.dps;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.LinkedList;
import java.util.Properties;

import de.uka.ipd.idaho.easyIO.EasyIO;
import de.uka.ipd.idaho.easyIO.IoProvider;
import de.uka.ipd.idaho.easyIO.SqlQueryResult;
import de.uka.ipd.idaho.easyIO.sql.TableDefinition;
import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.DocumentRoot;
import de.uka.ipd.idaho.gamta.util.feedback.FeedbackPanel;
import de.uka.ipd.idaho.gamta.util.validation.Level;
import de.uka.ipd.idaho.gamta.util.validation.Level.LevelValidationResult;
import de.uka.ipd.idaho.gamta.util.validation.Process;
import de.uka.ipd.idaho.gamta.util.validation.Process.ProcessValidationResult;
import de.uka.ipd.idaho.gamta.util.validation.Step;
import de.uka.ipd.idaho.gamta.util.validation.Step.StepValidationResult;
import de.uka.ipd.idaho.gamta.util.validation.Task;
import de.uka.ipd.idaho.gamta.util.validation.Task.TaskValidationResult;
import de.uka.ipd.idaho.gamta.util.validation.ValidationResult;
import de.uka.ipd.idaho.goldenGate.GoldenGATE;
import de.uka.ipd.idaho.goldenGate.GoldenGateConfiguration;
import de.uka.ipd.idaho.goldenGate.configuration.ConfigurationUtils;
import de.uka.ipd.idaho.goldenGate.markupWizard.ProcessPartMapper;
import de.uka.ipd.idaho.goldenGate.markupWizard.ProcessPartMapper.ProcessPartMapping;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessor;
import de.uka.ipd.idaho.goldenGateServer.AbstractGoldenGateServerComponent;
import de.uka.ipd.idaho.goldenGateServer.GoldenGateServerComponentRegistry;
import de.uka.ipd.idaho.goldenGateServer.dio.GoldenGateDIO;
import de.uka.ipd.idaho.goldenGateServer.dio.GoldenGateDioConstants;
import de.uka.ipd.idaho.goldenGateServer.dio.GoldenGateDioConstants.DioDocumentEvent;
import de.uka.ipd.idaho.goldenGateServer.dio.GoldenGateDioConstants.DioDocumentEvent.DioDocumentEventListener;
import de.uka.ipd.idaho.goldenGateServer.dio.data.DocumentList;
import de.uka.ipd.idaho.goldenGateServer.dio.data.DocumentListElement;
import de.uka.ipd.idaho.goldenGateServer.dio.util.AsynchronousDioAction;
import de.uka.ipd.idaho.goldenGateServer.uaa.UserAccessAuthority;
import de.uka.ipd.idaho.htmlXmlUtil.Parser;
import de.uka.ipd.idaho.htmlXmlUtil.TreeNode;
import de.uka.ipd.idaho.stringUtils.StringVector;
import de.uka.ipd.idaho.stringUtils.csvHandler.StringTupel;

/**
 * The GoldenGATE Document Processing Server (DPS) does automated processing of
 * documents stored in a CMS. In particular, it listens for updates to documents
 * in DIO and determines if there are any markup measures that can be done
 * automatically, based on the validation status of the document. For gaining
 * editing access to the documents, DPS creates a user account named
 * 'Markup_Community'.<br>
 * <b>Attention:</b> DPS runs an internal GoldenGATE Editor instance and
 * therefore cannot be in the same GoldenGATE Server as any other component
 * running a GoldenGATE Editor instance.
 * 
 * @author mathaess
 * @author sautter
 */
public class GoldenGateDPS extends AbstractGoldenGateServerComponent implements GoldenGateDpsConstants {
	
	private static final Parser parser = new Parser();
	
	private static final String mappingFileName = "mapping.txt";
	private static final String ggConfigNameParameter = "GgConfigName";
	private static final String ggConfigHostParameter = "GgConfigHost";
	private static final String ggConfigPathParameter = "GgConfigPath";
	private final static String goldenGateDpsUser = "DpsMarkupCommunity";
	
	private UserAccessAuthority uaa = null;
	private GoldenGateDIO dio;
	
	private String ggConfigHost;
	private String ggConfigPath;
	private Process process;
	private ProcessPartMapper processPartMapper = new ProcessPartMapper(new String[0]);
	private GoldenGATE goldenGate;
	private DistributionThread distributionThread;
	private boolean serverStartup = true;
	
	private static final String PROCESSOR_HISTORY_TABLE_NAME = "DpsProcessorHistory";
	private static final String DOCUMENT_ID_COLUMN_NAME = "documentID";
	private static final String PROCESSOR_NAME_COLUMN_NAME = "processor";
	private static final String START_TIME_COLUMN_NAME = "starttime";
	private static final String FINISH_TIME_COLUMN_NAME = "finishtime";
	private static final String ERROR_COUNTER_COLUMN_NAME = "errorcount"; 
	private static final String ERROR_TYPE_COLUMN_NAME = "errortype"; 
	private static final String ERROR_MESSAGE_COLUMN_NAME = "errormessage"; 
	private static final String MANUAL_EDIT_DP_NAME = "HUMAN_USER";
	private static final int MAX_ERROR_COUNT = 3;
	
	private IoProvider io;

	/** Constructor passing 'DPS' as the letter code to super constructor
	 */
	public GoldenGateDPS() {
		super("DPS");
	}

	/*
	 * (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGateServer.GoldenGateServerComponent#getActions()
	 */
	public ComponentAction[] getActions() {
		ArrayList cal = new ArrayList();
		ComponentAction ca;
		
		if (this.uaa == null)
			return ((ComponentAction[]) cal.toArray(new ComponentAction[cal.size()]));
		
		
		//	get statistics
		ca = new ComponentActionNetwork() {
			public String getActionCommand() {
				return GET_STATISICS;
			}
			public void performActionNetwork(BufferedReader input, BufferedWriter output) throws IOException {
				
				//	get statistics
				DpsStatistics dpss = getStatistics();
				
				//	indicate success
				output.write(GET_STATISICS);
				output.newLine();
				
				//	send data
				dpss.writeXml(output);
				output.newLine();
			}
		};
		cal.add(ca);
		
		//	request for document configuration name
		ca = new ComponentActionNetwork() {
			public String getActionCommand() {
				return GET_CONGIFURATION_NAME;
			}
			public void performActionNetwork(BufferedReader input, BufferedWriter output) throws IOException {
				
				//	check authentication
				String sessionId = input.readLine();
				if (!uaa.isValidSession(sessionId)) {
					output.write("Invalid session (" + sessionId + ")");
					output.newLine();
					return;
				}
				
				//	send data
				output.write(GET_CONGIFURATION_NAME);
				output.newLine();
				output.write((goldenGate == null) ? "" : goldenGate.getConfigurationName());
				output.newLine();
			}
		};
		cal.add(ca);
		
		//	get markup process
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
		
		//	request for mappings
		ca = new ComponentActionNetwork() {
			public String getActionCommand() {
				return GET_PROCESS_PART_MAPPINGS;
			}
			public void performActionNetwork(BufferedReader input, BufferedWriter output) throws IOException {
				
				//	check authentication
				String sessionId = input.readLine();
				if (!uaa.isValidSession(sessionId)) {
					output.write("Invalid session (" + sessionId + ")");
					output.newLine();
					return;
				}
				
				//	get markup process part names
				String[] processParts = getStepNames();
				
				//	send data
				output.write(GET_PROCESS_PART_MAPPINGS);
				output.newLine();
				for (int p = 0; p < processParts.length; p++) {
					output.write(processPartMapper.getProcessPartMapping(processParts[p]).toString());
					output.newLine();
				}
			}
		};
		cal.add(ca);
		
		//	configuration name update
		ca = new ComponentActionNetwork() {
			public String getActionCommand() {
				return SET_CONGIFURATION_NAME;
			}
			public void performActionNetwork(BufferedReader input, BufferedWriter output) throws IOException {
				
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
				
				//	read configuration name
				String configName = input.readLine();
				setGgConfiguration(configName);
				
				//	send data
				output.write(SET_CONGIFURATION_NAME);
				output.newLine();
			}
		};
		cal.add(ca);
		
		//	re-assess documents
		ca = new AsynchronousDioAction("update", dio, "Re-assess the markup process state of all documents, e.g. after an update to the markup process definition.", "document status", null, null) {
			
			/* (non-Javadoc)
			 * @see de.uka.ipd.idaho.goldenGateServer.dio.util.AsynchronousDioAction#update(de.uka.ipd.idaho.stringUtils.csvHandler.StringTupel)
			 */
			protected void update(StringTupel docData) throws IOException {
				String docId = docData.getValue(DOCUMENT_ID_ATTRIBUTE);
				if (docId == null)
					return;
				
				//	are we running at all?
				if (distributionThread == null) {
					System.out.println("DPS not active, check process definition and GG Configuration.");
					return;
				}
				
				//	get document
				DocumentRoot doc = dio.getDocument(docId);
				
				//	assess document status
				ValidationResult validationResult = process.getValidator().validate(doc);
				StepValidationResult failedStepResult = getFailedStepResult(validationResult);
				
				//	this one's done
				if (failedStepResult == null) {
					System.out.println("Document done.");
					return;
				}
				
				//	get processor to apply
				ProcessPartMapping mapping = processPartMapper.getProcessPartMapping(failedStepResult.getValidator().getFullName());
				String dpName = ((mapping == null) ? null : mapping.documentProcessorName);
				
				//	we cannot do anything about this one's
				if (dpName == null) {
					System.out.println("No processor found for document.");
					return;
				}
				
				// check if processor has not been applied to document before or is running on document at the moment
				// document is now only enqueued, if designated processor has not been assigned to the 
				// document in the past
				HashSet history = loadDocumentHistory(docId);
				if (!isProcessorRunningOnDocument(docId, dpName) && !history.contains(dpName))
					enqueueProcessorForDocument(docId, dpName);
			}
		};
		cal.add(ca);
		
		return ((ComponentAction[]) cal.toArray(new ComponentAction[cal.size()]));
	}
	
	private String[] getStepNames() {
		StringVector processPartNames = new StringVector();
		
		Level[] processLevels = this.process.getLevels();
		for (int l = 0; l < processLevels.length; l++) {
			String levelName = processLevels[l].getName();
			
			Task[] levelTasks = processLevels[l].getTasks();
			for (int t = 0; t < levelTasks.length; t++) {
				String taskName = levelName + "." + levelTasks[t].getName();
				
				Step[] taskSteps = levelTasks[t].getSteps();
				for (int s = 0; s < taskSteps.length; s++) {
					String stepName = taskName + "." + taskSteps[s].getName();
					processPartNames.addElement(stepName);
				}
			}
		}
		
		return processPartNames.toStringArray();
	}
	
	/*
	 * (non-Javadoc)
	 * 
	 * @see de.uka.ipd.idaho.goldenGateServer.AbstractGoldenGateServerComponent#initComponent()
	 */
	protected void initComponent() {
		
		//	get IoProvider from host
		this.io = this.host.getIoProvider();
		if (!this.io.isJdbcAvailable())
			throw new RuntimeException("GoldenGateDPS: Cannot work without database");
		
		//	create log table
		TableDefinition td = new TableDefinition(PROCESSOR_HISTORY_TABLE_NAME);
		td.addColumn(DOCUMENT_ID_COLUMN_NAME, TableDefinition.VARCHAR_DATATYPE, 32);
		td.addColumn(PROCESSOR_NAME_COLUMN_NAME, TableDefinition.VARCHAR_DATATYPE, 128);
		td.addColumn(START_TIME_COLUMN_NAME, TableDefinition.BIGINT_DATATYPE, 0);
		td.addColumn(FINISH_TIME_COLUMN_NAME, TableDefinition.BIGINT_DATATYPE, 0);
		td.addColumn(ERROR_COUNTER_COLUMN_NAME, TableDefinition.INT_DATATYPE, 0);
		td.addColumn(ERROR_TYPE_COLUMN_NAME, TableDefinition.VARCHAR_DATATYPE, 64);
		td.addColumn(ERROR_MESSAGE_COLUMN_NAME, TableDefinition.VARCHAR_DATATYPE, 256);
		if (!this.io.ensureTable(td, true))
			throw new RuntimeException("GoldenGateDPS: Cannot work without database");
		
		//	index log table
		this.io.indexColumn(PROCESSOR_HISTORY_TABLE_NAME, DOCUMENT_ID_COLUMN_NAME);
		this.io.indexColumn(PROCESSOR_HISTORY_TABLE_NAME, PROCESSOR_NAME_COLUMN_NAME);
		this.io.indexColumn(PROCESSOR_HISTORY_TABLE_NAME, START_TIME_COLUMN_NAME);
		this.io.indexColumn(PROCESSOR_HISTORY_TABLE_NAME, FINISH_TIME_COLUMN_NAME);
		this.io.indexColumn(PROCESSOR_HISTORY_TABLE_NAME, ERROR_COUNTER_COLUMN_NAME);
		
		//	read maximum parallelity of processing
		try {
			this.documentProcessingThreadLimit = Integer.parseInt(this.configuration.getSetting("documentProcessingThreadLimit", ("" + this.documentProcessingThreadLimit)));
		} catch (NumberFormatException nfe) {}
		
		//	load step to status mapping
		try {
			StringVector stepToStateLines = StringVector.loadList(new File(this.dataPath, "stepsToStates.cnfg"));
			for (int s = 0; s < stepToStateLines.size(); s++) {
				String stsLine = stepToStateLines.get(s);
				if (stsLine.startsWith("//"))
					continue;
				int split = stsLine.indexOf(" ");
				if (split == -1)
					continue;
				String step = stsLine.substring(0, split).trim();
				String stepStatus = stsLine.substring(split+1).trim();
				this.stepsToStates.setProperty(step, stepStatus);
			}
		}
		catch (IOException ioe) {
			System.out.println("Error loading step to status mappings: " + ioe.getMessage());
			ioe.printStackTrace(System.out);
		}
	}
	
	public void link() {
		
		//	get access authority
		this.uaa = ((UserAccessAuthority) GoldenGateServerComponentRegistry.getServerComponent(UserAccessAuthority.class.getName()));
		
		//	check success
		if (this.uaa == null) throw new RuntimeException("Cannot work without " + UserAccessAuthority.class.getName());
		
		// get GoldenGateDIO from server component registry
		this.dio = ((GoldenGateDIO) GoldenGateServerComponentRegistry.getServerComponent(GoldenGateDIO.class.getName()));
		
		//	check success
		if (this.dio == null) throw new RuntimeException("Cannot work without " + GoldenGateDIO.class.getName());
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGateServer.AbstractGoldenGateServerComponent#linkInit()
	 */
	public void linkInit() {
		
		//	read how to access GoldenGATE config
		this.ggConfigHost = this.configuration.getSetting(ggConfigHostParameter);
		this.ggConfigPath = this.configuration.getSetting(ggConfigPathParameter);
		
		//	get GoldenGATE configuration
		String ggConfigName = this.configuration.getSetting(ggConfigNameParameter);
		
		//	start with this configuration
		try {
			this.setGgConfiguration(ggConfigName);
		}
		catch (IOException ioe) {
			System.out.println("Error creating GoldenGATE instance from configuration '" + ggConfigName + "': " + ioe.getMessage());
			ioe.printStackTrace(System.out);
		}
		
		// register DocumentStorageListener with DIO
		this.dio.addDocumentEventListener(new DioDocumentEventListener() {
			
			public void documentCheckedOut(DioDocumentEvent dse) {
				/*
				 * If the DPS is the one just checking out, it would be bad to
				 * delete the apr needed next ... If someone else did the
				 * checkout, the request can be removed since a new one will be
				 * created anyway, as soon as the document is released again
				 */
				System.out.println("GoldenGateDPS: document " + dse.documentId + " checked out by " + dse.user);
				if (goldenGateDpsUser.equals(dse.user))
					System.out.println("  - oh, that's myself ...");
				
				else if (distributionThread != null) {
					distributionThread.cancelRequest(dse.documentId);
					System.out.println("  - request canceled (if there was any)");
				}
			}
			
			public void documentReleased(DioDocumentEvent dse) {
				System.out.println("GoldenGateDPS: got release for " + dse.documentId);
				if (distributionThread != null) {
					String dpName = getProcessorNameForDocument(dse.documentId);
					if (dpName != null) {
						System.out.println("  - got DP: " + dpName);
						
						DocumentProcessingRequest apr = new DocumentProcessingRequest(dse.documentId);
						System.out.println("  - DPR created");
						
						distributionThread.enqueueRequest(apr);
						System.out.println("  - DPR enqueued");
					}
				}
			}
			
			public void documentDeleted(DioDocumentEvent dse) {
				System.out.println("GoldenGateDPS: got delete for " + dse.documentId);
				if (distributionThread != null)
					cleanupDeletedDocument(dse.documentId);
			}
			
			// if document was updated, get new ValidationResult
			public void documentUpdated(DioDocumentEvent dse) {
				
				// if document is checked out, look if it was the DPS or another user.
				// If so, create a dummy-tupel in order to show when was the last time that another 
				// user worked on the document (check with Guido if this works)
				if (!goldenGateDpsUser.equals(dse.document.getAttribute(GoldenGateDioConstants.UPDATE_USER_ATTRIBUTE))) {
					String insertQuery = ("INSERT INTO " + PROCESSOR_HISTORY_TABLE_NAME + " (" + 
							DOCUMENT_ID_COLUMN_NAME + ", " + PROCESSOR_NAME_COLUMN_NAME + ", " + START_TIME_COLUMN_NAME + ", " + FINISH_TIME_COLUMN_NAME + ", " + ERROR_COUNTER_COLUMN_NAME + 
							") VALUES" + " (" +
							"'" + EasyIO.sqlEscape(dse.documentId) + "', '" + MANUAL_EDIT_DP_NAME + "', " + System.currentTimeMillis() + ", " + System.currentTimeMillis() + ", 0" +
							");");
					try {
						io.executeUpdateQuery(insertQuery);
					}
					catch (SQLException sqle) {
						System.out.println("Exception writing manual edit to table for Document " + dse.documentId + " when checked out by someone else: " + sqle.getMessage());
						System.out.println("  query was " + insertQuery);
					}
				}
				
				//	validate if possible
				if (distributionThread != null) {
					
					// some step failed, and apply the assigned document processors
					ValidationResult validationResult = process.getValidator().validate(dse.document);
					StepValidationResult failedStepResult = getFailedStepResult(validationResult);
					
					// create processingRequest
					String dpName;
					if (failedStepResult == null)
						dpName = null;
					else {
						ProcessPartMapping mapping = processPartMapper.getProcessPartMapping(failedStepResult.getValidator().getFullName());
						dpName = ((mapping == null) ? null : mapping.documentProcessorName);
					}
					if (dpName != null) {
						HashSet history = loadDocumentHistory(dse.documentId);
						
						// check if processor has not been applied to document before or is running on document at the moment
						// document is now only enqueued, if designated processor has not been assigned to the 
						// document in the past
						if (!isProcessorRunningOnDocument(dse.documentId, dpName) && !history.contains(dpName))
							enqueueProcessorForDocument(dse.documentId, dpName);
					}
				}
			}
		});
		
		//	unlatch distribution thread
		this.serverStartup = false;
	}
	
	private StepValidationResult getFailedStepResult(ValidationResult pvr) {
		if (pvr instanceof ProcessValidationResult) {
			if (pvr.isPassed()) return null;
			
			ValidationResult[] levelResults = pvr.getPartialResults();
			for (int l = 0; l < levelResults.length; l++) {
				if (!levelResults[l].isPassed())
					return getFailedStepResult(levelResults[l]);
			}
		}
		else if (pvr instanceof LevelValidationResult) {
			if (pvr.isPassed()) return null;
			
			ValidationResult[] taskResults = pvr.getPartialResults();
			for (int t = 0; t < taskResults.length; t++) {
				if (!taskResults[t].isPassed())
					return getFailedStepResult(taskResults[t]);
			}
		}
		else if (pvr instanceof TaskValidationResult) {
			if (pvr.isPassed()) return null;
			
			ValidationResult[] stepResults = pvr.getPartialResults();
			for (int s = 0; s < stepResults.length; s++) {
				if (!stepResults[s].isPassed())
					return ((StepValidationResult) stepResults[s]);
			}
		}
		else if (pvr instanceof StepValidationResult) {
			if (pvr.isPassed()) return null;
			else return ((StepValidationResult) pvr);
		}
		
		return null;
	}
	
	private DpsStatistics cachedDpsStatistics = null;
	private Properties stepsToStates = new Properties();
	private boolean stepsToStatesModified = false;
	
	private DpsStatistics getStatistics() {
		if (this.cachedDpsStatistics != null)
			return this.cachedDpsStatistics;
		return this.computeStatistics(null);
	}
	
	private synchronized DpsStatistics computeStatistics(String logPrefix) {
		if (logPrefix != null)
			System.out.println("  - (" + logPrefix + ") computing statistics");
		
		//	we'll need these two multiple times
		SqlQueryResult sqr = null;
		String query;
		
		//	get base data
		int docCount = 0;
		query = "SELECT count(DISTINCT " + DOCUMENT_ID_COLUMN_NAME + ")" +
				" FROM " + PROCESSOR_HISTORY_TABLE_NAME + 
				" WHERE (" + PROCESSOR_NAME_COLUMN_NAME + " NOT LIKE '" + MANUAL_EDIT_DP_NAME + "')" + 
					" AND (" + ERROR_COUNTER_COLUMN_NAME + " < " + MAX_ERROR_COUNT + ")" + 
				";";
		try {
			sqr = this.io.executeSelectQuery(query, true);
			docCount = (sqr.next() ? Integer.parseInt(sqr.getString(0)) : 0);
		}
		catch (SQLException sqle) {
			System.out.println("Exception getting document count: " + sqle.getMessage());
			System.out.println("  query was " + query);
		}
		finally {
			if (sqr != null)
				sqr.close();
		}
		if (logPrefix != null)
			System.out.println("    - doc count is " + docCount);
		
		int nonFinishedDocCount = 0; // all documents for which at least one DP as not been fully executed 
		query = "SELECT count(DISTINCT " + DOCUMENT_ID_COLUMN_NAME + ")" +
				" FROM " + PROCESSOR_HISTORY_TABLE_NAME + 
				" WHERE " + FINISH_TIME_COLUMN_NAME + " = 0" + 
					" AND " + ERROR_COUNTER_COLUMN_NAME + " < " + MAX_ERROR_COUNT + 
					" AND " + PROCESSOR_NAME_COLUMN_NAME + " NOT LIKE '" + MANUAL_EDIT_DP_NAME + "'" + 
				";";
		try {
			sqr = this.io.executeSelectQuery(query, true);
			nonFinishedDocCount = (sqr.next() ? Integer.parseInt(sqr.getString(0)) : 0);
		}
		catch (SQLException sqle) {
			System.out.println("Exception getting non-finished document count: " + sqle.getMessage());
			System.out.println("  query was " + query);
		}
		finally {
			if (sqr != null)
				sqr.close();
		}
		if (logPrefix != null)
			System.out.println("    - non-finished doc count is " + nonFinishedDocCount);
		
		int startedDocCount = 0; // all documents for which at least one DP has been started and did not exit in an error
		query = "SELECT count(DISTINCT " + DOCUMENT_ID_COLUMN_NAME + ")" +
				" FROM " + PROCESSOR_HISTORY_TABLE_NAME + 
				" WHERE (" + START_TIME_COLUMN_NAME + " > 0)" + 
					" AND (" + ERROR_COUNTER_COLUMN_NAME + " < " + MAX_ERROR_COUNT + ")" +
					" AND (" + PROCESSOR_NAME_COLUMN_NAME + " NOT LIKE '" + MANUAL_EDIT_DP_NAME + "')" + 
				";";
		try {
			sqr = this.io.executeSelectQuery(query, true);
			startedDocCount = (sqr.next() ? Integer.parseInt(sqr.getString(0)) : 0);
		}
		catch (SQLException sqle) {
			System.out.println("Exception getting started document count: " + sqle.getMessage());
			System.out.println("  query was " + query);
		}
		finally {
			if (sqr != null)
				sqr.close();
		}
		if (logPrefix != null)
			System.out.println("    - started doc count is " + startedDocCount);
		
		//	compute statistics data
		int pendingDocCount = (docCount - startedDocCount);
		if (logPrefix != null)
			System.out.println("    - pending doc count is " + pendingDocCount);
		int finishedDocCount = (docCount - nonFinishedDocCount);
		if (logPrefix != null)
			System.out.println("    - finished doc count is " + finishedDocCount);
		int processingDocCount = (startedDocCount - finishedDocCount);
		if (logPrefix != null)
			System.out.println("    - processing doc count is " + processingDocCount);
		
		//	create base object
		DpsStatistics dpss = new DpsStatistics(docCount, pendingDocCount, processingDocCount, finishedDocCount);
		
		//	if we have no process, we have no step order, so we're done here
		if (this.process == null) {
			this.cachedDpsStatistics = dpss;
			return dpss;
		}
		
		//	status doc counts
		query = "SELECT " + PROCESSOR_NAME_COLUMN_NAME + ", count(" + DOCUMENT_ID_COLUMN_NAME + ")" +
			" FROM " + PROCESSOR_HISTORY_TABLE_NAME + " dph " +
			" WHERE (" + PROCESSOR_NAME_COLUMN_NAME + " NOT LIKE '" + MANUAL_EDIT_DP_NAME + "')" +
				" AND (" + FINISH_TIME_COLUMN_NAME + " = (" +
					"SELECT max(" + FINISH_TIME_COLUMN_NAME + ")" +
					" FROM " + PROCESSOR_HISTORY_TABLE_NAME + 
					" WHERE " + DOCUMENT_ID_COLUMN_NAME + " LIKE dph." + DOCUMENT_ID_COLUMN_NAME + ")" +
				")" +
			" GROUP BY " + PROCESSOR_NAME_COLUMN_NAME +
			";";
		try {
			Properties dpDocCounts = new Properties();
			sqr = this.io.executeSelectQuery(query, true);
			while (sqr.next()) {
				String dpName = sqr.getString(0);
				String dpDocCount = sqr.getString(1);
				dpDocCounts.setProperty(dpName, dpDocCount);
			}
			
			String[] stepNames = this.getStepNames();
			LinkedHashMap statusDocCounts = new LinkedHashMap();
			for (int s = 0; s < stepNames.length; s++) {
				ProcessPartMapping stepMapping = this.processPartMapper.getProcessPartMapping(stepNames[s]);
				if ((stepMapping == null) || (stepMapping.documentProcessorName == null))
					continue;
				
				String stepDocCount = dpDocCounts.getProperty(stepMapping.processPartName);
				if (stepDocCount == null)
					continue;
				
				if (logPrefix != null)
					System.out.println("    - " + stepDocCount + " documents in step " + stepNames[s]);
				
				String status = this.stepsToStates.getProperty(stepNames[s]);
				if (status == null) {
					status = stepNames[s];
					this.stepsToStates.setProperty(stepNames[s], status);
					this.stepsToStatesModified = true;
				}
				Integer statusDocCount = ((Integer) statusDocCounts.get(status));
				if (statusDocCount == null)
					statusDocCounts.put(status, new Integer(stepDocCount));
				else statusDocCounts.put(status, new Integer(Integer.parseInt(stepDocCount) + statusDocCount.intValue()));
			}
			
			for (Iterator sit = statusDocCounts.keySet().iterator(); sit.hasNext();) {
				String status = ((String) sit.next());
				dpss.setStatusDocCount(status, ((Integer) statusDocCounts.get(status)).intValue());
				
				if (logPrefix != null)
					System.out.println("    - " + ((Integer) statusDocCounts.get(status)).intValue() + " documents in status " + status);
			}
		}
		catch (SQLException sqle) {
			System.out.println("Exception getting document status counts: " + sqle.getMessage());
			System.out.println("  query was " + query);
		}
		finally {
			if (sqr != null)
				sqr.close();
		}
		
		//	cache it
		this.cachedDpsStatistics = dpss;
		
		//	we're done
		return dpss;
	}
	
	private synchronized void startGoldenGate(GoldenGateConfiguration ggConfig) throws IOException {
		System.out.println("GoldenGateDPS: starting up GoldenGATE instance with configuration '" + ggConfig.getName() + "'");
		
		//	load markup process
		InputStream pis = ggConfig.getInputStream("process.xml");
		TreeNode pRoot = parser.parse(pis);
		pis.close();
		if (TreeNode.ROOT_NODE_TYPE.equals(pRoot.getNodeType()))
			pRoot = pRoot.getChildNode(Process.PROCESS, 0);
		Process process = new Process(pRoot);
		System.out.println("  - markup process loaded");
		
		//	load mappings
		InputStream mis = ggConfig.getInputStream(mappingFileName);
		StringVector mappingLines = StringVector.loadList(mis);
		mis.close();
		ProcessPartMapper processPartMapper = new ProcessPartMapper(new String[0]);
		for (int m = 0; m < mappingLines.size(); m++) try {
			ProcessPartMapping mapping = ProcessPartMapping.parseMapping(mappingLines.get(m));
			if (mapping != null)
				processPartMapper.setProcessPartMapping(mapping.processPartName, mapping.documentProcessorName, mapping.documentProcessorTrusted);
		}
		catch (RuntimeException re) {
			System.out.println("  - invalid mapping '" + mappingLines.get(m) + "'");
		}
		System.out.println("  - mappings loaded");
		
		//	create GG instance
		GoldenGATE gg = GoldenGATE.openGoldenGATE(ggConfig, false, false);
		while (!gg.isStartupFinished()) try {
			System.out.println("  - GG instance created, waiting on startup to complete");
			Thread.sleep(200);
		} catch (InterruptedException ie) {}
		System.out.println("  - GG instance startup complete");
		
		//	switch
		this.process = process;
		this.processPartMapper = processPartMapper;
		this.goldenGate = gg;
		
		//	clear statistics cache
		this.cachedDpsStatistics = null;
		
		//	start distribution thread
		this.distributionThread = new DistributionThread();
		this.distributionThread.start();
		System.out.println("  - distribution thread started");
		
		
		//	reset assignments that ended with an error
		String resetQuery = "UPDATE " + PROCESSOR_HISTORY_TABLE_NAME +
					" SET " + START_TIME_COLUMN_NAME + " = 0" +
					" WHERE " + START_TIME_COLUMN_NAME + " > 0" +
						" AND " + FINISH_TIME_COLUMN_NAME + " = 0" +
						" AND " + ERROR_COUNTER_COLUMN_NAME + " < " + MAX_ERROR_COUNT +
					";";
		
		System.out.println("  - resetting document processing requests");
		try {
			this.io.executeUpdateQuery(resetQuery);
		} 
		catch (SQLException sqle) {
			System.out.println("Exception resetting starttime for error-canceled requests in DB: " + sqle.getMessage() + "\n  query was " + resetQuery);
			sqle.printStackTrace(System.out);
		}
		
		/*
		 * refill request queue enqueue pending (non-finished) DP assignments
		 * from database this has to be done since we could just be in
		 * restart-phase after component crashed. This way, we do not lose all
		 * information we gathered before the crash and recover as much as
		 * possible from database
		 */
		
		//	cache checkout users (takes some memory, but WAY faster than getting checkout user individually for each document)
		Properties documentCheckoutUsers = new Properties();
		DocumentList dl = this.dio.getDocumentList(UserAccessAuthority.SUPERUSER_NAME);
		while (dl.hasNextDocument()) {
			DocumentListElement dle = dl.getNextDocument();
			String docId = ((String) dle.getAttribute(GoldenGateDIO.DOCUMENT_ID_ATTRIBUTE));
			String checkoutUser = ((String) dle.getAttribute(GoldenGateDIO.CHECKOUT_USER_ATTRIBUTE));
			documentCheckoutUsers.setProperty(docId, checkoutUser);
		}
		
		//	prepare database lookup
		SqlQueryResult result = null;
		String resumeQuery = "SELECT " + DOCUMENT_ID_COLUMN_NAME + ", " + PROCESSOR_NAME_COLUMN_NAME + "," + START_TIME_COLUMN_NAME +
				" FROM " + PROCESSOR_HISTORY_TABLE_NAME + 
				" WHERE " + FINISH_TIME_COLUMN_NAME + " = 0" +
					" AND " + ERROR_COUNTER_COLUMN_NAME + " < " + MAX_ERROR_COUNT + 
				" ORDER BY " + START_TIME_COLUMN_NAME + " DESC" +
				";";
		
		System.out.println("  - resuming interrupted document processing requests");
		
		//	get document processing states
		StringVector toRelease = new StringVector();
		try {
			result = this.io.executeSelectQuery(resumeQuery);
			
			// Get all pending assignements (i.e. those where we started but did not finish working on) and re-create requests
			while (result.next()) {
				String docId = result.getString(0);
				String dpName = result.getString(1);
				String checkoutUser = documentCheckoutUsers.getProperty(docId);
				
				//	we hold the lock on this document, remember to release it (release will enqueue the pending processing request)
				if (goldenGateDpsUser.equals(checkoutUser)) {
					toRelease.addElementIgnoreDuplicates(docId);
					continue;
				}
				
				//	document is checked out by other user (if so, release by checkout user will enqueue the processing request)
				else if (!"".equals(checkoutUser))
					continue;
				
				//	document is not checked out ==> enqueue processing request if processor available
				else if ((dpName != null) && (this.goldenGate.getDocumentProcessorForName(dpName) != null)) {
					DocumentProcessingRequest dpr = new DocumentProcessingRequest(docId);
					this.distributionThread.enqueueRequest(dpr);
//					System.out.println("    - " + dpr.docID);
				}
			}
		}
		catch (SQLException sqle) {
			System.out.println("Exception restoring pending processor assignments: " + sqle.getMessage() + "\n  query was " + resumeQuery);
			sqle.printStackTrace(System.out);
		}
		finally {
			if (result != null)
				result.close();
		}
		
		//	release document we hold the lock for
		for (int r = 0; r < toRelease.size(); r++)
			this.dio.releaseDocument(goldenGateDpsUser, toRelease.get(r));
		
		//	we're done
		System.out.println("  - startup complete");
	}
	
	private synchronized void stopGoldenGate() {
		System.out.println("GoldenGateDPS: stopping GoldenGATE instance");
		
		//	test if running
		if (this.distributionThread == null) {
			System.out.println("  - nothing to shut down");
			return;
		}
		
		//	shut down distribution thread
		System.out.println("  - shutting down distribution thread");
		DistributionThread dt = this.distributionThread;
		this.distributionThread = null;
		dt.shutdown();
		System.out.println("  - distribution thread shut down");
		
//		//	flush feedback queue
//		FeedbackPanel.getFeedbackService().shutdown();
//		System.out.println("  - feedback queue flushed");
//		
		//	shut down working thread pool
		synchronized (this.activeDocumentProcessingThreadList) {
			while (!this.activeDocumentProcessingThreadList.isEmpty()) {
				DocumentProcessingThread wt = ((DocumentProcessingThread) this.activeDocumentProcessingThreadList.removeFirst());
				if (wt.isWorking()) {
					FeedbackPanel.cancelFeedback(wt);
					this.activeDocumentProcessingThreadList.addLast(wt);
					try {
						System.out.println("  - waiting for automated processing thread '" + wt.getId() + "' to terminate ...");
						Thread.sleep(250);
					}
					catch (InterruptedException ie) {
						ie.printStackTrace(System.out);
					}
				}
				else {
					synchronized (wt.lock) {
						wt.lock.notify();
					}
				}
			}
		}
		
		
		//	shut down GG instance
		System.out.println("  - shutting down GoldenGATE instance");
		GoldenGATE gg = this.goldenGate;
		this.goldenGate = null;
		gg.exitShutdown();
		System.out.println("  - GoldenGATE instance shut down");
		
		//	clear data structures
		this.processPartMapper.clearProcessPartMappings();
		this.process = null;
		this.cachedDpsStatistics = null;
		System.out.println("  - markup process and mappings cleared");
	}
	
	private synchronized void setGgConfiguration(String ggConfigName) throws IOException {
		
		//	indicate configuration not found
		if (ggConfigName == null)
			throw new IOException("Invalid configuration name '" + ggConfigName + "'.");
		
		//	load GG configuration
		GoldenGateConfiguration ggConfig = ConfigurationUtils.getConfiguration(ggConfigName, ggConfigPath, ggConfigHost, this.dataPath);
		
		//	check if we got a configuration from somewhere
		if (ggConfig == null)
			throw new IOException("Configuration '" + ggConfigName + "' not found.");
		
		//	pre-charge configuration
		ggConfig.getPlugins();
		
		//	shut down activity
		this.stopGoldenGate();
		
		//	restart with new config
		this.startGoldenGate(ggConfig);
		
		//	remember new config name
		this.configuration.setSetting(ggConfigNameParameter, ggConfigName);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGateServer.AbstractGoldenGateServerComponent#exitComponent()
	 */
	protected void exitComponent() {
		this.stopGoldenGate();
		
		//	store step to status mapping
		if (this.stepsToStatesModified) try {
			File storeFile = new File(this.dataPath, "stepsToStates.cnfg");
			if (storeFile.exists()) {
				String storeFileName = storeFile.getPath();
				File oldFile = new File(storeFile.getPath() + "." + System.currentTimeMillis() + ".old");
				storeFile.renameTo(oldFile);
				storeFile = new File(storeFileName);
			}
			StringVector stepToStateLines = new StringVector();
			for (Iterator sit = this.stepsToStates.keySet().iterator(); sit.hasNext();) {
				String step = ((String) sit.next());
				String state = this.stepsToStates.getProperty(step);
				stepToStateLines.addElement(step + " " + state);
			}
			stepToStateLines.storeContent(storeFile);
		}
		catch (IOException ioe) {
			System.out.println("Error loading step to status mappings: " + ioe.getMessage());
			ioe.printStackTrace(System.out);
		}
		
		//	disconnect from database
		this.io.close();
	}
	
	/**
	 * A request to be processed by a WorkingThread, containing the qualified
	 * name of the failed criterion, the docID of the Document to be processed
	 * and the DocumentProcesser, which os suitable to do the magic
	 */
	private class DocumentProcessingRequest { 
		final String docID;
		
		DocumentProcessor dp;
		String dpFullName;
		DocumentRoot workingDoc = null;
		
		DocumentProcessingRequest(String docID) {
			this.docID = docID;
		}
	}
	
	private HashMap runningDocumentProcessors = new HashMap();
	
	/**
	 * the scheduling Thread, distributing the actual tasks to single WorkingTreads
	 */
	private class DistributionThread extends Thread {
		
		boolean isRunning = true;
		
		// RequestQueue can be made localy known to the distributionThread, since it is needed only here
		private LinkedList documentProcessingRequestQueue = new LinkedList();
		
		DistributionThread() {
			this.setPriority(MIN_PRIORITY);
		}
		
		public void run() {
			
			while (this.isRunning && serverStartup) try {
				System.out.println("DistributionThread: waiting for server startup to complete");
				sleep(1000);
			}
			catch (InterruptedException ie) {
				ie.printStackTrace(System.out);
			}
			
			try {
				sleep(2000);
			}
			catch (InterruptedException ie) {
				ie.printStackTrace(System.out);
			}
			
			System.out.println("DistributionThread: entering working loop");
			
			while (this.isRunning) {
				
				DocumentProcessingRequest apr = null;
				synchronized (this.documentProcessingRequestQueue) {
					if (this.documentProcessingRequestQueue.isEmpty()) {
						try {
							this.documentProcessingRequestQueue.wait();
						}
						catch (InterruptedException ie) {
							ie.printStackTrace(System.out);
						}
					}
					else apr = (DocumentProcessingRequest) this.documentProcessingRequestQueue.removeFirst();
				}
				
				
				/*
				 * Check checkout state. If the document is checked out by some
				 * other user, and there will be a 'Release' event before we can
				 * work on the document the next time ==> forget about APR if
				 * checkout fails, for new APR will be generated on that very
				 * release.
				 */
				if (apr != null) {
					System.out.println("DistributionThread: got APR for " + apr.docID);
					
					try {
						sleep(500);
					}
					catch (InterruptedException ie) {
						ie.printStackTrace(System.out);
					}
					
					//	get checkout user
					String checkoutUser = dio.getCheckoutUser(apr.docID);
					
					//	document deleted
					if (checkoutUser == null) {
						System.out.println("  - document does not exist");
						apr = null;
					}
					
					//	document checked out by other user, forget about request for now
					else if (!"".equals(checkoutUser) && !goldenGateDpsUser.equals(checkoutUser)) {
						System.out.println("  - document checked out by other user");
						apr = null;
					}
				}
				
				if (apr != null) {
					
					//	get pending document processor
					String dpName = getProcessorNameForDocument(apr.docID);
					if (dpName != null) {
						System.out.println("  - DP name is " + dpName);
						
						DocumentProcessor dp = goldenGate.getDocumentProcessorForName(dpName);
						if (dp != null) {
							System.out.println("  - got DP");
							
							//	check out document only if working thread available
							if (isDocumentProcessingThreadAvailable()) {
								
								// try if checkout succeeds, before actual work is delegated to workingThread
								try {
									DocumentRoot workingDoc = dio.checkoutDocument(goldenGateDpsUser, apr.docID);
									System.out.println("  - did document checkout attempt");
									if (workingDoc != null) {
										System.out.println("  - got document");
										DocumentProcessingThread dpt = getDocumentProcessingThread();
										System.out.println("  - got working thread");
										apr.dpFullName = dpName;
										apr.dp = dp;
										apr.workingDoc = workingDoc;
										dpt.processRequest(apr);
										System.out.println("  - APR handed to working thread");
									}
								}
								catch (IOException ioe) {
									ioe.printStackTrace(System.out);
									synchronized (this.documentProcessingRequestQueue) {
										this.documentProcessingRequestQueue.addLast(apr);
									}
								}
							}
							
							//	all working threads are busy, sleep some time
							else {
								synchronized (this.documentProcessingRequestQueue) {
									System.out.println("  - no working thread available");
									this.documentProcessingRequestQueue.addLast(apr);
								}
								
								try {
									synchronized (documentProcessingThreadQueue) {
										documentProcessingThreadQueue.wait();
									}
//									sleep(5000); //	TODOne wait on working thread to become available
								}
								catch (InterruptedException ie) {
									ie.printStackTrace(System.out);
								}
							}
						}
					}
				}
			}
		}
		
		// remove all APRs from requestQueue
		void cleanupDeletedDocument(String docID) {
			System.out.println("DistributionThread: cleaning up for deleted document " + docID);
			synchronized (this.documentProcessingRequestQueue) {
				System.out.println("  - start searching APR queue");
				Iterator queueIterator = this.documentProcessingRequestQueue.iterator();
				while (queueIterator.hasNext()) {
					DocumentProcessingRequest apr = ((DocumentProcessingRequest) queueIterator.next());
					if (apr.docID.equals(docID)) {
						queueIterator.remove();
						System.out.println("  - APR with processor " + apr.dp.getName() + " removed");
					}
					else System.out.println("  - APR with processor " + apr.dp.getName() + " retained");
				}
			}
		}
		
		void enqueueRequest(DocumentProcessingRequest apr) {
			synchronized (this.documentProcessingRequestQueue) {
				this.documentProcessingRequestQueue.addLast(apr);
				this.documentProcessingRequestQueue.notify();
			}
		}
		
		void cancelRequest(String docId) {
			synchronized (this.documentProcessingRequestQueue) {
				Iterator it = this.documentProcessingRequestQueue.iterator();
				while (it.hasNext()) {
					DocumentProcessingRequest apr = (DocumentProcessingRequest) it.next();
					if (apr.docID.equals(docId))
						it.remove();
				}
			}
		}
		
		
		// shutdown methods, etc.
		void shutdown() {
			this.isRunning = false;
			synchronized (this.documentProcessingRequestQueue) {
				this.documentProcessingRequestQueue.clear();
				this.documentProcessingRequestQueue.notifyAll();
			}
			synchronized (documentProcessingThreadQueue) {
				documentProcessingThreadQueue.notifyAll();
			}
		}
	}
	
	private LinkedList activeDocumentProcessingThreadList = new LinkedList();
	private LinkedList documentProcessingThreadQueue = new LinkedList();
	private int documentProcessingThreadCount = 0;
	private int documentProcessingThreadLimit = 20;

	/**
	 * a workingThread applies one DocumentProcessor to one Document.
	 * 
	 * @author Tobias
	 * 
	 */
	private class DocumentProcessingThread extends Thread {
		
		// introduce instance variable "lock" in order to solve the suspend-resume-problem
		final Object lock = new Object();
		
		private DocumentProcessingRequest request;
		
		DocumentProcessingThread() {}
		
		public void run() {
			synchronized (activeDocumentProcessingThreadList) {
				activeDocumentProcessingThreadList.add(this);
			}
			
			// WorkingThreads stay alive until distributionThread is shut down
			while ((distributionThread != null) && distributionThread.isRunning) {
				
				try {
					// has to be synchronized on lock, since getWorkingThread() accesses same lock,
					// which could led to unforeseen race conditions, when accessed in both places 
					// at same time
					synchronized (this.lock) {
						this.lock.notify();
						this.lock.wait();
					}
				}
				catch (InterruptedException ie) {
					ie.printStackTrace(System.out);
				}
				
				if (this.request != null) {
					System.out.println("WorkingThread (" + this.getName() + "): got request");
					System.out.println("  - (" + this.getName() + ") document is " + this.request.docID);
					System.out.println("  - (" + this.getName() + ") processor is " + this.request.dpFullName);
					
					// do the processing-work and release document
					try {
						// added an entry to the Log-List before and after work is done
						processorStartedOnDocument(this.request.docID, this.request.dpFullName);
						System.out.println("  - (" + this.getName() + ") start notification done");
						
						//	refresh statistics cache
						computeStatistics(this.getName());
						
						//	pre-validate document
						StepValidationResult failedStepResultBefore = getFailedStepResult(process.getValidator().validate(this.request.workingDoc));
						System.out.println("  - (" + this.getName() + ") document pre-validated");
						System.out.println("  - (" + this.getName() + ") failed step before is " + failedStepResultBefore.getValidator().getFullName());
						
						//	process document
						this.request.dp.process(this.request.workingDoc);
						System.out.println("  - (" + this.getName() + ") document processed");
						
						//	validate document and ignore errors if processor trusted
						StepValidationResult failedStepResultAfter = getFailedStepResult(failedStepResultBefore.getValidator().validate(this.request.workingDoc));
						System.out.println("  - (" + this.getName() + ") document post-validated");
						
						//	we do have a next step
						if (failedStepResultAfter != null) {
							System.out.println("  - (" + this.getName() + ") step " + failedStepResultBefore.getValidator().getFullName() + " still failed after");
							ProcessPartMapping mapping = processPartMapper.getProcessPartMapping(failedStepResultAfter.getValidator().getFullName());
							
							if (mapping == null)
								System.out.println("  - (" + this.getName() + ") no mapping available");
							
							else {
								System.out.println("  - (" + this.getName() + ") mapping is " + mapping.toString());
								
								/*
								 * we have a mapping to the same processor as the
								 * current one (==> errors remain), but this
								 * processor is trusted ==> ignore errors
								 */
								if (mapping.documentProcessorTrusted) {
									Annotation[] failed = failedStepResultAfter.getFailed();
									for (int f = 0; f < failed.length; f++)
										failedStepResultAfter.ignore(failed[f]);
									System.out.println("  - (" + this.getName() + ") ignored " + failed.length + " possible errors of trusted processor");
								}
							}
						}
						
						//	store document
						dio.updateDocument(goldenGateDpsUser, this.request.docID, this.request.workingDoc, null);
						System.out.println("  - (" + this.getName() + ") document updated");
						
						//	notify finished
						processorFinishedOnDocument(this.request.docID, this.request.dpFullName);
						System.out.println("  - (" + this.getName() + ") end notification done");
						
						//	refresh statistics cache
						computeStatistics(this.getName());
					}
					catch (Throwable t) {
						System.out.println(t.getClass().getName() + " (" + t.getMessage() + ") while processing document '" + this.request.docID + "' with processor '" + this.request.dp.getName() + "':");
						t.printStackTrace(System.out);
						
						processorErrorOnDocument(this.request.docID, t);
						
						if (distributionThread != null)
							computeStatistics(this.getName());
					}
					finally {
						// release Document under all circumstances
						dio.releaseDocument(goldenGateDpsUser, this.request.docID);
						System.out.println("  - (" + this.getName() + ") document released");
					}
					
					// clean up
					this.request = null;
				}
				
				// put it back to queue
				if ((distributionThread != null) && distributionThread.isRunning)
					synchronized (documentProcessingThreadQueue) {
						documentProcessingThreadQueue.addLast(this);
						documentProcessingThreadQueue.notify();
						System.out.println("  - (" + this.getName() + ") re-enqueued");
					}
			}
			synchronized (documentProcessingThreadQueue) {
				documentProcessingThreadCount--;
				System.out.println("WorkingThread (" + this.getName() + "): terminated");
			}
		}
		
		void processRequest(DocumentProcessingRequest apr) {
			this.request = apr;
			synchronized (this.lock) {
				this.lock.notify();
			}
		}
		
		boolean isWorking() {
			return (this.request != null);
		}
	}
	
	/**
	 * Checks if a document processing thread is available. If this method
	 * returns true, the next invocation of getDocumentProcessingThread() is
	 * guarantied not to return null. If this method returns false, the next
	 * invocation of getDocumentProcessingThread() is likely to return null, but
	 * might still return an actual thread that has become available between the
	 * invocations of the two methods.
	 * @return true if a document processing thread is available
	 */
	private boolean isDocumentProcessingThreadAvailable() {
		synchronized (this.documentProcessingThreadQueue) {
			
			//	check if a DPT is in the waiting queue or a new DPT can be created
			return ((this.documentProcessingThreadQueue.size() != 0) || (this.documentProcessingThreadCount < this.documentProcessingThreadLimit));
		}
	}
	
	/**
	 * starts and returns a document processing thread, pre-initialized with all
	 * the document- and Task-specific properties. Creates a new Thread, if
	 * there is no free one in the queue
	 * 
	 * @return a running WorkingThread
	 */
	private DocumentProcessingThread getDocumentProcessingThread() {
		
		synchronized (this.documentProcessingThreadQueue) {
			DocumentProcessingThread wt;
			if (this.documentProcessingThreadQueue.isEmpty()) {
				
				//	check limit
				if (this.documentProcessingThreadCount >= this.documentProcessingThreadLimit)
					return null;
				
				//	create new processing thread
				else {
					this.documentProcessingThreadCount++;
					wt = new DocumentProcessingThread();
					synchronized (wt.lock) {
						wt.start();
						try {
							wt.lock.wait();
						} catch (InterruptedException ie) {}
					}
				}
			}
			else wt = (DocumentProcessingThread) this.documentProcessingThreadQueue.removeFirst();
			
			// solved problem with continuing work by introducing lock to
			// synchronize workingThreads. Synchronizeation on lock of current workingThread is necessary,
			// since WorkingThread itself waits for this lock
			return wt;
		}
	}
	
	private void enqueueProcessorForDocument(String docID, String dpName) {
		
		/*
		 * write pending request to database. Since there can be only one
		 * pending request per document, first try an update query. If the
		 * latter reports "0 row(s) affected", use insert query.
		 */
		String query = "UPDATE " + PROCESSOR_HISTORY_TABLE_NAME + 
				" SET " + PROCESSOR_NAME_COLUMN_NAME + " = '" + EasyIO.sqlEscape(dpName) + "'" + 
				" WHERE " + DOCUMENT_ID_COLUMN_NAME + " LIKE '" + EasyIO.sqlEscape(docID) + "'" +
					" AND " + START_TIME_COLUMN_NAME + " = 0" +
					" AND " + FINISH_TIME_COLUMN_NAME + " = 0" +
					" AND " + ERROR_COUNTER_COLUMN_NAME + " < " + MAX_ERROR_COUNT +	
				";";
		
		try {
			int updateAffectedRows = this.io.executeUpdateQuery(query);
			if (updateAffectedRows == 0) {
				query = ("INSERT INTO " + PROCESSOR_HISTORY_TABLE_NAME + " (" + 
						DOCUMENT_ID_COLUMN_NAME + ", " + PROCESSOR_NAME_COLUMN_NAME + ", " + START_TIME_COLUMN_NAME + ", " + FINISH_TIME_COLUMN_NAME + ", " + ERROR_COUNTER_COLUMN_NAME + 
						") VALUES" + " (" +
						"'" + docID + "', '" + dpName + "', 0, 0, 0" +
						");");
				this.io.executeUpdateQuery(query);
			}
		}
		catch (SQLException sqle) {
			System.out.println("Exception storing processor assignment: " + sqle.getMessage() + "\n  query was " + query);
			sqle.printStackTrace(System.out);
		}
	}
	
	/**
	 * actually called if a document is deleted. All requests involving this
	 * document can be removed from any waiting-queue or Log-File, since even
	 * when System crashes, the information for this document will not be needed
	 * for recovery
	 * 
	 * @param The ID of the deleted document
	 */
	private void cleanupDeletedDocument(String docID) {
		this.distributionThread.cleanupDeletedDocument(docID);
		
		// clean up database
		String deleteQuery = "DELETE FROM " + PROCESSOR_HISTORY_TABLE_NAME + 
			" WHERE " + DOCUMENT_ID_COLUMN_NAME + " LIKE '" + EasyIO.sqlEscape(docID) + "'" +
			";";
		
		try {
			this.io.executeUpdateQuery(deleteQuery);
		}
		catch (SQLException sqle) {
			System.out.println("Exception removing processor assignment for deleted document with ID " + docID + ": " + sqle.getMessage() + "\n  query was " + deleteQuery);
			sqle.printStackTrace(System.out);
		}
	}
	
	private String getProcessorNameForDocument(String docID) {
		
		// if Document is released, get the name of the processor scheduled to work on the document next

		// get pending request from database
		String query = "SELECT " + PROCESSOR_NAME_COLUMN_NAME + 
			" FROM " + PROCESSOR_HISTORY_TABLE_NAME + 
			" WHERE " + DOCUMENT_ID_COLUMN_NAME + " LIKE '" + EasyIO.sqlEscape(docID) + "'" +
				" AND " + START_TIME_COLUMN_NAME + " = 0" +
				" AND " + ERROR_COUNTER_COLUMN_NAME + " < " + MAX_ERROR_COUNT +	
			";";
		
		SqlQueryResult sqr = null;
		String dpAssignedForDocument = null;
		try {
			sqr = this.io.executeSelectQuery(query);
			if (sqr.next())
				dpAssignedForDocument = sqr.getString(0);
		}
		catch (SQLException sqle) {
			System.out.println("Exception finding assigned processor for document with ID " + docID + ": " + sqle.getMessage() + "\n  query was " + query);
			sqle.printStackTrace(System.out);
		}
		finally {
			if (sqr != null)
				sqr.close();
		}
		return dpAssignedForDocument;
	}
	
	private void processorStartedOnDocument(String docID, String dpName) {
		this.runningDocumentProcessors.put(docID, dpName);
		
		// write start time to database. Since DB-Entry is created on update (with starttime =
		// finishtime = 0), only updating existing DB-Entry
		String query = ("UPDATE " + PROCESSOR_HISTORY_TABLE_NAME + 
				" SET " + START_TIME_COLUMN_NAME + " = " + System.currentTimeMillis() + 
				" WHERE " + DOCUMENT_ID_COLUMN_NAME + " LIKE '" + EasyIO.sqlEscape(docID) + "'" +
					" AND " + START_TIME_COLUMN_NAME + " = 0" +
					" AND " + ERROR_COUNTER_COLUMN_NAME + " < " + MAX_ERROR_COUNT + 
				";"); 
		try {
			this.io.executeUpdateQuery(query);
		}
		catch (SQLException sqle) {
			System.out.println("Exception storing processor assignment: " + sqle.getMessage() + "\n  query was " + query);
			sqle.printStackTrace(System.out);
		}
	}
	
//	private boolean isProcessorRunningOnDocument(String docId) {
//		return this.runningDocumentProcessors.containsKey(docId);
//	}
//	
	private boolean isProcessorRunningOnDocument(String docId, String dpName) {
		return dpName.equals(this.runningDocumentProcessors.get(docId));
	}
	
	private void processorFinishedOnDocument(String docID, String dpName) {
		
		// write finish time to database
		String query = ("UPDATE " + PROCESSOR_HISTORY_TABLE_NAME + 
				" SET " + FINISH_TIME_COLUMN_NAME + " = " + System.currentTimeMillis() + 
				" WHERE " + DOCUMENT_ID_COLUMN_NAME + " LIKE '" + EasyIO.sqlEscape(docID) + "' " +
					" AND " + PROCESSOR_NAME_COLUMN_NAME + " LIKE '" + EasyIO.sqlEscape(dpName) + "'" +
					" AND " + ERROR_COUNTER_COLUMN_NAME + " < " + MAX_ERROR_COUNT + 
				";"); 
		try {
			this.io.executeUpdateQuery(query);
		} 
		catch (SQLException sqle) {
			System.out.println("Exception marking processor assignment as finished: " + sqle.getMessage() + "\n  query was " + query);
			sqle.printStackTrace(System.out);
		}
		
		this.runningDocumentProcessors.remove(docID);
	}
	
	private void processorErrorOnDocument(String docID, Throwable t) {
		this.runningDocumentProcessors.remove(docID);
		
		//	log error in database
		String errorQuery;
		
		//	exception originates from shutdow flush of feedback queue
		if ((this.distributionThread == null) && (t instanceof RuntimeException)) {
			
			// error during processing, caused by shutdown flushing of feedback queue
			errorQuery = "UPDATE " + PROCESSOR_HISTORY_TABLE_NAME + 
				" SET " + 
					ERROR_MESSAGE_COLUMN_NAME + " = '" + EasyIO.sqlEscape(t.getMessage()) + "', " +
					ERROR_TYPE_COLUMN_NAME + " = '" + t.getClass().getName() + "' " +  
				" WHERE " + DOCUMENT_ID_COLUMN_NAME + " LIKE '" + docID + "'" +
					" AND " + FINISH_TIME_COLUMN_NAME + " = 0" +
				";";
			
		}
		
		//	other exception
		else {
			
			// error during processing, set starting time back to 0 to get another try.
			// additionally, increment errorCounter and store error class and error message
			errorQuery = "UPDATE " + PROCESSOR_HISTORY_TABLE_NAME + 
				" SET " +
					START_TIME_COLUMN_NAME + " = 0" + ", " + 
					ERROR_COUNTER_COLUMN_NAME + " = " + ERROR_COUNTER_COLUMN_NAME + " + 1" + ", " +
					ERROR_MESSAGE_COLUMN_NAME + " = '" + EasyIO.sqlEscape(t.getMessage()) + "', " +
					ERROR_TYPE_COLUMN_NAME + " = '" + t.getClass().getName() + "' " +  
				" WHERE " + DOCUMENT_ID_COLUMN_NAME + " LIKE '" + EasyIO.sqlEscape(docID) + "'" +
					" AND " + FINISH_TIME_COLUMN_NAME + " = 0" +
//					" AND " + ERROR_COUNTER_COLUMN_NAME + " <= " + MAX_ERROR_COUNT +
				";";
			
		}
		try {
			this.io.executeUpdateQuery(errorQuery);
		}
		catch (SQLException sqle) {
			System.out.println("Exception setting starttime to 0 in case of error: " + sqle.getMessage() + "\n  query was " + errorQuery);
			sqle.printStackTrace(System.out);
		}
		/*
		 * Erroneous requests: re-enqueue them in distributionThread.aprQueue or
		 * not, but I think this is not necessary, since after the query above
		 * is executed, there will definitely be a document-release-event, which
		 * will lead to the DPS trying to enqueue the same processor to this
		 * document again. The query executed on release has been adapted in
		 * order to return only rows with errorCount < MaxErrorCount, so if this
		 * error was the maximumError of the current DP, the DP which is being
		 * enqueued is null, and since null is checked and forbidden as DP,
		 * there will no APR be created. Therefor, the concerning document is
		 * out of the system, till another human treatment has taken place.
		 */
	}
	
	/**
	 * returns a HashSet which contains the names of all Processors applied to the specified document since
	 * the last treatment by a human user
	 * 
	 * @param docID the ID of the document to load history for
	 * @return HashSet with names of Processors since last human treatment
	 */
	public HashSet loadDocumentHistory(String docID) {
		
		// get all processors applied to this document, ordered by startingtime
		String query = "SELECT " + PROCESSOR_NAME_COLUMN_NAME + 
			" FROM " + PROCESSOR_HISTORY_TABLE_NAME + 
			" WHERE " + DOCUMENT_ID_COLUMN_NAME + " LIKE '" + EasyIO.sqlEscape(docID) + "'" + 
			" ORDER BY " + START_TIME_COLUMN_NAME + " DESC" +
			";";
		/*
		 * if in max-error-case, document is taken out of every queue,
		 * check for maxError is unnecessary, since all max-error-requests
		 * have an earlier timestamp then last human user update
		 */
		
		SqlQueryResult result = null;
		HashSet history = new HashSet();
		try {
			result = this.io.executeSelectQuery(query);
			
			// find all documentProcessors applied since another user had his fingers on that document
			// (since these are exactly those to be kept in history)
			String dpNameForHistory = "";
			while (result.next() && ((dpNameForHistory = result.getString(0)) != null) && !MANUAL_EDIT_DP_NAME.equals(dpNameForHistory))
				history.add(dpNameForHistory);
//			while (result.next() && (dpNameForHistory != null) && !MANUAL_EDIT_DP_NAME.equals(dpNameForHistory)) {
//				dpNameForHistory = result.getString(0);
//				if ((dpNameForHistory != null) && !MANUAL_EDIT_DP_NAME.equals(dpNameForHistory))
//					history.add(dpNameForHistory);
//			}
		}
		catch (SQLException sqle) {
			System.out.println("Exception getting all Processors for Document " + docID + ": " + sqle.getMessage());
			System.out.println("  query was " + query);
		}
		finally {
			if (result != null)
				result.close();
		}
		
		return history;
	}
//	
//	public static void main(String[] args) throws Exception {
//		InputStream is = new FileInputStream(new File("C:/temp/players.xml"));
//		TreeNode pRoot = (new Parser()).parse(is);
//		if (TreeNode.ROOT_NODE_TYPE.equals(pRoot.getNodeType()))
//			pRoot = pRoot.getChildNode(Process.PROCESS, 0);
//		Process process = new Process(pRoot);
//		
//		MutableAnnotation doc = SgmlDocumentReader.readDocument(new InputStreamReader(new FileInputStream("E:/Projektdaten/TaxonxTest/20286_gg2.xml"), "UTF-8"));
//
//		ValidationResult vr = process.validate(doc);
//		ProcessPartMapper.qualifyValidationResult(vr);
//
//		StringWriter sw = new StringWriter();
//		vr.writeXml(sw);
//
//		TreeNode vrRoot = parser.parse(new StringReader(sw.toString()));
//		if (TreeNode.ROOT_NODE_TYPE.equals(vrRoot.getNodeType()))
//			vrRoot = vrRoot.getChildNode(Process.PROCESS, 0);
//
//		ValidationResult loadedVr = process.loadValidationResult(vrRoot);
//		ProcessPartMapper.qualifyValidationResult(loadedVr);
//	}
}
