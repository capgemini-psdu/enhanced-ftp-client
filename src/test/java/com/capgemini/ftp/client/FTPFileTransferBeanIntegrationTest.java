package com.capgemini.ftp.client;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.MockitoAnnotations.initMocks;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.ftpserver.FtpServer;
import org.apache.ftpserver.FtpServerFactory;
import org.apache.ftpserver.ftplet.Authentication;
import org.apache.ftpserver.ftplet.AuthenticationFailedException;
import org.apache.ftpserver.ftplet.Authority;
import org.apache.ftpserver.ftplet.FtpException;
import org.apache.ftpserver.ftplet.User;
import org.apache.ftpserver.ftplet.UserManager;
import org.apache.ftpserver.listener.ListenerFactory;
import org.apache.ftpserver.ssl.SslConfigurationFactory;
import org.apache.ftpserver.usermanager.AnonymousAuthentication;
import org.apache.ftpserver.usermanager.UsernamePasswordAuthentication;
import org.apache.ftpserver.usermanager.impl.BaseUser;
import org.apache.ftpserver.usermanager.impl.ConcurrentLoginPermission;
import org.apache.ftpserver.usermanager.impl.TransferRatePermission;
import org.apache.ftpserver.usermanager.impl.WritePermission;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.capgemini.ftp.client.FTPFileTransferBean;
import com.capgemini.ftp.client.FileTransferBean;
import com.capgemini.ftp.client.apache.client.EnhancedFTPClient;
import com.capgemini.ftp.client.config.FTPClientConfig;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;

public class FTPFileTransferBeanIntegrationTest {

	// Root of all test files/locations
	private static final String TEST_RESOURCES = "src/test/resources";
	
	// Local files/locations
	private static final String LOCAL_WORKING_DIR_ROOT = TEST_RESOURCES + "/localworkingdir";
	private static final String TEMP_LOCAL_DIR = LOCAL_WORKING_DIR_ROOT + "/temp";
	private static final String SCENARIO_01_LOCAL_DIR = LOCAL_WORKING_DIR_ROOT + "/scenario_01";
	
	// Remote FTP server files/locations
	private static final String FTPSERVER_FILES_ROOT = TEST_RESOURCES + "/ftpserver";
	private static final String SCENARIO_01_REMOTE_DIR = FTPSERVER_FILES_ROOT + "/scenario_01";
	private static final String SCENARIO_02_REMOTE_DIR = FTPSERVER_FILES_ROOT + "/scenario_02";
	private static final String TEMP_REMOTE_DIR = FTPSERVER_FILES_ROOT + "/temp";
	private static final String TEMP_REMOTE_ARCHIVE_DIR = TEMP_REMOTE_DIR + "/archive";

	private static final String FILENAME_01 = "file1.xml";
	
	private static final String UNKNOWN_FILENAME = "unknown_file.xml";

	// Test config files/locations 
	private static final String KEYSTORE_PATH = TEST_RESOURCES + "/ftpserver.jks";
	
	private static final String CORRELATION_ID = "correlationid";

	private static final int FTP_PORT = 2221;

	protected TestAppender testAppender;

	FtpServer ftpServer;

	FileTransferBean fileTransferBean = new FTPFileTransferBean();

	@Before
	public void setUp() {
		deleteTempFiles();
		initMocks(this);
		ftpServer = createFtpServer(FTP_PORT, true);
	}

	@After
	public void tearDown() {
		deleteTempFiles();
	}

	@Test
	public void testGetLocalDirListing() throws Exception {
		Set<String> files = fileTransferBean.getLocalDirFileList(SCENARIO_01_LOCAL_DIR);
		assertEquals(1, countXMLFiles(files));
		assertTrue(CollectionUtils.contains(files.iterator(), FILENAME_01));
	}

	@Test
	public void testGetRemoteDirListing() throws Exception {
		ftpServer.start();
		try {
			FTPClientConfig clientConfig = createBaseFTPClientConfig(SCENARIO_01_REMOTE_DIR);
			EnhancedFTPClient ftpClient = fileTransferBean.getConnectedFTPClient(CORRELATION_ID, clientConfig);
			Set<String> files = fileTransferBean.getRemoteDirFileListing(CORRELATION_ID, ftpClient);
			fileTransferBean.disconnectFTPClient(CORRELATION_ID, ftpClient);
			assertEquals(1, countXMLFiles(files));
			assertTrue(CollectionUtils.contains(files.iterator(), FILENAME_01));
		} finally {
			ftpServer.stop();
		}
	}

