package lib;

import components.Drivable;
import components.DriveRemote;
import lejos.hardware.port.Port;
import lejos.hardware.sensor.EV3ColorSensor;
import lejos.hardware.sensor.EV3TouchSensor;
import lejos.hardware.sensor.EV3UltrasonicSensor;
import lejos.remote.ev3.RMIRegulatedMotor;
import lejos.remote.ev3.RemoteEV3;

import java.io.Closeable;
import java.util.ArrayList;
import java.util.List;

public class ResourceManagerRemote implements ResourceManager {
    private RemoteEV3 ev3;
    private List<RMIRegulatedMotor> regulatedMotors;
    private List<Closeable> sensors;

    ResourceManagerRemote(RemoteEV3 ev3) {
        this.ev3 = ev3;
        regulatedMotors = new ArrayList<>();
        sensors = new ArrayList<>();
        freeResourcesOnShutdown();
    }

    public EV3UltrasonicSensor createDistanceSensor(Port port) {
        EV3UltrasonicSensor sensor = new EV3UltrasonicSensor(port);
        sensor.setCurrentMode("Distance");
        sensors.add(sensor);
        return sensor;
    }

    public EV3TouchSensor createTouchSensor(Port port) {
        EV3TouchSensor sensor = new EV3TouchSensor(port);
        sensors.add(sensor);
        return sensor;
    }

    public EV3ColorSensor createColorSensor(Port port) {
        EV3ColorSensor colorSensor = new EV3ColorSensor(port);
        sensors.add(colorSensor);
        return colorSensor;
    }

    public Drivable createDrivable(Port motorA, Port motorB) {
        return new DriveRemote(createRegularMotor(motorA), createRegularMotor(motorB));
    }

    private RMIRegulatedMotor createRegularMotor(Port port) {
        RMIRegulatedMotor motor = ev3.createRegulatedMotor(port.getName(), 'L');
        regulatedMotors.add(motor);
        return motor;
    }

    private void freeResourcesOnShutdown() {
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            System.out.println(Thread.getAllStackTraces().size());
            System.out.println("Shutdown: Starting Shutdown-Hook");
            Controller.RUN = false;
            try {
                Thread.sleep(1000);
                for (RMIRegulatedMotor regulatedMotor : regulatedMotors) {
                    regulatedMotor.close();
                }
                System.out.println("Showdown: Closed RMIRegulatedMotor");
                for (Closeable sensor : sensors) {
                    sensor.close();
                }
                System.out.println("Showdown: Closed Sensors");
            } catch (Exception e) {
                e.printStackTrace();
            }
            System.out.println("Shutdown: Finished Shutdown");
        }));
    }

    public List<RMIRegulatedMotor> getRegulatedMotors() {
        return regulatedMotors;
    }

    public List<Closeable> getSensors() {
        return sensors;
    }
}
