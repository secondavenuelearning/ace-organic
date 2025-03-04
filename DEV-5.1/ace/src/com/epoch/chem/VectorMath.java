package com.epoch.chem;

import chemaxon.formats.MolImporter;
import chemaxon.struc.DPoint3;
import chemaxon.struc.MolAtom;
import chemaxon.struc.Molecule;
import chemaxon.struc.graphics.MPolyline;
import chemaxon.struc.graphics.MRectangle;
import com.epoch.utils.MathMethods;
import com.epoch.utils.Utils;
import java.awt.Polygon;
import org.apache.commons.math.util.MathUtils;

/**
 * Methods for doing 3D geometry such as calculating dihedral angles or bond
 * angles.
 * The implementation is based on a fairly simple 3D vector representation
 * that provides methods required by dihedral calculation but nothing more.
 * <br>
 * This class has a main() method that takes 5 parameters, a molecule string
 * (e.g. smiles) and four atom indices (starting from 0).
 * If the input molecule has no 3d data (e.g. a SMILES is given) the 3D
 * coordinates are calculated. Hydrogens are always added.
 * <pre>
 * Usage example: java VectorMath "C1CCCCC1" 1 2 3 4
 * </pre>
 * The above example calculates the dihedral angle over the bond between atoms
 * 2 and 3 with respect to atoms 1 and 4.
 *
 * @author  Miklos Vargyas
 */
public class VectorMath {

	private static void debugPrint(Object... msg) {
		// Utils.printToLog(msg);
	}

	/** Value for calcDihedral(). */
	private static final String DEGREES = "degrees";

	/** The molecule object whose dihedrals can be calculated by this class.  */
	transient private final Molecule mol;

	/** Constructor.
	 * @param m a molecule whose dihedral angles can be calculated by
	 *		  subsequent calls of <code>calcDihedral</code>
	 */
	public VectorMath(Molecule m) {
		mol = m;
	} // VectorMath(Molecule)

	/** Calculates and returns the dihedral angle specified by the given atoms.
	 * The angle is measured
	 * around the bond <code>atoms[1]</code>-<code>atoms[2]</code> with respect to
	 * <code>atoms[0]</code> and <code>atoms[3]</code>. Typically, <code>atoms[0]</code>
	 * is a neighbor of <code>atoms[1]</code>, while <code>atoms[3]</code> is a
	 * neighbor of <code>atoms[2]</code>. <br>
	 * The angle calculated is the angle between planes 
	 * <i>atoms[0] atoms[s[0]] atoms[s[1]]</i>
	 * and <i>atoms[1] atoms[2] atoms[3]</i>.
	 * @param i indices of four contiguous atoms; dihedral will be calculated about 
	 * the bond of the middle two
	 * @param	degOrRadian	whether to return the value in degrees or
	 * radians
	 * @param	range	whether to return a value between 0 and 180 or 0 and 360
	 * @return the dihedral angle
	 */
	public double calcDihedral(int[] i, String degOrRadian, int range) {
		final MolAtom[] atoms = new MolAtom[]
				{mol.getAtom(i[0]),
				mol.getAtom(i[1]),
				mol.getAtom(i[2]),
				mol.getAtom(i[3])};
		return calcDihedral(atoms, degOrRadian, range);
	} // calcDihedral(int[], String, int)

	/** Calculates and returns the dihedral angle specified by the given atoms.
	 * The angle is measured
	 * around the bond <code>atoms[1]</code>-<code>atoms[2]</code> with respect to
	 * <code>atoms[0]</code> and <code>atoms[3]</code>. Typically, <code>atoms[0]</code>
	 * is a neighbor of <code>atoms[1]</code>, while <code>atoms[3]</code> is a
	 * neighbor of <code>atoms[2]</code>. <br>
	 * The angle calculated is the angle between planes 
	 * <i>atoms[0] atoms[s[0]] atoms[s[1]]</i>
	 * and <i>atoms[1] atoms[2] atoms[3]</i>.
	 * @param i indices of four contiguous atoms; dihedral will be calculated about 
	 * the bond of the middle two
	 * @param	degOrRadian	whether to return the value in degrees or
	 * radians
	 * @return the dihedral angle between 0 and 180
	 */
	public double calcDihedral(int[] i, String degOrRadian) {
		return calcDihedral(i, degOrRadian, 180);
	} // calcDihedral(int[], String)

