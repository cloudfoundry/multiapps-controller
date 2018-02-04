#!/bin/bash
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
echo "Looking for output directory: ${2}"
if [ ! -d "${2}" ] ; then
    echo "ERROR not found!"
    exit 1;
fi
cp -rv ${1}/bin/* ${2}/
echo "Done"