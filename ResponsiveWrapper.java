package minigames.client.checkmates;

import javax.swing.*;
import java.awt.*;


/**
 * Facilitates responsive resizing.
 * Addresses the issue of a parent Swing layout manager that is non-stretching (eg. FlowLayout).
 * Provides a bridge between this object's parent, which is expected to not propagate resize events, and its children.
 * The wrapper will be resized, and pass this information to its children.
 */
public class ResponsiveWrapper extends JPanel {

    /**
     * Creates a ResponsiveWrapper with an internal layout manager of BorderLayout.
     * Allows responsive stretching of child components regardless of the originating parent's
     * (ie MinigameNetworkClientWindow's) layout manager's responsiveness to child components.
     */
    public ResponsiveWrapper() {
        super(new BorderLayout());
    }


    /**
     * Reports this panel's preferred size as being equal to its parent's current size.
     * Results in the non-stretching parent giving this panel all its available space.
     * @return Dimension of the parent's current size, or a default of (400,400) if the parent's
     * dimensions are not yet available.
     */
    @Override
    public Dimension getPreferredSize() {
        Container parent = getParent();
        if (parent != null && parent.getWidth() >0) {
            return parent.getSize();
        } else {
            return new Dimension(400,400);
        }
    }
}

  