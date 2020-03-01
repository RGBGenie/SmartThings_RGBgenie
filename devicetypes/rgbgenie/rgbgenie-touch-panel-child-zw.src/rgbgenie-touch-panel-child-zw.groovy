/*
*	Touch Panel Child Driver
*	Code written for RGBGenie by Bryan Copeland
*
*   Updated 2020-02-26 Added importUrl
*
*/

metadata {
	definition (name: "RGBGenie Touch Panel Child ZW", namespace: "rgbgenie", author: "RGBGenie") {
		capability "SwitchLevel"
		capability "Button"
		capability "Color Control"
		capability "Color Mode"
		capability "Color Temperature"
		capability "Switch"
		capability "Actuator"
		attribute "colorMode", "string"
	}

	preferences {
		input name: "logEnable", type: "bool", description: "", title: "Enable Debug Logging", defaultValue: true, required: true
		input name: "sceneCapture", type: "bool", description: "", title: "Enable scene capture and activate", defaultValue: false, required: true
	}

}
private getCOLOR_TEMP_MIN() { 2700 }
private getCOLOR_TEMP_MAX() { 6500 }
private getCOLOR_TEMP_DIFF() { COLOR_TEMP_MAX - COLOR_TEMP_MIN }

def updated() {
	if (sceneCapture && getDataValue("deviceModel")=="41221") { 
		sendEvent(name: "numberOfButtons", value: 0) 
	} else if (!sceneCapture && getDataValue("deviceModel")!="41221") {
		sendEvent(name: "numberOfButtons", value: 3)
	}
	if (logEnable) runIn(1800,logsOff)
}

def installed() {
	device.updateSetting("logEnable", [value: "true", type: "bool"])
	runIn(1800,logsOff)
}

def logsOff() {
	log.warn "debug logging disabled..."
	device.updateSetting("logEnable", [value: "false", type: "bool"])
	if (logEnable) runIn(1800,logsOff)
}

def defineMe() {
	//device.updateDataValue("deviceModel", "$value")
	sendEvent(name: "numberOfButtons", value: 3)
	sendEvent(name: "colorMode", value: "CT")
	sendEvent(name: "colorTemperature", value: 2700)
	sendEvent(name: "hue", value: 0)
	sendEvent(name: "saturation", value: 100)
	sendEvent(name: "level", value: 100)
	sendEvent(name: "switch", value: "on")
}

def parse(description) {
	if (logEnable) log.debug "description"
//	zwaveEvent(cmd)
}

def zwaveEvent(physicalgraph.zwave.Command cmd) {
    if (logEnable) log.debug "skip:${cmd}"
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicReport cmd) {
	dimmerEvents(cmd)
}

def zwaveEvent(physicalgraph.zwave.commands.basicv1.BasicSet cmd) {
	if (logEnable) log.debug "basic set: ${cmd}"
	dimmerEvents(cmd)
}

def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv3.SwitchMultilevelReport cmd) {
	dimmerEvents(cmd)
}




def zwaveEvent(physicalgraph.zwave.commands.switchcolorv3.SwitchColorSet cmd) {
	if (logEnable) log.debug "got SwitchColorReport: $cmd"
	def colorComponents=cmd.colorComponents
	def warmWhite=null
	def coldWhite=null
	def red=0
	def green=0
	def blue=0
    colorComponents.each { k, v ->
        if (logEnable) log.debug "color component: $k : $v"
		switch (k) {
			case 0:
				warmWhite=v
			break
			case 1:
				coldWhite=v
			break
			case 2:
				red=v
			break
			case 3:
				green=v
			break
			case 4:
				blue=v
			break
		}
    }
	if (red > 0 || green > 0 || blue > 0) {
		def hsv=rgbToHSV([red, green, blue])
		def hue=hsv["hue"]
		def sat=hsv["saturation"]
		def lvl=hsv["value"]
        def hexColor=colorUtil.rgbToHex(red as int, green as int, blue as int)
       	if (state.color != hexColor) {
        	sendEvent(name:"color", value: hexColor)
        }
		if (hue != state.hue) {
			sendEvent(name:"hue", value:Math.round(hue), unit:"%")
		}
		if (sat != state.saturation) { 
			sendEvent(name:"saturation", value:Math.round(sat), unit:"%")
		}
		sendEvent(name:"colorMode", value:"RGB")
	} else if (warmWhite != null && coldWhite != null) {
		sendEvent(name:"colorMode", value:"CT")
		def colorTemp = COLOR_TEMP_MIN + (COLOR_TEMP_DIFF / 2)
		if (warmWhite != coldWhite) {
			colorTemp = (COLOR_TEMP_MAX - (COLOR_TEMP_DIFF * warmWhite) / 255) as Integer
		}
		sendEvent(name:"colorTemperature", value: colorTemp)	
	} else if (warmWhite != null) {
		sendEvent(name:"colorMode", value:"CT")
		sendEvent(name:"colorTemperature", value: 2700)
	}
}

