package com.capgemini.ftp.client;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.net.ftp.FTPFile;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.capgemini.exception.MonitoredError;
import com.capgemini.ftp.client.apache.EnhancedFTPClientFactory;
import com.capgemini.ftp.client.apache.client.EnhancedFTPClient;
import com.capgemini.ftp.client.config.FTPClientConfig;
import com.capgemini.ftp.client.util.StopWatch;

/**
 * Bean containing utility methods for the FTP handling of files.
 */
public class FTPFileTransferBean implements FileTransferBean {

	private static final Logger logger = LoggerFactory.getLogger(FTPFileTransferBean.class);

	/**
	 * {@inheritDoc}
	 */
	@Override
	public EnhancedFTPClient getConnectedFTPClient(String correlationId, FTPClientConfig ftpClientConfig) {
		EnhancedFTPClient ftpClient = null;
		EnhancedFTPClientFactory ftpClientFactory = new EnhancedFTPClientFactory();
		logger.info("CorrelationId: {} Initializing FTP client...", correlationId);
		ftpClient = ftpClientFactory.getConnectedClient(correlationId, ftpClientConfig);
		logger.info("CorrelationId: {} FTP Client connected to server.", correlationId);
		File fileStoreDir = new File(ftpClientConfig.ftpFilesLocalWorkingDir);
		if (!fileStoreDir.exists()) {
			fileStoreDir.mkdirs();
		}
		return ftpClient;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<String> getRemoteDirFileListing(String correlationId, EnhancedFTPClient ftpClient) {
		logger.info("CorrelationId: {} Reading a list of files from the FTP server", correlationId);
		Set<String> filenames = new HashSet<String>();
		try {
			FTPFile[] ftpFiles = ftpClient.listFiles();
			for (FTPFile file : ftpFiles) {
                if (file.isFile()) {
                	filenames.add(file.getName());
                }  
            }
		} catch (IOException e) {
			MonitoredError.FTP_COMMUNICATION_FAILURE.create(correlationId, "n/a", "Remote directory file listing failed.", e);
		}
		if (!filenames.isEmpty()) {
			logger.debug("CorrelationId: {} Retrieved a list of " + filenames.size() + " files", correlationId);
			writeFileListToLog(correlationId, filenames);
		} else {
			logger.info("CorrelationId: {} No files to transfer", correlationId);
		}
		return filenames;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public Set<String> getLocalDirFileList(String path) {
		File folder = new File(path);
		File[] files = folder.listFiles();
		Set<String> fileList = new HashSet<String>();
		for (File file : files) {
			if (file.isFile()) {
				fileList.add(file.getName());
			}
		}
		return fileList;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public long getRemoteFileSize(String correlationId, EnhancedFTPClient ftpClient, String fileName) {
		logger.info("CorrelationId: {} Getting remote file size {} ", correlationId, fileName);
		StopWatch sw = new StopWatch();
		long size = 0;
		try {
			sw.start();
			FTPFile[] files = ftpClient.listFiles(fileName);
			if (files.length != 1) {
				MonitoredError.FTP_COMMUNICATION_FAILURE.create(correlationId, fileName, "Failed to read remote file to get file size.");
			}
			size = files[0].getSize();
		} catch (IOException e) {
			MonitoredError.FTP_COMMUNICATION_FAILURE.create(correlationId, fileName, "Failed to read remote file size.", e);
		}
		sw.end();
		logger.debug("CorrelationId: {} Took [{}] milliseconds to get file size for {} result {}", correlationId, sw.timeTaken(), fileName, size);
		return size;
	}

	/**
	 * {@inheritDoc}
	 */
	@Override	
	public void assertRemoteFileExists(String correlationId, EnhancedFTPClient ftpClient, String fileName) {
		logger.info("CorrelationId: {} Checking if remote file {} exists", correlationId, fileName);
		try {
			FTPFile[] files = ftpClient.listFiles(fileName);
			if (files.length == 0) {
				MonitoredError.FTP_COMMUNICATION_FAILURE.create(correlationId, fileName, "Remote file does not exists.");
			}
		} catch (IOException e) {
			MonitoredError.FTP_COMMUNICATION_FAILURE.create(correlationId, fileName, "Failed to check if remote file exists.", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override	
	public void deleteRemoteFile(String correlationId, EnhancedFTPClient ftpClient, String fileName) {
		logger.info("CorrelationId: {} Deleting remote file {}", correlationId, fileName);
		try {
			boolean success = ftpClient.deleteFile(fileName);
			if (!success) {
				MonitoredError.FTP_COMMUNICATION_FAILURE.create(correlationId, fileName, "Failed to delete remote file.");
			}
		} catch (IOException e) {
			MonitoredError.FTP_COMMUNICATION_FAILURE.create(correlationId, fileName, "Exception when trying to delete remote file.", e);
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void downloadFile(String correlationId, EnhancedFTPClient ftpClient, String localPath, String fileName) {		
		logger.info("CorrelationId: {} Transferring the file {} ", correlationId, fileName);
		StopWatch sw = new StopWatch();
		try {
			sw.start();
			String localFilePath = localPath + "/" + fileName;
			logger.debug("CorrelationId: {} Opening output stream to " + localFilePath, correlationId);
			FileOutputStream outputStream = new FileOutputStream(localFilePath);
			boolean retrieved = ftpClient.retrieveFile(fileName, outputStream);
			outputStream.flush();
			outputStream.close();
			if (!retrieved) {
				MonitoredError.FTP_COMMUNICATION_FAILURE.create(correlationId, fileName, "File could not be downloaded.");
			}
		} catch (IOException e) {
			MonitoredError.FTP_COMMUNICATION_FAILURE.create(correlationId, fileName, "File download error.", e);
		}
		sw.end();
		logger.debug("CorrelationId: {} Took [{}] milliseconds to download the file {} ", correlationId, sw.timeTaken(), fileName);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void uploadFile(String correlationId, EnhancedFTPClient ftpClient, String localpath, String fileName) {
		logger.info("CorrelationId: {} Transferring the file {} ", correlationId, fileName);
		StopWatch sw = new StopWatch();
		try {
			sw.start();
			String localFilePath = localpath + "/" + fileName;
			logger.debug("CorrelationId: {} Opening input stream from {}", correlationId, localFilePath);
			FileInputStream inputStream = new FileInputStream(localFilePath);
			boolean stored = ftpClient.storeFile(fileName, inputStream);
			inputStream.close();
			if (!stored) {
				MonitoredError.FTP_COMMUNICATION_FAILURE.create(correlationId, fileName, "File upload failed.");
			}
		} catch (IOException e) {
			MonitoredError.FTP_COMMUNICATION_FAILURE.create(correlationId, fileName, "File upload error.", e);
		}
		sw.end();
		logger.debug("CorrelationId: {} Took [{}] milliseconds to upload the file {} ", correlationId, sw.timeTaken(), fileName);
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void moveRemoteFile(String correlationId, EnhancedFTPClient ftpClient, String sourcePath, String destinationPath){
		logger.info("CorrelationId: {} Moving remote file {} to {}", correlationId, sourcePath, destinationPath);
		StopWatch sw = new StopWatch();
		sw.start();
		try {
			boolean success = ftpClient.rename(sourcePath, destinationPath);
			if (!success) {
				MonitoredError.FTP_COMMUNICATION_FAILURE.create(correlationId, sourcePath, "File move failed.");
			}
		} catch (IOException e) {
			MonitoredError.FTP_COMMUNICATION_FAILURE.create(correlationId, sourcePath, "Error when trying to move file.", e);
		}
		sw.end();
		logger.debug("CorrelationId: {} Took [{}] milliseconds to move file {} to {}", correlationId, sw.timeTaken(), sourcePath, destinationPath);		
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void disconnectFTPClient(String correlationId, EnhancedFTPClient ftpClient) {
		try {
			if (ftpClient != null && ftpClient.isConnected()) {
				ftpClient.logout();
				logger.info("CorrelationId: {} The FTP client has now logged out from the FTP server", correlationId);
			}
		} catch (IOException e) {
			logger.warn("CorrelationId: {} Failed to logout from the server, " + e.getMessage(), correlationId);
			ftpClient = null;
		} finally {
			if (ftpClient != null && ftpClient.isConnected()) {
				try {
					ftpClient.disconnect();
					logger.info("CorrelationId: {} FTP client has now disconnected from the FTP server", correlationId);
				} catch (IOException ioe) {
					logger.warn("CorrelationId: {} There was a problem when disconnecting from the FTP server: " + ioe.getMessage(), correlationId);
					// Do nothing.
				}
			}
		}
	}

	/**
	 * {@inheritDoc}
	 */
	@Override
	public void deleteLocalFile(String correlationId, String filePath) {
		logger.info("CorrelationId: {} About to delete local file {}", correlationId, filePath);
		File file = new File(filePath);
		boolean deleted = file.delete();
		if (!deleted) {
			MonitoredError.LOCAL_FILE_ACCESS_ERROR.create(correlationId, filePath, "Error when trying to delete local file.");
		}
		logger.info("CorrelationId: {} Local file {} deleted.", correlationId, filePath);
	}

	private void writeFileListToLog(String correlationId, Collection<String> fileNames) {
		StringBuilder msg = new StringBuilder("CorrelationId: %s : FTP Client found the following files on the server:\n");
		for (String fileName : fileNames) {
			msg.append(" --> ").append(fileName).append('\n');
		}
		String outputMsg = msg.toString();
		logger.info(String.format(outputMsg, correlationId));
	}

	/** 
	 * {@inheritDoc}
	 */
	@Override
	public void createLocalDirectory(String correlationId, String directoryName) {
		
		logger.info("CorrelationId: {} About to create local directory {}.", correlationId, directoryName);
		
		if (!(new File(directoryName).mkdirs())) {
			MonitoredError.UNHANDLED_TRANSFER_ERROR.create(correlationId, "n/a", "Failed to create temp directory : " + directoryName);
		}
		
		logger.info("CorrelationId: {} Local directory {} created.", correlationId, directoryName);
	}
	
	/** 
	 * {@inheritDoc}
	 */
	@Override
	public void deleteLocalDirectory(String correlationId, String directoryName) {
		
		logger.info("CorrelationId: {} About to delete local directory {}.", correlationId, directoryName);
		
		if((new File(directoryName).delete())) {
			logger.info("CorrelationId: {} Local directory {} deleted.", correlationId, directoryName);
		} else {
			logger.error("CorrelationId: {} Failed to delete {} Directory", correlationId, directoryName);
		}
	}
}
