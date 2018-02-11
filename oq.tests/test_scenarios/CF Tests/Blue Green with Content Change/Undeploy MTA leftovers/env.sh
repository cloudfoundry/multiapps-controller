#!/bin/bash
current_dir="$(dirname -- "$(realpath -- "${BASH_SOURCE[0]}")")"
parent_dir="$(dirname -- "${current_dir}")";
if [ -f "${parent_dir}/env.sh" ] ; then
        source "${parent_dir}/env.sh";
fi
export STEP_PATH="${STEP_PATH}/Undeploy MTA leftovers"
export APP_LOCATION="bg-archives"
export EXPECTED_APPLICATIONS="hello-router-blue hello-backend-blue hello-router-green hello-backend-green hello-router hello-backend"
