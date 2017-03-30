package com.capgemini.ftp.client.config;

import java.util.Map;

import org.springframework.util.CollectionUtils;

public class FTPClientConfig {

	public FTPClientConfig(Map<String, ?> map) {
		
		if (!CollectionUtils.isEmpty(map)) {
			this.ftpServerList = (String) map.get("ftpServerList");
			this.ftpPort = (Integer) map.get("ftpPort");
			this.ftpUser = (String) map.get("ftpUser");
			this.ftpPassword = (String) map.get("ftpPassword");
			this.ftpRetryCount = (Integer) map.get("ftpRetryCount");
			this.ftpRetrySleep = Long.valueOf(String.valueOf(map.get("ftpRetrySleep")));
			this.ftpFilesLocalWorkingDir = (String) map.get("ftpFilesLocalWorkingDir");
			this.ftpFilesRemoteWorkingDir = (String) map.get("ftpFilesRemoteWorkingDir");
			this.ftpInsecureMode = (Boolean) map.get("ftpInsecureMode");
			this.sslProtocol = (String) map.get("sslProtocol");					//SSLv3 -> 'SSL', TLSv1 -> 'TLS', TLSv1.1 -> 'TLSv1.1', TLSv1.2 -> 'TLSv1.2'
			this.xfrServiceHttpPort = map.get("xfrServiceHttpPort") == null ? 0 : (Integer) map.get("xfrServiceHttpPort");
			
			this.keyStoreLocation = (String) map.get("keyStoreLocation");		//file system location or CCS url (incl file name)
			this.keyStorePassword = (String) map.get("keyStorePassword");
			this.keyStoreCCS = (Boolean) map.get("keyStoreCCS");				//Is the keyStore in CCS
			
			this.keyManagerLocation = (String) map.get("keyManagerLocation");	//file system location or CCS url (incl file name)
			this.keyManagerPassword = (String) map.get("keyManagerPassword");
			this.keyManagerAlias = (String) map.get("keyManagerAlias");
			this.keyManagerKeyPassword = (String) map.get("keyManagerKeyPassword");
			this.keyManagerCCS = (Boolean) map.get("keyManagerCCS");			//Is the keyManager in CCS
			
			this.ccsUsername = (String) map.get("ccsUsername");
			this.ccsPassword = (String) map.get("ccsPassword");
			
			this.keyStoreCacheTimeToLive = (Integer) map.get("keyStoreCacheTimeToLive");
		}
	}

	public String ftpServerList;
	
	public int ftpPort;
	
	public String ftpUser;

	public String ftpPassword;

	public int ftpRetryCount;

	public long ftpRetrySleep;

	public String ftpFilesLocalWorkingDir;

	public String ftpFilesRemoteWorkingDir;

	public boolean ftpInsecureMode;
	
	public String sslProtocol;

	public int xfrServiceHttpPort;
	
	public boolean keyStoreCCS;

	public String keyStoreLocation;

	public String keyStorePassword;

	public String keyManagerLocation;

	public String keyManagerPassword;

	public String keyManagerAlias;

	public String keyManagerKeyPassword;
	
	public boolean keyManagerCCS; 
	
	public String ccsUsername;
	
	public String ccsPassword;
	
	public int keyStoreCacheTimeToLive;	//milliseconds

}
