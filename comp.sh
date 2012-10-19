#!/bin/sh
BUILDDIR="build"
JARNAME="build.jar"
BUILDCLASSPATH="./gnuprologjava-0.2.6.jar"
BUILDMAIN="./src/epics/camwin/Main.java"
MANIFESTNAME="MANIFEST.MF"
DASH="\n--------------------------------------\n"

#function createmanifest {
#    if [ -f $MANIFESTNAME ]; then
#	rm $MANIFESTNAME
#	echo "Removed old manifest"
#    fi
#    echo "Main-Class: epics.camwin.Main" > $MANIFESTNAME
#    echo "Created new manifest"
#}

if [ -d $BUILDDIR ]; then
   echo $DASH"Deleting previous build dir"$DASH
   rm -r .$BUILDDIR
fi

mkdir $BUILDDIR
javac -sourcepath src -classpath $BUILDCLASSPATH -d $BUILDDIR $BUILDMAIN

if [ "$?" -eq 0 ]; then
   echo $DASH"Compile successful"$DASH
   unzip gnuprologjava-0.2.6.jar -d build
   cd build

   if [ -f $MANIFESTNAME ]; then
       rm $MANIFESTNAME
       echo "Removed old manifest"
   fi
   echo "Main-Class: epics.camwin.Main" > $MANIFESTNAME
   echo "Created new manifest"

   jar -cvmf ../$MANIFESTNAME ../$JARNAME .
   JARSUCCESS=$?
   cd ..

   if [ "$JARSUCCESS" -eq 0 ]; then
       echo $DASH"Jar built. Setting perms..."$DASH
       chmod u+x ./$JARNAME
   fi
else
   echo $DASH"Compile failed"$DASH
fi

if [ -d $BUILDDIR ]; then
    rm -r $BUILDDIR
    echo "Build directory deleted"
fi

echo "Done."