# enhanced-ftp-client
This library is designed to provide a comprehensive set of FTP/FTPS functinoality exposed via a simple bean style interface with standard logging and error handling.

It is expected that the library will grow over time as new file transfer microservices introduce new requirements for different types of FTP interactions.

## Configuration
The expectation is that this library will be used with Spring. It provides a standard domain object annotated with @ConfigurationProperties that encapsulates the config necessary for the FTPFileTransferBean to work. The config class is is also annotated @Component so it won't generally have to be specified explicitly as a bean in the client microservice (assuming component scanning is enabled).

Client microservices must simply ensure that the configuration properties referenced in the FTPClientConfig class and the FTPTransferProperties class (which is an aggregation of FTPClientConfig objects with HashMap semantics) are provided in the environment e.g. via Cloud Config Server.

## Apache FTP Client
The library includes an extension to the Apache FTPClient and FTPSClient classes and a factory class to create them. Neither are intended to be used directly by client microservices but instead are used internally to provide certain capabilities over and above those provided out of the box by the Apache library.