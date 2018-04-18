# nunaliit_android

Android application for Nunaliit

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
```

----
Possible pitfalls:

- If Android Studio won't run the app and asks to select an Android SDK, you may need to select Tools -> Android -> Sync Project with Gradle Files.

- If the app runs but the webview fails to load the nunaliit2 interface, you may have skipped steps above.

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
