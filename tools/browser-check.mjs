// Browser QA through an isolated, locally launched Edge debugging session.
// Credentials are read in memory and never logged or passed on the command line.
import fs from 'node:fs/promises';
import path from 'node:path';
const root=path.resolve(import.meta.dirname,'..');
const env=Object.fromEntries((await fs.readFile(path.join(root,'.env'),'utf8')).split(/\r?\n/).filter(l=>l&&!l.startsWith('#')&&l.includes('=')).map(l=>{const n=l.indexOf('=');return [l.slice(0,n),l.slice(n+1)]}));
const target=await (await fetch('http://127.0.0.1:9223/json/new?http://localhost:13000',{method:'PUT'})).json();
const socket=new WebSocket(target.webSocketDebuggerUrl);
await new Promise((resolve,reject)=>{socket.onopen=resolve;socket.onerror=reject});
let nextId=1;const pending=new Map();const errors=[];
socket.onmessage=event=>{const message=JSON.parse(event.data);if(message.id){const listener=pending.get(message.id);pending.delete(message.id);if(message.error)listener?.reject(new Error(message.error.message));else listener?.resolve(message.result)}else if(message.method==='Runtime.exceptionThrown')errors.push(message.params.exceptionDetails.text)};
function send(method,params={}){return new Promise((resolve,reject)=>{const id=nextId++;pending.set(id,{resolve,reject});socket.send(JSON.stringify({id,method,params}))})}
async function evaluate(expression){const result=await send('Runtime.evaluate',{expression,returnByValue:true,awaitPromise:true});if(result.exceptionDetails)throw new Error(result.exceptionDetails.text);return result.result.value}
async function waitFor(expression){for(let i=0;i<40;i++){if(await evaluate(expression))return;await new Promise(r=>setTimeout(r,500))}throw new Error('Browser condition timed out')}
await send('Runtime.enable');await send('Page.enable');await send('Emulation.setDeviceMetricsOverride',{width:1440,height:1000,deviceScaleFactor:1,mobile:false});
await waitFor('!!document.querySelector("input[type=email]")');
for(const [selector,value] of [['input[type=email]',env.BOOTSTRAP_EMAIL],['input[type=password]',env.BOOTSTRAP_PASSWORD]])await evaluate(`(()=>{const input=document.querySelector(${JSON.stringify(selector)});Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,'value').set.call(input,${JSON.stringify(value)});input.dispatchEvent(new Event('input',{bubbles:true}));return true})()`);
await evaluate('document.querySelector("form").requestSubmit()');
await waitFor('!!document.querySelector("main") && document.body.innerText.includes("Dashboard")');
await waitFor('!!document.querySelector(\'main input[placeholder="Rechercher par nom ou code"]\')');
await new Promise(r=>setTimeout(r,2000));
await fs.mkdir(path.join(root,'.cache/screenshots'),{recursive:true});
let shot=await send('Page.captureScreenshot',{format:'png'});await fs.writeFile(path.join(root,'.cache/screenshots/dashboard-desktop.png'),Buffer.from(shot.data,'base64'));
await evaluate('document.querySelector(\'a[href="/m/work-orders"]\').click()');
await waitFor('!!document.querySelector("table")');
await send('Emulation.setDeviceMetricsOverride',{width:390,height:844,deviceScaleFactor:1,mobile:true});
await new Promise(r=>setTimeout(r,1000));
const overflow=await evaluate('document.documentElement.scrollWidth > window.innerWidth+2');
shot=await send('Page.captureScreenshot',{format:'png'});await fs.writeFile(path.join(root,'.cache/screenshots/interventions-mobile.png'),Buffer.from(shot.data,'base64'));
await evaluate('(()=>{const input=Array.from(document.querySelectorAll("input")).find(input=>input.placeholder==="Rechercher dans ce module");Object.getOwnPropertyDescriptor(HTMLInputElement.prototype,"value").set.call(input,"no-result-browser-check");input.dispatchEvent(new Event("input",{bubbles:true}));return true})()');
await waitFor('!!document.querySelector("tbody h6")');
const emptyVisible=await evaluate('(()=>{const bounds=document.querySelector("tbody h6").getBoundingClientRect();return bounds.left>=0&&bounds.right<=window.innerWidth+2})()');
shot=await send('Page.captureScreenshot',{format:'png'});await fs.writeFile(path.join(root,'.cache/screenshots/interventions-mobile-empty.png'),Buffer.from(shot.data,'base64'));
await fs.writeFile(path.join(root,'.cache/browser-result.json'),JSON.stringify({login:true,dashboard:true,interventions:true,mobileOverflow:overflow,emptyStateVisible:emptyVisible,runtimeErrors:errors},null,2));
console.log(JSON.stringify({login:true,dashboard:true,interventions:true,mobileOverflow:overflow,emptyStateVisible:emptyVisible,runtimeErrors:errors}));
await send('Page.close');socket.close();
if(errors.length||overflow||!emptyVisible)process.exitCode=1;
