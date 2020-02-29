/**
 *  RGBGenie LED Controller
 *
 *  Copyright 2020 Bryan Copeland
 *
 *  Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 *  in compliance with the License. You may obtain a copy of the License at:
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software distributed under the License is distributed
 *  on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License
 *  for the specific language governing permissions and limitations under the License.
 */
metadata {
	definition (name: "RGBGenie LED Controller", namespace: "rgbgenie", author: "Bryan Copeland", cstHandler: true) {
		capability "Switch"
		capability "Switch Level"
		capability "Color Control"
		capability "Color Mode"
		capability "Color Temperature"
		command "testRed"
        command "testGreen"
        command "testBlue"
        command "testWW"
		command "testCW"
        command "setEffect", ["number"]
        command "setNextEffect"
        command "setPreviousEffect"
        
        attribute "deviceModel", "string"
        attribute "effectName", "string"
        attribute "colorMode", "string"
		attribute "lightEffects", "JSON_OBJECT"        

		fingerprint mfr: "0330", prod: "0200", model: "D002", deviceJoinName: "RGBGenie LED Controller"
        fingerprint mfr: "0330", prod: "0201", model: "D002", deviceJoinName: "RGBGenie LED Controller"
        fingerprint mfr: "0330", prod: "0202", model: "D002", deviceJoinName: "RGBGenie LED Controller"
        fingerprint mfr: "0330", prod: "021A", model: "D002", deviceJoinName: "RGBGenie LED Controller"
	}
	preferences {
		input name: "deviceType", type: "enum", description: "", title: "Set Device Type", displayDuringSetup: true, required: true, defaultValue: 3, options: [0: "Single Color", 1: "CCT", 2: "RGBW"]
		input name: "dimmerSpeed", type: "number", description: "", title: "Dimmer Ramp Rate 0-255", defaultValue: 0, required: true, displayDuringSetup: true
		input name: "loadStateSave", type: "enum", description: "", title: "Power fail load state restore", defaultValue: 0, required: true, displayDuringSetup: true, options: [0: "Shut Off Load", 1: "Turn On Load", 2: "Restore Last State"]
		input name: "stageModeSpeed", type: "number", description: "", title: "Light Effect Speed 0-255 (default 243)", defaultValue: 243, displayDuringSetup: true, required: true
		input name: "stageModeHue", type: "number", description: "", title: "Hue Of Fixed Color Light Effects 0-360", defaultValue: 0, displayDuringSetup:true, required: true
		input name: "logLevel", type: "enum", title: "Logging Level", options: [1: "Error", 2: "Warn", 3: "Info", 4: "Debug", 5: "Trace"], required: false, displayDuringSetup: false, defaultValue: 4
		if (deviceType=="" || deviceType==null) {
			input description: "The device type has not been detected.. Please press the configure button", title: "Device Type Detection", displayDuringSetup: false, type: "paragraph", element: "paragraph"
		} else {
			if (deviceType == 1 || deviceType== 2) {
				// Color, or Color Temperature Model-specific settings
				input name: "colorPrestage", type: "bool", description: "", title: "Enable Color Prestaging", defaultValue: false, required: true
				input name: "colorDuration", type: "number", description: "", title: "Color Transition Duration", defaultValue: 3, required: true			
			}
			if (deviceType==2) {
				// Color Model-specific settings
				input name: "wwComponent", type: "bool", description: "", title: "Enable Warm White Component", defaultValue: true, required: true
				input name: "wwKelvin", type: "number", description: "", title: "Warm White Temperature", defaultValue: 2700, required: true
				input name: "hueMode", type: "bool", description: "", title: "Send hue in 0-100 (off) or 0-360 (on)", defaultValue: false, required: true
				input name: "enableGammaCorrect", type: "bool", description: "May cause a slight difference in reported color", title: "Enable gamma correction on setColor", defaultValue: false, required: true
			}

		}
	}

	simulator {
		// TODO: define status and reply messages here
	}

	tiles {
		// TODO: define your main and details tiles here
	}
}

