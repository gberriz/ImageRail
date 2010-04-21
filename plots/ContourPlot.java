/**
 * ContourPlot.java
 *
 * @author Created by Omnicore CodeGuide
 */

package plots;


import java.awt.*;

import java.util.ArrayList;
import javax.swing.JPanel;

//----------------------------------------------------------
// "ContourPlot" is the most important class. It is a
// user-interface component which parses the data, draws
// the contour plot, and returns a string of results.
//----------------------------------------------------------
public class ContourPlot extends JPanel
{
	final static int NUMBER_COMPONENTS	= 6;
	final static int MIN_X_STEPS =   2,
		MIN_Y_STEPS =   2,
		MAX_X_STEPS = 100,
		MAX_Y_STEPS = 100;
	private float[] color = new float[4];
	final static String EOL	= System.getProperty("line.separator");
	ArrayList arr = new ArrayList();
//	public int WIDTH;
//	public int HEIGHT;
//	public int XSTART;
//	public int YSTART;
	
	// Below, the six user-interface components:
	static 	ContourPlot thePlot;
	
	// Below, constant data members:
	final static boolean	SHOW_NUMBERS	= true;
	final static int
		N_CONTOURS	= 10,
		PLOT_MARGIN	= 20,
		WEE_BIT		=  3;
	final static double	Z_MAX_MAX	= 1.0E+10,
		Z_MIN_MIN	= -Z_MAX_MAX;
	
	
	// Below, data members which store the grid steps,
	// the z values, the interpolation flag, the dimensions
	// of the contour plot and the increments in the grid:
	int		xSteps, ySteps;
	float[][]	densityValues;
	boolean		logInterpolation = false;
//	Dimension	d;
	double		deltaX, deltaY;
	
	// Below, data members, most of which are adapted from
	// Fortran variables in Snyder's code:
	int	ncv = N_CONTOURS;
	int	l1[] = new int[4];
	int	l2[] = new int[4];
	int	ij[] = new int[2];
	int	i1[] = new int[2];
	int	i2[] = new int[2];
	int	i3[] = new int[6];
	int	ibkey,icur,jcur,ii,jj,elle,ix,iedge,iflag,ni,ks;
	int	cntrIndex,prevIndex;
	int	idir,nxidir,k;
	double	z1,z2,cval,zMax,zMin;
	double	intersect[]	= new double[4];
	double	xy[]		= new double[2];
	double	prevXY[]	= new double[2];
	float	cv[]		= new float[ncv];
	boolean	jump;
	
	//-------------------------------------------------------
	// A constructor method.
	//-------------------------------------------------------
//	public ContourPlot(int x, int y)
	public ContourPlot(float[][] densityValues_)
	{
		thePlot = this;
		densityValues = densityValues_;
	}
	
	
	public void DrawTheContourPlot(Graphics2D g2, int xstart, int ystart, int width, int height)
	{
		MakeMatrixRectangular();
		GetExtremes();
		if (zMax > Z_MAX_MAX) zMax = Z_MAX_MAX;
		if (zMin < Z_MIN_MIN) zMin = Z_MIN_MIN;
		AssignContourValues();
//		thePlot.paint(thePlot.getGraphics());
		draw(g2, xstart, ystart, width, height);
	}
	
	//-------------------------------------------------------
	int sign(int a, int b)
	{
		a = Math.abs(a);
		if (b < 0)	return -a;
		else		return  a;
	}
	
	//-------------------------------------------------------
	// "InvalidData" sets the first two components of the
	// contour value array to equal values, thus preventing
	// subsequent drawing of the contour plot.
	//-------------------------------------------------------
	void InvalidData()
	{
		cv[0] = (float)0.0;
		cv[1] = (float)0.0;
	}
	
	//-------------------------------------------------------
	// "GetExtremes" scans the data in "z" in order
	// to assign values to "zMin" and "zMax".
	//-------------------------------------------------------
	void GetExtremes()
	{
		int	i,j;
		double	here;
		
		zMin = densityValues[0][0];
		zMax = zMin;
		for (i = 0; i < xSteps; i++)
		{
			for (j = 0; j < ySteps; j++)
			{
				here = densityValues[i][j];
				if (zMin > here) zMin = here;
				if (zMax < here) zMax = here;
			}
		}
		if (zMin == zMax)
		{
			InvalidData();
		}
		return;
	}
	
