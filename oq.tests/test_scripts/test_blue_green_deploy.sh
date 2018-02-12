export PARENT_DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd );
source "${PARENT_DIR}"/test_functions.sh
generate_local_executable "$0"

MTAR_LOCATION="$(find_mtar $APP_LOCATION)"
test_blue_green_deploy
