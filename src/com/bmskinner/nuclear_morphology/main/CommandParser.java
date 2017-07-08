package com.bmskinner.nuclear_morphology.main;

import java.util.Scanner;

import com.bmskinner.nuclear_morphology.logging.Loggable;

public class CommandParser implements Loggable {
	
	Scanner sc = new Scanner(System.in);
	
	public CommandParser(){
		
		while(true){
			if(sc.hasNextLine()){
				execute(sc.nextLine());
			}
		}
		
	}

	private void execute(String s){
		log(s);
	}

}