private getRGBW_NAMES() { [RED, GREEN, BLUE, WARM_WHITE] }
private getRGB_NAMES() { [RED, GREEN, BLUE] }
private getCCT_NAMES() { [WARM_WHITE, COLD_WHITE] }
private getRED() { "red" }
private getGREEN() { "green" }
private getBLUE() { "blue" }
private getWARM_WHITE() { "warmWhite" }
private getCOLD_WHITE() { "coldWhite" }
private getCOLOR_TEMP_MIN() { 2700 }
private getCOLOR_TEMP_MAX() { 6500 }
private getCOLOR_TEMP_DIFF_RGBW() { COLOR_TEMP_MAX - wwKelvin }
private getCOLOR_TEMP_DIFF() { COLOR_TEMP_MAX - COLOR_TEMP_MIN }
private getCMD_CLASS_VERS() { [0x33:3,0x26:3,0x85:2,0x71:8,0x20:1] }
private getZWAVE_COLOR_COMPONENT_ID() { [warmWhite: 0, coldWhite: 1, red: 2, green: 3, blue: 4] }


// parse events into attributes
def parse(description) {
	if (description != "updated") {
		def cmd = zwave.parse(description, CMD_CLASS_VERS)
		if (cmd) {
			result = zwaveEvent(cmd)
			logDebug("${description} parsed to $result")
		} else {
			logWarn("unable to parse: ${description}")
		}
	}
}

def testRed() {
	def value=255
    commands ([zwave.switchColorV3.switchColorSet(red: value, green: 0, blue: 0, warmWhite:0, coldWhite:0)])
}

def testGreen(){
	def value=255
    commands ([zwave.switchColorV3.switchColorSet(red: 0, green: value, blue: 0, warmWhite:0, coldWhite:0)])
}

def testBlue(){
	def value=255
	commands ([zwave.switchColorV3.switchColorSet(red: 0, green: 0, blue: value, warmWhite:0, coldWhite:0)])
}

def testWW(){
	def value=255
    commands ([zwave.switchColorV3.switchColorSet(red: 0, green: 0, blue: 0, warmWhite: value, coldWhite:0)])
}

def testCW(){
	def value=255
    commands ([zwave.switchColorV3.switchColorSet(red: 0, green: 0, blue: 0, warmWhite: 0, coldWhite:255)])
}

def configure() {
	initializeVars()
	interrogate()
}

def logsOff(){
    logWarn "${device.label?device.label:device.name}: Disabling logging after timeout"
    device.updateSetting("logLevel",[value:"1",type:"number"])
}

def interrogate() {
	logDebug "Querying for device type"
	def cmds = []
	cmds << zwave.configurationV2.configurationGet([parameterNumber: 4])
	cmds << zwave.associationV2.associationGet(groupingIdentifier:1)
	commands(cmds)
}

def updated() {
	logDebug "updated().."
	log.info "Logging level is ${logLevel}"
	def cmds = [] 
    logDebug "deviceModel: "+currentValue("deviceModel") + " Updated setting: ${deviceType}"
	cmds << zwave.configurationV2.configurationSet([parameterNumber: 4, size: 1, scaledConfigurationValue: deviceType.toInteger()])
	cmds << zwave.configurationV2.configurationGet([parameterNumber: 4])
	cmds << zwave.configurationV2.configurationSet([parameterNumber: 2, size: 1, scaledConfigurationValue: loadStateSave])
	cmds << zwave.configurationV2.configurationSet([parameterNumber: 6, size: 1, configurationValue: [stageModeSpeed as Integer]])
	cmds << zwave.configurationV2.configurationSet([parameterNumber: 8, size: 1, scaledConfigurationValue: hueToHueByte(stageModeHue)])
    logDebug "commands: ${cmds}"
	commands(cmds)
}

private hueToHueByte(hueValue) {
	// hue as 0-360 return hue as 0-255
	return Math.round(hueValue / (360/255))
}

private initializeVars() {
	state.colorReceived = ["red": null, "green": null, "blue": null, "warmWhite": null, "coldWhite": null]
	state.lightEffects = [
		"0":"None",
		"1":"Fade in/out mode, fixed color", 
		"2":"Flash mode fixed color",
		"3":"Rainbow Mode, fixed change effect",
		"4":"Fade in/out mode, color changes randomly",
		"5":"Flash Mode, color changes randomly",
		"6":"Rainbow Mode, color changes randomly",
		"7":"Random Mode"
	]
}

def installed() {
	log.info "installed()..."
	def cmds = []
    cmds << zwave.associationV2.associationGet(groupingIdentifier:1)
    cmds << zwave.configurationV2.configurationGet([parameterNumber: 4])
    commands(cmds)
}

