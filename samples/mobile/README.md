# Gluon Maps Mobile Sample

This sample demonstrates how to use Gluon Maps inside a JavaFX application.

## Instructions

We use [GluonFX plugin](https://docs.gluonhq.com/) to build a native image for platforms including desktop, android and iOS.
Please follow the GluonFX prerequisites as stated [here](https://docs.gluonhq.com/#_requirements).

### Desktop

Run the application using:

    ./gradlew run

Build a native image using:

    ./gradlew nativeBuild

Run the native image app:

    ./gradlew nativeRun

### Android

Build a native image for Android using:

    ./gradlew nativeBuild -Ptarget=android

Package the native image as an 'apk' file:

    ./gradlew nativePackage -Ptarget=android

Install it on a connected android device:

    ./gradlew nativeInstall -Ptarget=android

Run the installed app on a connected android device:

    ./gradlew nativeRun -Ptarget=android

### iOS

Build a native image for iOS using:

    ./gradlew nativeBuild -Ptarget=ios

Install and run the native image on a connected iOS device:

    ./gradlew nativeRun -Ptarget=ios

Create an IPA file (for submission to TestFlight or App Store):

    ./gradlew nativePackage -Ptarget=ios


