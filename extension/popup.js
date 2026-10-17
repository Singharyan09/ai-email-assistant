const API_BASE = "http://localhost:8080/api/email";
const output = document.getElementById("output");

async function getEmailFromTab() {
  const [tab] = await chrome.tabs.query({ active: true, currentWindow: true });
  if (!tab || !tab.url || !tab.url.startsWith("https://mail.google.com/")) {
    throw new Error("Open Gmail in the active tab first.");
  }
  const email = await chrome.tabs.sendMessage(tab.id, { type: "GET_EMAIL" });
  if (!email || !email.body) {
    throw new Error("No open email found. Open an email thread and try again.");
  }
  return email;
}

async function run(endpoint) {
  output.textContent = "Working...";
  try {
    const email = await getEmailFromTab();
    const tone = document.getElementById("tone").value;
    const res = await fetch(`${API_BASE}/${endpoint}`, {
      method: "POST",
      headers: { "Content-Type": "application/json" },
      body: JSON.stringify({ ...email, tone }),
    });
    if (!res.ok) throw new Error(`Backend returned ${res.status}`);
    const data = await res.json();
    output.textContent = data.result;
  } catch (err) {
    output.textContent = err.message;
  }
}

document.getElementById("summarize").addEventListener("click", () => run("summarize"));
document.getElementById("reply").addEventListener("click", () => run("reply"));
document.getElementById("copy").addEventListener("click", () => {
  navigator.clipboard.writeText(output.textContent);
});
