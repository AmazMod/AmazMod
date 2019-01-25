# AmazMod  [![Build Status](https://travis-ci.org/edotassi/AmazMod.svg?branch=master)](https://travis-ci.org/edotassi/AmazMod) [![Crowdin](https://d322cqt584bo4o.cloudfront.net/amazmod/localized.svg)](https://crowdin.com/project/amazmod) [![Backers on Open Collective](https://opencollective.com/amazmod-33/backers/badge.svg)](#backers) [![Sponsors on Open Collective](https://opencollective.com/amazmod-33/sponsors/badge.svg)](#sponsors) 

AmazMod used to be a modified ("modded") Amazfit app, the companion app for Pace and Stratos watches built by Huami, changing and adding some of its features. But it has evolved to its own app that uses the data communication between Amazfit app on phone and the watch to implement its own notifications system and more.

### Some features:  
* Brand new notifications filter system, with the option to use customized ("canned") replies;  
* Battery log and other info that can be viewed on the app as a graph;  
* Control watch's screen brightness ("Auto brightness" on watch must be *off* for this to work);  
* Option to not send notifications to watch when the phone screen is on;  
* Receive messenger call notifications and maps navigation info on watch;
* File transfer to and from the watch over the air;
* Shell execution optionality to the watch from the phone;
* Improved Watch/Smartphone connection tunnel (lower battery usage);
* Emoji display ability;
* Various functions in watch widget menu (Wifi, Flash Light, QRCode, LPM, Admin, Reboot);
* More to come.  

### Installation

###### Download links:
* Latest AmazMod (for phone) from PlayStore: https://play.google.com/apps/testing/com.edotassi.amazmod
* Latest service-release.apk (for watch), with optional installer for Windows: https://github.com/edotassi/AmazMod/releases/latest
or
https://github.com/edotassi/AmazMod/tree/master/service-releases

###### Installation steps to use all features
1. Download and install latest AmazMod from Play Store on phone;
2. Download and install latest service-release.apk on watch, or use the provided installer for Windows;
3. Restart both phone *and* watch;
4. Disable "Push Notifications" in Amazfit Settings to prevent double notifications;
5. Check if you see AmazMod widget on watch, if you don't then something went wrong, check Troubleshooting below;
6. Open AmazMod app on phone and configure it to your needs.
7. For better power managment, in the tweaking section on the phone client with the latest version - run:
   `adb shell dpm set-active-admin com.amazmod.service/.AdminReceiver`

###### Installation steps for minimal set of features (notifications filter only)
1. Download and install latest AmazMod from Play Store on phone;
2. Disable "Push Notifications" in Amazfit Settings to prevent double notifications;
3. Open AmazMod and make sure "Custom replies" are *disabled* and "Disable battery chart" is *Enabled* (i.e. battery chart is hidden), then restart phone;
4. Configure the other options to your needs (in this case, ignore the "Connecting" message shown in AmazMod). 

*PS: You may or may not disable Amazfit access to notifications, as long as it doesn't push notifications to watch. If you keep the access, then you can still use your watch to control music and most notifications dismissed on watch will also be removed from phone.*


### Troubleshooting

* How do I install the app on watch?  

