package lab06;

public interface ISewagePlant {
    void setPumpIn(int number, int volume);
    int getStatus(int number);
    void setPayoff(int number);
}
