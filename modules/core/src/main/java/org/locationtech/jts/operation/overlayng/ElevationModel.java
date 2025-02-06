/*
 * Copyright (c) 2019 Martin Davis.
 *
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the Eclipse Public License 2.0
 * and Eclipse Distribution License v. 1.0 which accompanies this distribution.
 * The Eclipse Public License is available at http://www.eclipse.org/legal/epl-v20.html
 * and the Eclipse Distribution License is available at
 *
 * http://www.eclipse.org/org/documents/edl-v10.php.
 */
package org.locationtech.jts.operation.overlayng;

import org.locationtech.jts.geom.Coordinate;
import org.locationtech.jts.geom.CoordinateSequence;
import org.locationtech.jts.geom.CoordinateSequenceFilter;
import org.locationtech.jts.geom.Envelope;
import org.locationtech.jts.geom.Geometry;
import org.locationtech.jts.geom.ZInterpolating;
import org.locationtech.jts.math.MathUtil;

/**
 * A simple elevation model used to populate missing Z values
 * in overlay results.
 * <p>
 * The model divides the extent of the input geometry(s)
 * into an NxM grid.
 * The default grid size is 3x3.  
 * If the input has no extent in the X or Y dimension,
 * that dimension is given grid size 1.
 * The elevation of each grid cell is computed as the average of the Z values
 * of the input vertices in that cell (if any). 
 * If a cell has no input vertices within it, it is assigned
 * the average elevation over all cells.
 * <p>
 * If no input vertices have Z values, the model does not assign a Z value.
 * <p>
 * The elevation of an arbitrary location is determined as the 
 * Z value of the nearest grid cell.
 * <p>
 * An elevation model can be used to populate missing Z values
 * in an overlay result geometry.
 *  
 * @author Martin Davis
 *
 */
class ElevationModel {
  
  private static final int DEFAULT_CELL_NUM = 3;

  /**
   * Creates an elevation model from two geometries (which may be null).
   * 
   * @param geom1 an input geometry 
   * @param geom2 an input geometry, or null
   * @return the elevation model computed from the geometries
   */
  public static ElevationModel create(Geometry geom1, Geometry geom2) {
    Envelope extent = geom1.getEnvelopeInternal().copy();
    if (geom2 != null) {
      extent.expandToInclude(geom2.getEnvelopeInternal());
    }
    ElevationModel model = new ElevationModel(extent, DEFAULT_CELL_NUM, DEFAULT_CELL_NUM);
    if (geom1 != null) model.add(geom1);
    if (geom2 != null) model.add(geom2);
    return model;
  }
  
  private Envelope extent;
  private int numCellX;
  private int numCellY;
  private double cellSizeX;
  private double cellSizeY;
  private ElevationCell[][] cells;
  private boolean isInitialized = false;
  private boolean hasZValue = false;
  private double averageZ = Double.NaN;

  /**
   * Creates a new elevation model covering an extent by a grid of given dimensions.
   * 
   * @param extent the XY extent to cover
   * @param numCellX the number of grid cells in the X dimension
   * @param numCellY the number of grid cells in the Y dimension
   */
  public ElevationModel(Envelope extent, int numCellX, int numCellY) {
    this.extent = extent;
    this.numCellX = numCellX;
    this.numCellY = numCellY;
    
    cellSizeX = extent.getWidth() / numCellX;
    cellSizeY = extent.getHeight() / numCellY;
    if(cellSizeX <= 0.0) {
      this.numCellX = 1;
    }
    if(cellSizeY <= 0.0) {
      this.numCellY = 1;
    }
    cells = new ElevationCell[numCellX][numCellY];
  }
  
  /**
   * Updates the model using the Z values of a given geometry.
   * 
   * @param geom the geometry to scan for Z values
   */
  public void add(Geometry geom) {
    geom.apply(new CoordinateSequenceFilter() {

      private boolean hasZ = true;

      @Override
      public void filter(CoordinateSequence seq, int i) {
        if (! seq.hasZ()) {
          hasZ = false;;
          return;
        }
        double z = seq.getOrdinate(i, Coordinate.Z);
        add(seq.getOrdinate(i, Coordinate.X),
            seq.getOrdinate(i, Coordinate.Y),
            z);
      }

      @Override
      public boolean isDone() {
        // no need to scan if no Z present
        return ! hasZ;
      }

      @Override
      public boolean isGeometryChanged() {
        return false;
      }
      
    });
  }
  
