#!/bin/bash
ctags -R . --exclude=target --exclude=vendor
mvn clean scala:compile scala:testCompile surefire:test