	//-------------------------------------------------------
	// "AssignContourValues" interpolates between "zMin" and
	// "zMax", either logarithmically or linearly, in order
	// to assign contour values to the array "cv".
	//-------------------------------------------------------
	void AssignContourValues()
	{
		int	i;
		double	delta;
		
		if ((logInterpolation) && (zMin <= 0.0))
		{
			InvalidData();
		}
		if (logInterpolation)
		{
			double	temp = tools.MathOps.log(zMin);
			
			delta = (tools.MathOps.log(zMax)-temp) / ncv;
			for (i = 0; i < ncv; i++) cv[i] = (float)tools.MathOps.exp(temp + (i+1)*delta);
		}
		else
		{
			delta = (zMax-zMin) / ncv;
			for (i = 0; i < ncv; i++) cv[i] = (float)(zMin + (i+1)*delta);
		}
	}
	
	//-------------------------------------------------------
	// "GetContourValuesString" returns a list of the
	// contour values for display in the results area.
	//-------------------------------------------------------
	String GetContourValuesString()
	{
		String	s = new String();
		int	i;
		
		for (i = 0; i < ncv; i++) s = s + "[" + Integer.toString(i) + "] " +
				Float.toString(cv[i]) + EOL;
		return s;
	}
	
	//-------------------------------------------------------
	// "SetMeasurements" determines the dimensions of
	// the contour plot and the increments in the grid.
	//-------------------------------------------------------
	void SetMeasurements(int height, int width)
	{
//		d = size();
//		d.width  = d.height  - 2*PLOT_MARGIN;
//		d.height = d.width - 2*PLOT_MARGIN;
		deltaX = width  / (xSteps - 1.0);
		deltaY = height / (ySteps - 1.0);
	}
	
	//-------------------------------------------------------
	// "DrawGrid" draws the rectangular grid of gray lines
	// on top of which the contours will later be drawn.
	//-------------------------------------------------------
	void DrawGrid(Graphics2D g, int xstart, int ystart, int width, int height)
	{
		int i,j,kx,ky;
		
		// Interchange horizontal & vertical
//		g.clearRect(0, 0, d.height+2*PLOT_MARGIN, d.width +2*PLOT_MARGIN);
		g.setColor(Color.lightGray);
		for (i = 0; i < xSteps; i++)
		{
			kx = (int)((float)i * deltaX);
			int x1 = xstart;
			int x2 = xstart+width;
			int y1 = ystart-kx;
			int y2 = ystart-kx;
			g.drawLine(x1, y1, x2, y2);
		}
		for (j = 0; j < ySteps; j++)
		{
			ky = (int)((float)j * deltaY);
			int x1 = xstart+ky;
			int x2 = xstart+ky;
			int y1 = ystart;
			int y2 = ystart-height;
			g.drawLine(x1, y1, x2, y2);
		}
		g.setColor(Color.black);
	}
	
	
	
	//-------------------------------------------------------
	// "SetColour" sets the colour of the graphics object,
	// given the contour index, by interpolating linearly
	// between "Color.blue" & "Color.red".
	//-------------------------------------------------------
	private void SetColour(Graphics2D g)
	{
		float val = cntrIndex+1;
		float min = 0;
		float max = ncv;
		
		tools.ColorMaps.getColorValue(val, min, max, color, gui.MainGUI.getGUI().getTheColorMapIndex());
		Color c = new Color(color[0], color[1], color[2], color[3]);
		
//										  Color c = new Color(
//																 ((ncv-cntrIndex) * Color.blue.getRed()   +
//																	  cntrIndex * Color.red.getRed())/ncv,
//
//																 ((ncv-cntrIndex) * Color.blue.getGreen() +
//																	  cntrIndex * Color.red.getGreen())/ncv,
//
//																 ((ncv-cntrIndex) * Color.blue.getBlue()  +
//																	  cntrIndex * Color.red.getBlue())/ncv);
		
		g.setColor(c);
	}
	
