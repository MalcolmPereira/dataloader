#! /usr/bin/env sh

java -Xmx1024m -XX:+UseG1GC -XX:ActiveProcessorCount=2  -XX:ActiveProcessorCount=2 -Dcom.sun.management.jmxremote.port=1101 -Dcom.sun.management.jmxremote -Dcom.sun.management.jmxremote.authenticate=false -Dcom.sun.management.jmxremote.ssl=false  -jar ./target/dataloader-app-0.0.1-SNAPSHOT.jar
