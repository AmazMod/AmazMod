**Frequently asked questions of AmazMod for Amazfit Pace and Stratos.**

A- *What's AmazMod app?*  
R- AmazMod is an app that works in parallel with the stock Amazfit Watch app to add advanced features to your Pace and Stratos regarding notifications and other customizations.

A- *What's the best feature of AmazMod?*  
R- AmazMod was born to improve the stock notification system with the possibility to read the entire message without the limits of stock app, to reply a notification directly from watch, and more.

A- *How to use AmazMod?*  
R- You need to have a working Amazfit Watch app paired with your Pace or Stratos and then install AmazMod from Playstore, this is enough for basic notifications filtering. For advanced features, like reply to notifications, you need to install AmazMod service APK on your watch. For more details, please check the [installation guide](https://github.com/edotassi/AmazMod/blob/master/README.md#installation).

A- *How is the battery impact of AmazMod?*  
R- AmazMod is projected to have advanced features but without major impact on battery usage as it uses the same communication channel used by Amazfit!

A- *Why I don't receive any notifications?*  
R- First, make sure you do not have Silet (Do not Disturb) mode enabled in both watch and phone. On watch it displays a "moon" icon when watch is unlocked but some custom watchfaces hide the status line so you must chech with the quick settings menu (swipe down form top), then tap the moon icon if it's blue to disable it. If it's not the case, then restart both phone and watch, check if watch is connected correclt in stock  Amazfit, and AmazMod has access granted to notifications (either run the welcome wizzard again or go to AmazMod Settings -> Check permissions), then double check if you have added the desired apps in "Selected apps" entry of Notifications options in AmazMod app and no other setting/filter is blocking the notification (you can check "Notifications log" in stats page for hints). Also keep stock Amazfit installed and running (see next question).

A- *After I install AmazMod, I can remove the stock app?*  
R- No, AmazMod needs stock Amazfit app installed on phone to communicate with your watch, so you must keep Amazfit installed.

A- *I am getting two notifications/double on watch, is it normal?*  
R- Yes, if you have enabled "Custom replies" in AmazMod then you will get two notifications: one is short-lived and will enable you to reply (it will remain on screen for the time that it is selected in AmazMod Settings and then it will be gone, so it is not actually a notification but just a short-time interface for replies) and the regular notification that looks like common Amazfit notifications but were actually filtered and improved by AmazMod. Obviously, if you have *disabled* "Custom replies" then you will get only one notification.

A- *I cannot install service-release.apk on watch, why?*  
R- Well, this is hard to answer, it can be due to many factors But the most commons are: you don't have enough free space on watch to install a new APK, you have an old version of the APK that must be uninstalled first. If you cannot install a new version and uninstalling doesn't seem to work (this may happen if you have used third party app Amazfit Tool to uninstall APKs), try using this adb command:<br>
`adb shell pm uninstall com.amazmod.service`

You may also have the dreaded "communication issue" while watch is connected to computer, very common specially if you have Stratos: check your connection to the computer, including cable/charger, metallic contacts on both watch and charger (keep them clean!) and USB port on your computer. 

Last but not least, restart the watch if you have any error when tryin to install the APK, then try again.

A- *I don't get notifications for app ABCD, help!?!?*  
R- First, calm down. Then check if you have any setting that is preventing the notification to arrive on watch and change it to the appropriate value, then make sure the app ABCD is *enabled* in "Selected apps" setting. Note that if it's a system application, you must enable system applications in the 3 dots menu first (selected system apps will always be displayed tho).

A- *I want/need/can't live without missed voice call notifications, can you add this?*  
R- No, actually Amazfit will always send new voice call notifications to watch, so there is no need for this notification. But if you really can't live without it, then enable system apps in "Selected apps" and activate "Contacts" or whatever app is used for call notifications (you might need to enable "Local" notifications too, i.e. notifications that do not have any user interactions).

A- *Service APK is crashing on watch, why?*  
R- Check if you have enabled "CallUI" or "Telephone" system apps and disable it. Call notifications are handled by Amazfit stock app, you don't need to activate anything to get conventional call notifications while Amazfit is installed.

A- *Will you add feature XYZ because app VTW has it?*  
R- Firstly, each app has its own features and goals, if one app does something it doesn't mean that all other apps must do the same thing, please grow up. But if the feature is useful for most users and it doesn't cause problems like battery drain, etc, maybe it can be added in the future. Be nice, use your GitHub account and add a new "issue", then a handsome AmazMod developer will look into it and decide if it's useful/doable or not.

A- *How do I report a bug?*  
R- You are in the right place! Use your GitHub account to create a new "issue", please add as much info as you can, and logcats showing the error is very much appreciated! Thank you!

A- *I cannot install AmazMod? / How do I get support? / H-E-L-P M-E!*  
R- GitHub isn't a place for support. Currently your options are:

International:
- https://www.facebook.com/groups/HuamiAmazfit.Pace.Smartwarch/

Italian:
 - https://www.facebook.com/groups/Italiamazfit/
 - https://t.me/joinchat/GK1rkg-rXcATxgfTenq9sg

Brazilian:
- https://t.me/Amazfit
- https://t.me/ptBRpacebip
