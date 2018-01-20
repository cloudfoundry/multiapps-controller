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

