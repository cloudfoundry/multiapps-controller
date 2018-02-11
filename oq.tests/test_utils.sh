if [ -z $ROOT_SCRIPTS_DIR ] ; then
    export ROOT_SCRIPTS_DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd );
fi

if [[ ! -z "${RUNTIME}" && -z "${RT}" ]] ; then
    export RT=${RUNTIME};
fi

function assert_call_was_successful {
    if [ "$?" -ne 0 ]; then
        echo_error "Operation [${1}] failed."
        exit 1
    fi
    echo_info "Operation ${1} call was successful"
}

function assert_are_equal() {
    if [ "$1" != "$2" ]; then
        echo_error "Values were different ! Expected: $1 Actual: $2 ; $3"
        exit 1
    fi
}

function trim() {
    echo "$(echo -e "${1}" | sed -e 's/^[[:space:]]*//' -e 's/[[:space:]]*$//')"
}

function echo_info() {
    echo "[ INFO ] ${1}"
}

function echo_warning() {
    echo "[ WARNING (-) ] ${1}"
}

function echo_error() {
    echo "[ ERROR (!) ] ${1}"
}

function assert_directory_not_empty {
    if [ ! -d ${1} ] || [ ! "$(ls -A ${1})" ]
    then
        echo_error "Directory ${1} does not exist or is empty!"
        exit 1
    fi
}

function create_directory {
    if [ ! -d ${1} ] ; then
        mkdir -p ${1}
        echo_info "creating dir ${1}"
    fi
}

function delete_directory {
    if [ -d ${1} ]
    then
        rm -rfv ${1}
    fi
}

function download_file {
    local url=${1}
    local destination=${2}
    local is_optional=${3}
    
    wget -nv --no-proxy -O "${destination}" "${url}" --no-check-certificate
    if [ $? -ne 0 ]; then
      if [[  "${is_optional}" != "optional" ]]; then
        echo_error "Unable to download '${url}' to '${destination}'"
        return 1;
      fi
    fi
    return 0;
}

function get_apps {
    ${RT} apps 2>&1 | tee ${CONSOLE_OUT_LOCATION}
}


#session will be persisted to current CF_HOME or XSCLIENT_CONTEXTFILE paths
#expects ${1} - recreate_space flag. Values "YES"; "recreateSpace" / "No" anything else
#ORG_NAME
#SPACE_NAME
#DEFAULT_ORG - org targeted for initial login
#DEFAILT_SPACE - space targeted for initial login
#RT_API_ENDPOINT - CC api
#USER_NAME
#USER_PASS
function init_space(){
    local recreate_space="${1}"
    
    ${RT} login -a ${RT_API_ENDPOINT} -u "${USER_NAME}" -p "${USER_PASS}" --skip-ssl-validation -o "${DEFAULT_ORG}" -s "${DEFAULT_SPACE}"
    if [ $? -ne 0 ] ; then
        echo_error "Could not log-in : -a ${RT_API_ENDPOINT} -u ${USER_NAME}"
        return 1;
    fi
    
    ${RT} t -o "${ORG_NAME}" -s "${SPACE_NAME}" 
    local target_rc=$?;
    if [[ "${recreate_space}" != "recreateSpace" ]] ; then
        if [ $target_rc -eq 0 ] ; then 
            return 0;
        else
            echo_error "Targeting -o ${ORG_NAME} -s ${SPACE_NAME} failed and no RECREATE_SPACE is set"
            return 1;
        fi
    fi

    ${RT} space "${SPACE_NAME}";
    if [[ $? -eq 0 ]] && [[ ${target_rc} -eq 0 ]] ; then
        delete_space ${RT} "${SPACE_NAME}"
    fi
    ${RT} create-space "${SPACE_NAME}" -o "${ORG_NAME}"
    ${RT} set-space-role "${USER_NAME}" "${ORG_NAME}" "${SPACE_NAME}" SpaceDeveloper
    ${RT} target -o "${ORG_NAME}" -s "${SPACE_NAME}"
    target_rc=$?
    echo_info "Space ${SPACE_NAME} creation return code: ${target_rc}"
    return ${target_rc};
}

