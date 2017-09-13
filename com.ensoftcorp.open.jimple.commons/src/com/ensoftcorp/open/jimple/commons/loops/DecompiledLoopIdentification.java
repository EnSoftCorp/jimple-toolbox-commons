package com.ensoftcorp.open.jimple.commons.loops;

import static com.ensoftcorp.atlas.core.script.Common.resolve;
import static com.ensoftcorp.atlas.core.script.Common.universe;

import java.lang.Thread.State;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.eclipse.core.runtime.IProgressMonitor;
import org.eclipse.core.runtime.NullProgressMonitor;

import com.ensoftcorp.atlas.core.db.graph.Edge;
import com.ensoftcorp.atlas.core.db.graph.Graph;
import com.ensoftcorp.atlas.core.db.graph.GraphElement.EdgeDirection;
import com.ensoftcorp.atlas.core.db.graph.GraphElement.NodeDirection;
import com.ensoftcorp.atlas.core.db.graph.Node;
import com.ensoftcorp.atlas.core.db.graph.operation.ForwardGraph;
import com.ensoftcorp.atlas.core.db.set.AtlasHashSet;
import com.ensoftcorp.atlas.core.db.set.AtlasSet;
import com.ensoftcorp.atlas.core.db.set.SingletonAtlasSet;
import com.ensoftcorp.atlas.core.index.common.SourceCorrespondence;
import com.ensoftcorp.atlas.core.query.Q;
import com.ensoftcorp.atlas.core.xcsg.XCSG;
import com.ensoftcorp.open.jimple.commons.log.Log;

/**
 * Uses algorithm from Wei et al. to identify loops, even irreducible ones.
 * 
 * "A New Algorithm for Identifying Loops in Decompilation". Static Analysis
 * Lecture Notes in Computer Science Volume 4634, 2007, pp 170-183
 * http://link.springer.com/chapter/10.1007%2F978-3-540-74061-2_11
 * http://www.lenx.100871.net/papers/loop-SAS.pdf
 * 
 * @author Tom Deering - initial implementation
 * @author Jon Mathews - replaced recursive logic iterative implementation
 * @author Nikhil Ranade - added loop child edges to match Atlas for Java graph schema
 * @author Ben Holland - minor refactoring, integration utils, Atlas3 migrations
 */
public class DecompiledLoopIdentification implements Runnable {
	
	public static interface CFGNode {
		/**
		 * Tag applied to loop header CFG node
		 */
		public static final String LOOP_HEADER = "LOOP_HEADER";

		/**
		 * Tag applied to loop reentry CFG node
		 */
		public static final String LOOP_REENTRY_NODE = "LOOP_REENTRY_NODE";

		/**
		 * Tag applied to irreducible loop headers
		 */
		public static final String IRREDUCIBLE_LOOP = "IRREDUCIBLE_LOOP";

		/**
		 * Tag applied to natural loop headers (a LOOP_HEADER not tagged
		 * IRREDUCIBLE_LOOP).
		 */
		public static final String NATURAL_LOOP = "NATURAL_LOOP";

		/**
		 * Integer attribute identifier, matches the LOOP_HEADER_ID for the
		 * innermost loop header of this node.
		 */
		public static final String LOOP_MEMBER_ID = "LOOP_MEMBER_ID";

		/**
		 * Integer attribute identifier for this loop header.
		 */
		public static final String LOOP_HEADER_ID = "LOOP_HEADER_ID";
	}

	public static interface CFGEdge {
		/**
		 * Tag for ControlFlow_Edge indicating a loop re-entry. Also called a
		 * "cross edge".
		 */
		public static final String LOOP_REENTRY_EDGE = "LOOP_REENTRY_EDGE";

		/**
		 * Tag for loop back edges
		 */
		public static final String LOOP_BACK_EDGE = "LOOP_BACK_EDGE";
	}

	public static void recoverLoops() {
		recoverLoops(new NullProgressMonitor());
	}

	public static void recoverLoops(IProgressMonitor monitor) {
		_recoverLoops(monitor);
	}

