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

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DropTarget;
import java.awt.dnd.DropTargetAdapter;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;

import de.uka.ipd.idaho.gamta.Annotation;
import de.uka.ipd.idaho.gamta.AnnotationListener;
import de.uka.ipd.idaho.gamta.CharSequenceListener;
import de.uka.ipd.idaho.gamta.Gamta;
import de.uka.ipd.idaho.gamta.MutableAnnotation;
import de.uka.ipd.idaho.gamta.MutableCharSequence.CharSequenceEvent;
import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.Token;
import de.uka.ipd.idaho.gamta.util.validation.Step.StepValidationResult;
import de.uka.ipd.idaho.gamta.util.validation.ValidationResult;
import de.uka.ipd.idaho.gamta.util.validation.ValidationResultPanel;
import de.uka.ipd.idaho.gamta.util.validation.ValidationResultPanel.ValidationResultLabel;
import de.uka.ipd.idaho.gamta.util.validation.Validator;
import de.uka.ipd.idaho.goldenGate.DocumentEditorDialog;
import de.uka.ipd.idaho.goldenGate.GoldenGATE;
import de.uka.ipd.idaho.goldenGate.GoldenGateConfiguration;
import de.uka.ipd.idaho.goldenGate.GoldenGateConstants;
import de.uka.ipd.idaho.goldenGate.markupWizard.MarkupWizard;
import de.uka.ipd.idaho.goldenGate.markupWizard.ProcessPartMapper.ProcessPartMapping;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentFormat;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentLoader;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentLoader.DocumentData;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentProcessor;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentSaveOperation;
import de.uka.ipd.idaho.goldenGate.plugins.DocumentSaver;
import de.uka.ipd.idaho.goldenGate.plugins.Resource;
import de.uka.ipd.idaho.goldenGate.plugins.ResourceSplashScreen;
import de.uka.ipd.idaho.goldenGate.util.AttributeEditorDialog;
import de.uka.ipd.idaho.goldenGate.util.DialogPanel;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * GoldenGATE Markup Wizard is a slim application for marking up documents using
 * a markup process definition and document processor mapping, obtained from the
 * Markup Wizard Manager plugin. The GUI allows for opening one document at a
 * time. The markup process definition mostly controls which steps to take next,
 * which document processors to use, and where the problematic sections are in a
 * document. In case the automatic process control fails, however, users can
 * still open the document for manual editing.
 * 
 * @author sautter
 */
public class GoldenGateMarkupWizard extends JFrame implements GoldenGateConstants {
	
	private static final String WINDOW_BASE_TITLE = "GoldenGATE Markup Wizard";
	
	MarkupWizardBaseGUI loaderPanel;
	MarkupWizardPanel documentPanel;
	
	GoldenGateConfiguration ggConfiguration;
	GoldenGATE goldenGate;
	
	MarkupWizard markupWizard;
	Validator validator;
	
	HashMap stepNumbers = new HashMap();
	
	/**
	 * Constructor
	 * @param ggConfig the GoldenGATE configuration to use
	 * @param goldenGate the GoldenGATE Editor core to use
	 * @param markupWizard the markup wizard to use
	 */
	public GoldenGateMarkupWizard(GoldenGateConfiguration ggConfig, GoldenGATE goldenGate, MarkupWizard markupWizard) {
		super(WINDOW_BASE_TITLE);
		
		this.ggConfiguration = ggConfig;
		this.goldenGate = goldenGate;
		
		this.markupWizard = markupWizard;
		this.validator = this.markupWizard.process.getValidator();
		
		String[] stepNames = this.markupWizard.process.getStepNames();
		for (int s = 0; s < stepNames.length; s++)
			this.stepNumbers.put(stepNames[s], new Integer(s+1));
		
		this.setTitle(WINDOW_BASE_TITLE + " - " + this.markupWizard.name);
		this.setIconImage(this.ggConfiguration.getIconImage());
		this.setDefaultCloseOperation(DISPOSE_ON_CLOSE);
		
		this.loaderPanel = new MarkupWizardBaseGUI(GoldenGateMarkupWizard.this);
		
		this.getContentPane().setLayout(new BorderLayout());
		this.getContentPane().add(this.loaderPanel, BorderLayout.CENTER);
		
		this.setSize(600, 400);
		this.setResizable(true);
		this.setLocationRelativeTo(null);
	}
	
	/**
	 * Overwrites dispose in a way that the open document (if any) is closed
	 * before actually disposing.
	 * @see java.awt.Window#dispose()
	 */
	public void dispose() {
		if (this.isDocumentOpen())
			this.closeDocument();
		super.dispose();
	}
	
	/**
	 * Test whether a document is currently opened in the markup wizard.
	 * @return true if a document is currently opened in the markup wizard,
	 *         false otherwise
	 */
	public boolean isDocumentOpen() {
		return (this.documentPanel != null);
	}
	
	/**
	 * Open a document. If one is already open, this method has no effect.
	 * @param dd the document to open, together with associated data
	 */
	public void openDocument(DocumentData dd) {
		if (this.documentPanel != null) return;
		
		System.out.println("MarkupWizard: opening document '" + dd.name + "'");
		this.getContentPane().removeAll();
		this.documentPanel = new MarkupWizardPanel(this, dd);
		this.getContentPane().add(this.documentPanel, BorderLayout.CENTER);
		this.validate();
		this.repaint();
	}
	
	/**
	 * Close the document currently open. If none is open, this method has no
	 * effect. If a document is open and has unsaved changes, this method
	 * prompts the user, asking whether or not to save the changes. If there is
	 * a reusable document save operation associated with the document, its
	 * documentClased() method will be invoked if the document is closed
	 * successfully.
	 */
	public void closeDocument() {
		if (this.documentPanel == null) return;
		
		if (!this.documentPanel.closeDocument()) 
			return;
		
		this.getContentPane().removeAll();
		this.documentPanel = null;
		this.getContentPane().add(this.loaderPanel, BorderLayout.CENTER);
		this.validate();
		this.repaint();
	}
	
	private static class MarkupWizardBaseGUI extends JPanel {
		GoldenGateMarkupWizard parent;
		
