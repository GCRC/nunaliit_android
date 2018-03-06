# nunaliit_android

Android application for Nunaliit

## After cloning: Build nunaliit2

1. The nunaliit2 project is referenced as a git submodule and needs to be cloned separately after cloning the Android project repo:

    git submodule init && git submodule update

2. Then the nunaliit2-js and nunaliit2-js-external components need to be built (requires maven):

    (cd nunaliit2/nunaliit2-js && mvn package)
    (cd nunaliit2/nunaliit2-js-external && mvn package)

After doing the above steps you should have the entrypoint script used by the Android app's webview, nunaliit2-debug.js, and be able to build & run it.

If you skip the above steps, the app will run but the webview will fail to load the nunaliit2 interface.
