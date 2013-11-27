#!/bin/bash
ctags -R src --exclude=target --exclude=vendor
mvn clean scala:compile scala:testCompile surefire:test

