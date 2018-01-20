export ROOT_SCRIPTS_DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd );

#the resource location is determined by finding the git repo root first
#repo_root/test_scripts/<current script>.sh -> ../../test_resources
#the content from this dir should be copied to the test workign dir before each execution
SCRIPT_RESOURCES_ROOT="$(dirname $(dirname $0))/test_resources"

#Variables inherited from **** execution, which mess-up the nodejs maven build
unset SUDO_GID;
unset SUDO_UID;
unset SUDO_USER;

declare -a MODULES_ARR

OAUTH_TOKEN=""
CSRF_TOKEN=""
API_ENDPOINT=""
API_URL=""

source "${ROOT_SCRIPTS_DIR}/test_utils.sh"

function dummy_test_function(){
  echo_info "DOING SOME IMPORTANT SCENARIO VALIDATION HERE..."
  return 0;
}


