#!/usr/bin/env bash

mvn clean package -DskipTests
rm -rf old-user-list-db
echo 
echo 'Read data from id.txt ..'
java -cp target/ext-processor-identify-old-users-0.1.jar cn.sensorsdata.sample.CreateDbUtils < id.txt
echo
sleep 3
tar zcvf old-user-list-db.tar.gz old-user-list-db
mv old-user-list-db.tar.gz src/main/resources
mvn clean package -DskipTests
