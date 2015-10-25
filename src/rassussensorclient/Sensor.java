/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rassussensorclient;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.DatagramSocket;
import java.net.ServerSocket;
import java.net.Socket;
import java.util.Random;
import java.util.Scanner;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.me.dz1.Measurement;
import org.me.dz1.UserAddress;

/**
 *
 * @author adrianzgaljic
 */
public class Sensor extends Thread{

    private int port; // server port
    final static int MIN_PORT_NUMBER = 1025;
    final static int MAX_PORT_NUMBER =  65534;
    final static String IPaddress = "localhost";
    private String username; // sensor ID
    private double latitude; // sensor location latitude
    private double longitude; // sensor location longitude
    private long startTime; //sensor start time 
    


    @Override
    public void run(){
        
        init();
        
        boolean registration = register(username, latitude, longitude, IPaddress, port);
        if (registration){
            System.out.println("Sensor with ID="+this.username+" successfully registered");
        } else {
            System.out.println("Sensor with ID="+this.username+" failed to register");
        }
        
      
        try {
            startListening();
        } catch (IOException ex) {
            Logger.getLogger(Sensor.class.getName()).log(Level.SEVERE, null, ex);
        }
        
    }

    public void init(){
        startTime = System.currentTimeMillis();
        Random random = new Random();
        int randInt = random.nextInt(13);
        this.longitude = (double)randInt/100+ 15.87 ;
        randInt = random.nextInt(10);
        this.latitude = (double)randInt/100 + 45.75;
        this.username = UUID.randomUUID().toString().substring(0, 5);
        this.port = choosePort();
        System.out.println("\n**************************************************************************");
        System.out.println("Sensor with ID="+this.username+" started at lat="+this.latitude+", long="+this.longitude+" and port="+this.port);
    }
    
    public int choosePort(){
        Random random = new Random();
        while(true){
            port = random.nextInt(64509)+1025;
            if (available(port)){
                break;
            }
            
        }
        return port;
    }
    
    public static boolean available(int port) {
        if (port < MIN_PORT_NUMBER || port > MAX_PORT_NUMBER) {
            throw new IllegalArgumentException("Invalid start port: " + port);
        }

        ServerSocket ss = null;
        DatagramSocket ds = null;
        try {
            ss = new ServerSocket(port);
            ss.setReuseAddress(true);
            ds = new DatagramSocket(port);
            ds.setReuseAddress(true);
            return true;
        } catch (IOException e) {
        } finally {
            if (ds != null) {
                ds.close();
            }

            if (ss != null) {
                try {
                    ss.close();
                } catch (IOException e) {
                    /* should not be thrown */
                }
            }
        }

        return false;
    }
    
    public void doTheMeasuring(){
        Measurement myMeasurment = getmMeasurement();
        Measurement averageMeasurment = null;
        
        UserAddress neighbour = null;
        neighbour = searchNeighbour(username);
        if (neighbour==null){
            averageMeasurment = myMeasurment;
            System.out.println("["+this.username+"] There are no other sensors");
        } else {
            try {
                System.out.println("["+this.username+"] Closest neighbour "+neighbour.toString());
                String neighbourMeasurmentStr = getNeighbourMeasurment(neighbour.getPort());
                Measurement neighbourMeasurment = parseMeasurment(neighbourMeasurmentStr);
                averageMeasurment = calculateAverageMeasurment(myMeasurment,neighbourMeasurment);
                
            } catch (IOException e) {
                Logger.getLogger(Sensor.class.getName()).log(Level.SEVERE, null, e);
            }
            
        }
        System.out.println("["+this.username+"] Average measurment= "+averageMeasurment.toString());
        storeMeasurments(averageMeasurment);
    }
    
