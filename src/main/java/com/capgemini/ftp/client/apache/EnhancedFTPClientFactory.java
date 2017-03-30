package com.capgemini.ftp.client.apache;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.net.ssl.KeyManager;
import javax.net.ssl.SSLException;
import javax.net.ssl.X509TrustManager;

import org.apache.commons.net.ftp.FTP;
import org.apache.commons.net.io.Util;
import org.apache.commons.net.util.KeyManagerUtils;
import org.apache.commons.net.util.TrustManagerUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.ByteArrayHttpMessageConverter;
import org.springframework.util.StringUtils;

import com.capgemini.CacheManager;
import com.capgemini.Cacheable;
import com.capgemini.CachedKeyStore;
import com.capgemini.exception.MonitoredError;
import com.capgemini.ftp.client.apache.client.EnhancedFTPClient;
import com.capgemini.ftp.client.apache.client.EnhancedFTPSClient;
import com.capgemini.ftp.client.config.FTPClientConfig;

import com.capgemini.rest.AuthorizedRestTemplate;

/**
 * The responsibility of this class is to provide FTPClient Object that
 * successfully connected to FTPServer and ready for transfer of the files.
 * 
 * The type of client i.e. the protocol to be created (FTP or FTPS) is
 * governed by the insecureMode property.
 */
public class EnhancedFTPClientFactory {
    
	private static final Logger logger = LoggerFactory.getLogger(EnhancedFTPClientFactory.class);

	private static final String KEY_STORE_TYPE = "JKS";
	private static final String PROTECTION_PRIVATE = "P";
	private static final int TIMEOUT_IN_MILLIS = 10000;
    private static final Boolean SECURITY_MODE_IS_IMPLICIT = Boolean.FALSE;    
    private static final int PROTECTION_BUFFER_SIZE = 0;
    private static final int DEFAULT_KEEP_ALIVE_MESSAGE_INTERVAL = 60;
    private static final int DEFAULT_KEEP_ALIVE_REPLY_TIMEOUT = 5000;

	/**
	 * Get a connected FTP client according to the supplied config.
	 * 
	 * The caller must close the connection when they're done.
	 * 
	 * @param correlationId
	 *            for logging purposes.
	 * @return A connected and logged in FTP client. If insecureMode is set to
	 *         false then an FTPS client is returned in the connected and logged
	 *         in state.
	 */
	public EnhancedFTPClient getConnectedClient(String correlationId, FTPClientConfig ftpClientConfig) {
		EnhancedFTPClient client = null;
		if (ftpClientConfig.ftpInsecureMode) {
    		logger.info("CorrelationId: {} Creating new FTP client", correlationId);
    		client = new EnhancedFTPClient();
        } else {
            logger.info("CorrelationId: {} Creating new FTPS client", correlationId);
            client = new EnhancedFTPSClient(ftpClientConfig.sslProtocol, SECURITY_MODE_IS_IMPLICIT);       
    		applyFTPSPreConnectionSettingsTo(correlationId, (EnhancedFTPSClient)client, ftpClientConfig);   	     
        }
		applyCommonPreConnectionSettingsTo(correlationId, client, ftpClientConfig);
        return connect(correlationId, ftpClientConfig, client);
    }
    

