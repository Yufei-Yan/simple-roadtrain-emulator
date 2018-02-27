package edu.auburn.comp6360_vehicles;

/**
 *
 * @author Yufei Yan (yzy0050@auburn.edu)
 */
public class FollowingVehicle extends Vehicle {

  public FollowingVehicle() {
    super();
  }
  
  public FollowingVehicle(String addr,
          Gps initGps,
          double initVel,
          double initAcc,
          VSize initSize,
          int initNode) {
    super(addr, initGps, initVel, initAcc, initSize, initNode);
  }
  
  /**
   * Send the request for forming the road train
   * 
   * @return 
   */
  public int formRequest() {
    return 0;
  }
  
  /**
   * Send the request for leaving the road train
   * 
   * @return 
   */
  public int leaveRequest() {
    return 0;
  }
}