package com.capgemini;

import java.util.HashMap;
import java.util.Map;

public final class CacheManager {

	private static final Map<String, Cacheable<?>> cache = new HashMap<String, Cacheable<?>>();
	
	public static void cache(Cacheable<?> cacheable) {
		
		cache.put(cacheable.getId(), cacheable);
	}
	
	public static Cacheable<?> retrieve(String id) {
		
		Cacheable<?> cacheable = cache.get(id);
		if(cacheable != null && cacheable.isExpired()) {
			cache.remove(id);
			return null;
		}
		
		return cacheable;
	}
}