    /**
     * Connect a FTP/FTPS client.
     */
	private EnhancedFTPClient connect(String correlationId, FTPClientConfig ftpClientConfig, EnhancedFTPClient ftpClient) {
		logger.info("CorrelationId: {} Connecting FTP client", correlationId);
		try {
			loginToTheServerWith(ftpClient, correlationId, ftpClientConfig);
			changeDirFor(correlationId, ftpClient, ftpClientConfig);
			logger.info("CorrelationId: {} Successfully connected to FTP Server {}", correlationId, ftpClientConfig.ftpServerList);
		} catch (IOException ioe) {
			MonitoredError.FTP_CONNECTION_FAILURE.create(correlationId, "n/a", "When creating the FTP connection", ioe);
		}
		return ftpClient;
	}

	
	/**
	 * The goal of this method is to login to the FTP/FTPS server. Handles both FTP and FTPS logins. Accepts a list of FTP servers and tries them in turn.
	 */
	private String loginToTheServerWith(EnhancedFTPClient ftpClient, String correlationId, FTPClientConfig ftpClientConfig) throws IOException {		
		boolean loginSuccess = false;
		List<String> ftpServerList = new ArrayList<String>(Arrays.asList(ftpClientConfig.ftpServerList.split(",")));		
		for (String ftpServer : ftpServerList) {
			for(int i=0 ; i < ftpClientConfig.ftpRetryCount ; i++) {			
				try {				
					if (ftpClientConfig.ftpPort > 0) {
	                    logger.info("CorrelationId: {} Connecting to FTP server {} on port {}", correlationId, ftpServer, ftpClientConfig.ftpPort);
						ftpClient.connect(ftpServer, ftpClientConfig.ftpPort);
					} else {
	                    logger.info("CorrelationId: {} Connection to FTP server {} on default port", correlationId, ftpServer);
						ftpClient.connect(ftpServer);
					}				
					applyCommonPostConnectionSettingsTo(ftpClient, ftpClientConfig);
					if (ftpClient instanceof EnhancedFTPSClient) {
						applyFTPSPostConnectionSettingsTo((EnhancedFTPSClient)ftpClient, ftpClientConfig);
					}                           
	                logger.info("CorrelationId: {} Established connection to FTP server. Attempting to login...", correlationId);
					loginSuccess = ftpClient.login(ftpClientConfig.ftpUser, ftpClientConfig.ftpPassword);
					if (loginSuccess) {
						break;
					}
				} catch(IOException ex) {				
					if(i == ftpClientConfig.ftpRetryCount) {
						throw ex;
					}                
	                logger.info("CorrelationId: {} FTP Connection error was: {}", correlationId, ex);
					logger.info("CorrelationId: {} FTP Connection error : retrying after {} milliseconds", correlationId, ftpClientConfig.ftpRetrySleep);
					waitToRetry(ftpClientConfig);				
				}
			}
			if (loginSuccess) {
				applyCommonPostLoginSettingsTo(ftpClient, ftpClientConfig);
				return ftpServer;
			}
		}
		MonitoredError.FTP_AUTHENTICATION_FAILURE.create(correlationId, "n/a", "FTP Authentication failed. Unable to connect to any server in the list.");
		return null; // Above line will throw an exception
	}
	
	
	/**
	 * This method lets calling thread to wait for 5 seconds.
	 */
	private void waitToRetry(FTPClientConfig ftpsClientConfig) {
		try {
            logger.debug("FTP client going to sleep for " + ftpsClientConfig.ftpRetrySleep + "milliseconds");
			Thread.sleep(ftpsClientConfig.ftpRetrySleep);
		} catch (InterruptedException e) {
		}
	}
		
	
	/**
	 * The goal of this method is to apply settings specific to FTPS that are
	 * required BEFORE a connection can be made.
	 */
	private void applyFTPSPreConnectionSettingsTo(String correlationId, EnhancedFTPSClient ftpsClient, FTPClientConfig ftpsClientConfig) {
		logger.info("CorrelationId: {} Applying FTPS-specific pre-connection settings to FTPS client", correlationId);
		X509TrustManager trustManager = getTrustManager(correlationId, ftpsClientConfig);
		ftpsClient.setTrustManager(trustManager);
		KeyManager keyManager = getKeyManager(correlationId, ftpsClientConfig);
		if (keyManager == null) {
			// TODO this warn replicates the 19AS error logging and enables
			// testing without a key manager but should we not be throwing an
			// exception here? E.g.:
			// MonitoredError.KEYMANAGER_READ_ERROR.create(correlationId, "n/a",
			// "The key manager is incorrectly configured");
			logger.warn("CorrelationId: {} The key manager is incorrectly configured", correlationId);
		} else {
			ftpsClient.setKeyManager(keyManager);
		}
		ftpsClient.setDataTimeout(TIMEOUT_IN_MILLIS);
		// Uncomment below line, if responses from FTPS server need to be logged onto console.
		// ftpsClient.addProtocolCommandListener(new PrintCommandListener(new PrintWriter(System.out), true));
	}

