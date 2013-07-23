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
package de.uka.ipd.idaho.goldenGate.markupWizard.dps.client;

import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;

import javax.swing.JMenuItem;
import javax.swing.JOptionPane;

import de.uka.ipd.idaho.goldenGate.configuration.AbstractConfigurationManager;
import de.uka.ipd.idaho.goldenGate.configuration.ConfigurationUtils.Configuration;
import de.uka.ipd.idaho.goldenGate.configuration.ConfigurationUtils.SpecialDataHandler;
import de.uka.ipd.idaho.goldenGate.markupWizard.MarkupWizard;
import de.uka.ipd.idaho.goldenGate.markupWizard.manager.MarkupWizardConfigurationManager;
import de.uka.ipd.idaho.goldenGate.markupWizard.manager.MarkupWizardManager;
import de.uka.ipd.idaho.goldenGate.util.DialogPanel;
import de.uka.ipd.idaho.goldenGate.util.ResourceDialog;
import de.uka.ipd.idaho.goldenGateServer.uaa.client.AuthenticatedClient;
import de.uka.ipd.idaho.goldenGateServer.uaa.client.AuthenticationManagerPlugin;

/**
 * Manager for the GoldenGATE Document Processing Server, in particular for
 * creating and updating markup wizard definitions the DPS runs on, using
 * GoldenGATE Editor. In particular, this plugin allows for uploading a markup
 * wizard hosted by MarkupWizardManager to DPS. This includes uploading the
 * markup process definition and the dpcument processor mapping to DPS. The
 * backing GoldenGATE configuration has to be made available in another way,
 * e.g. by another configuration exporter using the export definition available
 * from this one.
 * 
 * @author sautter
 */
public class DpsConfigurationManager extends AbstractConfigurationManager {
	
	private MarkupWizardManager mwManager = null;
	
	private AuthenticationManagerPlugin authManager = null;
	
	private AuthenticatedClient authClient = null;
	private GoldenGateDpsClientAuth dpsClient = null;
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#init()
	 */
	public void init() {
		
		//	get markup wizard manager
		this.mwManager = ((MarkupWizardManager) this.parent.getPlugin(MarkupWizardManager.class.getName()));
		
		//	get authentication manager
		this.authManager = ((AuthenticationManagerPlugin) this.parent.getPlugin(AuthenticationManagerPlugin.class.getName()));
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#isOperational()
	 */
	public boolean isOperational() {
		return (super.isOperational() && (this.mwManager != null) && (this.authManager != null));
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#getPluginName()
	 */
	public String getPluginName() {
		return "DPS Configuration Manager";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#getMainMenuTitle()
	 */
	public String getMainMenuTitle() {
		return "DPS Manager";
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.plugins.AbstractGoldenGatePlugin#getMainMenuItems()
	 */
	public JMenuItem[] getMainMenuItems() {
		ArrayList collector = new ArrayList();
		JMenuItem mi;
		
		mi = new JMenuItem("Update DPS");
		mi.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent ae) {
				updateDps();
			}
		});
		collector.add(mi);
		return ((JMenuItem[]) collector.toArray(new JMenuItem[collector.size()]));
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.configuration.AbstractConfigurationManager#getExportNames()
	 */
	protected String[] getExportNames() {
		return new String[0]; // we're simply after the configurations provided by MarkupWizardConfigurationManager ...
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.configuration.AbstractConfigurationManager#doExport(java.lang.String, de.uka.ipd.idaho.goldenGate.configuration.ConfigurationUtils.SpecialDataHandler, de.uka.ipd.idaho.goldenGate.configuration.ConfigurationUtils.Configuration, java.io.File, de.uka.ipd.idaho.goldenGate.configuration.AbstractConfigurationManager.ExportStatusDialog)
	 */
	protected boolean doExport(String exportName, SpecialDataHandler specialData, Configuration config, File rootPath, ExportStatusDialog statusDialog) throws IOException {
		MarkupWizard markupWizard = this.mwManager.getMarkupWizard(exportName);
		
		//	connected to server, upload configuration
		if ((markupWizard != null) && this.ensureLoggedIn()) {
			statusDialog.setInfo("Switching DPS configuration");
			this.dpsClient.setGgConfigurationName(markupWizard.name);
			statusDialog.setProgress(100);
			return true;
		}
		
		//	not connected to server
		else return false;
	}
	
	private void updateDps() {
		
		//	select markup wizard to export
		MarkupWizard mw;
		ResourceDialog rd = ResourceDialog.getResourceDialog(this.mwManager, "Select Markup Wizard to Upload to DPS", "Do Upload");
		rd.setVisible(true);
		if (rd.isCommitted()) {
			
			//	get selected markup wizard
			String mwName = rd.getSelectedResourceName();
			
			//	test if selected markup wizard exists
			mw = this.mwManager.getMarkupWizard(mwName);
			if (mw == null) {
				JOptionPane.showMessageDialog(DialogPanel.getTopWindow(), ("Unable to find Markup Wizard '" + rd.getSelectedResourceName() + "'"), "Unknown Markup Wizard", JOptionPane.ERROR_MESSAGE);
				return;
			}
			
			//	update DPS
			this.doExport(mwName + "@" + MarkupWizardConfigurationManager.class.getName());
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
					this.dpsClient = null;
					this.authClient = null;
				}
			}
			
			//	server temporarily unreachable, re-login will be done by auth manager
			catch (IOException ioe) {
				this.dpsClient = null;
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
			this.dpsClient = new GoldenGateDpsClientAuth(this.authClient);
			return true;
		}
	}
}