	/**
	 * Identify all loop fragments, headers, re-entries, and nesting in the
	 * universe graph, applying the tags and attributes in interfaces CFGNode
	 * and CFGEdge.
	 * 
	 * NOTE: Handles both natural and irreducible loops
	 * 
	 * @return
	 */
	private static void _recoverLoops(IProgressMonitor monitor) {
		try {
			// find the work to be done
			Q u = universe();
			Graph cfContextG = resolve(null, u.edgesTaggedWithAny(XCSG.ControlFlow_Edge, XCSG.ExceptionalControlFlow_Edge).eval());
			AtlasSet<Node> cfRoots = u.nodesTaggedWithAny(XCSG.controlFlowRoot).eval().nodes();
			int work = (int) cfRoots.size();
			ArrayList<Node> rootList = new ArrayList<Node>(work);
			for (Node root : cfRoots){
				rootList.add(root);
			}

			monitor.beginTask("Identify Local Loops", rootList.size());

			// assign the work to worker threads
			int procs = Runtime.getRuntime().availableProcessors();
			Thread[] threads = new Thread[procs];
			int workPerProc = work / procs;
			int remainder = work % procs;
			for (int i = 0; i < procs; ++i) {
				int firstInclusive = workPerProc * i + Math.min(remainder, i);
				int lastExclusive = firstInclusive + workPerProc + (i < remainder ? 1 : 0);
				threads[i] = new Thread(new DecompiledLoopIdentification(monitor, cfContextG, rootList.subList(firstInclusive, lastExclusive)));
				threads[i].start();
			}

			// wait for worker threads to finish
			int waitIndex = 0;
			while (waitIndex < threads.length) {
				if (!State.TERMINATED.equals(threads[waitIndex].getState())) {
					try {
						threads[waitIndex].join();
					} catch (InterruptedException e) {
						Log.warning("Caught thread interruption exception", e);
					}
				} else {
					waitIndex++;
				}
			}
		} finally {
			monitor.done();
		}
	}

	private AtlasSet<Node> traversed, reentryNodes, irreducible;
	private AtlasSet<Edge> reentryEdges, loopbacks;
	private Graph cfContextG;

	/** The node's position in the DFSP (Depth-first search path) */
	private Map<Node, Integer> dfsp;
	private Map<Node, Node> innermostLoopHeaders;
	private List<Node> cfRoots;
	private static int idGenerator;
	private static Object idGeneratorLock = new Object();
	private IProgressMonitor monitor;

	private DecompiledLoopIdentification(IProgressMonitor monitor, Graph cfContextG, List<Node> cfRoots) {
		this.monitor = monitor;
		this.cfContextG = cfContextG;
		this.cfRoots = cfRoots;
		traversed = new AtlasHashSet<Node>();
		reentryNodes = new AtlasHashSet<Node>();
		reentryEdges = new AtlasHashSet<Edge>();
		irreducible = new AtlasHashSet<Node>();
		loopbacks = new AtlasHashSet<Edge>();
		dfsp = new HashMap<Node, Integer>();
		innermostLoopHeaders = new HashMap<Node, Node>();
	}

