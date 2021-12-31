public static void servicesKiller() {
    localActivityManager = (ActivityManager) imContext.getSystemService(Context.ACTIVITY_SERVICE);
    RunningServices = localActivityManager.getRunningAppProcesses();
    if (RunningServices == null) return;

    // Whitelist prefixes
    Set<String> whitelistPrefixes = new HashSet<>(Arrays.asList(
        "com.android.", "com.google.", "com.mgoogle.", "com.vanced.",
        "zhihu.", "ugc.", "GoogleCamera.", "com.whatsapp.",
        "com.spotify.", "com.amazon.", "com.twitter.", "com.netflix.",
        "com.discord.", "com.reddit.", "com.monzo.", "com.barclays.",
        "com.natwest.", "com.paypal."
    ));

    // Whitelist exact matches
    Set<String> whitelistExact = new HashSet<>(Arrays.asList(
        "org.telegram", "org.thoughtcrime.securesms",
        "chat.delta", "com.wire", "ims"
    ));

    for (ActivityManager.RunningAppProcessInfo procInfo : RunningServices) {
        if (procInfo.pkgList == null) continue;

        for (String pkg : procInfo.pkgList) {
            boolean skip = false;

            // Check prefixes
            for (String prefix : whitelistPrefixes) {
                if (pkg.startsWith(prefix)) {
                    skip = true;
                    break;
                }
            }

            // Check exact
            if (!skip && whitelistExact.contains(pkg)) {
                skip = true;
            }

            if (!skip) {
                localActivityManager.killBackgroundProcesses(pkg);
                Log.d(TAG, "Killed background process: " + pkg);
            }
        }
    }
}
