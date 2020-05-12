---
layout: page
title: What's New
include_in_header: true
---

# Changelog
Here you will find the full changelog of the __public__ __official__ __releases__ of __AmazMod__.
<br>
# Latest
## **Version 1.1.9 / 220**
### What's New
#### Smartphone side - AmazMod application on your phone:
- Add setting to use colored icon for custom UI notification
- Now dark theme will follow system theme setting (beta)
- Watch widgets can now be reordered directly from the phone
- Add "Clear ADB" button on tweaking menu to avoid to much adb processes keep running
- Monitor widgets option on to force apply the selected order while Amazfit app tries to revert it
- Ability to select the calendar accounts from which data are extracted
- Ability to export battery graph history (long press the graph)
- Added Hourly chime feature (a vibration every hour)
- Now notification filter can be used for title, content only or both two as before
- New option to enable/disable notification sound on Verge
- New accurate weather forecast data (based on OpenWeatherMap)
- New weather card in main screen
- Enable/Disable notification forwarding while driving (phone in driving mode)
- Music files show up directly after transfer to watch (mp3, m4a)
- Translation additions. App is almost fully translatable
#### Watch side - AmazMod Service app on your watch:
- Incoming notifications will only vibrate while you type (to avoid losing your message)
- Improved widget-reorder UI with a save button
- Ability to collect and save XDrip+ transmitted data
- Service now supports translations (language is set based on phone app) 

#### Bug Fixes
 - Fix some problems with Android 10
 - Fix missing Google Assistant notification
 - Filemanager and App list now use case insensitive sort order
 - Calendar data fixes
 - Small fix on Dark theme text visibilities and other graphical adjustments
 - Widgets list will show all installed widgets
 - (Service) Code cleanup
 
<br>

## **Version 1.1.8 / 217**
### What's New
- OTA service update fix

<br>

## **Version 1.1.8 / 216**
### What's New
- Phone app updated to with new libs (AndroidX)
- Internet access from the watch support with Amazfit Internet Companion service
- Overlay button notifications access - Notification access directly from the watchface
- Improved Emoji support
- Dark theme (beta)
- Option to activate WFZ watchface directly when pushing it to watch
- Ability to generate App/Service logs directly from AmazMod
- Command History is now saved/restored in backup
- New file picker (tap and hold to select multiple files)
- Option to hide heart rate chart, which stops battery data requests
- Files Downloaded from watch now opens directly in File Explorer
#### Bug Fixes
- re-worked Download/Upload/Screenshot (should be more stable)
- Widgets reorder fixes
- Maps and messenger calls notifications working
- Do not vibrate watch for alerts if Silence is enabled
- Fixed Sporadic "Welcome to Amazmod" notification on watch
- Lots of Small Bug fixes and Optimizations
#### Info
- UI/UX updates and optimizations
- Updated Internal libs
- Updated Translations

<br>

## **Version 1.1.2 / 212**
### What's New
- Faster app installation now is work for NON root user too! (PS Amazmod update will be fast from the next update too)
- Heart-rate graph added (phone app)
- Screenshots are auto rotated for Verge
- Watch's screen is unlocked before a screenshot
- New adaptive phone app icon
- Watch Info card style improvements (phone app)
- Battery graph added in watch's menu (service app)
- Low phone battery alert on watch
- Low watch battery notification on phone
- Fully charged watch notification on phone
- Connection status notification can now be hidden from device settings (new notification channel on Android O+)
- Number of calendar events found are shown after ICS URL test
- Icons for images, apps and watchfaces in the file explorer
#### Bug Fixes
- Screenshot color and other fix
- Translation option selection fix
- Notifications system app filter fix
- Settings titles/descriptions where simplified
- Installing apks with spaces/dots in their name fix
- Opening apk/wfz from Opera fix (maybe for other apps too)
- Small bug fixes
#### Info
- Updated translations

<br>