	@Override
	public void run() {
		// compute individually on a per-function basis
		for (Node root : cfRoots) {
			try {
				// clear data from previous function
				reentryNodes.clear();
				reentryEdges.clear();
				irreducible.clear();
				traversed.clear();
				innermostLoopHeaders.clear();
				loopbacks.clear();
				dfsp.clear();

				for (Node node : new ForwardGraph(cfContextG, new SingletonAtlasSet<Node>(root)).nodes()) {
					dfsp.put(node, 0);
				}

				// run loop identification algorithm
				
				// a recursive strategy may overflow the call stack in some cases
				// so not using the loopDFSRecursive(root, 1) implementation
				// better to use an equivalent iterative strategy
				loopDFSIterative(root, 1); 

				// modify universe graph
				Collection<Node> loopHeaders = innermostLoopHeaders.values();
				AtlasSet<Node> loopHeadersSet = new AtlasHashSet<Node>();
				loopHeadersSet.addAll(loopHeaders);
				
				ArrayList<Node> sortedLoopHeaders = new ArrayList<Node>((int) loopHeadersSet.size());
				for(Node loopHeader : loopHeadersSet){
					sortedLoopHeaders.add(loopHeader);
				}
				Collections.sort(sortedLoopHeaders, new Comparator<Node>(){
					@Override
					public int compare(Node n1, Node n2) {
						SourceCorrespondence n1SC = (SourceCorrespondence) n1.getAttr(XCSG.sourceCorrespondence);
						SourceCorrespondence n2SC = (SourceCorrespondence) n2.getAttr(XCSG.sourceCorrespondence);
						if(n1SC.sourceFile.equals(n2SC.sourceFile)){
							// same file, sort by source offset
							return Integer.compare(n1SC.offset, n2SC.offset);
						} else {
							// files are not the same sort broadly by file name
							String path1 = n1SC.sourceFile.getLocation().toOSString();
							String path2 = n2SC.sourceFile.getLocation().toOSString();
							return path1.compareTo(path2);
						}
					}
				});

				Map<Node, Integer> loopHeaderToID = new HashMap<Node, Integer>((int) loopHeadersSet.size());

				synchronized (idGeneratorLock) {
					for (Node header : sortedLoopHeaders) {
						int id = idGenerator++;
						loopHeaderToID.put(header, id);
						header.tag(CFGNode.LOOP_HEADER);

						header.putAttr(CFGNode.LOOP_HEADER_ID, id);
						if (irreducible.contains(header)) {
							header.tag(CFGNode.IRREDUCIBLE_LOOP);
						} else {
							header.tag(CFGNode.NATURAL_LOOP);
						}
					}
				}

				for (Node cfgNode : innermostLoopHeaders.keySet()) {
					Node loopHeader = innermostLoopHeaders.get(cfgNode);
					cfgNode.putAttr(CFGNode.LOOP_MEMBER_ID, loopHeaderToID.get(loopHeader));
					Edge edge = Graph.U.createEdge(loopHeader, cfgNode);
					edge.tag(XCSG.LoopChild);
				}

				for (Node reentryNode : reentryNodes) {
					reentryNode.tag(CFGNode.LOOP_REENTRY_NODE);
				}

				for (Edge reentryEdge : reentryEdges) {
					reentryEdge.tag(CFGEdge.LOOP_REENTRY_EDGE);
				}

				for (Edge loopbackEdge : loopbacks) {
					loopbackEdge.tag(CFGEdge.LOOP_BACK_EDGE);
				}
			} catch (Throwable t) {
				Log.error("Problem in loop analyzer thread for CFG root:\n" + root, t);
			}

			if (monitor.isCanceled()){
				return;
			}
			synchronized (monitor) {
				monitor.worked(1);
			}
		}
	}

	/**
	 * Recursively traverse the current node, returning its innermost loop
	 * header
	 * 
	 * @param b0
	 * @param position
	 * @return
	 */
	@SuppressWarnings("unused")
	private void loopDFSRecursive(Node b0, int position) {
		traversed.add(b0);
		dfsp.put(b0, position);

		for (Edge cfgEdge : cfContextG.edges(b0, NodeDirection.OUT)) {
			Node b = cfgEdge.getNode(EdgeDirection.TO);

			if (!traversed.contains(b)) {
				// Paper Case A
				// new
				loopDFSRecursive(b, position + 1);
				Node nh = innermostLoopHeaders.get(b);
				tag_lhead(b0, nh);
			} else {
				if (dfsp.get(b) > 0) {
					// Paper Case B
					// Mark b as a loop header
					loopbacks.add(cfgEdge);
					tag_lhead(b0, b);
				} else {
					Node h = innermostLoopHeaders.get(b);
					if (h == null) {
						// Paper Case C
						// do nothing
						continue;
					}

					if (dfsp.get(h) > 0) {
						// Paper Case D
						// h in DFSP(b0)
						tag_lhead(b0, h);
					} else {
						// Paper Case E
						// h not in DFSP(b0)
						reentryNodes.add(b);
						reentryEdges.add(cfgEdge);
						irreducible.add(h);

						while ((h = innermostLoopHeaders.get(h)) != null) {
							if (dfsp.get(h) > 0) {
								tag_lhead(b0, h);
								break;
							}
							irreducible.add(h);
						}
					}
				}
			}
		}

		dfsp.put(b0, 0);
	}

