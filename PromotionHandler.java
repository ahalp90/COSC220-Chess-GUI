package minigames.client.checkmates;

/**
 * Interface to allow calling GUI overlays from within the GUI EDT rather than from within GameBoard's thread.
 */
public interface PromotionHandler {
    char getPromotionChoice(char playerColour);
}