    public Measurement getmMeasurement(){
        try {
            
            long currentTime = System.currentTimeMillis();
            int elapsedTime = (int)(currentTime-startTime)/1000;
            System.out.println("["+this.username+"] Measurment started "+elapsedTime+" seconds after server startup");
            int lineNumber = (elapsedTime%100)+2;
            
            File file = new File("mjerenja.csv");
            Scanner in = new Scanner(file);
            //first line is Temperature,Pressure,Humidity,CO,NO2,SO2,
            int i=1;
            
            String line = "";
            while (in.hasNextLine()){
                in.nextLine();
                if (lineNumber==(i+=1)){
                     line = in.nextLine();
                }
            }
            String[] measurmentParts = line.split(",",-1);
            Measurement measurement = new Measurement(measurmentParts[0],
                    measurmentParts[1],
                    measurmentParts[2],
                    measurmentParts[3],
                    measurmentParts[4],
                    measurmentParts[5]);
            System.out.println("["+this.username+"] Sensor with ID="+this.username+" measurments are:"+measurement.toString());
            return measurement;
        } catch (FileNotFoundException ex) {
            Logger.getLogger(Sensor.class.getName()).log(Level.SEVERE, null, ex);
            return null;
        }
    }



    private String getNeighbourMeasurment(int userPort) throws IOException {
        String serverName = "localhost"; // server name        
        System.out.println("["+this.username+"] Trying conncet to "+userPort);
        Socket clientSocket = new Socket(serverName, userPort);//SOCKET->CONNECT

        // get the socket's output stream and open a PrintWriter on it
        PrintWriter outToServer = new PrintWriter(clientSocket.getOutputStream(), true);

        // get the socket's input stream and open a BufferedReader on it
        BufferedReader inFromServer = new BufferedReader(new InputStreamReader(
                clientSocket.getInputStream()));

        outToServer.println("get");
        String rcvString = inFromServer.readLine();
        clientSocket.close();
        return rcvString;
    }
    
    
    
    

    

    private Measurement parseMeasurment(String neighbourMeasurmentStr) {
        String[] measurmentParts = neighbourMeasurmentStr.split(",",-1);
        Measurement measurement = new Measurement(measurmentParts[0],
                    measurmentParts[1],
                    measurmentParts[2],
                    measurmentParts[3],
                    measurmentParts[4],
                    measurmentParts[5]);
        return measurement;
    }

    private Measurement calculateAverageMeasurment(Measurement myMeasurment, Measurement neighbourMeasurment) {
        String temperature;
        String pressure;
        String humidity;
        String co;
        String no2;
        String so2;
        
        double temperatureDouble = (Double.parseDouble(myMeasurment.getTemperature())+   
                    Double.parseDouble(neighbourMeasurment.getTemperature()))/2;
        temperature = Double.toString(temperatureDouble);
        
        double pressuereDouble = (Double.parseDouble(myMeasurment.getPressure())+   
                    Double.parseDouble(neighbourMeasurment.getPressure()))/2;
        pressure = Double.toString(pressuereDouble);
        
        double humidityDouble = (Double.parseDouble(myMeasurment.getHumidity())+   
                    Double.parseDouble(neighbourMeasurment.getHumidity()))/2;
        humidity = Double.toString(humidityDouble);
        
        double coDouble = (Double.parseDouble(myMeasurment.getCo())+   
                    Double.parseDouble(neighbourMeasurment.getCo()))/2;
        co = Double.toString(coDouble);
        
        if (myMeasurment.getN2().equals("") || neighbourMeasurment.getN2().equals("")){
            no2 = myMeasurment.getN2()+neighbourMeasurment.getN2();
        } else {
            double no2Double = (Double.parseDouble(myMeasurment.getN2())+   
                    Double.parseDouble(neighbourMeasurment.getN2()))/2;
            no2 = Double.toString(no2Double);
        }
        
        if (myMeasurment.getSo2().equals("") || neighbourMeasurment.getSo2().equals("")){
            so2 = myMeasurment.getSo2()+neighbourMeasurment.getSo2();
        } else {
            double so2Double = (Double.parseDouble(myMeasurment.getSo2())+   
                    Double.parseDouble(neighbourMeasurment.getSo2()))/2;
            so2 = Double.toString(so2Double);
        }
    
        return new  Measurement(temperature, pressure, humidity, co, no2, so2);
    }
    

