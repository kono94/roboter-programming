package components;

import lejos.hardware.Sound;
import lejos.remote.ev3.RMIRegulatedMotor;
import lejos.utility.Delay;

import java.rmi.RemoteException;

public class DriveRemote implements Drivable {
    private RMIRegulatedMotor left;
    private RMIRegulatedMotor right;

    public DriveRemote(RMIRegulatedMotor left, RMIRegulatedMotor right) {
        this.left = left;
        this.right = right;
    }

    /**
     *
     * @param speed from -100 to 100 in percent of max speed, negative equals backwards
     * @param turn from -100 to 100 relation between left (-) and right (+) wheel; 100 only the right wheel is
     *             rotation in speed %; 0 equals straight forward
     */
    public void drive(int speed, int turn) {
        try {
            int maxSpeed = (int) right.getMaxSpeed();
            int customSpeed = (maxSpeed * speed)/100;
            /*
                 300  turn 0 = left 300 right 300
                 300  turn 50 = left 150 right 300
                 300 turn 100 = left 0 right 300

                 300 turn -50 = left 300 right 150
                 300 turn -100 = left 300 right 0

                 // right side always
                 0.01 => -50 turn
                 0.14 => 100 turn
                 0.06 => 0 turn

                 0.01     0.06        0.14
                 -50       0          100


                            I------------------I
                0.16(blue)       0.06(black)

               0.5(grey-table)              0.2-0.3 (half-half)

                 - => drive left
                 + => drive right
             */

            if(turn > 0 && turn <= 50){
                right.setSpeed(customSpeed);
                right.forward();
                left.setSpeed((int) (customSpeed * ((100 - turn)/ (double)100)));
                left.forward();
            }else if(turn > 50){
                right.setSpeed(customSpeed);
                right.forward();
                left.setSpeed((int) (customSpeed * ((turn)/ (double)100)));
                left.backward();
            }else if(turn >= -50){
                left.setSpeed(customSpeed);
                left.forward();
                right.setSpeed((int) (customSpeed * (100 -(-turn))/(double)100));
                right.forward();
            }else{
                left.setSpeed(customSpeed);
                left.forward();
                right.setSpeed((int) (customSpeed * (-turn)/(double)100));
                right.backward();
            }

            //System.out.println("Left-Speed: " + left.getSpeed());
           // System.out.println("Right-Speed: " + right.getSpeed());

            /*
            if(speed > 0){
                right.forward();
                left.forward();
            }else if(speed < 0 ){
                right.backward();
                left.backward();
            }else{
                right.stop(true);
                left.stop(true);
            }
            */

        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }

    public void rotateOnPlace(int speed, int degree) {
        try {
            right.stop(true);
            left.stop(true);
            right.setSpeed(speed);
            left.setSpeed(speed);
            right.forward();
            left.backward();
            Delay.msDelay((772000 / speed * degree) / 360);
            left.stop(true);
            right.stop(true);
        } catch (RemoteException e) {
            e.printStackTrace();
        }
    }
}