---
layout: page
title: Install
permalink: /install/
---

Installing the Jimple Toolbox Commons Eclipse plugin is easy.  It is recommended to install the plugin from the provided update site, but it is also possible to install from source.
        
### Installing from Update Site (recommended)
1. Start Eclipse, then select `Help` &gt; `Install New Software`.
2. Click `Add`, in the top-right corner.
3. In the `Add Repository` dialog that appears, enter &quot;Atlas Toolboxes&quot; for the `Name` and &quot;[https://ensoftcorp.github.io/toolbox-repository/](https://ensoftcorp.github.io/toolbox-repository/)&quot; for the `Location`.
4. In the `Available Software` dialog, select the checkbox next to "Jimple Toolbox Commons" and click `Next` followed by `OK`.
5. In the next window, you'll see a list of the tools to be downloaded. Click `Next`.
6. Read and accept the license agreements, then click `Finish`. If you get a security warning saying that the authenticity or validity of the software can't be established, click `OK`.
7. When the installation completes, restart Eclipse.

## Installing from Source
If you want to install from source for bleeding edge changes, first grab a copy of the [source](https://github.com/EnSoftCorp/jimple-toolbox-commons) repository. In the Eclipse workspace, import the `com.ensoftcorp.open.jimple.commons` Eclipse project located in the source repository.  Right click on the project and select `Export`.  Select `Plug-in Development` &gt; `Deployable plug-ins and fragments`.  Select the `Install into host. Repository:` radio box and click `Finish`.  Press `OK` for the notice about unsigned software.  Once Eclipse restarts the plugin will be installed and it is advisable to close or remove the `com.ensoftcorp.open.jimple.commons` project from the workspace.

## Changelog
Note that version numbers are based off [Atlas](http://www.ensoftcorp.com/atlas/download/) version numbers.

### 3.9.2
- Updates for Atlas dependencies

### 3.6.0
- Updates for Atlas dependencies

### 3.3.0
- Added Jimple compilation / transformation APIs
- Code improvements to DLI loop header ID assignment

### 3.1.7
- Updates to depedencies
- Embedded javadoc with plugin

### 3.1.6
- Added loop filter
- Modified DLI algorithm to deterministically label loop headers
- Language agnostic callsite analysis handler
- Standarized highlighter logic

### 3.1.0
- Added callsite analysis implementation
- Added loop depth highlighter
- Standardized common queries

### 3.0.15
- Added loop boundary condition computations

### 3.0.14
- Added loop nesting depth filter

### 3.0.11
- Ported decompiled loop identification algorithm