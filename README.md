# AmazMod

AmazMod used to be a modified ("modded") Amazfit app, the companion app for Pace and Stratos watches build by Huami, changing and adding some of its features. But it have evolved to its own app that uses that data communication between Amazfit app on phone and the watch to implement its own notification system and more.

#### Some features:  
* Brand new notification system, with the option to use the builtin system or a customized one;  
* Battery log that can be viewed in the app as a graph;  
* Control watch's screen brightness;  
* Option to not send notifications to watch when the phone screen is on;  
* More to come.  

#### Installation:
1. Download and install amazmod.apk on phone;
2. Download and install amazmod_service to watch;
3. Restart both phone and watch;
4. Open AmazMod app on phone and configure it to your needs.
	
#### Troubleshooting
* How do I install the app on watch?  

You can use adb, APKInstaller or Amazfit Tool if you are on Windows. To use adb, you need the binaries on your computer (download them from https://developer.android.com/studio/releases/platform-tools, you may have them already if your computer run Linux), then open Terminal/Command Prompt, change to the same folder as amazmod_service.pak and run:
	adb install amazmod_service.apk

* I do not get notifications  

Make sure you have amazmod_service.apk installed on watch and that AmazMod on phone has granted permissions to access notifications and restart both phone and watch and test notifications again.
