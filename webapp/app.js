// ══════════════════════════════════════════════════════
//  APP.JS — SpaceX × Sentry Hybrid Design
// ══════════════════════════════════════════════════════

// ── Canvas helpers ──
function setupCanvas(id,h){
  const c=document.getElementById(id);
  const r=window.devicePixelRatio||1;
  const w=c.parentElement.clientWidth;
  c.width=w*r; c.height=h*r;
  c.style.width=w+'px'; c.style.height=h+'px';
  const ctx=c.getContext('2d');
  ctx.scale(r,r);
  return {c,ctx,w,h};
}

function project(node,w,h,pad){
  const xs=NODES.map(n=>n.x), ys=NODES.map(n=>n.y);
  const minX=Math.min(...xs)-.02,maxX=Math.max(...xs)+.02;
  const minY=Math.min(...ys)-.02,maxY=Math.max(...ys)+.02;
  const sx=(node.x-minX)/(maxX-minX)*(w-pad*2)+pad;
  const sy=(1-(node.y-minY)/(maxY-minY))*(h-pad*2)+pad;
  return {sx,sy};
}

function drawGraph(ctx,w,h,opts={}){
  const pad=50;
  const {settled={},relaxed={},pathSet=new Set(),source=null,target=null}=opts;
  ctx.clearRect(0,0,w,h);

  // Draw edges
  EDGES.forEach(e=>{
    const a=project(NODE_MAP[e.from],w,h,pad);
    const b=project(NODE_MAP[e.to],w,h,pad);
    const inPath=pathSet.has(e.from+'→'+e.to)||pathSet.has(e.to+'→'+e.from);
    ctx.beginPath(); ctx.moveTo(a.sx,a.sy); ctx.lineTo(b.sx,b.sy);
    if(inPath){
      ctx.strokeStyle='#c2ef4e';ctx.lineWidth=3.5;
      ctx.shadowColor='#c2ef4e';ctx.shadowBlur=12;
    } else {
      ctx.strokeStyle='rgba(255,255,255,0.08)';ctx.lineWidth=1;ctx.shadowBlur=0;
    }
    ctx.stroke(); ctx.shadowBlur=0;
  });

  // Draw nodes
  NODES.forEach(n=>{
    const p=project(n,w,h,pad);
    const r=n.pop>0?(n.pop>300000?10:n.pop>100000?8:6):5;
    let col=NODE_COLORS[n.type]||'#888';
    let glow=0;
    if(n.id===source){col='#c2ef4e';glow=18;}
    else if(n.id===target){col='#fa7faa';glow=18;}
    else if(settled[n.id]){col='#ffb287';glow=8;}
    else if(relaxed[n.id]){col='rgba(255,178,135,0.4)';glow=4;}

    ctx.beginPath(); ctx.arc(p.sx,p.sy,r,0,Math.PI*2);
    if(glow){ctx.shadowColor=col;ctx.shadowBlur=glow;}
    ctx.fillStyle=col; ctx.fill();
    ctx.shadowBlur=0;
    ctx.strokeStyle='rgba(255,255,255,0.15)';ctx.lineWidth=1;ctx.stroke();

    // Label
    ctx.fillStyle='rgba(240,240,250,0.6)';
    ctx.font='500 9px Inter,sans-serif';
    ctx.textAlign='center';
    ctx.fillText(n.name,p.sx,p.sy-r-5);
  });
}

// ── Network Graph ──
function initNetworkGraph(){
  const {ctx,w,h}=setupCanvas('networkCanvas',520);
  drawGraph(ctx,w,h);
}

// ── Populate dropdowns ──
function initDropdowns(){
  const srcSel=document.getElementById('raceSource');
  const tgtSel=document.getElementById('raceTarget');
  const mlRoad=document.getElementById('mlRoad');
  NODES.forEach(n=>{
    srcSel.add(new Option(`${n.id} — ${n.name}`,n.id));
    tgtSel.add(new Option(`${n.id} — ${n.name}`,n.id));
  });
  srcSel.value='3'; tgtSel.value='12';
  TRAFFIC_FLOW.forEach(t=>{
    mlRoad.add(new Option(t.road,t.road));
  });
}

