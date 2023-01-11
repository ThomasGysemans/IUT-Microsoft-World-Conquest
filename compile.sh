#!/bin/bash
export SOURCES="."
export CLASSES="classes"
export CLASSPATH=`find lib -name "*.jar" | tr '\n' ':'`

javac -cp ${CLASSPATH} -sourcepath ${SOURCES} -d ${CLASSES} ./*.java
