export PARENT_DIR=$( cd "$( dirname "${BASH_SOURCE[0]}" )" && pwd );
source $(dirname "${PARENT_DIR}")/test_functions.sh
generate_local_executable "$0"
if [[ -z "${MTAR_LOCATION}" ]]; then
  MTAR_LOCATION="$(find_mtar $APP_LOCATION)"
else
  find "${MTAR_LOCATION}" -name '*.mtar' | head -1
fi
echo "MTAR_LOCATION found ${MTAR_LOCATION}"
test_deploy