	/** Calculates and returns the dihedral angle specified by the given atoms.
	 * The angle is measured
	 * around the bond <code>atoms[1]</code>-<code>atoms[2]</code> with respect to
	 * <code>atoms[0]</code> and <code>atoms[3]</code>. Typically, <code>atoms[0]</code>
	 * is a neighbor of <code>atoms[1]</code>, while <code>atoms[3]</code> is a
	 * neighbor of <code>atoms[2]</code>. <br>
	 * The angle calculated is the angle between planes 
	 * <i>atoms[0] atoms[s[0]] atoms[s[1]]</i>
	 * and <i>atoms[1] atoms[2] atoms[3]</i>.
	 * @param i indices of four contiguous atoms; dihedral will be calculated about 
	 * the bond of the middle two
	 * @param	range	whether to return a value between 0 and 180 or 0 and 360
	 * @return the dihedral angle in degrees
	 */
	public double calcDihedral(int[] i, int range) {
		return calcDihedral(i, DEGREES, range);
	} // calcDihedral(int[], int)

	/** Calculates and returns the dihedral angle specified by the given atoms.
	 * The angle is measured
	 * around the bond <code>atoms[1]</code>-<code>atoms[2]</code> with respect to
	 * <code>atoms[0]</code> and <code>atoms[3]</code>. Typically, <code>atoms[0]</code>
	 * is a neighbor of <code>atoms[1]</code>, while <code>atoms[3]</code> is a
	 * neighbor of <code>atoms[2]</code>. <br>
	 * The angle calculated is the angle between planes 
	 * <i>atoms[0] atoms[s[0]] atoms[s[1]]</i>
	 * and <i>atoms[1] atoms[2] atoms[3]</i>.
	 * @param i indices of four contiguous atoms; dihedral will be calculated about 
	 * the bond of the middle two
	 * @return the dihedral angle in degrees between 0 and 180
	 */
	public double calcDihedral(int[] i) {
		return calcDihedral(i, DEGREES, 180);
	} // calcDihedral(int[])

/* **************** Static methods *******************/

	/** Calculates and returns the dihedral angle specified by the given atoms.
	 * The angle is measured
	 * around the bond <code>atoms[1]</code>-<code>atoms[2]</code> with respect to
	 * <code>atoms[0]</code> and <code>atoms[3]</code>. Typically, <code>atoms[0]</code>
	 * is a neighbor of <code>atoms[1]</code>, while <code>atoms[3]</code> is a
	 * neighbor of <code>atoms[2]</code>. <br>
	 * The angle calculated is the angle between planes 
	 * <i>atoms[0] atoms[s[0]] atoms[s[1]]</i>
	 * and <i>atoms[1] atoms[2] atoms[3]</i>.
	 * @param atoms four contiguous atoms; dihedral will be calculated about 
	 * the bond of the middle two
	 * @param	degOrRadian	whether to return the value in degrees or
	 * radians
	 * @param	range	whether to return a value between 0 and 180 or 0 and 360
	 * @return the dihedral angle
	 */
	public static double calcDihedral(MolAtom[] atoms, String degOrRadian, int range) {
		final DPoint3 p1 = atoms[0].getLocation();
		final DPoint3 p2 = atoms[1].getLocation();
		final DPoint3 p3 = atoms[2].getLocation();
		final DPoint3 p4 = atoms[3].getLocation();
		// p1-p2-p3-p4 and we need dihedral around the p2-p3 bond
		// this is the angle between two planes: (p1 p2 p3) and (p2 p3 p4)
		final DPoint3 v1 = diff(p1, p2); // points from p2 to p1
		final DPoint3 v2 = diff(p3, p2); // points from p2 to p3
		final DPoint3 n1 = crossProd(v1, v2);
			// perpendicular to (p1 p2 p3) plane
		final DPoint3 v3 = diff(p2, p3); // points from p3 to p2
		final DPoint3 v4 = diff(p4, p3); // points from p3 to p4
		final DPoint3 n2 = crossProd(v3, v4);
			// perpendicular to (p2 p3 p4) plane
		// angle between two vectors perpendicular to (p1 p2 p3) and (p2 p3 p4)
		// planes, respectively.  angle between the two vectors is equal to the
		// angle between the two planes
		double da = angle(n1, n2);
		// correction of NaN bug for coplanar planes
		if ((Double.valueOf(da)).isNaN()) da = Math.PI; 
		if (range == 360 && dotProd(n1, v4) < 0) da = -da; // direction of angle
		return (DEGREES.equals(degOrRadian) ? toDegrees(da) : da); // radians or degrees
	} // calcDihedral(MolAtom[], String, int)