		MarkupWizardBaseGUI(GoldenGateMarkupWizard parent) {
			super(new BorderLayout(), true);
			this.parent = parent;
			
			//	build load menu
			JMenu loadMenu = new JMenu("Load Document");
			DocumentLoader[] dls = this.parent.goldenGate.getDocumentLoaders();
			for (int l = 0; l < dls.length; l++) {
				JMenuItem loadItem = dls[l].getLoadDocumentMenuItem();
				if (loadItem != null) {
					final DocumentLoader loader = dls[l];
					loadItem.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							try {
								DocumentData dd = loader.loadDocument();
								if (dd != null)
									MarkupWizardBaseGUI.this.parent.openDocument(dd);
							}
							catch (Exception e) {
								System.out.println("Exception loading document from " + loader.getClass().getName() + ": " + e.getMessage());
								e.printStackTrace(System.out);
								JOptionPane.showMessageDialog(DialogPanel.getTopWindow(), e.getMessage(), "Exception Loading Document", JOptionPane.ERROR_MESSAGE);
							}
						}
					});
					loadMenu.add(loadItem);
				}
			}
			
			//	build config menu
			JMenu mwMenu = new JMenu("Markup Wizard");
			JMenuItem mwMenuItem;
			
			//	add exit option
			mwMenuItem = new JMenuItem("Exit Markup Wizard");
			mwMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					MarkupWizardBaseGUI.this.parent.dispose();
				}
			});
			mwMenu.add(mwMenuItem);
			
			//	assemble menu bar
			JMenuBar menu = new JMenuBar();
			menu.add(loadMenu);
			menu.add(mwMenu);
			menu.add(this.parent.goldenGate.getHelpMenu());
			
			
			//	build drop pad
			final JPanel dropPanel = new JPanel(new BorderLayout());
			dropPanel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(dropPanel.getBackground(), 5), BorderFactory.createLineBorder(Color.RED, 2)));
			dropPanel.add(new JLabel("Drop a file here to open it and mark it up with GoldenGATE Markup Wizard.", JLabel.CENTER), BorderLayout.CENTER);
			DropTarget dropTarget = new DropTarget(dropPanel, new DropTargetAdapter() {
				public void drop(DropTargetDropEvent dtde) {
					dtde.acceptDrop(dtde.getDropAction());
					Transferable transfer = dtde.getTransferable();
					DataFlavor[] dataFlavors = transfer.getTransferDataFlavors();
					for (int d = 0; d < dataFlavors.length; d++) {
						System.out.println(dataFlavors[d].toString());
						System.out.println(dataFlavors[d].getRepresentationClass());
						try {
							Object transferData = transfer.getTransferData(dataFlavors[d]);
							System.out.println(transferData.getClass().getName());
							
							List transferList = ((List) transferData);
							if (transferList.isEmpty()) return;
							
							String message = "Got dropped item(s):";
							for (int l = 0; l < transferList.size(); l++)
								message += ("\n  - " + transferList.get(l).toString());
							if (transferList.size() > 1) {
								message += ("\nOpening first one: " + transferList.get(0).toString());
								JOptionPane.showMessageDialog(MarkupWizardBaseGUI.this, message, "Got Item(s) Droped On", JOptionPane.INFORMATION_MESSAGE);
							}
							
							File droppedFile = ((File) transferList.get(0));
							try {
								String fileName = droppedFile.getName();
								String dataType;
								if ((fileName.indexOf('.') == -1) || fileName.endsWith(".")) dataType = "xml";
								else dataType = fileName.substring(fileName.lastIndexOf('.') + 1);
								
								DocumentFormat format = MarkupWizardBaseGUI.this.parent.goldenGate.getDocumentFormatForFileExtension(dataType);
								if (format == null) {
									String formatName = MarkupWizardBaseGUI.this.parent.goldenGate.selectLoadFormat();
									if (formatName != null)
										format = MarkupWizardBaseGUI.this.parent.goldenGate.getDocumentFormatForName(formatName);
								}
								
								if (format == null) JOptionPane.showMessageDialog(MarkupWizardBaseGUI.this, ("GoldenGATE Markup Wizzard cannot open the dropped file, sorry,\nthe data format in '" + droppedFile.getName() + "' is unknown."), "Unknown Document Format", JOptionPane.INFORMATION_MESSAGE);
								else {
									System.out.println("MarkupWizard: opening dropped file as '" + format.getDefaultSaveFileExtension() + "' (" + format.getDescription() + ") via " + format.getClass().getName());
									InputStream source = new FileInputStream(droppedFile);
									MutableAnnotation doc = format.loadDocument(source);
									source.close();
									if (doc != null)
										MarkupWizardBaseGUI.this.parent.openDocument(new DocumentData(doc, fileName, format));
								}
							}
							catch (IOException ioe) {
								System.out.println("Error opening document '" + droppedFile.getAbsolutePath() + "':\n   " + ioe.getClass().getName() + " (" + ioe.getMessage() + ")");
								ioe.printStackTrace(System.out);
								JOptionPane.showMessageDialog(dropPanel, ("Could not open file '" + droppedFile.getAbsolutePath() + "':\n" + ioe.getMessage()), "Error Opening File", JOptionPane.ERROR_MESSAGE);
							}
							catch (SecurityException se) {
								System.out.println("Error opening document '" + droppedFile.getName() + "':\n   " + se.getClass().getName() + " (" + se.getMessage() + ")");
								se.printStackTrace(System.out);
								JOptionPane.showMessageDialog(dropPanel, ("Not allowed to open file '" + droppedFile.getName() + "':\n" + se.getMessage() + "\n\nIf you are currently running GoldenGATE Markup Wizard as an applet, your\nbrowser's security mechanisms might prevent reading files from your local disc."), "Not Allowed To Open File", JOptionPane.ERROR_MESSAGE);
							}
						}
						catch (UnsupportedFlavorException ufe) {
							ufe.printStackTrace(System.out);
						}
						catch (IOException ioe) {
							ioe.printStackTrace(System.out);
						}
						catch (Exception e) {
							e.printStackTrace(System.out);
						}
					}
				}
			});
			dropTarget.setActive(true);
			
			
			//	put the whole stuff together
			this.add(menu, BorderLayout.NORTH);
			this.add(dropPanel, BorderLayout.CENTER);
		}
	}
	
	private static class MarkupWizardPanel extends JPanel {
		private GoldenGateMarkupWizard parent;
		private JMenuItem saveItem;
		
		private ValidationResultPanel processDisplay;
		private String currentStepName = null;
		
		private MutableAnnotation document;
		private String documentName;
		private DocumentFormat documentFormat;
		private DocumentSaveOperation documentSaveOperation;
		
//		ValidationResult documentValidationResult;
		private boolean contentModified = false;
		
		private JMenuBar menu = new JMenuBar();
		private JMenu undoMenu = new JMenu("Undo");
		private JMenuItem markupMenuItem = new JMenuItem("Mark Up Document");
		private JMenuItem correctMenuItem = new JMenuItem("Correct Manually");
		private JMenuItem editMenuItem = new JMenuItem("Edit Document");
		
		private JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER));
		private JButton markupButton = new JButton("Mark Up Document");
		private JButton correctButton = new JButton("Correct Manually");
		private JButton viewButton = new JButton("View Document");
		private JButton closeButton = new JButton("Close Document");
		
		private JProgressBar progress;
		
		private ValidationResultLabel mainLabel;
		private ValidationResultLabel detailLabel;
		
		MarkupWizardPanel(GoldenGateMarkupWizard parent, DocumentData dd) {
			super(new BorderLayout(), true);
			this.parent = parent;
			
			//	store base data
			this.document = dd.docData;
			this.documentName = dd.name;
			this.documentFormat = dd.format;
			this.documentSaveOperation = dd.saveOpertaion;
			
			
			//	listen for changes
			this.document.addCharSequenceListener(new CharSequenceListener() {
				public void charSequenceChanged(CharSequenceEvent change) {
					contentModified = true;
				}
			});
			this.document.addAnnotationListener(new AnnotationListener() {
				public void annotationAdded(QueriableAnnotation doc, Annotation annotation) {
					contentModified = true;
				}
				public void annotationAttributeChanged(QueriableAnnotation doc, Annotation annotation, String attributeName, Object oldValue) {
					contentModified = true;
				}
				public void annotationRemoved(QueriableAnnotation doc, Annotation annotation) {
					contentModified = true;
				}
				public void annotationTypeChanged(QueriableAnnotation doc, Annotation annotation, String oldType) {
					contentModified = true;
				}
			});
			
			
			//	build save menu
			JMenu saveMenu = new JMenu("Save Document");
			
			//	add generic save option
			this.saveItem = new JMenuItem("Save ...");
			this.saveItem.setEnabled(this.documentSaveOperation != null);
			this.saveItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					saveDocument(documentSaveOperation);
				}
			});
			saveMenu.add(this.saveItem);
			saveMenu.addSeparator();
			
			//	add specific saver plugins
			DocumentSaver[] dss = this.parent.goldenGate.getDocumentSavers();
			for (int l = 0; l < dss.length; l++) {
				JMenuItem saveAsItem = dss[l].getSaveDocumentMenuItem();
				if (saveAsItem != null) {
					final DocumentSaver saver = dss[l];
					saveAsItem.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							saveDocument(saver.getSaveOperation(documentName, documentFormat));
						}
					});
					saveMenu.add(saveAsItem);
				}
			}
			
			//	add closing option
			JMenuItem closeMenuItem = new JMenuItem("Close Document");
			closeMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					MarkupWizardPanel.this.parent.closeDocument();
				}
			});
			saveMenu.addSeparator();
			saveMenu.add(closeMenuItem);
			
			
			//	initialize undo menu
			this.refreshUndoMenu();
			
			//	prepare menu items
			this.markupMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					markUpDocument();
				}
			});
			this.correctMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					processDisplay.showDetailResult();
				}
			});
			this.editMenuItem.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					editDocument();
				}
			});
			
			//	create edit menu
			JMenu editMenu = new JMenu("Edit");
			editMenu.add(this.undoMenu);
			editMenu.addSeparator();
			editMenu.add(this.markupMenuItem);
			editMenu.add(this.correctMenuItem);
			editMenu.add(this.editMenuItem);
			
			
			//	create main menu
			this.menu.add(saveMenu);
			this.menu.add(editMenu);
			this.menu.add(this.parent.goldenGate.getHelpMenu());
			
			
			//	prepare progress indicator
			this.progress = new JProgressBar(0, this.parent.stepNumbers.size());
			this.progress.setStringPainted(true);
			
			
			//	prepate main label
			this.mainLabel = new ValidationResultLabel("", JLabel.LEFT) {
				public void validationResultChanged(ValidationResult vr) {
					int stepCount = MarkupWizardPanel.this.parent.stepNumbers.size();
					
					//	process finished
					if (vr.isPassed()) {
						setFinished();
						progress.setValue(stepCount);
						progress.setString("Finished (" + stepCount + "/" + stepCount + ")");
					}
					else {
						
						//	get current step
						StepValidationResult currentStep = processDisplay.getCurrentStep();
						String csName = currentStep.getValidator().getFullName();
						
						//	current step has remained the same since last validation, so auto-markup did not do the job
						if (csName.equals(currentStepName))
							setManually();
						
						//	we have proveeded to a new step, remember it and offer auto-markup
						else {
							currentStepName = csName;
							setAutomatically();
						}
						
						//	display progress
						Integer csProgress = ((Integer) MarkupWizardPanel.this.parent.stepNumbers.get(csName));
						progress.setValue((csProgress == null) ? 0 : csProgress.intValue());
						progress.setString(currentStep.getValidator().getLabel() + " (" + ((csProgress == null) ? 0 : csProgress.intValue()) + "/" + stepCount + ")");
					}
				}
			};
			this.mainLabel.setOpaque(true);
			this.mainLabel.setBackground(Color.WHITE);
			this.mainLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.WHITE, 2), BorderFactory.createLineBorder(Color.RED, 1)), BorderFactory.createLineBorder(Color.WHITE, 5)));
			
			//	prepate detail label
			this.detailLabel = new ValidationResultLabel("", JLabel.LEFT) {
				public void validationResultChanged(ValidationResult vr) {
					if (vr.isPassed())
						this.setText("<HTML><B>Document Status:</B> '" + vr.getValidator().getDescription() + "' complete." +
								"<BR>Use the <I>Close</I> button to return to the main window and proceed." +
								"</HTML>");
					
//					else this.setText("<HTML>" +
//								"<B>Document Status:</B> some annotations failed the test for '" + vr.getValidator().getDescription() + "'." +
//								"<BR>In particular, " + vr.getValidator().getErrorDescription() + ", please review whether or not this is an error in their particulat context, and handle them accordingly." + 
//								"<BR>The problematic annotations are highlighted (<I>Highlight Failing Annotations</I>) or listed (<I>Show Failing Annotations Only</I>) below." +
//								"<BR>Select problematic annotations with the mouse and use the context menu (right click) to modify them so they pass the curret test. These are your options:" +
//								"<BR>- <B>Edit</B>: Open the selected annotation(s) for editing. This provides all the editing options you might possibly need to make them pass the current test." +
//								"<BR>- <B>Edit Attributes</B>: Open the attributes of the selected annotation for editing. This option lets you fix erroneous attributes and add missing ones to make the annotation pass the current test." +
//								"<BR>- <B>Modify Attribute</B>: Modify the attributes of all selected annotations. This lets you add, change, and remove attributes to make the annotations pass the current test." +
//								"<BR>- <B>Remove Attribute</B>: Modify one or more attributes from all selected annotations so that they pass the current test." +
//								"<BR>- <B>Rename</B>: Change the XML element name of the selected annotation(s), which have to have the same name. This likely causes them to no longer be subject to the current test." + 
//								"<BR>- <B>Accept As Is</B>: Tell the markup wizard that the selected annotation(s) are not actual errors. This causes them to pass the current test." +
//								"<BR>- <B>Remove</B>: Remove the selected annotations(s) from the backing document. This option solely removes the markup, leaving the textual content unchanged." +
//								"<BR>- <B>Remove All</B>: Remove all annotations that have the same textual contant as the selected one. See <I>Remove</I> for behavior." +
//								"<BR>- <B>Delete</B>: Delete the selected annotations(s) from the backing document, inluding their textual content and all descendant markup." +
//								"</HTML>");
					else {
						ValidationResult[] pVrs = vr.getPartialResults();
						StringVector pErrors = new StringVector();
						for (int p = 0; p < pVrs.length; p++) {
//							if (!pVrs[p].isPassed())
								pErrors.addElement(pVrs[p].getValidator().getErrorDescription());
						}
						this.setText("<HTML>" +
							"<B>Document Status:</B> Some annotations fail the test for '" + vr.getValidator().getLabel() + "'." +
							((pErrors.size() < 2) ?
									("<BR>In particular, " + prepareLabelString(vr.getValidator().getErrorDescription(), false) + ". Please review whether or not this is an error in their particular context, and handle them accordingly.")
									:
									("<BR>In particular, " + prepareLabelString(vr.getValidator().getErrorDescription(), false) + ": " + (
											"<BR>&nbsp;&nbsp;- " + pErrors.concatStrings("<BR>&nbsp;&nbsp;- ")) + 
									"<BR>Please review whether or not this is an error in their particular context, and handle them accordingly.")
									) + 
							"<BR>The problematic annotations are highlighted (<I>Highlight Failing Annotations</I>) or listed (<I>Show Failing Annotations Only</I>) below." +
							"<BR>Select problematic annotations with the mouse and use the context menu (right click) to modify them so they pass the curret test. These are your options:" +
							"<BR>- <B>Edit</B>: Open the selected annotation(s) for inspection and editing. This provides all the editing options you might possibly need to make them pass the current test.<BR>&nbsp;&nbsp;The dialog also offers buttons for accepting the annottaions(s) in their current state, and for removing or deleting them." +
							"<BR>- <B>Edit Attributes</B>: Open the attributes of the selected annotation for editing.<BR>&nbsp;&nbsp;This option lets you fix erroneous attributes and add missing ones to make the annotation pass the current test." +
							"<BR>- <B>Modify Attribute</B>: Modify the attributes of all selected annotations. This lets you add, change, and remove attributes to make the annotations pass the current test." +
							"<BR>- <B>Remove Attribute</B>: Modify one or more attributes from all selected annotations so that they pass the current test." +
							"<BR>- <B>Rename</B>: Change the XML element name of the selected annotation(s), which have to have the same name. This likely causes them to no longer be subject to the current test." + 
							"<BR>- <B>Accept As Is</B>: Tell the markup wizard that the selected annotation(s) are not actual errors. This causes them to pass the current test." +
							"<BR>- <B>Remove</B>: Remove the selected annotations(s) from the backing document. This option solely removes the markup, leaving the textual content unchanged." +
							"<BR>- <B>Remove All</B>: Remove all annotations that have the same textual contant as the selected one. See <I>Remove</I> for behavior." +
							"<BR>- <B>Delete</B>: Delete the selected annotations(s) from the backing document, inluding their textual content and all descendant markup." +
							"</HTML>");
					}
					this.validate();
				}
			};
			this.detailLabel.setOpaque(true);
			this.detailLabel.setBackground(Color.WHITE);
			this.detailLabel.setBorder(BorderFactory.createCompoundBorder(BorderFactory.createCompoundBorder(BorderFactory.createLineBorder(Color.WHITE, 2), BorderFactory.createLineBorder(Color.RED, 1)), BorderFactory.createLineBorder(Color.WHITE, 5)));
			
			
			//	create main panel
			this.processDisplay = new GgMwValidationResultPanel();
			
			//	add labels
			this.processDisplay.setMainLabel(this.mainLabel);
			this.processDisplay.setDetailLabel(this.detailLabel);
			
			//	get document status
			ValidationResult vr = this.processDisplay.getValidationResult();
			if (vr.isPassed()) {
				this.setFinished();
				this.progress.setValue(MarkupWizardPanel.this.parent.stepNumbers.size());
				this.progress.setString("Finished (" + this.parent.stepNumbers.size() + "/" + this.parent.stepNumbers.size() + ")");
			}
			else {
				StepValidationResult currentStep = this.processDisplay.getCurrentStep();
				this.currentStepName = currentStep.getValidator().getFullName();
				
				this.setAutomatically();
				Integer csProgress = ((Integer) MarkupWizardPanel.this.parent.stepNumbers.get(this.currentStepName));
				this.progress.setValue((csProgress == null) ? 0 : csProgress.intValue());
				this.progress.setString(currentStep.getValidator().getLabel() + " (" + ((csProgress == null) ? 0 : csProgress.intValue()) + "/" + this.parent.stepNumbers.size() + ")");
				
				this.startNewUndoAction(currentStep.getValidator().getLabel(), false); // TODOne: use step name
			}
			
			
			//	prepare buttons
			this.markupButton.setBorder(BorderFactory.createRaisedBevelBorder());
			this.markupButton.setPreferredSize(new Dimension(100, 21));
			this.markupButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					markUpDocument();
				}
			});
			this.correctButton.setBorder(BorderFactory.createRaisedBevelBorder());
			this.correctButton.setPreferredSize(new Dimension(100, 21));
			this.correctButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					processDisplay.showDetailResult();
				}
			});
			this.viewButton.setBorder(BorderFactory.createRaisedBevelBorder());
			this.viewButton.setPreferredSize(new Dimension(100, 21));
			this.viewButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					editDocument();
				}
			});
			this.closeButton.setBorder(BorderFactory.createRaisedBevelBorder());
			this.closeButton.setPreferredSize(new Dimension(100, 21));
			this.closeButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					MarkupWizardPanel.this.parent.closeDocument();
				}
			});
			
			
			//	build function panel
			JPanel functionPanel = new JPanel(new BorderLayout());
			functionPanel.add(this.buttonPanel, BorderLayout.CENTER);
			functionPanel.add(this.progress, BorderLayout.SOUTH);
			
			//	put the whole stuff together
			this.add(this.menu, BorderLayout.NORTH);
			this.add(this.processDisplay, BorderLayout.CENTER);
			this.add(functionPanel, BorderLayout.SOUTH);
		}
		
		void setAutomatically() {
			this.markupMenuItem.setEnabled(true);
			this.correctMenuItem.setEnabled(false);
			
			this.buttonPanel.removeAll();
			this.buttonPanel.add(this.markupButton);
			this.buttonPanel.add(this.closeButton);
			this.buttonPanel.validate();
			this.buttonPanel.repaint();
			
			StepValidationResult currentStep = this.processDisplay.getCurrentStep();
//			this.mainLabel.setText("<HTML>" +
//					"<B>Document Status:</B> The document fails the test for '" + currentStep.getValidator().getDescription() + "'." +
//					"<BR>In particular, " + currentStep.getValidator().getErrorDescription() + 
//					"<BR><B>What to do?</B>" +
//					"<BR>- Click <I>Mark Up Document</I> to let the Markup Wizard use its automated routines for fixing the errors." +
//					"<BR>- Click <I>Close Document</I> to close the document and make way for a new one." +
//					"<BR>- Use the menus for additional options." +
//					"</HTML>");
			this.mainLabel.setText("<HTML>" +
					"<B>Document Status:</B> The document fails the test for '" + currentStep.getValidator().getLabel() + "'." +
					"<BR>In particular, " + this.prepareLabelString(currentStep.getValidator().getErrorDescription(), false) + 
					"<BR><B>To Do:</B> Please " + this.prepareLabelString(currentStep.getValidator().getDescription(), false) + "." +
					"<BR><B>How To Do It:</B> Click <I>Mark Up Document</I> to let the Markup Wizard use its automated routines for fixing the errors." +
					"<BR><B>Further Options:</B>" +
					"<BR>- Click <I>Close Document</I> to close the document and make way for marking up another one." +
					"<BR>- Use the menus for additional options." +
					"</HTML>");
			this.mainLabel.validate();
		}
		
		void setManually() {
			this.markupMenuItem.setEnabled(false);
			this.correctMenuItem.setEnabled(true);
			
			this.buttonPanel.removeAll();
			this.buttonPanel.add(this.correctButton);
			this.buttonPanel.add(this.closeButton);
			this.buttonPanel.validate();
			this.buttonPanel.repaint();
			
			StepValidationResult currentStep = this.processDisplay.getCurrentStep();
//			this.mainLabel.setText("<HTML>" +
//					"<B>Document Status:</B> The document still fails the test for '" + currentStep.getValidator().getDescription() + "'." +
//					"<BR>In particular, " + currentStep.getValidator().getErrorDescription() + 
//					"<BR><B>What to do?</B>" +
//					"<BR>- Click <I>Correct Manually</I> to manually inspect and accept or correct the problematic annotations." +
//					"<BR>- Click <I>Close Document</I> to close the document and make way for a new one." +
//					"<BR>- Use the menus for additional options." +
//					"</HTML>");
			this.mainLabel.setText("<HTML>" +
					"<B>Document Status:</B> The document still fails the test for '" + currentStep.getValidator().getLabel() + "'." +
					"<BR>In particular, still " + this.prepareLabelString(currentStep.getValidator().getErrorDescription(), false) + 
					"<BR><B>To Do:</B> Please " + this.prepareLabelString(currentStep.getValidator().getDescription(), false) + ", or accept them as non-errors." +
					"<BR><B>How To Do It:</B> Click <I>Correct Manually</I> to manually inspect and accept or correct the problematic annotations." +
					"<BR><B>Further Options:</B>" +
					"<BR>- Click <I>Close Document</I> to close the document and make way for marking up another one." +
					"<BR>- Use the menus for additional options." +
					"</HTML>");
			this.mainLabel.validate();
		}
		
		void setFinished() {
			this.markupMenuItem.setEnabled(false);
			this.correctMenuItem.setEnabled(false);
			
			this.buttonPanel.removeAll();
			this.buttonPanel.add(this.viewButton);
			this.buttonPanel.add(this.closeButton);
			this.buttonPanel.validate();
			this.buttonPanel.repaint();
			
//			this.mainLabel.setText("<HTML>" +
//					"<B>Document Status:</B> The markup of the document is finished." +
//					"<BR><B>What to do?</B>" +
//					"<BR>- Click <I>View Document</I> to inspect the finished document and make manual changes." +
//					"<BR>- Click <I>Close Document</I> to close the document and make way for a new one." +
//					"<BR>- Use the menus for additional options." +
//					"</HTML>");
			this.mainLabel.setText("<HTML>" +
					"<B>Document Status:</B>  The markup of the document is finished." +
					"<BR><B>Your Options:</B>" +
					"<BR>- Click <I>View Document</I> to inspect the finished document and make manual changes." +
					"<BR>- Click <I>Close Document</I> to close the document and make way for marking up another one." +
					"<BR>- Use the menus for additional options." +
					"</HTML>");
			this.mainLabel.validate();
		}
		
		private String prepareLabelString(String s, boolean upper) {
			return ((upper ? s.substring(0, 1).toUpperCase() : s.substring(0, 1).toLowerCase()) + s.substring(1));
		}
		
		boolean saveDocument(DocumentSaveOperation dso) {
			
			//	select save operration if none given
			if (dso == null) {
				DocumentSaver[] savers = this.parent.goldenGate.getDocumentSavers();
				String[] saverNames = new String[savers.length];
				StringVector choosableSaverNames = new StringVector();
				for (int s = 0; s < savers.length; s++) {
					JMenuItem mi = savers[s].getSaveDocumentMenuItem();
					if (mi == null) saverNames[s] = "";
					else {
						saverNames[s] = mi.getText();
						choosableSaverNames.addElement(saverNames[s]);
					}
				}
				Object o = JOptionPane.showInputDialog(this, "Please select how to save the document.", "Select Saving Method", JOptionPane.QUESTION_MESSAGE, null, choosableSaverNames.toStringArray(), null);
				for (int s = 0; s < savers.length; s++)
					if (saverNames[s].equals(o)) dso = savers[s].getSaveOperation(this.documentName, this.documentFormat);
			}
			
			//	indicate failure
			if (dso == null) return false;
			
			//	try to save if save operation given
			else try {
				String docName = dso.saveDocument(document);
				if (docName != null) {
					this.documentName = docName;
					this.documentFormat = dso.getDocumentFormat();
					this.documentSaveOperation = dso;
					this.contentModified = false;
				}
				return true;
			}
			catch (Exception e) {
				JOptionPane.showMessageDialog(this, e.getMessage(), "Exception Saving Document", JOptionPane.ERROR_MESSAGE);
				return false;
			}
			
			//	adjust availability of generic save item
			finally {
				this.saveItem.setEnabled(this.documentSaveOperation != null);
			}
		}
		
		boolean closeDocument() {
			if (this.contentModified) {
				int i = (JOptionPane.showConfirmDialog(DialogPanel.getTopWindow(), (this.documentName + " has been modified. Save Changes?"), "Save Changes", JOptionPane.YES_NO_CANCEL_OPTION));
				
				//	closing cancelled
				if ((i == JOptionPane.CANCEL_OPTION) || (i == JOptionPane.CLOSED_OPTION))
					return false;
				
				//	chosen to save, and saving cancelled or failed
				else if	((i == JOptionPane.YES_OPTION) && !this.saveDocument(this.documentSaveOperation))
					return false;
			}
			
			if (this.documentSaveOperation != null) {
				this.documentSaveOperation.documentClosed();
				this.documentSaveOperation = null;
			}
			
			return true;
		}
		
		void editDocument() {
			this.startNewUndoAction("Edit Document", false);
			DocumentEditDialog ded = new DocumentEditDialog(this.parent.goldenGate, "Edit Document", this.document);
			ded.setVisible(true);
			if (ded.isContentModified()) {
//				this.documentValidationResult = this.parent.validator.validate(this.document);
				this.currentStepName = null;
				this.processDisplay.validateDocument();
			}
		}
		
		private Dimension editDialogSize = new Dimension(800, 600);
		private Point editDialogLocation = null;
		
		private String[] taggedTypes = {};
		private String[] highlightTypes = {};
		
		private class DocumentEditDialog extends DocumentEditorDialog {
			DocumentEditDialog(GoldenGATE host, String title, MutableAnnotation data) {
				super(host, title, data);
				
				JButton okButton = new JButton("OK");
				okButton.setBorder(BorderFactory.createRaisedBevelBorder());
				okButton.setPreferredSize(new Dimension(100, 21));
				okButton.addActionListener(new ActionListener() {
					public void actionPerformed(ActionEvent ae) {
						DocumentEditDialog.this.documentEditor.writeChanges();
						DocumentEditDialog.this.dispose();
					}
				});
				this.mainButtonPanel.add(okButton);
				
				JButton cancelButton = new JButton("Cancel");
				cancelButton.setBorder(BorderFactory.createRaisedBevelBorder());
				cancelButton.setPreferredSize(new Dimension(100, 21));
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
				
				this.documentEditor.setShowExtensions(true);
				this.setSize(editDialogSize);
				if (editDialogLocation == null) this.setLocationRelativeTo(parent);
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
		
		
		
		
		
		private UndoAction undoAction = null;
		private UndoAction currentUndoAction = null;
		
		private class UndoAction implements CharSequenceListener, AnnotationListener {
			JMenuItem mi = null;
			
			UndoAction successor;
			UndoAction predecessor;
			
			final String name;
			final boolean isAutomated;
			
			private MutableAnnotation document;
			
			private LinkedList charEdits = new LinkedList();
			private TreeMap tokenAttributes = new TreeMap();
			
			private TreeMap originalAnnotations = new TreeMap();
			private boolean annotationsModified = false;
			
			UndoAction(String name, boolean isAutomated, MutableAnnotation document) {
				this.name = name;
				this.isAutomated = isAutomated;
				this.document = document;
				
				//	store token attributes
				for (int t = 0; t < this.document.size(); t++) {
					String[] tans = this.document.tokenAt(t).getAttributeNames();
					if (tans.length != 0) {
						Annotation ta = Gamta.newAnnotation(this.document, Token.TOKEN_ANNOTATION_TYPE, t, 1);
						ta.copyAttributes(this.document.tokenAt(t));
						this.tokenAttributes.put(new Integer(t), ta);
					}
				}
				
				//	store current annotations
				Annotation[] annotations = document.getAnnotations();
				for (int a = 0; a < annotations.length; a++) {
					Annotation original = Gamta.newAnnotation(document, annotations[a].getType(), annotations[a].getStartIndex(), annotations[a].size());
					original.copyAttributes(annotations[a]);
					original.setAttribute(Annotation.ANNOTATION_ID_ATTRIBUTE, annotations[a].getAnnotationID());
					this.originalAnnotations.put(getAnnotationKey(original), original);
				}
				
				//	start listening for changes
				this.document.addCharSequenceListener(this);
				this.document.addAnnotationListener(this);
			}
			private String getAnnotationKey(Annotation annotation) {
				return (annotation.getAnnotationID() + "-" + annotation.getType() + "-" + annotation.getStartIndex() + "-" + annotation.size());
			}
			
			public void charSequenceChanged(CharSequenceEvent change) {
				final int offset = change.offset;
				final CharSequence inserted = change.inserted;
				final CharSequence removed = change.removed;
				this.charEdits.addFirst(new Runnable() {
					public void run() {
						if (inserted.length() == 0)
							document.insertChars(removed, offset);
						else if (removed.length() == 0)
							document.removeChars(offset, inserted.length());
						else document.setChars(removed, offset, inserted.length());
					}
				});
			}
			public void annotationAdded(QueriableAnnotation doc, Annotation annotation) {
				this.annotationsModified = true;
			}
			public void annotationAttributeChanged(QueriableAnnotation doc, Annotation annotation, String attributeName, Object oldValue) {
				this.annotationsModified = true;
			}
			public void annotationRemoved(QueriableAnnotation doc, Annotation annotation) {
				this.annotationsModified = true;
			}
			public void annotationTypeChanged(QueriableAnnotation doc, Annotation annotation, String oldType) {
				this.annotationsModified = true;
			}
			
			JMenuItem getMenuItem() {
				if (this.mi == null) {
					
					String miLabel;
					if ("Edit Document".equals(this.name))
						miLabel = this.name;
					else if (this.isAutomated)
						miLabel = ("Automatically " + this.name);
					else miLabel = ("Manually " + this.name);
					
					this.mi = new JMenuItem(miLabel);
					this.mi.addActionListener(new ActionListener() {
						public void actionPerformed(ActionEvent ae) {
							runUndoAction(UndoAction.this);
						}
					});
				}
				return this.mi;
			}
			
			void undo() {
				this.undo(true);
			}
			private void undo(boolean isHead) {
				
				//	undo subsequent modifications
				if (this.successor != null)
					this.successor.undo(false);
				
				//	undo token sequence modifications
				for (Iterator cit = this.charEdits.iterator(); cit.hasNext();)
					((Runnable) cit.next()).run();
				
				//	discart modifications to prevent duplicate application
				this.charEdits.clear();
				
				//	restore token attributes
				for (int t = 0; t < this.document.size(); t++) {
					this.document.tokenAt(t).clearAttributes();
					Annotation ta = ((Annotation) this.tokenAttributes.get(new Integer(t)));
					if (ta != null)
						this.document.tokenAt(t).copyAttributes(ta);
				}
				
				//	restore annotations only if first in chain
				if (isHead) {
					
					//	remove/restore modified markup
					Annotation[] annotations = this.document.getAnnotations();
					for (int a = 0; a < annotations.length; a++) {
						Annotation original = ((Annotation) originalAnnotations.remove(getAnnotationKey(annotations[a])));
						
						//	this annotation did not exist before
						if (original == null)
							this.document.removeAnnotation(annotations[a]);
						
						//	annotation existed before, check attributes
						else {
							annotations[a].clearAttributes();
							annotations[a].copyAttributes(original);
						}
					}
					
					//	add remaining original markup
					for (Iterator ait = originalAnnotations.values().iterator(); ait.hasNext();) {
						Annotation original = ((Annotation) ait.next());
						Annotation restored = this.document.addAnnotation(original);
						restored.copyAttributes(original);
						restored.setAttribute(Annotation.ANNOTATION_ID_ATTRIBUTE, original.getAnnotationID());
					}
				}
			}
			
			void finish() {
				this.document.removeCharSequenceListener(this);
				this.document.removeAnnotationListener(this);
			}
			void setSuccessor(UndoAction successor) {
				this.successor = successor;
				successor.setPredecessor(this);
			}
			private void setPredecessor(UndoAction predecessor) {
				this.predecessor = predecessor;
			}
			
			boolean isModified() {
				return (this.annotationsModified || (this.charEdits.size() != 0));
			}
		}
		
		private void startNewUndoAction(String name, boolean isAutomated) {
			
			//	finish current undo action
			this.finishCurrentUndoAction();
			
			//	create new undo action
			this.currentUndoAction = new UndoAction(name, isAutomated, this.document);
			
			//	refresh undo menu
			this.refreshUndoMenu();
		}
		
		private void finishCurrentUndoAction() {
			if (this.currentUndoAction == null)
				return;
			
			this.currentUndoAction.finish();
			
			//	add to chain only if modified
			if (this.currentUndoAction.isModified()) {
				
				//	chain in
				if (this.undoAction != null)
					this.undoAction.setSuccessor(this.currentUndoAction);
				
				//	make new head of chain
				this.undoAction = this.currentUndoAction;
			}
			
			//	clear spot of active undo action
			this.currentUndoAction = null;
		}
		
		private void runUndoAction(UndoAction ua) {
			
			//	finish current undo action to put it in chain
			this.finishCurrentUndoAction();
			
			//	undo argument action and all successors
			ua.undo();
			
			//	cut undo action chain
			this.undoAction = ((ua.predecessor == null) ? null : ua.predecessor);
			if (this.undoAction != null)
				this.undoAction.successor = null;
			
			//	get status of document
//			this.documentValidationResult = this.parent.validator.validate(this.document);
			this.currentStepName = null;
			this.processDisplay.validateDocument();
			
			//	create new undo action for proceeding
			String uaName;
			if (ua.isAutomated) {
				if (ua.predecessor == null)
					uaName = "Correct Document"; // TODOne use step name (impossible to determine step name)
				else if ("Edit Document".equals(ua.predecessor.name)) {
					UndoAction pua = ua.predecessor;
					while ((pua != null) && "Edit Document".equals(pua.name))
						pua = pua.predecessor;
					uaName = ((pua == null) ? "Correct Document" : pua.name); // TODOne find & use step name
				}
				else if (ua.predecessor.isAutomated)
					uaName = ua.predecessor.name; // TODOne use step name
				else uaName = ua.predecessor.name;
			}
			else uaName = ua.name;
			this.startNewUndoAction(uaName, false);
			
			//	refresh undo menu
			this.refreshUndoMenu();
		}
		
		private void refreshUndoMenu() {
			this.undoMenu.removeAll();
			if (this.currentUndoAction != null)
				this.undoMenu.insert(this.currentUndoAction.getMenuItem(), 0);
			UndoAction ua = this.undoAction;
			while ((ua != null) && (this.undoMenu.getItemCount() < 10)) {
				this.undoMenu.insert(ua.getMenuItem(), 0);
				ua = ua.predecessor;
			}
			this.undoMenu.setEnabled(this.undoMenu.getItemCount() != 0);
		}
		
		
		
		
		
		void markUpDocument() {
			
			//	run document processors in extra thread so Swing's event dispatch thread is free for rendering dialogs, etc.
			Thread markupThread = new Thread() {
				public void run() {
					
					// remember what's already done in order to avoid loops
					Set dpHistory = new HashSet();
					
					//	prepare parameters
					Properties parameters = new Properties();
					parameters.setProperty(Resource.INTERACTIVE_PARAMETER, Resource.INTERACTIVE_PARAMETER);
					if (MarkupWizardPanel.this.parent.ggConfiguration.allowWebAccess())
						parameters.setProperty(ONLINE_PARAMETER, ONLINE_PARAMETER);
					
					//	go on as long as possible (return from the loop body to break)
					while (true) {
						
						//	get document status
						StepValidationResult currentStep = processDisplay.getCurrentStep();
						
						//	we're done
						if (currentStep == null) {
							JOptionPane.showMessageDialog(DialogPanel.getTopWindow(), ("Markup Wizard " + MarkupWizardPanel.this.parent.validator.getLabel() + " completed successfully."), "Markup Wizard Completed", JOptionPane.INFORMATION_MESSAGE);
							setFinished();
							return;
						}
						
						//	find next processor
						ProcessPartMapping mapping = MarkupWizardPanel.this.parent.markupWizard.mapper.getProcessPartMapping(currentStep.getValidator().getFullName());
						String dpName = ((mapping == null) ? null : mapping.documentProcessorName);
						
						System.out.println("Step is " + currentStep.getValidator().getFullName());
						System.out.println("DP Name is " + dpName);
						
						//	we don't have a processor to proceed with, or current processor has already been applied
						if ((dpName == null) || dpHistory.contains(dpName)) {
							
							//	start undo action for manual correction TODOne: add step name
							startNewUndoAction(currentStep.getValidator().getLabel(), false);
							setManually();
							return;
						}
						
						//	get next processor
						else {
							DocumentProcessor dp = MarkupWizardPanel.this.parent.goldenGate.getDocumentProcessorForName(dpName);
							
							//	invalid processor name
							if (dp == null) {
								
								//	start undo action for manual correction TODOne: add step name
								startNewUndoAction(currentStep.getValidator().getLabel(), false);
								setManually();
								return;
							}
							
							//	start undo action for DP TODOne: use step name
							startNewUndoAction(currentStep.getValidator().getLabel(), true);
							
							//	show splash screen
							ResourceSplashScreen splashScreen = new ResourceSplashScreen("Document Processor Running ...", "Please wait while '" + dp.getName() + "' is processing the document.");
							splashScreen.setLocationRelativeTo(MarkupWizardPanel.this);
							splashScreen.popUp();
							
							//	wait for splash screen to show
							while (!splashScreen.isVisible()) try {
								Thread.sleep(25);
							} catch (InterruptedException ie) {}
							
							//	apply next processor, and remember it
							try {
								dp.process(MarkupWizardPanel.this.document, parameters);
								dpHistory.add(dpName);
							}
							
							//	catch whatever may happen
							catch (Throwable t) {
								System.out.println("Error running document processor '" + dp.getName() + "': " + t.getClass().getName() + " (" + t.getMessage() + ")");
								t.printStackTrace(System.out);
								JOptionPane.showMessageDialog(splashScreen, ("Error running " + dp.getName() + ":\n" + t.getMessage()), "Error Running Document Processor", JOptionPane.ERROR_MESSAGE);
								setManually();
								return;
							}
							
							//	clean up
							finally {
								
								//	dispose splash screen (not before it's showing, though)
								while (!splashScreen.isVisible()) try {
									Thread.sleep(25);
								} catch (InterruptedException ie) {}
								splashScreen.dispose();
								
								//	refresh validation and display
								MarkupWizardPanel.this.processDisplay.validateDocument();
							}
						}
					}
				}
			};
			markupThread.start();
		}
		
		private class GgMwValidationResultPanel extends ValidationResultPanel {
			
			private Dimension editDialogSize = new Dimension(800, 600);
			private Point editDialogLocation = null;
			
			private Dimension attributeDialogSize = new Dimension(400, 300);
			private Point attributeDialogLocation = null;
			
			GgMwValidationResultPanel() {
				super(document, MarkupWizardPanel.this.parent.validator);
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
				
				if (covered > uncovered) {
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
				
				else content = document.addAnnotation(TEMP_ANNOTATION_TYPE, annotationTrays[0].annotation.getStartIndex(), (annotationTrays[annotationTrays.length - 1].annotation.getEndIndex() - annotationTrays[0].annotation.getStartIndex()));
				
				//	create dialog & show
				DocumentEditDialog ded = new DocumentEditDialog("Edit Annotation", content);
				ded.setVisible(true);
				
				//	remove temp annotation
				if (TEMP_ANNOTATION_TYPE.equals(content.getType()))
					document.removeAnnotation(content);
				
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
					super(MarkupWizardPanel.this.parent.goldenGate, title, content);
					
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
				AttributeEditorDialog aed = new AttributeEditorDialog("Edit Annotation Attributes", annotation, document) {
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
}
