package minigames.client.checkmates;

/**
 * Interface for components that can be dynamically themed.
 * <p>Implementing classes handle their own color updates when themes change by their implementation
 * of the <b>applyTheme(GuiThemes.ColorScheme colors)<b> method.
 * <p>Implementing this interface registers classes to receive GuiThemes.
 * This is an indirect link to the final colour values, as GuiThemes uses an internal
 * ColorScheme interface to communicate the precise details of the active colour scheme.
 */
public interface Themeable {
    /**
     * Apply the given color scheme to this component.
     * Each component decides how to use the colors.
     * Should call repaint() after updating colors.
     *
     * @param colors The color scheme to apply
     */
    void applyTheme(GuiThemes.ColorScheme colors);
}