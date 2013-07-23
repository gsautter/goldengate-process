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

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;

import de.uka.ipd.idaho.gamta.util.validation.Process;
import de.uka.ipd.idaho.gamta.util.validation.ValidationResult;
import de.uka.ipd.idaho.gamta.util.validation.Validator;
import de.uka.ipd.idaho.goldenGate.markupWizard.dmc.GoldenGateDmcConstants;
import de.uka.ipd.idaho.goldenGateServer.client.ServerConnection.Connection;
import de.uka.ipd.idaho.goldenGateServer.uaa.client.AuthenticatedClient;
import de.uka.ipd.idaho.htmlXmlUtil.Parser;
import de.uka.ipd.idaho.htmlXmlUtil.TreeNode;

/**
 * @author sautter
 *
 */
public class GoldenGateDmcClient implements GoldenGateDmcConstants {
	
	private AuthenticatedClient authClient;
	
	/**
	 * Constructor
	 * @param authClient the AuthenticatedClient providing authentication and
	 *            communication connections (needs to be logged in for the
	 *            methods of this client to work)
	 */
	public GoldenGateDmcClient(AuthenticatedClient authClient) {
		this.authClient = authClient;
	}
	
	private Process process = null;
	private static final Parser parser = new Parser();
	
	/**
	 * Retrieve the markup process used in the backing DMP.
	 * @return a markup process similar to the one used in the backing DMP
	 * @throws IOException
	 */
	public Process getProcess() throws IOException {
		return this.getProcess(true);
	}
	
	/**
	 * Retrieve the markup process used in the backing DMP.
	 * @param allowCache allow using cached process?
	 * @return a markup process similar to the one used in the backing DMP
	 * @throws IOException
	 */
	public Process getProcess(boolean allowCache) throws IOException {
		if (!allowCache || (this.process == null)) {
			if (!this.authClient.isLoggedIn()) throw new IOException("Not logged in.");
			
			Connection con = null;
			try {
				con = this.authClient.getConnection();
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
						System.out.println("Loading process failed: " + ioe.getMessage());
						ioe.printStackTrace(System.out);
						throw ioe;
					}
				}
				else throw new IOException(error);
			}
			finally {
				if (con != null)
					con.close();
			}
		}
		return this.process;
	}
	
	/**
	 * Retrieve the validator used in the backing CMS, e.g. for client side
	 * offline progress checks
	 * @return a document validator similar to the one used in the backing CMS
	 * @throws IOException
	 */
	public Validator getValidator() throws IOException {
		return this.getProcess().getValidator();
	}
	
	/**
	 * Retrieve the validation result of a document, a detailed markup status
	 * report
	 * @param documentId the ID of the document to retrieve the validation
	 *            result for
	 * @return the most recent validation result for the document with the
	 *         specified ID
	 * @throws IOException
	 */
	public ValidationResult getValidationResult(String documentId) throws IOException {
		if (!this.authClient.isLoggedIn()) throw new IOException("Not logged in.");
		
		Connection con = null;
		try {
			con = this.authClient.getConnection();
			BufferedWriter bw = con.getWriter();
			
			bw.write(GET_VALIDATION_RESULT);
			bw.newLine();
			bw.write(this.authClient.getSessionID());
			bw.newLine();
			bw.write(documentId);
			bw.newLine();
			bw.flush();
			
			BufferedReader br = con.getReader();
			String error = br.readLine();
			if (GET_VALIDATION_RESULT.equals(error)) {
				TreeNode vrRoot = parser.parse(br);
				if (TreeNode.ROOT_NODE_TYPE.equals(vrRoot.getNodeType()))
					vrRoot = vrRoot.getChildNode(Process.PROCESS, 0);
				return this.getProcess().loadValidationResult(vrRoot);
			}
			else throw new IOException(error);
		}
		finally {
			if (con != null)
				con.close();
		}
	}
	
	/**
	 * Update the markup process used in the backing DMP (requires
	 * administrative priviledges)
	 * @param process the new markup process to be used in the backing CMS
	 * @throws IOException
	 */
	public void updateProcess(Process process) throws IOException {
		if (!this.authClient.isLoggedIn()) throw new IOException("Not logged in.");
		
		Connection con = null;
		try {
			con = this.authClient.getConnection();
			BufferedWriter bw = con.getWriter();
			
			bw.write(UPDATE_PROCESS);
			bw.newLine();
			bw.write(this.authClient.getSessionID());
			bw.newLine();
			process.writeXml(bw);
			bw.newLine();
			bw.newLine();
			bw.flush();
			
			BufferedReader br = con.getReader();
			String error = br.readLine();
			if (!UPDATE_PROCESS.equals(error))
				throw new IOException(error);
		}
		finally {
			if (con != null)
				con.close();
		}
	}
}
