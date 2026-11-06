# Build BrowOS in a new repository
1. GitHub (Chrome, menu > Desktop site) > + > New repository > name it > Public or Private > Create repository.
2. Extract BrowOS-v1.3.zip. Add files with Add file > Upload files, then Commit changes. Upload loose files (no folders needed):
   - main folder: package.json, capacitor.config.json, debug.keystore
   - www: index.html, style.css
   - www/js: ALL 14 .js files
   - icons: the 5 png files
   - android-patch: MainActivity.java and NativeBrowserPlugin.java (for the Chromium browser window)
3. Actions > set up a workflow yourself > name it build-apk.yml > paste the workflow > Commit changes.
4. Wait 5-10 minutes for the green tick. Summary > Artifacts > BrowOS-APK. Extract, install app-debug.apk.
Safe mode: if the new app misbehaves, delete the two .java files from the repository and run the build again. You get the app without the Chromium window.
To update later: upload the new files again with the same names (they replace the old ones).
