package org.locationtech.jts.geom;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import junit.textui.TestRunner;
import org.locationtech.jts.geom.impl.PackedCoordinateSequenceFactory;
import org.locationtech.jts.io.WKTReader;
import org.locationtech.jts.util.AssertionFailedException;

/**
 * Test validity and simplicity cases.
 * <p>
 * Goals: Exhaustively test basic OGC and ISO validity/simplicity cases.
 * Non-goals: Test numerical precision, or test complicated interactions and subtle bugs.
 */
public class ValiditySimplicityTest
        extends TestCase
{
    PrecisionModel precisionModel = new PrecisionModel(1000);
    GeometryFactory geometryFactory = new GeometryFactory(precisionModel, 0);
    PackedCoordinateSequenceFactory packedCoordFactory = new PackedCoordinateSequenceFactory();
    WKTReader reader = new WKTReader(geometryFactory);

    public static void main(String args[])
    {
        TestRunner.run(suite());
    }

    public ValiditySimplicityTest(String name) { super(name); }

    public static Test suite() { return new TestSuite(ValiditySimplicityTest.class); }

    public void testPointIsValid()
    {
        assertTrue(geometryFactory.createPoint().isValid());
        assertTrue(!geometryFactory.createPoint(new Coordinate(Double.NaN, Double.NaN)).isValid());
        assertTrue(!geometryFactory.createPoint(new Coordinate(Double.NaN, 0)).isValid());
        assertTrue(!geometryFactory.createPoint(new Coordinate(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)).isValid());
        assertTrue(!geometryFactory.createPoint(new Coordinate(Double.POSITIVE_INFINITY, 0)).isValid());
        assertTrue(!geometryFactory.createPoint(new Coordinate(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY)).isValid());
        assertTrue(!geometryFactory.createPoint(new Coordinate(0, Double.NEGATIVE_INFINITY)).isValid());
        assertTrue(geometryFactory.createPoint(new Coordinate(Double.MIN_VALUE, 1)).isValid());
    }

    public void testPointIsSimple()
    {
        // Should all non-valid points be non-simple?
        assertTrue(geometryFactory.createPoint().isSimple());
        assertTrue(geometryFactory.createPoint(new Coordinate(Double.NaN, Double.NaN)).isSimple());
        assertTrue(geometryFactory.createPoint(new Coordinate(Double.NaN, 0)).isSimple());
        assertTrue(geometryFactory.createPoint(new Coordinate(Double.POSITIVE_INFINITY, Double.POSITIVE_INFINITY)).isSimple());
        assertTrue(geometryFactory.createPoint(new Coordinate(Double.POSITIVE_INFINITY, 0)).isSimple());
        assertTrue(geometryFactory.createPoint(new Coordinate(Double.NEGATIVE_INFINITY, Double.NEGATIVE_INFINITY)).isSimple());
        assertTrue(geometryFactory.createPoint(new Coordinate(0, Double.NEGATIVE_INFINITY)).isSimple());
        assertTrue(geometryFactory.createPoint(new Coordinate(Double.MIN_VALUE, 1)).isSimple());
    }

    public void testLineStringIsValidAndSimple()
    {
        LineString empty = geometryFactory.createLineString();
        assertTrue(empty.isValid());
        assertTrue(empty.isSimple());

        // XXX ISO: null coordinate sequence invalid under ISO, but reasonable
        assertTrue(geometryFactory.createLineString((Coordinate[]) null).isValid());
        assertTrue(geometryFactory.createLineString((Coordinate[]) null).isSimple());

        assertTrue(geometryFactory.createLineString(new Coordinate[] {}).isValid());
        assertTrue(geometryFactory.createLineString(new Coordinate[] {}).isSimple());

        try {
            geometryFactory.createLineString(new Coordinate[] {new Coordinate(0, 0)});
            fail("LineStrings with one point should fail on creation.");
        }
        catch (IllegalArgumentException e) {
            // expected
        }

        LineString basic = geometryFactory.createLineString(new Coordinate[] {
                new Coordinate(0, 0), new Coordinate(1, 1)
        });
        assertTrue(basic.isValid());
        assertTrue(basic.isSimple());

        LineString withNull = geometryFactory.createLineString(new Coordinate[] {
                new Coordinate(0, 0), new Coordinate(1, 1), null
        });
        try {
            // XXX ISO: Not valid.  Should throw NPE on construction?
            withNull.isValid();
            fail("LineStrings with null points throw NPEs on isValid.");
        }
        catch (NullPointerException e) {
            // expected
        }
        try {
            // XXX ISO: Not valid.  Should throw NPE on construction?
            withNull.isSimple();
            fail("LineStrings with null points throw NPEs on isSimple.");
        }
        catch (NullPointerException e) {
            // expected
        }

        LineString selfOverlap = geometryFactory.createLineString(packCoords(
                0, 0, 1, 1, 0, 0
        ));
        assertTrue(selfOverlap.isValid());
        assertTrue(!selfOverlap.isSimple());

        // XXX ISO: Invalid due to repeated consecutive points
        LineString repeatedConsecutive = geometryFactory.createLineString(packCoords(
                0, 0, 1, 1, 1, 1
        ));
        assertTrue(repeatedConsecutive.isValid());
        assertTrue(repeatedConsecutive.isSimple());

        LineString selfIntersection = geometryFactory.createLineString(packCoords(
                0, 0, 1, 1, 1, 0, 0, 1
        ));
        assertTrue(selfIntersection.isValid());
        assertTrue(!selfIntersection.isSimple());

        LineString selfIntersectionOnBoundary = geometryFactory.createLineString(packCoords(
                0, 0, 1, 1, 1, 0, 0.5, 0.5
        ));
        assertTrue(selfIntersectionOnBoundary.isValid());
        assertTrue(!selfIntersectionOnBoundary.isSimple());

        LineString selfIntersectionOnJoint = geometryFactory.createLineString(packCoords(
                0, 0, 0.5, 0.5, 1, 1, 1, 0, 0, 1
        ));
        assertTrue(selfIntersectionOnJoint.isValid());
        assertTrue(!selfIntersectionOnJoint.isSimple());

        LineString selfIntersectionOnBoundaryAndJoint = geometryFactory.createLineString(packCoords(
                0, 0, 0.5, 0.5, 1, 1, 1, 0, 0.5, 0.5
        ));
        assertTrue(selfIntersectionOnBoundaryAndJoint.isValid());
        assertTrue(!selfIntersectionOnBoundaryAndJoint.isSimple());

        LineString loop = geometryFactory.createLineString(packCoords(0, 0, 0, 2, 1, 2, 0, 0));
        assertTrue(loop.isValid());
        assertTrue(loop.isSimple());

        LineString withNaN = geometryFactory.createLineString(new Coordinate[] {
                new Coordinate(Double.NaN, 0), new Coordinate(1, 1)
        });
        assertTrue(!withNaN.isValid());
        assertTrue(withNaN.isSimple());

        LineString withPosInf = geometryFactory.createLineString(new Coordinate[] {
                new Coordinate(Double.POSITIVE_INFINITY, 0), new Coordinate(1, 1)
        });
        assertTrue(!withPosInf.isValid());
        assertTrue(withPosInf.isSimple());

        LineString withNegInf = geometryFactory.createLineString(new Coordinate[] {
                new Coordinate(Double.NEGATIVE_INFINITY, 0), new Coordinate(1, 1)
        });
        assertTrue(!withNegInf.isValid());
        assertTrue(withNegInf.isSimple());

        LineString withSubnormal = geometryFactory.createLineString(new Coordinate[] {
                new Coordinate(Double.MIN_VALUE, 0), new Coordinate(1, 1)
        });
        assertTrue(withSubnormal.isValid());
        assertTrue(withSubnormal.isSimple());
    }

    public void testPolygonIsValidAndSimple()
    {
        try {
            geometryFactory.createPolygon(new Coordinate[] {
                    new Coordinate(0, 0), new Coordinate(1, 1)
            });
            fail("Polygon loop must have >3 vertices");
        }
        catch (IllegalArgumentException e) {
            // expected
        }

        Polygon polyWithNullCoord = geometryFactory.createPolygon(new Coordinate[] {
                new Coordinate(0, 0),
                new Coordinate(1, 1),
                new Coordinate(1, 0),
                null,
                new Coordinate(0, 0)
        });
        try {
            // XXX: It should probably fail on construction or return !isValid.
            polyWithNullCoord.isValid();
            fail("Polygon loop must not have null points");
        }
        catch (NullPointerException e) {
            // expected
        }
        try {
            // XXX: It should probably fail on construction or return !isSimple.
            polyWithNullCoord.isSimple();
            fail("Polygon loop must not have null points");
        }
        catch (NullPointerException e) {
            // expected
        }

        Polygon empty = geometryFactory.createPolygon();
        assertTrue(empty.isValid());
        assertTrue(empty.isSimple());

        Polygon triangle = geometryFactory.createPolygon(packCoords(0, 0, 1, 1, 1, 0, 0, 0));
        assertTrue(triangle.isValid());
        assertTrue(triangle.isSimple());

        Polygon triangleNaN = geometryFactory.createPolygon(
                packCoords(0, 0, 1, 1, 1, 0, Double.NaN, 0, 0, 0)
        );
        assertTrue(!triangleNaN.isValid());
        try {
            // XXX: should this fail more gracefully?
            triangleNaN.isSimple();
            fail("Can't call isSimple on polygon with NaN coordinate.");
        }
        catch (org.locationtech.jts.util.AssertionFailedException e) {
            // expected
        }

        Polygon trianglePosInfinity = geometryFactory.createPolygon(
                packCoords(0, 0, 1, 1, 1, 0, Double.POSITIVE_INFINITY, 0, 0, 0)
        );
        assertTrue(!trianglePosInfinity.isValid());
        assertTrue(!trianglePosInfinity.isSimple());

        Polygon triangleNegInfinity = geometryFactory.createPolygon(
                packCoords(0, 0, 1, 1, 1, 0, Double.NEGATIVE_INFINITY, 0, 0, 0)
        );
        assertTrue(!triangleNegInfinity.isValid());
        assertTrue(!triangleNegInfinity.isSimple());

        Polygon triangleSubNormal = geometryFactory.createPolygon(
                packCoords(0, 0, 1, 1, 1, 0, Double.MIN_VALUE, 0, 0, 0)
        );
        assertTrue(triangleSubNormal.isValid());
        assertTrue(triangleSubNormal.isSimple());

        Polygon sidewaysHourglass = geometryFactory.createPolygon(
                packCoords(0, 0, 1, 1, 1, 0, 0, 1, 0, 0)
        );
        assertTrue(!sidewaysHourglass.isValid());
        assertTrue(!sidewaysHourglass.isSimple());

        Polygon danglingLozenge = geometryFactory.createPolygon(geometryFactory.createLinearRing(
                packCoords(-4, -4, 4, -4, 4, 4, -4, 4, -4, -4)
                ), // Exterior ring
                new LinearRing[] {
                        geometryFactory.createLinearRing(packCoords(2, 2, 3, 3, 2, 4, 1, 3, 2, 2))
                });
        assertTrue(danglingLozenge.isValid());
        assertTrue(danglingLozenge.isSimple());

        Polygon twoPointLozenge = geometryFactory.createPolygon(geometryFactory.createLinearRing(
                packCoords(0, 0, 4, 0, 4, 4, 0, 4, 0, 0)
                ), // Exterior ring
                new LinearRing[] {
                        geometryFactory.createLinearRing(packCoords(2, 2, 4, 3, 2, 4, 1, 3, 2, 2))
                });
        assertTrue(!twoPointLozenge.isValid());
        assertTrue(twoPointLozenge.isSimple());

        // Invalid under ISO; polygon interior not connected
        Polygon internalHourglass = geometryFactory.createPolygon(geometryFactory.createLinearRing(
                packCoords(0, 0, 4, 0, 4, 4, 0, 4, 0, 0)
                ), // Exterior ring
                new LinearRing[] {
                        geometryFactory.createLinearRing(packCoords(2, 2, 3, 3, 2, 4, 1, 3, 2, 2)),
                        geometryFactory.createLinearRing(packCoords(2, 2, 1, 1, 2, 0, 3, 1, 2, 2))
                });
        assertTrue(!internalHourglass.isValid());
        assertTrue(internalHourglass.isSimple());

        // Invalid under ISO; interior rings touch at two points
        Polygon internalLozengeWithHole = geometryFactory.createPolygon(geometryFactory.createLinearRing(
                packCoords(0, 0, 4, 0, 4, 4, 0, 4, 0, 0)
                ), // Exterior ring
                new LinearRing[] {
                        geometryFactory.createLinearRing(packCoords(2, 3.5, 3, 3, 2, 4, 1, 3, 2, 3.5)),
                        geometryFactory.createLinearRing(packCoords(2, 2, 3, 3, 2, 2.5, 1, 3, 2, 2))
                });
        assertTrue(!internalLozengeWithHole.isValid());
        assertTrue(internalLozengeWithHole.isSimple());

        Polygon internalHalvedLozenge = geometryFactory.createPolygon(geometryFactory.createLinearRing(
                packCoords(-4, -4, 4, -4, 4, 4, -4, 4, -4, -4)
                ), // Exterior ring
                new LinearRing[] {
                        geometryFactory.createLinearRing(packCoords(3, 3, 2, 4, 1, 3, 3, 3)),
                        geometryFactory.createLinearRing(packCoords(3, 3, 1, 3, 2, 2, 3, 3))
                });
        assertTrue(!internalHalvedLozenge.isValid());
        assertTrue(internalHalvedLozenge.isSimple());

        Polygon polygonWithCut = geometryFactory.createPolygon(geometryFactory.createLinearRing(
                packCoords(0, 0, 2, 0, 2, 2, 1, 2, 1, 1, 1, 2, 0, 2, 0, 0)
        ));
        assertTrue(!polygonWithCut.isValid());
        assertTrue(!polygonWithCut.isSimple());

        Polygon polygonWithSpike = geometryFactory.createPolygon(geometryFactory.createLinearRing(
                packCoords(0, 0, 2, 0, 2, 2, 1, 2, 1, 3, 1, 2, 0, 2, 0, 0)
        ));
        assertTrue(!polygonWithSpike.isValid());
        assertTrue(!polygonWithSpike.isSimple());
    }

    public void testMultipointValidAndSimple()
    {
        MultiPoint empty = geometryFactory.createMultiPoint();
        assertTrue(empty.isValid());
        assertTrue(empty.isSimple());

        // XXX ISO: Multipoints with empty points are invalid
        MultiPoint mpWithEmpty = geometryFactory.createMultiPoint(new Point[] {
                geometryFactory.createPoint()
        });
        assertTrue(mpWithEmpty.isValid());
        assertTrue(mpWithEmpty.isSimple());

        // XXX ISO: Multipoints with empty points are invalid
        MultiPoint mpWithSomeEmpty = geometryFactory.createMultiPoint(new Point[] {
                geometryFactory.createPoint(), geometryFactory.createPoint(new Coordinate(0, 0))
        });
        assertTrue(mpWithSomeEmpty.isValid());
        try {
            mpWithSomeEmpty.isSimple();
            fail("Empty point throws NPE");
        }
        catch (NullPointerException e) {
            // expected
        }

        try {
            geometryFactory.createMultiPoint(new Point[] {
                    geometryFactory.createPoint(new Coordinate(0, 0)), null
            });
            fail("MultiPoints cannot be constructed with null points.");
        }
        catch (IllegalArgumentException e) {
            // expected
        }

        // XXX ISO: MultiPoints with non-valid points are not valid
        MultiPoint withNaN = geometryFactory.createMultiPoint(new Point[] {
                geometryFactory.createPoint(new Coordinate(Double.NaN, 0))
        });
        assertTrue(withNaN.isValid());
        assertTrue(withNaN.isSimple());

        // XXX ISO: MultiPoints with non-valid points are not valid
        MultiPoint withPosInfinity = geometryFactory.createMultiPoint(new Point[] {
                geometryFactory.createPoint(new Coordinate(Double.POSITIVE_INFINITY, 0))
        });
        assertTrue(withPosInfinity.isValid());
        assertTrue(withPosInfinity.isSimple());

        // XXX ISO: MultiPoints with non-valid points are not valid
        MultiPoint withNegInfinity = geometryFactory.createMultiPoint(new Point[] {
                geometryFactory.createPoint(new Coordinate(Double.NEGATIVE_INFINITY, 0))
        });
        assertTrue(withNegInfinity.isValid());
        assertTrue(withNegInfinity.isSimple());

        MultiPoint withSubnormal = geometryFactory.createMultiPoint(new Point[] {
                geometryFactory.createPoint(new Coordinate(Double.MIN_VALUE, 0))
        });
        assertTrue(withSubnormal.isValid());
        assertTrue(withSubnormal.isSimple());

        MultiPoint repeated = geometryFactory.createMultiPoint(packCoords(0, 0, 1, 1, 0, 0));
        assertTrue(repeated.isValid());
        assertTrue(!repeated.isSimple());

        // XXX ISO: MultiPoints with non-valid points are not valid
        MultiPoint repeatedNaN = geometryFactory.createMultiPoint(packCoords(Double.NaN, 0, Double.NaN, 0));
        assertTrue(repeatedNaN.isValid());
        assertTrue(!repeatedNaN.isSimple());
    }

    public void testMultiLineIsValidAndSimple()
    {
        MultiLineString empty = geometryFactory.createMultiLineString();
        assertTrue(empty.isValid());
        assertTrue(empty.isSimple());

        // XXX ISO: MultiGeometry with empty geometry is invalid
        MultiLineString withEmpty = geometryFactory.createMultiLineString(new LineString[] {geometryFactory.createLineString()});
        assertTrue(withEmpty.isValid());
        assertTrue(withEmpty.isSimple());

        // XXX ISO: MultiGeometry with empty geometry is invalid
        MultiLineString withSomeEmpty = geometryFactory.createMultiLineString(new LineString[] {
                geometryFactory.createLineString(),
                geometryFactory.createLineString(packCoords(0, 0, 1, 1)),
        });
        assertTrue(withSomeEmpty.isValid());
        assertTrue(withSomeEmpty.isSimple());

        try {
            geometryFactory.createMultiLineString(new LineString[] {
                    geometryFactory.createLineString(packCoords(0, 0, 1, 1)),
                    null
            });
            fail("Constructing with null geometries should fail on creation.");
        }
        catch (IllegalArgumentException e) {
            // expected
        }

        MultiLineString withInvalid = geometryFactory.createMultiLineString(new LineString[] {
                geometryFactory.createLineString(packCoords(Double.NaN, 0, 1, 1)),
        });
        assertTrue(!withInvalid.isValid());
        assertTrue(withInvalid.isSimple());

        MultiLineString withNonSimple = geometryFactory.createMultiLineString(new LineString[] {
                geometryFactory.createLineString(packCoords(0, 0, 1, 1, 1, 0, 0, 1)),
        });
        assertTrue(withNonSimple.isValid());
        assertTrue(!withNonSimple.isSimple());

        MultiLineString intersectingAtEndpoints = geometryFactory.createMultiLineString(new LineString[] {
                geometryFactory.createLineString(packCoords(0, 0, 1, 1)),
                geometryFactory.createLineString(packCoords(0, 0, 1, 0)),
                geometryFactory.createLineString(packCoords(0, 0, 0, 1)),
        });
        assertTrue(intersectingAtEndpoints.isValid());
        assertTrue(intersectingAtEndpoints.isSimple());

        MultiLineString intersectingEndpointMidpoint = geometryFactory.createMultiLineString(new LineString[] {
                geometryFactory.createLineString(packCoords(0, 0, 2, 0)),
                geometryFactory.createLineString(packCoords(1, 0, 1, 1)),
        });
        assertTrue(intersectingEndpointMidpoint.isValid());
        assertTrue(!intersectingEndpointMidpoint.isSimple());

        MultiLineString intersectingMidpointMidpoint = geometryFactory.createMultiLineString(new LineString[] {
                geometryFactory.createLineString(packCoords(0, 0, 2, 0)),
                geometryFactory.createLineString(packCoords(1, -1, 1, 1)),
        });
        assertTrue(intersectingMidpointMidpoint.isValid());
        assertTrue(!intersectingMidpointMidpoint.isSimple());
    }

    public void testMultiPolygonIsValidAndSimple()
    {
        MultiPolygon empty = geometryFactory.createMultiPolygon();
        assertTrue(empty.isValid());
        assertTrue(empty.isSimple());

        // XXX ISO: MultiGeometry with empty geometry is invalid
        MultiPolygon withEmpty = geometryFactory.createMultiPolygon(new Polygon[] {
                geometryFactory.createPolygon(),
        });
        assertTrue(withEmpty.isValid());
        assertTrue(withEmpty.isSimple());

        // XXX ISO: MultiGeometry with empty geometry is invalid
        MultiPolygon withSomeEmpty = geometryFactory.createMultiPolygon(new Polygon[] {
                geometryFactory.createPolygon(packCoords(0, 0, 1, 1, 1, 0, 0, 0)),
                geometryFactory.createPolygon(),
        });
        assertTrue(withSomeEmpty.isValid());
        assertTrue(withSomeEmpty.isSimple());

        try {
            geometryFactory.createMultiPolygon(new Polygon[] {
                    geometryFactory.createPolygon(packCoords(0, 0, 1, 1, 1, 0, 0, 0)),
                    null,
            });
            fail("Cannot create MultiGeometries with null geometries");
        }
        catch (IllegalArgumentException e) {
            // expected
        }

        // XXX ISO: MultiGeometry with invalid geometry is invalid
        MultiPolygon withInvalid = geometryFactory.createMultiPolygon(new Polygon[] {
                geometryFactory.createPolygon(packCoords(0, 0, 1, 1, 1, 0, 0, 1, 0, 0)),
        });
        assertTrue(!withInvalid.isValid());
        assertTrue(!withInvalid.isSimple());

        MultiPolygon withNaN = geometryFactory.createMultiPolygon(new Polygon[] {
                geometryFactory.createPolygon(
                        packCoords(0, 0, 1, 1, 1, 0, Double.NaN, 0, 0, 0)
                )
        });
        assertTrue(!withNaN.isValid());
        try {
            withNaN.isSimple();
            fail("Cannot calculate with NaN");
        }
        catch (AssertionFailedException e) {
            // expected
        }

        MultiPolygon withInf = geometryFactory.createMultiPolygon(new Polygon[] {
                geometryFactory.createPolygon(
                        packCoords(0, 0, 1, 1, 1, 0, Double.NEGATIVE_INFINITY, 0, 0, 0)
                )
        });
        assertTrue(!withInf.isValid());
        assertTrue(!withInf.isSimple());

        // XXX ISO: MultiGeometries with intersecting interiors are valid but not simple
        MultiPolygon overlapping = geometryFactory.createMultiPolygon(new Polygon[] {
                geometryFactory.createPolygon(packCoords(0, 0, 2, 0, 2, 1, 0, 1, 0, 0)),
                geometryFactory.createPolygon(packCoords(1, 0, 3, 0, 3, 1, 1, 1, 1, 0)),
        });
        assertTrue(!overlapping.isValid());
        assertTrue(overlapping.isSimple());

        // XXX ISO: MultiGeometries with intersecting boundaries are valid and simple
        MultiPolygon touching = geometryFactory.createMultiPolygon(new Polygon[] {
                geometryFactory.createPolygon(packCoords(0, 0, 1, 0, 1, 1, 0, 1, 0, 0)),
                geometryFactory.createPolygon(packCoords(1, 0, 2, 0, 2, 1, 1, 1, 1, 0)),
        });
        assertTrue(!touching.isValid());
        assertTrue(touching.isSimple());
    }

    public void testGeometryCollectionIsValidAndSimple()
    {
        GeometryCollection empty = geometryFactory.createGeometryCollection();
        assertTrue(empty.isValid());
        assertTrue(empty.isSimple());

        // XXX ISO: MultiGeometry with empty geometry is invalid
        GeometryCollection withEmpty = geometryFactory.createGeometryCollection(new Geometry[] {
                geometryFactory.createPoint()
        });
        assertTrue(withEmpty.isValid());
        assertTrue(withEmpty.isSimple());

        // XXX ISO: MultiGeometry with empty geometry is invalid
        GeometryCollection withSomeEmpty = geometryFactory.createGeometryCollection(new Geometry[] {
                geometryFactory.createPoint(new Coordinate(0, 0)),
                geometryFactory.createLineString(),
        });
        assertTrue(withSomeEmpty.isValid());
        assertTrue(withSomeEmpty.isSimple());

        GeometryCollection withInvalid = geometryFactory.createGeometryCollection(new Geometry[] {
                geometryFactory.createPoint(new Coordinate(Double.NaN, 0)),
        });
        assertTrue(!withInvalid.isValid());
        assertTrue(withInvalid.isSimple());

        GeometryCollection withNonSimple = geometryFactory.createGeometryCollection(new Geometry[] {
                geometryFactory.createLineString(packCoords(
                        0, 0, 1, 1, 1, 0, 0, 1, 0, 0
                ))
        });
        assertTrue(withNonSimple.isValid());
        assertTrue(!withNonSimple.isSimple());

        // XXX ISO: GeometryCollections with intersecting interiors are not simple
        GeometryCollection overlapping = geometryFactory.createGeometryCollection(new Geometry[] {
                geometryFactory.createLineString(packCoords(
                        0, 0, 2, 2
                )),
                geometryFactory.createPolygon(geometryFactory.createLinearRing(packCoords(
                        0, 0, 1, 0, 1, 1, 0, 1, 0, 0
                )))
        });
        assertTrue(overlapping.isValid());
        assertTrue(overlapping.isSimple());

        GeometryCollection touching = geometryFactory.createGeometryCollection(new Geometry[] {
                geometryFactory.createLineString(packCoords(
                        1, 1, 2, 2
                )),
                geometryFactory.createPolygon(geometryFactory.createLinearRing(packCoords(
                        0, 0, 1, 0, 1, 1, 0, 1, 0, 0
                )))
        });
        assertTrue(touching.isValid());
        assertTrue(touching.isSimple());
    }

    private CoordinateSequence packCoords(double... coordinates)
    {
        return packedCoordFactory.create(coordinates, 2);
    }
}
