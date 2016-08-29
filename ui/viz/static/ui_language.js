var saved_queries = [
    {
        // This query is just an example. It should be deleted once a few more have been created.
        "name" : "2 Hops Away (limit 10)",
        "is_relevant" : function(n) {return n.label === "Subject"},
        "floating_query" : ".bothE().bothV().bothE().bothV().limit(10)"
    }, {
        "name" : "Previous",
        "is_relevant" : function(n) {return n.label === "Entity-File" && n['properties']['file-version'][0]['value'] > 1},
        "floating_query" : ".outE('EDGE_OBJECT_PREV_VERSION out').inV().outE('EDGE_OBJECT_PREV_VERSION in').inV()"
    }, {
        "name" : "Owner",
        "is_relevant" : function(n) {return n.label === "Subject" && n['properties']['subjectType'][0]['value'] == 0},
        "floating_query" : ".both().hasLabel('EDGE_SUBJECT_HASLOCALPRINCIPAL').out()",
        "is_default" : true
    }, {
        "name" : "Next",
        "is_relevant" : function(n) {return n.label === "Entity-File" && n['properties']['file-version'][0]['value'] > 1},
        "floating_query" : ".in().hasLabel('EDGE_OBJECT_PREV_VERSION').in()"
    }, {
        "name" : "Affected Event",
        "is_relevant" : function(n) {return n.label === "Entity-File"},
        "floating_query" : ".in().hasLabel('EDGE_FILE_AFFECTS_EVENT').both().hasLabel('Subject').has('subjectType',4)"
    }, {
        "name" : "Readers",
        "is_relevant" : function(n) {return n.label === "Entity-File"},
        "floating_query" : ".in().hasLabel('EDGE_EVENT_AFFECTS_FILE').in().has('eventType',17).out().hasLabel('EDGE_EVENT_ISGENERATEDBY_SUBJECT').both().hasLabel('Subject').has('subjectType',0).dedup().by('pid')"
    }, {
        "name" : "Writer",
        "is_relevant" : function(n) {return n.label === "Entity-File"},
        "floating_query" : ".in().hasLabel('EDGE_EVENT_AFFECTS_FILE').in().has('eventType',21).out().hasLabel('EDGE_EVENT_ISGENERATEDBY_SUBJECT').both().hasLabel('Subject').has('subjectType',0).dedup().by('pid')"
    }, {
        "name" : "Parent",
        "is_relevant" : function(n) {return n.label === "Subject" && n['properties']['subjectType'][0]['value'] == 0},
        "floating_query" : ".as('child').in().hasLabel('EDGE_EVENT_AFFECTS_SUBJECT').in().both().hasLabel('EDGE_EVENT_ISGENERATEDBY_SUBJECT').out().has('pid',select('child').values('ppid'))"
    }, {
        "name" : "URLs Written",
        "is_relevant" : function(n) {return n.label === "Subject" && n['properties']['subjectType'][0]['value'] == 0}, 
        "floating_query" : ".in().hasLabel('EDGE_EVENT_ISGENERATEDBY_SUBJECT').in().hasLabel('Subject').has('subjectType',4).has('eventType',21).out().hasLabel('EDGE_EVENT_AFFECTS_FILE').out().hasLabel('Entity-File').dedup().by('url')"
    }, {
        "name" : "URLs Read",
        "is_relevant" : function(n) {return n.label === "Subject" && n['properties']['subjectType'][0]['value'] == 0}, 
        "floating_query" : ".in().hasLabel('EDGE_EVENT_ISGENERATEDBY_SUBJECT').in().hasLabel('Subject').has('subjectType',4).has('eventType',17).out().hasLabel('EDGE_EVENT_AFFECTS_FILE').out().hasLabel('Entity-File').dedup().by('url')"       
    }, {
        "name" : "Child",
        "is_relevant" : function(n) {return n.label === "Subject" && n['properties']['subjectType'][0]['value'] == 0}, 
        "floating_query" : ".in().hasLabel('EDGE_EVENT_ISGENERATEDBY_SUBJECT').in().hasLabel('Subject').has('subjectType',4).has('eventType',10).outE().inV().hasLabel('Subject')"
    }
]