	/** Calculates and returns the dihedral angle specified by the given atoms.
	 * The angle is measured
	 * around the bond <code>atoms[1]</code>-<code>atoms[2]</code> with respect to
	 * <code>atoms[0]</code> and <code>atoms[3]</code>. Typically, <code>atoms[0]</code>
	 * is a neighbor of <code>atoms[1]</code>, while <code>atoms[3]</code> is a
	 * neighbor of <code>atoms[2]</code>. <br>
	 * The angle calculated is the angle between planes 
	 * <i>atoms[0] atoms[s[0]] atoms[s[1]]</i>
	 * and <i>atoms[1] atoms[2] atoms[3]</i>.
	 * @param atoms four contiguous atoms; dihedral will be calculated about 
	 * the bond of the middle two
	 * @param	degOrRadian	whether to return the value in degrees or
	 * radians
	 * @return the dihedral angle between 0 and 180
	 */
	public static double calcDihedral(MolAtom[] atoms, String degOrRadian) {
		return calcDihedral(atoms, degOrRadian, 180);
	} // calcDihedral(MolAtom[], String)

	/** Calculates and returns the dihedral angle specified by the given atoms.
	 * The angle is measured
	 * around the bond <code>atoms[1]</code>-<code>atoms[2]</code> with respect to
	 * <code>atoms[0]</code> and <code>atoms[3]</code>. Typically, <code>atoms[0]</code>
	 * is a neighbor of <code>atoms[1]</code>, while <code>atoms[3]</code> is a
	 * neighbor of <code>atoms[2]</code>. <br>
	 * The angle calculated is the angle between planes 
	 * <i>atoms[0] atoms[s[0]] atoms[s[1]]</i>
	 * and <i>atoms[1] atoms[2] atoms[3]</i>.
	 * @param atoms four contiguous atoms; dihedral will be calculated about 
	 * the bond of the middle two
	 * @param	range	whether to return a value between 0 and 180 or 0 and 360
	 * @return the dihedral angle in degrees
	 */
	public static double calcDihedral(MolAtom[] atoms, int range) {
		return calcDihedral(atoms, DEGREES, range);
	} // calcDihedral(MolAtom[], int)

	/** Calculates and returns the dihedral angle specified by the given atoms.
	 * The angle is measured
	 * around the bond <code>atoms[1]</code>-<code>atoms[2]</code> with respect to
	 * <code>atoms[0]</code> and <code>atoms[3]</code>. Typically, <code>atoms[0]</code>
	 * is a neighbor of <code>atoms[1]</code>, while <code>atoms[3]</code> is a
	 * neighbor of <code>atoms[2]</code>. <br>
	 * The angle calculated is the angle between planes 
	 * <i>atoms[0] atoms[s[0]] atoms[s[1]]</i>
	 * and <i>atoms[1] atoms[2] atoms[3]</i>.
	 * @param atoms four contiguous atoms; dihedral will be calculated about 
	 * the bond of the middle two
	 * @return the dihedral angle in degrees between 0 and 180
	 */
	public static double calcDihedral(MolAtom[] atoms) {
		return calcDihedral(atoms, DEGREES, 180);
	} // calcDihedral(MolAtom[])

