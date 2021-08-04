package test.jts.perf.geom.prep;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Assert;
import org.junit.Test;
import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.GeometryFactory;
import org.locationtech.jts.geom.Point;
import org.locationtech.jts.geom.prep.PreparedGeometry;
import org.locationtech.jts.geom.prep.PreparedGeometryFactory;
import org.locationtech.jts.io.WKTReader;

/**
 * Tests race condition in {@link org.locationtech.jts.index.intervalrtree.SortedPackedIntervalRTree}.
 * See https://github.com/locationtech/jts/pull/746
 * 
 * To achieve maximum reproducibility
 * please run this test JVM option " -Djava.compiler=NONE ".
 * 
 */
public class PreparedGeometryConcurrencyBugTest {
    private final PreparedGeometry preparedGeometry;

    public PreparedGeometryConcurrencyBugTest() throws Exception {
        Geometry geometry = new WKTReader().read("MULTIPOLYGON (((-68.3233207378872 45.95414744388297, -68.3233207378872 46.36350003948202, -67.78323905592147 46.36350003948202, -67.7831824582579 46.360613558640125, -67.78313216656608 46.35737980285657, -67.78303661235164 46.35108931804467, -67.78300844900423 46.34914504123918, -67.78284449808892 46.33717561858782, -67.78266847716758 46.325590424461865, -67.78267250050291 46.32359786763226, -67.78259505129752 46.31960370146851, -67.78254576543955 46.3175417421042, -67.78251860792597 46.31565680949507, -67.78243110038221 46.309206397103196, -67.78231542949105 46.29783343391671, -67.78229631864816 46.29618990142828, -67.78218668276001 46.28645443572722, -67.78211526855763 46.2793814121907, -67.78211325688996 46.27917320458661, -67.78208710521022 46.275919332126335, -67.78205491852746 46.27178233755782, -67.78201367934017 46.266482599074635, -67.78199859183263 46.264539328102984, -67.78198853349427 46.25005029169182, -67.78186381009857 46.23839468919735, -67.7818255884128 46.235723194528276, -67.7817923958962 46.23238684369343, -67.78174311003822 46.22733152283243, -67.78173707503521 46.22676624421646, -67.78164654998994 46.21753570710118, -67.78163850331926 46.21706397103199, -67.7815147857574 46.20460671897001, -67.7814041440354 46.19073627036815, -67.7813679340173 46.18616877891773, -67.78136692818346 46.18602494467913, -67.78130255481794 46.17465499899416, -67.78129450814725 46.173130154898416, -67.78127539730437 46.169318044659036, -67.78123616978475 46.15945986722994, -67.78119895393282 46.146595252464294, -67.7811959364313 46.14540736270368, -67.7811848722591 46.13702776101388, -67.78116173808087 46.13562663447999, -67.78116173808087 46.134921544960775, -67.78128545564273 46.13293804063568, -67.78117984308992 46.12505330919333, -67.78099577549789 46.12165359082678, -67.78096660631664 46.11688795011065, -67.78094749547375 46.11094749547374, -67.78098269965801 46.105255481794416, -67.78098873466104 46.10430597465299, -67.78091128545564 46.097327499497084, -67.78094548380608 46.09648662240997, -67.78093844296922 46.09618990142829, -67.78092737879702 46.095644739489046, -67.78084691209013 46.091684771675716, -67.78087205793602 46.088170388251854, -67.7808851337759 46.086454435727205, -67.78085596459465 46.07591329712332, -67.78081271373969 46.07353651176825, -67.78073627036814 46.06928082880708, -67.78069503118085 46.06146650573325, -67.78064574532287 46.05410883122107, -67.7806437336552 46.05378193522429, -67.78060350030175 46.04886944276805, -67.78058036612352 46.046000804667074, -67.7804385435526 46.038453027559854, -67.7805602494468 46.03127036813518, -67.78063971031986 46.026529873264934, -67.78052806276403 46.021696841681745, -67.78044659022329 46.01135184067593, -67.78046167773084 46.00444679139005, -67.78057634278817 46.00098672299336, -67.7806075236371 46.00005632669482, -67.7807020720177 45.99244518205592, -67.7807966203983 45.984834037417016, -67.78074331120499 45.984817944075644, -67.78107825387247 45.973686381009855, -67.78134479983906 45.96554817944076, -67.78128243814122 45.96297425065379, -67.78130356065178 45.95796218064777, -67.78124132526604 45.95414744388297, -68.3233207378872 45.95414744388297)))");
        preparedGeometry = new PreparedGeometryFactory().create(geometry);
    }

    void test0() throws Exception {
        List<Callable<Boolean>> tasks = IntStream.range(0, 50).mapToObj(i -> (Callable<Boolean>) this::getForPoint).collect(Collectors.toList());
        ExecutorService service = Executors.newFixedThreadPool(20);
        List<Future<Boolean>> list = service.invokeAll(tasks);
        for (Future<Boolean> f : list) {
            Boolean su = f.get();
            Assert.assertTrue(su);
        }
    }

    @Test
    public void test() throws Exception {
        for (int i=0; i<100; i++) {
            new PreparedGeometryConcurrencyBugTest().test0();
        }
    }

    private boolean getForPoint() {
        return getFor(-67.78258, 46.13388705);
    }

    public boolean getFor(final double x, final double y) {
        List<Boolean> speedUnits = IntStream.range(0, 5)
                                            .mapToObj(i -> getForEx(x, y))
                                            .collect(Collectors.toList());
        Boolean firstSpeedUnit = speedUnits.get(0);
        boolean allMatch = speedUnits.stream().allMatch(unit -> unit.equals(firstSpeedUnit));

        Assert.assertTrue("Inconsistent result: " + speedUnits, allMatch);
        return firstSpeedUnit;
    }

    private boolean getForEx(double x, double y) {
        Point point =  new GeometryFactory().createPoint(new Coordinate(x, y));
        return preparedGeometry.contains(point);
    }
}
