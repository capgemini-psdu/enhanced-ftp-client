package com.capgemini.ftp.client;

import java.util.Stack;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.AppenderBase;

public class TestAppender extends AppenderBase<ILoggingEvent> {

	private Stack<ILoggingEvent> events = new Stack<ILoggingEvent>();
	
	@Override
	protected void append(ILoggingEvent event) {
		if(event.getLevel() == Level.ERROR) {
			events.add(event);
		}
	}
	
	public void clear() {
		events.clear();
	}
	
	public ILoggingEvent getLastEvent() {
		return events.pop();
	}
}
