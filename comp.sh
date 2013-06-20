#!/bin/bash
STARTTIME=$SECONDS
BUILDDIR=build
JARNAME=build.jar
LIBJAR=gnuprologjava-0.2.6.jar
BUILDCLASSPATH=$LIBJAR
BUILDMAIN=./src/epics/camwin/Main.java
INCLUDECLASSES=`find ./src -name "*.java"`
MANIFESTNAME=MANIFEST.MF
DASH="--------------------------------------"

# If build exists, check for new source files
if [ -f "$JARNAME" ]; then
    NEWER=`find ./src -newer ./build.jar -name "*.java"`
    if [ -z "$NEWER" ]; then
        while true; do
	        read -p "There are no new source files since last compilation. Continue compilation? [y/n] " yn
	        case $yn in
	            [Yy] ) echo "Compiling..."; break;;
	            [Nn] ) echo "Exiting..."; exit;;
	            * ) echo "Please answer y or n";;
	        esac
        done
    else 
        echo "New source files since last compilation: $NEWER"
    fi
fi

function createmanifest {
    if [ -f $MANIFESTNAME ]; then
	rm $MANIFESTNAME
	echo "Removed old manifest"
    fi
    echo "Main-Class: epics.camwin.Main" > $MANIFESTNAME
	echo "Class-Path: $LIBJAR" >> $MANIFESTNAME
    echo "Created new manifest"
}

if [ ! -f $BUILDMAIN ]; then
    echo "Error: could not find Main.java in $BUILDMAIN. Your current directory must be the one containing 'src'. Try again."
    exit
fi

if [ -d $BUILDDIR ]; then
   echo $DASH
   echo "Deleting previous build dir"
   echo $DASH
   rm -r $BUILDDIR
fi

mkdir $BUILDDIR
echo "Compiling source"
javac -sourcepath src -classpath $BUILDCLASSPATH -d $BUILDDIR $INCLUDECLASSES

if [ "$?" -eq 0 ]; then
   echo $DASH
   echo " Compile successful "
   echo $DASH
   #unzip $LIBJAR -d build # Used for fully-packaged jar
   cd build
   
   createmanifest
   jar -cvmf ./$MANIFESTNAME ../$JARNAME .
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

if [ -f $MANIFESTNAME ]; then
	rm $MANIFESTNAME
	echo "Manifest removed"
fi

echo "Done in "$(($SECONDS-$STARTTIME))" seconds"
