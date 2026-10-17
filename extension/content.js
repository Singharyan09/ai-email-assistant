// Reads the currently open email from the Gmail page.
// Gmail's class names can change; adjust these selectors if extraction stops working.
function readOpenEmail() {
  const subject = document.querySelector("h2.hP")?.innerText?.trim() || "";
  const bodies = document.querySelectorAll("div.a3s.aiL");
  const lastBody = bodies.length ? bodies[bodies.length - 1] : null;
  return { subject, body: lastBody ? lastBody.innerText.trim() : "" };
}

chrome.runtime.onMessage.addListener((message, _sender, sendResponse) => {
  if (message.type === "GET_EMAIL") {
    sendResponse(readOpenEmail());
  }
});