	/** Gets the cross-product of two vectors originating at a center.
	 * @param	vector1	first vector
	 * @param	vector2	second vector
	 * @return	the cross-product vector
	 */
	public static DPoint3 crossProd(DPoint3 vector1, DPoint3 vector2) {
		return new DPoint3(
				vector1.y * vector2.z - vector1.z * vector2.y,
				-vector1.x * vector2.z + vector1.z * vector2.x,
				vector1.x * vector2.y - vector1.y * vector2.x);
	} // crossProd(DPoint3, DPoint3)

	/** Gets the dot product of two vectors originating at a center.
	 * @param	vector1	first vector
	 * @param	vector2	second vector
	 * @return	the dot product
	 */
	public static double dotProd(DPoint3 vector1, DPoint3 vector2) {
		return vector1.x * vector2.x
				+ vector1.y * vector2.y
				+ vector1.z * vector2.z;
	} // dotProd(DPoint3, DPoint3)

	/** Gets the scalar product of a vector originating from a center,
	 * and a scalar.
	 * @param	vector	a vector
	 * @param	scalar	a scalar
	 * @return	the product vector
	 */
	public static DPoint3 scalarProd(DPoint3 vector, double scalar) {
		return new DPoint3(
				vector.x * scalar,
				vector.y * scalar,
				vector.z * scalar);
	} // scalarProd(DPoint3, double)

	/** Gets the scalar quotient of a vector originating from a center,
	 * and a scalar.
	 * @param	vector	a vector
	 * @param	scalar	a scalar
	 * @return	the quotient vector
	 */
	public static DPoint3 scalarQuot(DPoint3 vector, double scalar) {
		return new DPoint3(
				vector.x / scalar,
				vector.y / scalar,
				vector.z / scalar);
	} // scalarQuot(DPoint3, double)

	/** Gets the difference of two vectors (first minus second),
	 * or the vector pointing from the second point to the first point.
	 * @param	vector1	first vector/point
	 * @param	vector2	second vector/point
	 * @return	the difference vector
	 */
	public static DPoint3 diff(DPoint3 vector1, DPoint3 vector2) {
		return new DPoint3(
				vector1.x - vector2.x,
				vector1.y - vector2.y,
				vector1.z - vector2.z);
	} // diff(DPoint3, DPoint3)

	/** Gets a vector from one atom to another.
	 * @param	origin	the origin of the vector
	 * @param	dest	the point of the vector
	 * @return	the vector
	 */
	public static DPoint3 getVector(MolAtom origin, MolAtom dest) {
		return diff(dest.getLocation(), origin.getLocation());
	} // getVector(MolAtom, MolAtom)

	/** Gets the sum of two vectors originating at a center.
	 * @param	vector1	first vector
	 * @param	vector2	second vector
	 * @return	the sum vector
	 */
	public static DPoint3 sum(DPoint3 vector1, DPoint3 vector2) {
		return new DPoint3(
				vector1.x + vector2.x,
				vector1.y + vector2.y,
				vector1.z + vector2.z);
	} // sum(DPoint3, DPoint3)

	/** Gets the midpoint of two points.
	 * @param	pt1	first point
	 * @param	pt2	second point
	 * @return	the midpoint
	 */
	public static DPoint3 midpoint(DPoint3 pt1, DPoint3 pt2) {
		return new DPoint3(
				(pt1.x + pt2.x) / 2,
				(pt1.y + pt2.y) / 2,
				(pt1.z + pt2.z) / 2);
	} // midpoint(DPoint3, DPoint3) 

	/** Gets the length of a vector originating at a center.
	 * @param	vector1	a vector
	 * @return	the length
	 */
	public static double length(DPoint3 vector1) {
		return Math.sqrt(vector1.x * vector1.x
				+ vector1.y * vector1.y
				+ vector1.z * vector1.z);
	} // length(DPoint3)

