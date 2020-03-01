/*
*	Touch Panel Driver
*	Code written for RGBGenie by Bryan Copeland
*
*   Updated 2020-02-26 Added importUrl
*/
metadata {
	definition (name: "RGBGenie Touch Panel ZW", namespace: "rgbgenie", author: "RGBGenie") {
        capability "Actuator"
        capability "Configuration"
        capability "Refresh"
        attribute "associationsG1", "string"
        attribute "associationsG2", "string"
        attribute "associationsG3", "string"
        attribute "associationsG4", "string"

		fingerprint mfr:"0330", prod:"0301", model:"A109", deviceJoinName: "RGBGenie Dimmer Touch Panel"
        fingerprint mfr:"0330", prod:"0301", model:"A106", deviceJoinName: "RGBGenie 3 Scene Color Touch Panel"
        fingerprint mfr:"0330", prod:"0301", model:"A105", deviceJoinName: "RGBGenie 3 Zone Color Touch Panel"
        fingerprint mfr:"0330", prod:"0301", model:"A101", deviceJoinName: "RGBGenie Color Temperature Touch Panel"

    }
    tiles(scale: 2) {
        valueTile("associationsG1", "device.associationsG1", decoration: "flat", width: 3, height: 1) {
            state "associationsG1", label:'G1: ${currentValue}'
        }    
        valueTile("associationsG2", "device.associationsG2", decoration: "flat", width: 3, height: 1) {
            state "associationsG2", label:'Z1: ${currentValue}'
        }
        valueTile("associationsG3", "device.associationsG3", decoration: "flat", width: 3, height: 1) {
            state "associationsG3", label:'Z2: ${currentValue}'
        }        
        valueTile("associationsG4", "device.associationsG4", decoration: "flat", width: 3, height: 1) {
            state "associationsG4", label:'Z2: ${currentValue}'
        }
        standardTile("associations", "device.status", decoration: "flat", width: 3, height:1) {
        	state "default", label:'Association'        
        }
        standardTile("states", "device.status", decoration: "flat", width: 3, height:1) {
        	state "default", label:'States'
        }
    	standardTile("zone1", "device.status", decoration: "flat", width: 3, height:1) {
        	state "default", label:'Zone 1'
        }
        standardTile("zone2", "device.status", decoration: "flat", width: 3, height:1) {
        	state "default", label:'Zone 2'
        }
        standardTile("zone3", "device.status", decoration: "flat", width: 3, height:1) {
        	state "default", label:'Zone 3'
        }
        childDeviceTile("zone1colorMode", "zone1", decoration: "flat", width: 3, height:1, childTileName: "colorMode") 
        childDeviceTile("zone1switch", "zone1", decoration: "flat", width: 3, height:1, childTileName: "switch") 
        childDeviceTile("zone1level", "zone1", decoration: "flat", width: 3, height:1, childTileName: "level") 
        childDeviceTile("zone1colorTemp", "zone1", decoration: "flat", width: 3, height:1, childTileName: "colorTemp")
        childDeviceTile("zone1color", "zone1", decoration: "flat", width: 3, height:1, childTileName: "color") 
        childDeviceTile("zone1hue", "zone1", decoration: "flat", width: 3, height:1, childTileName: "hue") 
        childDeviceTile("zone2colorMode", "zone2", decoration: "flat", width: 3, height:1, childTileName: "colorMode") 
        childDeviceTile("zone2switch", "zone2", decoration: "flat", width: 3, height:1, childTileName: "switch") 
        childDeviceTile("zone2level", "zone2", decoration: "flat", width: 3, height:1, childTileName: "level") 
        childDeviceTile("zone2colorTemp", "zone2", decoration: "flat", width: 3, height:1, childTileName: "colorTemp")
        childDeviceTile("zone2color", "zone2", decoration: "flat", width: 3, height:1, childTileName: "color") 
        childDeviceTile("zone2hue", "zone2", decoration: "flat", width: 3, height:1, childTileName: "hue") 
        childDeviceTile("zone3colorMode", "zone3", decoration: "flat", width: 3, height:1, childTileName: "colorMode") 
        childDeviceTile("zone3switch", "zone3", decoration: "flat", width: 3, height:1, childTileName: "switch") 
        childDeviceTile("zone3level", "zone3", decoration: "flat", width: 3, height:1, childTileName: "level") 
        childDeviceTile("zone3colorTemp", "zone3", decoration: "flat", width: 3, height:1, childTileName: "colorTemp")
        childDeviceTile("zone3color", "zone3", decoration: "flat", width: 3, height:1, childTileName: "color") 
        childDeviceTile("zone3hue", "zone3", decoration: "flat", width: 3, height:1, childTileName: "hue")         
    	details(["associations","states","associationsG2", "associationsG3", "associationsG4", "associationsG1", 
        	"zone1", "states", 
            "zone1colorMode", "zone1switch", "zone1level", "zone1colorTemp", "zone1color", "zone1hue", 
        	"zone2", "states", 
            "zone2colorMode", "zone2switch", "zone2level", "zone2colorTemp", "zone2color", "zone2hue",             
        	"zone3", "states", 
            "zone3colorMode", "zone3switch", "zone3level", "zone3colorTemp", "zone3color", "zone3hue",             
            ])
    }
    preferences {
	    input description: "On the 3 scene only models do not use zone 2 or 3", title: "Zones vs Scenes", displayDuringSetup: false, type: "paragraph", element: "paragraph"
        input name: "addHubZone1", type: "bool", description: "This creates a child device on the zone for sending the panel actions to the hub.", title: "Create child driver for Zone 1", required: false, defaultValue: false   
        input name: "addHubZone2", type: "bool", description: "This creates a child device on the zone for sending the panel actions to the hub.", title: "Create child driver for Zone 2", required: false, defaultValue: false
        input name: "addHubZone3", type: "bool", description: "This creates a child device on the zone for sending the panel actions to the hub.", title: "Create child driver for Zone 3", required: false, defaultValue: false
        input name: "associationsZ1", type: "string", description: "To add nodes to zone associations use the Hexidecimal nodeID from the IDE device list separated by commas into the space below", title: "Zone 1 Associations", required: false
        input name: "associationsZ2", type: "string", description: "To add nodes to zone associations use the Hexidecimal nodeID from the IDE device list separated by commas into the space below", title: "Zone 2 Associations", required: false
        input name: "associationsZ3", type: "string", description: "To add nodes to zone associations use the Hexidecimal nodeID from the IDE device list separated by commas into the space below", title: "Zone 3 Associations", required: false
//        input name: "logEnable", type: "bool", description: "", title: "Enable Debug Logging", defaultValue: true, required: true
	}
}

