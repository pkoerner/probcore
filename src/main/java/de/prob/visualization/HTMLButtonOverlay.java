package de.prob.visualization;

import java.awt.Dimension;
import java.util.ArrayList;
import java.util.List;

import javax.swing.JButton;
import javax.swing.JFrame;

import com.mxgraph.swing.util.mxICellOverlay;
import com.mxgraph.util.mxConstants;
import com.mxgraph.util.mxPoint;
import com.mxgraph.util.mxRectangle;
import com.mxgraph.view.mxCellState;

public class HTMLButtonOverlay extends JButton implements mxICellOverlay {

	/**
	 * 
	 */
	private static final long serialVersionUID = -464923512499245992L;

	protected Object align = mxConstants.ALIGN_RIGHT;
	protected Object verticalAlign = mxConstants.ALIGN_BOTTOM;

	public HTMLButtonOverlay(final String html, final int width,
			final int height) {
		super(html);
		setSize(width, height);
	}

	@Override
	public mxRectangle getBounds(final mxCellState state) {
		boolean isEdge = state.getView().getGraph().getModel()
				.isEdge(state.getCell());
		double s = state.getView().getScale();
		mxPoint pt = null;

		int w = getWidth();
		int h = getHeight();

		if (isEdge) {
			int n = state.getAbsolutePointCount();

			if (n % 2 == 1) {
				pt = state.getAbsolutePoint(n / 2 + 1);
			} else {
				int idx = n / 2;
				mxPoint p0 = state.getAbsolutePoint(idx - 1);
				mxPoint p1 = state.getAbsolutePoint(idx);
				pt = new mxPoint(p0.getX() + (p1.getX() - p0.getX()) / 2,
						p0.getY() + (p1.getY() - p0.getY()) / 2);
			}
		} else {
			pt = new mxPoint();

			if (align.equals(mxConstants.ALIGN_LEFT)) {
				pt.setX(state.getX());
			} else if (align.equals(mxConstants.ALIGN_CENTER)) {
				pt.setX(state.getX() + state.getWidth() / 2);
			} else {
				pt.setX(state.getX() + state.getWidth());
			}

			if (verticalAlign.equals(mxConstants.ALIGN_TOP)) {
				pt.setY(state.getY());
			} else if (verticalAlign.equals(mxConstants.ALIGN_MIDDLE)) {
				pt.setY(state.getY() + state.getHeight() / 2);
			} else {
				pt.setY(state.getY() + state.getHeight());
			}
		}
		return new mxRectangle(pt.getX() - w * s, pt.getY() - h * s, w, h);
	}

	public static void main(final String[] args) {
		// String html =
		// "<html><b>Foo</B><br><b>variables</b><br><b>constants</b><br><b>operations</b><br>not bold</html>";
		List<String> ss = new ArrayList<String>();
		ss.add("a");
		ss.add("b");
		ss.add("c");
		ss.add("d");
		ss.add("e");
		ss.add("f");
		ss.add("was langes");
		ss.add("was richtig langes");
		int right = "was richtig langes".length() * 15;
		HTMLgenerator g = new HTMLgenerator(right);
		g.writeHeading("Variables");
		g.writeList(ss);
		g.end();
		// HTMLButtonOverlay overlay = new HTMLButtonOverlay(html);
		HTMLButtonOverlay overlay = new HTMLButtonOverlay(g.get(), right, 100);
		overlay.setVisible(true);
		JFrame frame = new JFrame("=)");
		frame.add(overlay);
		frame.setSize(new Dimension(100, 100));
		frame.pack();
		frame.setVisible(true);
	}
}