	@Test
	public void testGetRemoteFileSize() throws Exception {
		ftpServer.start();
		try {
			FTPClientConfig clientConfig = createBaseFTPClientConfig(SCENARIO_01_REMOTE_DIR);
			EnhancedFTPClient ftpClient = fileTransferBean.getConnectedFTPClient(CORRELATION_ID, clientConfig);
			long size = fileTransferBean.getRemoteFileSize(CORRELATION_ID, ftpClient, FILENAME_01);
			fileTransferBean.disconnectFTPClient(CORRELATION_ID, ftpClient);
			assertEquals(7406, size);
		} finally {
			ftpServer.stop();
		}
	}

	@Test
	public void testCheckRemoteFileExistSuccess() throws Exception {
		ftpServer.start();
		try {
			FTPClientConfig clientConfig = createBaseFTPClientConfig(SCENARIO_01_REMOTE_DIR);
			EnhancedFTPClient ftpClient = fileTransferBean.getConnectedFTPClient(CORRELATION_ID, clientConfig);
			fileTransferBean.assertRemoteFileExists(CORRELATION_ID, ftpClient, FILENAME_01);
			fileTransferBean.disconnectFTPClient(CORRELATION_ID, ftpClient);
		} catch(RuntimeException fileTransferException) {
			fail("Should not throw FileTransferException");
		} finally {
			ftpServer.stop();
		}
	}

	@Test(expected = RuntimeException.class)
	public void testCheckRemoteFileExistFailure() throws Exception {
		ftpServer.start();
		try {
			FTPClientConfig clientConfig = createBaseFTPClientConfig(SCENARIO_01_REMOTE_DIR);
			EnhancedFTPClient ftpClient = fileTransferBean.getConnectedFTPClient(CORRELATION_ID, clientConfig);
			fileTransferBean.assertRemoteFileExists(CORRELATION_ID, ftpClient, UNKNOWN_FILENAME);
			fileTransferBean.disconnectFTPClient(CORRELATION_ID, ftpClient);
		} finally {
			ftpServer.stop();
		}
		fail("Must throw FileTransferException");
	}

	@Test
	public void testDeleteLocalFile() throws Exception {
		createTempFile(TEMP_LOCAL_DIR + "/newfile.xml");
		Set<String> files = fileTransferBean.getLocalDirFileList(TEMP_LOCAL_DIR);
		assertEquals(1, countXMLFiles(files));
		assertTrue(CollectionUtils.contains(files.iterator(), "newfile.xml"));
		fileTransferBean.deleteLocalFile(CORRELATION_ID, TEMP_LOCAL_DIR + "/newfile.xml");
		files = fileTransferBean.getLocalDirFileList(TEMP_LOCAL_DIR);
		assertEquals(0, countXMLFiles(files));
	}

	@Test
	public void testDownloadFile() throws Exception {
		ftpServer.start();
		try {
			FTPClientConfig clientConfig = createBaseFTPClientConfig(SCENARIO_01_REMOTE_DIR);
			EnhancedFTPClient ftpClient = fileTransferBean.getConnectedFTPClient(CORRELATION_ID, clientConfig);
			fileTransferBean.downloadFile(CORRELATION_ID, ftpClient, TEMP_LOCAL_DIR, FILENAME_01);
			Set<String> files = fileTransferBean.getLocalDirFileList(TEMP_LOCAL_DIR);
			assertEquals(1, countXMLFiles(files));
			assertTrue(CollectionUtils.contains(files.iterator(), FILENAME_01));
			fileTransferBean.disconnectFTPClient(CORRELATION_ID, ftpClient);
		} finally {
			ftpServer.stop();
		}
	}

