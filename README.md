# AmazMod

AmazMod used to be a modified ("modded") Amazfit app, the companion app for Pace and Stratos watches built by Huami, changing and adding some of its features. But it has evolved to its own app that uses the data communication between Amazfit app on phone and the watch to implement its own notifications system and more.

### Some features:  
* Brand new notifications filter system, with the option to use customized ("canned") replies;  
* Battery log and other info that can be viewed on the app as a graph;  
* Control watch's screen brightness ("Auto brightness" on watch must be *off* for this to work);  
* Option to not send notifications to watch when the phone screen is on;  
* Receive messenger call notifications and maps navigation info on watch;
* More to come.  

### Installation

###### Download links:
* Latest AmazMod (for phone) from PlayStore: https://play.google.com/apps/testing/com.edotassi.amazmod
* Latest service-release.apk (for watch), with optional installer for Windows: https://github.com/edotassi/AmazMod/releases/latest

###### Installation steps to use all features
1. Download and install latest AmazMod from Play Store on phone;
2. Download and install latest service-release.apk on watch, or use the provided installer for Windows;
3. Restart both phone *and* watch;
4. Disable "Push Notifications" in Amazfit Settings to prevent double notifications;
5. Open AmazMod app on phone and configure it to your needs.

###### Installation steps for minimal set of features (notifications filter only)
1. Download and install latest AmazMod from Play Store on phone;
2. Disable "Push Notifications" in Amazfit Settings to prevent double notifications;
3. Open AmazMod and make sure "Custom replies" are *disabled* and "Disable battery chart" is *Enabled* (i.e. battery chart is hidden), then restart phone;
4. Configure the other options to your needs.

*PS: You may or may not disable Amazfit access to notifications, as long as it doesn't push notifications to watch. If you keep the access, then you can still use your watch to control music and most notifications dismissed on watch will also be removed from phone.*


### Troubleshooting

* How do I install the app on watch?  

You can use adb (all platforms), APKInstaller, Amazfit Tool or the provided installer if you are on Windows. To use adb, you need the binaries on your computer (download them from [Android SDK Platform tools page](https://developer.android.com/studio/releases/platform-tools), you may have them already if your computer runs Linux), then open Terminal/Command Prompt, change to the same folder as service-release.apk and run: `adb install -t service-release.apk`.

* I do not get notifications  

Make sure you have amazmod_service.apk installed on watch and that AmazMod on phone has granted permissions to access notifications, restart both phone and watch and test notifications again.

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

### License

This code is distributed using Creative Commons Attribution-ShareAlike 4.0 International license, meaning it's free to use and modify as long as the authors are given the appropriate credits, and the product is distributed using the same license or compatible one (less restrictive).

<center>
<a rel="license" href="http://creativecommons.org/licenses/by-sa/4.0/"><img alt="Creative Commons License" style="border-width:0" src="https://i.creativecommons.org/l/by-sa/4.0/88x31.png" /></a><br />This work is licensed under a <a rel="license" href="http://creativecommons.org/licenses/by-sa/4.0/">Creative Commons Attribution-ShareAlike 4.0 International License</a>.
</center>
