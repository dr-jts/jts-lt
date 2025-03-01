/*
 * Copyright (c) 2016 Vivid Solutions.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */

package org.locationtech.jts.index.kdtree;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.PriorityQueue;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateList;
import org.locationtech.jts.geom.Envelope;

/**
 * A 2D <a href='https://en.wikipedia.org/wiki/K-d_tree'>KD-Tree</a> spatial
 * index for efficient point query and retrieval.
 * <p>
 * KD-trees provide fast range searching and fast lookup for point data. The
 * tree is built dynamically by inserting points. The tree supports queries by
 * location and range, and for point equality. For querying, an internal stack
 * is used instead of recursion to avoid overflow.
 * <p>
 * This implementation supports detecting and snapping points which are closer
 * than a given distance tolerance. If the same point (up to tolerance) is
 * inserted more than once, it is snapped to the existing node. In other words,
 * if a point is inserted which lies within the tolerance of a node already in
 * the index, it is snapped to that node. When an inserted point is snapped to a
 * node then a new node is not created but the count of the existing node is
 * incremented. If more than one node in the tree is within tolerance of an
 * inserted point, the closest and then lowest node is snapped to.
 * <p>
 * The structure of a KD-Tree depends on the order of insertion of the points. A
 * tree may become unbalanced if the inserted points are coherent (e.g.
 * monotonic in one or both dimensions). A perfectly balanced tree has depth of
 * only log2(N), but an unbalanced tree may be much deeper. This has a serious
 * impact on query efficiency. One solution to this is to randomize the order of
 * points before insertion (e.g. by using <a href=
 * "https://en.wikipedia.org/wiki/Fisher%E2%80%93Yates_shuffle">Fisher-Yates
 * shuffling</a>).
 * 
 * @author David Skea
 * @author Martin Davis
 */
public class KdTree {

  /**
   * Converts a collection of {@link KdNode}s to an array of {@link Coordinate}s.
   * 
   * @param kdnodes
   *          a collection of nodes
   * @return an array of the coordinates represented by the nodes
   */
  public static Coordinate[] toCoordinates(Collection<KdNode> kdnodes) {
    return toCoordinates(kdnodes, false);
  }

  /**
   * Converts a collection of {@link KdNode}s 
   * to an array of {@link Coordinate}s,
   * specifying whether repeated nodes should be represented
   * by multiple coordinates.
   * 
   * @param kdnodes a collection of nodes
   * @param includeRepeated true if repeated nodes should 
   *   be included multiple times
   * @return an array of the coordinates represented by the nodes
   */
  public static Coordinate[] toCoordinates(Collection<KdNode> kdnodes, boolean includeRepeated) {
    CoordinateList coord = new CoordinateList();
    for (Iterator<KdNode> it = kdnodes.iterator(); it.hasNext();) {
      KdNode node = (KdNode) it.next();
      int count = includeRepeated ? node.getCount() : 1;
      for (int i = 0; i < count; i++) {
       coord.add(node.getCoordinate(), true);
      }
    }
    return coord.toCoordinateArray();
  }

  private KdNode root = null;
  private long numberOfNodes;
  private final double tolerance;
  private final double toleranceSq;

  /**
   * Creates a new instance of a KdTree with a snapping tolerance of 0.0. (I.e.
   * distinct points will <i>not</i> be snapped)
   */
  public KdTree() {
    this(0.0);
  }

  /**
   * Creates a new instance of a KdTree, specifying a snapping distance
   * tolerance. Points which lie closer than the tolerance to a point already in
   * the tree will be treated as identical to the existing point.
   * 
   * @param tolerance
   *          the tolerance distance for considering two points equal
   */
  public KdTree(double tolerance) {
    this.tolerance = tolerance;
    this.toleranceSq = tolerance*tolerance;
  }

  /**
   * Gets the root node of this tree.
   * 
   * @return the root node of the tree
   */
  public KdNode getRoot() {
    return root;
  }
  