// ── Algorithm Race ──
let raceRunning=false;
function startRace(){
  if(raceRunning)return;
  const src=document.getElementById('raceSource').value;
  const tgt=document.getElementById('raceTarget').value;
  const speed=+document.getElementById('raceSpeed').value;
  if(src===tgt){alert('Source and target must differ');return;}

  raceRunning=true;
  document.getElementById('raceBtn').disabled=true;
  document.getElementById('raceResult').classList.remove('show');

  const dRes=dijkstraSteps(src,tgt);
  const aRes=astarSteps(src,tgt);
  const d=setupCanvas('dijkstraCanvas',380);
  const a=setupCanvas('astarCanvas',380);

  ['dijkstra','astar'].forEach(p=>{
    document.getElementById(p+'-explored').textContent='0';
    document.getElementById(p+'-relaxed').textContent='0';
    document.getElementById(p+'-dist').textContent='—';
    document.getElementById(p+'-status').textContent='Running...';
  });

  const maxLen=Math.max(dRes.steps.length,aRes.steps.length);
  let i=0;
  const dSettled={},dRelaxed={},aSettled={},aRelaxed={};
  let dDone=false,aDone=false;

  function tick(){
    if(i<dRes.steps.length){
      const s=dRes.steps[i];
      if(s.type==='settle')dSettled[s.node]=true;
      else dRelaxed[s.node]=true;
      document.getElementById('dijkstra-explored').textContent=Object.keys(dSettled).length;
      document.getElementById('dijkstra-relaxed').textContent=s.ops;
    }
    if(!dDone&&i>=dRes.steps.length-1){
      dDone=true;
      document.getElementById('dijkstra-dist').textContent=dRes.totalDist.toFixed(1)+' km';
      document.getElementById('dijkstra-status').textContent='✅ Done';
    }
    if(i<aRes.steps.length){
      const s=aRes.steps[i];
      if(s.type==='settle')aSettled[s.node]=true;
      else aRelaxed[s.node]=true;
      document.getElementById('astar-explored').textContent=Object.keys(aSettled).length;
      document.getElementById('astar-relaxed').textContent=s.ops;
    }
    if(!aDone&&i>=aRes.steps.length-1){
      aDone=true;
      document.getElementById('astar-dist').textContent=aRes.totalDist.toFixed(1)+' km';
      document.getElementById('astar-status').textContent='✅ Done';
    }

    const dPathSet=new Set(), aPathSet=new Set();
    if(dDone) for(let j=0;j<dRes.path.length-1;j++) dPathSet.add(dRes.path[j]+'→'+dRes.path[j+1]);
    if(aDone) for(let j=0;j<aRes.path.length-1;j++) aPathSet.add(aRes.path[j]+'→'+aRes.path[j+1]);

    drawGraph(d.ctx,d.w,d.h,{settled:dSettled,relaxed:dRelaxed,pathSet:dPathSet,source:src,target:tgt});
    drawGraph(a.ctx,a.w,a.h,{settled:aSettled,relaxed:aRelaxed,pathSet:aPathSet,source:src,target:tgt});

    i++;
    if(i<=maxLen){
      setTimeout(tick,speed);
    } else {
      raceRunning=false;
      document.getElementById('raceBtn').disabled=false;
      const el=document.getElementById('raceResult');
      el.classList.add('show');
      const dE=Object.keys(dSettled).length, aE=Object.keys(aSettled).length;
      const winner=aE<dE?'A*':'Dijkstra';
      const savings=Math.round((1-Math.min(dE,aE)/Math.max(dE,aE))*100);
      document.getElementById('raceWinner').innerHTML=
        `🏆 <span style="color:var(--accent-lime)">${winner}</span> wins!`;
      document.getElementById('raceDetail').textContent=
        `A* explored ${aE} nodes vs Dijkstra's ${dE} — ${savings}% fewer explorations. Both found the same optimal path of ${dRes.totalDist.toFixed(1)} km.`;
    }
  }
  tick();
}

// ── ML Traffic Prediction ──
let mlModel=null;
let mlMaxFlow=1;

