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
package de.uka.ipd.idaho.goldenGate.markupProcess;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;

import de.uka.ipd.idaho.gamta.QueriableAnnotation;
import de.uka.ipd.idaho.gamta.util.validation.Level;
import de.uka.ipd.idaho.gamta.util.validation.Process;
import de.uka.ipd.idaho.gamta.util.validation.editor.MarkupProcessEditorExtension;
import de.uka.ipd.idaho.gamta.util.validation.editor.MarkupProcessEditorHost;
import de.uka.ipd.idaho.gamta.util.validation.editor.MarkupProcessEditorPanel;
import de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager;
import de.uka.ipd.idaho.goldenGate.util.DataListListener;
import de.uka.ipd.idaho.goldenGate.util.DialogPanel;
import de.uka.ipd.idaho.htmlXmlUtil.Parser;
import de.uka.ipd.idaho.htmlXmlUtil.TreeNode;

/**
 * Manager for markup process resources inside the GoldenGATE Editor. This
 * plugin's createMarkupProcess() and editMarkupProcess() methods can be invoked
 * by other plugins that manage resources built on markup processes, allowing
 * invokers to specify extensions specific to their own resources.
 * 
 * @author sautter
 */
public class MarkupProcessManager extends AbstractResourceManager {
	
	private static final String FILE_EXTENSION = ".markupProcess";
	
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
		return "Markup Process";
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
				createMarkupProcess();
			}
		});
		collector.add(mi);
		mi = new JMenuItem("Edit");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				editMarkupProcesses();
			}
		});
		collector.add(mi);
		return ((JMenuItem[]) collector.toArray(new JMenuItem[collector.size()]));
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#getMainMenuTitle()
	 */
	public String getMainMenuTitle() {
		return "Markup Processes";
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getToolsMenuLabel()
	 */
	public String getToolsMenuLabel() {
		return "Apply";
	}
	
	private static final Parser parser = new Parser();
	
	/**
	 * Retrieve a markup process definition.
	 * @param name the name of the markup process
	 * @return the markup process with the specified name, or null, if there is
	 *         no such markup process
	 */
	public Process getMarkupProcess(String name) {
		if (name == null)
			return null;
		
		else if (this.markupProcessCache.containsKey(name))
			return ((Process) this.markupProcessCache.get(name));
		
		InputStreamReader isr = null;
		try {
			isr = new InputStreamReader(this.dataProvider.getInputStream(name), "UTF-8");
			TreeNode pRoot = parser.parse(isr);
			if (TreeNode.ROOT_NODE_TYPE.equals(pRoot.getNodeType()))
				pRoot = pRoot.getChildNode(Process.PROCESS, 0);
			
			Process process = new Process(pRoot);
			this.markupProcessCache.put(name, process);
			return process;
		}
		catch (IOException ioe) {
			System.out.println("MarkupProcessManager: Could not load markup process - " + ioe.getMessage());
			ioe.printStackTrace(System.out);
			return null;
		}
		finally {
			if (isr != null) try {
				isr.close();
			} catch (IOException ioe) {}
		}
	}
	
	private boolean storeMarkupProcess(String name, Process markupProcess) throws IOException {
		OutputStreamWriter osw = null;
		try {
			osw = new OutputStreamWriter(this.dataProvider.getOutputStream(name), "UTF-8");
			markupProcess.writeXml(osw);
			osw.flush();
			osw.close();
			this.markupProcessCache.put(name, markupProcess);
			return true;
		}
		finally {
			if (osw != null)
				osw.close();
		}
	}
	
	private Map markupProcessCache = new HashMap();
	
	/**
	 * Retrieve a panel for editing an existin markup process, cloning one, or
	 * creating a new markup process. To edit an existing markup process,
	 * specify its name and set 'clone' to false. To clone an existing markup
	 * process, specify the name of the process to clone and set 'clone' to
	 * true. To create a new markup process, specify null as the name and set
	 * 'clone' to true. In any case, to save any changes made to an existing or
	 * newlay created/cloned markup process, you have to invoke the
	 * saveMarkupProcess() method of the returned editing panel.
	 * @param name the name of the markup process to get an editor for
	 * @param clone clone the markup process with the specified name?
	 * @param extension a markup process editor extension to include in the
	 *            editing panel
	 * @return an editing panel containing the markup process to edit
	 */
	public ExtensibleMarkupProcessEditor getMarkupProcessEditor(String name, boolean clone, MarkupProcessEditorExtension extension) {
		Process process = this.getMarkupProcess(name);
		if (clone) {
			if (process == null)
				process = new Process("NewMarkupProcess", "(enter nice name)", "(enter description)", "(enter error message)", new Level[0]);
			else process = new Process(("New " + process.getName()), process.getLabel(), process.getDescription(), process.getErrorDescription(), process.getLevels());
		}
		else if (process == null)
			return null;
		
		return new ExtensibleMarkupProcessEditor(name, process, !clone, extension, MarkupProcessManager.this);
	}
	
	private boolean createMarkupProcess() {
		return this.createMarkupProcess(null, null, null);
	}
	
	private boolean cloneMarkupProcess() {
		String selectedResourceName = this.resourceNameList.getSelectedName();
		Process process = ((selectedResourceName == null) ? null : this.getMarkupProcess(selectedResourceName));
		if (process == null)
			return this.createMarkupProcess();
		else {
			Process cloneProcess = new Process(("New " + process.getName()), process.getLabel(), process.getDescription(), process.getErrorDescription(), process.getLevels());
			String cloneResourceName = "New " + selectedResourceName;
			return this.createMarkupProcess(cloneProcess, cloneResourceName, null);
		}
	}
	
	private boolean createMarkupProcess(Process modelMarkupProcess, String resourceName, MarkupProcessEditorExtension extension) {
		MarkupProcessEditorDialog cpd = new MarkupProcessEditorDialog(modelMarkupProcess, resourceName, false, extension);
		cpd.setVisible(true);
		
		if (cpd.isCommitted()) {
			this.resourceNameList.refresh();
			return true;
		}
		else return false;
	}
	
	private void editMarkupProcesses() {
		final ExtensibleMarkupProcessEditor[] editor = new ExtensibleMarkupProcessEditor[1];
		editor[0] = null;
		
		final DialogPanel editDialog = new DialogPanel("Edit Markup Processes", true);
		editDialog.setDefaultCloseOperation(JDialog.DO_NOTHING_ON_CLOSE);
		editDialog.addWindowListener(new WindowAdapter() {
			public void windowClosed(WindowEvent we) {
				this.closeDialog();
			}
			public void windowClosing(WindowEvent we) {
				this.closeDialog();
			}
			private void closeDialog() {
				if ((editor[0] != null) && editor[0].processEditor.isDirty()) {
					try {
						storeMarkupProcess(editor[0].resourceName, editor[0].processEditor.getProcess());
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
				createMarkupProcess();
			}
		});
		editButtons.add(button);
		button = new JButton("Clone");
		button.setBorder(BorderFactory.createRaisedBevelBorder());
		button.setPreferredSize(new Dimension(100, 21));
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				cloneMarkupProcess();
			}
		});
		editButtons.add(button);
		button = new JButton("Delete");
		button.setBorder(BorderFactory.createRaisedBevelBorder());
		button.setPreferredSize(new Dimension(100, 21));
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				String deleteName = resourceNameList.getSelectedName();
				if (deleteResource(deleteName)) {
					markupProcessCache.remove(deleteName);
					resourceNameList.refresh();
				}
			}
		});
		editButtons.add(button);
		
		editDialog.add(editButtons, BorderLayout.NORTH);
		
		final JPanel editorPanel = new JPanel(new BorderLayout());
		String selectedName = this.resourceNameList.getSelectedName();
		if (selectedName == null)
			editorPanel.add(this.getExplanationLabel(), BorderLayout.CENTER);
		else {
			Process markupProcess = this.getMarkupProcess(selectedName);
			if (markupProcess == null)
				editorPanel.add(this.getExplanationLabel(), BorderLayout.CENTER);
			else {
				editor[0] = new ExtensibleMarkupProcessEditor(selectedName, markupProcess, true, null, this);
				editorPanel.add(editor[0].processEditor, BorderLayout.CENTER);
			}
		}
		editDialog.add(editorPanel, BorderLayout.CENTER);
		
		editDialog.add(this.resourceNameList, BorderLayout.EAST);
		DataListListener dll = new DataListListener() {
			public void selected(String dataName) {
				if ((editor[0] != null) && editor[0].processEditor.isDirty()) {
					try {
						String oldName = editor[0].resourceName;
						String newName = editor[0].saveMarkupProcess();
						if (!oldName.equals(newName)) {
							editor[0] = null;
							resourceNameList.refresh();
						}
					}
					catch (IOException ioe) {
						if (JOptionPane.showConfirmDialog(editDialog, (ioe.getClass().getName() + " (" + ioe.getMessage() + ")\nwhile saving file to " + editor[0].resourceName + "\nProceed?"), "Could Not Save Analyzer", JOptionPane.YES_NO_OPTION) == JOptionPane.NO_OPTION) {
							resourceNameList.setSelectedName(editor[0].resourceName);
							editorPanel.validate();
							return;
						}
					}
				}
				editorPanel.removeAll();
				
				if (dataName == null)
					editorPanel.add(getExplanationLabel(), BorderLayout.CENTER);
				else {
					Process markupProcess = getMarkupProcess(dataName);
					if (markupProcess == null)
						editorPanel.add(getExplanationLabel(), BorderLayout.CENTER);
					else {
						editor[0] = new ExtensibleMarkupProcessEditor(dataName, markupProcess, true, null, MarkupProcessManager.this);
						editorPanel.add(editor[0].processEditor, BorderLayout.CENTER);
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
	
	private class MarkupProcessEditorDialog extends DialogPanel {
		
		private ExtensibleMarkupProcessEditor editor;
		private String markupProcessName = null;
		
		MarkupProcessEditorDialog(Process markupProcess, String resourceName, boolean isEditing, MarkupProcessEditorExtension extension) {
			super((isEditing ? ("Edit Markup Process '" + markupProcess.getName() + "'") : "Create Markup Process"), true);
			
			//	initialize main buttons
			JButton commitButton = new JButton("OK");
			commitButton.setBorder(BorderFactory.createRaisedBevelBorder());
			commitButton.setPreferredSize(new Dimension(100, 21));
			commitButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					try {
						markupProcessName = editor.saveMarkupProcess();
					}
					catch (IOException ioe) {
						ioe.printStackTrace(System.out);
					}
					dispose();
				}
			});
			
			JButton abortButton = new JButton("Cancel");
			abortButton.setBorder(BorderFactory.createRaisedBevelBorder());
			abortButton.setPreferredSize(new Dimension(100, 21));
			abortButton.addActionListener(new ActionListener() {
				public void actionPerformed(ActionEvent ae) {
					dispose();
				}
			});
			
			JPanel mainButtonPanel = new JPanel();
			mainButtonPanel.setLayout(new FlowLayout());
			mainButtonPanel.add(commitButton);
			mainButtonPanel.add(abortButton);
			
			//	initialize editor
			this.editor = new ExtensibleMarkupProcessEditor(resourceName, markupProcess, isEditing, extension, MarkupProcessManager.this);
			
			//	put the whole stuff together
			this.getContentPane().setLayout(new BorderLayout());
			this.getContentPane().add(this.editor.processEditor, BorderLayout.CENTER);
			this.getContentPane().add(mainButtonPanel, BorderLayout.SOUTH);
			
			this.setResizable(true);
			this.setSize(new Dimension(1000, 700));
			this.setLocationRelativeTo(DialogPanel.getTopWindow());
		}
		
		boolean isCommitted() {
			return (this.markupProcessName != null);
		}
//		
//		String getMarkupProcessName() {
//			return this.markupProcessName;
//		}
	}
	
	/**
	 * Markup process editor panel for external use.
	 * 
	 * @author sautter
	 */
	public static class ExtensibleMarkupProcessEditor {
		String resourceName;
		boolean checkNameChange;
		MarkupProcessEditorPanel processEditor;
		MarkupProcessManager host;
		ExtensibleMarkupProcessEditor(final String resourceName, Process markupProcess, boolean checkNameChange, MarkupProcessEditorExtension extension, final MarkupProcessManager host) {
			this.resourceName = resourceName;
			this.checkNameChange = checkNameChange;
			this.host = host;
			this.processEditor = new MarkupProcessEditorPanel(new MarkupProcessEditorHost() {
				public QueriableAnnotation getStartDocument() {
					return host.parent.getActiveDocument();
				}
				public QueriableAnnotation getGoldDocument() {
					return null;
				}
			}, extension) {
				protected Process[] getImportablePartSources() {
					String[] processNames = host.getResourceNames();
					ArrayList processList = new ArrayList();
					for (int p = 0; p < processNames.length; p++)
						if (!processNames[p].equals(resourceName)) {
							Process process = host.getMarkupProcess(processNames[p]);
							if (process != null)
								processList.add(process);
						}
					return ((Process[]) processList.toArray(new Process[processList.size()]));
				}
			};
			this.processEditor.setProcess((markupProcess == null) ? new Process("NewMarkupProcess", "(enter nice name)", "(enter description)", "(enter error message)", new Level[0]) : markupProcess);
		}
		
		/**
		 * Retrieve the actual editing panel to integrate it in a UI. This
		 * method actually returns a MarkupProcessEditorPanel instance, but the
		 * return type is JPanel to indicate that this method is merely meant
		 * for UI integration, not for manipulating the actual process editor.
		 * @return the actual process editing panel
		 */
		public JPanel getEditorPanel() {
			return this.processEditor;
		}
		
		/**
		 * Save the markup process in this editor. If the name of the markup
		 * process entered in the name field does not end with the appropriate
		 * file extension, the resource name of the markup process will be
		 * extended with this file extension. The returned string is the
		 * resource name the markup process can be retrieved with from the
		 * markup process mananger.
		 * @return the resource name the markup process was saved with
		 */
		public String saveMarkupProcess() throws IOException {
			Process process = this.processEditor.getProcess();
			String name = process.getName();
			if (!name.endsWith(FILE_EXTENSION))
				name = (name + FILE_EXTENSION);
			if (this.processEditor.isDirty()) {
				if (this.checkNameChange && !name.equals(this.resourceName) && (JOptionPane.showConfirmDialog(DialogPanel.getTopWindow(), ("The name of the markup process has changed from '" + this.resourceName + "' to '" + name + "'.\nClick YES to save a copy of the markup process under the new name, and NO to save the markup process under its original name."), "Markup Process Name Changed", JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE) != JOptionPane.YES_OPTION))
					name = this.resourceName;
				this.host.storeMarkupProcess(name, process);
			}
			return name;
		}
	}
}
