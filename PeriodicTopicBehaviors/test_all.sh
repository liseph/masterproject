#!/bin/bash

echo Number of documents: $1
echo Input file: $2

java -jar PeriodicTopicBehaviors-1.0-SNAPSHOT.jar 0 1 $1 11 $2 lpta.txt
java -jar PeriodicTopicBehaviors-1.0-SNAPSHOT.jar 1 1 $1 11 $2 periodica.txt
java -jar PeriodicTopicBehaviors-1.0-SNAPSHOT.jar 2 1 $1 11 $2 psta.txt

java -jar PeriodicTopicBehaviors-1.0-SNAPSHOT.jar 0 2 $1 11 $2 lpta.txt
java -jar PeriodicTopicBehaviors-1.0-SNAPSHOT.jar 1 2 $1 11 $2 periodica.txt
java -jar PeriodicTopicBehaviors-1.0-SNAPSHOT.jar 2 2 $1 11 $2 psta.txt

