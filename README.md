# FriendlyNeighborCore
[![Build Status](https://api.cirrus-ci.com/github/2DSharp/FriendlyNeighborCore.svg)](https://cirrus-ci.com/github/2DSharp/FriendlyNeighborCore)
![Maven Build](https://github.com/2DSharp/FriendlyNeighborCore/workflows/Maven%20Build/badge.svg)

FNCore provides the core service functionality for the FriendlyNeighbor project. 
It does the heavy lifting for other services with data transfer based on gRPC/Protocol Buffers.

## Getting started

To get started with FnCore you need to obtain a binary of the FnCore build. They are available at
the [releases](https://github.com/2DSharp/FriendlyNeighborCore/releases) section. You can use the jar
file to directly run the application. 

To run, you need to need to have **Java 8 (Not above)** installed. 
It is recommended to customize the `fnconfig.examples.properties` according to your needs and use it in the following way:

If you already have an `fnconfig.properties` file in the directory, run:

```java -jar fncore-0.3.1.jar```

To use a `properties` file from another directory, add the full path as an argument:

```java -jar fncore-0.3.1.jar /home/fnconfig.properties```

This will try to read the specified properties file based on availability. If it does fail or has missing values, the server will read from the defaults.

The properties file should also include a secret account service key file in json format that can be obtained from FCM admin console.

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

* `mvn clean package` to build the system. It will run all the tests in the project and verify everything's alright. If
the tests fail, it probably is because there was already a mongo or redis server running on your machine. Turn them off
and run `mvn clean package` once again.
A binary will be generated at `target/`, enter the directory.

* Run the generated binary with:
```java -jar  fncore-0.1.0.jar```

### Building with Docker (For consumers)

If you'd like to **use the FnCore API**, run it in an isolated container with **Docker** without having to set up maven
on your own system. This is advantageous if you want to run mongo/redis inside the container.
Docker will download all the dependencies for you. Helper scripts are available to run them:

**Warning:** The docker build by default uses port 9120 and uses the defaults specified 
in `src/main/resources/configuration.properties`. You can override them by changing the file before building
or **passing a custom `fnconfig.properties` to `/home/app/` with the defaults you want (recommended)**. Needless to say that if you
change the default `server.port` variable you have to change the Dockerfile to expose the port you want.

To continue with the defaults, just override the mongo/redis connection options and the rest are better left untouched.


```
# Make sure the scripts are executable
sudo chmod +x build.sh
sudo chmod +x fncore

# Build the project, this will create a container for you exposing port 9120
./build.sh

# Run the service
./fncore
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
