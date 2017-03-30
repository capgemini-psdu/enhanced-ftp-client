package com.capgemini;

public interface Cacheable<T> {

	String getId();
	
	boolean isExpired();
	
	Object getEntity();
}