private getDRIVER_VER() { "0.001" }
private getCMD_CLASS_VERS() { [0x33:3,0x26:3,0x85:2,0x8E:2,0x71:8,0x20:1] }
private getZONE_MODEL() {
        return true
}
private getNUMBER_OF_GROUPS() { 
    if (ZONE_MODEL) {
        return 4
	} else {
        return 2
	}
}

def initialize() {
    def cmds=[]
    cmds+=pollAssociations()
    commands(cmds)
}

def logsOff() {
	log.warn "debug logging disabled..."
//	device.updateSetting("logEnable", [value: "false", type: "bool"])
//	if (logEnable) runIn(1800,logsOff)
}

def updated() {
	def children = getChildDevices()
    //log.debug "Children: ${children}"
    children.each { child ->
    	log.debug "child ${child.displayName} has deviceNetworkId ${child.deviceNetworkId}"
	}	
    def cmds=[]
    for (int i = 1 ; i <= 3; i++) {
        if (settings."addHubZone$i") {
            if (!children.any { it -> it.deviceNetworkId == "${device.deviceNetworkId}-$i" } ) {
                def child=addChildDevice("RGBGenie Touch Panel Child ZW", "${device.deviceNetworkId}-$i", null, [completedSetup: true, label: "${device.displayName} (Zone$i)", isComponent: true, componentName: "zone$i", componentLabel: "Zone $i"])
                if (child) {
                    child.defineMe()   
				}        
            }
            addHubMultiChannel(i).each { cmds << it }
//            cmds += addHubMultiChannel(i)
        } else {
            if (children.any { it -> it.deviceNetworkId == "${device.deviceNetworkId}-$i" }) {
                deleteChildDevice("${device.deviceNetworkId}-$i".toString()) 
            }
//            cmds += removeHubMultiChannel(i)
			removeHubMultiChannel(i).each { cmds << it }
	    }
    }
    processAssociations().each { cmds << it }
    pollAssociations().each { cmds << it }
//    cmds+=processAssociations()
//    cmds+=pollAssociations()
    log.debug "update: ${cmds}"
//   	if (logEnable) runIn(1800,logsOff)
    response(commands(cmds))
}


