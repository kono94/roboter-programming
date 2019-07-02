package de.scr;

import de.scr.config.Constants;
import de.scr.ev3.ResourceManager;
import de.scr.ev3.ResourceManagerLocal;
import de.scr.ev3.ResourceManagerRemote;
import de.scr.ev3.components.Drivable;
import de.scr.ev3.components.MyColorSensor;
import de.scr.ev3.components.MyDistanceSensor;
import de.scr.ev3.components.MyGyroSensor;
import de.scr.logic.ConvoyController;
import de.scr.logic.EvadeObstacleController;
import de.scr.logic.FollowLineController;
import de.scr.logic.OdometryController;
import de.scr.utils.RunControl;
import de.scr.utils.TwoColors;
import lejos.remote.ev3.RemoteEV3;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.MalformedURLException;
import java.rmi.NotBoundException;
import java.rmi.RemoteException;

/**
 * Initialize the main components for the ev3-control
 */
public class Controller {
    private static Logger logger = LoggerFactory.getLogger(Controller.class);

    public volatile RunControl RUN = RunControl.STOP;
    private final Object lock = new Object();
    private ResourceManager resourceManager;
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
        logger.info("EV3 is started {}", isRunningOnDevice ? "on local" : "via remote");
        init();
    }

    private void init() {
        logger.info("Init Controller");
        initResourceManager();
        RUN = Constants.START_MODE;
        logger.info("Current Mode: {}", RUN);
        createEv3Components();
        modiSwitcher();
    }

    private void initResourceManager() {
        logger.info("Initializing ResourceManager");
        if (isRunningOnDevice) {
            logger.info("Connecting local");
            resourceManager = new ResourceManagerLocal(this);
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
            resourceManager = new ResourceManagerRemote(this, ev3);
        }
    }

    private void modiSwitcher() {
        switch (RUN) {
            case LINE_EVADE:
                followLine();
                evadeObstacle();
                break;
            case LINE_CONVOY:
                followLine();
                holdDistance();
                break;
            case LINE:
                followLine();
                break;
            case GUI_MODE:
                odometry();
                break;
            default:
                logger.warn("{} is not a valid Start-Mode!", RUN);
        }
    }

    public void changeRunControl(RunControl c) {
        this.RUN = c;
        synchronized (lock) {
            lock.notifyAll();
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
        FollowLineController followLineController = new FollowLineController(this, drivable, primaryColorSensor, secondaryColorSensor);
        followLineController.init();
        followLineController.start(lock);
    }

    private void holdDistance() {
        logger.info("Start holdDistance Mode");
        ConvoyController spaceKeeperController = new ConvoyController(this, drivable, primaryDistanceSensor);
        spaceKeeperController.init();
        spaceKeeperController.start(lock);
    }

    private void evadeObstacle() {
        logger.info("Start evadeObstacle Mode");
        EvadeObstacleController evadeObstacleController = new EvadeObstacleController(this, drivable, gyroSensor, primaryDistanceSensor);
        evadeObstacleController.init();
        evadeObstacleController.start(lock);
    }

    private void odometry() {
        logger.info("Start odometry Mode");
        OdometryController odometryController = new OdometryController(drivable, gyroSensor);
        odometryController.start();
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
