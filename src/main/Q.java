/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package main;

/**
 *
 * @author agungf
 */
public class Q {
   String n;
   boolean valueSet = false;
   synchronized String get() {
      if(!valueSet)
      try {
         wait();
      } catch(InterruptedException e) {
         System.out.println("InterruptedException caught");
      }
      System.out.print(", Got: " + n);
      valueSet = false;
      notify();
      return n;
   }

   synchronized void put(String n) {
      /*
      if(valueSet)      
      try {
         wait();
      } catch(InterruptedException e) {
         System.out.println("InterruptedException caught");
      } 
      * 
      */
      this.n = n;
      valueSet = true;
      System.out.print(", Put: " + n);
      notify();
   }
}
