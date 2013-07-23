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

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.JTextField;

import de.uka.ipd.idaho.easyIO.settings.Settings;
import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationListener;
import de.uka.ipd.idaho.gamta.CharSequenceListener;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.MutableCharSequence.CharSequenceEvent;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.util.validation.Criterion;
import de.uka.ipd.idaho.gamta.util.validation.Level;
import de.uka.ipd.idaho.gamta.util.validation.Process;
import de.uka.ipd.idaho.gamta.util.validation.Step;
import de.uka.ipd.idaho.gamta.util.validation.Step.StepValidationResult;
import de.uka.ipd.idaho.gamta.util.validation.Task;
import de.uka.ipd.idaho.gamta.util.validation.ValidationResult;
import de.uka.ipd.idaho.gamta.util.validation.ValidationResultPanel;
import de.uka.ipd.idaho.gamta.util.validation.Validator;
import de.uka.ipd.idaho.gamta.util.validation.editor.MarkupProcessEditorExtension;
import de.uka.ipd.idaho.gamta.util.validation.editor.MarkupProcessPartEditor;
import de.uka.ipd.idaho.gamta.util.validation.editor.MarkupProcessPartEditorExtension;
import de.uka.ipd.idaho.gamta.util.validation.editor.MarkupStepEditor;
import de.uka.ipd.idaho.goldenGate.DocumentEditor;
import de.uka.ipd.idaho.goldenGate.DocumentEditorDialog;
import de.uka.ipd.idaho.goldenGate.markupProcess.MarkupProcessManager;
import de.uka.ipd.idaho.goldenGate.markupProcess.MarkupProcessManager.ExtensibleMarkupProcessEditor;
import de.uka.ipd.idaho.goldenGate.markupWizard.MarkupWizard;
import de.uka.ipd.idaho.goldenGate.markupWizard.ProcessPartMapper;
import de.uka.ipd.idaho.goldenGate.markupWizard.ProcessPartMapper.ProcessPartMapping;
import de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentProcessorManager;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessor;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessorManager;
import de.uka.ipd.idaho.goldenGate.plugins.GoldenGatePlugin;
import de.uka.ipd.idaho.goldenGate.plugins.ResourceManager;
import de.uka.ipd.idaho.goldenGate.plugins.ResourceSplashScreen;
import de.uka.ipd.idaho.goldenGate.util.AttributeEditorDialog;
import de.uka.ipd.idaho.goldenGate.util.DataListListener;
import de.uka.ipd.idaho.goldenGate.util.DialogPanel;
import de.uka.ipd.idaho.goldenGate.util.ResourceDialog;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * Manager for markup wizard resources inside the GoldenGATE Editor. This plugin
 * can also install a given markup wizard to the GoldenGATE root folder so to
 * facilitate running GoldenGATE Markup Wizard as an application.
 * 
 * @author sautter
 */
public class MarkupWizardManager extends AbstractDocumentProcessorManager {
	
	private static final String FILE_EXTENSION = ".markupWizard";
	
