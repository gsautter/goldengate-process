Copyright (c) 2006-, IPD Boehm, Universitaet Karlsruhe (TH) / KIT, by Guido Sautter
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:

    * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
    * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
    * Neither the name of the Universität Karlsruhe (TH) nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY UNIVERSITÄT KARLSRUHE (TH) / KIT AND CONTRIBUTORS 
"AS IS" AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO,
THE IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE
ARE DISCLAIMED. IN NO EVENT SHALL THE REGENTS OR CONTRIBUTORS BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.


This project provides markup process description and control facilities for the
GoldenGATE system, including management tools for process descriptions. It builds
respective plug-ins and components for both GoldenGATE Editor and GoldenGATE
Server.
See also: Sautter, G., Böhm, K., Kühne, C., & Mathäß, T. (2010). ProcessTron:
Efficient Semi-automated Markup Generation for Scientific Documents. In Proceedings
of JCDL 2010, Gold Coast, Queensland, Australia.


This project requires the JAR files build by the Ant scripts in the idaho-core
(http://code.google.com/p/idaho-core/) project, as well as the JAR files
referenced from there. See http://code.google.com/p/idaho-core/source/browse/README.txt
for the latter. You can either check out idaho-core as a project into the same
workspace and build them first, or include the JAR files it generates in the
"lib" folder.

The GoldenGATE Editor plug-ins further depend on the goldengate-editor project
(http://code.google.com/p/goldengate-editor/) and the JAR files referenced from
there. See http://code.google.com/p/goldengate-editor/source/browse/README.txt
for the latter. If neither of the goldengate-editor project and the GoldenGATE.jar
it builds into are avaliable, the build as a whole won't fail, only the GoldenGATE
Editor plug-ins won't be created.
In addition, some plug-ins further depend on the goldengate-plugins project
(http://code.google.com/p/goldengate-plugins/) and the JAR files referenced from
there. See http://code.google.com/p/goldengate-plugins/source/browse/README.txt
for the latter. If neither of the goldengate-plugins project and the MarkupProcess.jar
it builds are avaliable, the build as a whole won't fail, only the affected
GoldenGATE Editor plug-ins won't be created.

Likewise, the GoldenGATE Server components depend on the goldengate-server-docs
project (http://code.google.com/p/goldengate-server-docs/) projects and the JAR
files referenced from there. See
http://code.google.com/p/goldengate-server-docs/source/browse/README.txt for the
latter. If neither of the goldengate-server-docs project and the GgServerDIO.jar
it builds are avaliable, the build as a whole won't fail, only the GoldenGATE
Server components won't be created.