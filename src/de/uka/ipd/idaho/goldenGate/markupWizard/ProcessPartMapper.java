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
package de.uka.ipd.idaho.goldenGate.markupWizard;

import java.util.HashMap;


/**
 * Outsourced functionality containing all mapping-related functionality.
 * 
 * @author Tobias
 * @author sautter
 */
public class ProcessPartMapper {
	
	/**
	 * An individual mapping from a markup process part to a document processor
	 * 
	 * @author sautter
	 */
	public static class ProcessPartMapping {
		
		/** the name of the markup process part */
		public final String processPartName;
		
		/** the name of the document processor (may be null) */
		public final String documentProcessorName;
		
		/**
		 * indicator if the document processor can be completely trusted to
		 * fully fix a given sort of error, thus not requiring manual correction
		 * afterward
		 */
		public final boolean documentProcessorTrusted;
		
		/**
		 * @param processPartName
		 * @param documentProcessorName
		 */
		public ProcessPartMapping(String processPartName, String documentProcessorName) {
			this.processPartName = processPartName;
			this.documentProcessorName = documentProcessorName;
			this.documentProcessorTrusted = false;
		}
		
		/**
		 * @param processPartName
		 * @param documentProcessorName
		 * @param documentProcessorTrusted
		 */
		public ProcessPartMapping(String processPartName, String documentProcessorName, boolean documentProcessorTrusted) {
			this.processPartName = processPartName;
			this.documentProcessorName = documentProcessorName;
			this.documentProcessorTrusted = (documentProcessorTrusted && (documentProcessorName != null));
		}
		
		/* (non-Javadoc)
		 * @see java.lang.Object#toString()
		 */
		public String toString() {
			return (this.processPartName + (this.documentProcessorTrusted ? "=>" : "->") + ((this.documentProcessorName == null) ? "" : this.documentProcessorName));
		}
		
		/**
		 * Parse a mapping from its string representation.
		 * @param mappingLine the string to parse
		 * @return a mapping parsed from the specified string
		 */
		public static ProcessPartMapping parseMapping(String mappingLine) {
			int untrustedSplit = mappingLine.indexOf("->");
			int trustedSplit = mappingLine.indexOf("=>");
			
			if ((untrustedSplit == -1) && (trustedSplit == -1))
				throw new IllegalArgumentException("Invalid mapping line '" + mappingLine + "'");
			
			else if (trustedSplit == -1) {
				String processPartName = mappingLine.substring(0, untrustedSplit).trim();
				String documentProcessorName = mappingLine.substring(mappingLine.indexOf("->") + 2).trim();
				if (documentProcessorName.length() == 0)
					documentProcessorName = null;
				return new ProcessPartMapping(processPartName, documentProcessorName);
			}
			else {
				String processPartName = mappingLine.substring(0, trustedSplit).trim();
				String documentProcessorName = mappingLine.substring(mappingLine.indexOf("=>") + 2).trim();
				if (documentProcessorName.length() == 0)
					documentProcessorName = null;
				return new ProcessPartMapping(processPartName, documentProcessorName, (documentProcessorName != null));
			}
		}
	}
	
	private final HashMap mappings = new HashMap();
	
	/**
	 * Retrieve the mapping for a given process part. This method just does a
	 * lookup for an actual mapping, but does not consider the process part
	 * hierarchy.
	 * @param processPart the name of the process part to retrieve the mapping
	 *            for
	 * @return the mapping belonging to the specified process part
	 */
	public synchronized ProcessPartMapping getProcessPartMapping(String processPart) {
		return ((ProcessPartMapping) this.mappings.get(processPart));
	}
	
	/**
	 * Retrieve all existing mappings.
	 * @return an array holding all existing mappings
	 */
	public synchronized ProcessPartMapping[] getProcessPartMappings() {
		return ((ProcessPartMapping[]) this.mappings.values().toArray(new ProcessPartMapping[this.mappings.size()]));
	}
	
	/**
	 * Change the mapping of a process part. If the argument document processor
	 * name is null, the mapping will be removed. Otherwise, it will be created,
	 * or changed, if there already was a mapping.
	 * @param processPartName the name of the process part
	 * @param processorName the name of the document processor to map the
	 *            process part to
	 */
	public synchronized void setProcessPartMapping(String processPartName, String processorName) {
		this.setProcessPartMapping(processPartName, processorName, false);
	}
	
	/**
	 * Change the mapping of a process part. If the argument document processor
	 * name is null, the mapping will be removed. Otherwise, it will be created,
	 * or changed, if there already was a mapping.
	 * @param processPartName the name of the process part
	 * @param processorName the name of the document processor to map the
	 *            process part to
	 */
	public synchronized void setProcessPartMapping(String processPartName, String processorName, boolean processorTrusted) {
		if (processorName == null)
			this.mappings.remove(processPartName);
		else this.mappings.put(processPartName, new ProcessPartMapping(processPartName, processorName, processorTrusted));
	}
	
	/**
	 * Clear all existing mappings.
	 */
	public synchronized void clearProcessPartMappings() {
		this.mappings.clear();
	}
	
	/**
	 * Constructor
	 * @param mappingLines an array holding the string representations of the
	 *            mappings
	 * @see de.uka.ipd.idaho.toma.markupWizard.ProcessPartMapper.ProcessPartMapping#parseMapping(String)
	 * @see de.uka.ipd.idaho.toma.markupWizard.ProcessPartMapper.ProcessPartMapping#toString()
	 */
	public ProcessPartMapper(String[] mappingLines) {
		for (int m = 0; m < mappingLines.length; m++) try {
			ProcessPartMapping mapping = ProcessPartMapping.parseMapping(mappingLines[m]);
			if (mapping != null)
				this.mappings.put(mapping.processPartName, mapping);
		}
		catch (RuntimeException re) {
			System.out.println("ProcessPartMapper: invalid mapping '" + mappingLines[m] + "'");
		}
	}
	
	/**
	 * Constructor
	 * @param mappings an array holding the mappings
	 */
	public ProcessPartMapper(ProcessPartMapping[] mappings) {
		for (int m = 0; m < mappings.length; m++) {
			if (mappings[m].documentProcessorName != null)
				this.mappings.put(mappings[m].processPartName, mappings[m]);
		}
	}
}
