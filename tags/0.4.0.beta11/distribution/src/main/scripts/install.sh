#!/bin/sh
set -e
if [ -z "$1" ]; then
  echo "Usage: $0 <WAS install directory>"
  exit 1
fi
DIST_DIR=$(dirname $0)
WAS_INSTALL_DIR=$1
echo "Discovering WAS installation ..."
if [ ! -d "$WAS_INSTALL_DIR" ]; then
  echo "$WAS_INSTALL_DIR doesn't exist or is not a directory"
  exit 1
fi
if [ ! -w "$WAS_INSTALL_DIR/plugins" ]; then
  echo "$WAS_INSTALL_DIR/plugins is not writable"
  exit 1
fi
MANAGE_PROFILES=$WAS_INSTALL_DIR/bin/manageprofiles.sh
if [ ! -x "$MANAGE_PROFILES" ]; then
  echo "manageprofiles.sh command not found"
  exit 1
fi
PROFILES=$($MANAGE_PROFILES -listProfiles | sed -e 's/[][,]//g')
PROFILE_PATHS=
for PROFILE_NAME in $PROFILES; do
  PROFILE_PATH=$($MANAGE_PROFILES -getPath -profileName $PROFILE_NAME)
  if [ ! -O $PROFILE_PATH/configuration ]; then
    echo "$PROFILE_PATH/configuration doesn't belong to the current user; not continuing."
    exit 1
  fi
  if [ -z "$PROFILE_PATHS" ]; then
    PROFILE_PATHS=$PROFILE_PATH
  else
    PROFILE_PATHS="$PROFILE_PATHS $PROFILE_PATH"
  fi
done
echo "  Install directory: $WAS_INSTALL_DIR"
echo "  Profiles: $PROFILE_PATHS"
echo "Installing plugins ..."
rm -f $WAS_INSTALL_DIR/plugins/com.googlecode.xm4was.*.jar
for PLUGIN in $DIST_DIR/plugins/*.jar; do
  echo "  $(basename $PLUGIN)"
  cp $PLUGIN $WAS_INSTALL_DIR/plugins/
done
echo "Cleaning OSGi caches ..."
for PROFILE_PATH in $PROFILE_PATHS; do
  $PROFILE_PATH/bin/osgiCfgInit.sh
done
