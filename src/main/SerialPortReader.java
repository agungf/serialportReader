/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package main;
import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.logging.Level;
import java.util.logging.Logger;
import jssc.SerialPort;
import jssc.SerialPortEvent;
import jssc.SerialPortEventListener;
import jssc.SerialPortException;



/**
 *
 * @author agungf
 */
/*
     * In this class must implement the method serialEvent, through it we learn about 
     * events that happened to our port. But we will not report on all events but only 
     * those that we put in the mask. In this case the arrival of the data and change the 
     * status lines CTS and DSR
     */
class SerialPortReader implements Runnable {
   public static SerialPort serialPort;   
   public boolean dataTerminalReady = false;
   public boolean dataSetReady = false;
   boolean firstRun = true;
   Q q;
   String port;
   Boolean debug;
   SerialPortReader(Q q, String port, Boolean debug) {
      this.q = q;
      this.port = port;
      this.debug = debug;
      //new Thread(this, "Producer").start();
   }
   
   @Override
   public void run() {
        System.out.println("Producer run on serialPort "+port);
        //String port="COM1";        

        serialPort = new SerialPort(port); 
        try {
            serialPort.openPort();//Open port
            serialPort.setParams(2400, 7, 1, 1, true, true);//.setParams(9600, 8, 1, 0);//Set params
            int mask = SerialPort.MASK_RXCHAR + SerialPort.MASK_CTS + SerialPort.MASK_DSR;// + SerialPort.MASK_BREAK;//Prepare mask
            serialPort.setEventsMask(mask);//Set mask
            serialPort.addEventListener(new SerialPortListner());//Add SerialPortEventListener
        }
        catch (SerialPortException ex) {
            System.out.println(ex);
        }                  
   }
   
   private class SerialPortListner implements SerialPortEventListener {
        private String weight;
        private Integer lastWeightInt = 0, weightInt = 0, highestWeight=0;
        private Integer recordCounter=0, vehicleCounter=0;
        private Integer JBI1 = 2540;
        private Integer JBI1UP = 5334;
        private Integer JBI2 = 7500;
        private Integer JBI2UP = 15750;
        private Integer JBI3 = 21000;
        private Integer JBI3UP = 28000;
        private Integer JBI14K = 2*14000;
        private Integer JBI14KUP = 36000;
        private Integer JBI18K = 2*18000;
        private Integer JBI18KUP = 50000;
        DateFormat dateFormat = new SimpleDateFormat("yyyy-MM-ddZHH:mm:ss");
        Date date = new Date();                                
        Boolean newline;
        
        @Override
        public void serialEvent(SerialPortEvent event) {
            //if(!App.getInstance().dataTerminalReady){
             //   App.getInstance().dataTerminalReady = App.getInstance().jmsConnect();
            //}System.out.println("newline?"+newline);            
            newline = newline == null?true:newline;            
            if(event.isRXCHAR()){
                try {
                    //If data is available//String newline = new String(newLineBuffer);
                    if(!newline || firstRun){
                        if(event.getEventValue() == 16){//Check bytes count in the input buffer
                            //Read data, if 18 bytes available 
                            byte buffer[] = serialPort.readBytes(16);                        
                            if(debug || firstRun){
                                System.out.println(dateFormat.format(date)+" First test data: "+new String(buffer));                            
                                System.out.println("dateTime, RecordNumber, vehicleCounter, currentWeight, highestWeight, synchronizedPutData, synchronizedGetData, jmsMessegeSent");
                            }                                                                                                      
                            vehicleCounter(new String(buffer));
                            //vehicleCounter(serialPort.readString(8));
                            newline = true;
                            firstRun = false;
                        }                                                
                    }
                    else{        
                        if(event.getEventValue() == 2){
                            byte newLineBuffer[] = serialPort.readBytes(2);                    
                            String emptyString = new String(newLineBuffer);
                            if(debug)
                                System.out.println(dateFormat.format(date)+" data: "+newLineBuffer+":"+emptyString+":"+emptyString.equals("\r\n")+":"+newline);                                                    
                            if(emptyString.equals("\r\n"))
                                newline = false;
                         }                        
                    }                        
                } 
                catch (SerialPortException ex) {
                        Logger.getLogger(SerialPortReader.class.getName()).log(Level.SEVERE, null, ex);
                }
            }
            else if(event.isCTS()){//If CTS line has changed state
                if(event.getEventValue() == 1){//If line is ON
                    System.out.println("CTS - ON");
                    dataTerminalReady = true;
                }
                else {
                    System.out.println("CTS - OFF");
                    dataTerminalReady = false;
                }
            }
            else if(event.isDSR()){///If DSR line has changed state
                if(event.getEventValue() == 1){//If line is ON
                    System.out.println("DSR - ON");
                    dataSetReady = true;
                    
                }
                else {
                    System.out.println("DSR - OFF");
                    dataSetReady = false;
                }
            }
            /*
            else if(event.isBREAK()){
                System.out.println("BREAK");
            }
            * 
            */
        }
        
        private void vehicleCounter(String data){       
            date = new Date();
            if(data.length() > 10){                                                        
                try{
                    weight = data.substring(8, 14);                  
                    lastWeightInt=weightInt;
                    weightInt=Integer.parseInt(weight,10);                                                                               
                }
                catch (Exception ex) {
                    System.out.println(ex);
                    
                    
                }                                       
                if (weightInt != lastWeightInt){//ignore double zeros                        
                        recordCounter++;                                            
                        //else  weight = weight.substring(weight.length()-4, weight.length()-1);                        
                        if (weightInt == 0){ // save the highest number                        
                            vehicleCounter++;
                            System.out.print(dateFormat.format(date)+", "+recordCounter+", "+vehicleCounter+", "+weightInt+", "+highestWeight);//+", Raw :"+data);
                            q.put(weightInt.toString());
                            // ignore highest weight below 500kg (bikes, humans etc. vehicle is more than that)
                            if(highestWeight > 500){
                                highestWeight=0;                                
                            }                                                                                    
                        }
                        if (weightInt > lastWeightInt){//get up number
                            System.out.print(dateFormat.format(date)+", "+recordCounter+", "+vehicleCounter+", "+weightInt+", "+highestWeight);//+", Raw :"+data);                            
                            if(weightInt > highestWeight){
                                highestWeight = weightInt;
                                q.put(weightInt.toString());
                            }
                            else
                                System.out.println();
                            if(lastWeightInt==0)//new vehicle, reset number and statuses
                                highestWeight = 0;                     
                        }
                        if (weightInt < lastWeightInt){//get down number
                        }
                        else{//it is flat
                        }
                }   
            }        
        }
   }
}