def zwaveEvent(physicalgraph.zwave.commands.configurationv2.ConfigurationReport cmd) {
	log.debug "got ConfigurationReport: $cmd"
	switch (cmd.parameterNumber) {
		case 4:
        	if (cmd.scaledConfigurationValue!=deviceType) {
				device.updateSetting("deviceType", [value:cmd.scaledConfigurationValue, type: "number"])
            }
		break
		case 5:
			def effectName = "None"
			switch (cmd.scaledConfigurationValue) {
				case 0:
					effectName="None"
				break
				case 1:
					effectName="Fade in/out mode, fixed color"
				break
				case 2:
					effectName="Flash mode fixed color"
				break
				case 3:
					effectName="Rainbow Mode, fixed change effect"
				break
				case 4:
					effectName="Fade in/out mode, color changes randomly"
				break
				case 5:
					effectName="Flash Mode, color changes randomly"
				break
				case 6:
					effectName="Rainbow Mode, color changes randomly"
				break
				case 7:
					effectName="Random Mode"
				break
			}
			if (device.currentValue("effectName")!=effectName) sendEvent(name: "effectName", value: effectName)
			state.effectNumber=cmd.scaledConfigurationValue
		break
	}
}

def setEffect(effectNumber) {
	log.debug "Got setEffect " + effectNumber
	def cmds=[]
	cmds << zwave.configurationV2.configurationSet([parameterNumber: 5, size: 1, scaledConfigurationValue: effectNumber])
	cmds << zwave.configurationV2.configurationGet([parameterNumber: 5])
	if (device.currentValue("switch") != "on") {
		cmds << zwave.basicV1.basicSet(value: 0xFF)
		cmds << zwave.switchMultilevelV3.switchMultilevelGet()
	}
	commands(cmds)
}

def setNextEffect() {
	if (state.effectNumber < 7) device.setEffect(state.effectNumber+1)
}