	@Test
	public void testDownloadMultipleFiles() throws Exception {
		ftpServer.start();
		try {
			FTPClientConfig clientConfig = createBaseFTPClientConfig(SCENARIO_02_REMOTE_DIR);
			EnhancedFTPClient ftpClient = fileTransferBean.getConnectedFTPClient(CORRELATION_ID, clientConfig);
			Set<String> files = fileTransferBean.getRemoteDirFileListing(CORRELATION_ID, ftpClient);
			assertEquals(2, countXMLFiles(files));			
			for (String file : files) {
				fileTransferBean.downloadFile(CORRELATION_ID, ftpClient, TEMP_LOCAL_DIR, file);				
			}
			files = fileTransferBean.getLocalDirFileList(TEMP_LOCAL_DIR);
			assertEquals(2, countXMLFiles(files));
			fileTransferBean.disconnectFTPClient(CORRELATION_ID, ftpClient);
		} finally {
			ftpServer.stop();
		}
	}

	@Test
	public void testRenameRemoteFile() throws Exception {
		ftpServer.start();
		try {
			createTempFile(TEMP_REMOTE_DIR + "/newfile.xml");
			FTPClientConfig clientConfig = createBaseFTPClientConfig(TEMP_REMOTE_DIR);
			EnhancedFTPClient ftpClient = fileTransferBean.getConnectedFTPClient(CORRELATION_ID, clientConfig);
			Set<String> files = fileTransferBean.getLocalDirFileList(TEMP_REMOTE_DIR); // Simpler to use the local variant of this in the test
			assertEquals(1, countXMLFiles(files));
			assertTrue(CollectionUtils.contains(files.iterator(), "newfile.xml"));
			fileTransferBean.moveRemoteFile(CORRELATION_ID, ftpClient, "newfile.xml", "newfile_archived.xml");
			files = fileTransferBean.getLocalDirFileList(TEMP_REMOTE_DIR);
			assertEquals(1, countXMLFiles(files));
			assertTrue(CollectionUtils.contains(files.iterator(), "newfile_archived.xml"));
			fileTransferBean.disconnectFTPClient(CORRELATION_ID, ftpClient);
		} finally {
			ftpServer.stop();
		}
	}

	@Test
	public void testMoveRemoteFile() throws Exception {
		ftpServer.start();
		try {
			createTempFile(TEMP_REMOTE_DIR + "/newfile.xml");
			FTPClientConfig clientConfig = createBaseFTPClientConfig(TEMP_REMOTE_DIR);
			EnhancedFTPClient ftpClient = fileTransferBean.getConnectedFTPClient(CORRELATION_ID, clientConfig);
			Set<String> files = fileTransferBean.getLocalDirFileList(TEMP_REMOTE_DIR); // Simpler to use the local variant of this in the test
			assertEquals(1, countXMLFiles(files));
			assertTrue(CollectionUtils.contains(files.iterator(), "newfile.xml"));
			files = fileTransferBean.getLocalDirFileList(TEMP_REMOTE_ARCHIVE_DIR);
			assertEquals(0, countXMLFiles(files));
			fileTransferBean.moveRemoteFile(CORRELATION_ID, ftpClient, "newfile.xml", "archive/newfile_archived.xml");
			files = fileTransferBean.getLocalDirFileList(TEMP_REMOTE_DIR);
			assertEquals(0, countXMLFiles(files));
			files = fileTransferBean.getLocalDirFileList(TEMP_REMOTE_ARCHIVE_DIR);
			assertEquals(1, countXMLFiles(files));
			assertTrue(CollectionUtils.contains(files.iterator(), "newfile_archived.xml"));
			fileTransferBean.disconnectFTPClient(CORRELATION_ID, ftpClient);
		} finally {
			ftpServer.stop();
		}
	}

