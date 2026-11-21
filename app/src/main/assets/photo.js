"use strict";
(async()=>{
 const status=document.getElementById("status");
 const key=new URLSearchParams(location.hash.slice(1)).get("key");
 if(!key){status.textContent="Open Export photo on the Portal, then scan its QR code.";return;}
 try{
  const response=await fetch("/api/photo",{headers:{"X-Portal-Photo-Key":key},cache:"no-store"});
  if(!response.ok){const body=await response.json();throw new Error(body.error||"Couldn’t open photo.");}
  const url=URL.createObjectURL(await response.blob());
  const photo=document.getElementById("photo");photo.src=url;photo.hidden=false;
  const save=document.getElementById("save");save.href=url;save.hidden=false;
  document.getElementById("hint").hidden=false;status.textContent="Ready to save from your Portal.";
  window.addEventListener("pagehide",()=>URL.revokeObjectURL(url),{once:true});
 }catch(error){status.textContent=error.message||"Connect to the same Wi-Fi as the Portal and try again.";}
})();
