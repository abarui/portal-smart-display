# Connect calendars and your phone

This setup uses **read-only calendar subscriptions**. The Portal refreshes published events; it does not merge phone calendars into a new account. Provider publishing rules and refresh delays still apply. Each calendar needs its own supported subscription URL.

## Add with the Portal's QR code

1. Open the calendar connection/phone setup option in the dashboard Settings.
2. Scan the displayed QR code with the iPhone or Android camera. Keep both devices on the same Wi-Fi.
3. Choose Google, iCloud or Outlook, enter a display name, and paste the calendar subscription link.
4. Read the link-sharing note and confirm the connection. The Portal checks the calendar before adding it.
5. Close the setup dialog when finished. If the code expires, request a new code on the Portal.

The QR code opens a temporary setup session. It does not silently read every calendar on the phone or grant the Portal your whole Apple/Google/Microsoft account. No password should be entered into the subscription-link form.

## iCloud Calendar from an iPhone

In Apple's Calendar app, open **Calendars**, then the information button beside an iCloud calendar you own. Enable **Public Calendar** and use **Share Link / Copy Link**. Paste that `webcal://` link into the Portal's iCloud form; the app converts it to HTTPS. Some shared or managed calendars cannot be published by you.

A public-calendar link lets anyone who obtains it read the published events. Choose a calendar suitable for that sharing model. Turning publishing off revokes access and stops the Portal's feed. See [Apple's iCloud calendar-sharing guide](https://support.apple.com/guide/icloud/share-a-calendar-mm6b1a9479/icloud).

## Google Calendar

Use Google Calendar in a desktop browser: Settings → select the calendar → Integrate calendar → **Secret address in iCal format**. Copy the complete `.ics` URL. Do not make a private calendar publicly searchable just to connect it. A work/school administrator may disable secret addresses or external access.

If the calendar is intentionally public, its public iCal address can also be used. A normal calendar webpage or an invitation link is not a subscription feed. See [Google's calendar-sync instructions](https://support.google.com/calendar/answer/37648).

## Outlook / Microsoft 365

Use Outlook on the web. Open Settings → Calendar → Shared calendars → **Publish a calendar**, choose the calendar and the detail level you want to share, then publish and copy the **ICS** link, not the HTML link. A university/employer may disable publishing or restrict available detail levels. The Outlook phone app may not expose publishing controls; use the web interface when needed.

Titles and locations appear only if the chosen publishing permissions include those details. Choose the least detail that meets your needs. See [Microsoft's calendar-sharing guide](https://support.microsoft.com/office/share-your-calendar-in-outlook-on-the-web-7ecef8ae-139c-40d9-bae2-a23977ee58d5).

## Rename, remove and refresh

Use the dashboard configuration page to rename/recolor feeds or remove one. A display name is local to the Portal; it does not rename the source calendar. Calendar refresh is periodic, so an edit on your phone may take time to appear. Republishing or resetting a secret URL requires reconnecting that feed.

`examples/calendar-feeds.json` is a deliberately non-working placeholder for the full USB configuration API. Replace it with your own data locally and never commit the result. The advanced upstream CalDAV/Google-writing features remain in the source but are not the default documented subscription workflow.
