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
package de.uka.ipd.idaho.goldenGate.markupWizard.dmc.client;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ItemEvent;
import java.awt.event.ItemListener;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.BorderFactory;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JDialog;
import javax.swing.JLabel;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;

import de.uka.ipd.idaho.gamta.util.validation.Criterion;
import de.uka.ipd.idaho.gamta.util.validation.Level;
import de.uka.ipd.idaho.gamta.util.validation.Process;
import de.uka.ipd.idaho.gamta.util.validation.Step;
import de.uka.ipd.idaho.gamta.util.validation.Task;
import de.uka.ipd.idaho.gamta.util.validation.editor.MarkupProcessEditorExtension;
import de.uka.ipd.idaho.gamta.util.validation.editor.MarkupProcessPartEditor;
import de.uka.ipd.idaho.gamta.util.validation.editor.MarkupProcessPartEditorExtension;
import de.uka.ipd.idaho.gamta.util.validation.editor.MarkupStepEditor;
import de.uka.ipd.idaho.goldenGate.markupProcess.MarkupProcessManager;
import de.uka.ipd.idaho.goldenGate.markupProcess.MarkupProcessManager.ExtensibleMarkupProcessEditor;
import de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager;
import de.uka.ipd.idaho.goldenGate.util.DataListListener;
import de.uka.ipd.idaho.goldenGate.util.DialogPanel;
import de.uka.ipd.idaho.goldenGate.util.ResourceDialog;
import de.uka.ipd.idaho.goldenGateServer.uaa.client.AuthenticatedClient;
import de.uka.ipd.idaho.goldenGateServer.uaa.client.AuthenticationManagerPlugin;
import de.uka.ipd.idaho.stringUtils.StringVector;

/**
 * Manager for the GoldenGATE Document Markup Coordinator, in particular for
 * creating and updating markup process definitions the DMC runs on, using
 * GoldenGATE Editor.
 * 
 * @author sautter
 */
public class DmcProcessManager extends AbstractResourceManager {
	
	private static final String FILE_EXTENSION = ".dmcProcess";
	
	private MarkupProcessManager mpManager = null;
	
	private AuthenticationManagerPlugin authManager = null;
	
	private AuthenticatedClient authClient = null;
	private GoldenGateDmcClient dmcClient = null;
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#init()
	 */
	public void init() {
		this.mpManager = ((MarkupProcessManager) this.parent.getPlugin(MarkupProcessManager.class.getName()));
		this.authManager = ((AuthenticationManagerPlugin) this.parent.getPlugin(AuthenticationManagerPlugin.class.getName()));
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#isOperational()
	 */
	public boolean isOperational() {
		return (super.isOperational() && (this.mpManager != null) && (this.authManager != null));
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractResourceManager#getFileExtension()
	 */
	protected String getFileExtension() {
		return FILE_EXTENSION;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#getPluginName()
	 */
	public String getPluginName() {
		return "DMC Process Manager";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.ResourceManager#getResourceTypeLabel()
	 */
	public String getResourceTypeLabel() {
		return "DMC Process";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#getMainMenuTitle()
	 */
	public String getMainMenuTitle() {
		return "DMC Process Manager";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#getMainMenuItems()
	 */
	public JMenuItem[] getMainMenuItems() {
		ArrayList collector = new ArrayList();
		JMenuItem mi;
		
		mi = new JMenuItem("Create");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				createDmcProcess();
			}
		});
		collector.add(mi);
		
		mi = new JMenuItem("Edit");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				editDmcProcesss();
			}
		});
		collector.add(mi);
		
		return ((JMenuItem[]) collector.toArray(new JMenuItem[collector.size()]));
	}
	
	/*
	 * @see de.uka.ipd.idaho.goldenGate.plugins.ResourceManager#getRequiredResourceNames(java.lang.String, boolean)
	 */
	public String[] getRequiredResourceNames(String name, boolean recourse) {
		DmcProcess dmcp = this.loadDmcProcess(name);
		if (dmcp == null)
			return new String[0];
		String[] rrns = {dmcp.processName};
		return rrns;
	}
	
