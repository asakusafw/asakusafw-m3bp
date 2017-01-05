#!/bin/bash
#
# Copyright 2011-2017 Asakusa Framework Team.
#
# Licensed under the Apache License, Version 2.0 (the "License");
# you may not use this file except in compliance with the License.
# You may obtain a copy of the License at
#
#     http://www.apache.org/licenses/LICENSE-2.0
#
# Unless required by applicable law or agreed to in writing, software
# distributed under the License is distributed on an "AS IS" BASIS,
# WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
# See the License for the specific language governing permissions and
# limitations under the License.
#

set -e

LIBDIR=$1
M3BPJNI_LIBFILE=libm3bpjni.dylib

# If libm3bpjni.dylib does not exist, nothing to do in this script,
# for example, on Mac OS X but cross compiling for Linux.
if [ ! -e ${LIBDIR}/${M3BPJNI_LIBFILE} ]
then
   exit 0
fi

if [ -z ${BOOST_ROOT} ]
then
    echo "$BOOST_ROOT MUST be set to build asakusafw-m3bp on Mac OS X."
    exit 1
fi

M3BP_LIBPATH=`ls ${LIBDIR}/libm3bp.*.dylib`
M3BP_LIBNAME=$(basename ${M3BP_LIBPATH})

cp ${BOOST_ROOT}/lib/libboost_*.dylib ${LIBDIR}

for f in ${LIBDIR}/libboost*.dylib
do
    for component in log thread system date_time log_setup filesystem regex chrono atomic
    do
        install_name_tool -change libboost_${component}.dylib \
                          @rpath/libboost_${component}.dylib ${f}
    done
done

install_name_tool -id "@rpath/${M3BPJNI_LIBFILE}" \
                  -change ${M3BP_LIBNAME} \
                          @rpath/${M3BP_LIBNAME} \
                  -add_rpath "@loader_path/." \
                  ${LIBDIR}/${M3BPJNI_LIBFILE}

install_name_tool -id "@rpath/${M3BP_LIBNAME}" \
                  -add_rpath "@loader_path/." \
                  ${LIBDIR}/${M3BP_LIBNAME}

for component in log thread system date_time log_setup filesystem regex chrono atomic
do
    install_name_tool -change libboost_${component}.dylib \
                      @rpath/libboost_${component}.dylib \
                      ${LIBDIR}/${M3BP_LIBNAME}
done

echo "SUCCESS: mac_post_compile"
