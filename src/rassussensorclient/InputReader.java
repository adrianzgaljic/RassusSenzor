/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rassussensorclient;

import com.sun.org.apache.xerces.internal.xs.XSConstants;
import java.util.ArrayList;
import java.util.Scanner;

/**
 *
 * @author adrianzgaljic
 */
public class InputReader {
    
    private String input;
    private ArrayList<Sensor> listOfSensors = new ArrayList<>();
    
    
    public void startListening(){
        Scanner reader = new Scanner(System.in);  // Reading from System.in
        String input;
        while(true){
            System.out.println("Type \"start\" to start new sensor, \"measure sensorID\" to start measuring: ");
            input = reader.nextLine();
                    
            if (input.equals("end")){
                break;
            } 
            if (input.equals("start")){
                 Sensor sensor = new Sensor();
                 listOfSensors.add(sensor);
                 sensor.start();
                 
            }
            if (input.startsWith("measure")){
                String sensorId = input.split("\\s")[1];
                
                for (Sensor sensor:listOfSensors){
                    if (sensorId.equals(sensor.getUsername())){
                        sensor.doTheMeasuring();
                    }
                }
            }
            
        }
    }
    
}