var saved_nodes = [
    {   // Icon codes:  http://ionicons.com/cheatsheet.html   
        // NOTE: the insertion of 'u' to make code prefixes of '\uf...' as below; because javascript.
        name : "Cluster",
        is_relevant : function(n) { return node_data_set.get(n.id) && network.isCluster(n.id) },
        icon_unicode : "\uf413",
        color : "red",  // setting color here will always override query-specific colors.
        size: 54
        // make_node_label : SPECIAL CASE!!! Don't put anything here right now.
    }, {
        name : "File",
        is_relevant : function(n) { return n.label === "Entity-File" },
        icon_unicode : "\uf41b",
        size: 40,
        make_node_label : function(node) {
            var url = (node['properties'].hasOwnProperty('url') ? node['properties']['url'][0]['value'] : "None")
            var file_version = (node['properties'].hasOwnProperty('file-version') ? node['properties']['file-version'][0]['value'] : "None")
            return "File " + url + " : " + file_version
        }
    }, {
        name : "Agent",
        is_relevant : function(n) { return n.label === "Agent" },
        icon_unicode : "\uf25d",
        size: 50,
        make_node_label : function(node) {
            var at = (node['properties'].hasOwnProperty('agentType') ? node['properties']['agentType'][0]['value'] : "None")
            return at + " Agent, uid " + node['properties']['userID'][0]['value']
        }
    }, {
        name : "Entity-NetFlow",
        is_relevant : function(n) { return n.label === "Entity-NetFlow" },
        make_node_label : function(node) {
            var dest = (node['properties'].hasOwnProperty('dstAddress') ? node['properties']['dstAddress'][0]['value'] : "None")
            var port = (node['properties'].hasOwnProperty('dstPort') ? node['properties']['dstPort'][0]['value'] : "None")
            return "Net " + dest + " : " + port
        }
    }, {
        name : "Subject",
        is_relevant : function(n) { return n.label === "Subject" },
        make_node_label : function(node) {
            var e = (node['properties'].hasOwnProperty('eventType') ? node['properties']['eventType'][0]['value'] : "None")
            var pid = (node['properties'].hasOwnProperty('pid') ? node['properties']['pid'][0]['value'] : "None")
            var t = (node['properties'].hasOwnProperty('subjectType') ? node['properties']['subjectType'][0]['value'] : "None")
            switch(t) {
                case "Process":
                    return t + " " + pid
                case "Thread":
                    return t + " of " + pid
                case "Event":
                    if (e === "Write" || e === "Read") {
                        var temp = node['properties'].hasOwnProperty('size') ? node['properties']['size'][0]['value'] : "size unknown"
                        return t + " " + e + " (" + temp + ")" 
                    } else { return t + " " + e }
                default:
                    return t
            }
        }
    }, {
        name : "Default",   // This will override anything below here!!!!
        is_relevant : function(n) { return true },
        icon_unicode : "\uf3a6",
        size: 30,
        make_node_label : function(n) {
            return n['label'].replace(/^(EDGE_)/,"").replace(/^(EVENT_)/,"")
        }
     // color : do not set a color for default values, or it will always override query-time color choice.
    }
]

var starting_queries = [

    {
        name : "find file by name & version",
        base_query : "g.V().has('label','Entity-File').has('url',{_}).has('file-version',{_})",
        default_values : ["myfile.txt",1]
    },
    {
        name : "find process by pid",
        base_query : "g.V().has('label','Subject').has('subjectType',0).has('pid',{_})",
        default_values : [1001]
    },
    {
        name : "find up to n processes of an owner",
        base_query : "g.V().has('label','localPrincipal').has('userID',{_}).both().hasLabel('EDGE_SUBJECT_HASLOCALPRINCIPAL').out().has('label','Subject').has('subjectType',0).limit({_})",
        default_values : [1234,10]
    },
    {
        name : "find NetFlow by dstAddress & port",
        base_query : "g.V().has('label','Entity_NetFlow').has('dstAddress',{_}).has('port',{_})",
        default_values : ["127.0.0.1",80]
    }
    
]