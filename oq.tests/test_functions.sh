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

function test_deploy() {
    local temp_directory_location="temp-${RANDOM}"
    local apps_output_location="${temp_directory_location}/${RT}-apps-output.txt"
    local services_output_location="${temp_directory_location}/${RT}-services-output.txt"
    local service_brokers_output_location="${temp_directory_location}/${RT}-service-brokers-output.txt"
    create_directory ${temp_directory_location}

    execute_deploy "${MTAR_LOCATION}" "${ADDITIONAL_OPTIONS}" "${RESTART_ON_FAILURE}"

    ${RT} apps > ${apps_output_location}
    assert_components_exist applications ${apps_output_location} ${EXPECTED_APPLICATIONS}
    ${RT} services > ${services_output_location}
    assert_components_exist services ${services_output_location} ${EXPECTED_SERVICES}
    ${RT} service-brokers > ${service_brokers_output_location}
    assert_components_exist service-brokers  ${service_brokers_output_location} ${EXPECTED_SERVICE_BROKERS}

    delete_directory ${temp_directory_location}
}
