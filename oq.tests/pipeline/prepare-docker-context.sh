#!/bin/bash
echo "Usage: ${0} <go-code-sources-dir> <go-code-package> <additional-image-content-dir> <docker-context-dir>"

echo "Looking for cf cli plugin project directory: ${1}"
if [ ! -d "${1}" ] ; then
    echo "ERROR: not found!"
    exit 1;
fi

go_source_directory="${GOPATH}/src/${2}"
mkdir -pv "$(dirname ${go_source_directory})"
cp -rv "${1}"  "${go_source_directory}"
(cd ${go_source_directory} ; go get -d ./...)

/bin/bash "${go_source_directory}/build.sh" "0.0.$(date +%s)"
if [ $? -ne 0 ] ; then
   echo "ERROR: build failed!"
   exit 1;
fi

echo "Looking for output directory: ${go_source_directory}/mta_plugin_linux_amd64 "
if [ ! -f ${go_source_directory}/mta_plugin_linux_amd64 ] ; then 
	echo "Note found"
    exit 1;	
fi

echo "Looking for output directory: ${4}"
if [ ! -d "${4}" ] ; then
    mkdir -pv ${4}
fi

cp -rv ${go_source_directory}/mta_plugin_linux_amd64 ${4}/
cp -rv ${3}/test_scripts ${4}/test_scripts
cp -rv ${3}/test_resources ${4}/test_resources
cp -rv ${3}/test_scenarios ${4}/test_scenarios
cp -rv ${3}/*.sh ${4}/
cp -rv ${3}/Dockerfile ${4}/

echo "Done"