  protected void add(double x, double y, double z) {
    if (Double.isNaN(z))
      return;
    hasZValue = true;
    ElevationCell cell = getCell(x, y, true);
    cell.add(z);
  }
  
  private void init() {
    isInitialized = true;
    int numCells = 0;
    double sumZ = 0.0;
    
    for (int i = 0; i < cells.length; i++) {
      for (int j = 0; j < cells[0].length; j++) {
        ElevationCell cell = cells[i][j];
        if (cell != null) {
          cell.compute();
          numCells++;
          sumZ += cell.getZ();
        }
      }
    }
    averageZ = Double.NaN;
    if (numCells > 0) {
      averageZ = sumZ / numCells;
    }
  }
  
  /**
   * Gets the model Z value at a given location.
   * If the location lies outside the model grid extent,
   * this returns the Z value of the nearest grid cell.
   * If the model has no elevation computed (i.e. due 
   * to empty input), the value is returned as {@link Double#NaN}.
   * 
   * @param x the x ordinate of the location
   * @param y the y ordinate of the location
   * @return the computed model Z value
   */
  public double getZ(double x, double y) {
    if (! isInitialized) 
      init();
    ElevationCell cell = getCell(x, y, false);
    if (cell == null) 
      return averageZ;
    return cell.getZ();
  }
  
  /**
   * Computes Z values for any missing Z values in a geometry,
   * using the computed model.
   * If the model has no Z value, or the geometry coordinate dimension
   * does not include Z, the geometry is not updated.
   * 
   * @param geom the geometry to populate Z values for
   */
  public void populateZ(Geometry geom) {
    // short-circuit if ZInterpolation is globally turned off
    if (!ZInterpolating.getZInterpolating())
      return;

    // short-circuit if no Zs are present in model
    if (! hasZValue)
      return;
    
    if (! isInitialized) 
      init();
    
    geom.apply(new CoordinateSequenceFilter() {

      private boolean isDone = false;

      @Override
      public void filter(CoordinateSequence seq, int i) {
        if (!seq.hasZ()) {
          // if no Z then short-circuit evaluation
          isDone = true;
          return;
        }
        // if Z not populated then assign using model
        if (Double.isNaN( seq.getZ(i) )) {
          double z = getZ(seq.getOrdinate(i, Coordinate.X),
                          seq.getOrdinate(i, Coordinate.Y));
          seq.setOrdinate(i, Coordinate.Z, z);
        }
      }

      @Override
      public boolean isDone() {
        return isDone;
      }

      @Override
      public boolean isGeometryChanged() {
        // geometry extent is not changed
        return false;
      }
      
    });
  }
  
  private ElevationCell getCell(double x, double y, boolean isCreateIfMissing) {
    int ix = 0;
    if (numCellX > 1) {
      ix = (int) ((x - extent.getMinX()) / cellSizeX);
      ix = MathUtil.clamp(ix, 0, numCellX - 1);
    }
    int iy = 0;
    if (numCellY > 1) {
      iy = (int) ((y - extent.getMinY()) / cellSizeY);
      iy = MathUtil.clamp(iy, 0, numCellY - 1);
    }
    ElevationCell cell = cells[ix][iy];
    if (isCreateIfMissing && cell == null) {
      cell = new ElevationCell();
      cells[ix][iy] = cell;
    }
    return cell;
  }

  static class ElevationCell {

    private int numZ = 0;
    private double sumZ = 0.0;
    private double avgZ;

    public void add(double z) {
      numZ++;
      sumZ += z;
    }
    
    public void compute() {
      avgZ = Double.NaN;
      if (numZ > 0) 
        avgZ = sumZ / numZ;
    }
    
    public double getZ() {
      return avgZ;
    }
  }
}