	/**
	 * The goal of this method is to apply common settings that are required BEFORE a
	 * connection can be made.
	 */
	private void applyCommonPreConnectionSettingsTo(String correlationId, EnhancedFTPClient ftpClient, FTPClientConfig ftpsClientConfig) {
		logger.info("CorrelationId: {} Applying common pre-connection settings to FTP client", correlationId);
		ftpClient.setSkipIpFromPasvReply(true);
		ftpClient.setPassiveNatWorkaround(false);
		ftpClient.setControlKeepAliveTimeout(DEFAULT_KEEP_ALIVE_MESSAGE_INTERVAL);
		ftpClient.setControlKeepAliveReplyTimeout(DEFAULT_KEEP_ALIVE_REPLY_TIMEOUT);
	}

	
	/**
	 * The goal of this method is to ensure that the FTP(S) client is setup 
	 * correctly for downloading files using FTP(S).
	 */
	private void applyCommonPostLoginSettingsTo(EnhancedFTPClient ftpClient, FTPClientConfig ftpsClientConfig) throws IOException, SSLException {		
        logger.debug("FTP client file type being set to binary file type.");
		ftpClient.setFileType(FTP.BINARY_FILE_TYPE);
	}

	
	/**
	 * The goal of this method is to ensure that the FTP(S) client is setup 
	 * correctly for downloading files using FTP(S).
	 */
	private void applyCommonPostConnectionSettingsTo(EnhancedFTPClient ftpClient, FTPClientConfig ftpsClientConfig) throws IOException, SSLException {		   
		// PASV mode to transfer data between client and server
        logger.debug("FTP Client configured to PASSIVE mode");
		ftpClient.enterLocalPassiveMode();        
        logger.debug("FTP Client set to ignore hidden files");
		ftpClient.setListHiddenFiles(false);
	}

	
	/**
	 * The goal of this method is to ensure that the FTP(S) client is setup 
	 * correctly for downloading files using FTP(S).
	 */
	private void applyFTPSPostConnectionSettingsTo(EnhancedFTPSClient ftpsClient, FTPClientConfig ftpsClientConfig) throws IOException, SSLException {		
		/*
		 * The PBSZ command is intended by RFC2228 to define the buffer-size to be
		 * used by the security mechanism when it is encrypting data on the
		 * data-channel. However, since TLS is a transport-layer protocol and
		 * therefore doesn't require explicit encryption of data by the application
		 * layer, this buffer-size is redundant. Ford-Hutchinson therefore requires
		 * that a value of '0' is always passed as a parameter.
		 * 
		 * > PBSZ 0
		 * 
		 * While this call is redundant (as it is effectively implied by the AUTH
		 * command), it is required and must precede the PROT command
		 */
        logger.debug("FTPS Client executing PBSZ ");
        ftpsClient.execPBSZ(PROTECTION_BUFFER_SIZE);  
		    
		// Options : Clear(C) and Private(P). 
		// Clear means that no security is to be used on the data-channel, and Private means
		// that the data- channel should be protected by TLS.
        logger.debug("FTPS Client executing PROT with parameter: " + PROTECTION_PRIVATE);
        ftpsClient.execPROT(PROTECTION_PRIVATE);	
	}

	
	/**
	 * The goal of this method is to obtain a reference to a Key Manager.
	 */
	private KeyManager getKeyManager(String correlationId, FTPClientConfig ftpsClientConfig)  {
		
        logger.info("CorrelationId: {} Initializing key manager", correlationId);        
        if (StringUtils.hasText(ftpsClientConfig.keyManagerLocation) && 
            StringUtils.hasText(ftpsClientConfig.keyManagerKeyPassword)) { 
        	
        	String location = ftpsClientConfig.keyManagerLocation;
        	String password = ftpsClientConfig.keyManagerPassword;
        	String keyPassword = ftpsClientConfig.keyManagerKeyPassword;
        	
        	try {
        	
	        	KeyStore keyStore = getKeyStore(correlationId, ftpsClientConfig, location, password);
	        	return KeyManagerUtils.createClientKeyManager(keyStore, ftpsClientConfig.keyManagerAlias, keyPassword);
                
            } catch (GeneralSecurityException ex) {
            	MonitoredError.KEYMANAGER_READ_ERROR.create(correlationId, "n/a", "Security exception while loading key manager for FTPS client", ex);
            }
        }
        return null;
	}    
	
	/**
	 * The goal of this method is to obtain a reference to a Trust Manager.
	 */
	private X509TrustManager getTrustManager(String correlationId, FTPClientConfig ftpsClientConfig) {
		
		try {
        
			logger.info("CorrelationId: {} Initializing trust manager", correlationId);

			String location = ftpsClientConfig.keyStoreLocation;
        	String password = ftpsClientConfig.keyStorePassword;
			
        	KeyStore keyStore = getKeyStore(correlationId, ftpsClientConfig, location, password);
        	return TrustManagerUtils.getDefaultTrustManager(keyStore);
		
		} catch (GeneralSecurityException gse) {
        	MonitoredError.TRUSTMANAGER_INITIALISATION_ERROR.create(correlationId, "n/a", "FTPS trust manager initialisation failed", gse);
		}
		return null;
	}
	