  /**
   * Tests whether the index contains any items.
   * 
   * @return true if the index does not contain any items
   */
  public boolean isEmpty() {
    if (root == null)
      return true;
    return false;
  }

  /**
   * Inserts a new point in the kd-tree, with no data.
   * 
   * @param p
   *          the point to insert
   * @return the kdnode containing the point
   */
  public KdNode insert(Coordinate p) {
    return insert(p, null);
  }

  /**
   * Inserts a new point into the kd-tree.
   * 
   * @param p
   *          the point to insert
   * @param data
   *          a data item for the point
   * @return returns a new KdNode if a new point is inserted, else an existing
   *         node is returned with its counter incremented. This can be checked
   *         by testing returnedNode.getCount() &gt; 1.
   */
  public KdNode insert(Coordinate p, Object data) {
    if (root == null) {
      root = new KdNode(p, data);
      return root;
    }
    
    /**
     * Check if the point is already in the tree, up to tolerance.
     * If tolerance is zero, this phase of the insertion can be skipped.
     */
    if ( tolerance > 0 ) {
      KdNode matchNode = findBestMatchNode(p);
      if (matchNode != null) {
        // point already in index - increment counter
        matchNode.increment();
        return matchNode;
      }
    }
    
    return insertExact(p, data);
  }
  
  /**
   * Finds the nearest node in the tree to the given query point.
   * 
   * @param query the query point
   * @return the nearest node, or null if the tree is empty
   */
  public KdNode nearestNeighbor(final Coordinate query) {
    if (root == null) {
      return null;
    }

    KdNode bestNode = null;
    double bestDistance = Double.POSITIVE_INFINITY;
    Deque<NNStackFrame> stack = new ArrayDeque<>();
    KdNode currentNode = root;
    boolean isXLevel = true;

    while (currentNode != null || !stack.isEmpty()) {
      if (currentNode != null) {
        double currentDist = query.distanceSq(currentNode.getCoordinate());
        if (currentDist < bestDistance) {
          bestNode = currentNode;
          bestDistance = currentDist;
          if (bestDistance == 0) {
              return bestNode; // Early termination
          }
        }

        boolean currentIsXLevel = isXLevel;
        double splitValue = currentNode.splitValue(currentIsXLevel);
        KdNode nextNode;
        KdNode otherNode;

        if (currentIsXLevel) {
          if (query.x < splitValue) {
            nextNode = currentNode.getLeft();
            otherNode = currentNode.getRight();
          } else {
            nextNode = currentNode.getRight();
            otherNode = currentNode.getLeft();
          }
        } else {
          if (query.y < splitValue) {
            nextNode = currentNode.getLeft();
            otherNode = currentNode.getRight();
          } else {
            nextNode = currentNode.getRight();
            otherNode = currentNode.getLeft();
          }
        }

        stack.push(new NNStackFrame(otherNode, currentIsXLevel, splitValue));
        currentNode = nextNode;
        isXLevel = !currentIsXLevel;
      } else {
        NNStackFrame frame = stack.pop();
        KdNode otherNode = frame.node;
        boolean parentSplitAxis = frame.parentSplitAxis;
        double parentSplitValue = frame.parentSplitValue;

        double diff = parentSplitAxis 
        	    ? query.x - parentSplitValue
        	    : query.y - parentSplitValue;
        double distanceToSplitSq = diff * diff;

        if (distanceToSplitSq < bestDistance) {
          currentNode = otherNode;
          isXLevel = !parentSplitAxis;
        } else {
          currentNode = null;
        }
      }
    }

    return bestNode;
  }
  
