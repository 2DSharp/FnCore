# FriendlyNeighborCore

FNCore provides the core service functionality for the FriendlyNeighbor project. 
It does the heavy lifting for other services with data transfer based on gRPC/Protocol Buffers.

## Build Instructions

If you don't have a copy of the pre-compiled binary, ask me at 2d@twodee.me.

For building the project do the following:

Clone the project
```
git clone https://github.com/2dsharp/FriendlyNeighborCore.git
cd FriendlyNeighborCore.git
```
Once you are done cloning, do either of the following methods.

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
```java -jar target/friendlyneighbor-core-jar-with-dependencies.jar```

### Building with Docker (For consumers)

If you'd like to **use the FnCore API**, run it in a container with **Docker**.

Docker will download all the dependencies for you. Helper scripts are available to run them:

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

The application will be using port *9120* for any communication with other services over gRPC.

Use the `.proto` files at `src/main/proto` to use the API.

The service requires **MongoDB** to be running on its default port for persisting data. 
If you are running on Docker you'll have to setup networking between the database server and FnCore yourself.

### TODO:

* Configurable Mongo connection
* Easy networking with Docker
* Support Java 8+
* Pre-compiled binaries
