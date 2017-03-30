package com.capgemini;

import java.security.KeyStore;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;

public class CachedKeyStore implements Cacheable<KeyStore> {

	private final String id;
	private final KeyStore entity;
	private final Date expiry;
	
	public CachedKeyStore(String id, KeyStore entity, int expiresInMilliseconds) {
		
		this.id = id;
		this.entity = entity;
		
		if(expiresInMilliseconds == -1) {
			this.expiry = null;
		} else {
			Calendar cal = GregorianCalendar.getInstance();
			cal.add(Calendar.MILLISECOND, expiresInMilliseconds);
			this.expiry = cal.getTime();
		}
	}
		
	@Override
	public String getId() {
		return id;
	}

	@Override
	public boolean isExpired() {		
		return expiry != null && expiry.after(new Date());
	}

	@Override
	public KeyStore getEntity() {
		return entity;
	}
}