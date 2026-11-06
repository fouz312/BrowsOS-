# Troubleshooting
Actions > failed run > build > the step with the red cross.
- "The file X is missing": upload X and run again.
- Warning "Chromium patch did not compile": the app was built without the Chromium window. It still works.
- App crashes when it opens: delete MainActivity.java and NativeBrowserPlugin.java from the repository, rebuild.
- App not installed: uninstall the old BrowOS first (this deletes its data).
- Window buttons under the status bar: Settings > Startup > Space for the Android status bar.
- Forgot the lock password: uninstall and reinstall BrowOS (deletes its data). Forgot the Vault master password: it cannot be recovered.
- Google or Microsoft sign-in blocked in the Browser: use Open in Chrome (Chrome has its own logins).