	private void tag_lhead(Node b, Node h) {
		if (h == null || h.equals(b)){
			return;
		}
		
		Node cur1 = b;
		Node cur2 = h;

		Node ih;
		while ((ih = innermostLoopHeaders.get(cur1)) != null) {
			if (ih.equals(cur2)){
				return;
			}
			if (dfsp.get(ih) < dfsp.get(cur2)) {
				innermostLoopHeaders.put(cur1, cur2);
				cur1 = cur2;
				cur2 = ih;
			} else {
				cur1 = ih;
			}
		}
		innermostLoopHeaders.put(cur1, cur2);
	}

	private Deque<Frame> stack = new ArrayDeque<Frame>();

	private static class Frame {
		int programCounter = 0;
		Node b = null;
		Node b0 = null;
		int position = 0;
		Iterator<Edge> iterator = null;
	}

	static private final int ENTER = 0;
	static private final int EACH_CFG_EDGE = 1;
	static private final int POP = 2;

	/**
	 * Iterative implementation, equivalent to loopDFSRecursive()
	 * 
	 * @param b0
	 * @param position
	 * @return
	 */
	private void loopDFSIterative(Node _b0, int _position) {
		stack.clear();

		Frame f = new Frame();
		f.b0 = _b0;
		f.position = _position;
		f.programCounter = ENTER;

		stack.push(f);

		stack: while (!stack.isEmpty()) {
			f = stack.peek();

			switch (f.programCounter) {
				case POP: {
					Node nh = innermostLoopHeaders.get(f.b);
					tag_lhead(f.b0, nh);
					f.programCounter = EACH_CFG_EDGE;
					continue stack;
				}
				case ENTER:
					traversed.add(f.b0);
					dfsp.put(f.b0, f.position);
					f.iterator = cfContextG.edges(f.b0, NodeDirection.OUT).iterator();
					// FALL THROUGH
				case EACH_CFG_EDGE:
					while (f.iterator.hasNext()) {
						Edge cfgEdge = f.iterator.next();
						f.b = cfgEdge.getNode(EdgeDirection.TO);
						if (!traversed.contains(f.b)) {
							// Paper Case A
							// new
							// BEGIN CONVERTED TO ITERATIVE
							// RECURSE: loopDFS(b, position + 1);
	
							f.programCounter = POP;
	
							Frame f2 = new Frame();
							f2.b0 = f.b;
							f2.position = f.position + 1;
							f2.programCounter = ENTER;
	
							stack.push(f2);
							continue stack;
	
							// case POP:
							// Node nh = innermostLoopHeaders.get(b);
							// tag_lhead(b0, nh);
	
							// END CONVERTED TO ITERATIVE
						} else {
							if (dfsp.get(f.b) > 0) {
								// Paper Case B
								// Mark b as a loop header
								loopbacks.add(cfgEdge);
								tag_lhead(f.b0, f.b);
							} else {
								Node h = innermostLoopHeaders.get(f.b);
								if (h == null) {
									// Paper Case C
									// do nothing
									continue;
								}
								
								if (dfsp.get(h) > 0) {
									// Paper Case D
									// h in DFSP(b0)
									tag_lhead(f.b0, h);
								} else {
									// Paper Case E
									// h not in DFSP(b0)
									reentryNodes.add(f.b);
									reentryEdges.add(cfgEdge);
									irreducible.add(h);
	
									while ((h = innermostLoopHeaders.get(h)) != null) {
										if (dfsp.get(h) > 0) {
											tag_lhead(f.b0, h);
											break;
										}
										irreducible.add(h);
									}
								}
							}
						}
					}
					
					dfsp.put(f.b0, 0);
					stack.pop();
			}
		}
	}

}