def setPreviousEffect() {
	if (state.effectNumber > 0) device.setEffect(state.effectNumber-1)
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

def zwaveEvent(physicalgraph.zwave.commands.switchcolorv3.SwitchColorReport cmd) {
	logDebug "got SwitchColorReport: $cmd"
	if (!state.colorReceived) state.colorReceived = ["red": null, "green": null, "blue": null, "warmWhite": null, "coldWhite": null]
	state.colorReceived[cmd.colorComponent] = cmd.targetValue
	switch (deviceType) {
		case 1:
			// CCT Device Type
			if (CCT_NAMES.every { state.colorReceived[it] != null }) {
				// Got all CCT colors
				def warmWhite = state.colorReceived["warmWhite"]
				def coldWhite = state.colorReceived["coldWhite"]
				def colorTemp = COLOR_TEMP_MIN + (COLOR_TEMP_DIFF / 2)
				if (warmWhite != coldWhite) {
					colorTemp = (COLOR_TEMP_MAX - (COLOR_TEMP_DIFF * warmWhite) / 255) as Integer
				}
				sendEvent(name: "colorTemperature", value: colorTemp)
				// clear state values
				CCT_NAMES.collect { state.colorReceived[it] = null }
			}
		break
		case 2:
			// RGBW Device Type
			if (RGBW_NAMES.every { state.colorReceived[it] != null }) {
				if (device.currentValue("colorMode") == "RGB") {
					def hsv=rgbToHSV([state.colorReceived["red"], state.colorReceived["green"], state.colorReceived["blue"]])
					def hue=hsv[0]
					def sat=hsv[1]
                    def lvl=hsv[2]
                    def colors = RGB_NAMES.collect { state.colorReceived[it] }
                    def hexColor = "#" + colors.collect { Integer.toHexString(it).padLeft(2, "0") }.join("")
					sendEvent(name: "color", value: hexColor)
					sendEvent(name:"hue", value:Math.round(hue), unit:"%")
					setGenericName(hue)
					sendEvent(name:"saturation", value:Math.round(sat), unit:"%")
					//sendEvent(name:"level", value:Math.round(lvl), unit:"%")
				} else { 
					if (wwComponent) {
						def colorTemp = COLOR_TEMP_MIN + (COLOR_TEMP_DIFF_RGBW / 2)
						def warmWhite = state.colorReceived["warmWhite"]
						def coldWhite = state.colorReceived["red"]
						if (warmWhite != coldWhite) colorTemp = (COLOR_TEMP_MAX - (COLOR_TEMP_DIFF_RGBW * warmWhite) / 255) as Integer
						sendEvent(name: "colorTemperature", value: colorTemp)
					} else {
						// Math is hard
						sendEvent(name: "colorTemperature", value: state.ctTarget)
						//sendEvent(name: "colorTemperature", value: rgbToCt(state.colorReceived['red'] as Float, state.colorReceived['green'] as Float, state.colorReceived['blue'] as Float))
					}

				}
				// clear state values
				RGBW_NAMES.collect { state.colorReceived[it] = null }
			}
		break
	}
}

private dimmerEvents(physicalgraph.zwave.Command cmd) {
	def value = (cmd.value ? "on" : "off")
	sendEvent(name: "switch", value: value, descriptionText: "$device.displayName was turned $value")
	if (cmd.value) {
		sendEvent(name: "level", value: cmd.value == 99 ? 100 : cmd.value , unit: "%")
	}
}

def zwaveEvent(physicalgraph.zwave.commands.hailv1.Hail cmd) {
	response(command(zwave.switchMultilevelV3.switchMultilevelGet()))
}

def zwaveEvent(physicalgraph.zwave.commands.securityv1.SecurityMessageEncapsulation cmd) {
	def encapsulatedCommand = cmd.encapsulatedCommand(CMD_CLASS_VERS)
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

def zwaveEvent(physicalgraph.zwave.Command cmd) {
    log.debug "skip:${cmd}"
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

def refresh() {
	// Queries a device for changes 
	def cmds=[]
	cmds << zwave.switchMultilevelV3.switchMultilevelGet()
	cmds << zwave.configurationV2.configurationGet([parameterNumber: 5])
	commands(cmds + queryAllColors())
}

def ping() {
	log.debug "ping().."
	refresh()
}

def setLevel(level, duration = settings.dimmerSpeed) {
	log.debug "setLevel($level, $duration)"
	level = clamp(level, 0, 99)
	commands([
		zwave.switchMultilevelV3.switchMultilevelSet(value: level, dimmingDuration: duration)
	])
}

def setSaturation(percent) {
	percent = clamp(percent)
	log.debug "setSaturation($percent)"
	setColor(saturation: percent)
}

def setHue(value) {
	value = clamp(value, 0, 360)
	log.debug "setHue($value)"
	setColor(hue: value)
}

def setColor(value) {
	// Sets the color of a device from HSL

	state.colorReceived = ["red": null, "green": null, "blue": null, "warmWhite": null, "coldWhite": null]
	def setValue = [:]
	def duration=colorDuration?colorDuration:3
	def rgb=[]
	log.debug "setColor($value)"

	if (state.deviceType==2) {
    	if (value.hex) {
			rgb = value.hex.findAll(/[0-9a-fA-F]{2}/).collect { Integer.parseInt(it, 16) }
		} else {
			setValue.hue = value.hue == null ? device.currentValue("hue") : clamp((settings.hueMode == true ? value.hue * 3.6 : value.hue), 0, 360)
			setValue.saturation = value.saturation == null ? device.currentValue("saturation") : clamp(value.saturation)
			setValue.level = value.level == null ? device.currentValue("level") : clamp(value.level)
			log.debug "setColor updated values to $setValue."
			rgb = huesatToRGB(setValue.hue, setValue.saturation)
		}
		// Device HSL values get updated with parse()
		
		def result = []
		
		log.debug "HSL Converted to R:${rgb[0]} G:${rgb[1]} B:${rgb[2]}"
		if (enableGammaCorrect) {
			result << zwave.switchColorV3.switchColorSet(red: gammaCorrect(rgb[0]), green: gammaCorrect(rgb[1]), blue: gammaCorrect(rgb[2]), warmWhite:0, dimmingDuration: duration)
		} else {
			result << zwave.switchColorV3.switchColorSet(red: rgb[0], green: rgb[1], blue: rgb[2], warmWhite:0, dimmingDuration: duration)
		}
		if ((device.currentValue("switch") != "on") && !colorPrestage) {
			log.debug "Turning device on with pre-staging"
 			result << zwave.basicV1.basicSet(value: 0xFF)
			result << zwave.switchMultilevelV3.switchMultilevelGet()
		}

		result+=queryAllColors()

		log.debug "commands: ${result}"

		sendEvent(name: "colorMode", value: "RGB")
		commands(result)
	} else {
		log.trace "setColor not supported on this device type"
	}
}


def setColorTemperature(temp) {
	// Sets the colorTemperature of a device
	temp = clamp(temp, 1000, 12000)
	state.colorReceived = ["red": null, "green": null, "blue": null, "warmWhite": null, "coldWhite": null]
	def duration=colorDuration?colorDuration:3
	def warmWhite=0
	def coldWhite=0
	if(temp > COLOR_TEMP_MAX) temp = COLOR_TEMP_MAX
	def result = []
	log.debug "setColorTemperature($temp)"
	switch (currentValue("deviceModel")) {
		case "0": 
			// Single Color Device Type
			log.trace "setColorTemperature not supported on this device type"
			return
		break
		case "1":
			// Full CCT Devie Type
			if(temp < COLOR_TEMP_MIN) temp = COLOR_TEMP_MIN
			state.ctTarget=temp
			warmValue = ((COLOR_TEMP_MAX - temp) / COLOR_TEMP_DIFF * 255) as Integer
			coldValue = 255 - warmValue
			result << zwave.switchColorV3.switchColorSet(warmWhite: warmValue, coldWhite: coldValue, dimmingDuration: duration)
		break
		case "2":
			// RGBW Device type
			if (wwComponent) {
				// LED strip has warm white
				if(temp < wwKelvin) temp = wwKelvin
				state.ctTarget=temp
				def warmValue = ((COLOR_TEMP_MAX - temp) / COLOR_TEMP_DIFF_RGBW * 255) as Integer
				def coldValue = 255 - warmValue
				def rgbTemp = ctToRgb(6500)
				result << zwave.switchColorV3.switchColorSet(red: gammaCorrect(coldValue), green: gammaCorrect(Math.round(coldValue*0.9765)), blue: gammaCorrect(Math.round(coldValue*0.9922)), warmWhite: gammaCorrect(warmValue), dimmingDuration: duration)
			} else {
				// LED strip is RGB and has no white
				if(temp < COLOR_TEMP_MIN) temp = COLOR_TEMP_MIN
				def rgbTemp = ctToRgb(temp)
				state.ctTarget=temp
				log.debug "r: " + rgbTemp["r"] + " g: " + rgbTemp["g"] + " b: "+ rgbTemp["b"]
				log.debug "r: " + gammaCorrect(rgbTemp["r"]) + " g: " + gammaCorrect(rgbTemp["g"]) + " b: " + gammaCorrect(rgbTemp["b"])
				result << zwave.switchColorV3.switchColorSet(red: gammaCorrect(rgbTemp["r"]), green: gammaCorrect(rgbTemp["g"]), blue: gammaCorrect(rgbTemp["b"]), warmWhite: 0, dimmingDuration: duration)
			}
		break
	}
	if ((device.currentValue("switch") != "on") && !colorPrestage) {
			log.debug "Turning device on with pre-staging"
 			result << zwave.basicV1.basicSet(value: 0xFF)
			result << zwave.switchMultilevelV3.switchMultilevelGet()
	}
	result+=queryAllColors()
	logDebug result
	sendEvent(name: "colorMode", value: "CT")
	commands(result)
}

private queryAllColors() {
	def cmds=[]
	switch (currentValue("deviceModel")) {
		case "1":
			// cct device type
			CCT_NAMES.collect { cmds << zwave.switchColorV3.switchColorGet(colorComponentId: ZWAVE_COLOR_COMPONENT_ID[it]) }
		break
		case "2":
			// rgbw device type
			RGBW_NAMES.collect { cmds << zwave.switchColorV3.switchColorGet(colorComponentId: ZWAVE_COLOR_COMPONENT_ID[it]) }
		break
	}
	return cmds
}

private secEncap(physicalgraph.zwave.Command cmd) {
	zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
}

private crcEncap(physicalgraph.zwave.Command cmd) {
	zwave.crc16EncapV1.crc16Encap().encapsulate(cmd).format()
}

private command(physicalgraph.zwave.Command cmd) {
	if (currentValue("zwaveSecurePairingComplete") == "true") {
		return zwave.securityV1.securityMessageEncapsulation().encapsulate(cmd).format()
    } else {
		return cmd.format()
    }	
}

private commands(commands, delay=200) {
	delayBetween(commands.collect{ command(it) }, delay)
}

private ctToRgb(colorTemp) {
	// ct with rgb only
	float red=0
	float blue=0
	float green=0
	def temperature = colorTemp / 100 
	red = 255
	green=(99.4708025861 *  Math.log(temperature)) - 161.1195681661
	if (green < 0) green = 0
	if (green > 255) green = 255
	if (temperature >= 65) {
		blue=255
	} else if (temperature <= 19) {
		blue=0
	} else {
		blue = temperature - 10
		blue = (138.5177312231 * Math.log(blue)) - 305.0447927307
		if (blue < 0) blue = 0
		if (blue > 255) blue = 255
	}
	return ["r": Math.round(red), "g": Math.round(green), "b": Math.round(blue)]
}

private gammaCorrect(value) {
	def temp=value/255
	def correctedValue=(temp>0.4045) ? Math.pow((temp+0.055)/ 1.055, 2.4) : (temp / 12.92)
	return Math.round(correctedValue * 255) as Integer
}

def setGenericTempName(temp){
    if (!temp) return
    def genericName
    def value = temp.toInteger()
    if (value <= 2000) genericName = "Sodium"
    else if (value <= 2100) genericName = "Starlight"
    else if (value < 2400) genericName = "Sunrise"
    else if (value < 2800) genericName = "Incandescent"
    else if (value < 3300) genericName = "Soft White"
    else if (value < 3500) genericName = "Warm White"
    else if (value < 4150) genericName = "Moonlight"
    else if (value <= 5000) genericName = "Horizon"
    else if (value < 5500) genericName = "Daylight"
    else if (value < 6000) genericName = "Electronic"
    else if (value <= 6500) genericName = "Skylight"
    else if (value < 20000) genericName = "Polar"
    sendEvent(name: "colorName", value: genericName)
}

def setGenericName(hue){
    def colorName
    hue = Math.round(hue * 3.6) as Integer
    switch (hue){
        case 0..15: colorName = "Red"	
            break
        case 16..45: colorName = "Orange"
            break
        case 46..75: colorName = "Yellow"
            break
        case 76..105: colorName = "Chartreuse"
            break
        case 106..135: colorName = "Green"
            break
        case 136..165: colorName = "Spring"
            break
        case 166..195: colorName = "Cyan"
            break
        case 196..225: colorName = "Azure"
            break
        case 226..255: colorName = "Blue"
            break
        case 256..285: colorName = "Violet"
            break
        case 286..315: colorName = "Magenta"
            break
        case 316..345: colorName = "Rose"
            break
        case 346..360: colorName = "Red"
            break
    }
    if (device.currentValue("saturation") == 0) colorName = "White"
    sendEvent(name: "colorName", value: colorName)
}

def clamp( value, lowerBound = 0, upperBound = 100 ){
    // Takes a value and ensures it's between two defined thresholds

    value == null ? value = upperBound : null

    if(lowerBound < upperBound){
        if(value < lowerBound ){ value = lowerBound}
        if(value > upperBound){ value = upperBound}
    }
    else if(upperBound < lowerBound){
        if(value < upperBound){ value = upperBound}
        if(value > lowerBound ){ value = lowerBound}
    }

    return value
}

def logError(msg) {
  if (logLevel?.toInteger() >= 1 || logLevel == null) { log.error msg }
}

def logWarn(msg) {
  if (logLevel?.toInteger() >= 2) { log.warn msg }
}

def logInfo(msg) {
  if (logLevel?.toInteger() >= 3) { log.info msg }
}

def logDebug(msg) {
  if (logLevel?.toInteger() >= 4) { log.debug msg }
}

def logTrace(msg) {
  if (logLevel?.toInteger() >= 5) { log.trace msg }
}

def rgbToHSV(red, green, blue) {
	def hex = colorUtil.rgbToHex(red as int, green as int, blue as int)
	def hsv = colorUtil.hexToHsv(hex)
	return [hue: hsv[0], saturation: hsv[1], value: hsv[2]]
}

def huesatToRGB(hue, sat) {
	def color = colorUtil.hsvToHex(Math.round(hue) as int, Math.round(sat) as int)
	return colorUtil.hexToRgb(color)
}