  /**
   * Finds the nearest N nodes in the tree to the given query point.
   * 
   * @param query the query point
   * @param n the number of nearest nodes to find
   * @return a list of the nearest nodes, sorted by distance (closest first), or an empty list if the tree is empty.
   */
  public List<KdNode> nearestNeighbors(final Coordinate query, final int n) {
	    if (root == null || n <= 0) {
	      return Collections.emptyList();
	    }

	    PriorityQueue<KdNode> heap = new PriorityQueue<>(n, (n1, n2) -> 
	        Double.compare(query.distanceSq(n2.getCoordinate()), query.distanceSq(n1.getCoordinate()))
	    );

	    Deque<NNStackFrame> stack = new ArrayDeque<>();
	    KdNode currentNode = root;
	    boolean isXLevel = true;

	    while (currentNode != null || !stack.isEmpty()) {
	      if (currentNode != null) {
	        double currentDist = query.distanceSq(currentNode.getCoordinate());
            if (heap.size() < n || currentDist < query.distanceSq(heap.peek().getCoordinate())) {
                if (heap.size() == n) {
                    heap.poll();
                }
                heap.offer(currentNode);
            }

	        boolean currentIsXLevel = isXLevel;
	        double splitValue = currentNode.splitValue(currentIsXLevel);
	        KdNode nextNode;
	        KdNode otherNode;

	        if (currentIsXLevel) {
	          if (query.x < splitValue) {
	            nextNode = currentNode.getLeft();
	            otherNode = currentNode.getRight();
	          } else {
	            nextNode = currentNode.getRight();
	            otherNode = currentNode.getLeft();
	          }
	        } else {
	          if (query.y < splitValue) {
	            nextNode = currentNode.getLeft();
	            otherNode = currentNode.getRight();
	          } else {
	            nextNode = currentNode.getRight();
	            otherNode = currentNode.getLeft();
	          }
	        }

	        stack.push(new NNStackFrame(otherNode, currentIsXLevel, splitValue));
	        currentNode = nextNode;
	        isXLevel = !currentIsXLevel;
	      } else {
	        NNStackFrame frame = stack.pop();
	        KdNode otherNode = frame.node;
	        boolean parentSplitAxis = frame.parentSplitAxis;
	        double parentSplitValue = frame.parentSplitValue;

	        double diff = parentSplitAxis 
	        	    ? query.x - parentSplitValue
	        	    : query.y - parentSplitValue;
	        double distanceToSplitSq = diff * diff;

	        double currentMaxDist = heap.isEmpty() ? Double.POSITIVE_INFINITY : query.distanceSq(heap.peek().getCoordinate());

	        if (distanceToSplitSq < currentMaxDist || heap.size() < n) {
	          currentNode = otherNode;
	          isXLevel = !parentSplitAxis;
	        } else {
	          currentNode = null;
	        }
	      }
	    }

	    List<KdNode> result = new ArrayList<>(heap);
	    Collections.sort(result, (n1, n2) -> 
	    	Double.compare(query.distanceSq(n1.getCoordinate()), query.distanceSq(n2.getCoordinate()))
	    );
	    return result;
	  }
  
  private static class NNStackFrame {
	    KdNode node;
	    boolean parentSplitAxis;
	    double parentSplitValue;

	    NNStackFrame(KdNode node, boolean parentSplitAxis, double parentSplitValue) {
	      this.node = node;
	      this.parentSplitAxis = parentSplitAxis;
	      this.parentSplitValue = parentSplitValue;
	    }
	  }
    
  /**
   * Finds the node in the tree which is the best match for a point
   * being inserted.
   * The match is made deterministic by returning the lowest of any nodes which
   * lie the same distance from the point.
   * There may be no match if the point is not within the distance tolerance of any
   * existing node.
   * 
   * @param p the point being inserted
   * @return the best matching node. null if no match was found.
   */
  public KdNode findBestMatchNode(Coordinate p) {
    BestMatchVisitor visitor = new BestMatchVisitor(p, tolerance);
    query(visitor.queryEnvelope(), visitor);
    return visitor.getNode();
  }

  static private class BestMatchVisitor implements KdNodeVisitor {

