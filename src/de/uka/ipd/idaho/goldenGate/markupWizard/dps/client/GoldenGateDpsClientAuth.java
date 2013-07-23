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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.util.ArrayList;

import de.uka.ipd.idaho.gamta.util.validation.Process;
import de.uka.ipd.idaho.goldenGate.markupWizard.ProcessPartMapper.ProcessPartMapping;
import de.uka.ipd.idaho.goldenGateServer.client.ServerConnection.Connection;
import de.uka.ipd.idaho.goldenGateServer.uaa.client.AuthenticatedClient;
import de.uka.ipd.idaho.htmlXmlUtil.Parser;
import de.uka.ipd.idaho.htmlXmlUtil.TreeNode;

/**
 * Client object for remotely configuring a DocumentProcessingServer
 * 
 * @author sautter
 */
public class GoldenGateDpsClientAuth extends GoldenGateDpsClient {
	
	private AuthenticatedClient authClient;
	
	/**
	 * Constructor
	 * @param ac the authenticated client to use for authentication and
	 *            connection
	 */
	public GoldenGateDpsClientAuth(AuthenticatedClient ac) {
		this.authClient = ac;
	}
	
	/* (non-Javadoc)
	 * @see de.uka.ipd.idaho.goldenGate.markupWizard.dps.client.GoldenGateDpsClient#getConnection()
	 */
	Connection getConnection() throws IOException {
		return this.authClient.getConnection();
	}
	
	/**
	 * Retrieve the name of the GoldenGATE configuration used in the backing
	 * DPS.
	 * @return the name of the GoldenGATE configuration used in the backing DPS
	 * @throws IOException
	 */
	public String getGgConfigurationName() throws IOException {
		if (!this.authClient.isLoggedIn()) throw new IOException("Not logged in.");
		Connection con = null;
		try {
			con = this.getConnection();
			BufferedWriter bw = con.getWriter();
			
			bw.write(GET_CONGIFURATION_NAME);
			bw.newLine();
			bw.write(this.authClient.getSessionID());
			bw.newLine();
			bw.flush();
			
			BufferedReader br = con.getReader();
			String error = br.readLine();
			if (GET_CONGIFURATION_NAME.equals(error))
				return br.readLine();
			
			else throw new IOException(error);
		}
		finally {
			if (con != null)
				con.close();
		}
	}
	
	private Process process = null;
	private static final Parser parser = new Parser();
	
	/**
	 * Retrieve the markup process used in the backing CMS.
	 * @return a markup process similar to the one used in the backing CMS
	 * @throws IOException
	 */
	public Process getProcess() throws IOException {
		if (this.process == null) {
			if (!this.authClient.isLoggedIn()) throw new IOException("Not logged in.");
			
			Connection con = null;
			try {
				con = this.getConnection();
				BufferedWriter bw = con.getWriter();
				
				bw.write(GET_PROCESS);
				bw.newLine();
				bw.write(this.authClient.getSessionID());
				bw.newLine();
				bw.flush();
				
				BufferedReader br = con.getReader();
				String error = br.readLine();
				if (GET_PROCESS.equals(error)) {
					try {
						TreeNode pRoot = parser.parse(br);
						if (TreeNode.ROOT_NODE_TYPE.equals(pRoot.getNodeType()))
							pRoot = pRoot.getChildNode(Process.PROCESS, 0);
						this.process = new Process(pRoot);
					}
					catch (IOException ioe) {
						ioe.printStackTrace(System.out);
						throw new IOException("Loading process validator failed: " + ioe.getMessage());
					}
				}
				else throw new IOException("Loading process validator failed: " + error);
			}
			finally {
				if (con != null)
					con.close();
			}
		}
		return this.process;
	}
	
	/**
	 * Retrieve the mapping of markup process parts to document processors.
	 * @return the individual mappings, packed in an array
	 * @throws IOException
	 */
	public ProcessPartMapping[] getProcessPartMappings() throws IOException {
		if (!this.authClient.isLoggedIn()) throw new IOException("Not logged in.");
		Connection con = null;
		try {
			con = this.getConnection();
			BufferedWriter bw = con.getWriter();
			
			bw.write(GET_PROCESS_PART_MAPPINGS);
			bw.newLine();
			bw.write(this.authClient.getSessionID());
			bw.newLine();
			bw.flush();
			
			BufferedReader br = con.getReader();
			String error = br.readLine();
			if (GET_PROCESS_PART_MAPPINGS.equals(error)) {
				ArrayList mappings = new ArrayList();
				String mappingLine;
				while (((mappingLine = br.readLine()) != null) && (mappingLine.length() != 0))
					mappings.add(ProcessPartMapping.parseMapping(mappingLine));
				return ((ProcessPartMapping[]) mappings.toArray(new ProcessPartMapping[mappings.size()]));
			}
			else throw new IOException(error);
		}
		finally {
			if (con != null)
				con.close();
		}
	}
	
	/**
	 * Change the name of the GoldenGATE configuration used in the backing DPS.
	 * This method requires administrative priviledges. Furthermore, a
	 * configuration with the specified name has to be present in the ECS on the
	 * backing server. If this is not given, an IOException will be thrown.
	 * @param ggConfigurationName the name of the GoldenGATE configuration to
	 *            use from now on
	 * @throws IOException
	 */
	public void setGgConfigurationName(String ggConfigurationName) throws IOException {
		if (!this.authClient.isLoggedIn()) throw new IOException("Not logged in.");
		Connection con = null;
		try {
			con = this.getConnection();
			BufferedWriter bw = con.getWriter();
			
			bw.write(SET_CONGIFURATION_NAME);
			bw.newLine();
			bw.write(this.authClient.getSessionID());
			bw.newLine();
			bw.write(ggConfigurationName);
			bw.newLine();
			bw.flush();
			
			BufferedReader br = con.getReader();
			String error = br.readLine();
			if (!SET_CONGIFURATION_NAME.equals(error))
				throw new IOException(error);
		}
		finally {
			if (con != null)
				con.close();
		}
	}
}
