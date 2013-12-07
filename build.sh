#!/bin/bash

# ctags -R src --exclude=target --exclude=vendor
# mvn clean scala:compile scala:testCompile
# mvn clean resources:resources resources:testResources scala:compile scala:testCompile surefire:test

#!/bin/bash

echo "Tips:"
echo "-c compile"
echo "-t test"
echo "-a all"

while getopts "b:cta" arg #选项后面的冒号表示该选项需要参数
do
	case $arg in
		c)
			ctags -R src --exclude=target --exclude=vendor
			mvn clean scala:compile scala:testCompile
			;;
		t)
			mvn resources:resources resources:testResources surefire:test
			;;
		a)
			ctags -R src --exclude=target --exclude=vendor
			mvn clean scala:compile scala:testCompile resources:resources resources:testResources surefire:test
			;;
		b)
			echo "b's arg:$OPTARG" #参数存在$OPTARG中
			;;
		?)  #当有不认识的选项的时候arg为?
			exit 1
			;;
	esac
done