	private KeyStore getKeyStore(String correlationId, FTPClientConfig ftpsClientConfig, String location, String password) {
		
		Cacheable<?> cached = CacheManager.retrieve(location);
    	if(cached != null) {
    		return (KeyStore) cached.getEntity();
    	}
		
    	KeyStore keyStore = null;
    	if(ftpsClientConfig.keyStoreCCS) {
    		keyStore = loadKeyStoreFromCloudConfig(correlationId, ftpsClientConfig, location, password);
    	} else {
    		keyStore = loadKeyStore(correlationId, ftpsClientConfig, location, password);
    	}
    	
    	CacheManager.cache(new CachedKeyStore(location, keyStore, ftpsClientConfig.keyStoreCacheTimeToLive));
    	return keyStore;
	}
	
	/**
	 * Loads the KeyStore (Java Repository for Security Certificates) from keystore repository (.jks).
	 * Helper method from apache: http://commons.apache.org/proper/commons-net/apidocs/index.html?org/apache/commons/net/util/KeyManagerUtils.html
	 */
	private KeyStore loadKeyStore(String correlationId, FTPClientConfig ftpsClientConfig, String location, String password) {		
		
		KeyStore ks = null;
		
		FileInputStream stream = null;
		final File storeFile = new File(ftpsClientConfig.keyStoreLocation);	
		try {
			logger.info("CorrelationId: {} Loading keystore from: {}", correlationId, location);
			ks = KeyStore.getInstance(KEY_STORE_TYPE);
			stream = new FileInputStream(storeFile);
			ks.load(stream, getKeyStorePassword(password));
            logger.info("CorrelationId: {} Loaded keystore", correlationId);
		} catch (NoSuchAlgorithmException | CertificateException | IOException | KeyStoreException e) {
        	MonitoredError.KEYSTORE_READ_ERROR.create(correlationId, "n/a", "Exception while loading keystore", e);
		}
		finally {
			Util.closeQuietly(stream);
		}        
		return ks;	
	}
	
	private KeyStore loadKeyStoreFromCloudConfig(String correlationId, FTPClientConfig ftpsClientConfig, String url, String password) {
		
		AuthorizedRestTemplate restTemplate = new AuthorizedRestTemplate(ftpsClientConfig.ccsUsername, ftpsClientConfig.ccsPassword);
		restTemplate.getMessageConverters().add(new ByteArrayHttpMessageConverter());
		
    	HttpHeaders headers = new HttpHeaders();
    	headers.setAccept(Arrays.asList(MediaType.APPLICATION_OCTET_STREAM));
    	HttpEntity<String> entity = new HttpEntity<String>(headers);  
    	
    	KeyStore keystore = null;
    	try {
    	
    		ResponseEntity<byte[]> resp = restTemplate.exchange(url, HttpMethod.GET,entity, byte[].class);
    		ByteArrayInputStream is = new ByteArrayInputStream(resp.getBody());      
    		keystore = KeyStore.getInstance("JCEKS");
    		keystore.load(is, password.toCharArray());
    	
    	} catch(NoSuchAlgorithmException | CertificateException | IOException | KeyStoreException e) {
    		
    		MonitoredError.KEYSTORE_READ_ERROR.create(correlationId, "n/a", "Exception while loading keystore", e);
    	}
	    
	    return keystore;
	}

	/**
	 * Simple utility method.
	 */
	private char[] getKeyStorePassword(String password) {
		return password != null ? password.toCharArray() : null;
	}

	/**
	 * The goal of this method is to point FTPS Client to specific directory on FTPS Server.
	 */
	private void changeDirFor(String correlationId, EnhancedFTPClient ftpClient, FTPClientConfig ftpsClientConfig) throws IOException {
		if(!StringUtils.isEmpty(ftpsClientConfig.ftpFilesRemoteWorkingDir)) {
            logger.info("CorrelationId: {} Changing FTP client directory to: " + ftpsClientConfig.ftpFilesRemoteWorkingDir, correlationId);
			ftpClient.changeWorkingDirectory(ftpsClientConfig.ftpFilesRemoteWorkingDir);
            logger.info("CorrelationId: {} Directory changed successfully", correlationId);
		} else {
            logger.info("CorrelationId: {} FTP client remote directory is the root directory", correlationId);
        }
	}
}