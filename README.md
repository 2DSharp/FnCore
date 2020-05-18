# FriendlyNeighborCore
[![Build Status](https://api.cirrus-ci.com/github/2DSharp/FriendlyNeighborCore.svg)](https://cirrus-ci.com/github/2DSharp/FriendlyNeighborCore)

FNCore provides the core service functionality for the FriendlyNeighbor project. 
It does the heavy lifting for other services with data transfer based on gRPC/Protocol Buffers.

## Getting started

To get started with FnCore you need to obtain a binary of the FnCore build. They are available at
the [releases](https://github.com/2DSharp/FriendlyNeighborCore/releases) section. You can use the jar
file to directly run the application with the default configuration. 
(Custom configuration will be available in future releases)

To run, you need to need to have **Java 8 (Not above)** installed.

```java -jar fncore-0.2.0-rc-alpha.3.jar```


If you'd like to build from source, follow along:

## Build Instructions

For building the project do the following:

Clone the project
```
git clone https://github.com/2dsharp/FriendlyNeighborCore.git
cd FriendlyNeighborCore/
```

### Configuration

To choose a custom port number, server name etc. head over to the [configuration.properties](https://github.com/2DSharp/FriendlyNeighborCore/blob/master/src/main/resources/config.properties) file in the `src/main/resources/` directory 
and change the configuration to your preferences.

Once you are done configuring, do either of the following methods.

### Building with Maven (For developers)

If you'd like to change something on the service itself and test it on your local machine, 
I'd personally prefer this method since it can use your local `.m2` repository.

#### Build Requirements

* **Java 8** (Not above, a core dependency (Guice) doesn't have proper support for 8+ yet)
* **Apache Maven**

#### Steps

If you already have maven installed in your system, you can compile the source to generate a binary.

* `mvn clean package` to build the system. It will run all the tests in the project. 
There may be cases where a test may fail because of a bug in one of our testing libraries with mongo.
Run again and the issue should be gone. If it persists, create an issue.

* Run the generated binary with:
```java -jar fncore-0.2.0-rc-alpha.3.jar```

### Building with Docker (For consumers)

If you'd like to **use the FnCore API**, run it in a container with **Docker**.
Docker will download all the dependencies for you. Helper scripts are available to run them:

**Warning:** You will need to set Mongo's `mongo.database` property in `configuration.properties`
to a proper hostname/IP Address for the service to recognize from inside the container.

By default it looks for Mongo inside the container itself and will fail.

```
# Make sure the scripts are executable
sudo chmod +x build.sh
sudo chmod +x run.sh

# Build the project, this will create a container for you exposing port 9120
./build.sh

# Run the service
./install
```
---

The service by default will be using port *9120* for any communication with other services over gRPC.

Use the `.proto` files at `src/main/proto` to use the API.

By default, the service requires **MongoDB** to be running on its default port for persisting data. 
If you are running on Docker you'll have to setup networking between the database server and FnCore yourself.

### TODO:

* [X] Configurable Mongo connection
* [X] Easy networking with Docker
* [ ] Support Java 8+
* [X] Pre-compiled binaries
* [ ] Add build script for configuration
