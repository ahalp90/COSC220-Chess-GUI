package minigames.client.checkmates;

import java.awt.Color;
import java.util.ArrayList;
import java.util.List;

/**
 * Dynamic theme system for the chess GUI.
 * <p>(Generated with AI assistance based on explicit architectural and design instructions).
 * <p>Manages color schemes and coordinates theme switching across components.
 * When the theme is switched, this class iterates through its list of registered components
 * and instructs them to apply the new colour schemes.
 * <p>Relies on two interfaces:
 * <li>internal ColorScheme interface defines the colours a theme must provide. Implemented by internal theme classes.
 * <li>standalone Themeable interface defines the method needed to apply themes to the relevant class objects.
 */
public enum GuiThemes {
    ARI("Rock Climbing & Chai", new AriColors()),
    GEOFF("Pizza Party", new GeoffColors()),
    CURTIS("Vim Beach", new CurtisColors()),
    HAHN("Cheerful Dystopia", new HahnColors()),
    DEFAULT("Classic Enhanced", new DefaultColors());

    private final String displayName;
    private final ColorScheme colors;

    // Current active theme; default is DEFAULT.
    private static GuiThemes currentTheme = DEFAULT;

    // Track all components that need to be updated when a theme changes
    private static final List<Themeable> themeableComponents = new ArrayList<>();

    GuiThemes(String displayName, ColorScheme colors) {
        this.displayName = displayName;
        this.colors = colors;
    }

    /**
     * Register a component to receive and apply theme updates.
     */
    public static void register(Themeable component) {
        themeableComponents.add(component);
        // Apply current theme to newly registered component
        component.applyTheme(currentTheme.colors);
    }

    /**
     * Clear all registered components from the notification list
     * <b>WARNING</b> Call this when closing/cleaning up the game.
     * Otherwise the registration List will continually continually accumulate irrelevant references.
     */
    public static void clearRegistrations() {
        themeableComponents.clear();
    }

    /**
     * Switch to a new theme and update all registered components.
     * @param newTheme, the GuiThemes Enum constant ot switch to.
     */
    public static void switchTheme(GuiThemes newTheme) {
        currentTheme = newTheme;
        // Update all registered observers of the theme change.
        for (Themeable component : themeableComponents) {
            component.applyTheme(currentTheme.colors);
        }
    }

    /**
     * Get the current active theme.
     * @return the currently active GuiThemes Enum constant.
     */
    public static GuiThemes getCurrentTheme() {
        return currentTheme;
    }

    /**
     * Get the current theme's colors.
     * @return the currently active ColorScheme object (GuiThemes inner-class).
     */
    public static ColorScheme current() {
        return currentTheme.colors;
    }

    /**
     * Gets a user-friendly display name for this theme.
     * @return String of the theme's display name.
     */
    public String getDisplayName() {
        return displayName;
    }

    /**
     * Interface defining all colors a theme must provide.
     * Each theme identified in the below Colors inner classes must implement this
     * to be made available to Themeable UI components.
     */
    public interface ColorScheme {
        // Board colours
        Color boardLight();
        Color boardDark();

        // UI colours
        Color background();
        Color surface();     // For nested panels
        Color text();
        Color textMuted(); //An offset text colour
        Color border();

        // Highlight colors
        Color moveHighlight();
        Color selectionHighlight();
    }

    //*** PRIVATE INNER CLASSES THAT DEFINE THE PRECISE COLOUR VALUES OF THE THEMES ***

    /**
     * ARI: Rock Climbing & Chai theme
     */
    private static class AriColors implements ColorScheme {
        public Color boardLight() { return new Color(0xE8, 0xDC, 0xC7); }  // Chalk/sandstone
        public Color boardDark() { return new Color(0x6B, 0x5B, 0x54); }   // Granite/rock face
        public Color background() { return new Color(0xF5, 0xEE, 0xE6); }  // Warm cream
        public Color surface() { return new Color(0xFA, 0xF4, 0xED); }     // Chai foam
        public Color text() { return new Color(0x3E, 0x2E, 0x26); }        // Espresso brown
        public Color textMuted() { return new Color(0x7C, 0x6F, 0x64); }   // Light brown
        public Color border() { return new Color(0xC9, 0xB7, 0xA7); }      // Tan
        public Color moveHighlight() { return new Color(0xFF, 0xD7, 0x00, 200); }  // Flashy yellow, high opacity
        public Color selectionHighlight() { return new Color(0x8B, 0xC3, 0x4A, 150); }  // Carabiner green
    }

