#!/bin/bash

set -eu

cd "$(dirname "$0")/.."

tcpath="$PWD/target/classes"
ttcpath="$PWD/target/test-classes"
if [ "$OSTYPE" != "cygwin" ]; then
  sep=":"
else
  sep=";"
  tcpath="$(cygpath -aw "$tcpath")"
  ttcpath="$(cygpath -aw "$ttcpath")"
fi

# find MD5 sum command.
md5sum=""
for cmd in md5sum md5; do
  if which "$cmd" 2>&1 > /dev/null; then
    md5sum="$cmd"
    break
  fi
done
if [ -z "$md5sum" ]; then
  echo "Missing MD5 sum command."
  echo "Abort."
  exit 1
fi

# generate classpath from pom.xml.
mkdir -p "tmp"
cp_conf="tmp/cp.conf"
pom_xml_md5="tmp/pom.xml.md5"
$md5sum pom.xml > "$pom_xml_md5.new"
if [ -f "$cp_conf" -a -f "$pom_xml_md5" ] &&
     cmp "$pom_xml_md5.new" "$pom_xml_md5" > /dev/null; then
  rm -f "$pom_xml_md5.new"
else
  echo "Update classpath."
  mvn -Dmdep.outputFile="$cp_conf" dependency:build-classpath
  mv "$pom_xml_md5.new" "$pom_xml_md5"
fi
echo "${tcpath}${sep}${ttcpath}${sep}$(< "$cp_conf")"