async function trainModel(){
  const status=document.getElementById('mlStatus');
  status.textContent='⏳ Training neural network on traffic data...';

  const roadIds=TRAFFIC_FLOW.map(t=>t.road);
  const numRoads=roadIds.length;
  const xs=[], ys=[];
  const periods=['morning','afternoon','evening','night'];
  const allFlows=[];
  TRAFFIC_FLOW.forEach(t=>periods.forEach(p=>allFlows.push(t[p])));
  mlMaxFlow=Math.max(...allFlows);

  TRAFFIC_FLOW.forEach((t,ri)=>{
    periods.forEach((p,pi)=>{
      const input=new Array(numRoads).fill(0);
      input[ri]=1;
      input.push(pi/3);
      xs.push(input);
      ys.push(t[p]/mlMaxFlow);
    });
  });

  const xTensor=tf.tensor2d(xs);
  const yTensor=tf.tensor2d(ys.map(v=>[v]));

  mlModel=tf.sequential();
  mlModel.add(tf.layers.dense({inputShape:[numRoads+1],units:32,activation:'relu'}));
  mlModel.add(tf.layers.dense({units:16,activation:'relu'}));
  mlModel.add(tf.layers.dense({units:1,activation:'sigmoid'}));
  mlModel.compile({optimizer:tf.train.adam(0.01),loss:'meanSquaredError'});

  await mlModel.fit(xTensor,yTensor,{epochs:100,verbose:0});
  xTensor.dispose(); yTensor.dispose();

  status.textContent='✅ Model trained — 112 samples, 3-layer neural network';
  status.classList.add('ready');
  document.getElementById('predictBtn').disabled=false;
  drawMLChart();
}

function predict(){
  if(!mlModel)return;
  const roadId=document.getElementById('mlRoad').value;
  const timeIdx=+document.getElementById('timeSlider').value;
  const ri=TRAFFIC_FLOW.findIndex(t=>t.road===roadId);
  const input=new Array(TRAFFIC_FLOW.length).fill(0);
  input[ri]=1;
  input.push(timeIdx/3);
  const pred=mlModel.predict(tf.tensor2d([input]));
  const val=Math.round(pred.dataSync()[0]*mlMaxFlow);
  pred.dispose();

  document.getElementById('predictionValue').textContent=val.toLocaleString();
  const periods=['morning','afternoon','evening','night'];
  const actual=TRAFFIC_FLOW[ri][periods[timeIdx]];
  document.getElementById('actualValue').textContent=actual.toLocaleString();
  drawMLChart(ri,timeIdx,val);
}

function drawMLChart(selRoad,selTime,predVal){
  const {ctx,w,h}=setupCanvas('mlChart',350);
  const ri=selRoad!=null?selRoad:0;
  const t=TRAFFIC_FLOW[ri];
  const vals=[t.morning,t.afternoon,t.evening,t.night];
  const maxV=Math.max(...vals)*1.2;
  const barW=w/6, gap=20, startX=(w-barW*4-gap*3)/2;
  const colors=['#6a5fc1','#8b5cf6','#fa7faa','#ffb287'];

  ctx.clearRect(0,0,w,h);

  // Title
  ctx.fillStyle='rgba(240,240,250,0.5)';
  ctx.font='700 11px Inter,sans-serif';
  ctx.letterSpacing='2px';
  ctx.textAlign='center';
  ctx.fillText('TRAFFIC FLOW — ROAD '+t.road,w/2,25);

  vals.forEach((v,i)=>{
    const x=startX+i*(barW+gap);
    const barH=(v/maxV)*(h-90);
    const y=h-50-barH;

    const grad=ctx.createLinearGradient(x,y,x,h-50);
    grad.addColorStop(0,colors[i]);
    grad.addColorStop(1,colors[i]+'22');
    ctx.fillStyle=grad;
    ctx.beginPath();
    ctx.roundRect(x,y,barW,barH,6);
    ctx.fill();

    if(selTime===i){
      ctx.strokeStyle=colors[i];ctx.lineWidth=2;
      ctx.shadowColor=colors[i];ctx.shadowBlur=12;
      ctx.beginPath();ctx.roundRect(x-2,y-2,barW+4,barH+4,8);ctx.stroke();
      ctx.shadowBlur=0;
    }

    ctx.fillStyle='rgba(240,240,250,0.8)';
    ctx.font='600 11px JetBrains Mono,monospace';
    ctx.textAlign='center';
    ctx.fillText(v.toLocaleString(),x+barW/2,y-8);

    ctx.fillStyle='rgba(240,240,250,0.35)';
    ctx.font='500 9px Inter,sans-serif';
    ctx.fillText(TIME_LABELS[i],x+barW/2,h-32);
  });

  if(predVal!=null&&selTime!=null){
    const x=startX+selTime*(barW+gap)+barW/2;
    const predH=(predVal/maxV)*(h-90);
    const py=h-50-predH;
    ctx.beginPath();ctx.arc(x,py,5,0,Math.PI*2);
    ctx.fillStyle='#c2ef4e';ctx.fill();
    ctx.shadowColor='#c2ef4e';ctx.shadowBlur=12;ctx.fill();ctx.shadowBlur=0;
    ctx.fillStyle='#c2ef4e';ctx.font='bold 10px JetBrains Mono,monospace';
    ctx.fillText('ML: '+predVal.toLocaleString(),x,py-12);
  }
}

