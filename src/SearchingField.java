
import lejos.hardware.Sound;
import lejos.hardware.motor.EV3LargeRegulatedMotor;


public class SearchingField extends Thread {
	
	private Odometer odo;
	private Navigation navi;
	private BlockDetector detector;
	private UltrasonicPoller frontPoller;
	private UltrasonicPoller sidePoller;
	private EV3LargeRegulatedMotor leftMotor;
	private EV3LargeRegulatedMotor rightMotor;
	
	private double[] pos = new double [3];
	private boolean isFlag; //determines if flag is found
	private double b = 6; 
	private double a = 3;
	private double x;
	private double y;
	
	//constructor 
	public SearchingField( EV3LargeRegulatedMotor leftMotor, EV3LargeRegulatedMotor rightMotor,
			UltrasonicPoller sidePoller,UltrasonicPoller frontPoller, Navigation navi, Odometer odo,BlockDetector detector, double zoneX, double zoneY)

	{
        this.odo = odo;
        this.navi= navi;
        this.leftMotor = leftMotor;
        this.rightMotor = rightMotor;
        this.detector= detector;
        this.frontPoller = frontPoller;
        this.sidePoller = sidePoller;
        this.x = zoneX;
        this.y = zoneY;
	}
	/**
	 * 
	 */
	public void run()
	{
		//turn off front sensor
		frontPoller.disableSensor();
		
		isFlag = detector.isFlag(); //false
		
		rightMotor.setSpeed(150);
		leftMotor.setSpeed(150);

		//travel side to upper right corner

		while(odo.getY() >y*30.4 - (3*30.4+a+b))
		{	
			if(sidePoller.getUsData() < (b+30.4))//check for objects in first 1x3 section
			{
				checkObject();
				if (isFlag)
				{
					break;
				}
			}
			leftMotor.setSpeed(150);
			rightMotor.setSpeed(150);
			rightMotor.forward();
			leftMotor.forward();

		}
		
		if(!isFlag)//didnt find flag in first side
		{
			//turn 90 degrees ccw
			rightMotor.rotate(convertAngle(Controller.WHEEL_RADIUS,Controller.TRACK,85), true);
			leftMotor.rotate(convertAngle(Controller.WHEEL_RADIUS,Controller.TRACK,-85), false);
				
		
			//navi.travelTo(x*30.4 + (b+2*30.4), odo.getY());	
			//travel side 
				while(odo.getX() < x*30.4+ (b+2*30.5)) 
				{
	//				if(sidePoller.getUsData() < (b+30.4))//object in front 1x2
	//				{
	//					checkObject();
	//					if (isFlag)
	//					{
	//						break;
	//					}
	//				}
					leftMotor.setSpeed(150);
					rightMotor.setSpeed(150);
					rightMotor.forward();
					leftMotor.forward();
	
				}
			if (!isFlag)
			{
				//turn 90 degree ccw
				rightMotor.rotate(convertAngle(Controller.WHEEL_RADIUS,Controller.TRACK,85), true);
				leftMotor.rotate(convertAngle(Controller.WHEEL_RADIUS,Controller.TRACK,-85), false);
			while (odo.getY() < y*30.4) //object in second 1x3 area
			{
				if(sidePoller.getUsData()< (b+30.4))
				{
					checkObject();
					if (isFlag)
					{
						break;
					}
				}
				leftMotor.setSpeed(150);
				rightMotor.setSpeed(150);
				rightMotor.forward();
				leftMotor.forward();
			}
			leftMotor.stop(true);
			rightMotor.stop(true);
		}
		
		
	}
}
	/**
	 * 
	 */
	public void checkObject()
	{
		Sound.beep();
		//move forward 7cm to approximately get to the middle of block 
		rightMotor.rotate(convertDistance(Controller.WHEEL_RADIUS,9), true);
		leftMotor.rotate(convertDistance(Controller.WHEEL_RADIUS,9), false);

		//record initial position
		odo.getPosition(pos);
		
		//turn 90 degrees ccw
//		rightMotor.rotate(convertAngle(Controller.WHEEL_RADIUS,Controller.TRACK,85), true);
//		leftMotor.rotate(convertAngle(Controller.WHEEL_RADIUS,Controller.TRACK,-85), false);
		
		//turn off side sensor and turn on front sensor
		sidePoller.disableSensor();
		frontPoller.enableSensor();
		
		//investigate the block
		detector.investigateBlock();
		//Sound.buzz();
		isFlag = detector.isFlag();

		if (isFlag)
		{
			Sound.buzz();   
			return;
		}
		else{
		//turn off front sensor and turn on side sensor
		frontPoller.disableSensor();
		sidePoller.enableSensor();
		
		leftMotor.setSpeed(100);
		rightMotor.setSpeed(100);
		
		//go back to original position
		navi.turnTo(pos[2], true);
//		//option 1; problem: blocks right next to each other
		while (sidePoller.getUsData() <= (b+30.4)) //travel into cant see block anymore
			{
				leftMotor.setSpeed(100);
				rightMotor.setSpeed(100);
				rightMotor.forward();
				leftMotor.forward();
			}
		try {
			Thread.sleep(300);
		} catch (InterruptedException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		Sound.beep();
		rightMotor.rotate(convertDistance(Controller.WHEEL_RADIUS,8), true);
		leftMotor.rotate(convertDistance(Controller.WHEEL_RADIUS,8), false);
		Sound.beep();
		
		//option 2; problem: hard coding too many variants and can skip block too 
		//skip the block just checked
//		Sound.beep();
//		Sound.beep();
//			rightMotor.rotate(convertDistance(WHEEL_RADIUS,80), true);
//			leftMotor.rotate(convertDistance(WHEEL_RADIUS,80), false);
//		Sound.buzz();

		
		}
	}
	private static int convertDistance(double radius, double distance) {
        return (int) ((180.0 * distance) / (Math.PI * radius));
    }
    
    /**
     * This method converts an angle to distance in cm
     * @param radius the wheel radius
     * @param width the bandwidth of the two wheels 
     * @param angle desired angle to convert
     * @return result of method convertDistanc() distance in cm
     */
    private static int convertAngle(double radius, double width, double angle) {
        return convertDistance(radius, Math.PI * width * angle / 360.0);
    }
	
//	private float getFilteredUSData() {
//        float distance = frontPoller.getUsData();
//        float result = 0;
//        if (distance > 200){
//            // true 255, therefore set distance to 255
//            result = 200; //clips it at 50
//        } else {
//            // distance went below 255, therefore reset everything.
//            result = distance;
//        }
//        //lastDistance = distance;
//        return result;
//    }
//	public boolean isReadingBlock(){
//		synchronized (this) {
//			return isReadingBlock;	
//		}
//	}

}
