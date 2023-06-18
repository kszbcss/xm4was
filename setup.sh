#!/bin/bash

set -e

rm -rf $HOME/was
mkdir -p $HOME/was

# Populate P2 repository
mvn -B -f buildutils clean package
cp -r buildutils/target/p2_repo/ $HOME/was/

# Install WebSphere libraries
mkdir -p $HOME/was/libs
docker run --rm -u $(id -u $USER) -v $HOME/was:/was:z ibmcom/websphere-traditional:8.5.5.17 cp /opt/IBM/WebSphere/AppServer/lib/bootstrap.jar /opt/IBM/WebSphere/AppServer/lib/j2ee.jar /was/libs
mvn -B -f buildutils install:install-file -DgroupId=websphere-library -DartifactId=bootstrap -Dversion=8.5.5.17 -Dpackaging=jar -Dfile=$HOME/was/libs/bootstrap.jar
mvn -B -f buildutils install:install-file -DgroupId=websphere-library -DartifactId=j2ee -Dversion=8.5.5.17 -Dpackaging=jar -Dfile=$HOME/was/libs/j2ee.jar

# Extract IBM JDK
docker run --rm -u $(id -u $USER) -v $HOME/was:/was:z ibmcom/websphere-traditional:8.5.5.17 cp -R /opt/IBM/WebSphere/AppServer/java /was
