# Android Application for Naviglass

## Introduction
This Android application is one component of a team project named Naviglass. The app was designed to be compatible with a pair of virtual reality eye-glasses and a Raspberry Pi, attached to the glasses, that were engineered for Naviglass. The app was developed by myself, while three other people on the team programmed the pi and designed hardware of the glasses. 

People often rely on map applications on their devices to reach their destination. The intention of Naviglass was to provide an alternative option of navigating through an area that would not require looking down at phones for directions as much, and instead allow users to focus more on the real-life view ahead. 

The app shows a map showing current location and route to the destination to the user. It also sends data containing directions, user's location, etc. to the Raspberry Pi via a Bluetooth connection. The Raspberry Pi and the glasses use image recognition and data sent from the app to display the real-life view ahead to the user wearing the glasses. 

## Documentation
Please see this repository's [Wiki](https://github.com/yeongeunkwon/Android-App-Naviglass/wiki) for documentation and directions to use the app. 

## Installation 
The instructions below will get the app running on your Android phone for testing purposes. 

**Prerequisites**
* Android Studio on your desktop 
* Android phone with OS 4.4 (KitKat) or higher

**Getting Started**
1. Download this repository as a ZIP and unzip to a folder on your desktop. 
1. Follow this [guide](https://developer.android.com/training/basics/firstapp/creating-project) to open the repository files on Android Studio, with the following exceptions to the instructions:  
    * Select **Java** from the **Language** drop-down menu. 
    * On the **Minimum SDK** field, select **API 19: Android 4.4 (KitKat)**. Alternatively, if your Android phone has more recent API, you may select the API of your phone. 
    * After clicking **Finish**, once the empty project has opened, click on File -> Open. Open the folder you downloaded earlier from this repository. 
1. Follow this [guide](https://developer.android.com/training/basics/firstapp/running-app) to run the app on your Android phone. 
    * Make sure that Google Play Services is up to date on your phone. 
