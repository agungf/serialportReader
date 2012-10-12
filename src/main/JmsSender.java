/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

import javax.jms.*;
import org.apache.activemq.ActiveMQConnectionFactory;

/**
 *
 * @author agungf
 */
public class JmsSender implements Runnable {
    Q q;
    String ipPort;
    JmsSender(Q q, String ipPort) {
       this.q = q;
       this.ipPort = ipPort;
       //new Thread(this, "Consumer").start();
    }
    private Destination topicDestination;

    /**
     * Send message.
     *
     * @param session the session
     * @param producer the producer
     * @throws Exception the exception
     */
    protected void sendMessage(Session session, MessageProducer producer) throws Exception {
    	String msg=q.get();
    	TextMessage message = session.createTextMessage(msg);
    	//session.createObjectMessage();
    	System.out.println(", [x] Sent '" + msg);
        producer.send(message);
    }

    @Override
    public void run() {                           
        Connection connection = null;
        try {
            String url="tcp://"+ipPort;//61616";
            String subject="timbangan";            
            // Create the connection.
            ActiveMQConnectionFactory connectionFactory = new ActiveMQConnectionFactory(url);
            connection = connectionFactory.createConnection();
            connection.start();
            System.out.println("Connected to activeMQ server at: " + url);
            // Create the session
            Session session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
            //javax.jms.Destination destination = session.createQueue(subject);
            
            if(topicDestination == null) {
            	topicDestination = session.createTopic(subject);
            }
            
            // Create the producer.
            MessageProducer producer = session.createProducer(topicDestination);
            producer.setDeliveryMode(DeliveryMode.PERSISTENT);
            // Start sending messages   
            while(true){
                sendMessage(session, producer);
                //System.out.println(" Done.");
            }
            // Use the ActiveMQConnection interface to dump the connection
            //ActiveMQConnection c = (ActiveMQConnection)connection;
            //c.getConnectionStats().dump(new IndentPrinter());
        } catch (Exception e) {
            System.out.println("Caught: " + e);
        } finally {
            try {
                connection.close();
            } catch (Throwable ignore) {
            }
        }
    }
}