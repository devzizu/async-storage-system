# Run storage server and client programs with maven

STORAGESERVER_MAIN=app.server.StorageServer
CLIENT_MAIN=app.client.Client

# Compile all code
compile:
	mvn compile
	mvn clean install

# Build package
package:
	mvn package

# Run storage server main
run_storageserver:
	@clear
	@read -p "arguments: " args; \
	mvn exec:java -Dexec.mainClass=$(STORAGESERVER_MAIN) -Dexec.args="$$args"

# Run client (arguments = clientID, e.g. id = 1 means port = 10000 + 1)
run_client:
	@clear
	@read -p "arguments: " args; \
	mvn exec:java -Dexec.mainClass=$(CLIENT_MAIN) -Dexec.args="$$args"
