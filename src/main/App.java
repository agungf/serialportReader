package main;

public class App {   
    private static String port, ipPort;
    private static boolean debug; 
    public static void main(String args[]) {
      if(args.length == 0){
          port = "/dev/ttyUSB0";
          ipPort = "127.0.0.1:9000";
          debug = false;
      } 
      else if(args.length == 3){ 
          port = args[0];
          ipPort = args[1];
          debug = args[2].endsWith("1")?true:false;
      } 
      else if(args.length == 2){
          port = args[0];
          ipPort = args[1];
      }
      else if(args.length == 1)
          port = args[0];
      else{
        System.out.println("USAGE [sudo] java -jar serialComm.jar [<serialPort> <activeMQ Server IP> <DEBUG=0>]");
        System.out.println("EXAMPLE [sudo] java -jar serialComm.jar COM1 (WINDOWS)");
        System.out.println("EXAMPLE [sudo] java -jar serialComm.jar /dev/ttyUSB0 110.138.225.218 1(LINUX)");
        System.exit(0);
      }      
              
      //System.out.println(args.length+" listening on port: "+port);
      //System.out.println(args.length+" ActiveMQ IP: "+ip);
      Q q = new Q();
      SerialPortReader producer = new SerialPortReader(q, port, debug);           
      new Thread(producer,"producer").start();      
      //amqpSender consumer = new amqpSender(q);
      JmsSender consumer = new JmsSender(q,ipPort);
      new Thread(consumer, "consumer").start();
      System.out.println("Press Control-C to stop.");
   }    
}