    /**
     * GEOFF: Pizza Party theme
     */
    private static class GeoffColors implements ColorScheme {
        public Color boardLight() { return new Color(0xFF, 0xF8, 0xE7); }  // Mozzarella
        public Color boardDark() { return new Color(0x8B, 0x2C, 0x1B); }   // Pepperoni
        public Color background() { return new Color(0xD4, 0xA5, 0x74); }  // Crust
        public Color surface() { return new Color(0xE5, 0xB8, 0x8D); } // Chestnut
        public Color text() { return new Color(0x4A, 0x0E, 0x0E); }        // Dark marinara
        public Color textMuted() { return new Color(0x6B, 0x2C, 0x2C); }
        public Color border() { return new Color(0xA5, 0x67, 0x3F); } //Dark tan
        public Color moveHighlight() { return new Color(0xFF, 0xD7, 0x00, 200); }  // Cheese yellow
        public Color selectionHighlight() { return new Color(0xFF, 0xE5, 0x4D, 150); }  // Brighter cheese
    }

    /**
     * CURTIS: Vim Beach theme
     */
    private static class CurtisColors implements ColorScheme {
        public Color boardLight() { return new Color(0xF4, 0xE4, 0xC1); }  // Sand
        public Color boardDark() { return new Color(0x7F, 0xB0, 0x69); }   // Sea foam
        public Color background() { return new Color(0xE8, 0xD5, 0xB7); }  // Driftwood
        public Color surface() { return new Color(0xF0, 0xE5, 0xCF); } //Warm beige
        public Color text() { return new Color(0x2C, 0x5F, 0x2D); }        // Seaweed
        public Color textMuted() { return new Color(0x4E, 0x7C, 0x4F); } //Fern green
        public Color border() { return new Color(0xB8, 0xA8, 0x90); } //Tan
        public Color moveHighlight() { return new Color(0xFF, 0xB3, 0xBA, 200); } // Coral
        public Color selectionHighlight() { return new Color(0x87, 0xCE, 0xEB, 150); } // Sky blue
    }

    /**
     * HAHN: Cheerful Dystopia theme
     */
    private static class HahnColors implements ColorScheme {
        public Color boardLight() { return new Color(0xD0, 0xD0, 0xD0); }  // Concrete
        public Color boardDark() { return new Color(0xFF, 0x6E, 0xC7); }   // Neon pink
        public Color background() { return new Color(0xE5, 0xE5, 0xE5); }  // Industrial light
        public Color surface() { return new Color(0xF0, 0xF0, 0xF0); }
        public Color text() { return new Color(0x33, 0x33, 0x33); }        // Dark Charcoal
        public Color textMuted() { return new Color(0x66, 0x44, 0x88); } // Muted Purple
        public Color border() { return new Color(0xC0, 0xC0, 0xC0); }
        public Color moveHighlight() { return new Color(0x39, 0xFF, 0x14, 200); } // Lime green
        public Color selectionHighlight() { return new Color(0xFF, 0x00, 0xFF, 150); } // Hot magenta
    }

    /**
     * Classic Enhanced theme
     */
    private static class DefaultColors implements ColorScheme {
        public Color boardLight() { return new Color(0xFF, 0xFD, 0xE0); }  // Keep current
        public Color boardDark() { return new Color(0x35, 0x34, 0x33); }   // Keep current
        public Color background() { return new Color(0xf7, 0xf3, 0xed); }  // Warm off-white
        public Color surface() { return new Color(0xfe, 0xfb, 0xf7); } // Creamy white
        public Color text() { return new Color(0x2C, 0x2C, 0x2C); }        // Charcoal
        public Color textMuted() { return Color.GRAY; }
        public Color border() { return new Color(0xd4, 0xcf, 0xc7); } // Warm grey
        public Color moveHighlight() { return new Color(0xFF, 0x64, 0x64, 200); } // Softer red
        public Color selectionHighlight() { return new Color(0xDB, 0xCC, 0xBD, 150); } // Wheat
    }
}