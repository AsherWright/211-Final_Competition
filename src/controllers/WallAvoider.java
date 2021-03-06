package controllers;
import odometry.Odometer;
import pollers.UltrasonicPoller;
import lejos.hardware.Sound;
import lejos.hardware.motor.EV3LargeRegulatedMotor;

/**
 * Enables the robot to avoid walls in the way. This method is used by navigation to
 * avoid walls while traveling.
 * @author AsherW
 *
 */
public class WallAvoider {
	private Odometer odo;
	private UltrasonicPoller frontPoller;
	private UltrasonicPoller sidePoller;
	private EV3LargeRegulatedMotor leftMotor;
	private EV3LargeRegulatedMotor rightMotor;
	
	private int bandCenter = 13;
	private int bandwidth = 5;
	//motorStraight is the default speed of the motors (deg/s). 
	//Filter out is the amount of maxdistance measurements we take before we consider them real.
	private final int motorStraight = 90;
	//PROPCONSTINSIDETURN is the Propogation constant if the robot is making an INSIDE turn.
	public static final double PROPCONSTINSIDETURN = 10.0; //proportionality constant 8,3
	//PROPCONSTOUTSIDETURN is the propogation constant if the robot is making an OUTSIDE turn.
	public static final double PROPCONSTOUTSIDETURN = 8.0;
	
	//we have two propogation constants so that we can make very sharp inside turns, but
	//-wide enough outside turns (otherwise we won't make inside turns OR will make too tight
	//-outside turns.
	
	//max correction is the max speed we allow the system to correct to! Unlike the original code
	//, we don't allow ANY speeds greater than this. This is the REAL max. 

	public static final int MAXCORRECTION = 50; //was 80
	/**
	 * Basic constructor
	 * @param odo The robot's odometer
	 * @param frontPoller The ultrasonic poller facing forwards
	 * @param sidePoller The ultrasonic poller facing 90-degrees
	 */
	public WallAvoider(Odometer odo, UltrasonicPoller frontPoller, UltrasonicPoller sidePoller){
		this.odo = odo;
		EV3LargeRegulatedMotor[] motors = odo.getMotors();
		leftMotor = motors[0];
		rightMotor = motors[1];
		this.frontPoller = frontPoller;
		this.sidePoller = sidePoller;
	}
	/**
	 * Avoids a wall that is in front of the robot. Works by rotating, and then performing a 
	 * wall-avoiding algorithm to follow along the wall with the side poller.
	 * Uses the block's position to figure out when it passes the block (and should stop).
	 * @param blockX The block's x position 
	 * @param blockY The block's y position
	 * @param endX The final x position
	 * @param endY The final y position
	 */
	public void avoidWall(double blockX, double blockY, double endX, double endY){
		
		while(true){
	
			if(frontPoller.getUsData() > 15 ){
				double distance = sidePoller.getUsData();
				int speedAdjustment = 0;
				
				//check to see if we are in the place we want to be
				if(distance < bandCenter + bandwidth && distance > bandCenter - bandwidth){
					//if so, we set speedAdjustment to 0 (keep motor speeds the same)
					speedAdjustment = 0;

				//if we are too close to the wall
				}else if (distance < bandCenter){
					//calculate and store our speedAdjustment
					speedAdjustment = getWheelRotationCorrection(distance-bandCenter, PROPCONSTINSIDETURN);
				//if we are too far from the wall
				}else if (distance > bandCenter) {
					//calculate and store our speed adjustment
					speedAdjustment = getWheelRotationCorrection(distance -bandCenter, PROPCONSTOUTSIDETURN);
				}
				//now we have the speed adjustment, we can set the speed of the motors.
				//we need to make sure that, if our adjustment is greater than our regular speed (motorStraight),
				//we spin one of the wheels backwards (this is the secret of our algorithm). 
				
				//NOTE that speedadjustment can be negative OR positive, depending on which way we want to spin
				//this is a change from the original code.
				
				//We check to see if our speedAdjustment was VERY Negative, and the new speed is a negative speed.
				if(motorStraight + speedAdjustment < 0){
					//if so, we set our motors to spin BACKWARDS!
					//we divide each speed by two because we don't want to go too fast when going backwards.
					int rightMotorBackwardsSpeed = (-1)*(speedAdjustment/2);
					//then we set the speeds of the motors, and put the right motor backwards, left forwards.
					rightMotor.setSpeed(rightMotorBackwardsSpeed);
					rightMotor.backward();
					leftMotor.setSpeed((motorStraight - speedAdjustment)/2);
					leftMotor.forward();
				}else{ //if our speedadjustment does not take us to a negative speed for the right wheel,
					//we just add and subtract it regularly. 
					rightMotor.setSpeed(motorStraight + speedAdjustment);
					rightMotor.forward();
					leftMotor.setSpeed(motorStraight - speedAdjustment);
					leftMotor.forward();
				}
				
				
			}else{ //rotate if we see a block in front..
				
				while(sidePoller.getUsData() > 15){
					leftMotor.setSpeed(100);
					rightMotor.setSpeed(100);
					leftMotor.forward();
					rightMotor.backward();
				}
			}
			
			//The end condition is if we are close to the end than the block is (+ a buffer amount)
			double xRobotDiff = odo.getX() - endX;
			double yRobotDiff = odo.getY() - endY;
			double xBlockDiff = blockX - endX;
			double yBlockDiff = blockY - endY;
			double robotDist = Math.sqrt(Math.pow(xRobotDiff,2) + Math.pow(yRobotDiff,2));
			double blockDist = Math.sqrt(Math.pow(xBlockDiff,2) + Math.pow(yBlockDiff,2));
			//end condition
			if(robotDist < blockDist-15){
				Sound.beep();
				break;
			}
			
		}
	}
	
	/**
	 * Calculates the correction for the robot's position based on how far it is 
	 * away from where it wants to be. If the robot is K away from where it should be,
	 * it will adjust the motors'speeds by K*(proportional constant), in opposite directions
	 * @param dist The distance the robot is from where it should be
	 * @param propC The proportionalality constant
	 * @return The amount to correct the motors' rotational speeds
	 */
	private int getWheelRotationCorrection(double dist, double propC){
		int correction; //the speed adjustment, or correction
		
		//simple formula for the correction
		correction = (int) (propC* dist); 
		
		//if our correction is greater than the max, we set it to the max
		if (correction >= MAXCORRECTION){ //
			correction = MAXCORRECTION;
		}
		//if our correction is VERY NEGATIVE (it wants to set the motor to spin more backwards than 
		//the default forwards rate, we just set the correction to spin us backwards at the forwards.
		if (correction < (-2)*motorStraight){
			correction = - 2*motorStraight;
		}
		return correction; //this could now be positive or negative
	}
}