	private MarkupProcessManager mpManager;
	private GoldenGatePlugin mwConfigManager;
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#init()
	 */
	public void init() {
		this.mwConfigManager = this.parent.getPlugin(MarkupWizardConfigurationManager.class.getName());
		this.mpManager = ((MarkupProcessManager) this.parent.getPlugin(MarkupProcessManager.class.getName()));
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#isOperational()
	 */
	public boolean isOperational() {
		return (super.isOperational() && (this.mpManager != null) && this.mpManager.isOperational());
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getFileExtension()
	 */
	protected String getFileExtension() {
		return FILE_EXTENSION;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.ResourceManager#getResourceTypeLabel()
	 */
	public String getResourceTypeLabel() {
		return "Markup Wizard";
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentProcessorManager#getMainMenuItems()
	 */
	public JMenuItem[] getMainMenuItems() {
		if (!this.dataProvider.isDataEditable())
			return new JMenuItem[0];
		
		ArrayList collector = new ArrayList();
		JMenuItem mi;
		
		mi = new JMenuItem("Create");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				createMarkupWizard();
			}
		});
		collector.add(mi);
		mi = new JMenuItem("Edit");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				editMarkupWizards();
			}
		});
		collector.add(mi);
		return ((JMenuItem[]) collector.toArray(new JMenuItem[collector.size()]));
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#getMainMenuTitle()
	 */
	public String getMainMenuTitle() {
		return "Markup Wizards";
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getToolsMenuLabel()
	 */
	public String getToolsMenuLabel() {
		return "Apply";
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getDataNamesForResource(java.lang.String)
	 */
	public String[] getDataNamesForResource(String name) {
		StringVector names = new StringVector();
		names.addContentIgnoreDuplicates(super.getDataNamesForResource(name));
		names.addElementIgnoreDuplicates(name + ".export@" + this.getClass().getName());
		
		MarkupWizardStub mws = this.loadMarkupWizard(name);
		if (mws == null)
			return names.toStringArray();
		
		names.addContentIgnoreDuplicates(this.mpManager.getDataNamesForResource(mws.processName));
		
		ProcessPartMapping[] mappings = mws.mapper.getProcessPartMappings();
		for (int m = 0; m < mappings.length; m++) {
			if (mappings[m].documentProcessorName != null) {
				DocumentProcessor dp = this.parent.getDocumentProcessorForName(mappings[m].documentProcessorName);
				if (dp != null) {
					DocumentProcessorManager dpm = this.parent.getDocumentProcessorProvider(dp.getProviderClassName());
					if (dpm != null) names.addContentIgnoreDuplicates(dpm.getDataNamesForResource(dp.getName()));
				}
			}
		}
		
		return names.toStringArray();
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.ResourceManager#getRequiredResourceNames(java.lang.String, boolean)
	 */
	public String[] getRequiredResourceNames(String name, boolean recourse) {
		MarkupWizardStub mws = this.loadMarkupWizard(name);
		if (mws == null) return new String[0];
		
		StringVector nameCollector = new StringVector();
		
		nameCollector.addElementIgnoreDuplicates(mws.processName + "@" + this.mpManager.getClass().getName());
		
		ProcessPartMapping[] mappings = mws.mapper.getProcessPartMappings();
		for (int m = 0; m < mappings.length; m++) {
			if (mappings[m].documentProcessorName != null)
				nameCollector.addElementIgnoreDuplicates(mappings[m].documentProcessorName);
		}
		
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
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessorManager#getDocumentProcessor(java.lang.String)
	 */
	public DocumentProcessor getDocumentProcessor(String name) {
		MarkupWizard mw = this.getMarkupWizard(name);
		return ((mw == null) ? null : new MarkupWizardDocumentProcessor(name, null, mw));
	}
	
	/**
	 * Retrieve a markup wizard (consisting of a markup process definition and a
	 * mapping of individual parts of that very markup process to specific
	 * document processors).
	 * @param name the name of the markup wizard
	 * @return the markup wizard with the specified name, or null, if there is
	 *         no such markup wizard
	 */
	public MarkupWizard getMarkupWizard(String name) {
		if (name == null)
			return null;
		MarkupWizardStub mws = this.loadMarkupWizard(name);
		return ((mws == null) ? null : mws.toExternalForm());
	}
	
	private MarkupWizardStub loadMarkupWizard(String name) {
		StringVector mappings = this.loadListResource(name);
		if (mappings == null)
			return null;
		
		ProcessPartMapper mapper = new ProcessPartMapper(mappings.toStringArray());
		ProcessPartMapping mpMapping = mapper.getProcessPartMapping("MarkupProcessName");
		if (mpMapping == null)
			return null;
		
		mapper.setProcessPartMapping("MarkupProcessName", null);
		return new MarkupWizardStub(name, mpMapping.documentProcessorName, mapper);
	}
	
	private boolean storeMarkupWizard(String name, MarkupWizardStub mws) throws IOException {
		StringVector mappings = new StringVector();
		ProcessPartMapping[] dpMappings = mws.mapper.getProcessPartMappings();
		for (int m = 0; m < dpMappings.length; m++)
			mappings.addElementIgnoreDuplicates(dpMappings[m].toString());
		
		ProcessPartMapping mpMapping = new ProcessPartMapping("MarkupProcessName", mws.processName);
		mappings.addElementIgnoreDuplicates(mpMapping.toString());
		
		return this.storeListResource(name, mappings);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractDocumentProcessorManager#applyDocumentProcessor(java.lang.String, de.uka.ipd.idaho.goldenGate.DocumentEditor, java.util.Properties)
	 */
	public void applyDocumentProcessor(String processorName, DocumentEditor data, Properties parameters) {
		String pn = processorName;
		
		//	select processor if not specified
		if (pn == null) {
			ResourceDialog rd = ResourceDialog.getResourceDialog(this, (this.getToolsMenuLabel() + " " + this.getResourceTypeLabel()), this.getToolsMenuLabel());
			rd.setLocationRelativeTo(DialogPanel.getTopWindow());
			rd.setVisible(true);
			if (rd.isCommitted()) pn = rd.getSelectedResourceName();
		}
		
		//	get pipeline
		MarkupWizard markupWizard = this.getMarkupWizard(pn);
		if (markupWizard != null) {
			
			//	apply processor
			ResourceSplashScreen splashScreen = new ResourceSplashScreen((this.getResourceTypeLabel() + " Running ..."), ("Please wait while '" + this.getResourceTypeLabel() + ": " + pn + "' is processing the Document ..."));
			DocumentProcessor dp = new MarkupWizardDocumentProcessor(pn, splashScreen, markupWizard);
			data.applyDocumentProcessor(dp, splashScreen, parameters);
		}
	}
	
	private class MarkupWizardDocumentProcessor implements DocumentProcessor {
		private String name;
		private ResourceSplashScreen splashScreen;
		private Validator validator;
		private MarkupWizard markupWizard;
		
		MarkupWizardDocumentProcessor(String name, ResourceSplashScreen splashScreen, MarkupWizard markupWizard) {
			this.name = name;
			this.splashScreen = splashScreen;
			this.markupWizard = markupWizard;
			this.validator = this.markupWizard.process.getValidator();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.Resource#getName()
		 */
		public String getName() {
			return this.name;
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.Resource#getProviderClassName()
		 */
		public String getProviderClassName() {
			return MarkupWizardManager.class.getName();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.Resource#getTypeLabel()
		 */
		public String getTypeLabel() {
			return getResourceTypeLabel();
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessor#process(de.uka.ipd.idaho.gamta.MutableAnnotation)
		 */
		public void process(MutableAnnotation data) {
			Properties parameters = new Properties();
			parameters.setProperty(INTERACTIVE_PARAMETER, INTERACTIVE_PARAMETER);
			if (dataProvider.allowWebAccess())
				parameters.setProperty(ONLINE_PARAMETER, ONLINE_PARAMETER);
			this.process(data, parameters);
		}
		
		/* (non-Javadoc)
		 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessor#process(de.uka.ipd.idaho.gamta.MutableAnnotation, java.util.Properties)
		 */
		public void process(MutableAnnotation data, Properties parameters) {
			Set dpHistory = new HashSet(); // remember what's already done in order to avoid loops
			while (true) {
				
				//	get document status
				ValidationResult vr = this.validator.validate(data);
				StepValidationResult failedStepResult = this.getFailedStepResult(vr);
				
				//	we're done
				if (failedStepResult == null) {
					if (parameters.containsKey(INTERACTIVE_PARAMETER))
						JOptionPane.showMessageDialog(DialogPanel.getTopWindow(), ("Markup Wizard " + this.validator.getLabel() + " completed successfully."), "Markup Wizard Completed", JOptionPane.INFORMATION_MESSAGE);
					return;
				}
				System.out.println("MarkupWizard '" + this.name + "': failed step is " + failedStepResult.getValidator().getFullName());
				
				//	find next processor
				ProcessPartMapping mapping = this.markupWizard.mapper.getProcessPartMapping(failedStepResult.getValidator().getFullName());
				String dpName = ((mapping == null) ? null : mapping.documentProcessorName);
				
				System.out.println("MarkupWizard '" + this.name + "': document processor is " + dpName);
				
				//	we don't have a processor to proceed with, or current processor has already been applied
				if ((dpName == null) || dpHistory.contains(dpName)) {
					
					//	show errors for manual correction (if allowed to)
					if (parameters.containsKey(INTERACTIVE_PARAMETER) && this.correctManually(data, failedStepResult))
						
						//	start over if manual correction commited
						dpHistory.clear();
					
					//	stop if manual correction impossible or cancelled
					else return;
				}
				
				//	get next processor
				else {
					DocumentProcessor dp = parent.getDocumentProcessorForName(dpName);
					
					//	invalid processor name
					if (dp == null) return;
					
					if (this.splashScreen != null)
						this.splashScreen.setText("Current Document Processor is " + dp.getName());
					
					//	apply next processor, and remember it
					dp.process(data, parameters);
					dpHistory.add(dpName);
				}
			}
		}
		
		private StepValidationResult getFailedStepResult(ValidationResult pvr) {
			ValidationResult[] levelResults = pvr.getPartialResults();
			for (int l = 0; l < levelResults.length; l++) {
				if (levelResults[l].isPassed()) continue;
				
				ValidationResult[] taskResults = levelResults[l].getPartialResults();
				for (int t = 0; t < taskResults.length; t++) {
					if (taskResults[t].isPassed()) continue;
					
					else {
						ValidationResult[] stepResults = taskResults[t].getPartialResults();
						for (int s = 0; s < stepResults.length; s++) {
							if (!stepResults[s].isPassed())
								return ((StepValidationResult) stepResults[s]);
						}
					}
				}
			}
			
			return null;
		}
		
		private boolean correctManually(MutableAnnotation data, StepValidationResult failedStepResult) {
			
			//	show result (use step result only if more than one criterion failed)
			ManualCorrectionDialog mcd = new ManualCorrectionDialog(data, failedStepResult);
			mcd.setVisible(true);
			
			//	check for modifications
			return mcd.isModified();
		}
		
		private class ManualCorrectionDialog extends DialogPanel implements CharSequenceListener, AnnotationListener {
			private MwDpValidationResultPanel resultPanel;
			
			private MutableAnnotation data;
			private boolean isModified = false;
			
			ManualCorrectionDialog(MutableAnnotation data, ValidationResult failedStepResult) {
				super(failedStepResult.getErrorDescription(), true);
				this.data = data;
				
				//	listen for changes
				this.data.addCharSequenceListener(this);
				this.data.addAnnotationListener(this);
				
				JLabel explanation = new JLabel(("<HTML>Some annotations failed validation for step '" + failedStepResult.getValidator().getLabel() + "',<BR>please correct them manually so Markup Wizard can continue.</HTML>"), JLabel.LEFT);
				explanation.setBorder(BorderFactory.createLineBorder(explanation.getBackground(), 4));
				this.add(explanation, BorderLayout.NORTH);
				
				//	add actual result display
				this.resultPanel = new MwDpValidationResultPanel(data, failedStepResult.getValidator());
				this.add(this.resultPanel, BorderLayout.CENTER);
				
				//	add button
				JButton okButton = new JButton("OK");
				okButton.setBorder(BorderFactory.createRaisedBevelBorder());
				okButton.setPreferredSize(new Dimension(100, 21));
				okButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						dispose();
					}
				});
				this.add(okButton, BorderLayout.SOUTH);
				
				//	configure window
				this.setResizable(true);
				this.setSize(500, 600);
				this.setLocationRelativeTo(getTopWindow());
			}
			
			public void dispose() {
				this.data.removeCharSequenceListener(this);
				this.data.removeAnnotationListener(this);
				super.dispose();
			}
			
			boolean isModified() {
				return this.isModified;
			}
			
			public void charSequenceChanged(CharSequenceEvent change) {
				this.isModified = true;
			}
			
			public void annotationAdded(QueriableAnnotation doc, Annotation annotation) {
				this.isModified = true;
			}
			public void annotationAttributeChanged(QueriableAnnotation doc, Annotation annotation, String attributeName, Object oldValue) {
				this.isModified = true;
			}
			public void annotationRemoved(QueriableAnnotation doc, Annotation annotation) {
				this.isModified = true;
			}
			public void annotationTypeChanged(QueriableAnnotation doc, Annotation annotation, String oldType) {
				this.isModified = true;
			}
		}
		
		private class MwDpValidationResultPanel extends ValidationResultPanel {
			
			private MutableAnnotation data;
			
			private Dimension editDialogSize = new Dimension(800, 600);
			private Point editDialogLocation = null;
			
			private String[] taggedTypes = {};
			private String[] highlightTypes = {};
			
			private Dimension attributeDialogSize = new Dimension(400, 300);
			private Point attributeDialogLocation = null;
			
			MwDpValidationResultPanel(MutableAnnotation data, Validator validator) {
				super(data, validator);
				this.data = data;
			}
			
			/* (non-Javadoc)
			 * @see de.uka.ipd.idaho.gamta.util.validation.ValidationResultPanel#getContextMenuItems(de.uka.ipd.idaho.gamta.util.validation.ValidationResultPanel.AnnotationTray[])
			 */
			protected JMenuItem[] getContextMenuItems(final AnnotationTray[] selectedTrays) {
				ArrayList collector = new ArrayList();
				JMenuItem mi;
				
				int covered = selectedTrays[0].annotation.size();
				int uncovered = 0;
				for (int t = 1; t < selectedTrays.length; t++) {
					uncovered += (selectedTrays[t].annotation.getStartIndex() - selectedTrays[t-1].annotation.getEndIndex());
					covered += selectedTrays[t].annotation.size();
				}
				
				if ((covered > uncovered) || (selectedTrays.length <= 5)) {
					mi = new JMenuItem("Edit");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							editAnnotations(selectedTrays);
						}
					});
					collector.add(mi);
				}
				
				if (selectedTrays.length == 1) {
					mi = new JMenuItem("Edit Attributes");
					mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							editAnnotationAttributes(selectedTrays[0].annotation);
						}
					});
					collector.add(mi);
				}
				
				return ((JMenuItem[]) collector.toArray(new JMenuItem[collector.size()]));
			}
			
			/* (non-Javadoc)
			 * @see de.uka.ipd.idaho.gamta.util.validation.ValidationResultPanel#reactOnDoubleClick(de.uka.ipd.idaho.gamta.util.validation.ValidationResultPanel.AnnotationTray)
			 */
			protected boolean reactOnDoubleClick(AnnotationTray clickedTray) {
				AnnotationTray[] clickedTrays = {clickedTray};
				return this.editAnnotations(clickedTrays);
			}
			
			private boolean editAnnotations(AnnotationTray[] annotationTrays) {
				if (annotationTrays.length == 0) return false;
				
				MutableAnnotation content;
				
				if (annotationTrays.length == 1)
					content = annotationTrays[0].annotation;
				
				else content = data.addAnnotation(TEMP_ANNOTATION_TYPE, annotationTrays[0].annotation.getStartIndex(), (annotationTrays[annotationTrays.length - 1].annotation.getEndIndex() - annotationTrays[0].annotation.getStartIndex()));
				
				//	create dialog & show
				DocumentEditDialog ded = new DocumentEditDialog("Edit Annotation", content);
				ded.setVisible(true);
				
				//	remove temp annotation
				if (TEMP_ANNOTATION_TYPE.equals(content.getType()))
					data.removeAnnotation(content);
				
				//	determine modifications
				boolean modified = ded.modified;
				
				//	ignore
				if (ded.ignore) {
					for (int i = 0; i < annotationTrays.length; i++)
						annotationTrays[i].ignore();
					modified = true;
				}
				
				//	remove
				else if (ded.remove) {
					for (int r = 0; r < annotationTrays.length; r++)
						annotationTrays[r].remove();
					modified = true;
				}
				
				//	delete
				else if (ded.delete) {
					for (int d = 0; d < annotationTrays.length; d++)
						annotationTrays[d].delete();
					modified = true;
				}
				
				//	indicate changes (if any)
				return modified;
			}
			
			private class DocumentEditDialog extends DocumentEditorDialog {
				boolean modified = false;
				boolean ignore = false;
				boolean remove = false;
				boolean delete = false;
				
				DocumentEditDialog(String title, MutableAnnotation content) {
					super(MarkupWizardManager.this.parent, title, content);
					
					JButton okButton = new JButton("OK / Edit");
					okButton.setBorder(BorderFactory.createRaisedBevelBorder());
					okButton.setPreferredSize(new Dimension(80, 21));
					okButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							DocumentEditDialog.this.writeChanges();
							DocumentEditDialog.this.modified = DocumentEditDialog.this.isContentModified();
							DocumentEditDialog.this.dispose();
						}
					});
					this.mainButtonPanel.add(okButton);
					
					//	ignore error annotations
					JButton ignoreButton = new JButton("Accept As Is");
					ignoreButton.setBorder(BorderFactory.createRaisedBevelBorder());
					ignoreButton.setPreferredSize(new Dimension(80, 21));
					ignoreButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							DocumentEditDialog.this.writeChanges();
							DocumentEditDialog.this.ignore = true;
							DocumentEditDialog.this.dispose();
						}
					});
					this.mainButtonPanel.add(ignoreButton);
					
					//	remove error annotations
					JButton removeButton = new JButton("Remove");
					removeButton.setBorder(BorderFactory.createRaisedBevelBorder());
					removeButton.setPreferredSize(new Dimension(80, 21));
					removeButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							DocumentEditDialog.this.remove = true;
							DocumentEditDialog.this.dispose();
						}
					});
					this.mainButtonPanel.add(removeButton);
					
					//	remove error annotations
					JButton deleteButton = new JButton("Delete");
					deleteButton.setBorder(BorderFactory.createRaisedBevelBorder());
					deleteButton.setPreferredSize(new Dimension(80, 21));
					deleteButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							DocumentEditDialog.this.delete = true;
							DocumentEditDialog.this.dispose();
						}
					});
					this.mainButtonPanel.add(deleteButton);
					
					JButton cancelButton = new JButton("Cancel");
					cancelButton.setBorder(BorderFactory.createRaisedBevelBorder());
					cancelButton.setPreferredSize(new Dimension(80, 21));
					cancelButton.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							DocumentEditDialog.this.dispose();
						}
					});
					this.mainButtonPanel.add(cancelButton);
					
					for (int t = 0; t < taggedTypes.length; t++)
						this.documentEditor.setAnnotationTagVisible(taggedTypes[t], true);
					for (int t = 0; t < highlightTypes.length; t++)
						this.documentEditor.setAnnotationValueHighlightVisible(highlightTypes[t], true);
					
					this.setSize(editDialogSize);
					if (editDialogLocation == null) this.setLocationRelativeTo(this.getOwner());
					else this.setLocation(editDialogLocation);
				}
				
				public void dispose() {
					editDialogSize = this.getSize();
					editDialogLocation = this.getLocation(editDialogLocation);
					taggedTypes = this.documentEditor.getTaggedAnnotationTypes();
					highlightTypes = this.documentEditor.getHighlightAnnotationTypes();
					
					super.dispose();
				}
			}
			
			private boolean editAnnotationAttributes(MutableAnnotation annotation) {
				
				//	create dialog
				AttributeEditorDialog aed = new AttributeEditorDialog("Edit Annotation Attributes", annotation, data) {
					public void dispose() {
						attributeDialogSize = this.getSize();
						attributeDialogLocation = this.getLocation(attributeDialogLocation);
						super.dispose();
					}
				};
				
				//	position and show dialog
				aed.setSize(attributeDialogSize);
				if (attributeDialogLocation == null) aed.setLocationRelativeTo(DialogPanel.getTopWindow());
				else aed.setLocation(attributeDialogLocation);
				aed.setVisible(true);
				
				return aed.isDirty();
			}
		}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessorManager#createDocumentProcessor()
	 */
	public String createDocumentProcessor() {
		return this.createMarkupWizard(null, null);
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessorManager#editDocumentProcessor(java.lang.String)
	 */
	public void editDocumentProcessor(String name) {
		MarkupWizardStub mws = this.loadMarkupWizard(name);
		if (mws == null) return;
		
		EditMarkupWizardDialog epd = new EditMarkupWizardDialog(mws, name);
		epd.setVisible(true);
		
		if (epd.isCommitted()) try {
			this.storeMarkupWizard(name, epd.getMarkupWizard());
		} catch (IOException ioe) {}
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessorManager#editDocumentProcessors()
	 */
	public void editDocumentProcessors() {
		this.editMarkupWizards();
	}
	
	private boolean createMarkupWizard() {
		return (this.createMarkupWizard(null, null) != null);
	}
	
	private boolean cloneMarkupWizard() {
		String selectedName = this.resourceNameList.getSelectedName();
		if (selectedName == null)
			return this.createMarkupWizard();
		else {
			String name = "New " + selectedName;
			return (this.createMarkupWizard(this.loadMarkupWizard(selectedName), name) != null);
		}
	}
	
	private String createMarkupWizard(MarkupWizardStub modelMarkupWizard, String name) {
		CreateMarkupWizardDialog cpd = new CreateMarkupWizardDialog(modelMarkupWizard, name);
		cpd.setVisible(true);
		
		if (cpd.isCommitted()) {
			MarkupWizardStub mws = cpd.getMarkupWizard();
			String markupWizardName = cpd.getMarkupWizardName();
			if (!markupWizardName.endsWith(FILE_EXTENSION)) markupWizardName += FILE_EXTENSION;
			try {
				if (this.storeMarkupWizard(markupWizardName, mws)) {
					this.resourceNameList.refresh();
					return markupWizardName;
				}
			} catch (IOException e) {}
		}
		return null;
	}
	
	private void editMarkupWizards() {
		final MarkupWizardEditorPanel[] editor = new MarkupWizardEditorPanel[1];
		editor[0] = null;
		
		final DialogPanel editDialog = new DialogPanel("Edit Markup Wizards", true);
		editDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		editDialog.addWindowListener(new WindowAdapter() {
			public void windowClosed(WindowEvent we) {
				this.closeDialog();
			}
			public void windowClosing(WindowEvent we) {
				this.closeDialog();
			}
			private void closeDialog() {
				if ((editor[0] != null) && editor[0].isDirty()) {
					try {
						storeMarkupWizard(editor[0].name, editor[0].getMarkupWizard());
					} catch (IOException ioe) {}
				}
				if (editDialog.isVisible()) editDialog.dispose();
			}
		});
		
		editDialog.getContentPane().setLayout(new BorderLayout());
		
		JPanel editButtons = new JPanel(new FlowLayout(FlowLayout.CENTER));
		JButton button;
		button = new JButton("Create");
		button.setBorder(BorderFactory.createRaisedBevelBorder());
		button.setPreferredSize(new Dimension(100, 21));
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				createMarkupWizard();
			}
		});
		editButtons.add(button);
		button = new JButton("Clone");
		button.setBorder(BorderFactory.createRaisedBevelBorder());
		button.setPreferredSize(new Dimension(100, 21));
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				cloneMarkupWizard();
			}
		});
		editButtons.add(button);
		button = new JButton("Delete");
		button.setBorder(BorderFactory.createRaisedBevelBorder());
		button.setPreferredSize(new Dimension(100, 21));
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				if (deleteResource(resourceNameList.getSelectedName()))
					resourceNameList.refresh();
			}
		});
		editButtons.add(button);
		
		if (this.mwConfigManager != null) {
			button = new JButton("Export Config");
			button.setBorder(BorderFactory.createRaisedBevelBorder());
			button.setPreferredSize(new Dimension(100, 21));
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					((MarkupWizardConfigurationManager) mwConfigManager).exportMarkupWizardConfiguration(resourceNameList.getSelectedName());
				}
			});
			editButtons.add(button);
			button = new JButton("Externalize");
			button.setBorder(BorderFactory.createRaisedBevelBorder());
			button.setPreferredSize(new Dimension(100, 21));
			button.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					externalizeMarkupWizard(resourceNameList.getSelectedName());
				}
			});
			editButtons.add(button);
		}
		
		editDialog.add(editButtons, BorderLayout.NORTH);
		
		final JPanel editorPanel = new JPanel(new BorderLayout());
		String selectedName = this.resourceNameList.getSelectedName();
		if (selectedName == null)
			editorPanel.add(this.getExplanationLabel(), BorderLayout.CENTER);
		else {
			MarkupWizardStub markupWizzard = this.loadMarkupWizard(selectedName);
			if (markupWizzard == null)
				editorPanel.add(this.getExplanationLabel(), BorderLayout.CENTER);
			else {
				editor[0] = new MarkupWizardEditorPanel(selectedName, markupWizzard);
				editorPanel.add(editor[0], BorderLayout.CENTER);
			}
		}
		editDialog.add(editorPanel, BorderLayout.CENTER);
		
		editDialog.add(this.resourceNameList, BorderLayout.EAST);
		DataListListener dll = new DataListListener() {
			public void selected(String dataName) {
				if ((editor[0] != null) && editor[0].isDirty()) {
					try {
						storeMarkupWizard(editor[0].name, editor[0].getMarkupWizard());
					}
					catch (IOException ioe) {
						if (JOptionPane.showConfirmDialog(editDialog, (ioe.getClass().getName() + " (" + ioe.getMessage() + ")\nwhile saving file to " + editor[0].name + "\nProceed?"), "Could Not Save Analyzer", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
							resourceNameList.setSelectedName(editor[0].name);
							editorPanel.validate();
							return;
						}
					}
				}
				editorPanel.removeAll();
				
				if (dataName == null)
					editorPanel.add(getExplanationLabel(), BorderLayout.CENTER);
				else {
					MarkupWizardStub markupWizzard = loadMarkupWizard(dataName);
					if (markupWizzard == null)
						editorPanel.add(getExplanationLabel(), BorderLayout.CENTER);
					else {
						editor[0] = new MarkupWizardEditorPanel(dataName, markupWizzard);
						editorPanel.add(editor[0], BorderLayout.CENTER);
					}
				}
				editorPanel.validate();
			}
		};
		this.resourceNameList.addDataListListener(dll);
		
		editDialog.setSize(DEFAULT_EDIT_DIALOG_SIZE);
		editDialog.setLocationRelativeTo(editDialog.getOwner());
		editDialog.setVisible(true);
		
		this.resourceNameList.removeDataListListener(dll);
	}
	
	private void externalizeMarkupWizard(String name) {
		
		//	get root path
		File ggRoot;
		try {
			ggRoot = new File(".");
			
			//	check if we got the root path
			File ggJar = new File(ggRoot, "GoldenGATE.jar");
			if (!ggJar.exists()) {
				JOptionPane.showMessageDialog(DialogPanel.getTopWindow(), "Cannot externalize Markup Wizard without GoldenGATE root folder.", "GoldenGATE Root Folder Not Found", JOptionPane.ERROR_MESSAGE);
				return;
			}
		}
		
		//	we may not be allowed to access the file system ...
		catch (SecurityException se) {
			JOptionPane.showMessageDialog(DialogPanel.getTopWindow(), "Cannot externalize Markup Wizard without access to GoldenGATE root folder.", "GoldenGATE Root Folder Not Accessible", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		//	get markup wizard
		MarkupWizard mw = this.getMarkupWizard(name);
		if (mw == null) {
			JOptionPane.showMessageDialog(DialogPanel.getTopWindow(), ("Unable to find Markup Wizard '" + name + "'"), "Unknown Markup Wizard", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		
		//	export configuration
		if (!((MarkupWizardConfigurationManager) this.mwConfigManager).exportMarkupWizardConfiguration(name)) {
			JOptionPane.showMessageDialog(DialogPanel.getTopWindow(), "Could not externalize Markup Wizard configuration.", "Configuration Not Exported", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		//	store GgMarkupWizard.cnfg
		try {
			Settings mwSet = new Settings();
			mwSet.setSetting("WizardName", name);
			mwSet.storeAsText(new File(ggRoot, "GgMarkupWizard.cnfg"));
		}
		catch (IOException ioe) {
			System.out.println("Exception externalizing Markup Wizard: " + ioe.getMessage());
			ioe.printStackTrace(System.out);
			JOptionPane.showMessageDialog(DialogPanel.getTopWindow(), ("Exception externalizing Markup Wizard: " + ioe.getMessage()), "Exception Externalizing Markup Wizard", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		//	copy forward markup wizard main jar
		try {
			File mwJarFile = new File(ggRoot, "GgMarkupWizard.jar");
			if (mwJarFile.exists()) {
				mwJarFile.renameTo(new File(mwJarFile.getPath() + "." + System.currentTimeMillis() + ".old"));
				mwJarFile = new File(ggRoot, "GgMarkupWizard.jar");
			}
			mwJarFile.createNewFile();
			OutputStream os = new BufferedOutputStream(new FileOutputStream(mwJarFile));
			InputStream is = this.dataProvider.getInputStream("GgMarkupWizard.jar");
			int count;
			byte data[] = new byte[1024];
			while ((count = is.read(data, 0, 1024)) != -1)
				os.write(data, 0, count);
			os.flush();
			os.close();
			is.close();
		}
		catch (IOException ioe) {
			System.out.println("Exception externalizing Markup Wizard: " + ioe.getMessage());
			ioe.printStackTrace(System.out);
			JOptionPane.showMessageDialog(DialogPanel.getTopWindow(), ("Exception externalizing Markup Wizard: " + ioe.getMessage()), "Exception Externalizing Markup Wizard", JOptionPane.ERROR_MESSAGE);
			return;
		}
		
		//	copy forward markup wizard starter jar
		try {
			File mwStarterJarFile = new File(ggRoot, "GgMarkupWizardStarter.jar");
			if (mwStarterJarFile.exists()) {
				mwStarterJarFile.renameTo(new File(mwStarterJarFile.getPath() + "." + System.currentTimeMillis() + ".old"));
				mwStarterJarFile = new File(ggRoot, "GgMarkupWizardStarter.jar");
			}
			mwStarterJarFile.createNewFile();
			OutputStream os = new BufferedOutputStream(new FileOutputStream(mwStarterJarFile));
			InputStream is = this.dataProvider.getInputStream("GgMarkupWizardStarter.jar");
			int count;
			byte data[] = new byte[1024];
			while ((count = is.read(data, 0, 1024)) != -1)
				os.write(data, 0, count);
			os.flush();
			os.close();
			is.close();
		}
		catch (IOException ioe) {
			System.out.println("Exception externalizing Markup Wizard: " + ioe.getMessage());
			ioe.printStackTrace(System.out);
			JOptionPane.showMessageDialog(DialogPanel.getTopWindow(), ("Exception externalizing Markup Wizard: " + ioe.getMessage()), "Exception Externalizing Markup Wizard", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private class MarkupWizardStub {
		final String name;
		final String processName;
		final ProcessPartMapper mapper;
		MarkupWizardStub(String name, String processName, ProcessPartMapper mapper) {
			this.name = name;
			this.processName = processName;
			this.mapper = mapper;
		}
		MarkupWizard toExternalForm() {
			Process process = mpManager.getMarkupProcess(this.processName);
			return ((process == null) ? null : new MarkupWizard(this.name, process, this.mapper));
		}
	}
	
	private class CreateMarkupWizardDialog extends DialogPanel {
		
		private JTextField nameField;
		
		private MarkupWizardEditorPanel editor;
		private String markupWizardName = null;
		
		CreateMarkupWizardDialog(MarkupWizardStub mws, String name) {
			super("Create MarkupWizard", true);
			
			this.nameField = new JTextField((name == null) ? "New MarkupWizard" : name);
			this.nameField.setBorder(BorderFactory.createLoweredBevelBorder());
			
			//	initialize main buttons
			JButton commitButton = new JButton("Create");
			commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
			commitButton.setPreferredSize(new Dimension(100, 21));
			commitButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					markupWizardName = nameField.getText();
					dispose();
				}
			});
			
			JButton abortButton = new JButton("Cancel");
			abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
			abortButton.setPreferredSize(new Dimension(100, 21));
			abortButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					markupWizardName = null;
					dispose();
				}
			});
			
			JPanel mainButtonPanel = new JPanel();
			mainButtonPanel.setLayout(new FlowLayout());
			mainButtonPanel.add(commitButton);
			mainButtonPanel.add(abortButton);
			
			//	initialize editor
			this.editor = new MarkupWizardEditorPanel(name, mws);
			
			//	put the whole stuff together
			this.getContentPane().setLayout(new BorderLayout());
			this.getContentPane().add(this.nameField, BorderLayout.NORTH);
			this.getContentPane().add(this.editor, BorderLayout.CENTER);
			this.getContentPane().add(mainButtonPanel, BorderLayout.SOUTH);
			
			this.setResizable(true);
			this.setSize(new Dimension(1000, 750));
			this.setLocationRelativeTo(DialogPanel.getTopWindow());
		}
		
		boolean isCommitted() {
			return (this.markupWizardName != null);
		}
		
		MarkupWizardStub getMarkupWizard() {
			return this.editor.getMarkupWizard();
		}
		
		String getMarkupWizardName() {
			return this.markupWizardName;
		}
	}

	private class EditMarkupWizardDialog extends DialogPanel {
		
		private MarkupWizardEditorPanel editor;
		private String markupWizardName = null;
		
		EditMarkupWizardDialog(MarkupWizardStub mws, String name) {
			super(("Edit MarkupWizard '" + name + "'"), true);
			this.markupWizardName = name;
			
			//	initialize main buttons
			JButton commitButton = new JButton("OK");
			commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
			commitButton.setPreferredSize(new Dimension(100, 21));
			commitButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					dispose();
				}
			});
			
			JButton abortButton = new JButton("Cancel");
			abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
			abortButton.setPreferredSize(new Dimension(100, 21));
			abortButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent e) {
					markupWizardName = null;
					dispose();
				}
			});
			
			JPanel mainButtonPanel = new JPanel();
			mainButtonPanel.setLayout(new FlowLayout());
			mainButtonPanel.add(commitButton);
			mainButtonPanel.add(abortButton);
			
			//	initialize editor
			this.editor = new MarkupWizardEditorPanel(name, mws);
			
			//	put the whole stuff together
			this.getContentPane().setLayout(new BorderLayout());
			this.getContentPane().add(this.editor, BorderLayout.CENTER);
			this.getContentPane().add(mainButtonPanel, BorderLayout.SOUTH);
			
			this.setResizable(true);
			this.setSize(new Dimension(1000, 700));
			this.setLocationRelativeTo(DialogPanel.getTopWindow());
		}
		
		boolean isCommitted() {
			return (this.markupWizardName != null);
		}
		
		MarkupWizardStub getMarkupWizard() {
			return this.editor.getMarkupWizard();
		}
	}

	private class MarkupWizardEditorPanel extends JPanel {
		
		private String name;
		private String processName;
		private JLabel processLabel;
		private boolean dirty = false;
		private ExtensibleMarkupProcessEditor processEditor;
		private ProcessPartMapper mapper;
		private ProcessorSelectorExtension mappingExtension;
		
		MarkupWizardEditorPanel(String name, MarkupWizardStub mws) {
			super(new BorderLayout(), true);
			this.name = name;
			this.processName = ((mws == null) ? null : mws.processName);
			
			this.mapper = new ProcessPartMapper(new String[0]);
			if (mws != null) {
				ProcessPartMapping[] mappings = mws.mapper.getProcessPartMappings();
				for (int m = 0; m < mappings.length; m++)
					this.mapper.setProcessPartMapping(mappings[m].processPartName, mappings[m].documentProcessorName, mappings[m].documentProcessorTrusted);
			}
			this.mappingExtension = new ProcessorSelectorExtension(this.mapper);
			this.processEditor = mpManager.getMarkupProcessEditor(this.processName, (this.processName == null), this.mappingExtension);
			
			this.processLabel = new JLabel(("Markup Process: " + ((mws == null) ? "<double click to select markup process>" : (mws.processName + " (double click to change)"))), JLabel.LEFT);
			this.processLabel.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					if (me.getClickCount() > 1)
						selectMarkupProcess();
				}
			});
			
			this.add(this.processLabel, BorderLayout.NORTH);
			this.add(this.processEditor.getEditorPanel(), BorderLayout.CENTER);
		}
		
		void selectMarkupProcess() {
			ResourceDialog rd = ResourceDialog.getResourceDialog(mpManager, "Select Markup Process", "Select");
			rd.setLocationRelativeTo(DialogPanel.getTopWindow());
			rd.setVisible(true);
			
			String newProcessName = rd.getSelectedResourceName();
			if ((newProcessName == null) || newProcessName.equals(this.processName))
				return;
			
			ProcessorSelectorExtension.ProcessorSelector[] pss = this.mappingExtension.getConcreteInstances();
			for (int p = 0; p < pss.length; p++) {
				ProcessPartMapping mapping = pss[p].getMapping();
				if (mapping != null)
					this.mapper.setProcessPartMapping(mapping.processPartName, mapping.documentProcessorName, mapping.documentProcessorTrusted);
			}
			
			this.dirty = true;
			this.processName = newProcessName;
			this.processLabel.setText("Markup Process: " + this.processName + " (double click to change)");
			this.processEditor = mpManager.getMarkupProcessEditor(this.processName, false, this.mappingExtension);
			
			this.removeAll();
			this.add(this.processLabel, BorderLayout.NORTH);
			this.add(this.processEditor.getEditorPanel(), BorderLayout.CENTER);
			this.validate();
			this.repaint();
		}
		
		boolean isDirty() {
			if (this.dirty)
				return true;
			ProcessorSelectorExtension.ProcessorSelector[] pss = this.mappingExtension.getConcreteInstances();
			for (int p = 0; p < pss.length; p++)
				if (pss[p].isDirty())
					return true;
			return false;
		}
		
		MarkupWizardStub getMarkupWizard() {
			String processName;
			try {
				processName = this.processEditor.saveMarkupProcess();
			}
			catch (IOException e) {
				e.printStackTrace(System.out);
				return null;
			}
			Process process = mpManager.getMarkupProcess(processName);
			if (process == null)
				return null;
			
			Properties rawMappings = new Properties();
			ProcessorSelectorExtension.ProcessorSelector[] pss = this.mappingExtension.getConcreteInstances();
			for (int p = 0; p < pss.length; p++) {
				ProcessPartMapping mapping = pss[p].getMapping();
				if (mapping != null)
					rawMappings.setProperty(mapping.processPartName, mapping.toString());
			}
			
			StringVector mappings = new StringVector();
			String ppName;
			Level[] levels = process.getLevels();
			for (int l = 0; l < levels.length; l++) {
				ppName = levels[l].getFullName();
				if (rawMappings.containsKey(ppName))
					mappings.addElementIgnoreDuplicates(rawMappings.getProperty(ppName));
				
				Task[] tasks = levels[l].getTasks();
				for (int t = 0; t < tasks.length; t++) {
					ppName = tasks[t].getFullName();
					if (rawMappings.containsKey(ppName))
						mappings.addElementIgnoreDuplicates(rawMappings.getProperty(ppName));
					
					Step[] steps = tasks[t].getSteps();
					for (int s = 0; s < steps.length; s++) {
						ppName = steps[s].getFullName();
						if (rawMappings.containsKey(ppName))
							mappings.addElementIgnoreDuplicates(rawMappings.getProperty(ppName));
						
						Criterion[] criterions = steps[s].getCriterions();
						for (int c = 0; c < criterions.length; c++) {
							ppName = criterions[c].getFullName();
							if (rawMappings.containsKey(ppName))
								mappings.addElementIgnoreDuplicates(rawMappings.getProperty(ppName));
						}
					}
				}
			}
			
			return new MarkupWizardStub(this.name, processName, new ProcessPartMapper(mappings.toStringArray()));
		}
	}
	
	private class ProcessorSelectorExtension extends MarkupProcessEditorExtension {
		ProcessPartMapper mapper;
		
		ProcessorSelectorExtension(ProcessPartMapper mapper) {
			this.mapper = mapper;
		}
		
		protected MarkupProcessPartEditorExtension produceExtension(MarkupProcessPartEditor ppe) {
			return ((ppe instanceof MarkupStepEditor) ? new ProcessorSelector(ppe) : null);
		}
		
		ProcessorSelector[] getConcreteInstances() {
			MarkupProcessPartEditorExtension[] instances = super.getInstances();
			ProcessorSelector[] concreteInstances = new ProcessorSelector[instances.length];
			for (int i = 0; i < instances.length; i++)
				concreteInstances[i] = ((ProcessorSelector) instances[i]);
			return concreteInstances;
		}
		
		class ProcessorSelector extends MarkupProcessPartEditorExtension {
			private ProcessorSelectorPanel selector;
			ProcessorSelector(MarkupProcessPartEditor host) {
				super(host);
				String dpName = null;
				boolean trustedDp = false;
				ProcessPartMapping mapping = mapper.getProcessPartMapping(host.getProcessPartFullName());
				if (mapping != null) {
					dpName = mapping.documentProcessorName;
					trustedDp = mapping.documentProcessorTrusted;
				}
				this.selector = new ProcessorSelectorPanel(dpName, trustedDp);
				this.add(this.selector, BorderLayout.CENTER);
			}
			boolean isDirty() {
				return this.selector.isDirty();
			}
			ProcessPartMapping getMapping() {
				return this.selector.getMapping(this.host.getProcessPartFullName());
			}
		}
	}
	
	private class ProcessorSelectorPanel extends JPanel {
		
		private boolean dirty = false;
		
		private String processorName = null;
		private String processorProviderClassName = null;
		private JLabel processorLabel = new JLabel("<No Document Processor Selected>", JLabel.LEFT);
		private JCheckBox processorTrusted = new JCheckBox("Trusted");
		
		ProcessorSelectorPanel(String dpName, boolean trustedDp) {
			super(new BorderLayout(), true);
			
			if (dpName != null) {
				DocumentProcessor dp = parent.getDocumentProcessorForName(dpName);
				if (dp != null) {
					this.processorName = dp.getName();
					this.processorProviderClassName = dp.getProviderClassName();
					this.processorLabel.setText(dp.getTypeLabel() + ": " + dp.getName() + " (double click to edit)");
					if (trustedDp)
						this.processorTrusted.setSelected(true);
				}
			}
			this.processorLabel.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					if (me.getClickCount() > 1) {
						DocumentProcessorManager dpm = parent.getDocumentProcessorProvider(processorProviderClassName);
						if (dpm != null) dpm.editDocumentProcessor(processorName);
					}
				}
			});
			this.processorTrusted.addItemListener(new ItemListener() {
				public void itemStateChanged(ItemEvent ie) {
					dirty = true;
				}
			});
			
			final JPopupMenu useDpMenu = new JPopupMenu();
			final JPopupMenu createDpMenu = new JPopupMenu();
			
			DocumentProcessorManager[] dpms = parent.getDocumentProcessorProviders();
			for (int p = 0; p < dpms.length; p++)
				if (dpms[p] != MarkupWizardManager.this) {
					final String className = dpms[p].getClass().getName();
					
					JMenuItem useDpMi = new JMenuItem("Use " + dpms[p].getResourceTypeLabel());
					useDpMi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							selectProcessor(className);
						}
					});
					useDpMenu.add(useDpMi);
					
					JMenuItem createDpMi = new JMenuItem("Create " + dpms[p].getResourceTypeLabel());
					createDpMi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent e) {
							createProcessor(className);
						}
					});
					createDpMenu.add(createDpMi);
				}
			
			final JButton useDpButton = new JButton("Use ...");
			useDpButton.setBorder(BorderFactory.createRaisedBevelBorder());
			useDpButton.setSize(new Dimension(120, 21));
			useDpButton.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					useDpMenu.show(useDpButton, me.getX(), me.getY());
				}
			});
			
			final JButton createDpButton = new JButton("Create ...");
			createDpButton.setBorder(BorderFactory.createRaisedBevelBorder());
			createDpButton.setSize(new Dimension(120, 21));
			createDpButton.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					createDpMenu.show(createDpButton, me.getX(), me.getY());
				}
			});
			
			final JButton clearDpButton = new JButton("Clear");
			clearDpButton.setBorder(BorderFactory.createRaisedBevelBorder());
			clearDpButton.setSize(new Dimension(120, 21));
			clearDpButton.addMouseListener(new MouseAdapter() {
				public void mouseClicked(MouseEvent me) {
					clearProcessor();
				}
			});
			
			JPanel setDpPanel = new JPanel(new GridLayout(1, 3, 3, 0), true);
			setDpPanel.add(createDpButton);
			setDpPanel.add(useDpButton);
			setDpPanel.add(this.processorTrusted);
			
			this.add(setDpPanel, BorderLayout.WEST);
			this.add(this.processorLabel, BorderLayout.CENTER);
			this.add(clearDpButton, BorderLayout.EAST);
		}
		
		void selectProcessor(String providerClassName) {
			DocumentProcessorManager dpm = parent.getDocumentProcessorProvider(providerClassName);
			if (dpm != null) {
				ResourceDialog rd = ResourceDialog.getResourceDialog(dpm, ("Select " + dpm.getResourceTypeLabel()), "Select");
				rd.setLocationRelativeTo(DialogPanel.getTopWindow());
				rd.setVisible(true);
				DocumentProcessor dp = dpm.getDocumentProcessor(rd.getSelectedResourceName());
				if (dp != null) {
					this.processorName = dp.getName();
					this.processorProviderClassName = dp.getProviderClassName();
					this.processorLabel.setText(dp.getTypeLabel() + ": " + dp.getName() + " (double click to edit)");
					this.processorTrusted.setSelected(false);
					this.dirty = true;
				}
			}
		}
		
		void createProcessor(String providerClassName) {
			DocumentProcessorManager dpm = parent.getDocumentProcessorProvider(providerClassName);
			if (dpm != null) {
				String dpName = dpm.createDocumentProcessor();
				DocumentProcessor dp = dpm.getDocumentProcessor(dpName);
				if (dp != null) {
					this.processorName = dp.getName();
					this.processorProviderClassName = dp.getProviderClassName();
					this.processorLabel.setText(dp.getTypeLabel() + ": " + dp.getName() + " (double click to edit)");
					this.processorTrusted.setSelected(false);
					this.dirty = true;
				}
			}
		}
		
		void clearProcessor() {
			this.processorName = null;
			this.processorProviderClassName = null;
			this.processorLabel.setText("<No Document Processor Selected>");
			this.processorTrusted.setSelected(false);
			this.dirty = true;
		}
		
		boolean isDirty() {
			return this.dirty;
		}
		
		ProcessPartMapping getMapping(String processPartName) {
			String dpName;
			if ((this.processorName == null) || (this.processorProviderClassName == null)) dpName = null;
			else dpName = (this.processorName + "@" + this.processorProviderClassName);
			return new ProcessPartMapping(processPartName, dpName, this.processorTrusted.isSelected());
		}
	}
}