def refresh() {
    def cmds=[]
    cmds+=pollAssociations()
    response(commands(cmds))
    
}

def installed() {
	device.updateSetting("logEnable", [value: "true", type: "bool"])
//	runIn(1800,logsOff)
    initialize()
}



def pollAssociations() {
    def cmds=[]
    for(int i = 1;i<=4;i++) {
        cmds << zwave.associationV2.associationGet(groupingIdentifier:i)
        cmds << zwave.multiChannelAssociationV2.multiChannelAssociationGet(groupingIdentifier: i)
    }
    if (logEnable) log.debug "pollAssociations cmds: ${cmds}"
    return cmds
}

def configure() {
    initialize()
}

def zwaveEvent(physicalgraph.zwave.commands.multichannelv3.MultiChannelCmdEncap cmd) {
    def encapsulatedCommand = cmd.encapsulatedCommand()
    log.debug "Got multichannel encap for endpoint: ${cmd.destinationEndPoint}"
    if (encapsulatedCommand) {
    	def child=null
        def children=getChildDevices()
        children.each { 
        	if (it.deviceNetworkId=="${device.deviceNetworkId}-${cmd.destinationEndPoint}") {
            	child=it
            }
        }
//        def child=getChildDevice("${device.deviceNetworkId}-${cmd.destinationEndPoint}")
        if (child) {
            child.zwaveEvent(encapsulatedCommand)
	    }
    }
}

def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
    def encapsulatedCommand = cmd.encapsulatedCommand(CMD_CLASS_VERS)
    if (encapsulatedCommand) {
        state.sec = 1
        zwaveEvent(encapsulatedCommand)
    }
}

def zwaveEvent(physicalgraph.zwave.commands.multichannelassociationv2.MultiChannelAssociationReport cmd) {
    log.debug "multichannel association report: ${cmd}"
    state."zwaveAssociationMultiG${cmd.groupingIdentifier}"="${cmd.nodeId}"
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
    if (logEnable) log.debug "skip:${cmd}"
}

def parse(String description) {
        def result = null
        def cmd = zwave.parse(description, CMD_CLASS_VERS)
        if (cmd) {
                result = zwaveEvent(cmd)
                log.debug "Parsed ${cmd} to ${result.inspect()}"
        } else {
                log.debug "Non-parsed event: ${description}"
        }
        result
}


private command(physicalgraph.zwave.Command cmd) {
//	if (getDataValue("zwaveSecurePairingComplete") == "true") {
//		return zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
//    } else {
		return cmd.format()
//    }	
}

private commands(commands, delay=200) {
    delayBetween(commands.collect{ command(it) }, delay)
}

def setDefaultAssociation() {
    //def hubitatHubID = (zwaveHubNodeId.toString().format( '%02x', zwaveHubNodeId )).toUpperCase()
    def cmds=[]
    cmds << zwave.associationV2.associationSet(groupingIdentifier: 1, nodeId: zwaveHubNodeId)
//    cmds << zwave.multiChannelAssociationV2.multiChannelAssociationGet(groupingIdentifier: group)
    return cmds
}