function delete_space(){
    local RT=${1};
    local SPACE_NAME=${2};
	
	clean_space ${RUNTIME} "${SPACE_NAME}"
	
	echo_info "deleting space ${SPACE_NAME}"
    ${RT} delete-space -f "${SPACE_NAME}"
    local delete_rc=$?
	
	return ${delete_rc}
}

function clean_space(){
    local RT=${1};
    local SPACE_NAME=${2};
    
    #because cf cli can't work without a target
    ${RT} target -o "${DEFAULT_ORG}" -s "${DEFAULT_SPACE}"
    ${RT} spaces | grep "^${SPACE_NAME}\s*.*\$"; 
    if [[ $? -ne 0 ]] ;  then
       echo_warning "Space ${SPACE_NAME} not found"
       return 0;
    fi
    ${RT} target -o "${ORG_NAME}" -s "${SPACE_NAME}" 
    local target_rc=$?;
    if [[ ${target_rc} -ne 0 ]] ; then
        echo_error "Could not target ${SPACE_NAME}"
        return 1;
    fi
    delete_space_content ${RT} "${SPACE_NAME}"
    
    local delete_rc=$?	
    return ${delete_rc}
}

function delete_space_content(){
        #if successfully targeted
        local RT=${1};
        local SPACE_NAME=${2};
        
        delete_services ${RT}
        delete_applications ${RT}     
}