	/** Gets the positive or negative angle in radians about the middle 
	 * point formed by three atoms in the xy plane; returns 0 if two of the
	 * atoms have the same location.
	 * @param	atoms	array of three atoms in the xy plane
	 * @return	the angle between -&pi; and &pi;
	 */
	public static double angle(MolAtom[] atoms) {
		final DPoint3[] atomPts = new DPoint3[] 
				{atoms[0].getLocation(), 
				atoms[1].getLocation(), 
				atoms[2].getLocation()};
		final DPoint3 vec21 = getVector(atoms[1], atoms[0]);
		final DPoint3 vec23 = getVector(atoms[1], atoms[2]);
		return angle(vec21, vec23) * angleSign(atomPts);
	} // angle(MolAtom[])

	/** Gets the positive angle made by three points.
	 * @param	pt1	first point
	 * @param	pt2	second point
	 * @param	pt3	third point
	 * @return	the angle between 0 and &pi;
	 */
	public static double angle(DPoint3 pt1, DPoint3 pt2, DPoint3 pt3) {
		return angle(diff(pt1, pt2), diff(pt3, pt2));
	} // angle(DPoint3, DPoint3, DPoint3)

	/** Gets the positive angle between two vectors originating at a center, or
	 * 0 if one of the vectors has a length of 0.
	 * @param	vector1	first vector
	 * @param	vector2	second vector
	 * @return	the angle between 0 and &pi;
	 */
	public static double angle(DPoint3 vector1, DPoint3 vector2) {
		double angle = Math.acos(dotProd(vector1, vector2) /
				(length(vector1) * length(vector2)));
		if (Double.isNaN(angle)) angle = 0;
		return angle;
	} // angle(DPoint3, DPoint3)

	/** Gets the sign of the angle about the middle point formed by three 
	 * points in the xy plane.
	 * @param	pts	array of three points in the xy plane
	 * @return	the sign of the angle
	 */
	public static double angleSign(DPoint3[] pts) {
		return MathUtils.sign(
				(pts[0].x - pts[1].x) * (pts[2].y - pts[1].y)
				- (pts[0].y - pts[1].y) * (pts[2].x - pts[1].x));
	} // angleSign(DPoint3[]) 

	/** Gets the sign of the angle between two vectors originating at a center.
	 * @param	vector1	first vector
	 * @param	vector2	second vector
	 * @return	the sign of the angle
	 */
	public static double angleSign(DPoint3 vector1, DPoint3 vector2) {
		return angleSign(new DPoint3[] {vector1, new DPoint3(), vector2});
	} // angleSign(DPoint3, DPoint3) 

	/** Generates a projection of vector1 on a plane perpendicular to
	 * vector2.  Both vectors originate at a center.  Formula from
	 * http://www.euclideanspace.com/maths/geometry/elements/plane/lineOnPlane/index.htm
	 * @param	vector1	vector to be projected
	 * @param	vector2	plane-defining vector
	 * @return	the projection
	 */
	public static DPoint3 proj1onPlaneNormalTo2(DPoint3 vector1,
			DPoint3 vector2) {
		final double length2 = length(vector2);
		final DPoint3 unit2 = scalarQuot(vector2, length2);
		return scalarQuot(crossProd(vector2,
				crossProd(vector1, unit2)), length2);
	} // proj1onPlaneNormalTo2(DPoint3, DPoint3)

	/** Determines if a point is in a box.  Algorithm: true if angles between
	 * vectors to adjacent corners add up to 360 degrees.
	 * @param	p	a point
	 * @param	box	a rectangle defined by four points
	 * @return	true if the point is in the box defined by the four points
	 */
	public static boolean pointInRect(DPoint3 p, MRectangle box) {
		return pointInRect(p, box, 1);
	} // pointInRect(DPoint3, MRectangle)

