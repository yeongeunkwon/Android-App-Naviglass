# Naviglass Android Application

## Introduction
This Android application is one component of a team project named Naviglass. The app complements a pair of virtual reality eye-glasses and a Raspberry Pi attached to the glasses, that were engineered for the project Naviglass. The app was developed by myself, while three other people on my team programmed the Raspberry Pi and designed hardware of the glasses. 

People often rely on map applications on their devices to reach their destination. The intention of Naviglass was to provide an alternative option of navigating through an area that would not require looking down at phones for directions as much, and instead allow users to focus more on the real-life view ahead. 

The app shows a map showing current location and route to the destination to the user. It also sends data containing directions, user's location, etc. to the Raspberry Pi via a Bluetooth connection. The Raspberry Pi and the glasses use image recognition and data sent from the app to display the real-life view ahead to the user wearing the glasses. 

## Documentation
Please refer to the [wiki](https://github.com/yeongeunkwon/Android-App-Naviglass/wiki) for detailed information on functionality and how to use the application. 

## Installation 
For testing and verification, you can download this repository and use Android Studio to generate the app. Alternatively, you can install the app on your phone using the app's [APK](/app/build/outputs/apk/debug/app-debug.apk). 

**Prerequisites**

* Android phone with OS 4.4 (KitKat) or higher (which are 98% of Android devices) 
* Android Studio on your computer

**Getting Started**

To generate application from repository files: 
1. Download the repository to your computer. 
1. On Android Studio, click **File -> Open** and open the folder you downloaded. If a pop-up asks, you can either open on a new window or replace the project on the existing window. 
1. Wait 1-2 minutes for gradle to build configuration. The configuration "app" should show on the top menu: ![configuration](https://user-images.githubusercontent.com/46125838/82724739-ab30c780-9d1b-11ea-8178-065473826559.PNG) 
1. Follow **Run on a real device** section of this [guide](https://developer.android.com/training/basics/firstapp/running-app#RealDevice). An app called "Naviglass" should be installed to your phone. 
    * Make sure that Google Play Services on your phone is up to date. 
    * Note: Application will not run on the Android Studio emulator, as the emulator does not support Bluetooth. 

To install application from APK: 
1. Follow "Set up your device as follows:" of the section **Run on a real device** of this [guide](https://developer.android.com/training/basics/firstapp/running-app#RealDevice). 
1. Download the [APK](/app/build/outputs/apk/debug/app-debug.apk) to your phone. 
1. Click on the APK file to install the Naviglass app. 
