package com.capgemini.exception;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * The responsibility of this class is to define monitored errors in the file
 * transfer solution.
 * 
 * It will always throw a FileTransferException so if your code encounters
 * a FileTransferException you know it has already been logged.
 * 
 * <p>
 * This class should be used like this:
 * <p>
 * 
 * <pre>
 * <code>
 *  try {
 *    ...
 *    // Some code
 *    ...
 *  } catch (Exception e) {
 *    MonitoredError.create(correlationId, filename, "Some message text", e);   
 *  }
 * </code>
 * </pre>
 */
public enum MonitoredError {

	FTP_AUTHENTICATION_FAILURE,
	FTP_CONNECTION_FAILURE,
	FTP_COMMUNICATION_FAILURE,
	LOCAL_FILE_ACCESS_ERROR,
	KEYMANAGER_READ_ERROR,
	KEYSTORE_READ_ERROR,
	TRUSTMANAGER_INITIALISATION_ERROR,
	UNHANDLED_TRANSFER_ERROR;

	private static final Logger logger = LoggerFactory.getLogger(MonitoredError.class);

	private static final String ERROR_MESSAGE_BASE = "%s : CorrelationId=%s; Filename=%s : %s";

	/**
	 * Logs a failure in a consistent format while raising a new exception.
	 * 
	 * @param correlationId
	 *            The correlation id
	 * @param filename
	 *            The filename
	 * @param msg
	 *            The failure message
	 */
	public void create(String correlationId, String filename, String msg) {
		this.create(correlationId, filename, msg, null);
	}

	/**
	 * Logs a failure in a consistent format while re-raising an existing
	 * exception.
	 * 
	 * @param correlationId
	 *            The correlation id
	 * @param filename
	 *            The filename
	 * @param msg
	 *            The failure message
	 * @param cause
	 *            The failure cause, or null if no exception was the cause.
	 */
	public void create(String correlationId, String filename, String msg, Exception cause) {
		String errorMessage = report(correlationId, filename, msg, cause);
		if (cause != null) {
			throw new RuntimeException(errorMessage, cause);
		} else {
			throw new RuntimeException(errorMessage);
		}
	}

	/**
	 * Logs a failure in a consistent format.
	 */
	private String report(String correlationId, String filename, String msg, Exception cause) {
		String errorMessage = String.format(ERROR_MESSAGE_BASE, name(), correlationId, filename, msg);
		if (cause != null) {
			errorMessage = errorMessage + ". Cause was: \n" + cause.getStackTrace();
		}
		logger.error(errorMessage);
		return errorMessage;
	}

}