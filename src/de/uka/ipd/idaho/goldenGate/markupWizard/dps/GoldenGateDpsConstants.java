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

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.util.LinkedHashMap;

import de.uka.ipd.idaho.goldenGateServer.GoldenGateServerConstants;
import de.uka.ipd.idaho.htmlXmlUtil.Parser;
import de.uka.ipd.idaho.htmlXmlUtil.TokenReceiver;
import de.uka.ipd.idaho.htmlXmlUtil.TreeNodeAttributeSet;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.Grammar;
import de.uka.ipd.idaho.htmlXmlUtil.grammars.StandardGrammar;

/**
 * Constant holder for network communication with DocumentProcessingServer
 * 
 * @author sautter
 */
public interface GoldenGateDpsConstants extends GoldenGateServerConstants {
	
	/**
	 * Statistics object providing a summary of the community markup effort and
	 * its current status in a GoldenGATE DPS.
	 * 
	 * @author sautter
	 */
	public static class DpsStatistics {
		
		private static final String STATISTICS_TYPE = "statistics";
		private static final String STATUS_TYPE = "status";
		
		private static final String DOC_COUNT_ATTRIBUTE = "docCount";
		private static final String PENDING_DOC_COUNT_ATTRIBUTE = "pendingDocCount";
		private static final String PROCESSING_DOC_COUNT_ATTRIBUTE = "processingDocCount";
		private static final String FINISHED_COUNT_ATTRIBUTE = "finishedDocCount";
		private static final String NAME_ATTRIBUTE = "name";
		
		/** the total number of documents the backing DPS is aware of */
		public final int docCount;
		
		/** the number of documents that have not entered the markup process so far */
		public final int pendingDocCount;
		
		/** the number of documents that have proceeded into the markup process somewhat, but are not finished yet */
		public final int processingDocCount;
		
		/** the number of documents that have already run through the whole markup process */
		public final int finishedDocCount;
		
		private LinkedHashMap statusDocCounts = new LinkedHashMap();
		
		DpsStatistics(int docCount, int pendingDocCount, int processingDocCount, int finishedDocCount) {
			this.docCount = docCount;
			this.pendingDocCount = pendingDocCount;
			this.processingDocCount = processingDocCount;
			this.finishedDocCount = finishedDocCount;
		}
		
		/**
		 * Retrieve the markup states that at least one document is in in the
		 * DPS. The strings in the returned array are in the order that
		 * documents run through them in the backing markup process.
		 * @return an array holding the markup states
		 */
		public String[] getDocStates() {
			return ((String[]) this.statusDocCounts.keySet().toArray(new String[this.statusDocCounts.size()]));
		}
		
		/**
		 * Retrieve the number of documents in a given markup status. If the
		 * argument status is anything but one of the strings contained in the
		 * array returned by the getDocStates() method, this method returns 0.
		 * @param status the markup status to retieve the document count for
		 * @return the number of documents in the specified markup status
		 */
		public int getStatusDocCount(String status) {
			Integer docCount = ((Integer) this.statusDocCounts.get(status));
			return ((docCount == null) ? 0 : docCount.intValue());
		}
		
		void setStatusDocCount(String name, int docCount) {
			if (docCount < 1)
				this.statusDocCounts.remove(name);
			else this.statusDocCounts.put(name, new Integer(docCount));
		}
		
