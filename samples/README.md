# Gluon Maps Mobile Sample

This sample demonstrates how to use Gluon Maps inside a JavaFX application.

## Instructions

To run the application, simply execute the following command from the `samples` directory:

    mvn javafx:run

## Native Image

[GluonFX plugin](https://docs.gluonhq.com/) is used to build a native image for platforms including desktop, android, iOS and embedded.
Please follow the GluonFX prerequisites as stated [here](https://docs.gluonhq.com/#_requirements).

### Desktop

Build a native image using:

    mvn gluonfx:build

Run the native image app:

    mvn gluonfx:nativerun

### Android

Build a native image for Android using:

    mvn gluonfx:build -Ptarget=android

Package the native image as an 'apk' file:

    mvn gluonfx:package -Ptarget=android

Install it on a connected android device:

    mvn gluonfx:install -Ptarget=android

Run the installed app on a connected android device:

    mvn gluonfx:nativerun -Ptarget=android

### iOS

Build a native image for iOS using:

    mvn gluonfx:build -Ptarget=ios

Install and run the native image on a connected iOS device:

    mvn gluonfx:nativeRun -Ptarget=ios

Create an IPA file (for submission to TestFlight or App Store):

    mvn gluonfx:package -Ptarget=ios