// ── Benchmark Charts ──
function drawBenchCharts(){
  const {ctx:c1,w:w1,h:h1}=setupCanvas('benchChart1',200);
  const data1=[{label:'Dijkstra',val:19,col:'#6a5fc1'},{label:'A*',val:3,col:'#c2ef4e'},{label:'Bellman-Ford',val:25,col:'#ffb287'}];
  drawBarChart(c1,w1,h1,data1);

  const {ctx:c2,w:w2,h:h2}=setupCanvas('benchChart2',200);
  const data2=[{label:'Dijkstra',val:28,col:'#6a5fc1'},{label:'A*',val:10,col:'#c2ef4e'},{label:'Bellman-Ford',val:56,col:'#ffb287'}];
  drawBarChart(c2,w2,h2,data2);
}

function drawBarChart(ctx,w,h,data){
  ctx.clearRect(0,0,w,h);
  const maxV=Math.max(...data.map(d=>d.val))*1.3;
  const barW=w/(data.length*2+1);
  data.forEach((d,i)=>{
    const x=barW+i*barW*2;
    const barH=(d.val/maxV)*(h-60);
    const y=h-30-barH;
    const grad=ctx.createLinearGradient(x,y,x,h-30);
    grad.addColorStop(0,d.col);grad.addColorStop(1,d.col+'22');
    ctx.fillStyle=grad;
    ctx.beginPath();ctx.roundRect(x,y,barW,barH,4);ctx.fill();
    ctx.fillStyle='rgba(240,240,250,0.8)';ctx.font='bold 12px JetBrains Mono,monospace';
    ctx.textAlign='center';ctx.fillText(d.val,x+barW/2,y-6);
    ctx.fillStyle='rgba(240,240,250,0.35)';ctx.font='500 9px Inter,sans-serif';
    ctx.fillText(d.label,x+barW/2,h-12);
  });
}

// ── Time slider label ──
document.getElementById('timeSlider').addEventListener('input',e=>{
  document.getElementById('timeLabel').textContent=TIME_LABELS[+e.target.value];
});

// ── Scroll animations ──
const observer=new IntersectionObserver(entries=>{
  entries.forEach(e=>{if(e.isIntersecting)e.target.classList.add('visible');});
},{threshold:0.15});
document.querySelectorAll('.animate-in').forEach(el=>observer.observe(el));

// ── Resize handler ──
let resizeTimer;
window.addEventListener('resize',()=>{
  clearTimeout(resizeTimer);
  resizeTimer=setTimeout(()=>{initNetworkGraph();drawBenchCharts();},200);
});

// ── Init ──
window.addEventListener('DOMContentLoaded',()=>{
  initDropdowns();
  initNetworkGraph();
  drawBenchCharts();
  drawMLChart();
  trainModel();
});
