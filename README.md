# Naviglass Android Application

## Introduction
**About this repository**

This repository contains code for an Android application that was one component of a team project, called **Naviglass**. The project Naviglass consisted of a pair of virtual reality eye-glasses and a Raspberry Pi attached to the glasses, in addition to this Android application. This app uses Google Maps API to provide several features that require a map. Also, it sends important data (e.g. location, directions) to the Raspberry Pi over a Bluetooth connection. 

**About Naviglass, the team project**

People rely on map applications on their devices to reach their destination. The intention of project Naviglass was to provide an alternative option of navigating through an area that would not require looking at phones for directions as much, and instead allow people to focus on the real-life view ahead. To use Naviglass, you would wear the glasses, open the app, and connect to the Raspberry Pi with Bluetooth from the app. Naviglass had objectives similar to that of Google Glass, but hoped to be more intuitive to use by highlighting roads, landmarks, and directions in the user's field of view. The app was developed by myself, and the Pi and the glasses were designed by others on my team. 

## Documentation 
**Android Application** 
<br />
Please refer to the Wiki of this repository for the following contents: 
* [Wiki Home](https://github.com/yeongeunkwon/Android-App-Naviglass/wiki) | Why the App was made for Naviglass, how it works 
* [Block Diagram](https://github.com/yeongeunkwon/Android-App-Naviglass/wiki/Block-Diagram) | Block Diagram of App
* [Video Tutorial](https://github.com/yeongeunkwon/Android-App-Naviglass/wiki/Video-Tutorial) | Video Demonstrations of using the app 

**Complete Team Project** 
<br />
For context, check out the [poster](https://www.ece.rutgers.edu/sites/default/files/capstone/capstone2019/posters/S19-10-poster.pdf) for Naviglass. 

## Installation 
For testing and verification, you can download this repository and use Android Studio to generate the app. Alternatively, you can install the app on your phone using the app's [APK](/app/build/outputs/apk/debug/app-debug.apk). 

**Prerequisites**
* Android phone with OS 4.4 (KitKat) or higher (which are 98% of Android devices) 
* Android Studio on your computer

**Getting Started**

To generate application from repository files: 
1. Download the repository to your computer. 
1. On Android Studio, click **File -> Open** and open the folder you downloaded. 
1. Wait 1-2 minutes for gradle to build configuration. The configuration "app" should show on the top menu: ![configuration](https://user-images.githubusercontent.com/46125838/82724739-ab30c780-9d1b-11ea-8178-065473826559.PNG) 
1. Follow **Run on a real device** section of this [guide](https://developer.android.com/training/basics/firstapp/running-app#RealDevice). An app called "Naviglass" should be installed to your phone. 
    * Make sure that Google Play Services on your phone is up to date. 
    * Note: Application will not run on the Android Studio emulator, as the emulator does not support Bluetooth. 

To install application from APK: 
1. Follow "Set up your device as follows:" of the section **Run on a real device** of this [guide](https://developer.android.com/training/basics/firstapp/running-app#RealDevice). 
1. Download the [APK](/app/build/outputs/apk/debug/app-debug.apk) to your phone. 
1. Click on the APK file to install the Naviglass app. 

## Credits 
* Author(s): Yeongeun Kwon 
* With Guidance By: Capstone Team S19-10 at the Department of Electrical and Computer Engineering, Rutgers University