	public void SetColour(Graphics g, int contourLevel)
	{
		float val = contourLevel+1;
		float min = 0;
		float max = ncv;
		
		tools.ColorMaps.getColorValue(val, min, max, color, gui.MainGUI.getGUI().getTheColorMapIndex());
		Color c = new Color(color[0], color[1], color[2], color[3]);
		
		g.setColor(c);
	}
	
	//-------------------------------------------------------
	// "DrawKernel" is the guts of drawing and is called
	// directly or indirectly by "ContourPlotKernel" in order
	// to draw a segment of a contour or to set the pen
	// position "prevXY". Its action depends on "iflag":
	//
	// iflag == 1 means Continue a contour
	// iflag == 2 means Start a contour at a boundary
	// iflag == 3 means Start a contour not at a boundary
	// iflag == 4 means Finish contour at a boundary
	// iflag == 5 means Finish closed contour not at boundary
	// iflag == 6 means Set pen position
	//
	// If the constant "SHOW_NUMBERS" is true then when
	// completing a contour ("iflag" == 4 or 5) the contour
	// index is drawn adjacent to where the contour ends.
	//-------------------------------------------------------
	void DrawKernel(Graphics2D g2, int xstart, int ystart, int width, int height)
	{
		int	prevU,prevV,u,v;
//		g.setStroke(main.MainGUI.Stroke_2);
		if ((iflag == 1) || (iflag == 4) || (iflag == 5))
		{
			if (cntrIndex != prevIndex) { // Must change colour
				SetColour(g2);
				prevIndex = cntrIndex;
			}
			prevU = (int)((prevXY[0] - 1.0) * deltaX);
			prevV = (int)((prevXY[1] - 1.0) * deltaY);
			u = (int)((xy[0] - 1.0) * deltaX);
			v = (int)((xy[1] - 1.0) * deltaY);
			
			// Interchange horizontal & vertical
			// Interchange horizontal & vertical
			int x1 = xstart+prevV;
			int x2 = xstart+v;
			int y1 = ystart-prevU;
			int y2 = ystart-u;
			
			arr.add(new Point(x1, y1));
			
//			int x1 = PLOT_MARGIN+prevV;
//			int x2 = PLOT_MARGIN+v;
//			int y1 = PLOT_MARGIN+prevU;
//			int y2 = PLOT_MARGIN+u;
			
			//Draw the line
//			g.drawLine(x1,y1,x2,y2);
			
			if (iflag==5 || iflag==4)
			{
				int num = arr.size();
				Polygon poly = new Polygon();
				for (int i = 0; i < num; i++)
				{
					Point p = (Point)arr.get(i);
					poly.addPoint(p.x, p.y);
				}
				g2.fillPolygon(poly);//(poly);
				arr = new ArrayList();
			}
			
			//Draw the Label
//			if ((SHOW_NUMBERS) && ((iflag==4) || (iflag==5)))
//			{
//				if      (u == 0)	u = u - WEE_BIT;
//				else if	(u == width)  u = u + PLOT_MARGIN/2;
//				else if	(v == 0)	v = v - PLOT_MARGIN/2;
//				else if	(v == height) v = v + WEE_BIT;
//				g.setFont(main.MainGUI.LargeFont);
//				Color c = g.getColor();
//				g.setColor(Color.black);
//				g.drawString(Integer.toString(cntrIndex), x2, y2);
//				g.setColor(c);
//			}
		}
//		g.setStroke(main.MainGUI.Stroke_1);
		prevXY[0] = xy[0];
		prevXY[1] = xy[1];
	}
	
	//-------------------------------------------------------
	// "DetectBoundary"
	//-------------------------------------------------------
	void DetectBoundary()
	{
		ix = 1;
		if (ij[1-elle] != 1)
		{
			ii = ij[0] - i1[1-elle];
			jj = ij[1] - i1[elle];
			if (densityValues[ii-1][jj-1] <= Z_MAX_MAX)
			{
				ii = ij[0] + i2[elle];
				jj = ij[1] + i2[1-elle];
				if (densityValues[ii-1][jj-1] < Z_MAX_MAX) ix = 0;
			}
			if (ij[1-elle] >= l1[1-elle])
			{
				ix = ix + 2;
				return;
			}
		}
		ii = ij[0] + i1[1-elle];
		jj = ij[1] + i1[elle];
		if (densityValues[ii-1][jj-1] > Z_MAX_MAX)
		{
			ix = ix + 2;
			return;
		}
		if (densityValues[ij[0]][ij[1]] >= Z_MAX_MAX) ix = ix + 2;
	}
	