    private void storeMeasurments(Measurement averageMeasurment) {
        
        String temperature = averageMeasurment.getTemperature();
        float tamperatureFloat = Float.parseFloat(temperature);
        storeMeasurment(this.username, "temperature", tamperatureFloat);
        
        String pressure = averageMeasurment.getPressure();
        float pressureFloat = Float.parseFloat(pressure);
        storeMeasurment(this.username, "pressure", pressureFloat);
        
        String humidity = averageMeasurment.getHumidity();
        float humidityFloat = Float.parseFloat(humidity);
        storeMeasurment(this.username, "humidity", humidityFloat);
        
        String co = averageMeasurment.getCo();
        float coFloat = Float.parseFloat(co);
        storeMeasurment(this.username, "co", coFloat);
        
        String so2 = averageMeasurment.getSo2();
        float so2Float;
        if (so2.equals("")){
            so2Float = 0;
        } else {
            so2Float = Float.parseFloat(so2);
        }
        storeMeasurment(this.username, "so2", so2Float);
        
        String no2 = averageMeasurment.getN2();
        float no2Float;
        if (no2.equals("")){
            no2Float = 0;
        } else {
            no2Float = Float.parseFloat(no2);
        }
        storeMeasurment(this.username, "no2", no2Float);
        
        System.out.println("["+this.username+"] Measurments stored to server");
    }
    
    private void startListening() throws IOException {
        System.out.println("["+this.username+"] listening on port " + this.port);
      
        String rcvStr = null; // received String

        // create a server socket and bind it to the specified port on the local
        // host
        ServerSocket serverSocket = new ServerSocket(this.port);//SOCKET->BIND->LISTEN
        while (true) {
            
            // create a new socket, accept and listen for a connection to be
            // made to this socket
            Socket socket = serverSocket.accept();//ACCEPT

            // create a new PrintWriter from an existing OutputStream
            PrintWriter outToClient = new PrintWriter(socket.getOutputStream(),
                    true);

            // create a buffering character-input stream that uses a
            // default-sized input buffer
            BufferedReader inFromClient = new BufferedReader(
                    new InputStreamReader(socket.getInputStream()));

            // read few lines of text
            while ((rcvStr = inFromClient.readLine()) != null) {
                System.out.println("["+this.username+"] Sensor received: " + rcvStr);

                //shutdown if requested
                if (rcvStr.contains("shutdown")) {
                    return;
                } else if(rcvStr.equals("get")){
                    outToClient.println(getmMeasurement().toString());
                } 
            }

            outToClient.close();
            inFromClient.close();
            socket.close();//CLOSE
                    
        }
    }
    
    public void end(){
        Thread.currentThread().interrupt();
    }
    
    private static boolean register(java.lang.String username, double latitude, double longitude, java.lang.String iPaddress, int portNumber) {
        org.me.dz1.Server_Service service = new org.me.dz1.Server_Service();
        org.me.dz1.Server port = service.getServerPort();
        return port.register(username, latitude, longitude, iPaddress, portNumber);
    }

    private static UserAddress searchNeighbour(java.lang.String username) {
        org.me.dz1.Server_Service service = new org.me.dz1.Server_Service();
        org.me.dz1.Server port = service.getServerPort();
        return port.searchNeighbour(username);
    }

    private static boolean storeMeasurment(java.lang.String username, java.lang.String parameter, float averageValue) {
        org.me.dz1.Server_Service service = new org.me.dz1.Server_Service();
        org.me.dz1.Server port = service.getServerPort();
        return port.storeMeasurment(username, parameter, averageValue);
    }
    
    public double getLatitude(){
        return this.latitude;
    }

    public  double getLongitude(){
        return  this.longitude;
    }
    
    public String getUsername(){
        return this.username;
    }
    
    

    

   

   
}
