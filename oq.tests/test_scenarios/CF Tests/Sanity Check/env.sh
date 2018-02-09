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
export APP_LOCATION="${TEST_RESOURCE_DIRECTORY}/sanity-check"
export EXPECTED_APPLICATIONS="spring-music-web spring-music-news"
export EXPECTED_SERVICES="spring-music-db spring-music-news-external"
