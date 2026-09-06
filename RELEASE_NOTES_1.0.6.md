# Safeer Mobile 1.0.6

- Wider address/search field, larger text and 48 dp controls. Editing expands the field across the toolbar; Back dismisses editing without leaving the page.
- Long press the tab counter for a new tab. Home, refresh/stop, bookmarks and other actions are in the scrollable menu.
- Native Android default-browser selection, with the result checked against the system. Already-default status is shown accurately.
- Authentication and signed links keep their original query parameters. Redirects and POST requests are left to WebView.
- Login pages and verification controls are protected from intrusive page-cleanup scripts. Malware checks remain active.
- Links from other apps open in a new tab or reuse an exact matching tab, preserving the page being read.

Validation: JVM login URL/domain-boundary, threat-policy, local proxy and encrypted-DNS tests passed. Signed APK upgraded in place on Samsung Galaxy S25. Account-free on-device fixture verified direct and delayed popups, opener return, POST body, cookies, verification frame, preserved form, collapsed ad space and blocked automatic popup. Native role query confirms Safeer remains the default browser. No personal account credentials were entered.

Live public ChatGPT check reached the Google account chooser through Continue with Google. No account was selected. Search entry replaced the full old URL and opened the configured search provider. Address text width on S25: 610 px browsing, 696 px editing, previously 125 px (density 3).
