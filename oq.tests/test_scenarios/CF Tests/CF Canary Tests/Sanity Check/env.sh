#!/bin/bash
current_dir="$(dirname -- "$(realpath -- "${BASH_SOURCE[0]}")")"
parent_dir="$(dirname -- "${current_dir}")";
if [ -f "${parent_dir}/env.sh" ] ; then
        source "${parent_dir}/env.sh";
fi
RECREATE_SPACE="YES"
export STEP_PATH="${STEP_PATH}/Sanity Check"
#export DEPLOY_SERVICE_URL="deploy-service.<landscape>"

export SPACE_NAME="sanity-check-poc"
export APP_LOCATION="sanity-check"
export EXPECTED_APPLICATIONS="sanity-check-module"
export EXPECTED_SERVICES="sanity-check-service"