	//-------------------------------------------------------
	// "Routine_label_020" corresponds to a block of code
	// starting at label 20 in Synder's subroutine "GCONTR".
	//-------------------------------------------------------
	boolean Routine_label_020()
	{
		l2[0] =  ij[0];
		l2[1] =  ij[1];
		l2[2] = -ij[0];
		l2[3] = -ij[1];
		idir = 0;
		nxidir = 1;
		k = 1;
		ij[0] = Math.abs(ij[0]);
		ij[1] = Math.abs(ij[1]);
		if (densityValues[ij[0]-1][ij[1]-1] > Z_MAX_MAX)
		{
			elle = idir % 2;
			ij[elle] = sign(ij[elle],l1[k-1]);
			return true;
		}
		elle = 0;
		return false;
	}
	
	//-------------------------------------------------------
	// "Routine_label_050" corresponds to a block of code
	// starting at label 50 in Synder's subroutine "GCONTR".
	//-------------------------------------------------------
	boolean Routine_label_050()
	{
		while (true)
		{
			if (ij[elle] >= l1[elle])
			{
				if (++elle <= 1) continue;
				elle = idir % 2;
				ij[elle] = sign(ij[elle],l1[k-1]);
				if (Routine_label_150()) return true;
				continue;
			}
			ii = ij[0] + i1[elle];
			jj = ij[1] + i1[1-elle];
			if (densityValues[ii-1][jj-1] > Z_MAX_MAX)
			{
				if (++elle <= 1) continue;
				elle = idir % 2;
				ij[elle] = sign(ij[elle],l1[k-1]);
				if (Routine_label_150()) return true;
				continue;
			}
			break;
		}
		jump = false;
		return false;
	}
	
	//-------------------------------------------------------
	// "Routine_label_150" corresponds to a block of code
	// starting at label 150 in Synder's subroutine "GCONTR".
	//-------------------------------------------------------
	boolean Routine_label_150()
	{
		while (true)
		{
			//------------------------------------------------
			// Lines from z[ij[0]-1][ij[1]-1]
			//	   to z[ij[0]  ][ij[1]-1]
			//	  and z[ij[0]-1][ij[1]]
			// are not satisfactory. Continue the spiral.
			//------------------------------------------------
			if (ij[elle] < l1[k-1])
			{
				ij[elle]++;
				if (ij[elle] > l2[k-1])
				{
					l2[k-1] = ij[elle];
					idir = nxidir;
					nxidir = idir + 1;
					k = nxidir;
					if (nxidir > 3) nxidir = 0;
				}
				ij[0] = Math.abs(ij[0]);
				ij[1] = Math.abs(ij[1]);
				if (densityValues[ij[0]-1][ij[1]-1] > Z_MAX_MAX)
				{
					elle = idir % 2;
					ij[elle] = sign(ij[elle],l1[k-1]);
					continue;
				}
				elle = 0;
				return false;
			}
			if (idir != nxidir)
			{
				nxidir++;
				ij[elle] = l1[k-1];
				k = nxidir;
				elle = 1 - elle;
				ij[elle] = l2[k-1];
				if (nxidir > 3) nxidir = 0;
				continue;
			}
			
			if (ibkey != 0) return true;
			ibkey = 1;
			ij[0] = icur;
			ij[1] = jcur;
			if (Routine_label_020()) continue;
			return false;
		}
	}
	
	//-------------------------------------------------------
	// "Routine_label_200" corresponds to a block of code
	// starting at label 200 in Synder's subroutine "GCONTR".
	// It has return values 0, 1 or 2.
	//-------------------------------------------------------
	short Routine_label_200(Graphics2D g,  boolean workSpace[], int xstart, int ystart, int width, int height)
	{
		
		while (true)
		{
			xy[elle] = 1.0*ij[elle] + intersect[iedge-1];
			xy[1-elle] = 1.0*ij[1-elle];
			workSpace[2*(xSteps*(ySteps*cntrIndex+ij[1]-1)
							 +ij[0]-1) + elle] = true;
			DrawKernel(g, xstart, ystart, width, height);
			if (iflag >= 4)
			{
				icur = ij[0];
				jcur = ij[1];
				return 1;
			}
			ContinueContour();
			if (!workSpace[2*(xSteps*(ySteps*cntrIndex+ij[1]-1)+ij[0]-1)+elle]) return 2;
			iflag = 5;		// 5. Finish a closed contour
			iedge = ks + 2;
			if (iedge > 4) iedge = iedge - 4;
			intersect[iedge-1] = intersect[ks-1];
		}
	}
	
