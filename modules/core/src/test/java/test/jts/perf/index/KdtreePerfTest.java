package test.jts.perf.index;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.index.kdtree.KdNode;
import org.locationtech.jts.index.kdtree.KdTree;

import test.jts.perf.PerformanceTestCase;
import test.jts.perf.PerformanceTestRunner;

public class KdtreePerfTest extends PerformanceTestCase {

	public static void main(String args[]) {
		PerformanceTestRunner.run(KdtreePerfTest.class);
	}

	public KdtreePerfTest(String name) {
		super(name);
		setRunSize(new int[] { 100_000, 1_000_000, 5_000_000 });
		setRunIterations(1);
	}

	@Override
	public void setUp() throws Exception {
		System.out.println("KDtree nearest neighbors query perf test");
	}

	@Override
	public void startRun(int size) {
		testNearestNeighborsPerformance(size);
	}

	private void testNearestNeighborsPerformance(int n) {
		int k = n / 100;
		KdTree tree = new KdTree();
		Random rand = new Random();

		List<Coordinate> points = new ArrayList<>();
		for (int i = 0; i < n; i++) {
			double x = rand.nextDouble();
			double y = rand.nextDouble();
			points.add(new Coordinate(x, y));
		}

		long startTime = System.nanoTime();
		for (Coordinate coordinate : points) {
			tree.insert(coordinate);
		}
		long insertTime = System.nanoTime() - startTime;
		System.out.println("Kdtree: Time to insert " + n + " points: " + (insertTime / 1_000_000) + " ms");

		Coordinate query = new Coordinate(rand.nextDouble(), rand.nextDouble());

		// Time k-NN query using k-d tree
		startTime = System.nanoTime();
		List<KdNode> nearest = tree.nearestNeighbors(query, k);
		long knnTime = System.nanoTime() - startTime;
		System.out.println("Kdtree: Time to find " + k + " nearest neighbors: " + (knnTime / 1_000_000) + " ms");

		// Time k-NN query using brute-force
		startTime = System.nanoTime();
		List<Coordinate> bruteForceNearest = bruteForceNearestNeighbors(tree, query, k);
		long bruteForceTime = System.nanoTime() - startTime;
		System.out.println("brute-force: time to find " + k + " nearest neighbors: " + (bruteForceTime / 1_000_000) + " ms");

		System.out.println("");
	}

	private List<Coordinate> bruteForceNearestNeighbors(KdTree tree, Coordinate query, int k) {
		List<Coordinate> allPoints = getAllPoints(tree);

		// Sort all points by distance to the query point
		allPoints.sort(Comparator.comparingDouble(point -> query.distance(point)));

		// Return the first k points (ordered closest first)
		return allPoints.subList(0, Math.min(k, allPoints.size()));
	}

	private List<Coordinate> getAllPoints(KdTree tree) {
		return Arrays.stream(KdTree.toCoordinates(tree.getNodes())).collect(Collectors.toList());
	}

}
