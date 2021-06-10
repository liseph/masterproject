#!/bin/bash

echo Number of documents max: $1
echo Number of documents for topic variation: $2
echo Number of topics for document variation: $3
echo Input file: $4

java -jar PeriodicTopicBehaviors-1.0-SNAPSHOT.jar 0 1 $2 11 2 $4 lpta.txt
java -jar PeriodicTopicBehaviors-1.0-SNAPSHOT.jar 1 1 $2 11 2 $4 periodica.txt
java -jar PeriodicTopicBehaviors-1.0-SNAPSHOT.jar 2 1 $2 11 2 $4 psta.txt

java -jar PeriodicTopicBehaviors-1.0-SNAPSHOT.jar 0 2 $1 11 $3 $4 lpta.txt
java -jar PeriodicTopicBehaviors-1.0-SNAPSHOT.jar 1 2 $1 11 $3 $4 periodica.txt
java -jar PeriodicTopicBehaviors-1.0-SNAPSHOT.jar 2 2 $1 11 $3 $4 psta.txt

