metadata {
	definition(name: "RGBGneie Z-Wave Micro Dimmer", namespace: "rgbgenie", author: "RGBGenie", ocfDeviceType: "oic.d.switch", runLocally: true, minHubCoreVersion: '000.019.00012', executeCommandsLocally: false, genericHandler: "Z-Wave") {
		capability "Actuator"
		capability "Health Check"
		capability "Switch"
		capability "Switch Level"

		fingerprint mfr: "0330", prod: "0201", model: "D005", deviceJoinName: "RGBGenie Micro Controller"
	}

	simulator {

	}

	tiles(scale: 2) {
		multiAttributeTile(name: "switch", type: "lighting", width: 6, height: 4, canChangeIcon: true) {
			tileAttribute("device.switch", key: "PRIMARY_CONTROL") {
				attributeState "on", label: '${name}', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#00a0dc", nextState: "turningOff"
				attributeState "off", label: '${name}', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff", nextState: "turningOn"
				attributeState "turningOn", label: '${name}', action: "switch.off", icon: "st.switches.switch.on", backgroundColor: "#00a0dc", nextState: "turningOff"
				attributeState "turningOff", label: '${name}', action: "switch.on", icon: "st.switches.switch.off", backgroundColor: "#ffffff", nextState: "turningOn"
			}
			tileAttribute("device.level", key: "SLIDER_CONTROL") {
				attributeState "level", action: "switch level.setLevel"
			}
		}
    	preferences {
			input name: "parameter2", type: "enum", description: "", title: "Power fail load state restore", defaultValue: 0, required: true, options: [0: "Shut Off Load", 1: "Turn On Load", 2: "Restore Last State"]
        	input name: "parameter4", type: "number", description: "", title: "Default Fade Time 0-254", defaultValue: 1, required: true
        	input name: "parameter5", type: "number", description: "", title: "Minimum Level", defaultValue: 0, required: true
        	input name: "parameter6", type: "enum", description: "", title: "MOSFET Driving Type", defaultValue: 0, required: true, options: [0: "Trailing Edge", 1: "Leading Edge"]
    	}
		valueTile("level", "device.level", inactiveLabel: false, decoration: "flat", width: 2, height: 2) {
			state "level", label: '${currentValue} %', unit: "%", backgroundColor: "#ffffff"
		}

		main(["switch"])
		details(["switch", "level"])

	}
}

def installed() {
// Device-Watch simply pings if no device events received for 32min(checkInterval)
	sendEvent(name: "checkInterval", value: 2 * 15 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])
	def commands = refresh()

	response(commands)
}

def updated() {
// Device-Watch simply pings if no device events received for 32min(checkInterval)
	sendEvent(name: "checkInterval", value: 2 * 15 * 60 + 2 * 60, displayed: false, data: [protocol: "zwave", hubHardwareId: device.hub.hardwareID, offlinePingable: "1"])
	def cmds = []
	if (parameter2) cmds << zwave.configurationV2.configurationSet([parameterNumber: 2, size: 1, scaledConfigurationValue: parameter2]).format()
    if (parameter4) cmds << zwave.configurationV2.configurationSet([parameterNumber: 4, size: 1, scaledConfigurationValue: parameter2]).format()
    if (parameter5) cmds << zwave.configurationV2.configurationSet([parameterNumber: 5, size: 1, scaledConfigurationValue: parameter2]).format()
    if (parameter6) cmds << zwave.configurationV2.configurationSet([parameterNumber: 6, size: 1, scaledConfigurationValue: parameter2]).format()
    log.debug "udpate commands: ${cmds}" 
	delayBetween(cmds,500)
    
}

def getCommandClassVersions() { [0x20:1,0x26:3,0x85:2,0x71:8] }

def parse(String description) {
	def result = null
	if (description != "updated") {
		log.debug "parse() >> zwave.parse($description)"
		def cmd = zwave.parse(description, commandClassVersions)
		if (cmd) {
			result = zwaveEvent(cmd)
		}
	}
	if (result?.name == 'hail' && hubFirmwareLessThan("000.011.00602")) {
		result = [result, response(zwave.basicV1.basicGet())]
		log.debug "Was hailed: requesting state update"
	} else {
		log.debug "Parse returned ${result?.descriptionText}"
	}
	return result
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd) {
	dimmerEvents(cmd)
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd) {
	dimmerEvents(cmd)
}

