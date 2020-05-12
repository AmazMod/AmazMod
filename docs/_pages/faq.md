---
layout: page
title: FAQ
include_in_header: true
---
# Table of contents
1. [Introduction](#introduction)
2. [Installation / Upgrade / Removal instructions](#paragraph1)
    1. [Clean installation](#subparagraph1)
    2. [Upgrade from previous versions](#subparagraph2)
    3. [Removal](#subparagraph3)
3. [Mobile Application overview](#paragraph2)
    1. [Settings](#subparagraph4)
    2. [Tweaking](#subparagraph5)
    3. [File Explorer](#subparagraph6)
    4. [Widgets](#subparagraph7)
    5. [GreatFit / Calendar / Weather](#subparagraph8)
    6. [Stats / Logs](#subparagraph9)
    7. [FAQ](#subparagraph10)
    8. [Support us](#subparagraph11)
    9. [About](#subparagraph12)
4. [Watch Service overview](#paragraph3)
    1. [UI](#subparagraph13)
    2. [Functionality](#subparagraph14)
    3. [FAQ](#subparagraph15)
    4. [Troubleshooting](#subparagraph16)
5. [General Troubleshooting](#paragraph4)
    1. [I can't install AmazMod on my Stratos 3](#subparagraph17)
    
    

## So what is AmazMod? <a name="introduction"></a>
AmazMod used to be a modified ("modded") Amazfit app, the companion app for Pace and Stratos watches built by Huami, changing and adding some of its features. But it has evolved to its own app that uses the data communication between Amazfit app on phone and the watch to implement its own notifications system and more.
As of today, AmazMod is supported by the following devices:
**Amazfit PACE, Amazfit Verge, Amazfit Stratos and Amazfit Stratos 3**

Some features:
* Brand new notifications filter system, with the option to use customized ("canned") replies;
* Battery log and other info that can be viewed on the app as a graph;
* Control watch's screen brightness ("Auto brightness" on watch must be off for this to work);
* Option to not send notifications to watch when the phone screen is on;
* Receive messenger call notifications and maps navigation info on the watch;
* File transfer to and from the watch over the air;
* Shell execution optionality to the watch from the phone;
* Improved Watch/Smartphone connection tunnel (lower battery usage);
* Emoji display ability;
* Various functions in watch widget menu (Wifi, Flash Light, QRCode, LPM, Admin, Reboot);


## Installation / Upgrade / Removal instructions <a name="paragraph1"></a>
### Clean installation <a name="subparagraph1"></a>
### Upgrade from previous versions <a name="subparagraph2"></a>
### Removal <a name="subparagraph3"></a>
For a proper removal of AmazMod (**why would you do that?!**), follow these steps:
1. On your watch, open the AmazMod widget and tap the **cog** icon.
2. Scroll down to the **Revoke Device Admin Permission** and tap it.
3. Open the **App Manager** on the AmazMod widget, and remove AmazMod service :(

Alternatively, you can watch this video:

[![Uninstall AmazMod](http://img.youtube.com/vi/M6P57yv3yd4/0.jpg)](http://www.youtube.com/watch?v=M6P57yv3yd4 "Uninstall AmazMod")

## Mobile Application overview <a name="paragraph2"></a>
### Settings <a name="subparagraph4"></a>
### Tweaking <a name="subparagraph5"></a>
### File Explorer <a name="subparagraph6"></a>
### Widgets <a name="subparagraph7"></a>
### GreatFit / Calendar / Weather <a name="subparagraph8"></a>
### Stats / Logs <a name="subparagraph9"></a>
### FAQ <a name="subparagraph10"></a>
### Support us <a name="subparagraph11"></a>
### About <a name="subparagraph12"></a>

## Watch Service overview <a name="paragraph3"></a>
### UI <a name="subparagraph13"></a>
### Functionality <a name="subparagraph14"></a>
### FAQ <a name="subparagraph15"></a>
### Troubleshooting <a name="subparagraph16"></a>

## General Troubleshooting <a name="paragraph4"></a>
### I can't install AmazMod on my Stratos 3 <a name="subparagraph17"></a>
With Amazfit Stratos 3, Huami changed few things, and we had to get creative.
Please follow these steps for a proper installation on your Stratos 3 sportswatch:
1. **Install latest AmazMod from the playstore** - <https://play.google.com/store/apps/details?id=com.edotassi.amazmod> - make sure to give full permissions to Amazfit and AmazMod apps and lock to work in the background.
2. **Head to the AmazMod github download page** - <https://github.com/edotassi/AmazMod/releases/latest> - and install the service on your watch by running the installation wizard or install the service apk manually and at the end run the following by adb:

`adb -d shell monkey -p com.amazmod.service 1 > NUL`

and reboot your watch.

Enable the AmazMod UI by following the steps in video:


[![AmazMod Stratos 3](https://img.youtube.com/vi/4fAhb6cylqY/0.jpg)](https://www.youtube.com/watch?v=4fAhb6cylqY "AmazMod installation on Amazfit Stratos 3")