	//-------------------------------------------------------
	// "CrossedByContour" is true iff the current segment in
	// the grid is crossed by one of the contour values and
	// has not already been processed for that value.
	//-------------------------------------------------------
	boolean CrossedByContour(boolean workSpace[])
	{
		ii = ij[0] + i1[elle];
		jj = ij[1] + i1[1-elle];
		z1 = densityValues[ij[0]-1][ij[1]-1];
		z2 = densityValues[ii-1][jj-1];
		for (cntrIndex = 0; cntrIndex < ncv; cntrIndex++)
		{
			int i = 2*(xSteps*(ySteps*cntrIndex+ij[1]-1) + ij[0]-1) + elle;
			
			if (!workSpace[i])
			{
				float x = cv[cntrIndex];
				if ((x>Math.min(z1,z2)) && (x<=Math.max(z1,z2)))
				{
					workSpace[i] = true;
					return true;
				}
			}
		}
		return false;
	}
	
	//-------------------------------------------------------
	// "ContinueContour" continues tracing a contour. Edges
	// are numbered clockwise, the bottom edge being # 1.
	//-------------------------------------------------------
	void ContinueContour()
	{
		short local_k;
		
		ni = 1;
		if (iedge >= 3)
		{
			ij[0] = ij[0] - i3[iedge-1];
			ij[1] = ij[1] - i3[iedge+1];
		}
		for (local_k = 1; local_k < 5; local_k++)
			if (local_k != iedge)
			{
				ii = ij[0] + i3[local_k-1];
				jj = ij[1] + i3[local_k];
				z1 = densityValues[ii-1][jj-1];
				ii = ij[0] + i3[local_k];
				jj = ij[1] + i3[local_k+1];
				z2 = densityValues[ii-1][jj-1];
				if ((cval > Math.min(z1,z2) && (cval <= Math.max(z1,z2))))
				{
					if ((local_k == 1) || (local_k == 4))
					{
						double	zz = z2;
						
						z2 = z1;
						z1 = zz;
					}
					intersect[local_k-1] = (cval - z1)/(z2 - z1);
					ni++;
					ks = local_k;
				}
			}
		if (ni != 2)
		{
			//-------------------------------------------------
			// The contour crosses all 4 edges of cell being
			// examined. Choose lines top-to-left & bottom-to-
			// right if interpolation point on top edge is
			// less than interpolation point on bottom edge.
			// Otherwise, choose the other pair. This method
			// produces the same results if axes are reversed.
			// The contour may close at any edge, but must not
			// cross itself inside any cell.
			//-------------------------------------------------
			ks = 5 - iedge;
			if (intersect[2] >= intersect[0])
			{
				ks = 3 - iedge;
				if (ks <= 0) ks = ks + 4;
			}
		}
		//----------------------------------------------------
		// Determine whether the contour will close or run
		// into a boundary at edge ks of the current cell.
		//----------------------------------------------------
		elle = ks - 1;
		iflag = 1;		// 1. Continue a contour
		jump = true;
		if (ks >= 3)
		{
			ij[0] = ij[0] + i3[ks-1];
			ij[1] = ij[1] + i3[ks+1];
			elle = ks - 3;
		}
	}
	
