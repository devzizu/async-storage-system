
STORAGESERVER_MAIN=app.server.StorageServer
CLIENT_MAIN=app.client.Client

compile:
	mvn compile
	mvn clean install
	
package:
	mvn package

run_storageserver:
	@clear
	@read -p "arguments: " args; \
	mvn exec:java -Dexec.mainClass=$(STORAGESERVER_MAIN) -Dexec.args="$$args"

run_client:
	@clear
	@read -p "arguments: " args; \
	mvn exec:java -Dexec.mainClass=$(CLIENT_MAIN) -Dexec.args="$$args"
