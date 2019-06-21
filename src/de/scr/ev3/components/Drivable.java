package de.scr.ev3.components;

public interface Drivable {
    void drive(int speed, int turn);

    void rotateOnPlaceOld(int speed, int degree);

    void rotateOnPlace(int speed, int degree, MyGyroSensor gyroSensor, boolean oneWheelOnly);
    void drive(int turn);
    void setSpeed(int speed);
    int getSpeed();
}