	@Test
	public void testMoveRemoteFileBackUpThroughDirTree() throws Exception {
		ftpServer.start();
		try {
			createTempFile(TEMP_REMOTE_DIR + "/newfile.xml");
			FTPClientConfig clientConfig = createBaseFTPClientConfig(TEMP_REMOTE_DIR);
			EnhancedFTPClient ftpClient = fileTransferBean.getConnectedFTPClient(CORRELATION_ID, clientConfig);
			Set<String> files = fileTransferBean.getLocalDirFileList(TEMP_REMOTE_DIR); // Simpler to use the local variant of this in the test
			assertEquals(1, countXMLFiles(files));
			assertTrue(CollectionUtils.contains(files.iterator(), "newfile.xml"));
			files = fileTransferBean.getLocalDirFileList(TEMP_REMOTE_ARCHIVE_DIR);
			assertEquals(0, countXMLFiles(files));
			fileTransferBean.moveRemoteFile(CORRELATION_ID, ftpClient, "newfile.xml", "../temp/archive/newfile_archived.xml");
			files = fileTransferBean.getLocalDirFileList(TEMP_REMOTE_DIR);
			assertEquals(0, countXMLFiles(files));
			files = fileTransferBean.getLocalDirFileList(TEMP_REMOTE_ARCHIVE_DIR);
			assertEquals(1, countXMLFiles(files));
			assertTrue(CollectionUtils.contains(files.iterator(), "newfile_archived.xml"));
			fileTransferBean.disconnectFTPClient(CORRELATION_ID, ftpClient);
		} finally {
			ftpServer.stop();
		}
	}

	@Test
	public void testUploadFile() throws Exception {
		ftpServer.start();
		try {
			FTPClientConfig clientConfig = createBaseFTPClientConfig(TEMP_REMOTE_DIR);
			EnhancedFTPClient ftpClient = fileTransferBean.getConnectedFTPClient(CORRELATION_ID, clientConfig);
			Set<String> files = fileTransferBean.getRemoteDirFileListing(CORRELATION_ID, ftpClient);
			assertEquals(0, countXMLFiles(files));
			fileTransferBean.uploadFile(CORRELATION_ID, ftpClient, SCENARIO_01_LOCAL_DIR, FILENAME_01);
			files = fileTransferBean.getRemoteDirFileListing(CORRELATION_ID, ftpClient);
			assertEquals(1, countXMLFiles(files));
			fileTransferBean.disconnectFTPClient(CORRELATION_ID, ftpClient);
		} finally {
			ftpServer.stop();
		}
	}

	@Test
	public void testUploadFileToFirstOfMultipleServers() throws Exception {
		ftpServer.start();
		try {
			FTPClientConfig clientConfig = createBaseFTPClientConfig(TEMP_REMOTE_DIR);
			clientConfig.ftpServerList = "localhost,someinvalidhost";
			EnhancedFTPClient ftpClient = fileTransferBean.getConnectedFTPClient(CORRELATION_ID, clientConfig);
			Set<String> files = fileTransferBean.getRemoteDirFileListing(CORRELATION_ID, ftpClient);
			assertEquals(0, countXMLFiles(files));
			fileTransferBean.uploadFile(CORRELATION_ID, ftpClient, SCENARIO_01_LOCAL_DIR, FILENAME_01);
			files = fileTransferBean.getRemoteDirFileListing(CORRELATION_ID, ftpClient);
			assertEquals(1, countXMLFiles(files));
			fileTransferBean.disconnectFTPClient(CORRELATION_ID, ftpClient);
		} finally {
			ftpServer.stop();
		}
	}

	@Test
	public void testUploadFileToLastOfMultipleServers() throws Exception {
		ftpServer.start();
		try {
			FTPClientConfig clientConfig = createBaseFTPClientConfig(TEMP_REMOTE_DIR);
			clientConfig.ftpServerList = "someinvalidhost,localhost";
			EnhancedFTPClient ftpClient = fileTransferBean.getConnectedFTPClient(CORRELATION_ID, clientConfig);
			Set<String> files = fileTransferBean.getRemoteDirFileListing(CORRELATION_ID, ftpClient);
			assertEquals(0, countXMLFiles(files));
			fileTransferBean.uploadFile(CORRELATION_ID, ftpClient, SCENARIO_01_LOCAL_DIR, FILENAME_01);
			files = fileTransferBean.getRemoteDirFileListing(CORRELATION_ID, ftpClient);
			assertEquals(1, countXMLFiles(files));
			fileTransferBean.disconnectFTPClient(CORRELATION_ID, ftpClient);
		} finally {
			ftpServer.stop();
		}
	}