    private double tolerance;
    private KdNode matchNode = null;
    private double matchDist = 0.0;
    private Coordinate p;
    
    public BestMatchVisitor(Coordinate p, double tolerance) {
      this.p = p;
      this.tolerance = tolerance;
    }
    
    public Envelope queryEnvelope() {
      Envelope queryEnv = new Envelope(p);
      queryEnv.expandBy(tolerance);
      return queryEnv;
    }

    public KdNode getNode() {
      return matchNode;
    }

    public void visit(KdNode node) {
      double dist = p.distance(node.getCoordinate());
      boolean isInTolerance =  dist <= tolerance; 
      if (! isInTolerance) return;
      boolean update = false;
      if (matchNode == null
          || dist < matchDist
          // if distances are the same, record the lesser coordinate
          || (matchNode != null && dist == matchDist 
          && node.getCoordinate().compareTo(matchNode.getCoordinate()) < 1))
        update = true;

      if (update) {
        matchNode = node;
        matchDist = dist;
      }
    }
  }
  
  /**
   * Inserts a point known to be beyond the distance tolerance of any existing node.
   * The point is inserted at the bottom of the exact splitting path, 
   * so that tree shape is deterministic.
   * 
   * @param p the point to insert
   * @param data the data for the point
   * @return the created node
   */
  private KdNode insertExact(Coordinate p, Object data) {
    KdNode currentNode = root;
    KdNode leafNode = root;
    boolean isXLevel = true;
    boolean isLessThan = true;

    /**
     * Traverse the tree, first cutting the plane left-right (by X ordinate)
     * then top-bottom (by Y ordinate)
     */
    while (currentNode != null) {
      boolean isInTolerance = p.distanceSq(currentNode.getCoordinate()) <= toleranceSq;

      // check if point is already in tree (up to tolerance) and if so simply
      // return existing node
      if (isInTolerance) {
        currentNode.increment();
        return currentNode;
      }

      double splitValue = currentNode.splitValue(isXLevel);
      if (isXLevel) {
        isLessThan = p.x < splitValue;
      } else {
        isLessThan = p.y < splitValue;
      }
      leafNode = currentNode;
      if (isLessThan) {
        currentNode = currentNode.getLeft();
      } else {
        currentNode = currentNode.getRight();
      }

      isXLevel = ! isXLevel;
    }
    //System.out.println("<<");
    // no node found, add new leaf node to tree
    numberOfNodes = numberOfNodes + 1;
    KdNode node = new KdNode(p, data);
    if (isLessThan) {
      leafNode.setLeft(node);
    } else {
      leafNode.setRight(node);
    }
    return node;
  }

  /**
   * Performs a range search of the points in the index and visits all nodes found.
   * 
   * @param queryEnv the range rectangle to query
   * @param visitor a visitor to visit all nodes found by the search
   */
  public void query(Envelope queryEnv, KdNodeVisitor visitor) {
    //-- Deque is faster than Stack
    Deque<QueryStackFrame> queryStack = new ArrayDeque<QueryStackFrame>();
    KdNode currentNode = root;
    boolean isXLevel = true;

    // search is computed via in-order traversal
    while (true) {
      if ( currentNode != null ) {
        queryStack.push(new QueryStackFrame(currentNode, isXLevel));

        boolean searchLeft = currentNode.isRangeOverLeft(isXLevel, queryEnv);
        if ( searchLeft ) {
          currentNode = currentNode.getLeft();
          if ( currentNode != null ) {
            isXLevel = ! isXLevel;
          }
        } 
        else {
          currentNode = null;
        }
      } 
      else if ( ! queryStack.isEmpty() ) {
        // currentNode is empty, so pop stack
        QueryStackFrame frame = queryStack.pop();
        currentNode = frame.getNode();
        isXLevel = frame.isXLevel();

        //-- check if search matches current node
        if ( queryEnv.contains(currentNode.getCoordinate()) ) {
          visitor.visit(currentNode);
        }

        boolean searchRight = currentNode.isRangeOverRight(isXLevel, queryEnv);
        if ( searchRight ) {
          currentNode = currentNode.getRight();
          if ( currentNode != null ) {
            isXLevel = ! isXLevel;
          }
        } 
        else {
          currentNode = null;
        }
      } else {
        //-- stack is empty and no current node
        return;
      }
    }
  }