	//-------------------------------------------------------
	// "ContourPlotKernel" is the guts of this class and
	// corresponds to Synder's subroutine "GCONTR".
	//-------------------------------------------------------
	void ContourPlotKernel(Graphics2D g, boolean workSpace[], int xstart, int ystart, int width, int height)
	{
		short val_label_200;
		
		l1[0] = xSteps;	l1[1] = ySteps;
		l1[2] = -1;l1[3] = -1;
		i1[0] =	1; i1[1] =  0;
		i2[0] =	1; i2[1] = -1;
		i3[0] =	1; i3[1] =  0; i3[2] = 0;
		i3[3] =	1; i3[4] =  1; i3[5] = 0;
		prevXY[0] = 0.0; prevXY[1] = 0.0;
		xy[0] = 1.0; xy[1] = 1.0;
		cntrIndex = 0;
		prevIndex = -1;
		iflag = 6;
		DrawKernel(g, xstart, ystart, width, height);
		icur = Math.max(1, Math.min((int)Math.floor(xy[0]), xSteps));
		jcur = Math.max(1, Math.min((int)Math.floor(xy[1]), ySteps));
		ibkey = 0;
		ij[0] = icur;
		ij[1] = jcur;
		if (Routine_label_020() &&
			Routine_label_150()) return;
		if (Routine_label_050()) return;
		while (true)
		{
			DetectBoundary();
			if (jump)
			{
				if (ix != 0) iflag = 4; // Finish contour at boundary
				iedge = ks + 2;
				if (iedge > 4) iedge = iedge - 4;
				intersect[iedge-1] = intersect[ks-1];
				val_label_200 = Routine_label_200(g,workSpace, xstart, ystart, width, height);
				if (val_label_200 == 1)
				{
					if (Routine_label_020() && Routine_label_150()) return;
					if (Routine_label_050()) return;
					continue;
				}
				if (val_label_200 == 2) continue;
				return;
			}
			if ((ix != 3) && (ix+ibkey != 0) && CrossedByContour(workSpace))
			{
				//
				// An acceptable line segment has been found.
				// Follow contour until it hits a
				// boundary or closes.
				//
				iedge = elle + 1;
				cval = cv[cntrIndex];
				if (ix != 1) iedge = iedge + 2;
				iflag = 2 + ibkey;
				intersect[iedge-1] = (cval - z1) / (z2 - z1);
				val_label_200 = Routine_label_200(g,workSpace, xstart, ystart, width, height);
				if (val_label_200 == 1)
				{
					if (Routine_label_020() && Routine_label_150()) return;
					if (Routine_label_050()) return;
					continue;
				}
				if (val_label_200 == 2) continue;
				return;
			}
			if (++elle > 1)
			{
				elle = idir % 2;
				ij[elle] = sign(ij[elle],l1[k-1]);
				if (Routine_label_150()) return;
			}
			if (Routine_label_050()) return;
		}
	}
	
	
	//-------------------------------------------------------
	// "paint" overrides the superclass' "paint()" method.
	// This method draws the grid and then the contours,
	// provided that the first two contour values are not
	// equal (which would indicate invalid data).
	// The "workSpace" is used to remember which segments in
	// the grid have been crossed by which contours.
	//-------------------------------------------------------
	public void draw(Graphics2D g2, int xstart, int ystart, int width, int height)
	{
		g2.setRenderingHint(RenderingHints.KEY_ANTIALIASING,RenderingHints.VALUE_ANTIALIAS_ON);
		int workLength = 2 * xSteps * ySteps * ncv;
		boolean	workSpace[]; // Allocate below if data valid
		
		SetMeasurements(width, height);
		DrawGrid(g2, xstart, ystart, width, height);
		if (cv[0] != cv[1]) { // Valid data
			workSpace = new boolean[workLength];
			ContourPlotKernel(g2, workSpace, xstart, ystart, width, height);
		}
	}
	
	
	//-------------------------------------------------------
	// "MakeMatrixRectangular" appends zero(s) to the end of
	// any row of "z" which is shorter than the longest row.
	//-------------------------------------------------------
	public void MakeMatrixRectangular()
	{
		int	i,y,leng;
		
		xSteps = densityValues.length;
		ySteps = MIN_Y_STEPS;
		for (i = 0; i < xSteps; i++)
		{
			y = densityValues[i].length;
			if (ySteps < y) ySteps = y;
		}
		
		for (i = 0; i < xSteps; i++)
		{
			leng = densityValues[i].length;
			if (leng < ySteps)
			{
				float temp[] = new float[ySteps];
				
				System.arraycopy(densityValues[i], 0, temp, 0, leng);
				while (leng < ySteps) temp[leng++] = 0;
				densityValues[i] = temp;
			}
		}
	}
	
}
