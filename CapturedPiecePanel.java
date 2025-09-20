package minigames.client.checkmates;

import javax.swing.*;
import java.awt.*;
import java.util.ArrayList;
import java.util.List;

/**
 * A panel that displays a row of ImageIcons, intended for captured chess pieces.
 * <p>Handles all the logic for drawing, scaling, and updating the icons.
 * Designed to support a fully responsive UI, using paintComponent to direct draw and
 * minimise reliance on layout manager quirks.
 * <p> Scales icons dynamically to fit a panel's changing height.
 * Prevents pieces from being clipped when a window shrinks.
 * <p> Supports layout stability. Prevents the entire parent component from popping in and out of existence,
 * or recalculating itself when piece is captured and an icon is added.
 * <p> Explicitly defines predictable size hints to its parent layout manager by
 * overriding getPreferredSize() and getMinSize().
 * This prevents the panel from disappearing or glitching at small window sizes.
 */
public class CapturedPiecePanel extends JPanel {
    private List<ImageIcon> iconsToDraw = new ArrayList<>();
    private final int preferredHeight;
    private final int initialWidthHint;

    /**
     * Builds a new CapturedPiecePanel. Transparent panel around its icon children. Custom drawing logic to
     * support responsive layout without being squished by larger sibling components.
     * @param preferredHeight int of a constant preferred height to reserve for the parent layout manager
     * @param initialWidthHint int hint of a minimum to provide during an initial pack() before the parent
     *                         has a defined size.
     */
    public CapturedPiecePanel(int preferredHeight, int initialWidthHint) {
        this.preferredHeight = preferredHeight;
        this.initialWidthHint = initialWidthHint;
        setOpaque(false); //Transparent.
    }

    /**
     * Sets the list of icons this panel should display.
     * <p> Clears any previously displayed icons and triggers a repaint() to render the new set.
     * <p> Deliberately does not revalidate to avoid reproportioning, which could trigger the Label popping in
     * and out of existence in its intended use.
     * @param icons a List of ImageIcons to display (eg. for captured pieces).
     */
    public void setIcons(List<ImageIcon> icons) {
        this.iconsToDraw = icons;
        repaint(); // Trigger a repaint to show the new icons.
    }

    /**
     * Does the painting. Iterates through the iconsToDraw List and draws each one,
     * dynamically scaled to fit the panel's current height.
     * <p>Aligns icons starting from the left of this Label.
     * <p>Necessitated because the only perfectly responsive Swing manager for our context--
     * GridLayout--forced auto-centring.
     * Method generated with AI assistance.
     * @param g the <code>Graphics</code> object to protect
     */
    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        int panelHeight = getHeight();
        if (panelHeight <= 0 || iconsToDraw.isEmpty()) {
            return; // Don't draw if there's no space or nothing to draw.
        }

        int padding = 2; // A small gap from the top/bottom edges.
        int iconSize = Math.max(0, panelHeight - (2 * padding));
        int y = padding;
        int x = padding; // Start drawing from the left.

        for (ImageIcon icon : iconsToDraw) {
            if (icon != null) {
                // Draw the icon, scaled on-the-fly to the correct size.
                g.drawImage(icon.getImage(), x, y, iconSize, iconSize, this);
                x += iconSize + 2; // Move the starting point for the next icon.
            }
        }
    }

    /**
     * Provides a predictable preferred size to the parent.
     * <p>Critical for layout stability. Reports consistent and concrete dimensions to consider even if
     * the object has not yet been populated with relevant content to determine its size, and in lieu of
     * static size assignment by the parent.
     * <p> Height is set as an (argument) constant to prevent layout popping.
     * <p> Width is reported dynamically based on the parent's size to ensure it fills the available horizontal space.
     * @return
     */
    @Override
    public Dimension getPreferredSize() {
        int width;
        if (getParent() != null && getParent().getWidth() > 0) {
            width = getParent().getWidth();
        } else {
            width = initialWidthHint;
        }
        return new Dimension(width, this.preferredHeight);
    }

    /**
     * Provides an absolute minimum for this panel's height, preventing it from disappearing.
     * <p>Essential to prevent GridBagLayout from squishing this out of existence when the
     * parent window shrinks and GridBag assesses that a sigbling element needs the space more than this object does.
     * @return Dimension minimum of (0,10).
     */
    @Override
    public Dimension getMinimumSize() {return new Dimension(0,10);}

}
