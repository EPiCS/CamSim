#!/bin/sh
BUILDDIR="build"
JARNAME="build.jar"
BUILDCLASSPATH="./gnuprologjava-0.2.6.jar"
BUILDMAIN="./src/epics/camwin/Main.java"
DASH="\n--------------------------------------\n"

if [ -d ./build ]; then
   echo $DASH"Deleting previous build dir"$DASH
   rm -r ./build
fi

mkdir build

javac -sourcepath src -classpath $BUILDCLASSPATH -d $BUILDDIR $BUILDMAIN
if [ "$?" -eq 0 ]; then
   echo $DASH"Compile successful"$DASH
   cd build
   jar -cvf ../$JARNAME .
   JARSUCCESS=$?
   cd ..
else
   echo $DASH"Compile failed"$DASH
fi

if [ "$JARSUCCESS" -eq 0 ]; then
    echo $DASH"Jar built. Setting perms..."$DASH
    chmod u+x ./$JARNAME
fi

echo "Done."