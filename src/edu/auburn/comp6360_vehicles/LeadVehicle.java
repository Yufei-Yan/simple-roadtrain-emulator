package edu.auburn.comp6360_vehicles;

import edu.auburn.com6360_utility.ConfigFileHandler;
import edu.auburn.com6360_utility.NetworkHandler;
import edu.auburn.com6360_utility.PacketHeader;
import edu.auburn.com6360_utility.RoadTrainHandler;
import edu.auburn.com6360_utility.VehicleParaHandler;
import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetAddress;
import java.net.SocketTimeoutException;
import java.util.Random;
import jdk.nashorn.internal.codegen.CompilerConstants;

/**
 *
 * @author Yufei Yan (yzy0050@auburn.edu)
 */
public class LeadVehicle extends Vehicle {
  
  public LeadVehicle() {
    super();
  }
  
  public LeadVehicle(byte[] addr,
                 Gps initGps,
                 double initVel,
                 double initAcc,
                 VSize initSize,
                 int initNode,
                 String initServerAddr) {
    super(addr, initGps, initVel, initAcc, initSize, initNode, initServerAddr);
  }
  
  @Override
  public void run() {
    System.out.println(this.address);
    System.out.println(this.gps.getLat());
    
    ConfigFileHandler configHandler = new ConfigFileHandler(filename);
    boolean ret = false;
    
    ret = configHandler.isFileExist(filename);
    if (!ret) {
      System.out.println(filename + " not exits");
      ret = configHandler.createConfigFile();
      if (!ret) {
        System.err.println("config file not created!\nProgram exits");
        System.exit(0);
      }
      
      System.out.println("New file created...");
      
      try {
        ret = configHandler.writeAll(this);
      } catch (IOException e) {
        System.err.println("Failed to write to config file.");
        e.printStackTrace();
      }
      
      if (!ret) {
        System.out.println("Failed to write to config file");
      } else {
        this.startServer();
      }
    } else {
      System.out.println(filename + " found!");
      ret = configHandler.removeConfigFile();
      
      if (ret) {
        System.out.println("Old config file removed.");
        ret = configHandler.createConfigFile();
        try {
          ret = configHandler.writeAll(this);
        } catch (IOException e) {
          System.err.println("Failed to write to config file.");
          e.printStackTrace();
        }
      } else {
        System.out.println("Old config file cannot be removed.");
        System.out.println("Please mannually remove the file and restart the program");
        System.exit(0);
      }

//      if (!ret) {
//        System.out.println("Failed to write to config file");
//      } else {
        this.startServer();
      //}
    }
  }
  
  @Override
  public void start() {
    System.out.println("Start lead vehicle server.");
    String threadName = "Lead";

    Thread t = new Thread(this, threadName);
    t.start();
  }
  
  /**
   * Obtain the acceleration for the Leading Vehicle (every 10 ms)
   * 
   * @param
   * @return a random number between -1 and 1
   */
  public void setAcc() {
    this.acceleration = Math.random() * 2 - 1;
  }
  
  
  private void startServer() {
    try {
      this.socketServer();
    } catch (Exception ex) {
      System.err.println(ex);
    }
  }
  
  @Override
  protected void socketServer() throws Exception {
    DatagramSocket server = new DatagramSocket(10121);
    PacketHeader clientHeader = null;
    FollowingVehicle fv = null;
    int serverSn = new Random().nextInt(100);
    int clientSn = 0;

    byte[] dataRx = new byte[1024];
    byte[] dataTx = new byte[1024];
    
    DatagramPacket sendPacket = null;
    InetAddress IPAddress = null;
    int port = 0;
    while(true) {
      DatagramPacket receivePacket = new DatagramPacket(dataRx, dataRx.length);
      
      server.setSoTimeout(timeout);
      
      try {
        server.receive(receivePacket);
        byte[] data = receivePacket.getData();
        clientHeader = new NetworkHandler().headerDessembler(data);
        clientSn = clientHeader.getSn();
        //System.out.println("lv client packet type: " + clientHeader.getType());
//        System.out.println("sn: " + clientHeader.getSn());
//        System.out.println("ip: " + clientHeader.getIp());
//        System.out.println("type: " + clientHeader.getType());
        
        fv = (FollowingVehicle)new NetworkHandler().payloadDessembler(data);
//        System.out.println("out of payloadDessembler");
//        System.out.println("fv.nodeNum: " + fv.getNodeNum());
//        System.out.println("fv.addr " + fv.getAddr());
//        System.out.println("fv.length " + fv.getLength());
        
      } catch (SocketTimeoutException e) {
        System.out.println("Update timeout.");
        this.update(timeout);
        System.out.println("No following vehicles detected.\nBeacon broadcasting\n");
        ++serverSn;
        continue;
      } catch (Exception e) {
        System.err.println(e);
      }
//      String sentence = new String(receivePacket.getData());
//      System.out.println("RECEIVED: " + sentence);
      
      IPAddress = receivePacket.getAddress();
      port = receivePacket.getPort();
      
      PacketHeader serverHeader = new PacketHeader(serverSn, this.getAddr(), PacketHeader.NORMAL);
      
      //deal with packet header type
      if (clientHeader.equals(null) || PacketHeader.NORMAL == clientHeader.getType()) {
        serverHeader = new PacketHeader(serverSn, this.getAddr(), PacketHeader.NORMAL);
      } else if (PacketHeader.FORM == clientHeader.getType()) {
        System.out.println("Following vehicle requested to JOIN Road Train.");
        serverHeader = new PacketHeader(serverSn, this.getAddr(), PacketHeader.ACCEPT);
        NetworkHandler.packetState = PacketHeader.NORMAL;
        RoadTrainHandler.roadTrainState = RoadTrainHandler.LEAVE;
      } else if (PacketHeader.LEAVE == clientHeader.getType()) {
        System.out.println("Following vehicle requested to LEAVE Road Train.");
        serverHeader = new PacketHeader(serverSn, this.getAddr(), PacketHeader.ACCEPT);
        NetworkHandler.packetState = PacketHeader.NORMAL;
        RoadTrainHandler.roadTrainState = RoadTrainHandler.FORM;
      } else if (PacketHeader.ACK == clientHeader.getType()) {
        System.out.println("Acknowledgement received.");
        
        if (RoadTrainHandler.roadTrainState == RoadTrainHandler.FORM) {
          System.out.println("Disable link.");
          this.setLink(0);
        } else 
        {
          System.out.println("Set up link.");
          this.setLink(fv.getNodeNum());
        }
        serverHeader = new PacketHeader(serverSn, this.getAddr(), PacketHeader.NORMAL);
      }
      
      //System.out.println("lv server packet type: " + serverHeader.getType());
      
      dataTx = new NetworkHandler().packetAssembler(serverHeader, this);
      sendPacket = new DatagramPacket(dataTx, dataTx.length, 
            IPAddress, port);
      
      server.send(sendPacket);
      ++serverSn;
      
      this.update(timeInterval);
      System.out.println("client SN:" + clientSn);
      System.out.println("server SN:" + serverSn);
      System.out.println();
    }
  }
}
