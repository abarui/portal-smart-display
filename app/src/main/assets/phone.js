"use strict";
(() => {
  const $ = id => document.getElementById(id);
  const providers = {
    icloud: {
      name: "My iCloud calendar", heading: "Get the link on your iPhone",
      steps: [
        "Open the <strong>Calendar</strong> app and tap <strong>Calendars</strong>.",
        "Tap <strong>ⓘ</strong> beside the iCloud calendar you want to show.",
        "Turn on <strong>Public Calendar</strong>. Tap <strong>Share Link → Copy</strong>.",
        "Return to this page and paste the link below."
      ],
      note: "Public Calendar creates a read-only link. Anyone who has that link can see this calendar’s events. This does not share your other calendars. If you prefer to keep it private, use the iCloud account connection in the Portal’s calendar settings instead.",
      consent: "I understand that anyone with this iCloud calendar link can read its events."
    },
    google: {
      name: "My Google calendar", heading: "Copy your calendar’s subscription link",
      steps: [
        "Open <strong>Google Calendar in a web browser</strong>. On a phone, you may need <strong>Request Desktop Website</strong>.",
        "Open <strong>Settings</strong> and choose the calendar you want to show.",
        "Under <strong>Integrate calendar</strong>, copy the <strong>Secret address in iCal format</strong>.",
        "Return to this page and paste that link below."
      ],
      note: "Use the secret iCal address, not the calendar’s browser address. You do not need to make your Google calendar public. Keep the subscription link private.",
      consent: "I understand this link gives read access to the calendar, and I want to add it to this Portal."
    },
    outlook: {
      name: "My Outlook calendar", heading: "Copy your Outlook subscription link",
      steps: [
        "Open <strong>Outlook Calendar in a web browser</strong> and go to <strong>Settings</strong>.",
        "Choose <strong>Calendar → Shared calendars → Publish a calendar</strong>.",
        "Choose your calendar and the details to share, tap <strong>Publish</strong>, then copy the <strong>ICS link</strong>.",
        "Return to this page and paste the ICS link below."
      ],
      note: "Publishing creates a read-only link. Anyone with it can see the details you choose to share. Some work accounts do not allow calendar publishing.",
      consent: "I understand that anyone with this published calendar link can read the details I chose to share."
    }
  };
  let provider = "icloud";
  let token = "";
  let ready = false;
  let busy = false;
  let finished = false;
  try {
    token = new URLSearchParams(location.hash.slice(1)).get("key") || sessionStorage.getItem("portal-pairing") || "";
    if (token) sessionStorage.setItem("portal-pairing", token);
    if (location.hash) history.replaceState(null, "", location.pathname);
  } catch (_) { /* The QR fragment still supplies the token if storage is unavailable. */ }

  function chooseProvider(value) {
    const old = providers[provider];
    provider = value;
    const info = providers[value];
    document.querySelectorAll("[data-provider]").forEach(button => button.setAttribute("aria-pressed", String(button.dataset.provider === value)));
    $("stepsHeading").textContent = info.heading;
    $("steps").innerHTML = info.steps.map(step => "<li><span>" + step + "</span></li>").join("");
    $("providerNote").textContent = info.note;
    $("sharingText").textContent = info.consent;
    if (!$("name").value.trim() || $("name").value === old.name) $("name").value = info.name;
    $("url").value = "";
    $("sharing").checked = false;
    $("formError").hidden = true;
  }

  async function api(path, data) {
    const controller = new AbortController();
    const timeout = setTimeout(() => controller.abort(), data ? 75000 : 10000);
    try {
      const response = await fetch(path, {
        method: data ? "POST" : "GET", credentials: "omit", cache: "no-store", signal: controller.signal,
        headers: {"X-Portal-Token": token, ...(data ? {"Content-Type": "application/json"} : {})},
        ...(data ? {body: JSON.stringify(data)} : {})
      });
      const result = await response.json();
      if (!response.ok) {
        const error = new Error(result.error || "Could not connect. Please try again.");
        error.expired = response.status === 401;
        throw error;
      }
      return result;
    } finally { clearTimeout(timeout); }
  }

  function showSessionError(message) {
    ready = false;
    $("connection").textContent = "Setup needs a new connection";
    $("sessionError").textContent = message;
    $("sessionError").hidden = false;
    $("connectForm").hidden = true;
  }

  async function checkSession() {
    if (busy || finished) return;
    if (!token) return showSessionError("On the Portal, tap Add calendar and scan the QR code with your phone’s camera. Keep both devices on the same Wi-Fi.");
    try {
      await api("/api/session");
      ready = true;
      $("connection").textContent = "Connected to your Portal";
      $("sessionError").hidden = true;
      $("connectForm").hidden = false;
    } catch (error) {
      showSessionError(error.expired ? error.message : "Cannot reach your Portal. Check that your phone is on the same Wi-Fi, then scan the QR code again.");
    }
  }

  document.querySelectorAll("[data-provider]").forEach(button => button.addEventListener("click", () => chooseProvider(button.dataset.provider)));
  $("connectForm").addEventListener("submit", async event => {
    event.preventDefault();
    if (!ready || busy || !$("connectForm").reportValidity()) return;
    busy = true;
    $("formFields").disabled = true;
    $("submit").textContent = "Connecting…";
    $("formError").hidden = true;
    try {
      const result = await api("/api/subscribe", {
        provider, name: $("name").value.trim(), url: $("url").value.trim(), sharingAccepted: $("sharing").checked
      });
      finished = true;
      $("url").value = "";
      $("connectedName").textContent = result.name;
      $("connectForm").hidden = true;
      $("success").hidden = false;
      window.scrollTo({top: 0, behavior: "smooth"});
    } catch (error) {
      if (error.expired) showSessionError(error.message);
      else {
        $("formError").textContent = error.name === "AbortError" ? "This is taking longer than expected. Check your Wi-Fi and try again." : error.message || "Could not connect. Please try again.";
        $("formError").hidden = false;
      }
    } finally {
      busy = false;
      $("formFields").disabled = false;
      $("submit").textContent = "Connect calendar";
    }
  });
  $("another").addEventListener("click", () => {
    finished = false;
    $("success").hidden = true;
    $("sharing").checked = false;
    $("name").value = providers[provider].name;
    checkSession();
  });
  document.addEventListener("visibilitychange", () => { if (!document.hidden) checkSession(); });
  chooseProvider("icloud");
  checkSession();
  setInterval(checkSession, 30000);
})();
