# Build BrowOS once, then update from inside the app
## First time (one APK build)
1. GitHub (Chrome, menu > Desktop site) > + > New repository > Create repository.
2. Extract the zip. Upload loose files with Add file > Upload files, Commit changes after each batch:
   - main folder: package.json, capacitor.config.json, debug.keystore
   - www: index.html, style.css
   - www/js: ALL 20 .js files
   - icons: the 5 png files
   - android-patch: all 5 files (MainActivity.java, NativeBrowserPlugin.java, Extras.java, Browsing.java, KeepAliveService.java). NATIVE_VERSION is optional.
3. Actions > set up a workflow yourself > name it build-apk.yml > paste the workflow > Commit changes.
4. Wait 5-10 minutes for the green tick. Summary > Artifacts > BrowOS-APK. Extract, install app-debug.apk.
## Every later update (no APK, no GitHub)
1. Download the new BrowOS zip (do not extract it).
2. Open BrowOS > Settings > App update > Choose zip > pick the zip > Restart now.
If the app says the zip has Android changes, build a new APK as above. If an update fails to start, BrowOS removes it by itself and uses the built-in version. Settings > App update > Remove update also does this.
