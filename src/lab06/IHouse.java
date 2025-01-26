package lab06;

public interface IHouse {
    /**
     * Pumps out sewage from the septic tank, returning the amount cleared.
     *
     * @param max Maximum amount the Tanker can pump
     * @return The actual amount pumped out
     */
    int getPumpOut(int max);
}
