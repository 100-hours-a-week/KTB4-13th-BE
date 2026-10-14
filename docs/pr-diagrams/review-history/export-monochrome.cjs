const fs = require('node:fs');
const {chromium} = require('playwright');
(async () => {
 const path=require('node:path');
 const dir=path.resolve(process.argv[2] || __dirname);
 const browser=await chromium.launch({headless:true,channel:"chrome"});
 const page=await browser.newPage({viewport:{width:1600,height:1000},acceptDownloads:true});
 await page.goto('file://'+dir+'/review-history.html?theme=light');
 await page.evaluate(async()=>{await document.fonts.ready});
 await page.evaluate(()=>{
  const svg=document.querySelector('.diagram-container svg');
  for(const el of [svg,...svg.querySelectorAll('*')]){
   if(el.closest('defs') && !el.closest('marker'))continue;
   const style=getComputedStyle(el);
   const fill=style.fill;
   const stroke=style.stroke;
   const tag=el.tagName.toLowerCase();
   if(fill!=='none'&&fill!=='rgba(0, 0, 0, 0)'){
    const color=['text','tspan','path','polygon'].includes(tag)?'#000000':'#ffffff';
    el.style.setProperty('fill',color,'important');
   }
   if(stroke!=='none'&&stroke!=='rgba(0, 0, 0, 0)')el.style.setProperty('stroke','#000000','important');
   el.style.setProperty('filter','none','important');
   el.style.setProperty('opacity','1','important');
  }
  document.body.style.backgroundColor='#ffffff';
 });
 const pending=page.waitForEvent('download');
 await page.evaluate(()=>document.querySelector('button[data-format="png"]').click());
 const download=await pending;await download.saveAs(dir+'/review-history.monochrome.png');
 const receipt=await page.evaluate(()=>Object.fromEntries([...document.documentElement.attributes].filter(x=>x.name.startsWith('data-last-export')).map(x=>[x.name,x.value])));
 fs.writeFileSync(dir+'/monochrome-export.json',JSON.stringify({source:'review-history.html',derivative:'review-history.monochrome.png',style:'white background, black strokes and text; geometry unchanged',receipt},null,2)+'\n');
 await browser.close();
})().catch(e=>{process.stderr.write(e.stack);process.exit(1)});
