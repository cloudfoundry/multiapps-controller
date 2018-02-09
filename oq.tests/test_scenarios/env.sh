#!/bin/bash
current_dir="$(dirname -- "$(realpath -- "${BASH_SOURCE[0]}")")"
parent_dir="$(dirname -- "${current_dir}")";
if [ -f "${parent_dir}/env.sh" ] ; then
  source "${parent_dir}/env.sh";
else 
  export TEST_REPO_ROOT="${parent_dir}"
		
fi

#####CUSTOM CODE:
export proxy_ip=$(env | grep https_proxy |  sed -e "s/https_proxy=http:\/\/\(.*\):8080/\1/" | xargs getent hosts | cut -d " " -f 1)
echo "proxy ip is ${proxy_ip}"
if [ ! -z ${proxy_ip} ] ; then
    echo "original https_proxy ${https_proxy}"
    export https_proxy="http://${proxy_ip}:8080"
    echo "new https_proxy ${https_proxy}"
fi
#####END CUSTOM CODE

export STEP_PATH="${STEP_PATH}/ROOT"
export TEST_RESOURCE_DIRECTORY="${TEST_WORKING_DIRECTORY}/test_resources"