	/** Determines if a point is in a box.  Algorithm: true if angles between
	 * vectors to adjacent corners add up to 360 degrees.
	 * @param	p	a point
	 * @param	box	a rectangle defined by four points
	 * @param	scale	how much to change the size of the rectangle
	 * @return	true if the point is in the box defined by the four points
	 */
	public static boolean pointInRect(DPoint3 p, MRectangle box, double scale) {
		DPoint3 pt1 = box.getPointRef(0, null).getLocation();
		DPoint3 pt2 = box.getPointRef(1, null).getLocation();
		DPoint3 pt3 = box.getPointRef(2, null).getLocation();
		DPoint3 pt4 = box.getPointRef(3, null).getLocation();
		pt1 = new DPoint3(pt1.x, pt1.y, 0);
		pt2 = new DPoint3(pt2.x, pt2.y, 0);
		pt3 = new DPoint3(pt3.x, pt3.y, 0);
		pt4 = new DPoint3(pt4.x, pt4.y, 0);
		if (scale != 1) {
			final DPoint3 center = new DPoint3();
			box.calcCenter(center, null);
			final DPoint3 ctrTo1 = diff(pt1, center);
			final DPoint3 ctrTo2 = diff(pt2, center);
			final DPoint3 ctrTo3 = diff(pt3, center);
			final DPoint3 ctrTo4 = diff(pt4, center);
			pt1 = sum(center, scalarProd(ctrTo1, scale));
			pt2 = sum(center, scalarProd(ctrTo2, scale));
			pt3 = sum(center, scalarProd(ctrTo3, scale));
			pt4 = sum(center, scalarProd(ctrTo4, scale));
		} // if want to change size of box
		final DPoint3 pt = new DPoint3(p.x, p.y, 0); // project onto XY plane
		final DPoint3 vecTo1 = diff(pt1, pt);
		final DPoint3 vecTo2 = diff(pt2, pt);
		final DPoint3 vecTo3 = diff(pt3, pt);
		final DPoint3 vecTo4 = diff(pt4, pt);
		final int sumAngles = MathMethods.roundToInt(toDegrees(Math.abs(
				angle(vecTo1, vecTo2) + angle(vecTo2, vecTo3)
					+ angle(vecTo3, vecTo4) + angle(vecTo4, vecTo1))));
		debugPrint("VectorMath.pointInRect: sumAngles = ", sumAngles);
		return MathMethods.inRange(sumAngles, new int[] {355, 365});
	} // pointInRect(DPoint3, MRectangle, double)

	/** Determines if a point is on a line.  Algorithm: true if angles between
	 * vectors to line endpoints add up to 180 degrees.
	 * @param	p	a point
	 * @param	line	a line
	 * @return	true if the point is on the line
	 */
	public static boolean pointOnLine(DPoint3 p, MPolyline line) {
		return pointOnLine(p, line, false);
	} // pointOnLine(DPoint3, MPolyline)

	/** Determines if a point is on a line segment.  Algorithm: true if angles 
	 * between vectors to segment endpoints add up to 180 degrees.
	 * @param	p	a point
	 * @param	segment	a line segment
	 * @param	debugPrint	whether to debugPrint
	 * @return	true if the point is on the line segment
	 */
	public static boolean pointOnLine(DPoint3 p, MPolyline segment, 
			boolean debugPrint) {
		final String SELF = "VectorMath.pointOnLine: ";
		if (debugPrint) Utils.alwaysPrint(SELF + "pt = ", p);
		final DPoint3 pt1 = segment.getPoint(0).getLocation();
		if (debugPrint) Utils.alwaysPrint(SELF + "segment endPt1 locn = ", pt1);
		final DPoint3 pt2 = segment.getPoint(1).getLocation();
		if (debugPrint) Utils.alwaysPrint(SELF + "segment endPt2 locn = ", pt2);
		final DPoint3 pt = new DPoint3(p.x, p.y, 0); // project onto XY plane
		final DPoint3 vecTo1 = diff(new DPoint3(pt1.x, pt1.y, 0), pt);
		if (debugPrint) Utils.alwaysPrint(SELF 
				+ "2D vector from pt to endPt1 = ", vecTo1);
		final DPoint3 vecTo2 = diff(new DPoint3(pt2.x, pt2.y, 0), pt);
		if (debugPrint) Utils.alwaysPrint(SELF 
				+ "2D vector from pt to endPt2 = ", vecTo2);
		final double angleRad = angle(vecTo1, vecTo2); // 0 to pi
		if (debugPrint) Utils.alwaysPrint(SELF 
				+ "endPt1_pt_endPt2 angle in radians = ", angleRad);
		final int angleDeg = MathMethods.roundToInt(toDegrees(angleRad));
		if (debugPrint) Utils.alwaysPrint(SELF 
				+ "rounded angle in degrees = ", angleDeg, 
				" (should be >= 170 to be found on line segment)");
		final boolean inRange = MathMethods.inRange(angleDeg, new int[] {170, 180});
		if (debugPrint) Utils.alwaysPrint(SELF + "inRange = ", inRange);
		return inRange;
	} // pointOnLine(DPoint3, MPolyline, boolean)

