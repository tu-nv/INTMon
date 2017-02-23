#!/bin/bash
# clean build project
mvn clean install

onos-app localhost deactivate org.onosproject.intmon
onos-app localhost uninstall org.onosproject.intmon
onos-app localhost install target/intmon-1.0-SNAPSHOT.oar
onos-app localhost activate org.onosproject.intmon
