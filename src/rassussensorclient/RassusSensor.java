/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package rassussensorclient;

import java.util.Scanner;


/**
 *
 * @author adrianzgaljic
 */
public class RassusSensor {

    /**
     * @param args the command line arguments
     */
    public static void main(String[] args) {
        
        InputReader inputreader = new InputReader();
        inputreader.startListening();
    }
 
}
