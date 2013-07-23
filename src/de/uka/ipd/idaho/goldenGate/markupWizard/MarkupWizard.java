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
import de.uka.ipd.idaho.gamta.util.validation.Process;

/**
 * A markup wizard, basically a triplet of a name, a markup process definition,
 * and a mapper from process part names to document processor names.
 * 
 * @author sautter
 */
public class MarkupWizard {
	
	/** the name of the markup wizard */
	public final String name;
	
	/** the definition of the markup process */
	public final Process process;
	
	/** the mapping from individual parts of the merkup process to document processors */
	public final ProcessPartMapper mapper;
	
	/**
	 * Constructor
	 * @param name the name of the markup wizard
	 * @param process the definition of the markup process
	 * @param dpMappings the mappings from individual parts of the markup
	 *            process to document processors, in their String representation
	 */
	public MarkupWizard(String name, Process process, String[] dpMappings) {
		this(name, process, new ProcessPartMapper(dpMappings));
	}
	
	/**
	 * Constructor
	 * @param name the name of the markup wizard
	 * @param process the definition of the markup process
	 * @param mapper the mapping from individual parts of the markup process
	 *            to document processors
	 */
	public MarkupWizard(String name, Process process, ProcessPartMapper mapper) {
		this.name = name;
		this.process = process;
		this.mapper = mapper;
	}
}
