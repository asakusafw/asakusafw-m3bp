#!/bin/bash

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

install_name_tool -change ${M3BP_LIBNAME} @rpath/${M3BP_LIBNAME} ${LIBDIR}/${M3BPJNI_LIBFILE}

for component in log thread system date_time log_setup filesystem regex chrono atomic
do
    install_name_tool -change libboost_${component}.dylib \
                      @rpath/libboost_${component}.dylib \
                      ${LIBDIR}/${M3BP_LIBNAME}
done

echo "SUCCESS: mac_post_compile"
