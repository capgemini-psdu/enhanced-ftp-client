package com.capgemini.ftp.client;

import java.util.Set;

import com.capgemini.ftp.client.apache.client.EnhancedFTPClient;
import com.capgemini.ftp.client.config.FTPClientConfig;

public interface FileTransferBean {

	/**
	 * Get a EnhancedFTPClient connected to the remote server.
	 * 
	 * @param correlationId
	 *            For logging purposes.
	 * @param ftpsClientConfig
	 *            the client configuration.
	 * @return An EnhancedFTPClient connected to the remote server.
	 */
	EnhancedFTPClient getConnectedFTPClient(String correlationId, FTPClientConfig ftpsClientConfig);

	/**
	 * The goal of this method is to return a list of candidate files for
	 * potential download.
	 * 
	 * @param correlationId
	 *            For logging purposes.
	 * @param ftpClient
	 *            Connected EnhancedFTPClient.
	 * @return Array of filenames.
	 */
	Set<String> getRemoteDirFileListing(String correlationId, EnhancedFTPClient ftpClient);

	
	/**
	 * Get local directory listing of files (only i.e. discounting nested
	 * directories).
	 * 
	 * @param path
	 *            the dir path to list.
	 * @return list of files in the directory.
	 */
	Set<String> getLocalDirFileList(String path);
	
	/**
	 * Get the size in bytes of a remote file.
	 * 
	 * @param correlationId
	 *            For logging purposes.
	 * @param ftpClient
	 *            Connected EnhancedFTPClient.
	 * @param fileName
	 *            The filename.
	 * @return The size in bytes.
	 */
	long getRemoteFileSize(String correlationId, EnhancedFTPClient ftpClient, String fileName);

	/**
	 * Download a single file.
	 * 
	 * @param correlationId
	 *            For logging purposes.
	 * @param ftpClient
	 *            Connected EnhancedFTPClient.
	 * @param localPath
	 *            Local path where file is being stored.
	 * @param fileName
	 *            Remote filename.
	 */
	void downloadFile(String correlationId, EnhancedFTPClient ftpClient, String localPath, String fileName);

	/**
	 * Upload a single file.
	 * 
	 * @param correlationId
	 *            For logging purposes.
	 * @param ftpClient
	 *            Connected EnhancedFTPClient.
	 * @param localPath
	 *            Local path from where file is being uploaded.
	 * @param fileName
	 *            Remote filename.
	 */
	public void uploadFile(String correlationId, EnhancedFTPClient ftpClient, String localPath, String fileName);
	
	/**
	 * @param correlationId
	 *            For logging.
	 * @param filePath
	 *            The full path including filename of the local file to be
	 *            deleted.
	 */
	public void deleteLocalFile(String correlationId, String filePath);
	
	/**
	 * @param correlationId
	 *            For logging.
	 * @param ftpClient
	 *            Connected EnhancedFTPClient.
	 * @param sourcePath
	 *            Full path of the source file.
	 * @param destinationPath
	 *            Full path of the destination file.
	 */
	public void moveRemoteFile(String correlationId, EnhancedFTPClient ftpClient, String sourcePath, String destinationPath);

	/**
	 * The goal of this method is to safely close the FTP connection.
	 * 
	 * @param correlationId
	 *            For logging.
	 * @param ftpClient
	 *            The client to close.
	 */
	void disconnectFTPClient(String correlationId, EnhancedFTPClient ftpClient);

	/**
	 * Assert if the file with supplied file name exists on the remote ftp server.
	 * 
	 * @param correlationId
	 *            For logging.
	 * @param ftpClient
	 *            Connected EnhancedFTPClient.
	 * @param fileName
	 *            Remote filename.
	 * 
	 * @throws FileTransferException throws FileTransferException if the file does not exists.
	 */
	void assertRemoteFileExists(String correlationId, EnhancedFTPClient ftpClient, String fileName);
	
	/**
	 * Delete a remote file from the FTP server.
	 * 
	 * @param correlationId
	 *            For logging.
	 * @param ftpClient
	 *            Connected EnhancedFTPClient.
	 * @param fileName
	 *            Remote filename.
	 * 
	 * @throws FileTransferException throws FileTransferException if the file could not be deleted.
	 */
	void deleteRemoteFile(String correlationId, EnhancedFTPClient ftpClient, String fileName);

	/**
	 * Create a local temporary directory
	 * 
	 * @param correlationId
	 *            For logging.
	 * @param directoryName
	 *            Local Directory name.
	 *
	 * @throws FileTransferException throws FileTransferException if the directory could not be created.
	 */
	void createLocalDirectory(String correlationId, String directoryName);
	
	/**
	 * Delete a local temporary directory
	 * 
	 * @param correlationId
	 *            For logging.
	 * @param directoryName
	 *            Local Directory name.
	 */
	void deleteLocalDirectory(String correlationId, String directoryName);

}