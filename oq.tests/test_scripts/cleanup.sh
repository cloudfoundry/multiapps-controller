export PARENT_DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd );
source "${PARENT_DIR}"/test_functions.sh
generate_local_executable "$0"
set +e
cleanup
exit 0;
