// Run: node tests/language.test.cjs
const assert = require('node:assert/strict');
const fs = require('node:fs');
const vm = require('node:vm');
const path = require('node:path');
const source = fs.readFileSync(path.join(__dirname, '../js/language.js'), 'utf8');
for (const [navigator, expected] of [
  [{language:'sl'},'sl'], [{languages:['sl-SI','en'],language:'en'},'sl'],
  [{language:'SL_si'},'sl'], [{language:'en-US'},'en'],
  [{languages:['de-DE','sl-SI']},'en'], [{language:'ja-JP'},'en'],
  [{languages:[]},'en'], [{},'en']
]) {
  const text = {nodeValue:' Downloads ',parentElement:{tagName:'A'}};
  const attribute = {tagName:'NAV',getAttribute:key=>key==='aria-label'?'Main navigation':null,
    setAttribute(key,value){this[key]=value;}};
  const document = {documentElement:{},createTreeWalker(){let read=false;return {nextNode(){if(read)return null;read=true;return text;}};},querySelectorAll(){return [attribute];}};
  vm.runInNewContext(source,{navigator,document,NodeFilter:{SHOW_TEXT:4}});
  assert.equal(document.documentElement.lang,expected);
  assert.equal(text.nodeValue,expected==='sl'?' Prenosi ':' Downloads ');
  assert.equal(attribute['aria-label'],expected==='sl'?'Glavna navigacija':undefined);
}
console.log('Passed: 8 language scenarios, translated text and accessibility labels.');
