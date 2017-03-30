package com.capgemini.ftp.client.util;

/**
 * The responsibility of this utility class is to provide the time differences. 
 * 
 * @author CG14265
 *
 */
public class StopWatch {
	
	private long startTime;
	
	private long endTime;
	
	
	public StopWatch(){
	}
	
	
	/**
	 * The goal of this method is to set the start time on the StopWatch
	 */
	public void start(){
		startTime = System.currentTimeMillis();
	}
	
	
	/**
	 * The goal of this method is to set the end time on the StopWatch
	 */
	public void end(){
		endTime = System.currentTimeMillis();
	}

	
	/**
	 * The goal of this method is to provide the time difference between start and end times.
	 * 
	 * @return
	 *  time difference in milliseconds.
	 */
	public long timeTaken(){
		return (endTime - startTime);
	}
}
