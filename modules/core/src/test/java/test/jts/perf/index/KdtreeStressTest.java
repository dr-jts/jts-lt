package test.jts.perf.index;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.index.kdtree.KdTree;

/**
 * Tests an issue where deep KdTrees caused a {@link StackOverflowError}
 * when using a recursive query implementation.
 * 
 * See a fix for this issue in GEOS 
 * at https://github.com/libgeos/geos/pull/481.
 * 
 * @author mdavis
 *
 */
public class KdtreeStressTest {

  // In code with recursive query 50,000 points causes StackOverflowError
  int NUM_PTS = 50000;
  
  public static void main(String[] args) throws Exception
  {
    KdtreeStressTest test = new KdtreeStressTest();
    test.run();
  }

  private void run() {
    System.out.format("Loading iIndex with %d points\n", NUM_PTS);
    KdTree index = createUnbalancedTree(NUM_PTS);
    
    System.out.format("Querying Index loaded with %d points\n", NUM_PTS);
    for (int i = 0; i < NUM_PTS; i++) {
      Envelope env = new Envelope(i, i + 10, 0, 1);
      index.query(env);
    }
    System.out.format("Queries complete\n");
  }
  
  

  /**
   * Create an unbalanced tree by loading a 
   * series of monotonically increasing points
   * 
   * @param numPts number of points to load
   * @return a new index
   */
  private KdTree createUnbalancedTree(int numPts) {
    KdTree index = new KdTree();
    for (int i = 0; i < numPts; i++) {
      Coordinate pt = new Coordinate(i, 0);
      index.insert(pt);
    }
    return index;
  }

}