	private DmcProcess loadDmcProcess(String name) {
		StringVector filter = this.loadListResource(name);
		if ((filter == null) || filter.isEmpty())
			return null;
		
		String mpName = filter.get(0);
		return new DmcProcess(mpName, filter);
	}
	
	private boolean storeDmcProcess(String name, DmcProcess dmcp) throws IOException {
		StringVector filter = new StringVector();
		filter.addElement(dmcp.processName);
		filter.addContent(dmcp.filter);
		return this.storeListResource(name, filter);
	}
	
	private boolean createDmcProcess() {
		return (this.createDmcProcess(null, null) != null);
	}
	
	private boolean cloneDmcProcess() {
		String selectedName = this.resourceNameList.getSelectedName();
		if (selectedName == null)
			return this.createDmcProcess();
		else {
			String name = "New " + selectedName;
			return (this.createDmcProcess(this.loadDmcProcess(selectedName), name) != null);
		}
	}
	
	private String createDmcProcess(DmcProcess modelDmcProcess, String name) {
		CreateDmcProcessDialog cpd = new CreateDmcProcessDialog(modelDmcProcess, name);
		cpd.setVisible(true);
		
		if (cpd.isCommitted()) {
			DmcProcess dmcp = cpd.getDmcProcess();
			String dmcpName = cpd.getDmcProcessName();
			if (!dmcpName.endsWith(FILE_EXTENSION)) dmcpName += FILE_EXTENSION;
			try {
				if (this.storeDmcProcess(dmcpName, dmcp)) {
					this.resourceNameList.refresh();
					return dmcpName;
				}
			} catch (IOException e) {}
		}
		return null;
	}
	
