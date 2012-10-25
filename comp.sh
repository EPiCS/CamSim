#!/bin/bash
STARTTIME=$SECONDS
ROOTDIR=~/code/OGAMOTUMBNDSC
BUILDDIR=build
JARNAME=build.jar
BUILDCLASSPATH=./gnuprologjava-0.2.6.jar
BUILDMAIN=./src/epics/camwin/Main.java
MANIFESTNAME=MANIFEST.MF
DASH="--------------------------------------"

function createmanifest {
    if [ -f $MANIFESTNAME ]; then
	rm $MANIFESTNAME
	echo "Removed old manifest"
    fi
    echo "Main-Class: epics.camwin.Main" > $MANIFESTNAME
    echo "Created new manifest"
}

cd $ROOTDIR

if [ -d $BUILDDIR ]; then
   echo $DASH
   echo "Deleting previous build dir"
   echo $DASH
   rm -r .$BUILDDIR
fi

mkdir $BUILDDIR
javac -sourcepath src -classpath $BUILDCLASSPATH -d $BUILDDIR $BUILDMAIN

if [ "$?" -eq 0 ]; then
   echo $DASH
   echo " Compile successful "
   echo $DASH
   unzip gnuprologjava-0.2.6.jar -d build
   cd build
   
   createmanifest

   jar -cvmf ../$MANIFESTNAME ../$JARNAME .
   JARSUCCESS=$?
   cd ..

   if [ "$JARSUCCESS" -eq 0 ]; then
       echo $DASH
       echo "Jar built. Setting perms..."
       echo $DASH
       chmod u+x ./$JARNAME
   fi
else
   echo $DASH"Compile failed"$DASH
fi

if [ -d $BUILDDIR ]; then
    rm -r $BUILDDIR
    echo "Build directory deleted"
fi

echo "Done in "$(($SECONDS-$STARTTIME))"seconds"
