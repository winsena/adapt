graph = TitanFactory.open('cassandra:localhost'); g = graph.traversal();

def startWindow(t,d) { return t - t % d }
def endWindow(t,d) { return t + d - t % d }

def startHour(t) { startWindow(t,1000*1000*60*60) }
def endHour(t) { endWindow(t,1000*1000*60*60) }
def startMin(t) { startWindow(t,1000*1000*60) }
def endMin(t) { endWindow(t,1000*1000*60) }

def getTimes(g) { g.V().has('startedAtTime').values('startedAtTime').is(gt(0)) }

def addTimeSegments(g,delta) {
  segments = getTimes(g).map{startWindow((long)it.get(),delta)}.dedup()
  for(s in segments) {
    graph.addVertex(label,'Segment','segment:name','byTime','startedAtTime',s)
  }
}


def removeSegments(g) {
  g.V().has(label,'Segment').has('segment:name','byTime').drop().iterate()
}

:> @script3