function delete_services {
    local RT=${1}
    # Delete leftover services
    services=$(${RT} services | grep -vE 'Getting|Found|name|-------*' | cut -d ' ' -f 1)
    # Create an array
    services=(${services// /})
    echo_info "Services to delete: " ${services[@]}
    for ((x=0; x<${#services[@]}; x++)) ; do
        ${RT} ds ${services[x]} -f || true
    done
}

function delete_applications {

    local RT=${1}
    # Delete leftover applications (TODO why skip the auditlog?)
    applications=$(${RT} apps | grep -vE 'Getting|Found|name|-------*' | cut -d ' ' -f 1)
    # Create an array
    applications=(${applications// /})
    echo_info "Applications to delete: " ${applications[@]}
    for ((x=0; x<${#applications[@]}; x++));
      do
       ${RT} d ${applications[x]} -f || true
      done
}

function find_mtar() {
    local archive_location=${1}
    if [ -z ${archive_location} ]; then
        return;
    fi
    local content_dir=$(pwd);
    if [ -d "${TEST_WORKING_DIRECTORY}" ] ; then
        content_dir=${TEST_WORKING_DIRECTORY}
    fi
    find "${content_dir}/${archive_location}" -name '*.mtar' | head -1
}

function execute_deploy {
    local mta_archive_location=${1}
    local additional_arguments=${2}
    local resume_on_failure=${3}
    echo_info "executing: ${RT} deploy ${mta_archive_location} ${additional_arguments} -f; resume on failure: ${resume_on_failure}"

    if [ "yes" == "${resume_on_failure}" ] ; then
        (rm deploy_pipe) #optional;in sub shell - in order not to fail safe if no pipe exists
        mkfifo deploy_pipe
        coproc resumefd { cat deploy_pipe | grep " to resume the process";}
        ${RT} deploy ${mta_archive_location} ${additional_arguments} -f | tee deploy_pipe | cat 
        if [ ${PIPESTATUS[0]} -ne 0 ] ; then
            echo_error "Execution Failed! Waiting for a minute!"
            read -ru ${resumefd[0]} resume_line
            resume_command=&(echo "${resume_line}" | sed -e "s/Use \"\(.*\)\" to resume the process/\1/")
            sleep 60;
            echo_info "Retrying after a minute with ${resume_command}"
            $resume_command; #calling retry
            assert_call_was_successful "Deploy"
        fi
        kill -9 ${resumefd_PID} # to kill the fork
        rm deploy_pipe #to end the named pipe
    else
        ${RT} deploy ${mta_archive_location} ${additional_arguments} -f
        assert_call_was_successful "Deploy"
    fi
}

function execute_undeploy {
    local mta_id=${1}
    echo "calling ${RT} undeploy ${mta_id} --delete-services --delete-service-brokers --do-not-fail-on-missing-permissions -f"
    ${RT} undeploy ${mta_id} --delete-services --delete-service-brokers --do-not-fail-on-missing-permissions -f

    assert_call_was_successful "Undeploy"
}

function generate_local_executable(){
    echo $@;
    echo "generating local executable for ${1}"
    local original_path=$1
    local script_name=`basename ${original_path}`
    local original_dir=`dirname ${original_path}`
    local script_location="/var/tmp/${script_name}_$(date +"%m-%d-%y_%H-%M-%S").sh"
    echo "#!/bin/bash" > ${script_location}
    export -p | grep -v -E '(PWD)|(OLDPWD)|(SHELL)|(STORAGE)|(CPU)|(tesi)|(custom\.)|(smtp\.server)|(sl_auto)|(skip\.verifyDVDs)|(report\.recipients\.error)|(repo\.root\.dir)|(production\.server\.location)|(dashboard\.purpose)|(custom\.script\.run\.dir)|(ENV\.SL_AUTO_HOST)|(test\.purpose)' >> ${script_location}
    echo "cd $(pwd)" >> ${script_location}
    echo "eval ${original_path}" >> ${script_location}
    chmod a+x ${script_location};
    echo "re-execute script location is ${script_location}"
}

function determine_existing_components_cnt {
    local command_output=${1}; shift
    local expected_component_names=("${@}")

    EXISTING_COMPONENTS_CNT=0
    local component_name
    for component_name in ${expected_component_names[@]}; do
        echo_info "Looking for ${component_name}..."
        local result=$(grep --extended-regexp "(^|\s)${component_name}\s" ${command_output})
        if [ "${result}" != "" ]; then
            echo_info "Component ${component_name} found."
            EXISTING_COMPONENTS_CNT=$((EXISTING_COMPONENTS_CNT+1))
        else
            echo_info "Component ${component_name} not found!"
        fi
    done
}

function assert_components_do_not_exist {
    local component_type=${1}; shift
    local command_output_file=${1}; shift
    local expected_component_names=("${@}")
    echo_info "Asserting the ${component_type} [${expected_component_names[@]}] do not exist in ${command_output_file}..."
    determine_existing_components_cnt ${command_output_file} "${expected_component_names[@]}"

    if [ ${EXISTING_COMPONENTS_CNT} -ne 0 ]; then
        echo_error "Some ${component_type} were found!"
        exit 1
    fi
    echo_info "None of the ${component_type} exist!"
}

function assert_components_exist {
    local component_type=${1}; shift
    local command_output_file=${1}; shift
    local expected_component_names=("${@}")
    local expected_cnt=${#expected_component_names[@]}
    echo_info "Asserting the ${component_type} [${expected_component_names[@]}] exist in ${command_output_file}..."
    determine_existing_components_cnt ${command_output_file} "${expected_component_names[@]}"

    if [ ${EXISTING_COMPONENTS_CNT} -ne ${expected_cnt} ]; then
        echo_error "Not all ${component_type} were found!"
        exit 1
    fi
    echo_info "All ${component_type} exist!"
}

function get_mta_id {
    	local content_dir=${TEST_WORKING_DIRECTORY}
    	local deployment_descriptor_location="${content_dir}/${1}"
    	echo $(trim $(cat ${deployment_descriptor_location} | grep -o "^ID: .*$" | cut -d " " -f2 | tail -1))
}

function find_mta_id_from_file {
    	echo $(trim $(cat ${1} | grep -o "^ID: .*$" | cut -d " " -f2 | tail -1))
}

function cleanup(){
    if [ -z ${MTA_ID} ]; then
        MTA_ID=$(get_mta_id ${APP_LOCATION}/mtad.yaml)
    fi
    ${RT} undeploy ${MTA_ID} --delete-services --delete-service-brokers --do-not-fail-on-missing-permissions -f
    for component_name in ${EXPECTED_APPLICATIONS}; do
        echo_info "deleting ${component_name}"
        ${RT} delete ${component_name} -f
    done
    for service_name in ${EXPECTED_SERVICES}; do
        echo_info "deleting ${service_name}"
        ${RT} delete-service ${service_name} -f
    done
}

