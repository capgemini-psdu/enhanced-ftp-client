package com.capgemini.ftp.client.config;

import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix="ftp.transfers")
public class FTPTransferProperties extends HashMap<String, Map<String, ?>> {

	private static final long serialVersionUID = 1L;
	
	public FTPClientConfig get(String key) {
		return new FTPClientConfig(super.get(key));
	}
	
}