	@Test(expected=RuntimeException.class)
	public void testGetFtpClientWhenAllServersUnavailable() throws Exception {
		ftpServer.start();
		try {
			FTPClientConfig clientConfig = createBaseFTPClientConfig(TEMP_REMOTE_DIR);
			clientConfig.ftpServerList = "someinvalidhost,anotherinvalidhost";
			fileTransferBean.getConnectedFTPClient(CORRELATION_ID, clientConfig);
		} finally {
			ftpServer.stop();
		}
	}

	@Test
	public void testDeleteRemoteFile() throws Exception {
		ftpServer.start();
		try {
			FTPClientConfig clientConfig = createBaseFTPClientConfig(TEMP_REMOTE_DIR);
			EnhancedFTPClient ftpClient = fileTransferBean.getConnectedFTPClient(CORRELATION_ID, clientConfig);
			// Create temp remote file first
			Set<String> files = fileTransferBean.getRemoteDirFileListing(CORRELATION_ID, ftpClient);
			assertEquals(0, countXMLFiles(files));
			fileTransferBean.uploadFile(CORRELATION_ID, ftpClient, SCENARIO_01_LOCAL_DIR, FILENAME_01);
			files = fileTransferBean.getRemoteDirFileListing(CORRELATION_ID, ftpClient);
			assertEquals(1, countXMLFiles(files));
			// Now delete it
			fileTransferBean.deleteRemoteFile(CORRELATION_ID, ftpClient, FILENAME_01);
			files = fileTransferBean.getRemoteDirFileListing(CORRELATION_ID, ftpClient);
			assertEquals(0, countXMLFiles(files));
			fileTransferBean.disconnectFTPClient(CORRELATION_ID, ftpClient);
		} finally {
			ftpServer.stop();
		}
	}
	
	@Test
	public void testCreateLocalDirectorySuccessfullyCreatesDirectory() throws IOException {
		
		fileTransferBean.createLocalDirectory(CORRELATION_ID, TEMP_LOCAL_DIR + "/newDirectory");
		
		File newDirectory = new File(TEMP_LOCAL_DIR + "/newDirectory");
		
		assertTrue(newDirectory.exists());
		
		FileUtils.deleteDirectory(newDirectory);
	}
	
	@Test(expected = RuntimeException.class)
	public void testCreateLocalDirectoryRaisesExceptionAsDirectoryAlreadyExists() throws Exception {
		
		File newDirectory = new File(TEMP_LOCAL_DIR + "/newDirectory");
		
		newDirectory.mkdirs();
		newDirectory.deleteOnExit();
		
		assertTrue(newDirectory.exists());
		
		try {
			fileTransferBean.createLocalDirectory(CORRELATION_ID, TEMP_LOCAL_DIR + "/newDirectory");
		} catch (RuntimeException fte) {
			
			assertEquals("UNHANDLED_TRANSFER_ERROR : CorrelationId=correlationid; Filename=n/a : Failed to create temp directory : src/test/resources/localworkingdir/temp/newDirectory",fte.getMessage());
			throw fte; 
		}
	}
	
	@Test
	public void testDeleteLocalDirectorySuccessfullyDeletesDirectory() {
		
		File newDirectory = new File(TEMP_LOCAL_DIR + "/newDirectory");
		newDirectory.mkdirs();
		assertTrue(newDirectory.exists());
		
		fileTransferBean.deleteLocalDirectory(CORRELATION_ID, TEMP_LOCAL_DIR + "/newDirectory");
		
		assertFalse(newDirectory.exists());
	}
	
	private void deleteTempFiles() {
		for (String dirString : new String[]{TEMP_LOCAL_DIR, TEMP_REMOTE_DIR, TEMP_REMOTE_ARCHIVE_DIR}) {
			File localTempDir = new File(dirString);
			File[] files = localTempDir.listFiles();
			for (File filename : files) {
				if (filename.getName().endsWith(".xml")) {
					filename.delete();
				}
			}
		}
	}
	
	private int countXMLFiles(Collection<String> files) {
		int i = 0;
		for (String file : files) {
			if (StringUtils.endsWithIgnoreCase(file, ".xml")) {
				i++;
			}
		}
		return i;
	}
	
	private void createTempFile(String fullFilePath) throws Exception {
		File file = new File(fullFilePath);
		if (!file.createNewFile()) {
			throw new RuntimeException("Unable to create file during test.");
		}
	}
	
