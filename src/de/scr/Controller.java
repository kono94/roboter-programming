package de.scr;

import de.scr.config.Constants;
import de.scr.config.RunControl;
import de.scr.ev3.ResourceManager;
import de.scr.ev3.ResourceManagerLocal;
import de.scr.ev3.ResourceManagerRemote;
import de.scr.ev3.components.Drivable;
import de.scr.ev3.components.MyColorSensor;
import de.scr.ev3.components.MyDistanceSensor;
import de.scr.ev3.components.MyGyroSensor;
import de.scr.logic.EvadeObstacleController;
import de.scr.logic.FollowLineController;
import de.scr.logic.HoldDistanceController;
import de.scr.utils.TwoColors;
import lejos.hardware.Battery;
import lejos.hardware.Button;
import lejos.remote.ev3.RemoteEV3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

public class Controller {
    private static Logger logger = LoggerFactory.getLogger(Controller.class);

    public static volatile RunControl RUN = RunControl.STOP;
    private final Object lock = new Object();
    private ResourceManager resourceManager;
    private FollowLineController followLineController;
    private HoldDistanceController spaceKeeperController;
    private EvadeObstacleController evadeObstacleController;
    private Drivable drivable;
    private MyColorSensor primaryColorSensor;
    private MyDistanceSensor primaryDistanceSensor;
    private MyColorSensor secondaryColorSensor;
    private MyGyroSensor gyroSensor;
    private TwoColors darkColor;
    private TwoColors lightColor;

    // when true => is running locally on the robot,
    // when false => using RMI
    private boolean isRunningOnDevice;

    public Controller() {
        logger.info("Creating Controller");
        this.isRunningOnDevice = System.getProperty("os.arch").toLowerCase().matches("arm");
        init();
    }

    private void init() {
        logger.info("Init Controller");
        Controller.RUN = RunControl.FOLLOW_EVADE;
        initResourceManager();
        createEv3Components();
//        registerShutdownOnClick();

        modiSwitcher();
    }

    //TODO: Implement kill-switch
    private void modiSwitcher() {
        switch (RUN) {
            case FOLLOW_EVADE:
                followLine();
                evadeObstacle();
                break;
            case FOLLOW_HOLD:
                followLine();
                holdDistance();
                break;
            case EVADE_OBSTACLE:
                evadeObstacle();
                break;
            case FOLLOW_LINE:
                followLine();
                break;
            case HOLD_DISTANCE:
                holdDistance();
                break;
        }
    }

    private void initResourceManager() {
        if (isRunningOnDevice) {
            logger.info("Connecting local");
            resourceManager = new ResourceManagerLocal();
        } else {
            logger.info("Connecting with RMI");
            RemoteEV3 ev3;
            try {
                ev3 = new RemoteEV3(Constants.REMOTE_HOST);
                ev3.setDefault();
            } catch (RemoteException | MalformedURLException | NotBoundException e) {
                e.printStackTrace();
                throw new RuntimeException("Could not setup RemoteEV3");
            }
            resourceManager = new ResourceManagerRemote(ev3);
        }
    }

    private void createEv3Components() {
        drivable = resourceManager.createDrivable(Constants.MOTOR_PORT_LEFT, Constants.MOTOR_PORT_RIGHT);
        primaryColorSensor = new MyColorSensor(resourceManager.createColorSensor(Constants.COLOR_SENSOR_PORT));
        primaryDistanceSensor = new MyDistanceSensor(resourceManager.createDistanceSensor(Constants.DISTANCE_SENSOR_PORT));
        secondaryColorSensor = new MyColorSensor(resourceManager.createColorSensor(Constants.COLOR_SENSOR_2_PORT));
        gyroSensor = resourceManager.createGyroSensor(Constants.GYRO_SENSOR_PORT);
    }

    private void followLine() {
        logger.info("Start followLine Mode");
        followLineController = new FollowLineController(this, drivable, primaryColorSensor, secondaryColorSensor);
        followLineController.init();
        followLineController.start(lock);
    }

    private void holdDistance() {
        logger.info("Start holdDistance Mode");
        spaceKeeperController = new HoldDistanceController(drivable, primaryDistanceSensor);
        spaceKeeperController.init();
        spaceKeeperController.start(lock);
    }

    private void evadeObstacle() {
        logger.info("Start evadeObstacle Mode");
        evadeObstacleController = new EvadeObstacleController(this, drivable, gyroSensor, primaryDistanceSensor, primaryColorSensor);
        evadeObstacleController.init();
        evadeObstacleController.start(lock);
    }

    private void registerShutdownOnClick() {
        new Thread(() -> {
            Button.UP.waitForPressAndRelease();
            Controller.RUN = RunControl.STOP;
            logger.info("battery: " + Battery.getVoltage());
        }).start();
    }

    public TwoColors getDarkColor() {
        return darkColor;
    }

    public TwoColors getLightColor() {
        return lightColor;
    }

    public void setDarkColor(TwoColors darkColor) {
        this.darkColor = darkColor;
    }

    public void setLightColor(TwoColors lightColor) {
        this.lightColor = lightColor;
    }
}