  private static class QueryStackFrame {
    private KdNode node;
    private boolean isXLevel = false;
    
    public QueryStackFrame(KdNode node, boolean isXLevel) {
      this.node = node;
      this.isXLevel = isXLevel;
    }
    
    public KdNode getNode() {
      return node;
    }
    
    public boolean isXLevel() {
      return isXLevel;
    }
  }
  
  /**
   * Performs a range search of the points in the index.
   * 
   * @param queryEnv the range rectangle to query
   * @return a list of the KdNodes found
   */
  public List<KdNode> query(Envelope queryEnv) {
    final List<KdNode> result = new ArrayList<KdNode>();
    query(queryEnv, result);
    return result;
  }

  /**
   * Performs a range search of the points in the index.
   * 
   * @param queryEnv
   *          the range rectangle to query
   * @param result
   *          a list to accumulate the result nodes into
   */
  public void query(Envelope queryEnv, final List<KdNode> result) {
    query(queryEnv, new KdNodeVisitor() {

      public void visit(KdNode node) {
        result.add(node);
      }
      
    });
  }

  /**
   * Searches for a given point in the index and returns its node if found.
   * 
   * @param queryPt the point to query
   * @return the point node, if it is found in the index, or null if not
   */
  public KdNode query(Coordinate queryPt) {
    KdNode currentNode = root;
    boolean isXLevel = true;
    
    while (currentNode != null) {
      if ( currentNode.getCoordinate().equals2D(queryPt) )
        return currentNode;

      boolean searchLeft = currentNode.isPointOnLeft(isXLevel, queryPt);
      if ( searchLeft ) {
        currentNode = currentNode.getLeft();
      } else {
        currentNode = currentNode.getRight();
      }
      isXLevel = ! isXLevel;
    }
    //-- point not found
    return null;           
  }
  
	/**
	 * Performs an in-order traversal of the tree, collecting and returning all
	 * nodes that have been inserted.
	 * 
	 * @return A list containing all nodes in the KdTree. Returns an empty list if
	 *         the tree is empty.
	 */
	public List<KdNode> getNodes() {
		List<KdNode> nodeList = new ArrayList<>();
		if (root == null) {
			return nodeList; // empty list for empty tree
		}

		Deque<KdNode> stack = new ArrayDeque<>();
		KdNode currentNode = root;

		while (currentNode != null || !stack.isEmpty()) {
			if (currentNode != null) {
				stack.push(currentNode);
				currentNode = currentNode.getLeft();
			} else {
				currentNode = stack.pop();
				nodeList.add(currentNode);
				currentNode = currentNode.getRight();
			}
		}
		return nodeList;
	}

  /**
   * Computes the depth of the tree.
   * 
   * @return the depth of the tree
   */
  public int depth() {
    return depthNode(root);
  }
  
  private int depthNode(KdNode currentNode) {
    if (currentNode == null)
      return 0;

    int dL = depthNode(currentNode.getLeft());
    int dR = depthNode(currentNode.getRight());
    return 1 + (dL > dR ? dL : dR);
  }
  
  /**
   * Computes the size (number of items) in the tree.
   * 
   * @return the size of the tree
   */
  public int size() {
    return sizeNode(root);
  }
  
  private int sizeNode(KdNode currentNode) {
    if (currentNode == null)
      return 0;

    int sizeL = sizeNode(currentNode.getLeft());
    int sizeR = sizeNode(currentNode.getRight());
    return 1 + sizeL + sizeR;
  }
  
}