def addHubMultiChannel(zone) {
    def cmds=[]
    def group=zone+1
    cmds << zwave.multiChannelAssociationV2.multiChannelAssociationSet(groupingIdentifier: group, nodeId: [0,zwaveHubNodeId,zone as Integer])
//    cmds << zwave.multiChannelAssociationV2.multiChannelAssociationGet(groupingIdentifier: group)
    return cmds
}

def removeHubMultiChannel(zone) {
    def cmds=[]
    def group=zone+1
    cmds << zwave.multiChannelAssociationV2.multiChannelAssociationRemove(groupingIdentifier: group, nodeId: [0,zwaveHubNodeId,zone as Integer])
//    cmds << zwave.multiChannelAssociationV2.multiChannelAssociationGet(groupingIdentifier: group)
    return cmds
}

def processAssociations(){
    def cmds = []
    cmds += setDefaultAssociation()
        def associationGroups = NUMBER_OF_GROUPS
        for (int i = 2 ; i <= associationGroups; i++) {
            def z=i-1
            if (logEnable) log.debug "group: $i dataValue: " + getDataValue("zwaveAssociationG$i") + " parameterValue: " + settings."associationsZ$z"
            def parameterInput=settings."associationsZ$z"
            def newNodeList = []
            def oldNodeList = []
            if (state."zwaveAssociationsG$i" != null) {
                state."zwaveAssociationsG$i".minus("[").minus("]").split(",").each {
                    if (it!="") {
                        oldNodeList.add(it.minus(" "))
                    }
			    }
            }
            if (parameterInput!=null) {
                parameterInput.minus("[").minus("]").split(",").each {
                    if (it!="") {
                        newNodeList.add(it.minus(" "))
                    }
				}
            }
            if (oldNodeList.size > 0 || newNodeList.size > 0) {
                if (logEnable) log.debug "${oldNodeList.size} - ${newNodeList.size}"
                oldNodeList.each {
                    if (!newNodeList.contains(it)) {
                        // user removed a node from the list
                        if (logEnable) log.debug "removing node: $it, from group: $i"
                        cmds << zwave.associationV2.associationRemove(groupingIdentifier:i, nodeId:Integer.parseInt(it,16))
					}        
				}
                newNodeList.each {
                    cmds << zwave.associationV2.associationSet(groupingIdentifier:i, nodeId:Integer.parseInt(it,16))   
				}                            
			}
       }
    if (logEnable) log.debug "processAssociations cmds: ${cmds}"
    return cmds
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationReport cmd) {
    if (logEnable) log.debug "${device.label?device.label:device.name}: ${cmd}"
    def temp = []
    if (cmd.nodeId != []) {
       cmd.nodeId.each {
          temp += it.toString().format( '%02x', it.toInteger() ).toUpperCase()
       }
    }
    def zone=cmd.groupingIdentifier-1
    log.debug "${cmd.groupingIdentifier} - $zone - $temp"
    if (zone > 0) {
        //device.updateSetting("associationsZ$zone",[value: "${temp.toString().minus("[").minus("]")}", type: "string"])
    }
    def group=cmd.groupingIdentifier
    sendEvent(name: "associationsG$group", value: "$temp")
    state."associationsG$group"="$temp"
    log.debug "Sending Event (name: associationsG$group, value: $temp)" 
	log.debug "associationsG$group: ${state.assocationsG$group}"
}

def zwaveEvent(physicalgraph.zwave.commands.associationv2.AssociationGroupingsReport cmd) {
    if (logEnable) log.debug "${device.label?device.label:device.name}: ${cmd}"
    sendEvent(name: "groups", value: cmd.supportedGroupings)
    log.info "${device.label?device.label:device.name}: Supported association groups: ${cmd.supportedGroupings}"
    state.associationGroups = cmd.supportedGroupings
}