def levelChanging(options){
	def level=0
	if (options.upDown) {
		level=options.level-5
	} else {
		level=options.level+5
	}
	if (level>100) level=100
	if (level<0) level=0 

	sendEvent(name: "level", value: level == 99 ? 100 : level , unit: "%")
	if (level>0 && level<100) {
		if (device.currentValue("switch")=="off") sendEvent(name: "switch", value: "on")
		runInMillis(500, "levelChanging", [data: [upDown: options.upDown, level: level]])
	} else if (level==0) {
		if (device.currentValue("switch")=="on") sendEvent(name: "switch", value: "off")
	}
}

def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv3.SwitchMultilevelStartLevelChange cmd){
	runInMillis(500, "levelChanging", [data: [upDown: cmd.upDown, level: device.currentValue("level")]])
}

def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv3.SwitchMultilevelStopLevelChange cmd) {
	unschedule()
}

def zwaveEvent(physicalgraph.zwave.commands.switchmultilevelv3.SwitchMultilevelSet cmd) {
	sendEvent(name: "level", value: cmd.value)
}

private dimmerEvents(physicalgraph.zwave.Command cmd) {
	def value = (cmd.value ? "on" : "off")
	sendEvent(name: "switch", value: value, descriptionText: "$device.displayName was turned $value")
	if (cmd.value) {
		if (cmd.value>100) cmd.value=100
		sendEvent(name: "level", value: cmd.value == 99 ? 100 : cmd.value , unit: "%")
	}
}

def zwaveEvent(physicalgraph.zwave.commands.sceneactuatorconfv1.SceneActuatorConfSet cmd) {
	if (sceneCapture) {
		if (!state.scene) { state.scene=[:] }
		if(device.currentValue("colorMode")=="RGB") {
			state.scene["${cmd.sceneId}"]=["hue": device.currentValue("hue"), "saturation": device.currentValue("saturation"), "level": device.currentValue("level"), "colorMode": device.currentValue("colorMode"), "switch": device.currentValue("switch")]
		} else {
			state.scene["${cmd.sceneId}"]=["colorTemperature": device.currentValue("colorTemperature"), "level": device.currentValue("level"), "switch": device.currentValue("switch"), "colorMode": device.currentValue("colorMode")]
		}
	} else {
		sendEvent(name: "pushed", value: (cmd.sceneId/16))
	}
}

def zwaveEvent(physicalgraph.zwave.commands.sceneactivationv1.SceneActivationSet cmd) {
	if (sceneCapture) {
		if (!state.scene) { state.scene=[:] }
		def scene=state.scene["${cmd.sceneId}"] 
		scene.each { k, v ->
			sendEvent(name: k, value: v)
		}
	} else {
		sendEvent(name: "held", value: (cmd.sceneId/16))
	}
}

def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
	def encapsulatedCommand = cmd.encapsulatedCommand(cmdClassVers)
	if (encapsulatedCommand) {
		state.sec = 1
		def result = zwaveEvent(encapsulatedCommand)
		result = result.collect {
			if (it instanceof physicalgraph.device.HubAction && !it.toString().startsWith("9881")) {
				response(cmd.CMD + "00" + it.toString())
			} else {
				it
			}
		}
		result
	}
}



def zwaveEvent(physicalgraph.zwave.commands.switchcolorv3.SwitchColorSupportedReport cmd) {
	if (logEnable) log.debug cmd
}

def buildOffOnEvent(cmd){
	[zwave.basicV1.basicSet(value: cmd), zwave.switchMultilevelV3.switchMultilevelGet()]
}

def on() {
	commands(buildOffOnEvent(0xFF), 3500)
}

def off() {
	commands(buildOffOnEvent(0x00), 3500)
}

def setLevel(level) {
	setLevel(level, 1)
}

def setLevel(level, duration) {

}

def setSaturation(percent) {

}

def setHue(value) {

}

def setColor(value) {

}

def setColorTemperature(temp) {

}

private secEncap(physicalgraph.zwave.Command cmd) {
	zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
}

private crcEncap(physicalgraph.zwave.Command cmd) {
	zwave.crc16EncapV1.crc16Encap().encapsulate(cmd).format()
}

private command(physicalgraph.zwave.Command cmd) {
//	if (zwaveInfo.zw.contains("s") || state.sec == 1) {
//		secEncap(cmd)
//	} else if (zwaveInfo.cc.contains("56")){
//		crcEncap(cmd)
//	} else {
		cmd.format()
//	}
}

private commands(commands, delay=200) {
	delayBetween(commands.collect{ command(it) }, delay)
}

def rgbToHSV(red, green, blue) {
	def hex = colorUtil.rgbToHex(red as int, green as int, blue as int)
	def hsv = colorUtil.hexToHsv(hex)
	return [hue: hsv[0], saturation: hsv[1], value: hsv[2]]
}