You can use adb (all platforms), APKInstaller, Amazfit Tool or the provided installer if you are on Windows. To use adb, you need the binaries on your computer (download them from [Android SDK Platform tools page](https://developer.android.com/studio/releases/platform-tools), you may have them already if your computer runs Linux), then open Terminal/Command Prompt, change to the same folder as service-release.apk and run: `adb install -r service-release.apk`.

* I do not get notifications  

First, make sure you do not have Silet (Do not Disturb) mode enabled in both watch and phone. On watch it displays a "moon" icon when watch is unlocked but some custom watchfaces hide the status line so you must chech with the quick settings menu (swipe down form top), then tap the moon icon if it's blue to disable it. If it does not fix the problem, then make sure that AmazMod on phone has granted permissions to access notifications, restart both phone and watch and test notifications again ("standard" and "custom" notification tests from AmazMod app only works if you have service.apk installed on watch). Also keep in mind that you *must* keep stock Amazfit app installed and running on your phone for the current version of AmazMod to work!

For more info, please check the [FAQ](https://github.com/edotassi/AmazMod/blob/master/FAQ.md).

### Screenshots

<table>
	<tr>
		<td>
			<img src="https://github.com/edotassi/AmazMod/raw/master/images/screen_1.png"/>		
		</td>
		<td>
			<img src="https://github.com/edotassi/AmazMod/raw/master/images/screen_2.png"/>		
		</td>
				<td>
			<img src="https://github.com/edotassi/AmazMod/raw/master/images/screen_3.png"/>		
		</td>
		<td>
			<img src="https://github.com/edotassi/AmazMod/raw/master/images/screen_4.png"/>		
		</td>
				<td>
			<img src="https://github.com/edotassi/AmazMod/raw/master/images/screen_5.jpg"/>		
		</td>
	</tr>
</table>

## Contributors

This project exists thanks to all the people who contribute. [[Contribute](CONTRIBUTING.md)].
<a href="https://github.com/AmazMod/AmazMod/graphs/contributors"><img src="https://opencollective.com/amazmod-33/contributors.svg?width=890&button=false" /></a>


## Backers

Thank you to all our backers! 🙏 [[Become a backer](https://opencollective.com/amazmod-33#backer)]

<a href="https://opencollective.com/amazmod-33#backers" target="_blank"><img src="https://opencollective.com/amazmod-33/backers.svg?width=890"></a>


## Sponsors

Support this project by becoming a sponsor. Your logo will show up here with a link to your website. [[Become a sponsor](https://opencollective.com/amazmod-33#sponsor)]

<a href="https://opencollective.com/amazmod-33/sponsor/0/website" target="_blank"><img src="https://opencollective.com/amazmod-33/sponsor/0/avatar.svg"></a>
<a href="https://opencollective.com/amazmod-33/sponsor/1/website" target="_blank"><img src="https://opencollective.com/amazmod-33/sponsor/1/avatar.svg"></a>
<a href="https://opencollective.com/amazmod-33/sponsor/2/website" target="_blank"><img src="https://opencollective.com/amazmod-33/sponsor/2/avatar.svg"></a>
<a href="https://opencollective.com/amazmod-33/sponsor/3/website" target="_blank"><img src="https://opencollective.com/amazmod-33/sponsor/3/avatar.svg"></a>
<a href="https://opencollective.com/amazmod-33/sponsor/4/website" target="_blank"><img src="https://opencollective.com/amazmod-33/sponsor/4/avatar.svg"></a>
<a href="https://opencollective.com/amazmod-33/sponsor/5/website" target="_blank"><img src="https://opencollective.com/amazmod-33/sponsor/5/avatar.svg"></a>
<a href="https://opencollective.com/amazmod-33/sponsor/6/website" target="_blank"><img src="https://opencollective.com/amazmod-33/sponsor/6/avatar.svg"></a>
<a href="https://opencollective.com/amazmod-33/sponsor/7/website" target="_blank"><img src="https://opencollective.com/amazmod-33/sponsor/7/avatar.svg"></a>
<a href="https://opencollective.com/amazmod-33/sponsor/8/website" target="_blank"><img src="https://opencollective.com/amazmod-33/sponsor/8/avatar.svg"></a>
<a href="https://opencollective.com/amazmod-33/sponsor/9/website" target="_blank"><img src="https://opencollective.com/amazmod-33/sponsor/9/avatar.svg"></a>



### License

This code is distributed using Creative Commons Attribution-ShareAlike 4.0 International license, meaning it's free to use and modify as long as the authors are given the appropriate credits, and the product is distributed using the same license or compatible one (less restrictive).


<a rel="license" href="http://creativecommons.org/licenses/by-sa/4.0/"><img alt="Creative Commons License" style="border-width:0" src="https://i.creativecommons.org/l/by-sa/4.0/88x31.png" /></a><br />This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/4.0/">Creative Commons Attribution-ShareAlike 4.0 International License</a>.
</center>