def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv3.SwitchMultilevelReport cmd) {
	dimmerEvents(cmd)
}

def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv3.SwitchMultilevelSet cmd) {
	dimmerEvents(cmd)
}

private dimmerEvents(physicalgraph.zwave.Command cmd) {
	def value = (cmd.value ? "on" : "off")
	def result = [createEvent(name: "switch", value: value)]
	if (cmd.value && cmd.value <= 100) {
		result << createEvent(name: "level", value: cmd.value == 99 ? 100 : cmd.value)
	}
	return result
}

def zwaveEvent(physicalgraph.zwave.commands.hailv1.Hail cmd) {
	createEvent([name: "hail", value: "hail", descriptionText: "Switch button was pressed", displayed: false])
}

def zwaveEvent(physicalgraph.zwave.commands.manufacturerspecificv2.ManufacturerSpecificReport cmd) {
	log.debug "manufacturerId:   $cmd.manufacturerId"
	log.debug "manufacturerName: $cmd.manufacturerName"
	log.debug "productId:        $cmd.productId"
	log.debug "productTypeId:    $cmd.productTypeId"
	def msr = String.format("%04X-%04X-%04X", cmd.manufacturerId, cmd.productTypeId, cmd.productId)
	updateDataValue("MSR", msr)
	updateDataValue("manufacturer", cmd.manufacturerName)
	createEvent([descriptionText: "$device.displayName MSR: $msr", isStateChange: false])
}

def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv1.SwitchMultilevelStopLevelChange cmd) {
	[createEvent(name: "switch", value: "on"), response(zwave.switchMultilevelV3.switchMultilevelGet().format())]
}

def zwaveEvent(physicalgraph.zwave.commands.crc16encapv1.Crc16Encap cmd) {
	def versions = commandClassVersions
	def version = versions[cmd.commandClass as Integer]
	def ccObj = version ? zwave.commandClass(cmd.commandClass, version) : zwave.commandClass(cmd.commandClass)
	def encapsulatedCommand = ccObj?.command(cmd.command)?.parse(cmd.data)
	if (encapsulatedCommand) {
		zwaveEvent(encapsulatedCommand)
	}
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
	// Handles all Z-Wave commands we aren't interested in
	[:]
}

def on() {
	delayBetween([
		zwave.basicV1.basicSet(value: 0xFF).format(),
		zwave.switchMultilevelV3.switchMultilevelGet().format()
	], 5000)
}

def off() {
	delayBetween([
		zwave.basicV1.basicSet(value: 0x00).format(),
		zwave.switchMultilevelV3.switchMultilevelGet().format()
	], 5000)
}

def setLevel(value) {
	log.debug "setLevel >> value: $value"
	def valueaux = value as Integer
	def level = Math.max(Math.min(valueaux, 99), 0)
	if (level > 0) {
		sendEvent(name: "switch", value: "on")
	} else {
		sendEvent(name: "switch", value: "off")
	}
	delayBetween([zwave.basicV1.basicSet(value: level).format(), zwave.switchMultilevelV3.switchMultilevelGet().format()], 5000)
}

def setLevel(value, duration) {
	log.debug "setLevel >> value: $value, duration: $duration"
	def valueaux = value as Integer
	def level = Math.max(Math.min(valueaux, 99), 0)
	def dimmingDuration = duration < 128 ? duration : 128 + Math.round(duration / 60)
	def getStatusDelay = duration < 128 ? (duration * 1000) + 2000 : (Math.round(duration / 60) * 60 * 1000) + 2000
	delayBetween([zwave.switchMultilevelV3.switchMultilevelSet(value: level, dimmingDuration: dimmingDuration).format(),
				  zwave.switchMultilevelV3.switchMultilevelGet().format()], getStatusDelay)
}

def poll() {
	refresh()
}

/**
 * PING is used by Device-Watch in attempt to reach the Device
 * */
def ping() {
	refresh()
}

def refresh() {
	log.debug "refresh() is called"
	def commands = []
	commands << zwave.switchMultilevelV1.switchMultilevelGet().format()
	if (getDataValue("MSR") == null) {
		commands << zwave.manufacturerSpecificV1.manufacturerSpecificGet().format()
	}
	delayBetween(commands, 100)
}

