SCRIPT_DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd );

#TODO - these probably are no longer set
#Variables inherited from **** execution, which mess-up the nodejs maven build
unset SUDO_GID;
unset SUDO_UID;
unset SUDO_USER;

declare -a MODULES_ARR

OAUTH_TOKEN=""
CSRF_TOKEN=""
API_ENDPOINT=""
API_URL=""

source "${SCRIPT_DIR}/test_utils.sh"

function dummy_test_function(){
  echo_info "DOING SOME IMPORTANT SCENARIO VALIDATION HERE..."
  return 0;
}

function test_blue_green_deploy() {
    local temp_location="temp-${RANDOM}"
    create_directory ${temp_location}
    local action="${SLP_ACTION}"
    if [ -z "${action}" ]; then
      action="resume"
    fi

    execute_blue_green_deploy "${MTAR_LOCATION}" "${ADDITIONAL_OPTIONS}" | tee ${temp_location}/bg-deploy-output.txt
    grep -q "Process finished" ${temp_location}/bg-deploy-output.txt;
    local finished=$0 #if found(fnished)->0 if not finished -> 1;
    if  [ $finished != 0 ] && [ "${action}" != "none" ] ; then #if not finished and SLP_ACTION is not 'none'
        execute_action_on_process ${temp_location}/bg-deploy-output.txt "${action}"
        assert_call_was_successful "Resume"
    fi
    ${RT} a > ${temp_location}/xs-a-output.txt
    assert_components_exist applications ${temp_location}/xs-a-output.txt ${EXPECTED_APPLICATIONS}
    assert_components_do_not_exist applications ${temp_location}/xs-a-output.txt ${UNEXPECTED_APPLICATIONS}
    ${RT} s > ${temp_location}/xs-s-output.txt
    assert_components_exist services ${temp_location}/xs-s-output.txt ${EXPECTED_SERVICES}

    delete_directory ${temp_location}
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

function test_undeploy() {
    if [ -z ${MTA_ID} ]; then
        MTA_ID=$(get_mta_id ${APP_LOCATION}/mtad.yaml)
        echo_info "MTA ID detected from app location: ${APP_LOCATION} is ${MTA_ID}"
    fi
    local temp_directory_location="temp-${APP_LOCATION}"
    local apps_output_location="${temp_directory_location}/${RT}-apps-output.txt"
    local mtas_output_location="${temp_directory_location}/${RT}-mtas-output.txt"
    local services_output_location="${temp_directory_location}/${RT}-services-output.txt"
    local service_brokers_output_location="${temp_directory_location}/${RT}-service-brokers-output.txt"
    create_directory ${temp_directory_location}

    execute_undeploy ${MTA_ID}
    
    ${RT} apps > ${apps_output_location}
    assert_components_do_not_exist apps ${apps_output_location} ${EXPECTED_APPLICATIONS}
    ${RT} services > ${services_output_location}
    assert_components_do_not_exist services ${services_output_location} ${EXPECTED_SERVICES}
    ${RT} service-brokers > ${service_brokers_output_location}
    assert_components_do_not_exist service-brokers  ${service_brokers_output_location} ${EXPECTED_SERVICE_BROKERS}
    ${RT} mtas > ${mtas_output_location}
    assert_components_do_not_exist MTAs ${mtas_output_location} ${MTA_ID}

    delete_directory ${temp_directory_location}
}