	private static FTPClientConfig createBaseFTPClientConfig(String remoteDir) {
		Map<String, Object> map = new HashMap<String, Object>();
		map.put("ftpServerList", "localhost");
		map.put("ftpPort", FTP_PORT);
		map.put("ftpUser", "test");
		map.put("ftpPassword", "test");
		map.put("ftpRetryCount", 3);
		map.put("ftpRetrySleep", 1000);
		map.put("ftpFilesLocalWorkingDir", TEMP_LOCAL_DIR);
		map.put("ftpFilesRemoteWorkingDir", remoteDir);
		map.put("ftpInsecureMode", false);
		map.put("sslProtocol", "TLSv1.2");
		map.put("keyStoreLocation", KEYSTORE_PATH);
		map.put("keyStorePassword", "supermdp");
		map.put("keyStoreCCS", false);
		map.put("keyManagerCCS", false);
		map.put("keyStoreCacheTimeToLive", 60000);
		FTPClientConfig clientConfig = new FTPClientConfig(map);
		return clientConfig;
	}

	private FtpServer createFtpServer(int port, boolean ftps) {
		final ListenerFactory listenerFactory = new ListenerFactory();
		listenerFactory.setPort(port);
		if (ftps) {
			// SSL config
			final SslConfigurationFactory sslConfigurationFactory = new SslConfigurationFactory();
			// Create store: keytool -genkey -alias ftptest -keyalg RSA
			// -keystore ftpserver.jks -keysize 4096
			sslConfigurationFactory.setKeystoreFile(new File(KEYSTORE_PATH));
			sslConfigurationFactory.setKeystorePassword("supermdp");
			listenerFactory.setSslConfiguration(sslConfigurationFactory.createSslConfiguration());
			listenerFactory.setImplicitSsl(false);
		}
		// Listener
		final FtpServerFactory ftpServerFactory = new FtpServerFactory();
		ftpServerFactory.addListener("default", listenerFactory.createListener());
		// Authentication
		ftpServerFactory.setUserManager(new MyUserManager());
		return ftpServerFactory.createServer();
	}

	class MyUserManager implements UserManager {
		@Override
		public User getUserByName(final String userName) {
			BaseUser user = new BaseUser();
			user.setName(userName);
			user.setEnabled(true);
			// Home dir = .
			user.setHomeDirectory("./");
			List<Authority> authorities = new ArrayList<Authority>();
			authorities.add(new WritePermission());
			// No special limit
			int maxLogin = 0;
			int maxLoginPerIP = 0;
			authorities.add(new ConcurrentLoginPermission(maxLogin, maxLoginPerIP));
			int uploadRate = 0;
			int downloadRate = 0;
			authorities.add(new TransferRatePermission(downloadRate, uploadRate));
			user.setAuthorities(authorities);
			user.setMaxIdleTime(0);
			return user;
		}

		@Override
		public String[] getAllUserNames() throws FtpException {
			return new String[] { "bob" };
		}

		@Override
		public void delete(final String s) throws FtpException {
		}

		@Override
		public void save(final User user) throws FtpException {
		}

		@Override
		public boolean doesExist(final String s) {
			return true;
		}

		@Override
		public User authenticate(final Authentication authentication) throws AuthenticationFailedException {
			if (authentication instanceof UsernamePasswordAuthentication) {
				UsernamePasswordAuthentication upauth = (UsernamePasswordAuthentication) authentication;
				String user = upauth.getUsername();
				String password = upauth.getPassword();
				if (user == null) {
					throw new AuthenticationFailedException("Authentication failed");
				}
				// Simple auth for tests: password = login
				if (!user.equals(password)) {
					throw new AuthenticationFailedException("Dummy authentication: password must be equals to login");
				}
				return getUserByName(user);
			} else if (authentication instanceof AnonymousAuthentication) {
				if (doesExist("anonymous")) {
					return getUserByName("anonymous");
				} else {
					throw new AuthenticationFailedException("Authentication failed");
				}
			} else {
				throw new IllegalArgumentException("Authentication not supported by this user manager");
			}
		}

		@Override
		public String getAdminName() throws FtpException {
			return "admin";
		}

		@Override
		public boolean isAdmin(final String s) throws FtpException {
			return "admin".equals(s);
		}
	}
}