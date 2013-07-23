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

import de.uka.ipd.idaho.goldenGate.markupWizard.dps.GoldenGateDpsConstants;
import de.uka.ipd.idaho.goldenGateServer.client.ServerConnection;
import de.uka.ipd.idaho.goldenGateServer.client.ServerConnection.Connection;

/**
 * Client for retrieving statistics data from DPS without authentication.
 * 
 * @author sautter
 */
public class GoldenGateDpsClient implements GoldenGateDpsConstants {
	
	private ServerConnection sCon;
	
	/**
	 * Constructor
	 * @param sc the server connection to use for connection
	 */
	public GoldenGateDpsClient(ServerConnection sc) {
		this.sCon = sc;
	}
	
	GoldenGateDpsClient() {
		this(null);
	}
	
	Connection getConnection() throws IOException {
		return this.sCon.getConnection();
	}
	
	/**
	 * Retrieve a statistics on the progress of community markup in the backing
	 * DPS. Both keys and values in the returned map are strings. They should be
	 * displayed in the order the keySet() iterator of the map returns them.
	 * @return a statistics on the progress of community markup in the backing
	 *         DPS
	 * @throws IOException
	 */
	public DpsStatistics getDpsStatistics() throws IOException {
		Connection con = null;
		try {
			con = this.getConnection();
			BufferedWriter bw = con.getWriter();
			
			bw.write(GET_STATISICS);
			bw.newLine();
			bw.flush();
			
			BufferedReader br = con.getReader();
			String error = br.readLine();
			if (GET_STATISICS.equals(error))
				return DpsStatistics.readDpsStatistics(br);
			
			else throw new IOException(error);
		}
		finally {
			if (con != null)
				con.close();
		}
	}
}
