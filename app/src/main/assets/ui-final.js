/* Impossible Landscapes — interfaz de biblioteca privada sin conexión */
(function(){'use strict';
const CATALOG=Array.isArray(window.IL_CATALOG)?window.IL_CATALOG:[];
const KEY='il_library_final_v1';
const $=s=>document.querySelector(s);
const create=(tag,cls,text)=>{const e=document.createElement(tag);if(cls)e.className=cls;if(text!==undefined)e.textContent=String(text);return e;};
const android=window.Android;
let state={records:{},extra:[]};
try{const old=JSON.parse(localStorage.getItem(KEY)||'null');if(old&&typeof old==='object')state={records:old.records||{},extra:Array.isArray(old.extra)?old.extra:[]};}catch(e){}
const save=()=>{try{localStorage.setItem(KEY,JSON.stringify(state));}catch(e){alert('No se pudo guardar. Exportá un respaldo desde Supervisor.');}};
const allItems=()=>CATALOG.concat(state.extra);
const hasMedia=id=>{try{return !!(android&&android.hasMedia&&android.hasMedia(id));}catch(e){return false;}};
const urlFor=id=>{try{return (android&&android.mediaUrl)?android.mediaUrl(id):'';}catch(e){return '';}};
const rec=id=>state.records[id]||(state.records[id]={favorite:false,status:'pendiente',notes:''});
const phases={night:'The Night Floors',faces:'A Volume of Secret Faces',skin:'Like a Map Made of Skin',end:'End of the World',all:'General'};
let section='home',chosenMap='map-ground-clean',lightbox=null;
function textButton(label,fn,klass=''){const b=create('button',klass,label);b.type='button';b.addEventListener('click',fn);return b;}
function nav(name){
 section=name;
 document.querySelector('header').hidden=name!=='index';
 const set={home:'#ilHome',library:'#ilLibrary',atlas:'#ilAtlas',cases:'#ilCases',index:'#ilIndexHolder',ops:'#opsSuite'};
 document.querySelectorAll('.il-panel').forEach(p=>p.hidden=true);
 $('#opsSuite').hidden=name!=='ops';
 $(set[name]).hidden=false;
 document.querySelectorAll('#il-tabs button').forEach(b=>b.classList.toggle('on',b.dataset.ilnav===name));
 if(name==='library')renderLibrary();
 if(name==='atlas')renderAtlas();
 if(name==='cases')renderCases();
 if(name==='home')renderHome();
 window.scrollTo({top:0,behavior:'auto'});
}
// Conservar intacto el índice antiguo. Sus controles quedan agrupados en una única pantalla.
const holder=$('#ilIndexHolder');
for(const element of Array.from(document.querySelector('main').children)){
 if(element===holder||element.id==='il-tabs'||element.id==='opsSuite'||element.classList.contains('il-panel'))continue;
 holder.append(element);
}
document.querySelectorAll('[data-ilnav]').forEach(b=>b.addEventListener('click',()=>nav(b.dataset.ilnav)));
document.querySelectorAll('[data-go]').forEach(b=>b.addEventListener('click',()=>{
 const phase=b.dataset.filterphase;
 if(phase)$('#ilPhase').value=phase;
 nav(b.dataset.go);
}));
function renderHome(){
 const qty=allItems().filter(x=>hasMedia(x.id)).length;
 $('#ilHomeStatus').textContent=`Biblioteca privada: ${qty} de ${allItems().length} recursos disponibles · índices originales y cuaderno incluidos en la aplicación`;
}
function matches(x){
 const phrase=($('#ilSearch').value||'').normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase();
 const value=(x.title+' '+x.desc+' '+phases[x.phase]+' '+x.kind).normalize('NFD').replace(/[\u0300-\u036f]/g,'').toLowerCase();
 const phase=$('#ilPhase').value,kind=$('#ilKind').value,status=$('#ilLibraryStatus').value;
 if(phrase&&!phrase.split(/\s+/).every(word=>value.includes(word)))return false;
 if(phase!=='all'&&x.phase!==phase&&x.phase!=='all')return false;
 if(kind!=='all'&&x.kind!==kind)return false;
 if(status==='present'&&!hasMedia(x.id))return false;
 if(status==='missing'&&hasMedia(x.id))return false;
 if(status==='favorite'&&!rec(x.id).favorite)return false;
 if(status==='shown'&&rec(x.id).status!=='mostrado')return false;
 return true;
}
function thumbnail(x){
 const src=hasMedia(x.id)?urlFor(x.id):'';
 if(x.typ==='image'&&src){const img=create('img','il-thumb');img.loading='lazy';img.src=src;img.alt=x.title;img.addEventListener('click',()=>viewItem(x,false));return img;}
 const n=create('div','il-thumb-missing',src?(x.typ==='pdf'?'▤ PDF disponible':'♫ Audio disponible'):'Archivo pendiente de importar');
 return n;
}
function importItem(x){if(android&&android.importOne)android.importOne(x.id);else alert('Función disponible en Android.');}
function viewItem(x,present){
 if(!hasMedia(x.id)){importItem(x);return;}
 if(x.typ==='pdf'){
  if(android&&android.openDocument)android.openDocument(x.id);
  else alert('Abrí el documento desde la APK Android.');
  return;
 }
 if(x.typ==='audio'){
  const card=document.querySelector('[data-card-id="'+x.id+'"]');
  const audio=card&&card.querySelector('audio');
  if(audio){audio.play().catch(()=>{});return;}
  alert('Tocá reproducir en el control de audio.');return;
 }
 if(x.typ==='image'){showLightbox(x,present);return;}
}
function card(x){
 const record=rec(x.id),loaded=hasMedia(x.id);
 const box=create('article','il-tile');box.dataset.cardId=x.id;
 const badge=create('div','il-tag',`${phases[x.phase]||x.phase} · ${x.kind}`);box.append(badge,thumbnail(x));
 box.append(create('h3','',x.title),create('p','',x.desc||'Archivo de tu colección personal.'));
 if(x.page){const button=textButton('Manual · PDF '+(x.page+2),()=>{if(android&&android.openPage)android.openPage(x.page+2);});box.append(button);}
 const stateRow=create('div','il-field');
 stateRow.append(create('span','il-status',loaded?'✓ Disponible':'○ Pendiente'));
 const star=textButton(record.favorite?'★ Favorito':'☆ Favorito',()=>{rec(x.id).favorite=!rec(x.id).favorite;save();renderLibrary();});stateRow.append(star);box.append(stateRow);
 if(x.typ==='audio'&&loaded){const player=create('audio');player.controls=true;player.preload='none';player.style.width='100%';player.src=urlFor(x.id);box.append(player);}
 const buttons=create('div','il-actions');
 buttons.append(textButton(loaded?(x.typ==='pdf'?'Abrir PDF':x.typ==='audio'?'Escuchar':'Ampliar'):'Importar',()=>loaded?viewItem(x,false):importItem(x),loaded?'il-primary':''));
 if(x.typ==='image'&&x.scope==='player'&&loaded){buttons.append(textButton('Presentar',()=>{rec(x.id).status='mostrado';save();showLightbox(x,true);}));}
 else if(x.scope==='handler')buttons.append(create('span','il-mini','Solo Supervisor'));
 box.append(buttons);
 const selector=create('select');selector.setAttribute('aria-label','Estado de revelación');
 [['pendiente','No mostrado'],['revisado','Preparado'],['mostrado','Mostrado a jugadores']].forEach(([v,label])=>{const option=create('option','',label);option.value=v;selector.append(option);});
 selector.value=record.status||'pendiente';selector.addEventListener('change',()=>{rec(x.id).status=selector.value;save();});
 box.append(selector);
 return box;
}
function renderLibrary(){
 const items=allItems().filter(matches);const dest=$('#ilCards');dest.replaceChildren();
 items.forEach(item=>dest.append(card(item)));
 $('#ilLibraryCount').textContent=`${items.length} resultados · ${allItems().filter(x=>hasMedia(x.id)).length} de ${allItems().length} recursos importados`;
 if(!items.length)dest.append(create('div','il-callout','No hay recursos que coincidan con los filtros.'));
}
for(const f of ['#ilSearch','#ilPhase','#ilKind','#ilLibraryStatus'])$(f).addEventListener(f==='#ilSearch'?'input':'change',renderLibrary);
$('#ilImportZip').onclick=()=>android&&android.requestPack?android.requestPack():alert('Importación disponible en Android.');
$('#ilAddCustom').onclick=()=>{
 const name=prompt('Nombre de la nueva ayuda (quedará en tu colección privada):');if(!name||!name.trim())return;
 const phase=prompt('Operación: night, faces, skin, end o all','night');if(!['night','faces','skin','end','all'].includes(phase))return alert('Operación inválida.');
 const kind=prompt('Tipo: Mapa, Pista, Documento, Objeto, Ambientación, Audio o PDF','Pista');if(!['Mapa','Pista','Documento','Objeto','Ambientación','Audio','PDF'].includes(kind))return alert('Tipo inválido.');
 const count=state.extra.length;
 if(count>=150)return alert('Alcanzaste el máximo de 150 recursos propios.');
 const typ=kind==='PDF'?'pdf':kind==='Audio'?'audio':'image';
 const item={id:'propio-'+Date.now()+'-'+count,file:'',title:name.trim().slice(0,120),phase,kind,page:0,scope:'handler',desc:'Ayuda incorporada por el Supervisor',typ,group:''};
 state.extra.push(item);save();renderLibrary();importItem(item);
};
function renderAtlas(){
 const maps=allItems().filter(x=>x.kind==='Mapa'&&x.group);
 const bar=$('#ilMapChooser');bar.replaceChildren();
 for(const m of maps){const b=textButton(m.title,()=>{chosenMap=m.id;renderAtlas();},chosenMap===m.id?'chosen':'');bar.append(b);}
 let selected=maps.find(x=>x.id===chosenMap)||maps[0];if(!selected)return;
 const dest=$('#ilActiveMap');dest.replaceChildren();dest.append(create('h3','',selected.title));
 dest.append(create('p','il-mini',selected.desc));
 if(hasMedia(selected.id)){
  const image=create('img');image.src=urlFor(selected.id);image.alt=selected.title;
  image.addEventListener('click',()=>showLightbox(selected,false));dest.append(image);
  dest.append(textButton('Ampliar / mover mapa',()=>showLightbox(selected,false),'il-primary'));
 }else{dest.append(create('div','il-thumb-missing','Mapa todavía no importado'));
  dest.append(textButton('Seleccionar archivo del mapa',()=>importItem(selected),'il-primary'));
 }
 dest.append(create('p','il-mini','Abrir página del manual: '+(selected.page?('PDF '+(selected.page+2)):'sin referencia')));
}
function renderCases(){const dest=$('#ilCaseItems');dest.replaceChildren();for(const id of ['doc-handouts','doc-ancillary','doc-missing']){
 const x=allItems().find(x=>x.id===id);if(x)dest.append(card(x));
}}
function showLightbox(x,present){
 if(lightbox)lightbox.remove();
 const v=create('div');v.id='ilLightbox';
 const tools=create('div');tools.id='ilLightbar';
 tools.append(create('span',present?'il-white':'il-warning',present?'AYUDA PARA JUGADORES':x.title));
 tools.append(textButton('−',()=>zoomBy(.75)),textButton('+',()=>zoomBy(1.35)),textButton('Restablecer',()=>fitImage()),textButton('Cerrar ✕',()=>close()));
 v.append(tools);
 const stage=create('div');stage.id='ilStage';
 const img=create('img');img.src=urlFor(x.id);img.alt=present?'Evidencia':x.title;img.draggable=false;stage.append(img);v.append(stage);
 document.body.append(v);lightbox=v;
 const pointers=new Map();let scale=1,dx=0,dy=0,startDist=0,startScale=1,prevX=0,prevY=0;
 function apply(){img.style.transform=`translate(${dx}px,${dy}px) scale(${scale})`;}
 function fitImage(){scale=1;dx=0;dy=0;img.style.maxWidth='97%';img.style.maxHeight='94%';apply();}
 function zoomBy(factor){scale=Math.max(.5,Math.min(7,scale*factor));apply();}
 function close(){v.remove();if(lightbox===v)lightbox=null;}
 img.onload=fitImage;
 stage.addEventListener('pointerdown',e=>{stage.setPointerCapture(e.pointerId);pointers.set(e.pointerId,{x:e.clientX,y:e.clientY});prevX=e.clientX;prevY=e.clientY;if(pointers.size===2){const ps=[...pointers.values()];startDist=Math.hypot(ps[0].x-ps[1].x,ps[0].y-ps[1].y);startScale=scale;}});
 stage.addEventListener('pointermove',e=>{if(!pointers.has(e.pointerId))return;const previous=pointers.get(e.pointerId);pointers.set(e.pointerId,{x:e.clientX,y:e.clientY});if(pointers.size===2){const ps=[...pointers.values()];const dist=Math.hypot(ps[0].x-ps[1].x,ps[0].y-ps[1].y);if(startDist>0)scale=Math.max(.5,Math.min(7,startScale*dist/startDist));}else if(pointers.size===1){dx+=e.clientX-previous.x;dy+=e.clientY-previous.y;}apply();});
 const up=e=>{pointers.delete(e.pointerId);if(pointers.size===1){const p=[...pointers.values()][0];prevX=p.x;prevY=p.y;}};
 stage.addEventListener('pointerup',up);stage.addEventListener('pointercancel',up);
 v.addEventListener('click',e=>{if(e.target===v)close();});
}
// Cruce del índice editorial con la biblioteca: los enlaces se generan por página física real.
const entryIndex=new Map((typeof DATA!=='undefined'&&Array.isArray(DATA)?DATA:[]).map(x=>[x.id,x]));
function linkIndexEvidence(){
 const results=$('#results');if(!results)return;
 results.querySelectorAll('article.card').forEach(card=>{
  if(card.querySelector('.il-related'))return;
  const entry=entryIndex.get(card.id);if(!entry||!Array.isArray(entry.pages))return;
  const related=allItems().filter(x=>x.page&&entry.pages.includes(x.page+2));
  if(!related.length)return;
  const strip=create('div','il-related');strip.style.marginTop='12px';
  strip.append(create('div','il-mini','Ayudas vinculadas a esta página del manual:'));
  for(const x of related.slice(0,3)){strip.append(textButton('◈ '+x.title,()=>{
    $('#ilSearch').value=x.title;$('#ilPhase').value='all';$('#ilKind').value='all';$('#ilLibraryStatus').value='all';nav('library');
  }));}
  card.append(strip);
 });
}
const resultNode=$('#results');
if(resultNode){new MutationObserver(linkIndexEvidence).observe(resultNode,{childList:true});linkIndexEvidence();}
window.ilMediaImported=function(count){renderHome();if(section==='library')renderLibrary();if(section==='atlas')renderAtlas();if(section==='cases')renderCases();};
// El cuaderno anterior y las evidencias se exportan juntos. Los PDF e imágenes se reimportan con el ZIP.
$('#opsExport').onclick=()=>{
 const base=JSON.parse(localStorage.getItem('il_camp_final_v1')||'{"agents":[],"clues":[],"memo":""}');
 const data=JSON.stringify({format:'il-archivo-v3',base:base,library:state});
 if(android&&android.exportCampaign)android.exportCampaign(data);else alert('Disponible desde la APK Android.');
};
window.restoreCampaign=function(payload){
 try{
  const data=JSON.parse(payload);const base=data.format==='il-archivo-v3'?data.base:data;
  if(!base||!Array.isArray(base.agents)||!Array.isArray(base.clues))throw Error('No es un cuaderno válido');
  if(!confirm('Importar el respaldo reemplazará las notas y marcas actuales. ¿Continuar?'))return;
  localStorage.setItem('il_camp_final_v1',JSON.stringify(base));
  if(data.format==='il-archivo-v3'&&data.library){
   const lib=data.library;if(!lib||!Array.isArray(lib.extra)||typeof lib.records!=='object')throw Error('Biblioteca dañada');
   localStorage.setItem(KEY,JSON.stringify({records:lib.records,extra:lib.extra}));
  }
  alert('Respaldo importado. Se volverá a cargar la aplicación.');location.reload();
 }catch(e){alert('Respaldo incompatible o dañado: '+e.message);}
};
nav('home');
})();