		/**
		 * Write an XML representation of this statistics to some writer.
		 * @param w the writer to write to
		 * @throws IOException
		 */
		public void writeXml(Writer w) throws IOException {
			BufferedWriter bw = ((w instanceof BufferedWriter) ? ((BufferedWriter) w) : new BufferedWriter(w));
			
			bw.write("<" + STATISTICS_TYPE +
					" " + DOC_COUNT_ATTRIBUTE + "=\"" + this.docCount + "\"" +
					" " + PENDING_DOC_COUNT_ATTRIBUTE + "=\"" + this.pendingDocCount + "\"" +
					" " + PROCESSING_DOC_COUNT_ATTRIBUTE + "=\"" + this.processingDocCount + "\"" +
					" " + FINISHED_COUNT_ATTRIBUTE + "=\"" + this.finishedDocCount + "\"" +
					">");
			bw.newLine();
			
			String[] docStates = this.getDocStates();
			for (int s = 0; s < docStates.length; s++) {
				bw.write("<" + STATUS_TYPE +
						" " + NAME_ATTRIBUTE + "=\"" + grammar.escape(docStates[s]) + "\"" +
						" " + DOC_COUNT_ATTRIBUTE + "=\"" + this.getStatusDocCount(docStates[s]) + "\"" +
						"/>");
				bw.newLine();
			}
			
			bw.write("</" + STATISTICS_TYPE + ">");
			bw.newLine();
			
			if (bw != w)
				bw.flush();
		}
		
		/**
		 * Reconstuct a DPS statistics from its XML representation. This method
		 * closes the argument reader before returning.
		 * @param r the reader to read from
		 * @return the statistics object reconstructed from the data provided by
		 *         the argument reader
		 * @throws IOException
		 */
		public static DpsStatistics readDpsStatistics(final Reader r) throws IOException {
			final DpsStatistics[] dpsStatistics = {null};
			TokenReceiver tr = new TokenReceiver() {
				public void close() throws IOException {
					r.close();
				}
				public void storeToken(String token, int treeDepth) throws IOException {
					if (grammar.isTag(token)) {
						if (grammar.isEndTag(token)) // might need this later, when elements other than the statistics (root) element have children or textual content
							return;
						
						String type = grammar.getType(token);
						if (STATISTICS_TYPE.equals(type)) {
							TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, grammar);
							dpsStatistics[0] = new DpsStatistics(
									Integer.parseInt(tnas.getAttribute(DOC_COUNT_ATTRIBUTE, "0")),
									Integer.parseInt(tnas.getAttribute(PENDING_DOC_COUNT_ATTRIBUTE, "0")),
									Integer.parseInt(tnas.getAttribute(PROCESSING_DOC_COUNT_ATTRIBUTE, "0")),
									Integer.parseInt(tnas.getAttribute(FINISHED_COUNT_ATTRIBUTE, "0"))
								);
						}
						else if (dpsStatistics[0] == null)
							return;
						else if (STATUS_TYPE.equals(type)) {
							TreeNodeAttributeSet tnas = TreeNodeAttributeSet.getTagAttributes(token, grammar);
							String name = tnas.getAttribute(NAME_ATTRIBUTE);
							if (name != null)
								dpsStatistics[0].setStatusDocCount(name, Integer.parseInt(tnas.getAttribute(DOC_COUNT_ATTRIBUTE, "0")));
						}
					}
					//	ignore textual content for now, as we are tag-only at the moment (might change, though, eg when adding explanations to the states)
				}
			};
			parser.stream(r, tr);
			tr.close();
			return dpsStatistics[0];
		}
		
		//	parser and grammar for data
		private static final Grammar grammar = new StandardGrammar();
		private static final Parser parser = new Parser(grammar);
	}
	
	/** command for retrieving a statistics on the progress of community markup in DPS */
	public static final String GET_STATISICS = "DPS_GET_STATISTICS";
	
	/** command for retrieving the name of the GoldenGATE configuration used in DPS */
	public static final String GET_CONGIFURATION_NAME = "DPS_GET_CONGIFURATION_NAME";
	
	/** command for changing the name of the GoldenGATE configuration used in DPS */
	public static final String SET_CONGIFURATION_NAME = "DPS_SET_CONGIFURATION_NAME";
	
	/** the command for obtaining the markup process from the backing server */
	public static final String GET_PROCESS = "DPS_GET_PROCESS";
	
	/** command for retrieving the current mappings of markup process definition parts to document processors */
	public static final String GET_PROCESS_PART_MAPPINGS = "DPS_GET_PROCESS_PART_MAPPINGS";
}