	private void editDmcProcesss() {
		final DmcProcessEditorPanel[] editor = new DmcProcessEditorPanel[1];
		editor[0] = null;
		
		final DialogPanel editDialog = new DialogPanel("Edit DMC Processes", true);
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
						storeDmcProcess(editor[0].name, editor[0].getDmcProcess());
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
				createDmcProcess();
			}
		});
		editButtons.add(button);
		button = new JButton("Clone");
		button.setBorder(BorderFactory.createRaisedBevelBorder());
		button.setPreferredSize(new Dimension(100, 21));
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				cloneDmcProcess();
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
		button = new JButton("Update DMC");
		button.setBorder(BorderFactory.createRaisedBevelBorder());
		button.setPreferredSize(new Dimension(100, 21));
		button.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				updateDmc(resourceNameList.getSelectedName());
			}
		});
		editButtons.add(button);
		editDialog.add(editButtons, BorderLayout.NORTH);
		
		final JPanel editorPanel = new JPanel(new BorderLayout());
		String selectedName = this.resourceNameList.getSelectedName();
		if (selectedName == null)
			editorPanel.add(this.getExplanationLabel(), BorderLayout.CENTER);
		else {
			DmcProcess markupWizzard = this.loadDmcProcess(selectedName);
			if (markupWizzard == null)
				editorPanel.add(this.getExplanationLabel(), BorderLayout.CENTER);
			else {
				editor[0] = new DmcProcessEditorPanel(selectedName, markupWizzard);
				editorPanel.add(editor[0], BorderLayout.CENTER);
			}
		}
		editDialog.add(editorPanel, BorderLayout.CENTER);
		
		editDialog.add(this.resourceNameList, BorderLayout.EAST);
		DataListListener dll = new DataListListener() {
			public void selected(String dataName) {
				if ((editor[0] != null) && editor[0].isDirty()) {
					try {
						storeDmcProcess(editor[0].name, editor[0].getDmcProcess());
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
					DmcProcess markupWizzard = loadDmcProcess(dataName);
					if (markupWizzard == null)
						editorPanel.add(getExplanationLabel(), BorderLayout.CENTER);
					else {
						editor[0] = new DmcProcessEditorPanel(dataName, markupWizzard);
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
	
	private void updateDmc(String dmcProcessName) {
		DmcProcess dmcp = this.loadDmcProcess(dmcProcessName);
		
		//	connected to server, upload configuration
		if ((dmcp != null) && this.ensureLoggedIn()) try {
			this.dmcClient.updateProcess(dmcp.getProjectedProcess());
			JOptionPane.showMessageDialog(DialogPanel.getTopWindow(), "GoldenGATE Document Markup Coordinator updated successfully.", "GoldenGATE DMC Updated", JOptionPane.INFORMATION_MESSAGE);
		}
		catch (IOException ioe) {
			JOptionPane.showMessageDialog(DialogPanel.getTopWindow(), ("An error occurred while updating GoldenGATE Document Markup Coordinator:\n" + ioe.getMessage()), "Error Updating DMC", JOptionPane.ERROR_MESSAGE);
		}
	}
	
	private boolean ensureLoggedIn() {
		
		//	test if connection alive
		if (this.authClient != null) {
			try {
				//	test if connection alive
				if (this.authClient.ensureLoggedIn())
					return true;
				
				//	connection dead (eg a session timeout), make way for re-getting from auth manager
				else {
					this.dmcClient = null;
					this.authClient = null;
				}
			}
			
			//	server temporarily unreachable, re-login will be done by auth manager
			catch (IOException ioe) {
				this.dmcClient = null;
				this.authClient = null;
				return false;
			}
		}
		
		//	got no valid connection at the moment
		if (this.authClient == null)
			this.authClient = this.authManager.getAuthenticatedClient();
		
		//	authentication failed
		if (this.authClient == null) return false;
		
		//	got valid connection
		else {
			this.dmcClient = new GoldenGateDmcClient(this.authClient);
			return true;
		}
	}
	
	private class CreateDmcProcessDialog extends DialogPanel {
		
		private JTextField nameField;
		
		private DmcProcessEditorPanel editor;
		private String markupWizardName = null;
		
		CreateDmcProcessDialog(DmcProcess mws, String name) {
			super("Create DmcProcess", true);
			
			this.nameField = new JTextField((name == null) ? "New DmcProcess" : name);
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
			this.editor = new DmcProcessEditorPanel(name, mws);
			
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
		
		DmcProcess getDmcProcess() {
			return this.editor.getDmcProcess();
		}
		
		String getDmcProcessName() {
			return this.markupWizardName;
		}
	}
	
	private class DmcProcessEditorPanel extends JPanel {
		
		private String name;
		private String processName;
		private JLabel processLabel;
		private boolean dirty = false;
		private ExtensibleMarkupProcessEditor processEditor;
		private StringVector filter;
		private StepSelectorExtension mappingExtension;
		
		DmcProcessEditorPanel(String name, DmcProcess dmcp) {
			super(new BorderLayout(), true);
			this.name = name;
			this.processName = ((dmcp == null) ? null : dmcp.processName);
			
			this.filter = new StringVector();
			if (dmcp != null)
				this.filter.addContent(dmcp.filter);
			
			this.mappingExtension = new StepSelectorExtension(this.filter);
			this.processEditor = mpManager.getMarkupProcessEditor(this.processName, (this.processName == null), this.mappingExtension);
			
			this.processLabel = new JLabel(("Markup Process: " + ((dmcp == null) ? "<double click to select markup process>" : (dmcp.processName + " (double click to change)"))), JLabel.LEFT);
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
			StepSelectorExtension.StepSelector[] pss = this.mappingExtension.getConcreteInstances();
			for (int p = 0; p < pss.length; p++)
				if (pss[p].isDirty())
					return true;
			return false;
		}
		
		DmcProcess getDmcProcess() {
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
			
			StringVector filter = new StringVector();
			String ppName;
			Level[] levels = process.getLevels();
			for (int l = 0; l < levels.length; l++) {
				ppName = levels[l].getFullName();
				if (this.filter.contains(ppName))
					filter.addElementIgnoreDuplicates(ppName);
				
				Task[] tasks = levels[l].getTasks();
				for (int t = 0; t < tasks.length; t++) {
					ppName = tasks[t].getFullName();
					if (this.filter.contains(ppName))
						filter.addElementIgnoreDuplicates(ppName);
					
					Step[] steps = tasks[t].getSteps();
					for (int s = 0; s < steps.length; s++) {
						ppName = steps[s].getFullName();
						if (this.filter.contains(ppName))
							filter.addElementIgnoreDuplicates(ppName);
						
						Criterion[] criterions = steps[s].getCriterions();
						for (int c = 0; c < criterions.length; c++) {
							ppName = criterions[c].getFullName();
							if (this.filter.contains(ppName))
								filter.addElementIgnoreDuplicates(ppName);
						}
					}
				}
			}
			
			return new DmcProcess(processName, filter);
		}
	}
	
	private class StepSelectorExtension extends MarkupProcessEditorExtension {
		StringVector filter;
		
		StepSelectorExtension(StringVector filter) {
			this.filter = filter;
		}
		
		protected MarkupProcessPartEditorExtension produceExtension(MarkupProcessPartEditor ppe) {
			return ((ppe instanceof MarkupStepEditor) ? new StepSelector(ppe) : null);
		}
		
		StepSelector[] getConcreteInstances() {
			MarkupProcessPartEditorExtension[] instances = super.getInstances();
			StepSelector[] concreteInstances = new StepSelector[instances.length];
			for (int i = 0; i < instances.length; i++)
				concreteInstances[i] = ((StepSelector) instances[i]);
			return concreteInstances;
		}
		
		class StepSelector extends MarkupProcessPartEditorExtension {
			private JCheckBox selected;
			private boolean dirty = false;
			StepSelector(final MarkupProcessPartEditor host) {
				super(host);
				this.selected = new JCheckBox("Use this step in Document Markup Coordinator?", filter.contains(host.getProcessPartFullName()));
				this.selected.addItemListener(new ItemListener() {
					public void itemStateChanged(ItemEvent ie) {
						dirty = true;
						if (selected.isSelected())
							filter.addElementIgnoreDuplicates(host.getProcessPartFullName());
						else filter.removeAll(host.getProcessPartFullName());
					}
				});
				this.add(this.selected, BorderLayout.CENTER);
			}
			boolean isDirty() {
				return this.dirty;
			}
		}
	}
	
	private class DmcProcess {
		final String processName;
		final StringVector filter;
		DmcProcess(String processName, StringVector mapper) {
			this.processName = processName;
			this.filter = mapper;
		}
		Process getProjectedProcess() {
			Process process = mpManager.getMarkupProcess(this.processName);
			if (process == null)
				return null;
			return new Process(process.getName(), process.getLabel(), process.getDescription(), process.getErrorDescription(), this.getProjectedLevels(process));
		}
		private Level[] getProjectedLevels(Process process) {
			Level[] levels = process.getLevels();
			ArrayList collector = new ArrayList();
			for (int l = 0; l < levels.length; l++) {
				Task[] tasks = this.getProjectedTasks(levels[l]);
				if (tasks.length != 0)
					collector.add(new Level(process, levels[l].getName(), levels[l].getLabel(), levels[l].getDescription(), levels[l].getErrorDescription(), tasks));
			}
			return ((Level[]) collector.toArray(new Level[collector.size()]));
		}
		private Task[] getProjectedTasks(Level level) {
			Task[] tasks = level.getTasks();
			ArrayList collector = new ArrayList();
			for (int t = 0; t < tasks.length; t++) {
				Step[] steps = this.getProjectedSteps(tasks[t]);
				if (steps.length != 0)
					collector.add(new Task(level, tasks[t].getName(), tasks[t].getLabel(), tasks[t].getDescription(), tasks[t].getErrorDescription(), steps));
			}
			return ((Task[]) collector.toArray(new Task[collector.size()]));
		}
		private Step[] getProjectedSteps(Task task) {
			Step[] steps = task.getSteps();
			ArrayList collector = new ArrayList();
			for (int s = 0; s < steps.length; s++) {
				if (this.filter.contains(steps[s].getFullName()))
					collector.add(new Step(task, steps[s].getName(), steps[s].getLabel(), steps[s].getDescription(), steps[s].getErrorDescription(), steps[s].getContextPath(), steps[s].getCriterions()));
			}
			return ((Step[]) collector.toArray(new Step[collector.size()]));
		}
	}
}
