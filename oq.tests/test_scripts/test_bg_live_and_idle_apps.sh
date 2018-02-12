export PARENT_DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd );
source "${PARENT_DIR}"/test_functions.sh
generate_local_executable "$0"

#example input:
#LIVE_APP_NAME="hello-router"
#IDLE_APP_NAME="hello-router-green"
#REST_API_PATH="hello"
#EXPECTED_LIVE_OUTPUT="BLUE"
#EXPECTED_IDLE_OUTPUT="GREEN"
call_app_rest_api "${LIVE_APP_NAME}" "${REST_API_PATH}" "${EXPECTED_LIVE_OUTPUT}"
if [ -n ${IDLE_APP_NAME} ] && [ -n ${EXPECTED_IDLE_OUTPUT} ] ; then
    call_app_rest_api "${IDLE_APP_NAME}" "${REST_API_PATH}" "${EXPECTED_IDLE_OUTPUT}"
fi
