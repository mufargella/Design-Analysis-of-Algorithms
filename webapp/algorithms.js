// Dijkstra (binary heap) and A* implementations
// Returns {dist, prev, steps} where steps is array of {node, type} for animation

function dijkstraSteps(source, target){
  const dist={}, prev={}, settled={}, steps=[];
  let ops=0, pops=0;
  NODES.forEach(n=>{dist[n.id]=Infinity; prev[n.id]=null; settled[n.id]=false;});
  dist[source]=0;
  // Simple min-heap via sorted array (fine for 25 nodes)
  const pq=[{k:0,v:source}];
  while(pq.length){
    pq.sort((a,b)=>a.k-b.k);
    const {k,v:u}=pq.shift();
    if(settled[u]) continue;
    if(k>dist[u]) continue;
    settled[u]=true; pops++;
    steps.push({node:u,type:"settle",dist:dist[u],ops,pops});
    if(u===target) break;
    for(const e of ADJ[u]){
      if(settled[e.to]) continue;
      ops++;
      const alt=dist[u]+e.w;
      if(alt<dist[e.to]){
        dist[e.to]=alt; prev[e.to]=u;
        pq.push({k:alt,v:e.to});
        steps.push({node:e.to,type:"relax",dist:alt,ops,pops});
      }
    }
  }
  // reconstruct path
  const path=[];
  let c=target;
  while(c){path.unshift(c);c=prev[c];}
  if(path[0]!==source) return {dist,prev,steps,path:[],totalDist:Infinity,ops,pops};
  return {dist,prev,steps,path,totalDist:dist[target],ops,pops};
}

function astarSteps(source, target){
  const gScore={}, prev={}, closed={}, steps=[];
  let ops=0, pops=0;
  const tgt=NODE_MAP[target];
  NODES.forEach(n=>{gScore[n.id]=Infinity; prev[n.id]=null; closed[n.id]=false;});
  gScore[source]=0;
  const pq=[{f:haversine(NODE_MAP[source],tgt),v:source}];
  while(pq.length){
    pq.sort((a,b)=>a.f-b.f);
    const {v:u}=pq.shift();
    if(closed[u]) continue;
    closed[u]=true; pops++;
    steps.push({node:u,type:"settle",dist:gScore[u],ops,pops});
    if(u===target) break;
    for(const e of ADJ[u]){
      if(closed[e.to]) continue;
      ops++;
      const tent=gScore[u]+e.w;
      if(tent<gScore[e.to]){
        gScore[e.to]=tent; prev[e.to]=u;
        pq.push({f:tent+haversine(NODE_MAP[e.to],tgt),v:e.to});
        steps.push({node:e.to,type:"relax",dist:tent,ops,pops});
      }
    }
  }
  const path=[];
  let c=target;
  while(c){path.unshift(c);c=prev[c];}
  if(path[0]!==source) return {gScore,prev,steps,path:[],totalDist:Infinity,ops,pops};
  return {gScore,prev,steps,path,totalDist:gScore[target],ops,pops};
}