## **Version 1.1.5 / 210**
### What's New

        <new>Notifications control adjustments:
            1. Store and deploy on watch re-connection
            2. Dismiss if watch not connected</new>
        <new>New layout for notifications</new>
        <new>Improved power management</new>
        <bugfix>Backup/Restore fix</bugfix>
        <new>Direct language selection</new>
        <info>Updated translations</info>
        <bugfix>Many Bug fixes</bugfix>
        <new>Many new undiscovered magical bugs</new>
    </release>
    <release
        date="2019-02-08"
        versionCode="201"
        versionName="v1.1.3 (201)">
        <new>Calendar Sync (native and iCal) - need a third party app to view calendar events: https://goo.gl/oc97Qv</new>
        <new>Improved Launcher (now it fixes in first position)</new>
        <new>Built-in file explorer on your watch! (Install apk, manage files and view Text and Images directly from watch)</new>
        <info>Translations updated</info>
        <bugfix>ungrouped notifications removal (only standard notifications)</bugfix>
        <bugfix>OTA (Over The Air) updates now should work</bugfix>
        <bugfix>Autolock bug corrected</bugfix>
        <bugfix>Lots of Small fixes</bugfix>
    </release>
    <release
        date="2019-01-31"
        versionCode="200"
        versionName="v1.1.0 (200)">
        <new>Added users avatar icons</new>
        <new>Added notifications pictures</new>
        <new>New watch widget (now it's a launcher)</new>
        <new>Notification Drawer for Custom Notifications</new>
        <new>Widgets Reorder Feature (based on KieronQuinn's AmazfitSpringboardSettings)</new>
        <new>Filter notification based on a text</new>
        <new>Mute Specific App Notifications for some time</new>
        <info>Improvements in APK Installer</info>
        <bugfix>Fixed some crashes</bugfix>
    </release>
    <release
        date="2018-11-14"
        versionCode="196"
        versionName="v1.0.65">
        <new>Added developer mode</new>
        <bugifx>Fixed some crashes</bugifx>
    </release>
    <release
        date="2018-11-09"
        versionCode="194"
        versionName="v1.0.64">
        <new>Auto update watch service from phone app</new>
        <new>Full emoji compatibility</new>
        <new>Swipe right to discard and left to reply notifications</new>
        <new>File Explorer can Create/Delete/Compress/Uncompress folders</new>
        <new>Tweaking now enable/disable watch auto-brightness</new>
        <new>Tweaking stores list of executed commands</new>
        <new>Option to Delete All notifications in log</new>
        <new>Various functions in watch widget menu (Wifi, Flash Light, QRCode, LPM, Admin,
            Reboot)
        </new>
        <new>Drag and Drop to reorder quick replies</new>
        <new>Ability to send multiple files to watch</new>
        <new>Improved WatchData transfer (lower battery usage)</new>
        <bugfix>Grouped notifications were not being removed from watch</bugfix>
        <info>Improved translations</info>
        <info>Minor fixes</info>
    </release>
    <release
        date="2018-09-22"
        versionCode="180"
        versionName="v1.0.50">
        <new>File explorer with file transfer</new>
        <new>Adb command executor in tweaking section</new>
        <new>Low Power Mode</new>
        <new>Added apk installation utility</new>
        <new>Theme in custom notifications</new>
        <new>Better battery management in custom notifications (need admin owner to enable it)</new>
        <new>GreatFit integration</new>
        <new>Added Spanish translation</new>
        <new>Added Hungarian translation</new>
        <info>Updated translations</info>
        <info>Improved disconnected state</info>
        <info>Minor fix</info>
    </release>
    <release
        date="2018-09-21"
        versionCode="179"
        versionName="v1.0.49">
        <bugfix>Too many notifications blocked (regression)</bugfix>
    </release>
    <release
        versionCode="163"
        versioneName="v1.0.34">
        <bugfix>Fixed issue #164</bugfix>
        <bugfix>Fixed issue #160</bugfix>
    </release>
    <release
        date="2018-08-25"
        versionCode="162"
        versionName="v1.0.33">
        <bugfix>Fixed a bug with preferences in Oreo</bugfix>
        <bugfix>Fixed issue #157</bugfix>
        <info>Updated hu, fr, it translations</info>
    </release>
    <release
        date="2018-08-23"
        versionCode="160"
        versionName="v1.0.32">
        <new>Added inverted theme and configurable font size for Custom Replies</new>
        <new>Preferences backup and restore</new>
        <new>Added preference to disable screen on for Custom replies</new>
        <new>Added persistent notification with Preferences</new>
        <new>Added Czech translations</new>
        <new>Added Greek translations</new>
        <new>Added Russian translations</new>
        <info>Improvements to Maps navigation notifications and Notifications log</info>
        <info>Updated FAQ</info>
        <info>Updated Italian translations</info>
        <bugfix>Skype notifications fix</bugfix>
        <bugfix>Fix for Outlook notifications too</bugfix>
        <bugfix>Minor fix</bugfix>
    </release>
    <release
        date="2018-08-14"
        versionCode="154"
        versionName="v1.0.27">
        <new>Added Italian translations</new>
        <new>Added French translations</new>
        <new>Added Hebrew translations</new>
        <new>Added credits for translators in About page</new>
        <bugfix>Fixed a crash in start screen</bugfix>
    </release>
    <release
        date="2018-08-12"
        versionCode="153"
        versionName="v1.0.26">
        <new>First beta release</new>
        <new>Added faq section</new>
        <info>Battery chart improvements</info>
    </release>
    <release
        date="2018-08-08"
        versionCode="152"
        versionName="v1.0.25">
        <new>Added email and telegram channel</new>
        <info>Improved battery prediction</info>
        <bugfix>Fixed some crashes</bugfix>
    </release>
    <release
        date="2018-08-08"
        versionCode="151"
        versionName="v1.0.23-RC5">
        <new>battery prediction</new>
        <bugfix>Minor fix</bugfix>
    </release>
    <release
        date="2018-08-07"
        versionCode="150"
        versionName="v1.0.22-RC4">
        <new>Add maps navigation</new>
        <new>Connection status</new>
        <new>Internal code refactoring</new>
        <bugfix>Minor fix</bugfix>
    </release>
    <release
        date="2018-08-05"
        versionCode="149"
        versionName="v1.0.21-RC3">
        <bugfix>Check if transport service is connected</bugfix>
        <bugfix>Minor fix</bugfix>
    </release>
    <release
        date="2018-08-04"
        versionCode="148"
        versionName="v1.0.20-RC2">
        <new>Published on Play Store</new>
        <bugfix>Minor fix</bugfix>
    </release>
    <release
        date="2018-08-04"
        versionCode="147"
        versionName="v1.0.19-RC2">
        <new>Merged code</new>
    </release>
    <release
        date="2018-08-04"
        versionCode="146"
        versionName="v1.0.18-RC1">
        <new>Release candidate</new>
    </release>
    <release
        date="2018-08-02"
        versionCode="145"
        versionName="v1.0.17-RC1">
        <new>Release Candidate #1</new>
        <new>Added ungroup option for standard notifications</new>
        <bugfix>Custom UI is now Replies</bugfix>
        <info>Some code cleaning</info>
    </release>
    <release
        date="2018-08-02"
        versionCode="144"
        versionName="v1.0.16-ALPHA">
        <new>Notifications log</new>
        <bugfix>Notifications when screen is locked</bugfix>
        <bugfix>Fix null icon in test notification with custom ui</bugfix>
    </release>
    <release
        date="2018-08-01"
        versionCode="143"
        versionName="v1.0.15-ALPHA">
        <bugfix>Fixed battery data insertion in the database</bugfix>
    </release>
    <release
        date="2018-07-30"
        versionCode="142"
        versionName="v1.0.14-ALPHA">
        <bugfix>Fixed double transported data in HermesEventBus, finally!!!</bugfix>
    </release>
    <release
        date="2018-07-29"
        versionCode="141"
        versionName="v1.0.13-ALPHA">
        <new>Initial support to voice call notifications for messengers</new>
        <new>CustomUI notifications will now respect Silent mode</new>
        <new>Added Preferences for both Voice call and Local only notifications</new>
    </release>
    <release
        date="2018-07-29"
        versionCode="140"
        versionName="v1.0.12-ALPHA">
        <new>Added current battery value to battery chart in phone app and value and icon to
            widget
        </new>
        <info>Widget code cleaned and optimized</info>
    </release>
    <release
        date="2018-07-27"
        versionCode="139"
        versionName="v1.0.11-ALPHA">
        <new>Added Setting to disable remove of notifications from watch when they are removed from
            phone
        </new>
    </release>
    <release
        date="2018-07-27"
        versionCode="138"
        versionName="v1.0.10-ALPHA">
        <new>AmazMod will now respect DND mode (exprimental)</new>
        <new>Setting added to disable notifications when DND is enabled</new>
        <bugfix>Time since last full charge display enhanced</bugfix>
    </release>
    <release
        date="2018-07-26"
        versionCode="137"
        versionName="v1.0.9-ALPHA">
        <new>Remove Notifications from watch when they are removed from phone</new>
        <new>Added Time Since Last Charge</new>
        <new>Implemented both Disable Battery Chart and Chart Time Interval</new>
        <bugfix>App start using default locale after restart with Force to English enabled</bugfix>
        <bugfix>Check for notifications access on Oreo after Wizard</bugfix>
    </release>
    <release
        date="2018-07-14"
        versionCode="136"
        versionName="v1.0.8-ALPHA">
        <bugfix>Charging color in battery chart</bugfix>
    </release>
    <release
        date="2018-07-04"
        versionCode="135"
        versionName="v1.0.7-ALPHA">
        <new>Added preference to force English localization</new>
        <info>Updated Preferences (again)</info>
        <info filter="watch">Keep button on Custom UI notifications save them to system drawer
        </info>
        <info>Added initial Brazilian Portuguese localization</info>
        <info>Added optional Activity for preferences (WIP)</info>
    </release>
    <release
        date="2018-07-02"
        versionCode="134"
        versionName="v1.0.6-ALPHA">
        <info filter="watch">Now when you reply the notification is closed</info>
        <info>Better wizard</info>
    </release>
    <release
        date="2018-07-01"
        versionCode="133"
        versionName="v1.0.5-ALPHA">
        <bugfix>App selection not saved in "Filter by apps"</bugfix>
        <info>Selected apps are saved when toggle is pressed</info>
        <info>System apps are showed if enabled</info>
        <new>In battery card there is the time of the last update</new>
    </release>
    <release
        date="2018-07-01"
        versionCode="132"
        versionName="v1.0.4-ALPHA">
        <bugfix>Back button in toolbar</bugfix>
        <bugfix>Check notifications access permission</bugfix>
        <new>Intro with first setup</new>
        <new>Replies list in settings</new>
        <new>Brightness control</new>
        <new>Settings to choose UI for notifications: standard or custom</new>
        <new>Initial support for replies in standard notifications</new>
        <new>Changed replies internal format, now they are stored as json (now you can use the comma
            in the text)
        </new>
    </release>
    <release
        date="2018-06-26"
        versionCode="131"
        versionName="v1.0.3-ALPHA">
        <bugfix>No notifications when "Disable when screen is ON" is enabled</bugfix>
        <new>Add option to filter system apps in packages selector</new>
        <new>Add option to select/unselect all app in packages selector</new>
        <new>Ordered packages by selection status</new>
    </release>
    <release
        date="2018-06-26"
        versionCode="130"
        versionName="v1.0.2-ALPHA">
        <new>Add option to disable notifications while screen is ON</new>
        <new>Add changelog dialog</new>
        <new>Add control for notification access</new>
        <info>Initial support for Hungarian translations</info>
    </release>
