# nunaliit_android

Android application for working with [Nunaliit](http://nunaliit.org) atlas data while offline.

In development:

- sync documents to Android device (version 6 or higher) while online
- add, edit, and delete documents
- record and attach multimedia to documents
- insert device location into documents
- submit new and modified documents to atlas submissions database when online

Near future features:

- Filter document list by schema type
- Sort document list alphabetically, chronologically, by proximity to device
- Search documents
- Backup app data to SDCard
- Restore app data from SDCard

Wish list:

- online map view
- offline-capable map view

## Building

After cloning you must build nunaliit2.

1. The nunaliit2 project is referenced as a git submodule and needs to be cloned separately after cloning the Android project repo:

`git submodule init && git submodule update`

2. Then the nunaliit2-js and nunaliit2-js-external components need to be built (requires maven):

```
cd nunaliit2 && mvn clean install
cd nunaliit2-js && mvn package
cd ../nunaliit2-js-external && mvn package
```

After doing the above steps you should have the entrypoint script used by the Android app's webview, nunaliit2-debug.js, and be able to build & run it in Android Studio.

## Updating

```
git fetch
git pull origin master
git submodule update
cd nunaliit2 && mvn clean install
```

----
Possible pitfalls:

- If Android Studio won't run the app and asks to select an Android SDK, you may need to select Tools -> Android -> Sync Project with Gradle Files.

- If the app runs but the webview fails to load the nunaliit2 interface, you may have skipped steps above.

- If Android Studio gives warnings such as `Warning: License for package Android SDK Build-Tools 27.0.3 not accepted.`, you will need to download the version that is missing. Go to the menu **Android Studio > Preferences** and open **Appearance & Behaviour > System Settings > Android SDK**. Check off the missing versions and click **OK**.

## Adding plugins

Plugins must be added manually and cannot use the CLI. To do so:
- Download Plugin from the github.
- Add the java source files to app/java/your.plugin.org.
- Using the plugin.xml as a reference, update: app/src/main/res/xml/config.xml, app/src/main/AndroidManifest.xml, and app/src/main/assets/www/cordova_plugins.js.
- Add plugin js code to app/src/main/assets/www/plugins/your.plugin.org
- If the plugin js files depend on `require`, `exports` or `module` global variables, wrap the code with :
cordova.define("your.plugin.org.filename", function(require, exports, module) {
// plugin code...
// "your.plugin.org.filename" must match the name given in app/src/main/assets/www/cordova_plugins.js
});
- Cross your fingers and use Chrome's device console to debug issues. (chrome://inspect/#devices)
