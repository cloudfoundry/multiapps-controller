#!/bin/bash
current_dir="$(dirname -- "$(realpath -- "${BASH_SOURCE[0]}")")"
parent_dir="$(dirname -- "${current_dir}")";
if [ -f "${parent_dir}/env.sh" ] ; then
        source "${parent_dir}/env.sh";
fi
export STEP_PATH="${STEP_PATH}/Undeploy"
###TODO! make resolution of mta resources unified - now both 'get_mta_id' and 'find_mtar' are used and both use this variable!  
export APP_LOCATION="${TEST_RESOURCE_DIRECTORY}/sanity-check"