	/** Determines if a point is in a polygon. 
	 * @param	vertices	points defining the polygon
	 * @param	point	point to see if it is in the polygon
	 * @return	true if the point is in the polygon
	 */
	public static boolean pointInPolygon(DPoint3[] vertices, DPoint3 point) {
		final int numVertices = vertices.length;
		final int[] xCoords = new int[numVertices];
		final int[] yCoords = new int[numVertices];
		int vertexNum = 0;
		for (final DPoint3 vertex : vertices) {
			xCoords[vertexNum] = MathMethods.roundToInt(vertex.x * 1000.0);
			yCoords[vertexNum] = MathMethods.roundToInt(vertex.y * 1000.0);
			vertexNum++;
		} // for each vertex
		final Polygon shape = new Polygon(xCoords, yCoords, numVertices);
		return shape.contains(point.x * 1000.0, point.y * 1000.0);
	} // pointInPolygon(DPoint3[], DPoint3)

	/** Rotates a 2D vector in the plane.
	 * @param	vector	the vector
	 * @param	angle	the angle of rotation
	 * @return	the new location of the moving point
	 */
	public static DPoint3 rotateVector(DPoint3 vector, double angle) {
		final double newX = vector.x * Math.cos(angle)
				- vector.y * Math.sin(angle);
		final double newY = vector.x * Math.sin(angle)
				+ vector.y * Math.cos(angle);
		return new DPoint3(newX, newY, 0);
	} // rotateVector(DPoint3, double)

	/** Converts radians to degrees. 
	 * @param	radians	an angle in radians
	 * @return	the angle in degrees
	 */
	public static double toDegrees(double radians) {
		return radians * 180 / Math.PI;
	} // toDegrees(double)

	/** Converts degrees to radians. 
	 * @param	degrees	an angle in degrees
	 * @return	the angle in radians
	 */
	public static double toRadians(double degrees) {
		return degrees * Math.PI / 180;
	} // toRadians(double)

	/** Converts degrees to radians. 
	 * @param	degrees	an angle in degrees
	 * @return	the angle in radians
	 */
	public static double toRadians(int degrees) {
		return toRadians((double) degrees);
	} // toRadians(int)

	/** Allows to test the <code>VectorMath</code> class. Takes five parameters,
	* a molecule string and 4 atom indices and output the dihedral angle
	* specified by these four atoms.
	* @param	args	a molecule string and 4 atom indices
	*/
	public static void main(String[] args) {
		try {
			// read and build up the molecule given as string in command-line
			final Molecule m = MolImporter.importMol(args[0]);
			ChemUtils.explicitizeH(m);  // add explicit hydrogens to input molecule
			// if input structure is not a 3d molecule, assign 3d coordinates to all atoms
			StereoFunctions.convertTo3D(m);
			// Create a dihedral calculator object
			final VectorMath dh = new VectorMath(m);
			// calculate the dihedral angle specified atom indices given
			// in the command line
			final double a = dh.calcDihedral(new int[] 
					{Integer.parseInt(args[1]),
					Integer.parseInt(args[2]), 
					Integer.parseInt(args[3]),
					Integer.parseInt(args[4])});
			// output dihedral in degrees
			debugPrint("VectorMath.main: angle = ", a);
		} catch (Exception e) {
			e.printStackTrace();
		}
	} // main

} // VectorMath

